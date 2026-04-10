package com.justnothing.testmodule.command.functions.threads;

import androidx.annotation.NonNull;

import com.justnothing.testmodule.utils.functions.Logger;
import com.justnothing.testmodule.utils.io.IOManager;
import com.justnothing.testmodule.utils.concurrent.ThreadPoolManager;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class ProfileManager {
    private static final Logger logger = Logger.getLoggerForName("ProfileManager");
    private static final ProfileManager instance = new ProfileManager();
    private final AtomicBoolean profiling;
    private final AtomicInteger profilingDuration;
    private final List<ProfileSample> samples;
    private final Map<String, ProcessStats> processStatsMap;
    private final Map<String, ThreadStats> threadStatsMap;
    private ProfileTask currentTask;
    
    private ProfileManager() {
        this.profiling = new AtomicBoolean(false);
        this.profilingDuration = new AtomicInteger(60);
        this.samples = new ArrayList<>();
        this.processStatsMap = new ConcurrentHashMap<>();
        this.threadStatsMap = new ConcurrentHashMap<>();
    }
    
    public static ProfileManager getInstance() {
        return instance;
    }
    
    public void startProfiling(int duration) {
        if (profiling.get()) {
            throw new IllegalStateException("性能分析已在运行中");
        }
        
        profilingDuration.set(duration);
        profiling.set(true);
        samples.clear();
        processStatsMap.clear();
        threadStatsMap.clear();
        
        currentTask = new ProfileTask(duration, this);
        ThreadPoolManager.submitFastRunnable(currentTask);
        
        logger.info("开始性能分析，持续时间: " + duration + "秒");
    }
    
    public void stopProfiling() {
        if (!profiling.get()) {
            throw new IllegalStateException("性能分析未在运行");
        }
        
        profiling.set(false);
        if (currentTask != null) {
            currentTask.stop();
        }
        
        logger.info("停止性能分析，共采集 " + samples.size() + " 个样本");
    }
    
    public void addSample(ProfileSample sample) {
        synchronized (samples) {
            samples.add(sample);
        }
    }
    
    public void updateProcessStats(String packageName, ProcessStats stats) {
        processStatsMap.put(packageName, stats);
    }
    
    public void updateThreadStats(String threadName, ThreadStats stats) {
        threadStatsMap.put(threadName, stats);
    }
    
    @SuppressWarnings("SequencedCollectionMethodCanBeUsed")
    public String getProfileReport() {
        if (samples.isEmpty()) {
            return "暂无性能分析数据";
        }
        
        StringBuilder sb = new StringBuilder();
        sb.append("===== 性能分析报告 =====\n");
        sb.append("样本数量: ").append(samples.size()).append("\n");
        sb.append("分析时间: ").append(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date())).append("\n\n");
        
        sb.append("===== 系统资源概况 =====\n");
        ProfileSample lastSample = samples.get(samples.size() - 1);
        sb.append("CPU使用率: ").append(String.format(Locale.getDefault(), "%.2f%%", lastSample.cpuUsage * 100)).append("\n");
        sb.append("内存使用: ").append(formatBytes(lastSample.memoryUsage)).append("\n");
        sb.append("线程数: ").append(lastSample.threadCount).append("\n");
        sb.append("进程数: ").append(lastSample.processCount).append("\n\n");
        
        sb.append("===== 进程资源统计 =====\n");
        for (Map.Entry<String, ProcessStats> entry : processStatsMap.entrySet()) {
            ProcessStats stats = entry.getValue();
            sb.append(String.format(Locale.getDefault(),
                    "  %s: CPU=%.2f%%, 内存=%s, 线程=%d\n",
                    entry.getKey(),
                    stats.cpuUsage * 100,
                    formatBytes(stats.memoryUsage),
                    stats.threadCount));
        }
        sb.append("\n");
        
        sb.append("===== 线程资源统计 =====\n");
        for (Map.Entry<String, ThreadStats> entry : threadStatsMap.entrySet()) {
            ThreadStats stats = entry.getValue();
            sb.append(String.format(Locale.getDefault(),
                    "  %s: CPU=%.2f%%, 状态=%s\n",
                    entry.getKey(),
                    stats.cpuUsage * 100,
                    stats.state));
        }
        sb.append("\n");
        
        sb.append("===== 性能趋势 =====\n");
        int sampleCount = Math.min(10, samples.size());
        int step = samples.size() / sampleCount;
        for (int i = 0; i < sampleCount; i++) {
            int index = i * step;
            ProfileSample sample = samples.get(index);
            sb.append(String.format(Locale.getDefault(),
                    "  [%s] CPU=%.2f%%, 内存=%s\n",
                    sample.timestamp,
                    sample.cpuUsage * 100,
                    formatBytes(sample.memoryUsage)));
        }
        
        return sb.toString();
    }
    
    @SuppressWarnings("SequencedCollectionMethodCanBeUsed")
    public boolean exportToFile(String filePath) {
        synchronized (samples) {
            try {
                StringBuilder content = new StringBuilder();
                content.append("===== 性能分析报告 =====\n");
                content.append("样本数量: ").append(samples.size()).append("\n");
                content.append("分析时间: ").append(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date())).append("\n\n");
                
                content.append("===== 系统资源概况 =====\n");
                if (!samples.isEmpty()) {
                    ProfileSample lastSample = samples.get(samples.size() - 1);
                    content.append("CPU使用率: ").append(String.format(Locale.getDefault(), "%.2f%%", lastSample.cpuUsage * 100)).append("\n");
                    content.append("内存使用: ").append(formatBytes(lastSample.memoryUsage)).append("\n");
                    content.append("线程数: ").append(lastSample.threadCount).append("\n");
                    content.append("进程数: ").append(lastSample.processCount).append("\n\n");
                }
                
                content.append("===== 进程资源统计 =====\n");
                for (Map.Entry<String, ProcessStats> entry : processStatsMap.entrySet()) {
                    ProcessStats stats = entry.getValue();
                    content.append(String.format(Locale.getDefault(),
                            "  %s: CPU=%.2f%%, 内存=%s, 线程=%d\n",
                            entry.getKey(),
                            stats.cpuUsage * 100,
                            formatBytes(stats.memoryUsage),
                            stats.threadCount));
                }
                content.append("\n");
                
                content.append("===== 线程资源统计 =====\n");
                for (Map.Entry<String, ThreadStats> entry : threadStatsMap.entrySet()) {
                    ThreadStats stats = entry.getValue();
                    content.append(String.format(Locale.getDefault(),
                            "  %s: CPU=%.2f%%, 状态=%s\n",
                            entry.getKey(),
                            stats.cpuUsage * 100,
                            stats.state));
                }
                content.append("\n");
                
                content.append("===== 详细样本数据 =====\n");
                for (ProfileSample sample : samples) {
                    content.append(sample.toString()).append("\n");
                }
                
                IOManager.writeFile(filePath, content.toString());
                return true;
            } catch (IOException e) {
                logger.error("导出性能分析数据失败", e);
                return false;
            }
        }
    }
    

    private static String formatBytes(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        } else if (bytes < 1024 * 1024) {
            return String.format(Locale.getDefault(), "%.2f KB", bytes / 1024.0);
        } else if (bytes < 1024 * 1024 * 1024) {
            return String.format(Locale.getDefault(), "%.2f MB", bytes / (1024.0 * 1024.0));
        } else {
            return String.format(Locale.getDefault(), "%.2f GB", bytes / (1024.0 * 1024.0 * 1024.0));
        }
    }

    public record ProfileSample(String timestamp, double cpuUsage, long memoryUsage,
                                int threadCount, int processCount) {

        @NonNull
        @Override
            public String toString() {
                return String.format(Locale.getDefault(),
                        "[%s] CPU=%.2f%%, 内存=%s, 线程=%d, 进程=%d",
                        timestamp, cpuUsage * 100, formatBytes(memoryUsage), threadCount, processCount);
            }

        private static String formatBytes(long bytes) {
                if (bytes < 1024) {
                    return bytes + " B";
                } else if (bytes < 1024 * 1024) {
                    return String.format(Locale.getDefault(), "%.2f KB", bytes / 1024.0);
                } else if (bytes < 1024 * 1024 * 1024) {
                    return String.format(Locale.getDefault(), "%.2f MB", bytes / (1024.0 * 1024.0));
                } else {
                    return String.format(Locale.getDefault(), "%.2f GB", bytes / (1024.0 * 1024.0 * 1024.0));
                }
            }
        }

    public record ProcessStats(String packageName, double cpuUsage, long memoryUsage,
                               int threadCount) {
    }

    public record ThreadStats(String threadName, double cpuUsage, String state) {
    }
}
