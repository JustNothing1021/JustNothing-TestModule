package com.justnothing.testmodule.utils.io;

import androidx.annotation.NonNull;

import com.justnothing.testmodule.utils.concurrent.ThreadPoolManager;
import com.justnothing.testmodule.utils.data.BootMonitor;
import com.justnothing.testmodule.utils.functions.Logger;

import java.io.*;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Locale;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;

public class IOManager {
    private static final String TAG = "IOManager";
    private static final Logger logger = Logger.getLoggerForName(TAG);
    private static volatile IOManager instance = null;


    private final AtomicLong totalBytesRead = new AtomicLong(0);
    private final AtomicLong totalBytesWritten = new AtomicLong(0);
    private final AtomicLong totalReadTime = new AtomicLong(0);
    private final AtomicLong totalWriteTime = new AtomicLong(0);


    private IOManager() {
        super();
        logger.info("IOManager初始化完成");
    }


    public static IOManager getInstance() {
        if (instance == null) {
            synchronized (IOManager.class) {
                if (instance == null) {
                    if (BootMonitor.isZygotePhase()) {
                        return null;
                    }
                    instance = new IOManager();
                }
            }
        }
        return instance;
    }

    public static Future<String> readFileAsync(String filePath) {
        return readFileAsync(filePath, StandardCharsets.UTF_8);
    }

    public static Future<String> readFileAsync(String filePath, Charset charset) {
        return ThreadPoolManager.submitIOCallable(() -> readFile(filePath, charset));
    }

    public static String readFile(String filePath) throws IOException {
        return readFile(filePath, StandardCharsets.UTF_8);
    }

    public static String readFile(String filePath, Charset charset) throws IOException {
        return readFile(filePath, charset, 10 * 1024 * 1024);
    }

    public static String readFile(String filePath, Charset charset, long maxSize) throws IOException {
        return readFile(filePath, charset, maxSize, -1);
    }

    public static String readFile(String filePath, Charset charset, long maxSize, int maxLines) throws IOException {
        if (BootMonitor.isZygotePhase()) {
            return null;
        }

        long startTime = System.currentTimeMillis();
        Path path = Paths.get(filePath);

        if (!Files.exists(path)) {
            throw new FileNotFoundException("文件不存在: " + filePath);
        }

        long fileSize = Files.size(path);
        if (maxSize > 0 && fileSize > maxSize) {
            throw new IOException("文件过大: " + filePath + " (" + fileSize + " bytes, 最大允许: " + maxSize + " bytes)");
        }

        logger.debug("开始读取文件: " + filePath + ", 大小: " + fileSize + " bytes, 最大行数: " + maxLines);
        
        byte[] bytes;
        if (maxLines > 0) {
            bytes = readLastLines(path, charset, maxLines);
        } else {
            bytes = Files.readAllBytes(path);
        }
        
        String content = new String(bytes, charset);

        long duration = System.currentTimeMillis() - startTime;
        IOManager mgr = getInstance();
        if (mgr != null) {
            mgr.totalBytesRead.addAndGet(bytes.length);
            mgr.totalReadTime.addAndGet(duration);

            logger.debug("文件读取完成: " + filePath + ", 耗时: " + duration + "ms, 字节数: " + bytes.length);
            
            if (duration > 1000) {
                logger.warn("读取文件耗时过长: " + filePath + " (" + duration + "ms, " + bytes.length + " bytes)");
            }
        }

        return content;
    }

