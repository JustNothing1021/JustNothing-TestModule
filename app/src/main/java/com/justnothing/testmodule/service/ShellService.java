package com.justnothing.testmodule.service;

import android.os.Binder;
import android.os.Parcel;

import androidx.annotation.NonNull;

import com.justnothing.testmodule.command.CommandExecutor;
import com.justnothing.testmodule.service.handler.CommandHandler;
import com.justnothing.testmodule.service.handler.ServerPortManager;
import com.justnothing.testmodule.service.handler.SocketClientHandler;
import com.justnothing.testmodule.service.handler.SocketServer;
import com.justnothing.testmodule.service.handler.TransactionHandler;
import com.justnothing.testmodule.utils.data.DataDirectoryManager;
import com.justnothing.testmodule.utils.functions.CmdUtils;
import com.justnothing.testmodule.utils.functions.Logger;

import java.io.IOException;
import java.util.concurrent.Executors;

public class ShellService extends Binder {


    public static final class ServiceLogger extends Logger {
        @Override
        public String getTag() {
            return "ShellService";
        }
    }

    public static final ServiceLogger logger = new ServiceLogger();

    private final ServerPortManager serverPortManager;
    private final CommandExecutor commandExecutor;
    private final SocketClientHandler clientHandler;
    private final SocketServer socketServer;
    private final CommandHandler commandHandler;
    private final TransactionHandler transactionHandler;

