package com.justnothing.testmodule.command.functions.profile;

import com.justnothing.testmodule.utils.functions.Logger;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class ProfileManager {
    
    public static class ProfileManagerLogger extends Logger {
        @Override
        public String getTag() {
            return "ProfileManager";
        }
    }
    
    private static final ProfileManager instance = new ProfileManager();
    private static final ProfileManagerLogger logger = new ProfileManagerLogger();
    
    private final ExecutorService executor;
    private final AtomicBoolean profiling;
    private final AtomicInteger profilingDuration;
    private final List<ProfileSample> samples;
    private final Map<String, ProcessStats> processStatsMap;
    private final Map<String, ThreadStats> threadStatsMap;
    private ProfileTask currentTask;
    
    private ProfileManager() {
        this.executor = Executors.newSingleThreadExecutor(r -> {
            Thread thread = new Thread(r, "ProfileTask");
            thread.setDaemon(true);
            return thread;
        });
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
        executor.submit(currentTask);
        
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
    
    public String getProfileReport() {
        if (samples.isEmpty()) {
            return "暂无性能分析数据";
        }
        
        StringBuilder sb = new StringBuilder();
        sb.append("===== 性能分析报告 =====\n");
        sb.append("样本数量: ").append(samples.size()).append("\n");
        sb.append("分析时间: ").append(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date())).append("\n\n");
        
        sb.append("===== 系统资源概况 =====\n");
        ProfileSample lastSample = samples.get(samples.size() - 1);
        sb.append("CPU使用率: ").append(String.format("%.2f%%", lastSample.cpuUsage * 100)).append("\n");
        sb.append("内存使用: ").append(formatBytes(lastSample.memoryUsage)).append("\n");
        sb.append("线程数: ").append(lastSample.threadCount).append("\n");
        sb.append("进程数: ").append(lastSample.processCount).append("\n\n");
        
        sb.append("===== 进程资源统计 =====\n");
        for (Map.Entry<String, ProcessStats> entry : processStatsMap.entrySet()) {
            ProcessStats stats = entry.getValue();
            sb.append(String.format("  %s: CPU=%.2f%%, 内存=%s, 线程=%d\n",
                    entry.getKey(),
                    stats.cpuUsage * 100,
                    formatBytes(stats.memoryUsage),
                    stats.threadCount));
        }
        sb.append("\n");
        
        sb.append("===== 线程资源统计 =====\n");
        for (Map.Entry<String, ThreadStats> entry : threadStatsMap.entrySet()) {
            ThreadStats stats = entry.getValue();
            sb.append(String.format("  %s: CPU=%.2f%%, 状态=%s\n",
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
            sb.append(String.format("  [%s] CPU=%.2f%%, 内存=%s\n",
                    sample.timestamp,
                    sample.cpuUsage * 100,
                    formatBytes(sample.memoryUsage)));
        }
        
        return sb.toString();
    }
    
    public boolean exportToFile(String filePath) {
        synchronized (samples) {
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath))) {
                writer.write("===== 性能分析报告 =====\n");
                writer.write("样本数量: " + samples.size() + "\n");
                writer.write("分析时间: " + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()) + "\n\n");
                
                writer.write("===== 系统资源概况 =====\n");
                if (!samples.isEmpty()) {
                    ProfileSample lastSample = samples.get(samples.size() - 1);
                    writer.write("CPU使用率: " + String.format("%.2f%%", lastSample.cpuUsage * 100) + "\n");
                    writer.write("内存使用: " + formatBytes(lastSample.memoryUsage) + "\n");
                    writer.write("线程数: " + lastSample.threadCount + "\n");
                    writer.write("进程数: " + lastSample.processCount + "\n\n");
                }
                
                writer.write("===== 进程资源统计 =====\n");
                for (Map.Entry<String, ProcessStats> entry : processStatsMap.entrySet()) {
                    ProcessStats stats = entry.getValue();
                    writer.write(String.format("  %s: CPU=%.2f%%, 内存=%s, 线程=%d\n",
                            entry.getKey(),
                            stats.cpuUsage * 100,
                            formatBytes(stats.memoryUsage),
                            stats.threadCount));
                }
                writer.write("\n");
                
                writer.write("===== 线程资源统计 =====\n");
                for (Map.Entry<String, ThreadStats> entry : threadStatsMap.entrySet()) {
                    ThreadStats stats = entry.getValue();
                    writer.write(String.format("  %s: CPU=%.2f%%, 状态=%s\n",
                            entry.getKey(),
                            stats.cpuUsage * 100,
                            stats.state));
                }
                writer.write("\n");
                
                writer.write("===== 详细样本数据 =====\n");
                for (ProfileSample sample : samples) {
                    writer.write(sample.toString() + "\n");
                }
                
                return true;
            } catch (IOException e) {
                logger.error("导出性能分析数据失败", e);
                return false;
            }
        }
    }
    
    public boolean isProfiling() {
        return profiling.get();
    }
    
    public int getProfilingDuration() {
        return profilingDuration.get();
    }
    
    public int getSampleCount() {
        return samples.size();
    }
    
    private static String formatBytes(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        } else if (bytes < 1024 * 1024) {
            return String.format("%.2f KB", bytes / 1024.0);
        } else if (bytes < 1024 * 1024 * 1024) {
            return String.format("%.2f MB", bytes / (1024.0 * 1024.0));
        } else {
            return String.format("%.2f GB", bytes / (1024.0 * 1024.0 * 1024.0));
        }
    }
    
    public static class ProfileSample {
        public final String timestamp;
        public final double cpuUsage;
        public final long memoryUsage;
        public final int threadCount;
        public final int processCount;
        
        public ProfileSample(String timestamp, double cpuUsage, long memoryUsage, int threadCount, int processCount) {
            this.timestamp = timestamp;
            this.cpuUsage = cpuUsage;
            this.memoryUsage = memoryUsage;
            this.threadCount = threadCount;
            this.processCount = processCount;
        }
        
        @Override
        public String toString() {
            return String.format("[%s] CPU=%.2f%%, 内存=%s, 线程=%d, 进程=%d",
                    timestamp, cpuUsage * 100, formatBytes(memoryUsage), threadCount, processCount);
        }
        
        private static String formatBytes(long bytes) {
            if (bytes < 1024) {
                return bytes + " B";
            } else if (bytes < 1024 * 1024) {
                return String.format("%.2f KB", bytes / 1024.0);
            } else if (bytes < 1024 * 1024 * 1024) {
                return String.format("%.2f MB", bytes / (1024.0 * 1024.0));
            } else {
                return String.format("%.2f GB", bytes / (1024.0 * 1024.0 * 1024.0));
            }
        }
    }
    
    public static class ProcessStats {
        public final String packageName;
        public final double cpuUsage;
        public final long memoryUsage;
        public final int threadCount;
        
        public ProcessStats(String packageName, double cpuUsage, long memoryUsage, int threadCount) {
            this.packageName = packageName;
            this.cpuUsage = cpuUsage;
            this.memoryUsage = memoryUsage;
            this.threadCount = threadCount;
        }
    }
    
    public static class ThreadStats {
        public final String threadName;
        public final double cpuUsage;
        public final String state;
        
        public ThreadStats(String threadName, double cpuUsage, String state) {
            this.threadName = threadName;
            this.cpuUsage = cpuUsage;
            this.state = state;
        }
    }
}
