package com.justnothing.methodsclient.executor;

import static com.justnothing.testmodule.hooks.tests.ShellServiceHook.SERVICE_NAME;
import static com.justnothing.testmodule.service.handler.TransactionHandler.TRANSACTION_EXECUTE_FILE;
import static com.justnothing.testmodule.service.handler.TransactionHandler.TRANSACTION_WRITE_HOOK_DATA;

import com.justnothing.methodsclient.StreamClient;
import com.justnothing.methodsclient.monitor.PerformanceMonitor;
import com.justnothing.testmodule.constants.AppEnvironment;
import com.justnothing.testmodule.constants.FileDirectory;
import com.justnothing.testmodule.hooks.HookEntry;
import com.justnothing.testmodule.utils.io.IOManager;
import com.justnothing.testmodule.utils.io.RootProcessPool;

import android.os.Process;

import java.io.File;
import java.io.IOException;

/**
 * 文件模式命令执行器。
 * <p>
 * 通过将命令写入临时文件，然后调用服务端执行，最后读取输出文件来获得结果。
 * 支持 app_process 环境和普通应用环境，自动处理目录和权限。
 * </p>
 */
public class FileCommandExecutor {

    private static final StreamClient.ClientLogger logger = new StreamClient.ClientLogger();
    public static volatile boolean isInAppProcess = false;


    public record ExecutionResult(boolean success, String output, String error) {}

    private record ExecutionContext(String tmpDir, String inputFile, String outputFile) {}


    /**
     * 准备执行环境：创建临时目录、写入命令文件、设置权限。
     *
     * @param command 要执行的命令
     * @return 执行上下文（包含临时目录路径、输入文件、输出文件），若失败则返回 null
     */
    private static ExecutionContext prepareExecution(String command) throws IOException, InterruptedException {
        String baseDataDir = isInAppProcess ? FileDirectory.METHODS_DATA_DIR : FileDirectory.SDCARD_PATH;
        String sessionDir = baseDataDir + "/" + FileDirectory.EXECUTE_SESSIONS_DIR_NAME;
        String tmpDir = sessionDir + "/" + FileDirectory.SESSION_PREFIX + System.nanoTime() + "_" + Process.myPid();
        String inputFile = tmpDir + "/" + FileDirectory.INPUT_FILE_NAME;
        String outputFile = tmpDir + "/" + FileDirectory.OUTPUT_FILE_NAME;

        // 创建会话目录和临时目录
        if (isInAppProcess) {
            IOManager.ProcessResult mkdirResult = RootProcessPool.executeCommand("mkdir -p " + sessionDir, 5000, true);
            if (!mkdirResult.isSuccess()) {
                logger.error("创建会话目录失败: " + mkdirResult.stdout());
                return null;
            }
            IOManager.ProcessResult mkdirTmpResult = RootProcessPool.executeCommand("mkdir -p " + tmpDir, 5000, true);
            if (!mkdirTmpResult.isSuccess()) {
                logger.error("创建临时目录失败: " + mkdirTmpResult.stdout());
                cleanupTempDir(tmpDir);
                return null;
            }
        } else {
            File sessionDirFile = new File(sessionDir);
            if (!sessionDirFile.exists() && !IOManager.createDirectory(sessionDirFile)) {
                logger.error("创建会话目录失败: " + sessionDir);
                return null;
            }
            File tmpDirFile = new File(tmpDir);
            if (!tmpDirFile.exists() && !IOManager.createDirectory(tmpDirFile)) {
                logger.error("创建临时目录失败: " + tmpDir);
                cleanupTempDir(tmpDir);
                return null;
            }
            // 设置临时目录权限为 777
            IOManager.ProcessResult chmodResult = RootProcessPool.executeCommand("chmod 777 " + tmpDir, 5000, true);
            if (!chmodResult.isSuccess()) {
                logger.warn("设置临时目录权限失败: " + tmpDir);
            }
        }

        // 写入输入文件
        IOManager.writeFile(inputFile, command);
        File input = new File(inputFile);
        if (!input.exists() || input.length() == 0) {
            logger.error("无法创建输入文件");
            cleanupTempDir(tmpDir);
            return null;
        }

        // 设置输入文件权限（仅在非 app_process 环境需要）
        if (!isInAppProcess) {
            IOManager.ProcessResult chmodResult = RootProcessPool.executeCommand("chmod 644 " + inputFile, 5000, true);
            if (!chmodResult.isSuccess()) {
                logger.warn("设置输入文件权限失败: " + inputFile);
            }
        }

        logger.info("文件模式执行，命令长度: " + command.length());
        logger.debug("输入文件: " + inputFile);
        return new ExecutionContext(tmpDir, inputFile, outputFile);
    }

    /**
     * 调用服务端执行命令，并等待输出文件生成，返回输出内容。
     *
     * @param inputFile  输入文件路径
     * @param outputFile 输出文件路径
     * @return 输出文件的内容，如果失败则返回 null
     */
    private static String callServiceAndGetOutput(String inputFile, String outputFile) throws IOException, InterruptedException {
        String[] serviceCmd = new String[]{
                "service", "call", "justnothing_xposed_method_cli",
                String.valueOf(TRANSACTION_EXECUTE_FILE), "s16", "FILE:" + inputFile + ":" + outputFile
        };
        String serviceCommand = String.join(" ", serviceCmd);
        IOManager.ProcessResult serviceResult = RootProcessPool.executeCommand(serviceCommand, 60000, false);
        if (!serviceResult.isSuccess()) {
            logger.error("服务调用失败，退出码: " + serviceResult.exitCode() + ", 输出: " + serviceResult.stdout());
            return null;
        }

        // 轮询等待输出文件（最多 30 次，每次 500ms）
        int maxWait = 30;
        int waited = 0;
        while (waited < maxWait) {
            File out = new File(outputFile);
            if (out.exists() && out.length() > 0) {
                break;
            }
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                logger.warn("等待输出文件被中断");
                return null;
            }
            waited++;
        }