    private static byte[] readLastLines(Path path, Charset charset, int maxLines) throws IOException {
        try (RandomAccessFile raf = new RandomAccessFile(path.toFile(), "r")) {

            long fileLength = raf.length();
            if (fileLength == 0) {
                logger.debug("日志文件为空");
                return new byte[0];
            }

            long filePointer = fileLength - 1;
            int lineCount = 0;

            while (filePointer >= 0 && lineCount < maxLines) {
                raf.seek(filePointer);
                byte b = raf.readByte();

                if (b == '\n') {
                    lineCount++;
                    if (lineCount >= maxLines) {
                        filePointer++;
                        break;
                    }
                }

                filePointer--;
            }

            if (filePointer < 0) {
                filePointer = 0;
            }

            logger.debug("从位置 " + filePointer + " 开始读取，共 " + lineCount + " 行");

            raf.seek(filePointer);

            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(new FileInputStream(raf.getFD()), charset));

            StringBuilder sb = new StringBuilder();
            String line;
            int linesRead = 0;
            while ((line = reader.readLine()) != null && linesRead < maxLines) {
                if (linesRead > 0) {
                    sb.append('\n');
                }
                sb.append(line);
                linesRead++;
            }

            reader.close();
            logger.debug("实际读取 " + linesRead + " 行，总长度: " + sb.length());
            return sb.toString().getBytes(charset);
        }
    }

    public static Future<Void> writeFileAsync(String filePath, String content) {
        return writeFileAsync(filePath, content, StandardCharsets.UTF_8, false);
    }

    public static Future<Void> writeFileAsync(String filePath, String content, boolean append) {
        return writeFileAsync(filePath, content, StandardCharsets.UTF_8, append);
    }

    public static Future<Void> writeFileAsync(String filePath, String content, Charset charset, boolean append) {
        return ThreadPoolManager.submitIOCallable(() -> {
            writeFile(filePath, content, charset, append);
            return null;
        });
    }

    public static void writeFile(String filePath, String content) throws IOException {
        writeFile(filePath, content, StandardCharsets.UTF_8, false);
    }

    public static void writeFile(String filePath, byte[] content) throws IOException {
        writeFile(filePath, content, false);
    }

    public static void writeFile(String filePath, String content, boolean append) throws IOException {
        writeFile(filePath, content, StandardCharsets.UTF_8, append);
    }
    public static void writeFile(String filePath, String content, Charset charset, boolean append)
            throws IOException {
        byte[] bytes = content.getBytes(charset);
        writeFile(filePath, bytes, append);
    }

    public static void writeFile(String filePath, byte[] bytes, boolean append) throws IOException {
        if (BootMonitor.isZygotePhase()) {
            return;
        }

        long startTime = System.currentTimeMillis();
        Path path = Paths.get(filePath);

        ensureParentDirectoryExists(path.getParent());

        logger.debug("开始写入文件: " + filePath + ", 字节数: " + bytes.length + ", 追加模式: " + append);
        
        if (append) {
            Files.write(path, bytes, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } else {
            Files.write(path, bytes, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        }

        long duration = System.currentTimeMillis() - startTime;
        IOManager mgr = getInstance();
        if (mgr != null) {
            mgr.totalBytesWritten.addAndGet(bytes.length);
            mgr.totalWriteTime.addAndGet(duration);

            logger.debug("文件写入完成: " + filePath + ", 耗时: " + duration + "ms");
            
            if (duration > 1000) {
                logger.warn("写入文件耗时过长: " + filePath + " (" + duration + "ms, " + bytes.length + " bytes)");
            }
        }
    }

    public static Future<Void> appendFileAsync(String filePath, String content) {
        return appendFileAsync(filePath, content, StandardCharsets.UTF_8);
    }

    public static Future<Void> appendFileAsync(String filePath, String content, Charset charset) {
        return ThreadPoolManager.submitIOCallable(() -> {
            appendFile(filePath, content, charset);
            return null;
        });
    }

    public static void appendFile(String filePath, String content) throws IOException {
        appendFile(filePath, content, StandardCharsets.UTF_8);
    }

    public static void appendFile(String filePath, String content, Charset charset) throws IOException {
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
            logger.error("删除文件失败: " + filePath, e);
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
            logger.error("检查文件存在失败: " + filePath, e);
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
            logger.warn("截断文件耗时: " + filePath + " (" + duration + "ms)");
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
            logger.warn("复制文件耗时: " + sourcePath + " -> " + targetPath + " (" + duration + "ms)");
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
            logger.error("创建目录失败: " + dirPath, e);
            return false;
        }
    }

    public static boolean createDirectory(File file) {
        return createDirectory(file.getAbsolutePath());
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
                try (Stream<Path> files = Files.walk(path)
                        .sorted(java.util.Comparator.reverseOrder())) {
                    files.forEach(p -> {
                        try {
                            Files.delete(p);
                        } catch (IOException e) {
                            logger.error("删除失败: " + p, e);
                        }
                    });
                }
            }
            return true;
        } catch (IOException e) {
            logger.error("删除目录失败: " + dirPath, e);
            return false;
        }
    }

    private static void ensureParentDirectoryExists(Path parentDir) throws IOException {
        if (parentDir != null && !Files.exists(parentDir)) {
            Files.createDirectories(parentDir);
        }
    }

    public static String getStats() {
        IOManager mgr = getInstance();
        if (mgr == null) {
            return "IOManager[未初始化]";
        }
        return String.format(
                Locale.getDefault(),
                "IOManager[readBytes=%d, writeBytes=%d, readTime=%dms, writeTime=%dms]",
                mgr.totalBytesRead.get(),
                mgr.totalBytesWritten.get(),
                mgr.totalReadTime.get(),
                mgr.totalWriteTime.get()
        );
    }

    public record ProcessResult(int exitCode, String stdout, String stderr, long executionTime) {
            public ProcessResult(int exitCode, String stdout, String stderr) {
                this(exitCode, stdout, stderr, 0);
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

            @NonNull
            @Override
            public String toString() {
                return String.format(
                        Locale.getDefault(),
                        "ProcessResult[exitCode=%d, stdout=%s, stderr=%s, executionTime=%dms]",
                        exitCode, stdout, stderr, executionTime);
            }
        }
}
