package com.justnothing.testmodule.utils.data;

import android.util.Log;

import com.justnothing.testmodule.constants.FileDirectory;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class LogWriter {
    private static final ThreadLocal<SimpleDateFormat> TIMESTAMP_FORMAT = ThreadLocal.withInitial(() ->
        new SimpleDateFormat("yyyy/MM/dd HH:mm:ss.SSS", Locale.getDefault()));

    public static final String TAG = "JustNothing[LogWriter]";
    
    private static final int LOG_BUFFER_SIZE = 1000;
    private static final long LOG_FLUSH_INTERVAL = 2000;
    private static final long MAX_BUFFER_SIZE = 1024 * 1024;
    private static final List<String> logWriteBuffer = new ArrayList<>();
    private static int logBufferCount = 0;
    private static long totalBufferSize = 0;
    private static long lastFlushTime = 0;
    private static File cachedLogFile = null;
    private static final ReentrantReadWriteLock logFileLock = new ReentrantReadWriteLock();
    private static final int MAX_MEMORY_LOGS = 200;
    private static final long MAX_LOG_FILE_SIZE = 10 * 1024 * 1024;
    private static final long MAX_MEMORY_SIZE = 5 * 1024 * 1024;
    private boolean enabled = true;
    private boolean saveLogs = false;
    private boolean cacheDirty = true;
    private List<LogEntry> readLogs = new ArrayList<>();
    private final ReentrantReadWriteLock cacheLock = new ReentrantReadWriteLock();
    private static long lastRefreshTime = 0;
    private static final long CACHE_REFRESH_INTERVAL = 5000;
    private static long lastCleanupTime = 0;
    private static final long CLEANUP_INTERVAL = 30000;
    private static long totalMemoryUsage = 0;


    public static class LogEntry {

        public final String timestamp;
        public final String level;
        public final String tag;
        public final String message;
        public final long timestampMs;

        public LogEntry(String level, String tag, String message) {
            this(System.currentTimeMillis(), level, tag, message);
        }

        public LogEntry(long timestampMs, String level, String tag, String message) {
            this.timestampMs = timestampMs;
            this.timestamp = formatTimestamp(timestampMs);
            this.level = level.strip();
            this.tag = tag.strip();
            this.message = message;
        }

        public LogEntry(String timestamp, long timestampMs, String level, String tag, String message) {
            this.timestamp = timestamp;
            this.timestampMs = timestampMs;
            this.level = level.strip();
            this.tag = tag.strip();
            this.message = message;
        }

        public String getFormattedMessage() {
            return String.format("[%s] [%5s] %20s %s", timestamp, level, tag, message);
        }

        public static LogEntry fromString(String logLine) {
            try {
                String timestamp = "";
                long timestampMs = System.currentTimeMillis();
                String level = "";
                String tag;
                String message = logLine;

                if (logLine.matches("^\\[.*] \\[.*].*")) {
                    int firstBracketStart = logLine.indexOf("[");
                    int firstBracketEnd = logLine.indexOf("]");
                    int secondBracketStart = logLine.indexOf("[", firstBracketEnd);
                    int secondBracketEnd = logLine.indexOf("]", secondBracketStart);

                    if (firstBracketEnd > 0 && secondBracketStart > firstBracketEnd && secondBracketEnd > secondBracketStart) {
                        timestamp = logLine.substring(firstBracketStart + 1, firstBracketEnd);
                        level = logLine.substring(secondBracketStart + 1, secondBracketEnd);

                        String remaining = logLine.substring(secondBracketEnd + 1).trim();
                        int tagEndIndex = remaining.indexOf(" ");
                        if (tagEndIndex > 0) {
                            tag = remaining.substring(0, tagEndIndex).trim();
                            message = remaining.substring(tagEndIndex).trim();
                        } else {
                            tag = "UnknownTag";
                            message = remaining;
                        }

                        try {
                            SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss.SSS", Locale.getDefault());
                            Date date = sdf.parse(timestamp);
                            if (date != null) {
                                timestampMs = date.getTime();
                            }
                        } catch (Exception e) {
                            Log.w(TAG, "解析时间戳失败: " + timestamp, e);
                        }
                    } else {
                        tag = "UnknownTag";
                    }
                } else {
                    tag = "UnknownTag";
                }

                return new LogEntry(timestamp, timestampMs, level, tag, message);
            } catch (Exception e) {
                Log.e(TAG, "解析日志失败: " + logLine, e);
                return new LogEntry("ERROR", "UnknownTag", logLine);
            }
        }
    }

    
    public LogWriter() {}
    
    public LogWriter(boolean doCacheLogs) {
        this.saveLogs = doCacheLogs;
    }

    private static String formatTimestamp(long timestampMs) {
        return Objects.requireNonNull(TIMESTAMP_FORMAT.get()).format(new Date(timestampMs));
    }

    public void addLog(String level, String tag, String message, long timestamp) {
        if (!enabled) return;

        LogEntry entry = new LogEntry(timestamp, level, tag, message);
        String formattedMessage = entry.getFormattedMessage();
        
        appendToLogFile(formattedMessage);
        
        if (saveLogs) {
            cacheLock.writeLock().lock();
            try {
                long entrySize = estimateEntrySize(entry);
                
                while (totalMemoryUsage + entrySize > MAX_MEMORY_SIZE && !readLogs.isEmpty()) {
                    LogEntry oldest = readLogs.remove(0);
                    totalMemoryUsage -= estimateEntrySize(oldest);
                }
                
                while (readLogs.size() >= MAX_MEMORY_LOGS) {
                    LogEntry oldest = readLogs.remove(0);
                    totalMemoryUsage -= estimateEntrySize(oldest);
                }
                
                readLogs.add(entry);
                totalMemoryUsage += entrySize;
                
                cacheDirty = true;
            } finally {
                cacheLock.writeLock().unlock();
            }
        }
        
        long currentTime = System.currentTimeMillis();
        if ((currentTime - lastCleanupTime) >= CLEANUP_INTERVAL) {
            lastCleanupTime = currentTime;
            cleanupOldLogs();
        }
    }
    
    private static void appendToLogFile(String log) {
        if (BootMonitor.isZygotePhase()) {
            return;
        }
        
        logFileLock.writeLock().lock();
        try {
            long logSize = log != null ? log.length() * 2L : 0;
            
            while ((logBufferCount >= LOG_BUFFER_SIZE || totalBufferSize + logSize > MAX_BUFFER_SIZE) && !logWriteBuffer.isEmpty()) {
                String oldest = logWriteBuffer.remove(0);
                logBufferCount--;
                totalBufferSize -= oldest != null ? oldest.length() * 2L : 0;
            }
            
            logWriteBuffer.add(log);
            logBufferCount++;
            totalBufferSize += logSize;
            
            long currentTime = System.currentTimeMillis();
            boolean shouldFlush = logBufferCount >= LOG_BUFFER_SIZE || 
                                 totalBufferSize >= MAX_BUFFER_SIZE ||
                                 (currentTime - lastFlushTime) >= LOG_FLUSH_INTERVAL;
            
            if (shouldFlush) {
                Log.d("JustNothing[LogWriter]", "刷新日志文件，当前缓冲区大小：" + logBufferCount);
                flushLogFile();
                lastFlushTime = currentTime;
            }
        } finally {
            logFileLock.writeLock().unlock();
        }
    }
    
    private static void flushLogFile() {
        if (logWriteBuffer.isEmpty()) {
            return;
        }
        
        try {
            File logFile = getCachedLogFile();
            if (logFile == null) {
                return;
            }
            
            File logDir = logFile.getParentFile();
            if (logDir != null && !logDir.exists()) {
                Log.d(TAG, "创建日志目录: " + logDir.getAbsolutePath());
                if (!logDir.mkdirs()) Log.w(TAG, "创建日志目录失败，mkdirs返回false");
            }
            
            if (!logFile.exists()) {
                Log.d(TAG, "创建日志文件: " + logFile.getAbsolutePath());
                if (!logFile.createNewFile()) Log.w(TAG, "创建日志文件失败，mkdirs返回false");

            }
            
            Log.d(TAG, "开始写入日志文件: " + logFile.getAbsolutePath() +
                    ", 缓冲区数量: " + logBufferCount + 
                    ", 总大小: " + totalBufferSize + " bytes");
            
            StringBuilder sb = new StringBuilder();
            while (!logWriteBuffer.isEmpty()) {
                String log = logWriteBuffer.remove(0);
                sb.append(log).append("\n");
            }
            
            try (FileWriter writer = new FileWriter(logFile, true)) {
                writer.write(sb.toString());
            }
            
            Log.d(TAG, "日志文件写入完成");
            
            logBufferCount = 0;
            totalBufferSize = 0;
        } catch (IOException e) {
            Log.e(TAG, "写入日志文件失败", e);
        }
    }
    
    private static File getCachedLogFile() {
        if (cachedLogFile != null) {
            return cachedLogFile;
        }
        
        if (BootMonitor.isZygotePhase()) {
            return null;
        }
        
        try {
            File dataDir = DataBridge.getDataDir();
            if (dataDir != null) {
                cachedLogFile = new File(dataDir, FileDirectory.MODULE_LOG_FILE_NAME);
                return cachedLogFile;
            }
        } catch (Exception e) {
            Log.e(TAG, "获取日志文件路径失败", e);
        }
        
        return null;
    }
    
    private void cleanupOldLogs() {
        try {
            File logFile = getCachedLogFile();
            if (logFile != null && logFile.exists()) {
                long fileSize = logFile.length();
                if (fileSize > MAX_LOG_FILE_SIZE) {
                    long targetSize = (long) (MAX_LOG_FILE_SIZE * 0.8);
                    
                    try (RandomAccessFile raf = new RandomAccessFile(logFile, "rw")) {
                        raf.seek(targetSize);
                        
                        byte[] buffer = new byte[8192];
                        long readPos = targetSize;
                        long writePos = 0;
                        
                        while (readPos < fileSize) {
                            int bytesToRead = (int) Math.min(buffer.length, fileSize - readPos);
                            int bytesRead = raf.read(buffer, 0, bytesToRead);
                            
                            if (bytesRead == -1) break;
                            
                            raf.seek(writePos);
                            raf.write(buffer, 0, bytesRead);
                            
                            writePos += bytesRead;
                            readPos += bytesRead;
                        }
                        
                        raf.setLength(writePos);
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "清理旧日志失败", e);
        }
    }

    private void refreshCache() {
        long currentTime = System.currentTimeMillis();
        if (!cacheDirty && (currentTime - lastRefreshTime) < CACHE_REFRESH_INTERVAL) {
            return;
        }
        
        List<LogEntry> logs = new ArrayList<>();
        String logsText = DataBridge.readLogs();
        if (!logsText.isEmpty()) {
            String[] lines = logsText.split("\n");
            for (String line : lines) {
                if (!line.trim().isEmpty()) {
                    try {
                        LogEntry entry = LogEntry.fromString(line);
                        logs.add(entry);
                    } catch (Exception e) {
                        Log.e(TAG, "解析日志失败: " + line, e);
                    }
                }
            }
        }
        
        cacheLock.writeLock().lock();
        try {
            readLogs = logs;
            cacheDirty = false;
            lastRefreshTime = currentTime;
        } finally {
            cacheLock.writeLock().unlock();
        }
    }

    public List<LogEntry> getLogs() {
        cacheLock.readLock().lock();
        try {
            if (!cacheDirty) {
                return new ArrayList<>(readLogs);
            }
        } finally {
            cacheLock.readLock().unlock();
        }

        refreshCache();
        cacheLock.readLock().lock();
        try {
            return new ArrayList<>(readLogs);
        } finally {
            cacheLock.readLock().unlock();
        }
    }

    public void clearLogs() {
        DataBridge.clearLogs();
        cacheLock.writeLock().lock();
        try {
            readLogs.clear();
            totalMemoryUsage = 0;
            cacheDirty = false;
        } finally {
            cacheLock.writeLock().unlock();
        }
    }

    private long estimateEntrySize(LogEntry entry) {
        if (entry == null) return 0;
        long size = 0;
        if (entry.timestamp != null) size += entry.timestamp.length() * 2L;
        if (entry.level != null) size += entry.level.length() * 2L;
        if (entry.tag != null) size += entry.tag.length() * 2L;
        if (entry.message != null) size += entry.message.length() * 2L;
        return size + 40;
    }
    

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public int getLogCount() {
        return getLogs().size();
    }
}
