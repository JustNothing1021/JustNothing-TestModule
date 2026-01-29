package com.justnothing.testmodule.service.handler;

import com.justnothing.testmodule.utils.functions.Logger;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

public class SocketServer {
    private static final Logger logger = new Logger() {
        @Override
        public String getTag() {
            return "SocketServer";
        }
    };

    private final ServerPortManager serverPortManager;
    private final SocketClientHandler clientHandler;
    private ServerSocket socketServer;
    private final ExecutorService socketExecutor = Executors.newCachedThreadPool();
    private final AtomicBoolean socketServerRunning = new AtomicBoolean(false);
    private final AtomicBoolean restartingSocketServer = new AtomicBoolean(false);

    public SocketServer(ServerPortManager serverPortManager, SocketClientHandler clientHandler) {
        this.serverPortManager = serverPortManager;
        this.clientHandler = clientHandler;
    }

    public void start() {
        if (socketServerRunning.get() || restartingSocketServer.get()) {
            logger.warn("Socket服务器已经在运行或正在重启，跳过启动");
            return;
        }

        restartingSocketServer.set(true);
        logger.info("准备启动Socket服务器，当前状态: running=" + socketServerRunning.get() + ", restarting=" + restartingSocketServer.get());

        socketExecutor.submit(() -> {
            Thread currentThread = Thread.currentThread();
            String originalThreadName = currentThread.getName();
            currentThread.setName("ShellServiceSocketServer");
            
            try {
                logger.info("正在启动Socket服务器，端口: " + serverPortManager.getCurrentPort());

                if (!serverPortManager.isPortAvailable(serverPortManager.getCurrentPort())) {
                    logger.error("端口 " + serverPortManager.getCurrentPort() + " 不可用或已被占用");

                    int availablePort = serverPortManager.findAvailablePort(serverPortManager.getCurrentPort());
                    if (availablePort > 0) {
                        logger.warn("端口 " + serverPortManager.getCurrentPort() + " 被占用，自动切换到端口: " + availablePort);
                        serverPortManager.setCurrentPort(availablePort);
                        serverPortManager.writePortToFileWithRetry(3);
                    } else {
                        logger.error("找不到可用端口，Socket服务器启动失败");
                        socketServerRunning.set(false);
                        restartingSocketServer.set(false);
                        return;
                    }
                }

                socketServer = new ServerSocket(serverPortManager.getCurrentPort());
                socketServer.setReuseAddress(true);
                socketServer.setSoTimeout(5000);

                logger.info("TCP Socket服务器已启动，端口: " + serverPortManager.getCurrentPort());
                socketServerRunning.set(true);
                restartingSocketServer.set(false);

                while (socketServerRunning.get()) {
                    try {
                        Socket clientSocket = socketServer.accept();
                        logger.debug("接受到新的客户端连接: " + clientSocket.getRemoteSocketAddress());
                        clientHandler.handleClient(clientSocket);
                    } catch (SocketTimeoutException ignored) {
                    } catch (IOException e) {
                        if (socketServerRunning.get()) {
                            if (e.getMessage() != null && (
                                    e.getMessage().contains("Socket closed") ||
                                            e.getMessage().contains("Interrupted") ||
                                            e.getMessage().contains("socket closed"))) {
                                logger.info("ServerSocket已关闭，退出循环: " + e.getMessage());
                                break;
                            } else {
                                logger.warn("接受客户端连接时发生异常: " + e.getMessage());
                            }
                        }
                    }
                }

            } catch (Exception e) {
                logger.error("启动Socket服务器失败，端口: " + serverPortManager.getCurrentPort(), e);
                if (e instanceof IOException) {
                    String message = e.getMessage();
                    if (message != null && message.contains("Address already in use")) {
                        logger.error("端口 " + serverPortManager.getCurrentPort() + " 已被占用");
                    } else if (message != null && message.contains("Permission denied")) {
                        logger.error("端口 " + serverPortManager.getCurrentPort() + " 权限不足");
                    }
                }
                socketServerRunning.set(false);
                restartingSocketServer.set(false);
            } finally {
                currentThread.setName(originalThreadName);
                if (socketServer != null) {
                    try {
                        socketServer.close();
                        logger.info("ServerSocket已关闭");
                    } catch (IOException e) {
                        logger.warn("关闭ServerSocket时出错: " + e.getMessage());
                    }
                    socketServer = null;
                }
                logger.info("Socket服务器线程结束");
            }
        });
    }

    public synchronized void stop() {
        if (!socketServerRunning.get()) {
            return;
        }

        logger.info("停止Socket服务器，端口: " + serverPortManager.getCurrentPort());
        socketServerRunning.set(false);

        if (socketServer != null) {
            try {
                socketServer.close();
            } catch (IOException e) {
                logger.warn("关闭ServerSocket时出错: " + e.getMessage());
            }
            socketServer = null;
        }

        try {
            Thread.sleep(200);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public boolean isRunning() {
        return socketServerRunning.get();
    }

    public boolean restartWithNewPort(int newPort) {
        if (serverPortManager.getCurrentPort() == newPort) {
            logger.info("端口未改变，无需重启: " + newPort);
            return true;
        }

        logger.info("更新端口: " + serverPortManager.getCurrentPort() + " -> " + newPort);

        int oldPort = serverPortManager.getCurrentPort();

        if (!serverPortManager.updatePort(newPort)) {
            logger.error("端口 " + newPort + " 不可用或已被占用");
            return false;
        }

        stop();

        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        start();

        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        if (socketServerRunning.get()) {
            logger.info("端口更新成功，从 " + oldPort + " 切换到 " + serverPortManager.getCurrentPort());
            return true;
        } else {
            logger.error("端口更新失败，无法启动服务器，回退到旧端口: " + oldPort);
            serverPortManager.setCurrentPort(oldPort);
            serverPortManager.writePortToFile();
            start();
            return false;
        }
    }
}
