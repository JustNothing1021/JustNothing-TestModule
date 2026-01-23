package com.justnothing.methodsclient.executor;

import com.justnothing.methodsclient.monitor.ClientPortManager;
import com.justnothing.methodsclient.StreamClient;
import com.justnothing.methodsclient.monitor.PerformanceMonitor;
import com.justnothing.testmodule.service.protocol.InteractiveProtocol;
import com.justnothing.testmodule.utils.concurrent.ThreadPoolManager;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

public class SocketCommandExecutor {

    private static final int CONNECT_TIMEOUT = 5000;
    private static final int EXEC_TIMEOUT = 86400000;
    private static final int SOCKET_READ_TIMEOUT = 30000;

    private static final StreamClient.ClientLogger logger = new StreamClient.ClientLogger();

    public boolean executeInteractiveSocket(String command) {
        long startTime = System.currentTimeMillis();
        int port = ClientPortManager.getSocketPort();
        AtomicLong bytesRead = new AtomicLong(0);

        Socket socket = null;

        try {
            socket = new Socket();
            socket.setTcpNoDelay(true);
            socket.setKeepAlive(true);
            socket.setSoTimeout(SOCKET_READ_TIMEOUT);
            socket.connect(new InetSocketAddress("localhost", port), CONNECT_TIMEOUT);
            logger.info("创建交互式Socket连接，端口: " + port);

            final InputStream input = socket.getInputStream();
            final OutputStream output = socket.getOutputStream();

            InteractiveProtocol.writeMessage(output, InteractiveProtocol.TYPE_CLIENT_COMMAND,
                    command.getBytes(StandardCharsets.UTF_8));

            logger.info("命令已发送，开始读取响应...");
            logger.debug("命令长度: " + command.length() + " 字符");

            AtomicBoolean reading = new AtomicBoolean(true);

            Socket finalSocket = socket;
            Callable<Boolean> readTask = () -> SocketStreamReader.readInteractiveSocketStream(input, output, reading, bytesRead, finalSocket);

            Future<Boolean> future = ThreadPoolManager.submitSocketCallable(readTask);

            try {
                Boolean success = future.get(EXEC_TIMEOUT, TimeUnit.MILLISECONDS);
                long duration = System.currentTimeMillis() - startTime;

                if (success != null && success) {
                    PerformanceMonitor.recordSocketCommand(duration, bytesRead.get(),
                            bytesRead.get(), true);
                    logger.info("命令执行成功，耗时: " + duration + "ms, 读取字节: " + bytesRead.get());

                    System.out.flush();
                    System.err.flush();

                    return true;
                } else {
                    PerformanceMonitor.recordSocketCommand(duration, bytesRead.get(),
                            bytesRead.get(), false);
                    logger.error("命令执行失败，耗时: " + duration + "ms");
                    System.err.println("命令执行失败");
                    return false;
                }

            } catch (TimeoutException e) {
                System.err.println("\n命令执行超时");
                logger.error("命令执行超时（" + EXEC_TIMEOUT + "ms）");
                reading.set(false);
                future.cancel(true);
                PerformanceMonitor.recordSocketCommand(
                        System.currentTimeMillis() - startTime, 0, 0, false);
                return false;
            } catch (Exception e) {
                System.err.println("\n执行异常: " + e.getMessage());
                logger.error("命令执行出错", e);
                reading.set(false);
                PerformanceMonitor.recordSocketCommand(
                        System.currentTimeMillis() - startTime, 0, 0, false);
                return false;
            }

        } catch (SocketTimeoutException e) {
            System.err.println("\n连接超时");
            logger.error("连接超时");
            PerformanceMonitor.recordSocketCommand(
                    System.currentTimeMillis() - startTime, 0, 0, false);
            return false;
        } catch (IOException e) {
            System.err.println("\nSocket连接失败: " + e.getMessage());
            logger.error("Socket连接失败: ", e);
            PerformanceMonitor.recordSocketCommand(
                    System.currentTimeMillis() - startTime, 0, 0, false);
            return false;
        } catch (Exception e) {
            System.err.println("\n未知错误: " + e.getMessage());
            logger.error("未知错误", e);
            PerformanceMonitor.recordSocketCommand(
                    System.currentTimeMillis() - startTime, 0, 0, false);
            return false;
        } finally {
            closeQuietly(socket);
        }
    }

