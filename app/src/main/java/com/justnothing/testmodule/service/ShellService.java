package com.justnothing.testmodule.service;


import android.os.Binder;
import android.os.Parcel;

import androidx.annotation.NonNull;

import com.justnothing.testmodule.command.framework.CommandExecutor;
import com.justnothing.testmodule.constants.FileDirectory;
import com.justnothing.testmodule.service.handler.CommandHandler;
import com.justnothing.testmodule.service.handler.ServerPortManager;
import com.justnothing.testmodule.service.handler.SocketClientHandler;
import com.justnothing.testmodule.service.handler.SocketServer;
import com.justnothing.testmodule.service.handler.TransactionHandler;
import com.justnothing.testmodule.utils.data.DataBridge;
import com.justnothing.testmodule.utils.data.DataDirectoryManager;
import com.justnothing.testmodule.utils.logging.Logger;
import com.justnothing.testmodule.utils.concurrent.ThreadPoolManager;
import com.justnothing.testmodule.utils.io.IOManager;
import com.justnothing.testmodule.utils.io.ShellExecutionException;
import com.justnothing.testmodule.utils.io.ShellExecutorProvider;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.Objects;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class ShellService extends Binder {


    public static final Logger logger = Logger.getLoggerForName("ShellService");

    private final SocketServer socketServer;
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
        int suMaxRetries = 2;
        long suTimeoutMs = 5000;
        long suRetryDelayMs = 1000;
        
        boolean suAvailable = false;
        for (int suAttempt = 1; suAttempt <= suMaxRetries; suAttempt++) {
            try {
                logger.info("su检查尝试 " + suAttempt + "/" + suMaxRetries + " (超时: " + suTimeoutMs + "ms)");
                // 用 `id -u` 真正验证是否拿到 root。
                IOManager.ProcessResult suCheck = ShellExecutorProvider.get().execute("id -u", suTimeoutMs);
                String suUid = suCheck.stdout() != null ? suCheck.stdout().trim() : "";

                if (suCheck.isSuccess() && "0".equals(suUid)) {
                    logger.info("su可用，继续执行chmod");
                    suAvailable = true;
                    break;
                } else {
                    logger.warn("su检查尝试 " + suAttempt + " 失败，退出码: " + suCheck.exitCode() + 
                               ", 输出: " + (suCheck.stdout() != null ? suCheck.stdout() : "(空)") + 
                               ", 错误: " + (suCheck.stderr() != null ? suCheck.stderr() : "(空)"));
                }
            } catch (ShellExecutionException e) {
                logger.error("su检查尝试 " + suAttempt + " Shell执行异常: " + e.getMessage(), e);
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
        
        int maxRetries = 2;
        int retryDelayMs = 1000;
        long timeoutMs = 5000;
        
        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                logger.info("chmod尝试 " + attempt + "/" + maxRetries + " (超时: " + timeoutMs + "ms)");
                IOManager.ProcessResult result = ShellExecutorProvider.get().execute("chmod -R 777 " + dataDir, timeoutMs);

                logger.info("chmod命令执行结果 - 退出码: " + result.exitCode() +
                            ", stdout: " + Objects.requireNonNullElse(result.stdout(), "空") +
                            ", stderr: " + Objects.requireNonNullElse(result.stderr(), "空"));
                
                if (result.isSuccess()) {
                    logger.info("chmod -R 777 " + dataDir + " 执行成功");
                    
                    logger.info("验证权限设置...");
                    try {
                        IOManager.ProcessResult statResult = ShellExecutorProvider.get().execute("stat -c '%a' " + dataDir, 3000);
                        if (statResult.isSuccess() && statResult.stdout() != null) {
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
                    logger.warn("chmod尝试 " + attempt + " 失败，退出码: " + result.exitCode() + 
                               ", 错误: " + (result.stderr() != null ? result.stderr() : "(空)"));
                }
            } catch (ShellExecutionException e) {
                logger.error("chmod尝试 " + attempt + " Shell执行异常: " + e.getMessage(), e);
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

        ServerPortManager serverPortManager;
        try {
            serverPortManager = new ServerPortManager();
            logger.info("ServerPortManager创建成功");
        } catch (Exception e) {
            logger.error("创建ServerPortManager失败: " + e.getMessage(), e);
            throw new RuntimeException("ShellService初始化失败: ServerPortManager创建失败", e);
        }

        CommandExecutor commandExecutor;
        try {
            commandExecutor = new CommandExecutor();
            logger.info("CommandExecutor创建成功");
        } catch (Exception e) {
            logger.error("创建CommandExecutor失败: " + e.getMessage(), e);
            throw new RuntimeException("ShellService初始化失败: CommandExecutor创建失败", e);
        }

        SocketClientHandler clientHandler;
        try {
            clientHandler = new SocketClientHandler(commandExecutor);
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

        CommandHandler commandHandler;
        try {
            commandHandler = new CommandHandler(commandExecutor);
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
        
        ThreadPoolManager.submitFastRunnable(() -> {
            try {
                logger.info("ShellService构造完成，等待启动Socket服务器");
                socketServer.start();
                logger.info("Socket服务器启动成功");
            } catch (Exception e) {
                logger.error("启动Socket服务器失败: " + e.getMessage(), e);
            }
        });
        
        ThreadPoolManager.submitFastRunnable(() -> {
            try {
                logger.info("开始复制Hook示例代码到scripts目录");
                copyHookExamplesToScripts();
                logger.info("Hook示例代码复制完成");
            } catch (Exception e) {
                logger.error("复制Hook示例代码失败: " + e.getMessage(), e);
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

    private void copyHookExamplesToScripts() {
        String modulePath = DataBridge.getModulePath();
        if (modulePath == null) {
            logger.info("模块路径未初始化，跳过复制Hook示例代码");
            return;
        }
        
        File scriptsDir = DataBridge.getScriptsDirectory();
        IOManager.createDirectory(scriptsDir.getAbsolutePath());
        
        File moduleApk = new File(modulePath);
        if (!moduleApk.exists()) {
            logger.info("模块APK文件不存在: " + modulePath);
            return;
        }
        
        try (ZipFile zipFile = new ZipFile(moduleApk)) {
            Enumeration<? extends ZipEntry> entries = zipFile.entries();
            
            int successCount = 0;
            int skipCount = 0;
            int failCount = 0;
            
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                String entryName = entry.getName();
                
                if (!entryName.startsWith("assets/codebase/") || entryName.endsWith("/")) {
                    continue;
                }
                
                if (entryName.equals("assets/codebase/README.md")) {
                    continue;
                }
                
                String fileName = entryName.substring("assets/codebase/".length());
                // 脚本相关的命令统一按「名字 + .java」去脚本目录里找文件，所以落盘时就得
                // 补上后缀，否则这些示例脚本（hook add ... codebase <名字>）谁都读不到。
                // 本身带扩展名的（目录里那两个同步脚本）保持原样。
                String targetName = fileName.indexOf('.') < 0
                        ? fileName + FileDirectory.SCRIPT_SUFFIX
                        : fileName;
                File targetFile = new File(scriptsDir, targetName);
                
                if (targetFile.exists()) {
                    logger.debug("文件已存在，跳过复制: " + targetName);
                    skipCount++;
                    continue;
                }
                
                try (InputStream inputStream = zipFile.getInputStream(entry)) {
                    byte[] content = readAllBytesCompat(inputStream);
                    IOManager.writeFile(targetFile.getAbsolutePath(), content);
                    
                    logger.debug("成功复制Hook示例文件: " + targetName);
                    successCount++;
                } catch (Exception e) {
                    logger.error("复制Hook示例文件失败: " + targetName, e);
                    failCount++;
                }
            }
            
            logger.info("Hook示例代码复制完成 - 成功: " + successCount + 
                        ", 跳过: " + skipCount + ", 失败: " + failCount);
        } catch (Exception e) {
            logger.error("复制Hook示例代码时发生异常", e);
        }
    }

    private byte[] readAllBytesCompat(InputStream inputStream) throws IOException {
        byte[] buffer = new byte[8192];
        int bytesRead;
        java.io.ByteArrayOutputStream outputStream = new java.io.ByteArrayOutputStream();
        while ((bytesRead = inputStream.read(buffer)) != -1) {
            outputStream.write(buffer, 0, bytesRead);
        }
        return outputStream.toByteArray();
    }

}
