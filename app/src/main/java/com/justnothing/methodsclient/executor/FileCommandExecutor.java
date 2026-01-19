package com.justnothing.methodsclient.executor;


import static com.justnothing.testmodule.hooks.tests.ShellServiceHook.SERVICE_NAME;
import static com.justnothing.testmodule.service.handler.TransactionHandler.TRANSACTION_EXECUTE_FILE;
import static com.justnothing.testmodule.service.handler.TransactionHandler.TRANSACTION_WRITE_HOOK_DATA;

import java.lang.Process;

import com.justnothing.methodsclient.StreamClient;
import com.justnothing.methodsclient.monitor.PerformanceMonitor;
import com.justnothing.testmodule.constants.AppEnvironment;
import com.justnothing.testmodule.constants.FileDirectory;
import com.justnothing.testmodule.hooks.HookEntry;
import com.justnothing.testmodule.utils.functions.CmdUtils;
import com.justnothing.testmodule.utils.io.IOManager;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

public class FileCommandExecutor {

    private static final int BUFFER_SIZE = 8192;

    private static final StreamClient.ClientLogger logger = new StreamClient.ClientLogger();
    
    public static volatile boolean isInAppProcess = false;

    public record ExecutionResult(boolean success, String output, String error) {
    }

    public static ExecutionResult executeFileWithOutput(String command) {
        long startTime = System.currentTimeMillis();
        boolean success = false;
        StringBuilder output = new StringBuilder();
        StringBuilder error = new StringBuilder();

        try {
            // 根据运行环境选择不同的数据目录
            String baseDataDir;
            if (isInAppProcess) {
                // app_process环境：使用/data/local/tmp目录
                baseDataDir = FileDirectory.METHODS_DATA_DIR;
            } else {
                // 应用环境：使用sdcard目录
                baseDataDir = FileDirectory.SDCARD_PATH;
            }
            
            String sessionDir = baseDataDir + "/" + FileDirectory.EXECUTE_SESSIONS_DIR_NAME;
            String tmpDir = sessionDir + "/" + FileDirectory.SESSION_PREFIX + System.nanoTime() + "_" + android.os.Process.myPid();
            String inputFile = tmpDir + "/" + FileDirectory.INPUT_FILE_NAME;
            String outputFile = tmpDir + "/" + FileDirectory.OUTPUT_FILE_NAME;

            if (isInAppProcess) {
                CmdUtils.CommandOutput mkdirResult = CmdUtils.runRootCommand("mkdir -p " + sessionDir, 5000);
                if (!mkdirResult.succeed()) {
                    error.append("创建会话目录失败: ").append(mkdirResult.stdout());
                    logger.error("创建会话目录失败: " + mkdirResult.stdout());
                    return new ExecutionResult(false, "", error.toString());
                }

                CmdUtils.CommandOutput mkdirTmpResult = CmdUtils.runRootCommand("mkdir -p " + tmpDir, 5000);
                if (!mkdirTmpResult.succeed()) {
                    error.append("创建临时目录失败: ").append(mkdirTmpResult.stdout());
                    logger.error("创建临时目录失败: " + mkdirTmpResult.stdout());
                    return new ExecutionResult(false, "", error.toString());
                }
            } else {
                // 应用环境：使用普通文件操作创建目录
                File sessionDirFile = new File(sessionDir);
                if (!sessionDirFile.exists() && !sessionDirFile.mkdirs()) {
                    error.append("创建会话目录失败: ").append(sessionDir);
                    logger.error("创建会话目录失败: " + sessionDir);
                    return new ExecutionResult(false, "", error.toString());
                }

                File tmpDirFile = new File(tmpDir);
                if (!tmpDirFile.exists() && !tmpDirFile.mkdirs()) {
                    error.append("创建临时目录失败: ").append(tmpDir);
                    logger.error("创建临时目录失败: " + tmpDir);
                    return new ExecutionResult(false, "", error.toString());
                }
                CmdUtils.CommandOutput chmodResult = CmdUtils.runRootCommand("chmod 777 " + tmpDir, 5000);
                if (!chmodResult.succeed()) {
                    logger.warn("设置临时目录权限失败: " + tmpDir);
                } else {
                    logger.debug("临时目录权限设置成功");
                }
            }

            File input = new File(inputFile);
            if (!Objects.requireNonNull(input.getParentFile()).exists()) {
                error.append("无法创建临时目录");
                cleanupTempDir(tmpDir);
                return new ExecutionResult(false, "", error.toString());
            }

            IOManager.writeFile(inputFile, command);

            if (!input.exists() || input.length() == 0) {
                error.append("无法创建输入文件");
                cleanupTempDir(tmpDir);
                return new ExecutionResult(false, "", error.toString());
            }

            // 设置输入文件权限为可读，确保服务端可以访问
            if (!isInAppProcess) {
                // 应用环境：需要设置文件权限
                CmdUtils.CommandOutput chmodResult = CmdUtils.runRootCommand("chmod 644 " + inputFile, 5000);
                if (!chmodResult.succeed()) {
                    logger.warn("设置输入文件权限失败: " + inputFile);
                } else {
                    logger.debug("输入文件权限设置成功");
                }
            }

            logger.info("文件模式执行，命令长度: " + command.length());
            logger.debug("输入文件: " + inputFile);

            String[] serviceCmd = new String[]{"service", "call", "justnothing_xposed_method_cli",
                    String.valueOf(TRANSACTION_EXECUTE_FILE), "s16", "FILE:" + inputFile + ":" + outputFile};

            // 使用CmdUtils执行服务调用
            String serviceCommand = String.join(" ", serviceCmd);
            CmdUtils.CommandOutput serviceResult = CmdUtils.runCommand(serviceCommand, 60000);
            
            if (serviceResult.succeed()) {
                int maxWait = 30;
                int waited = 0;
                boolean fileFound = false;

                while (waited < maxWait) {
                    File out = new File(outputFile);
                    if (out.exists() && out.length() > 0) {
                        fileFound = true;
                        break;
                    }
                    Thread.sleep(500);
                    waited++;
                }

                if (fileFound) {
                    String fileContent = IOManager.readFile(outputFile);
                    if (fileContent != null) {
                        output.append(fileContent);
                        System.out.print(fileContent);
                        long totalChars = fileContent.length();
                        success = true;
                        logger.info("文件模式执行成功，输出字符数: " + totalChars);
                    } else {
                        error.append("读取输出文件失败");
                        logger.error("读取输出文件失败");
                    }
                } else {
                    error.append("文件模式执行超时");
                    logger.error("文件模式执行超时");
                }
            } else {
                error.append("服务调用失败，退出码: ").append(serviceResult.stat());
                logger.error("服务调用失败，退出码: " + serviceResult.stat() + ", 输出: " + serviceResult.stdout());
            }

            cleanupTempDir(tmpDir);

        } catch (Exception e) {
            error.append("执行出错: ").append(e.getMessage());
            logger.error("执行出错", e);
        }

        long duration = System.currentTimeMillis() - startTime;
        PerformanceMonitor.recordFileCommand(duration, success);
        return new ExecutionResult(success, output.toString(), error.toString());
    }