    private static boolean setupDirectoryPermissions() {
        logger.info("设置数据目录权限...");
        
        String dataDir;
        try {
            dataDir = DataDirectoryManager.getMethodsCmdlineDataDirectory();
            if (dataDir == null || dataDir.isEmpty()) {
                logger.error("数据目录路径为空，无法设置权限");
                return false;
            }
            logger.info("开始设置数据目录权限: " + dataDir);
        } catch (Exception e) {
            logger.error("获取数据目录路径失败: " + e.getMessage(), e);
            return false;
        }
        
        logger.info("检查su是否可用...");
        int suMaxRetries = 5;
        long suTimeoutMs = 10000;
        long suRetryDelayMs = 3000;
        
        boolean suAvailable = false;
        for (int suAttempt = 1; suAttempt <= suMaxRetries; suAttempt++) {
            try {
                logger.info("su检查尝试 " + suAttempt + "/" + suMaxRetries + " (超时: " + suTimeoutMs + "ms)");
                CmdUtils.CommandOutput suCheck = CmdUtils.runRootCommand("echo 'su available'", suTimeoutMs);
                
                if (suCheck.succeed() && suCheck.stdout() != null && suCheck.stdout().contains("su available")) {
                    logger.info("su可用，继续执行chmod");
                    suAvailable = true;
                    break;
                } else {
                    logger.warn("su检查尝试 " + suAttempt + " 失败，退出码: " + suCheck.stat() + 
                               ", 输出: " + (suCheck.stdout() != null ? suCheck.stdout() : "(空)") + 
                               ", 错误: " + (suCheck.stderr() != null ? suCheck.stderr() : "(空)"));
                }
            } catch (InterruptedException e) {
                logger.warn("su检查尝试 " + suAttempt + " 超时: " + e.getMessage());
                Thread.currentThread().interrupt();
            } catch (IOException e) {
                logger.error("su检查尝试 " + suAttempt + " IO异常: " + e.getMessage(), e);
            } catch (Exception e) {
                logger.error("su检查尝试 " + suAttempt + " 未知异常: " + e.getMessage(), e);
            }
            
            if (suAttempt < suMaxRetries) {
                logger.info("等待 " + suRetryDelayMs + "ms 后重试su检查...");
                try {
                    Thread.sleep(suRetryDelayMs);
                } catch (InterruptedException ie) {
                    logger.warn("su重试等待被中断");
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
        
        if (!suAvailable) {
            logger.error("su不可用，所有尝试均失败，无法执行chmod");
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
                    
                    logger.info("验证权限设置...");
                    try {
                        CmdUtils.CommandOutput statResult = CmdUtils.runRootCommand("stat -c '%a' " + dataDir, 3000);
                        if (statResult.succeed() && statResult.stdout() != null) {
                            String permissions = statResult.stdout().trim();
                            logger.info("目录权限: " + permissions);
                            
                            if ("777".equals(permissions)) {
                                logger.info("权限验证成功，目录权限已正确设置为777");
                                return true;
                            } else {
                                logger.warn("权限验证失败，期望777但得到: " + permissions);
                            }
                        } else {
                            logger.warn("权限验证失败，无法获取目录权限");
                        }
                    } catch (Exception e) {
                        logger.warn("权限验证时出错: " + e.getMessage());
                    }
                    
                    return true;
                } else {
                    logger.warn("chmod尝试 " + attempt + " 失败，退出码: " + result.stat() + 
                               ", 错误: " + (result.stderr() != null ? result.stderr() : "(空)"));
                }
            } catch (InterruptedException e) {
                logger.warn("chmod尝试 " + attempt + " 超时: " + e.getMessage());
                Thread.currentThread().interrupt();
            } catch (IOException e) {
                logger.error("chmod尝试 " + attempt + " IO异常: " + e.getMessage(), e);
            } catch (Exception e) {
                logger.error("chmod尝试 " + attempt + " 未知异常: " + e.getMessage(), e);
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
        
        logger.error("chmod命令[" + "chmod -R 777 " + dataDir + "]所有尝试均失败");
        return false;
    }

    public ShellService() {
        logger.info("ShellService初始化开始");
        
        boolean permissionsSet = setupDirectoryPermissions();
        if (!permissionsSet) {
            logger.error("目录权限设置失败，服务可能无法正常工作");
        }
        
        try {
            this.serverPortManager = new ServerPortManager();
            logger.info("ServerPortManager创建成功");
        } catch (Exception e) {
            logger.error("创建ServerPortManager失败: " + e.getMessage(), e);
            throw new RuntimeException("ShellService初始化失败: ServerPortManager创建失败", e);
        }
        
        try {
            this.commandExecutor = new CommandExecutor();
            logger.info("CommandExecutor创建成功");
        } catch (Exception e) {
            logger.error("创建CommandExecutor失败: " + e.getMessage(), e);
            throw new RuntimeException("ShellService初始化失败: CommandExecutor创建失败", e);
        }
        
        try {
            this.clientHandler = new SocketClientHandler(commandExecutor);
            logger.info("SocketClientHandler创建成功");
        } catch (Exception e) {
            logger.error("创建SocketClientHandler失败: " + e.getMessage(), e);
            throw new RuntimeException("ShellService初始化失败: SocketClientHandler创建失败", e);
        }
        
        try {
            this.socketServer = new SocketServer(serverPortManager, clientHandler);
            logger.info("SocketServer创建成功");
        } catch (Exception e) {
            logger.error("创建SocketServer失败: " + e.getMessage(), e);
            throw new RuntimeException("ShellService初始化失败: SocketServer创建失败", e);
        }
        
        try {
            this.commandHandler = new CommandHandler(commandExecutor);
            logger.info("CommandHandler创建成功");
        } catch (Exception e) {
            logger.error("创建CommandHandler失败: " + e.getMessage(), e);
            throw new RuntimeException("ShellService初始化失败: CommandHandler创建失败", e);
        }
        
        try {
            this.transactionHandler = new TransactionHandler(commandHandler, serverPortManager, socketServer);
            logger.info("TransactionHandler创建成功");
        } catch (Exception e) {
            logger.error("创建TransactionHandler失败: " + e.getMessage(), e);
            throw new RuntimeException("ShellService初始化失败: TransactionHandler创建失败", e);
        }

        logger.info("ShellService初始化完成");
        
        Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "ShellService-StartThread");
            t.setDaemon(true);
            return t;
        }).submit(() -> {
            try {
                logger.info("ShellService构造完成，等待启动Socket服务器");
                socketServer.start();
                logger.info("Socket服务器启动成功");
            } catch (Exception e) {
                logger.error("启动Socket服务器失败: " + e.getMessage(), e);
            }
        });
    }

    @Override
    protected boolean onTransact(int code, @NonNull Parcel data, Parcel reply, int flags) {
        return transactionHandler.handleTransaction(code, data, reply, flags);
    }

    @Override
    public String getInterfaceDescriptor() {
        return transactionHandler.getInterfaceDescriptor();
    }


}
