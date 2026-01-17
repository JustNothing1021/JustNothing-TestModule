package com.justnothing.testmodule.service.handler;

import static com.justnothing.testmodule.constants.FileDirectory.PORT_FILE;

import android.os.IBinder;
import android.os.Parcel;

import androidx.annotation.NonNull;

import com.justnothing.testmodule.hooks.HookEntry;
import com.justnothing.testmodule.utils.data.DataDirectoryManager;
import com.justnothing.testmodule.utils.functions.CmdUtils;
import com.justnothing.testmodule.utils.functions.Logger;

import java.io.File;
import java.io.PrintWriter;

public class TransactionHandler {
    private static final Logger logger = new Logger() {
        @Override
        public String getTag() {
            return "TransactionHandler";
        }
    };

    public static final String DESCRIPTOR = "com.justnothing.testmodule.ShellService";
    public static final int TRANSACTION_EXECUTE_FILE = IBinder.FIRST_CALL_TRANSACTION + 1;
    public static final int TRANSACTION_EXECUTE_STREAM = IBinder.FIRST_CALL_TRANSACTION + 2;
    public static final int TRANSACTION_WRITE_PORT_FILE = IBinder.FIRST_CALL_TRANSACTION + 3;
    public static final int TRANSACTION_UPDATE_PORT = IBinder.FIRST_CALL_TRANSACTION + 4;
    public static final int TRANSACTION_WRITE_HOOK_DATA = IBinder.FIRST_CALL_TRANSACTION + 6;

    private final CommandHandler commandHandler;
    private final ServerPortManager serverPortManager;
    private final SocketServer socketServer;

    public TransactionHandler(CommandHandler commandHandler, ServerPortManager serverPortManager, SocketServer socketServer) {
        this.commandHandler = commandHandler;
        this.serverPortManager = serverPortManager;
        this.socketServer = socketServer;
    }

    public boolean handleTransaction(int code, @NonNull Parcel data, Parcel reply, int flags) {
        logger.debug("ShellService收到事务请求");
        logger.debug("事务码: " + code + " (十六进制: 0x" + Integer.toHexString(code) + ")");
        logger.debug("flags: " + flags);
        try {
            if (code != IBinder.INTERFACE_TRANSACTION) {
                try {
                    data.enforceInterface(DESCRIPTOR);
                    logger.debug("接口验证成功: " + DESCRIPTOR);
                } catch (Exception e) {
                    logger.warn("接口验证失败，但仍继续处理事务: " + e.getMessage());
                    data.setDataPosition(0);
                }
            }

            return switch (code) {
                case IBinder.INTERFACE_TRANSACTION -> {
                    logger.debug("处理INTERFACE_TRANSACTION，返回接口描述符: " + DESCRIPTOR);
                    reply.writeString(DESCRIPTOR);
                    yield true;
                }
                case TRANSACTION_EXECUTE_FILE -> {
                    logger.debug("处理文件执行命令");
                    yield handleExecuteFile(data, reply);
                }
                case TRANSACTION_EXECUTE_STREAM -> {
                    logger.debug("处理流式执行命令");
                    yield handleExecuteStream(data, reply);
                }
                case TRANSACTION_WRITE_PORT_FILE -> {
                    logger.debug("执行写入端口文件命令");
                    yield handleWritePortFile(reply);
                }
                case TRANSACTION_UPDATE_PORT -> {
                    logger.debug("处理更新端口事务");
                    yield handleUpdatePort(data, reply);
                }
                case TRANSACTION_WRITE_HOOK_DATA -> {
                    logger.debug("处理写入Hook数据命令");
                    yield handleWriteHookData(data, reply);
                }
                default -> {
                    logger.warn("未知的事务码: " + code);
                    yield false;
                }
            };
        } catch (Exception e) {
            logger.error("处理事务时发生错误，code: " + code, e);
            if (code == IBinder.INTERFACE_TRANSACTION) {
                try {
                    reply.writeString(DESCRIPTOR);
                    return true;
                } catch (Exception e2) {
                    logger.error("返回接口描述符失败", e2);
                }
            }
            return false;
        }
    }

    private boolean handleWritePortFile(Parcel reply) {
        boolean result = serverPortManager.writePortToFileWithRetry(5);
        reply.writeNoException();
        reply.writeInt(result ? 0 : 1);
        return true;
    }