    public boolean executeTextSocket(String command) {
        long startTime = System.currentTimeMillis();
        int port = ClientPortManager.getSocketPort();
        AtomicLong bytesRead = new AtomicLong(0);
        AtomicLong charsRead = new AtomicLong(0);

        Socket socket = null;
        boolean success;

        try {
            socket = new Socket();
            socket.setTcpNoDelay(true);
            socket.setKeepAlive(true);
            socket.setSoTimeout(SOCKET_READ_TIMEOUT);
            socket.connect(new InetSocketAddress("localhost", port), CONNECT_TIMEOUT);
            logger.info("创建Socket连接，端口: " + port);

            InputStream input = socket.getInputStream();
            OutputStream output = socket.getOutputStream();

            PrintWriter writer = new PrintWriter(output, true);
            writer.println(command);
            writer.flush();

            logger.info("命令已发送，开始实时读取响应...");
            logger.debug("命令长度: " + command.length() + " 字符");

            AtomicBoolean reading = new AtomicBoolean(true);
            var future = ThreadPoolManager.submitSocketCallable(() -> SocketStreamReader.readTextProtocolSocketStream(input, reading, bytesRead, charsRead));

            try {
                boolean serverClosedConnection = future.get(EXEC_TIMEOUT, TimeUnit.MILLISECONDS);

                if (serverClosedConnection) {
                    logger.info("命令执行完成（服务器主动关闭连接）");
                    success = true;
                } else {
                    logger.warn("读取线程结束但未检测到服务器关闭连接");
                    success = false;
                }

            } catch (TimeoutException e) {
                System.err.println("命令执行超时");
                logger.error("命令执行超时（" + EXEC_TIMEOUT + "ms）");
                reading.set(false);
                future.cancel(true);
                success = false;
            } catch (Exception e) {
                System.err.println("执行异常: " + e.getMessage());
                logger.error("Socket命令执行出错", e);
                reading.set(false);
                success = false;
            }

            long duration = System.currentTimeMillis() - startTime;

            if (success) {
                PerformanceMonitor.recordSocketCommand(duration, bytesRead.get(), charsRead.get(), true);
                logger.info("命令执行成功，耗时: " + duration + "ms, 读取字符: " + charsRead.get());
            } else {
                PerformanceMonitor.recordSocketCommand(duration, bytesRead.get(), charsRead.get(), false);
                logger.error("命令执行失败，耗时: " + duration + "ms");
            }

            return success;

        } catch (SocketTimeoutException e) {
            System.err.println("连接超时");
            logger.error("连接超时");
            closeQuietly(socket);
            PerformanceMonitor.recordSocketCommand(
                    System.currentTimeMillis() - startTime, 0, 0, false);
            return false;
        } catch (IOException e) {
            System.err.println("Socket连接失败: " + e.getMessage());
            logger.error("Socket连接失败: ", e);
            closeQuietly(socket);
            PerformanceMonitor.recordSocketCommand(
                    System.currentTimeMillis() - startTime, 0, 0, false);
            return false;
        } catch (Exception e) {
            System.err.println("未知错误: " + e.getMessage());
            logger.error("未知错误", e);
            closeQuietly(socket);
            PerformanceMonitor.recordSocketCommand(
                    System.currentTimeMillis() - startTime, 0, 0, false);
            return false;
        } finally {
            closeQuietly(socket);
        }
    }

    private static void closeQuietly(Socket socket) {
        if (socket != null) {
            try {
                socket.close();
            } catch (IOException ignored) {
            }
        }
    }

