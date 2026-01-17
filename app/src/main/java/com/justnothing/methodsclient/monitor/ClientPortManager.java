package com.justnothing.methodsclient.monitor;


import static com.justnothing.testmodule.constants.CommandClient.UPDATE_PORT_TEMP_DIR;
import static com.justnothing.testmodule.constants.FileDirectory.PORT_FILE;
import static com.justnothing.testmodule.constants.FileDirectory.SESSION_PREFIX;
import static com.justnothing.testmodule.constants.FileDirectory.RESULT_FILE_NAME;
import static com.justnothing.testmodule.hooks.tests.ShellServiceHook.SERVICE_NAME;
import static com.justnothing.testmodule.service.handler.TransactionHandler.TRANSACTION_UPDATE_PORT;

import com.justnothing.methodsclient.StreamClient;
import com.justnothing.methodsclient.executor.FileCommandExecutor;
import com.justnothing.testmodule.utils.functions.CmdUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.lang.Process;

public class ClientPortManager {

    private static final int DEFAULT_PORT = 11451;

    private static final StreamClient.ClientLogger logger = new StreamClient.ClientLogger();

    public static int getSocketPort() {
        try {
            File portFile = new File(PORT_FILE);
            if (portFile.exists()) {
                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(new FileInputStream(portFile), StandardCharsets.UTF_8));
                String portStr = reader.readLine();
                reader.close();

                if (portStr != null) {
                    return Integer.parseInt(portStr.trim());
                }
            }
        } catch (Exception e) {
            // 忽略
        }
        return DEFAULT_PORT;
    }

    public static boolean checkSocketServer() {
        try {
            File portFile = new File(PORT_FILE);
            if (portFile.exists()) {
                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(new FileInputStream(portFile), StandardCharsets.UTF_8));
                String portStr = reader.readLine();
                reader.close();

                if (portStr != null) {
                    int port = Integer.parseInt(portStr.trim());
                    return tryConnect(port);
                }
            }
        } catch (Exception ignored) {
        }

        return tryConnect(DEFAULT_PORT);
    }

    private static boolean tryConnect(int port) {
        long startTime = System.currentTimeMillis();
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress("localhost", port), 1000);
            long duration = System.currentTimeMillis() - startTime;
            logger.debug("连接测试成功，端口: " + port + ", 耗时: " + duration + "ms");
            return true;
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            logger.error("连接测试失败，端口: " + port + ", 耗时: " + duration + "ms");
            return false;
        }
    }

    public static boolean updateSocketPort(int newPort) {
        if (newPort < 1024 || newPort > 65535) {
            System.err.println("端口号必须在1024-65535范围内");
            return false;
        }

        boolean success = false;
        File tempOutputFile = null;

        try {
            String tmpDir = UPDATE_PORT_TEMP_DIR + "/" + SESSION_PREFIX + System.nanoTime() + "_" + android.os.Process.myPid();
            File tempDir = new File(tmpDir);
            if (!tempDir.exists()) {
                if (!tempDir.mkdirs()) logger.warn("创建新目录失败");
            }

            String outputFilePath = tmpDir + "/" + RESULT_FILE_NAME;
            tempOutputFile = new File(outputFilePath);

            logger.info("更新端口: " + newPort + ", 输出文件: " + outputFilePath);

            String[] serviceCmd = new String[]{"service", "call", SERVICE_NAME,
                    String.valueOf(TRANSACTION_UPDATE_PORT),
                    "i32", String.valueOf(newPort),
                    "s16", outputFilePath};

            // 使用CmdUtils执行服务调用
            String serviceCommand = String.join(" ", serviceCmd);
            CmdUtils.CommandOutput serviceResult = CmdUtils.runCommand(serviceCommand, 15000);
            
            if (serviceResult.succeed()) {
                logger.info("执行完成, 输出: " + serviceResult.stdout());

                int maxWait = 10;
                int waited = 0;
                boolean fileFound = false;

                while (waited < maxWait) {
                    if (tempOutputFile.exists() && tempOutputFile.length() > 0) {
                        fileFound = true;
                        break;
                    }
                    Thread.sleep(500);
                    waited++;
                }

                if (fileFound) {
                    try (BufferedReader fileReader = new BufferedReader(
                            new InputStreamReader(new FileInputStream(tempOutputFile), StandardCharsets.UTF_8))) {
                        String fileLine;
                        while ((fileLine = fileReader.readLine()) != null) {
                            System.out.println(fileLine);
                        }
                    }
                    success = true;
                    logger.info("端口更新成功: " + newPort);

                    writePortToFile(newPort);

                } else {
                    String errorMsg = "等待输出文件超时，可能服务端写入失败";
                    logger.error(errorMsg);
                    System.err.println("错误: " + errorMsg);
                }
            } else {
                String errorMsg = "服务调用失败，退出码: " + serviceResult.stat() + ", 输出: " + serviceResult.stdout();
                logger.error(errorMsg);
                System.err.println("错误: " + errorMsg);
            }

        } catch (Exception e) {
            String errorMsg = "更新端口失败: " + e.getMessage();
            logger.error("更新端口失败", e);
            System.err.println("错误: " + errorMsg);
        } finally {
            if (tempOutputFile != null) {
                String tempDir = tempOutputFile.getParent();
                FileCommandExecutor.cleanupTempDir(tempDir);
            }
        }

        return success;
    }

    private static void writePortToFile(int port) {
        try {
            File portFile = new File(PORT_FILE);
            File parent = portFile.getParentFile();
            if (parent != null && !parent.exists()) {
                if (!parent.mkdirs()) {
                    logger.error("创建端口文件目录失败: " + parent.getAbsolutePath());
                    return;
                }
                try {
                    CmdUtils.CommandOutput chmodResult = CmdUtils.runRootCommand("chmod 777 " + parent.getAbsolutePath(), 5000);
                    if (!chmodResult.succeed()) {
                        logger.warn("设置端口文件目录权限失败: " + parent.getAbsolutePath());
                    }
                } catch (Exception e) {
                    logger.warn("设置端口文件目录权限异常: " + parent.getAbsolutePath() + ", " + e.getMessage());
                }
            } else if (parent != null && (!parent.canRead() || !parent.canWrite() || !parent.canExecute())) {
                try {
                    CmdUtils.CommandOutput chmodResult = CmdUtils.runRootCommand("chmod 777 " + parent.getAbsolutePath(), 5000);
                    if (!chmodResult.succeed()) {
                        logger.warn("设置端口文件目录权限失败: " + parent.getAbsolutePath());
                    }
                } catch (Exception e) {
                    logger.warn("设置端口文件目录权限异常: " + parent.getAbsolutePath() + ", " + e.getMessage());
                }
            }

            try (PrintWriter writer = new PrintWriter(portFile)) {
                writer.println(port);
                writer.flush();
            }

            logger.info("客户端端口文件已更新: " + port);

        } catch (Exception e) {
            logger.warn("客户端写入端口文件失败: " + e.getMessage());
        }
    }


}