    private boolean handleUpdatePort(Parcel data, Parcel reply) {
        File outputFile = null;
        PrintWriter writer = null;

        try {
            int newPort = data.readInt();
            logger.info("收到更新端口请求，新端口: " + newPort);

            String outputFilePath = data.readString();
            if (outputFilePath == null || outputFilePath.isEmpty()) {
                logger.error("输出文件路径为空");
                reply.writeNoException();
                reply.writeInt(-3);
                return true;
            }

            logger.info("输出文件: " + outputFilePath);
            outputFile = new File(outputFilePath);

            File parentDir = outputFile.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                boolean mkdirSuccess = parentDir.mkdirs();
                if (!mkdirSuccess) {
                    logger.warn("无法创建输出文件目录: " + parentDir.getAbsolutePath());
                }
                try {
                    DataDirectoryManager.setFilePermissions(parentDir, "777", "输出文件目录");
                } catch (Exception e) {
                    logger.warn("设置输出文件目录权限异常: " + parentDir.getAbsolutePath() + ", " + e.getMessage());
                }
            }

            if (!serverPortManager.isValidPort(newPort)) {
                String errorMsg = "端口号无效，必须在1024-65535范围内: " + newPort;
                logger.error(errorMsg);

                writer = new PrintWriter(outputFile);
                writer.println(errorMsg);
                writer.flush();

                reply.writeNoException();
                reply.writeInt(-1);
                return true;
            }

            if (!serverPortManager.isPortAvailable(newPort)) {
                String errorMsg = "端口 " + newPort + " 已被占用";
                logger.error(errorMsg);

                writer = new PrintWriter(outputFile);
                writer.println(errorMsg);
                writer.flush();

                reply.writeNoException();
                reply.writeInt(-2);
                return true;
            }

            logger.info("开始更新端口，从 " + serverPortManager.getCurrentPort() + " 到 " + newPort);
            boolean result = socketServer.restartWithNewPort(newPort);
            logger.info("端口更新结果: " + (result ? "成功" : "失败"));

            writer = new PrintWriter(outputFile);
            if (result) {
                String successMsg = "端口更新成功，新端口: " + serverPortManager.getCurrentPort();
                logger.info(successMsg);
                writer.println(successMsg);
                writer.println("端口文件已更新: " + PORT_FILE);
                writer.println("服务器已重启，正在监听新端口");

                reply.writeNoException();
                reply.writeInt(0);
            } else {
                String errorMsg = "端口更新失败，服务器重启失败";
                logger.error(errorMsg);
                writer.println(errorMsg);
                writer.println("当前端口仍为: " + serverPortManager.getCurrentPort());
                writer.println("建议：请检查端口是否被其他程序占用");

                reply.writeNoException();
                reply.writeInt(-4);
            }
            writer.flush();

            return true;

        } catch (Exception e) {
            logger.error("处理更新端口事务失败", e);

            if (writer == null && outputFile != null) {
                try {
                    writer = new PrintWriter(outputFile);
                    writer.println("处理更新端口时发生错误: " + e.getMessage());
                    writer.flush();
                } catch (Exception e2) {
                    logger.error("写入错误信息到输出文件失败", e2);
                }
            }

            reply.writeException(e);
            return false;
        } finally {
            if (writer != null) {
                writer.close();
            }
        }
    }

    private boolean handleExecuteFile(Parcel data, Parcel reply) {
        return commandHandler.handleExecuteFile(data, reply);
    }

    private boolean handleExecuteStream(Parcel data, Parcel reply) {
        return commandHandler.handleExecuteStream(data, reply);
    }

    public String getInterfaceDescriptor() {
        return DESCRIPTOR;
    }

    private boolean handleWriteHookData(Parcel data, Parcel reply) {
        try {
            logger.info("开始处理写入Hook数据事务");
            
            boolean fixPermissions = data.readInt() == 1;
            
            logger.info("写入Hook数据参数 - 修复权限: " + fixPermissions);
            
            if (fixPermissions) {
                logger.info("开始修复数据目录权限");
                boolean permissionFixed = setupDirectoryPermissions();
                if (!permissionFixed) {
                    logger.warn("权限修复失败，但继续尝试写入Hook数据");
                } else {
                    logger.info("权限修复成功");
                }
            }
            
            boolean writeSuccess = HookEntry.executeFileOperations();
            
            if (writeSuccess) {
                logger.info("Hook数据写入成功");
                reply.writeNoException();
                reply.writeInt(0);
                reply.writeString("Hook数据写入成功");
            } else {
                logger.error("Hook数据写入失败");
                reply.writeNoException();
                reply.writeInt(-1);
                reply.writeString("Hook数据写入失败");
            }
            
            return true;
            
        } catch (Exception e) {
            logger.error("处理写入Hook数据事务失败", e);
            reply.writeException(e);
            return false;
        }
    }
    
    private boolean setupDirectoryPermissions() {
        logger.info("设置数据目录权限...");
        
        String dataDir;
        try {
            dataDir = com.justnothing.testmodule.utils.data.DataDirectoryManager.getMethodsCmdlineDataDirectory();
            if (dataDir == null || dataDir.isEmpty()) {
                logger.error("数据目录路径为空，无法设置权限");
                return false;
            }
            logger.info("开始设置数据目录权限: " + dataDir);
        } catch (Exception e) {
            logger.error("获取数据目录路径失败: " + e.getMessage(), e);
            return false;
        }
        
        int maxRetries = 3;
        int retryDelayMs = 2000;
        long timeoutMs = 5000;
        
        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                logger.info("chmod尝试 " + attempt + "/" + maxRetries + " (超时: " + timeoutMs + "ms)");
                CmdUtils.CommandOutput result = CmdUtils.runRootCommand("chmod -R 777 " + dataDir, timeoutMs);
                
                logger.info("chmod命令执行结果 - 退出码: " + result.stat() + 
                            ", stdout: " + (result.stdout() != null ? result.stdout() : "(空)") + 
                            ", stderr: " + (result.stderr() != null ? result.stderr() : "(空)"));
                
                if (result.succeed()) {
                    logger.info("chmod -R 777 " + dataDir + " 执行成功");
                    return true;
                } else {
                    logger.warn("chmod尝试 " + attempt + " 失败，退出码: " + result.stat() + 
                               ", 错误: " + (result.stderr() != null ? result.stderr() : "(空)"));
                }
            } catch (Exception e) {
                logger.error("chmod尝试 " + attempt + " 异常: " + e.getMessage(), e);
            }
            
            if (attempt < maxRetries) {
                logger.info("等待 " + retryDelayMs + "ms 后重试...");
                try {
                    Thread.sleep(retryDelayMs);
                } catch (InterruptedException ie) {
                    logger.warn("chmod重试等待被中断");
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
        
        logger.error("chmod命令所有尝试均失败");
        return false;
    }

}