        File out = new File(outputFile);
        if (!out.exists() || out.length() == 0) {
            logger.error("输出文件未生成或为空");
            return null;
        }

        return IOManager.readFile(outputFile);
    }

    // ==================== 公共 API ====================

    /**
     * 执行命令，并返回包含输出内容的执行结果。
     *
     * @param command 要执行的命令
     * @return ExecutionResult 包含执行状态和输出内容
     */
    @SuppressWarnings("unused")
    public static ExecutionResult executeFileWithOutput(String command) {
        long startTime = System.currentTimeMillis();
        ExecutionContext ctx = null;
        boolean success = false;
        String output = "";
        String error = "";

        try {
            ctx = prepareExecution(command);
            if (ctx == null) {
                error = "准备执行环境失败";
                return new ExecutionResult(false, "", error);
            }

            String outputContent = callServiceAndGetOutput(ctx.inputFile, ctx.outputFile);
            success = outputContent != null;
            if (success) {
                output = outputContent;
                System.out.print(output);
            } else {
                error = "执行失败（服务调用或输出文件读取失败）";
            }
            return new ExecutionResult(success, output, error);
        } catch (IOException | InterruptedException e) {
            logger.error("文件模式命令执行异常", e);
            error = "执行异常: " + e.getMessage();
            return new ExecutionResult(false, "", error);
        } finally {
            if (ctx != null) {
                cleanupTempDir(ctx.tmpDir);
            }
            long duration = System.currentTimeMillis() - startTime;
            PerformanceMonitor.recordFileCommand(duration, success);
        }
    }
    /**
     * 执行命令，输出直接打印到控制台，只返回是否成功。
     *
     * @param command 要执行的命令
     * @return true 表示成功，false 表示失败
     */
    public static boolean executeFile(String command) {
        long startTime = System.currentTimeMillis();
        String outputContent = null;
        ExecutionContext ctx = null;
        try {
            ctx = prepareExecution(command);
            if (ctx == null) return false;
            outputContent = callServiceAndGetOutput(ctx.inputFile, ctx.outputFile);
        } catch (InterruptedException | IOException e) {
            logger.error("文件模式命令执行失败", e);
        }

        boolean success = outputContent != null;
        if (success) System.out.print(outputContent);
        else System.err.println("执行失败");

        if (ctx != null) cleanupTempDir(ctx.tmpDir);

        long duration = System.currentTimeMillis() - startTime;
        PerformanceMonitor.recordFileCommand(duration, success);
        return success;
    }

    /**
     * 清理临时目录（递归删除）。
     *
     * @param dir 临时目录路径
     */
    public static void cleanupTempDir(String dir) {
        if (dir == null || dir.isEmpty()) {
            return;
        }

        if (com.justnothing.testmodule.utils.data.BootMonitor.isZygotePhase()) {
            return;
        }

        try {
            Thread.sleep(200);
            IOManager.ProcessResult result = RootProcessPool.executeCommand("rm -rf " + dir, 5000, false);
            if (result.isSuccess()) {
                logger.debug("清理临时目录: " + dir);
            } else {
                logger.warn("清理临时目录失败: " + result.stderr());
            }
        } catch (Exception e) {
            logger.warn("清理临时目录失败: " + e.getMessage());
        }
    }

    /**
     * 写入 Hook 数据（用于 Xposed 环境）。
     *
     * @param fixPermissions 是否修复权限
     * @return true 表示成功，false 表示失败
     */
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
            IOManager.ProcessResult serviceResult = RootProcessPool.executeCommand(serviceCommand, 15000, false);

            if (serviceResult.isSuccess()) {
                logger.info("写入Hook数据请求成功, 修复权限: " + fixPermissions + ", 输出: " + serviceResult.stdout());
                return true;
            } else {
                String errorMsg = "写入Hook数据服务调用失败，退出码: " + serviceResult.exitCode() + ", 修复权限: " + fixPermissions + ", 输出: " + serviceResult.stdout();
                logger.error(errorMsg);
                return false;
            }

        } catch (Exception e) {
            String errorMsg = "写入Hook数据执行失败: " + e.getMessage() + ", 修复权限: " + fixPermissions;
            logger.error(errorMsg, e);
            return false;
        }
    }

    /**
     * 修改文件或目录权限（如果权限已经满足则跳过）。
     *
     * @param targetPath  目标路径
     * @param permissions 权限字符串（八进制，如 "755"）
     * @param recursive   是否递归修改目录
     * @return true 表示成功，false 表示失败
     */
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

    /**
     * 检查文件或目录权限是否已经符合要求。
     */
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
            if (canRead) actualPerms |= 256;   // 400 octal
            if (canWrite) actualPerms |= 128;  // 200 octal
            if (canExecute) actualPerms |= 64; // 100 octal

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

}