    public static boolean executeFile(String command) {
        long startTime = System.currentTimeMillis();
        boolean success = false;

        try {
            String baseDataDir;
            if (isInAppProcess) {
                baseDataDir = FileDirectory.METHODS_DATA_DIR;
            } else {
                baseDataDir = FileDirectory.SDCARD_PATH;
            }
            String sessionDir = baseDataDir + "/" + FileDirectory.EXECUTE_SESSIONS_DIR_NAME;
            String tmpDir = sessionDir + "/" + FileDirectory.SESSION_PREFIX + System.nanoTime() + "_" + android.os.Process.myPid();
            String inputFile = tmpDir + "/" + FileDirectory.INPUT_FILE_NAME;
            String outputFile = tmpDir + "/" + FileDirectory.OUTPUT_FILE_NAME;

            if (isInAppProcess) {
                CmdUtils.CommandOutput mkdirResult = CmdUtils.runRootCommand("mkdir -p " + sessionDir, 5000);
                if (!mkdirResult.succeed()) {
                    System.err.println("创建会话目录失败: " + mkdirResult.stdout());
                    logger.error("创建会话目录失败: " + mkdirResult.stdout());
                    cleanupTempDir(tmpDir);
                    return false;
                }

                CmdUtils.CommandOutput mkdirTmpResult = CmdUtils.runRootCommand("mkdir -p " + tmpDir, 5000);
                if (!mkdirTmpResult.succeed()) {
                    System.err.println("创建临时目录失败: " + mkdirTmpResult.stdout());
                    logger.error("创建临时目录失败: " + mkdirTmpResult.stdout());
                    cleanupTempDir(tmpDir);
                    return false;
                }
            } else {
                File sessionDirFile = new File(sessionDir);
                if (!sessionDirFile.exists() && !sessionDirFile.mkdirs()) {
                    System.err.println("创建会话目录失败: " + sessionDir);
                    logger.error("创建会话目录失败: " + sessionDir);
                    cleanupTempDir(tmpDir);
                    return false;
                }

                File tmpDirFile = new File(tmpDir);
                if (!tmpDirFile.exists() && !tmpDirFile.mkdirs()) {
                    System.err.println("创建临时目录失败: " + tmpDir);
                    logger.error("创建临时目录失败: " + tmpDir);
                    cleanupTempDir(tmpDir);
                    return false;
                }

                CmdUtils.CommandOutput chmodResult = CmdUtils.runRootCommand("chmod 777 " + tmpDir, 5000);
                if (!chmodResult.succeed()) {
                    logger.warn("设置临时目录权限失败: " + tmpDir);
                } else {
                    logger.debug("临时目录权限设置成功");
                }
            }

            File input = new File(inputFile);
            if (!Objects.requireNonNull(input.getParentFile()).exists()) {
                System.err.println("无法创建临时目录");
                cleanupTempDir(tmpDir);
                return false;
            }

            IOManager.writeFile(inputFile, command);

            if (!input.exists() || input.length() == 0) {
                System.err.println("无法创建输入文件");
                cleanupTempDir(tmpDir);
                return false;
            }

            CmdUtils.CommandOutput chmodResult = CmdUtils.runRootCommand("chmod 644 " + inputFile, 5000);
            if (!chmodResult.succeed()) {
                logger.warn("设置输入文件权限失败: " + inputFile);
            } else {
                logger.debug("输入文件权限设置成功");
            }

            logger.info("文件模式执行，命令长度: " + command.length());
            logger.debug("输入文件: " + inputFile);

            String[] serviceCmd = new String[]{"service", "call", "justnothing_xposed_method_cli",
                    String.valueOf(TRANSACTION_EXECUTE_FILE), "s16", "FILE:" + inputFile + ":" + outputFile};

            String serviceCommand = String.join(" ", serviceCmd);
            CmdUtils.CommandOutput serviceResult = CmdUtils.runCommand(serviceCommand, 60000);
            
            if (serviceResult.succeed()) {
                int maxWait = 30;
                int waited = 0;
                boolean fileFound = false;

                while (waited < maxWait) {
                    File output = new File(outputFile);
                    if (output.exists() && output.length() > 0) {
                        fileFound = true;
                        break;
                    }
                    Thread.sleep(500);
                    waited++;
                }

                if (fileFound) {
                    String fileContent = IOManager.readFile(outputFile);
                    if (fileContent != null) {
                        System.out.print(fileContent);
                        long totalChars = fileContent.length();
                        success = true;
                        logger.info("文件模式执行成功，输出字符数: " + totalChars);
                    } else {
                        System.err.println("读取输出文件失败");
                        logger.error("读取输出文件失败");
                    }
                } else {
                    System.err.println("文件模式执行超时");
                    logger.error("文件模式执行超时");
                }
            } else {
                String t = "服务调用失败，退出码: " + serviceResult.stat() + ", 输出: " + serviceResult.stdout();
                System.err.println(t);
                logger.error(t);
            }

            cleanupTempDir(tmpDir);

        } catch (Exception e) {
            System.err.println("执行出错: " + e.getMessage());
            logger.error("执行出错", e);
        }

        long duration = System.currentTimeMillis() - startTime;
        PerformanceMonitor.recordFileCommand(duration, success);
        return success;
    }

