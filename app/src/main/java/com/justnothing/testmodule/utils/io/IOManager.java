package com.justnothing.testmodule.utils.io;

import com.justnothing.testmodule.utils.concurrent.ThreadPoolManager;
import com.justnothing.testmodule.utils.data.BootMonitor;
import com.justnothing.testmodule.utils.functions.Logger;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

public class IOManager extends Logger {
    private static final String TAG = "IOManager";
    private static final Logger logger = new Logger() {
        @Override
        public String getTag() {
            return TAG;
        }
    };

    private static final IOManager instance = new IOManager();

    private static final int DEFAULT_BUFFER_SIZE = 8192;
    private static final int LARGE_BUFFER_SIZE = 65536;
    private static final int MAX_RETRIES = 3;
    private static final long RETRY_DELAY_MS = 100;

    private static final long WRITE_BATCH_SIZE = 64 * 1024;
    private static final long WRITE_BATCH_TIMEOUT_MS = 100;

    private final AtomicLong totalBytesRead = new AtomicLong(0);
    private final AtomicLong totalBytesWritten = new AtomicLong(0);
    private final AtomicLong totalReadTime = new AtomicLong(0);
    private final AtomicLong totalWriteTime = new AtomicLong(0);

    private volatile boolean initialized = false;

    private IOManager() {
        super();
        initialized = true;
        info("IOManager初始化完成");
    }

    @Override
    public String getTag() {
        return TAG;
    }

    public static IOManager getInstance() {
        return instance;
    }

    public static void initialize() {
        if (!instance.initialized) {
            logger.warn("IOManager未初始化");
        }
    }

    public static Future<String> readFileAsync(String filePath) {
        return readFileAsync(filePath, StandardCharsets.UTF_8);
    }

    public static Future<String> readFileAsync(String filePath, java.nio.charset.Charset charset) {
        return ThreadPoolManager.submitIOCallable(() -> readFile(filePath, charset));
    }

    public static String readFile(String filePath) throws IOException {
        return readFile(filePath, StandardCharsets.UTF_8);
    }

    public static String readFile(String filePath, java.nio.charset.Charset charset) throws IOException {
        if (BootMonitor.isZygotePhase()) {
            return null;
        }

        long startTime = System.currentTimeMillis();
        Path path = Paths.get(filePath);

        if (!Files.exists(path)) {
            throw new FileNotFoundException("文件不存在: " + filePath);
        }

        byte[] bytes = Files.readAllBytes(path);
        String content = new String(bytes, charset);

        long duration = System.currentTimeMillis() - startTime;
        instance.totalBytesRead.addAndGet(bytes.length);
        instance.totalReadTime.addAndGet(duration);

        if (duration > 1000) {
            instance.warn("读取文件耗时过长: " + filePath + " (" + duration + "ms, " + bytes.length + " bytes)");
        }

        return content;
    }

    public static Future<Void> writeFileAsync(String filePath, String content) {
        return writeFileAsync(filePath, content, StandardCharsets.UTF_8, false);
    }

    public static Future<Void> writeFileAsync(String filePath, String content, boolean append) {
        return writeFileAsync(filePath, content, StandardCharsets.UTF_8, append);
    }

    public static Future<Void> writeFileAsync(String filePath, String content, java.nio.charset.Charset charset, boolean append) {
        return ThreadPoolManager.submitIOCallable(() -> {
            writeFile(filePath, content, charset, append);
            return null;
        });
    }

    public static void writeFile(String filePath, String content) throws IOException {
        writeFile(filePath, content, StandardCharsets.UTF_8, false);
    }

    public static void writeFile(String filePath, String content, boolean append) throws IOException {
        writeFile(filePath, content, StandardCharsets.UTF_8, append);
    }

