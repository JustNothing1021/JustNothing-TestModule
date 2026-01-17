package com.justnothing.testmodule.utils.data;

import android.util.Log;

import java.io.File;
import java.io.RandomAccessFile;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class LogCache {
    private static final ThreadLocal<SimpleDateFormat> TIMESTAMP_FORMAT = ThreadLocal.withInitial(() ->
        new SimpleDateFormat("yyyy/MM/dd HH:mm:ss.SSS", Locale.getDefault()));

    private static String formatTimestamp(long timestampMs) {
        return Objects.requireNonNull(TIMESTAMP_FORMAT.get()).format(new Date(timestampMs));
    }

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
                            Log.w("LogCache", "解析时间戳失败: " + timestamp, e);
                        }
                    } else {
                        tag = "UnknownTag";
                    }
                } else {
                    tag = "UnknownTag";
                }

                return new LogEntry(timestamp, timestampMs, level, tag, message);
            } catch (Exception e) {
                Log.e("LogCache", "解析日志失败: " + logLine, e);
                return new LogEntry("ERROR", "UnknownTag", logLine);
            }
        }
    }

    private static final int MAX_CACHED_LOGS = 1000;
    private static final int MAX_MEMORY_LOGS = 200;
    private static final long MAX_LOG_FILE_SIZE = 10 * 1024 * 1024;
    private static final long MAX_MEMORY_SIZE = 5 * 1024 * 1024;
    private boolean enabled = true;
    private boolean saveLogs = false;
    private boolean cacheDirty = true;
    private List<LogEntry> cachedLogs = new ArrayList<>();
    private final ReentrantReadWriteLock cacheLock = new ReentrantReadWriteLock();
    private static long lastRefreshTime = 0;
    private static final long CACHE_REFRESH_INTERVAL = 5000;
    private static long lastCleanupTime = 0;
    private static final long CLEANUP_INTERVAL = 30000;
    private static long totalMemoryUsage = 0;
    
    public LogCache() {
    }
    
    public LogCache(boolean doCacheLogs) {
        this.saveLogs = doCacheLogs;
    }

    public void addLog(String level, String tag, String message, long timestamp) {
        if (!enabled) return;
        
        // 检查日志级别，过滤掉过于频繁的调试日志
        if (shouldFilterLog(level, tag, message)) {
            return;
        }

        LogEntry entry = new LogEntry(timestamp, level, tag, message);
        
        // 写入文件日志
        DataBridge.appendLog(entry.getFormattedMessage());
        
        if (saveLogs) {
            cacheLock.writeLock().lock();
            try {
                // 检查内存使用情况
                long entrySize = estimateEntrySize(entry);
                
                // 如果内存使用超过限制，清理最旧的日志
                while (totalMemoryUsage + entrySize > MAX_MEMORY_SIZE && !cachedLogs.isEmpty()) {
                    LogEntry oldest = cachedLogs.remove(0);
                    totalMemoryUsage -= estimateEntrySize(oldest);
                }
                
                // 如果日志数量超过限制，清理最旧的日志
                while (cachedLogs.size() >= MAX_MEMORY_LOGS && !cachedLogs.isEmpty()) {
                    LogEntry oldest = cachedLogs.remove(0);
                    totalMemoryUsage -= estimateEntrySize(oldest);
                }
                
                cachedLogs.add(entry);
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
    
    private void cleanupOldLogs() {
        try {
            File logFile = DataBridge.getLogFile();
            if (logFile != null && logFile.exists()) {
                long fileSize = logFile.length();
                if (fileSize > MAX_LOG_FILE_SIZE) {
                    try (RandomAccessFile raf = new RandomAccessFile(logFile, "rw")) {

                        long targetSize = (long) (MAX_LOG_FILE_SIZE * 0.8);
                        long bytesToRemove = fileSize - targetSize;

                        long writePos = 0;
                        long readPos = 0;
                        byte[] buffer = new byte[8192];

                        raf.seek(0);
                        while (readPos < fileSize) {
                            int bytesToRead = (int) Math.min(buffer.length, fileSize - readPos);
                            int bytesRead = raf.read(buffer, 0, bytesToRead);

                            if (bytesRead == -1) break;

                            if (readPos >= bytesToRemove) {
                                raf.seek(writePos);
                                raf.write(buffer, 0, bytesRead);
                                writePos += bytesRead;
                            }

                            readPos += bytesRead;
                        }

                        raf.setLength(writePos);
                    }
                }
            }
        } catch (Exception e) {
            Log.e("LogCache", "清理旧日志失败", e);
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
                        Log.e("LogCache", "解析日志失败: " + line, e);
                    }
                }
            }
        }
        
        cacheLock.writeLock().lock();
        try {
            cachedLogs = logs;
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
                return new ArrayList<>(cachedLogs);
            }
        } finally {
            cacheLock.readLock().unlock();
        }
        
        refreshCache();
        
        cacheLock.readLock().lock();
        try {
            return new ArrayList<>(cachedLogs);
        } finally {
            cacheLock.readLock().unlock();
        }
    }

    public void clearLogs() {
        DataBridge.clearLogs();
        
        cacheLock.writeLock().lock();
        try {
            cachedLogs.clear();
            totalMemoryUsage = 0;
            cacheDirty = false;
        } finally {
            cacheLock.writeLock().unlock();
        }
    }
    
    /**
     * 估算日志条目占用的内存大小
     */
    private long estimateEntrySize(LogEntry entry) {
        if (entry == null) return 0;
        
        // 估算字符串占用的内存（UTF-16编码，每个字符2字节）
        long size = 0;
        if (entry.timestamp != null) size += entry.timestamp.length() * 2L;
        if (entry.level != null) size += entry.level.length() * 2L;
        if (entry.tag != null) size += entry.tag.length() * 2L;
        if (entry.message != null) size += entry.message.length() * 2L;
        
        // 加上对象头和其他字段的开销（约40字节）
        return size + 40;
    }
    
    /**
     * 检查是否应该过滤掉该日志
     */
    private boolean shouldFilterLog(String level, String tag, String message) {
        // 过滤过于频繁的调试日志
        if ("DEBUG".equals(level)) {
            // 检查是否是重复的调试日志
            if (isDuplicateDebugLog(tag, message)) {
                return true;
            }
            
            // 限制调试日志的频率
            if (isDebugLogTooFrequent(tag)) {
                return true;
            }
        }
        
        // 过滤过长的日志消息
        if (message != null && message.length() > 1000) {
            // 截断过长的消息
            return true;
        }
        
        return false;
    }
    
    /**
     * 检查是否是重复的调试日志
     */
    private boolean isDuplicateDebugLog(String tag, String message) {
        // 简单的重复检测：检查最近几条日志是否有相同的标签和消息
        cacheLock.readLock().lock();
        try {
            int checkCount = Math.min(10, cachedLogs.size());
            for (int i = cachedLogs.size() - 1; i >= Math.max(0, cachedLogs.size() - checkCount); i--) {
                LogEntry recent = cachedLogs.get(i);
                if (recent.tag.equals(tag) && recent.message.equals(message)) {
                    return true;
                }
            }
        } finally {
            cacheLock.readLock().unlock();
        }
        return false;
    }
    
    /**
     * 检查调试日志是否过于频繁
     */
    private boolean isDebugLogTooFrequent(String tag) {
        // 检查最近一段时间内相同标签的日志数量
        cacheLock.readLock().lock();
        try {
            long currentTime = System.currentTimeMillis();
            int count = 0;
            
            for (int i = cachedLogs.size() - 1; i >= 0; i--) {
                LogEntry entry = cachedLogs.get(i);
                
                // 只检查最近5秒内的日志
                if (currentTime - entry.timestampMs > 5000) {
                    break;
                }
                
                if (entry.tag.equals(tag) && "DEBUG".equals(entry.level)) {
                    count++;
                    
                    // 如果5秒内相同标签的调试日志超过10条，限制频率
                    if (count > 10) {
                        return true;
                    }
                }
            }
        } finally {
            cacheLock.readLock().unlock();
        }
        return false;
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