    public static void cleanupTempDir(String dir) {
        if (dir == null || dir.isEmpty()) {
            return;
        }

        if (com.justnothing.testmodule.utils.data.BootMonitor.isZygotePhase()) {
            return;
        }

        try {
            Thread.sleep(200);

            String[] cmd = {"rm", "-rf", dir};
            Process process = Runtime.getRuntime().exec(cmd);

            long timeoutMs = 5000;
            Thread waiter = new Thread(() -> {
                try {
                    process.waitFor();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
            waiter.start();
            waiter.join(timeoutMs);
            
            if (waiter.isAlive()) {
                waiter.interrupt();
                process.destroyForcibly();
                logger.warn("清理临时目录超时 (" + timeoutMs + "ms): " + dir);
            } else {
                logger.debug("清理临时目录: " + dir);
            }
        } catch (Exception e) {
            logger.warn("清理临时目录失败: " + e.getMessage());
        }
    }

    public static boolean writeHookData(boolean fixPermissions) {
        if (AppEnvironment.isHookEnv()) {
            try {
                logger.info("在Hook环境中直接调用HookEntry.executeFileOperations()");
                HookEntry.executeFileOperations();
                logger.info("Hook数据写入完成");
                return true;
            } catch (Exception e) {
                logger.error("Hook数据写入失败", e);
                return false;
            }
        }

        try {
            String[] serviceCmd = new String[]{"service", "call", SERVICE_NAME,
                    String.valueOf(TRANSACTION_WRITE_HOOK_DATA),
                    "i32", String.valueOf(fixPermissions ? 1 : 0)};

            String serviceCommand = String.join(" ", serviceCmd);
            CmdUtils.CommandOutput serviceResult = CmdUtils.runCommand(serviceCommand, 15000);
            
            if (serviceResult.succeed()) {
                logger.info("写入Hook数据请求成功, 修复权限: " + fixPermissions + ", 输出: " + serviceResult.stdout());
                return true;
            } else {
                String errorMsg = "写入Hook数据服务调用失败，退出码: " + serviceResult.stat() + ", 修复权限: " + fixPermissions + ", 输出: " + serviceResult.stdout();
                logger.error(errorMsg);
                return false;
            }

        } catch (Exception e) {
            String errorMsg = "写入Hook数据执行失败: " + e.getMessage() + ", 修复权限: " + fixPermissions;
            logger.error(errorMsg, e);
            return false;
        }
    }

    public static boolean chmodFile(String targetPath, String permissions, boolean recursive) {
        if (targetPath == null || targetPath.isEmpty()) {
            logger.error("chmod目标路径不能为空");
            System.err.println("错误: chmod目标路径不能为空");
            return false;
        }
        if (permissions == null || permissions.isEmpty()) {
            logger.error("chmod权限参数不能为空");
            System.err.println("错误: chmod权限参数不能为空");
            return false;
        }
        File targetFile = new File(targetPath);
        if (!targetFile.exists()) {
            logger.warn("chmod目标不存在: " + targetPath);
            return false;
        }
        if (checkPermissions(targetFile, permissions, recursive)) {
            logger.debug("权限已满足，跳过chmod: " + targetPath + " (" + permissions + ")");
            return true;
        }
        return AsyncChmodExecutor.chmodFile(targetPath, permissions, recursive);
    }

    private static boolean checkPermissions(File file, String desiredPermissions, boolean recursive) {
        try {
            if (recursive && file.isDirectory()) {
                return checkDirectoryPermissions(file, desiredPermissions);
            } else {
                return checkFilePermissions(file, desiredPermissions);
            }
        } catch (Exception e) {
            logger.debug("权限检查失败: " + e.getMessage());
            return false;
        }
    }

    private static boolean checkFilePermissions(File file, String desiredPermissions) {
        if (!file.exists()) {
            return false;
        }

        try {
            int desiredPerms = parsePermissions(desiredPermissions);
            if (desiredPerms == -1) {
                return false;
            }

            boolean canRead = file.canRead();
            boolean canWrite = file.canWrite();
            boolean canExecute = file.canExecute();

            int actualPerms = 0;
            if (canRead) actualPerms |= 256;
            if (canWrite) actualPerms |= 128;
            if (canExecute) actualPerms |= 64;

            return (actualPerms & desiredPerms) == desiredPerms;
        } catch (Exception e) {
            logger.debug("检查文件权限失败: " + e.getMessage());
            return false;
        }
    }

    private static boolean checkDirectoryPermissions(File directory, String desiredPermissions) {
        if (!directory.exists() || !directory.isDirectory()) {
            return false;
        }

        try {
            int desiredPerms = parsePermissions(desiredPermissions);
            if (desiredPerms == -1) {
                return false;
            }

            boolean canRead = directory.canRead();
            boolean canWrite = directory.canWrite();
            boolean canExecute = directory.canExecute();

            int actualPerms = 0;
            if (canRead) actualPerms |= 256;
            if (canWrite) actualPerms |= 128;
            if (canExecute) actualPerms |= 64;

            return (actualPerms & desiredPerms) == desiredPerms;
        } catch (Exception e) {
            logger.debug("检查目录权限失败: " + e.getMessage());
            return false;
        }
    }

    private static int parsePermissions(String permissions) {

        try {
            int perms = Integer.parseInt(permissions, 8);
            if (perms >= 0 && perms <= 511) {
                return perms;
            }
            return -1;
        } catch (NumberFormatException e) {
            logger.debug("解析权限字符串失败: " + permissions);
            return -1;
        }
    }


    public static String readOutputFile(String filePath) {
        try {
            return IOManager.readFile(filePath);
        } catch (Exception e) {
            logger.warn("读取输出文件失败: " + e.getMessage());
            return "";
        }
    }
}