    public static void writeFile(String filePath, String content, java.nio.charset.Charset charset, boolean append) throws IOException {
        if (BootMonitor.isZygotePhase()) {
            return;
        }

        long startTime = System.currentTimeMillis();
        byte[] bytes = content.getBytes(charset);
        Path path = Paths.get(filePath);

        ensureParentDirectoryExists(path.getParent());

        if (append) {
            Files.write(path, bytes, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } else {
            Files.write(path, bytes, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        }

        long duration = System.currentTimeMillis() - startTime;
        instance.totalBytesWritten.addAndGet(bytes.length);
        instance.totalWriteTime.addAndGet(duration);

        if (duration > 1000) {
            instance.warn("写入文件耗时过长: " + filePath + " (" + duration + "ms, " + bytes.length + " bytes)");
        }
    }

    public static Future<Void> appendFileAsync(String filePath, String content) {
        return appendFileAsync(filePath, content, StandardCharsets.UTF_8);
    }

    public static Future<Void> appendFileAsync(String filePath, String content, java.nio.charset.Charset charset) {
        return ThreadPoolManager.submitIOCallable(() -> {
            appendFile(filePath, content, charset);
            return null;
        });
    }

    public static void appendFile(String filePath, String content) throws IOException {
        appendFile(filePath, content, StandardCharsets.UTF_8);
    }

    public static void appendFile(String filePath, String content, java.nio.charset.Charset charset) throws IOException {
        writeFile(filePath, content, charset, true);
    }

    public static Future<Boolean> deleteFileAsync(String filePath) {
        return ThreadPoolManager.submitIOCallable(() -> deleteFile(filePath));
    }

    public static boolean deleteFile(String filePath) {
        if (BootMonitor.isZygotePhase()) {
            return false;
        }

        try {
            Path path = Paths.get(filePath);
            return Files.deleteIfExists(path);
        } catch (IOException e) {
            instance.error("删除文件失败: " + filePath, e);
            return false;
        }
    }

    public static Future<Boolean> fileExistsAsync(String filePath) {
        return ThreadPoolManager.submitIOCallable(() -> fileExists(filePath));
    }

    public static boolean fileExists(String filePath) {
        try {
            Path path = Paths.get(filePath);
            return Files.exists(path);
        } catch (Exception e) {
            instance.error("检查文件存在失败: " + filePath, e);
            return false;
        }
    }

    public static Future<Long> getFileSizeAsync(String filePath) {
        return ThreadPoolManager.submitIOCallable(() -> getFileSize(filePath));
    }

    public static long getFileSize(String filePath) throws IOException {
        Path path = Paths.get(filePath);
        if (!Files.exists(path)) {
            throw new FileNotFoundException("文件不存在: " + filePath);
        }
        return Files.size(path);
    }

    public static Future<Void> truncateFileAsync(String filePath, long size) {
        return ThreadPoolManager.submitIOCallable(() -> {
            truncateFile(filePath, size);
            return null;
        });
    }

    public static void truncateFile(String filePath, long size) throws IOException {
        if (BootMonitor.isZygotePhase()) {
            throw new IOException("Zygote阶段，跳过文件截断");
        }

        long startTime = System.currentTimeMillis();
        Path path = Paths.get(filePath);

        try (FileChannel channel = FileChannel.open(path,
                StandardOpenOption.WRITE, StandardOpenOption.READ)) {
            channel.truncate(size);
        }

        long duration = System.currentTimeMillis() - startTime;
        if (duration > 500) {
            instance.warn("截断文件耗时: " + filePath + " (" + duration + "ms)");
        }
    }

    public static Future<Void> copyFileAsync(String sourcePath, String targetPath) {
        return ThreadPoolManager.submitIOCallable(() -> {
            copyFile(sourcePath, targetPath);
            return null;
        });
    }

    public static void copyFile(String sourcePath, String targetPath) throws IOException {
        if (BootMonitor.isZygotePhase()) {
            throw new IOException("Zygote阶段，跳过文件复制");
        }

        long startTime = System.currentTimeMillis();
        Path source = Paths.get(sourcePath);
        Path target = Paths.get(targetPath);

        ensureParentDirectoryExists(target.getParent());
        Files.copy(source, target, java.nio.file.StandardCopyOption.REPLACE_EXISTING);

        long duration = System.currentTimeMillis() - startTime;
        if (duration > 1000) {
            instance.warn("复制文件耗时: " + sourcePath + " -> " + targetPath + " (" + duration + "ms)");
        }
    }

    public static Future<Boolean> createDirectoryAsync(String dirPath) {
        return ThreadPoolManager.submitIOCallable(() -> createDirectory(dirPath));
    }

    public static boolean createDirectory(String dirPath) {
        if (BootMonitor.isZygotePhase()) {
            return false;
        }

        try {
            Path path = Paths.get(dirPath);
            if (!Files.exists(path)) {
                Files.createDirectories(path);
            }
            return true;
        } catch (IOException e) {
            instance.error("创建目录失败: " + dirPath, e);
            return false;
        }
    }

    public static Future<Boolean> deleteDirectoryAsync(String dirPath) {
        return ThreadPoolManager.submitIOCallable(() -> deleteDirectory(dirPath));
    }

    public static boolean deleteDirectory(String dirPath) {
        if (BootMonitor.isZygotePhase()) {
            return false;
        }

        try {
            Path path = Paths.get(dirPath);
            if (Files.exists(path)) {
                Files.walk(path)
                        .sorted(java.util.Comparator.reverseOrder())
                        .forEach(p -> {
                            try {
                                Files.delete(p);
                            } catch (IOException e) {
                                instance.error("删除失败: " + p, e);
                            }
                        });
            }
            return true;
        } catch (IOException e) {
            instance.error("删除目录失败: " + dirPath, e);
            return false;
        }
    }

    public static Future<ProcessResult> executeCommandAsync(String command) {
        return executeCommandAsync(command, 30000);
    }

    public static Future<ProcessResult> executeCommandAsync(String command, long timeoutMs) {
        return ThreadPoolManager.submitIOCallable(() -> executeCommand(command, timeoutMs));
    }

    public static ProcessResult executeCommand(String command) throws IOException, InterruptedException {
        return executeCommand(command, 30000);
    }

    public static ProcessResult executeCommand(String command, long timeoutMs) throws IOException, InterruptedException {
        long startTime = System.currentTimeMillis();
        Process process = Runtime.getRuntime().exec(command);

        try {
            ProcessResult result = readProcessOutput(process, timeoutMs);

            long duration = System.currentTimeMillis() - startTime;
            if (duration > 5000) {
                instance.warn("命令执行耗时过长: " + command + " (" + duration + "ms)");
            }

            return result;
        } catch (Exception e) {
            process.destroyForcibly();
            throw e;
        }
    }

    public static Future<ProcessResult> executeRootCommandAsync(String command) {
        return executeRootCommandAsync(command, 30000);
    }

    public static Future<ProcessResult> executeRootCommandAsync(String command, long timeoutMs) {
        return ThreadPoolManager.submitIOCallable(() -> executeRootCommand(command, timeoutMs));
    }

    public static ProcessResult executeRootCommand(String command) throws IOException, InterruptedException {
        return executeRootCommand(command, 30000);
    }

    public static ProcessResult executeRootCommand(String command, long timeoutMs) throws IOException, InterruptedException {
        long startTime = System.currentTimeMillis();
        Process process = Runtime.getRuntime().exec("su");

        try {
            DataOutputStream os = new DataOutputStream(process.getOutputStream());
            os.writeBytes(command + "\n");
            os.writeBytes("exit\n");
            os.flush();
            os.close();

            ProcessResult result = readProcessOutput(process, timeoutMs);

            long duration = System.currentTimeMillis() - startTime;
            if (duration > 5000) {
                instance.warn("Root命令执行耗时过长: " + command + " (" + duration + "ms)");
            }

            return result;
        } catch (Exception e) {
            process.destroyForcibly();
            throw e;
        }
    }

    private static ProcessResult readProcessOutput(Process process, long timeoutMs) throws InterruptedException, IOException {
        StringBuilder stdout = new StringBuilder();
        StringBuilder stderr = new StringBuilder();

        Thread stdoutThread = new Thread(() -> {
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    stdout.append(line).append('\n');
                }
            } catch (IOException e) {
                instance.error("读取stdout失败", e);
            }
        });

        Thread stderrThread = new Thread(() -> {
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getErrorStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    stderr.append(line).append('\n');
                }
            } catch (IOException e) {
                instance.error("读取stderr失败", e);
            }
        });

        stdoutThread.setDaemon(true);
        stderrThread.setDaemon(true);
        stdoutThread.start();
        stderrThread.start();

        int exitCode;
        try {
            if (timeoutMs > 0) {
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
                    throw new InterruptedException("命令执行超时 (" + timeoutMs + "ms)");
                }
                exitCode = process.exitValue();
            } else {
                exitCode = process.waitFor();
            }
        } catch (InterruptedException e) {
            process.destroyForcibly();
            throw e;
        }

        stdoutThread.join(1000);
        stderrThread.join(1000);

        return new ProcessResult(exitCode, stdout.toString(), stderr.toString());
    }

    private static void ensureParentDirectoryExists(Path parentDir) throws IOException {
        if (parentDir != null && !Files.exists(parentDir)) {
            Files.createDirectories(parentDir);
        }
    }

    public static String getStats() {
        return String.format(
                "IOManager[readBytes=%d, writeBytes=%d, readTime=%dms, writeTime=%dms]",
                instance.totalBytesRead.get(),
                instance.totalBytesWritten.get(),
                instance.totalReadTime.get(),
                instance.totalWriteTime.get()
        );
    }

    public static class ProcessResult {
        public final int exitCode;
        public final String stdout;
        public final String stderr;
        public final long executionTime;

        public ProcessResult(int exitCode, String stdout, String stderr) {
            this(exitCode, stdout, stderr, 0);
        }

        public ProcessResult(int exitCode, String stdout, String stderr, long executionTime) {
            this.exitCode = exitCode;
            this.stdout = stdout;
            this.stderr = stderr;
            this.executionTime = executionTime;
        }

        public boolean isSuccess() {
            return exitCode == 0;
        }

        public String getOutput() {
            return stdout;
        }

        public String getError() {
            return stderr;
        }

        @Override
        public String toString() {
            return String.format("ProcessResult[exitCode=%d, stdout=%s, stderr=%s, executionTime=%dms]",
                    exitCode, stdout, stderr, executionTime);
        }
    }
}