    public void shutdown() {
    }

    public static class ExecutionResult {
        public boolean success;
        public String output;
        public String error;

        public ExecutionResult(boolean success, String output, String error) {
            this.success = success;
            this.output = output;
            this.error = error;
        }
    }

    public ExecutionResult executeTextSocketWithOutput(String command) {
        long startTime = System.currentTimeMillis();
        int port = ClientPortManager.getSocketPort();
        AtomicLong bytesRead = new AtomicLong(0);
        AtomicLong charsRead = new AtomicLong(0);
        StringBuilder outputBuilder = new StringBuilder();

        Socket socket = null;
        boolean success;

        try {
            socket = new Socket();
            socket.setTcpNoDelay(true);
            socket.setKeepAlive(true);
            socket.setSoTimeout(SOCKET_READ_TIMEOUT);
            socket.connect(new InetSocketAddress("localhost", port), CONNECT_TIMEOUT);
            logger.info("创建Socket连接，端口: " + port);

            InputStream input = socket.getInputStream();
            OutputStream output = socket.getOutputStream();

            PrintWriter writer = new PrintWriter(output, true);
            writer.println(command);
            writer.flush();

            logger.info("命令已发送，开始读取响应...");
            logger.debug("命令长度: " + command.length() + " 字符");

            AtomicBoolean reading = new AtomicBoolean(true);
            var future = ThreadPoolManager.submitSocketCallable(() -> SocketStreamReader.readSocketStreamToString(input, reading, bytesRead, charsRead, outputBuilder));

            try {
                boolean serverClosedConnection = future.get(EXEC_TIMEOUT, TimeUnit.MILLISECONDS);

                if (serverClosedConnection) {
                    logger.info("命令执行完成（服务器主动关闭连接）");
                    success = true;
                } else {
                    logger.warn("读取线程结束但未检测到服务器关闭连接");
                    success = false;
                }

            } catch (TimeoutException e) {
                System.err.println("命令执行超时");
                logger.error("命令执行超时（" + EXEC_TIMEOUT + "ms）");
                reading.set(false);
                future.cancel(true);
                success = false;
            } catch (Exception e) {
                System.err.println("执行异常: " + e.getMessage());
                logger.error("Socket命令执行出错", e);
                reading.set(false);
                success = false;
            }

            long duration = System.currentTimeMillis() - startTime;

            if (success) {
                PerformanceMonitor.recordSocketCommand(duration, bytesRead.get(), charsRead.get(), true);
                logger.info("命令执行成功，耗时: " + duration + "ms, 读取字符: " + charsRead.get());
            } else {
                PerformanceMonitor.recordSocketCommand(duration, bytesRead.get(), charsRead.get(), false);
                logger.error("命令执行失败，耗时: " + duration + "ms");
            }

            return new ExecutionResult(success, outputBuilder.toString(), "");

        } catch (SocketTimeoutException e) {
            System.err.println("连接超时");
            logger.error("连接超时");
            closeQuietly(socket);
            PerformanceMonitor.recordSocketCommand(
                    System.currentTimeMillis() - startTime, 0, 0, false);
            return new ExecutionResult(false, "", "连接超时");
        } catch (IOException e) {
            System.err.println("Socket连接失败: " + e.getMessage());
            logger.error("Socket连接失败: ", e);
            closeQuietly(socket);
            PerformanceMonitor.recordSocketCommand(
                    System.currentTimeMillis() - startTime, 0, 0, false);
            return new ExecutionResult(false, "", "Socket连接失败: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("未知错误: " + e.getMessage());
            logger.error("未知错误", e);
            closeQuietly(socket);
            PerformanceMonitor.recordSocketCommand(
                    System.currentTimeMillis() - startTime, 0, 0, false);
            return new ExecutionResult(false, "", "未知错误: " + e.getMessage());
        } finally {
            closeQuietly(socket);
        }
    }
}
