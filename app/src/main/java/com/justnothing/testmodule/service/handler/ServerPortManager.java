package com.justnothing.testmodule.service.handler;

import static com.justnothing.testmodule.constants.CommandServer.DEFAULT_SOCKET_PORT;
import static com.justnothing.testmodule.constants.FileDirectory.PORT_FILE;

import com.justnothing.testmodule.utils.data.DataDirectoryManager;
import com.justnothing.testmodule.utils.functions.Logger;
import com.justnothing.testmodule.utils.functions.CmdUtils;
import com.justnothing.testmodule.utils.io.IOManager;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.ServerSocket;

public class ServerPortManager {
    private static final Logger logger = new Logger() {
        @Override
        public String getTag() {
            return "ServerPortManager";
        }
    };

    private int currentSocketPort = DEFAULT_SOCKET_PORT;

    public ServerPortManager() {
        initializePort();
    }

    public int getCurrentPort() {
        return currentSocketPort;
    }

    public void setCurrentPort(int port) {
        this.currentSocketPort = port;
    }

    public boolean updatePort(int newPort) {
        if (currentSocketPort == newPort) {
            logger.info("端口未改变，无需更新: " + newPort);
            return true;
        }

        if (!isValidPort(newPort)) {
            logger.error("端口号无效，必须在1024-65535范围内: " + newPort);
            return false;
        }

        if (!isPortAvailable(newPort)) {
            logger.error("端口 " + newPort + " 不可用或已被占用");
            return false;
        }

        currentSocketPort = newPort;
        writePortToFile();
        logger.info("端口已更新: " + currentSocketPort);
        return true;
    }

    public boolean isValidPort(int port) {
        return port >= 1024 && port <= 65535;
    }

    public boolean isPortAvailable(int port) {
        try (ServerSocket testSocket = new ServerSocket(port)) {
            testSocket.setReuseAddress(true);
            testSocket.setSoTimeout(750);
            return true;
        } catch (IOException e) {
            logger.warn("端口 " + port + " 不可用: " + e.getMessage());
            return false;
        }
    }

    public int findAvailablePort(int preferredPort) {
        if (isPortAvailable(preferredPort)) {
            return preferredPort;
        }

        logger.warn("端口 " + preferredPort + " 被占用，尝试其他端口...");

        for (int i = 0; i < 10; i++) {
            int port = 20000 + (int)(Math.random() * 10000);
            if (isPortAvailable(port)) {
                logger.info("找到可用端口: " + port);
                return port;
            }
        }

        return -1;
    }

    public void writePortToFile() {
        try {
            File portFile = new File(PORT_FILE);

            File parentDir = portFile.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                boolean mkdirSuccess = parentDir.mkdirs();
                if (!mkdirSuccess) {
                    logger.warn("无法创建端口文件目录: " + parentDir.getAbsolutePath());
                }
                try {
                    DataDirectoryManager.setFilePermissions(parentDir, "777", "端口文件目录");
                } catch (Exception e) {
                    logger.warn("设置端口文件目录权限异常: " + parentDir.getAbsolutePath() + ", " + e.getMessage());
                }
            } else if (parentDir != null && (!parentDir.canRead() || !parentDir.canWrite() || !parentDir.canExecute())) {
                try {
                    DataDirectoryManager.setFilePermissions(parentDir, "777", "端口文件目录");
                } catch (Exception e) {
                    logger.warn("设置端口文件目录权限异常: " + parentDir.getAbsolutePath() + ", " + e.getMessage());
                }
            }

            IOManager.writeFile(portFile.getAbsolutePath(), String.valueOf(currentSocketPort));

            logger.info("端口文件已写入: " + PORT_FILE + ", 端口: " + currentSocketPort);

            try {
                DataDirectoryManager.setFilePermissions(new File(PORT_FILE), "777", "端口文件");
            } catch (Exception e) {
                logger.warn("设置端口文件权限异常: " + PORT_FILE + ", " + e.getMessage());
            }

        } catch (Exception e) {
            logger.error("写入端口文件失败: " + e.getMessage());
        }
    }

    public boolean writePortToFileWithRetry(int maxRetries) {
        for (int i = 0; i < maxRetries; i++) {
            try {
                writePortToFile();

                File portFile = new File(PORT_FILE);
                if (portFile.exists()) {
                    String portStr = IOManager.readFile(portFile.getAbsolutePath());
                    if (portStr != null && Integer.parseInt(portStr.trim()) == currentSocketPort) {
                        logger.debug("端口文件验证成功，尝试 " + (i + 1) + "/" + maxRetries);
                        return true;
                    }
                }

                logger.warn("端口文件写入验证失败，等待后重试...");
                Thread.sleep(100);

            } catch (Exception e) {
                logger.warn("写入端口文件尝试 " + (i + 1) + "/" + maxRetries + " 失败: " + e.getMessage());
                try {
                    Thread.sleep(100);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }

        logger.error("写入端口文件失败，已达到最大重试次数: " + maxRetries);
        return false;
    }

    private boolean readPortFromFile() {
        try {
            File portFile = new File(PORT_FILE);
            if (!portFile.exists()) {
                logger.info("端口文件不存在: " + PORT_FILE);
                return false;
            }

            String portStr = IOManager.readFile(portFile.getAbsolutePath());
            if (portStr == null || portStr.trim().isEmpty()) {
                logger.warn("端口文件为空");
                return false;
            }

            int port = Integer.parseInt(portStr.trim());

            if (!isValidPort(port)) {
                logger.warn("端口文件中的端口号无效: " + port + "，有效范围: 1024-65535");
                return false;
            }

            currentSocketPort = port;
            logger.info("从端口文件读取端口: " + currentSocketPort);
            return true;

        } catch (NumberFormatException e) {
            logger.warn("端口文件中的内容不是有效的数字: " + e.getMessage());
        } catch (Exception e) {
            logger.warn("读取端口文件时发生未知错误: " + e.getMessage());
        }

        return false;
    }

    private void initializePort() {
        boolean readSuccess = readPortFromFile();

        if (!readSuccess) {
            currentSocketPort = DEFAULT_SOCKET_PORT;
            logger.info("使用默认端口: " + currentSocketPort);
        }

        boolean writeSuccess = writePortToFileWithRetry(3);
        if (!writeSuccess) {
            logger.error("初始化端口文件失败，但将继续启动服务");
        }
    }

    public static boolean isSocketServerRunning() {
        try {
            File portFile = new File(PORT_FILE);
            if (!portFile.exists()) {
                return false;
            }

            String portStr = IOManager.readFile(portFile.getAbsolutePath());

            if (portStr == null) {
                return false;
            }

            int port = Integer.parseInt(portStr.trim());

            try (java.net.Socket socket = new java.net.Socket()) {
                socket.connect(new java.net.InetSocketAddress("localhost", port), 1000);
                return true;
            } catch (Exception e) {
                return false;
            }

        } catch (Exception e) {
            return false;
        }
    }
}
