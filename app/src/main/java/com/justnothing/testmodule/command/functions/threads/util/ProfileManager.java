package com.justnothing.testmodule.command.functions.threads.util;

import androidx.annotation.NonNull;

import com.justnothing.testmodule.command.framework.i18n.Text;
import com.justnothing.testmodule.command.functions.threads.ThreadsTexts;
import com.justnothing.testmodule.utils.logging.Logger;
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
            throw new IllegalStateException(Text.zhEn("性能分析已在运行中", "Profiling is already running").text());
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
            throw new IllegalStateException(Text.zhEn("性能分析未在运行", "Profiling is not running").text());
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
            return ThreadsTexts.NO_PROFILE_DATA.text();
        }
        
        StringBuilder sb = new StringBuilder();
        sb.append(ThreadsTexts.TITLE_PROFILE_REPORT.text());
        sb.append(ThreadsTexts.LABEL_SAMPLE_COUNT.text()).append(samples.size()).append("\n");
        sb.append(ThreadsTexts.LABEL_ANALYSIS_TIME.text()).append(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date())).append("\n\n");
        
        sb.append(ThreadsTexts.TITLE_SYSTEM_RESOURCES.text());
        ProfileSample lastSample = samples.get(samples.size() - 1);
        sb.append(ThreadsTexts.LABEL_CPU_USAGE.text()).append(String.format(Locale.getDefault(), "%.2f%%", lastSample.cpuUsage * 100)).append("\n");
        sb.append(ThreadsTexts.LABEL_MEMORY_USAGE.text()).append(formatBytes(lastSample.memoryUsage)).append("\n");
        sb.append(ThreadsTexts.LABEL_THREADS.text()).append(lastSample.threadCount).append("\n");
        sb.append(ThreadsTexts.LABEL_PROCESSES.text()).append(lastSample.processCount).append("\n\n");
        
        sb.append(ThreadsTexts.TITLE_PROCESS_STATS.text());
        for (Map.Entry<String, ProcessStats> entry : processStatsMap.entrySet()) {
            ProcessStats stats = entry.getValue();
            sb.append(ThreadsTexts.LINE_PROCESS_STATS.format(
                    entry.getKey(),
                    stats.cpuUsage * 100,
                    formatBytes(stats.memoryUsage),
                    stats.threadCount));
        }
        sb.append("\n");
        
        sb.append(ThreadsTexts.TITLE_THREAD_STATS.text());
        for (Map.Entry<String, ThreadStats> entry : threadStatsMap.entrySet()) {
            ThreadStats stats = entry.getValue();
            sb.append(ThreadsTexts.LINE_THREAD_STATS.format(
                    entry.getKey(),
                    stats.cpuUsage * 100,
                    stats.state));
        }
        sb.append("\n");
        
        sb.append(Text.zhEn("===== 性能趋势 =====\n", "===== Performance trend =====\n").text());
        int sampleCount = Math.min(10, samples.size());
        int step = samples.size() / sampleCount;
        for (int i = 0; i < sampleCount; i++) {
            int index = i * step;
            ProfileSample sample = samples.get(index);
            sb.append(Text.zhEn("  [%s] CPU=%.2f%%, 内存=%s\n", "  [%s] CPU=%.2f%%, memory=%s\n").format(
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
                content.append(ThreadsTexts.TITLE_PROFILE_REPORT.text());
                content.append(ThreadsTexts.LABEL_SAMPLE_COUNT.text()).append(samples.size()).append("\n");
                content.append(ThreadsTexts.LABEL_ANALYSIS_TIME.text()).append(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date())).append("\n\n");
                
                content.append(ThreadsTexts.TITLE_SYSTEM_RESOURCES.text());
                if (!samples.isEmpty()) {
                    ProfileSample lastSample = samples.get(samples.size() - 1);
                    content.append(ThreadsTexts.LABEL_CPU_USAGE.text()).append(String.format(Locale.getDefault(), "%.2f%%", lastSample.cpuUsage * 100)).append("\n");
                    content.append(ThreadsTexts.LABEL_MEMORY_USAGE.text()).append(formatBytes(lastSample.memoryUsage)).append("\n");
                    content.append(ThreadsTexts.LABEL_THREADS.text()).append(lastSample.threadCount).append("\n");
                    content.append(ThreadsTexts.LABEL_PROCESSES.text()).append(lastSample.processCount).append("\n\n");
                }
                
                content.append(ThreadsTexts.TITLE_PROCESS_STATS.text());
                for (Map.Entry<String, ProcessStats> entry : processStatsMap.entrySet()) {
                    ProcessStats stats = entry.getValue();
                    content.append(ThreadsTexts.LINE_PROCESS_STATS.format(
                            entry.getKey(),
                            stats.cpuUsage * 100,
                            formatBytes(stats.memoryUsage),
                            stats.threadCount));
                }
                content.append("\n");
                
                content.append(ThreadsTexts.TITLE_THREAD_STATS.text());
                for (Map.Entry<String, ThreadStats> entry : threadStatsMap.entrySet()) {
                    ThreadStats stats = entry.getValue();
                    content.append(ThreadsTexts.LINE_THREAD_STATS.format(
                            entry.getKey(),
                            stats.cpuUsage * 100,
                            stats.state));
                }
                content.append("\n");
                
                content.append(Text.zhEn("===== 详细样本数据 =====\n", "===== Detailed samples =====\n").text());
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
                return Text.zhEn("[%s] CPU=%.2f%%, 内存=%s, 线程=%d, 进程=%d",
                        "[%s] CPU=%.2f%%, memory=%s, threads=%d, processes=%d").format(
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
