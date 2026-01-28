package com.justnothing.testmodule.command.functions.threads;

import com.justnothing.testmodule.utils.functions.Logger;

import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.content.Context;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

public class ProfileTask implements Runnable {

    public static class ProfileTaskLogger extends Logger {
        @Override
        public String getTag() {
            return "ProfileTask";
        }
    }

    private static final ProfileTaskLogger logger = new ProfileTaskLogger();
    private final int duration;
    private final ProfileManager manager;
    private final AtomicBoolean running;
    private final long sampleInterval = 1000;

    public ProfileTask(int duration, ProfileManager manager) {
        this.duration = duration;
        this.manager = manager;
        this.running = new AtomicBoolean(false);
    }

    @Override
    public void run() {
        running.set(true);
        logger.info("Profile任务开始运行，持续时间: " + duration + "秒");

        try {
            int sampleCount = 0;
            long startTime = System.currentTimeMillis();
            
            while (running.get() && (System.currentTimeMillis() - startTime) < duration * 1000) {
                try {
                    ProfileManager.ProfileSample sample = collectSample();
                    manager.addSample(sample);
                    
                    collectProcessStats();
                    collectThreadStats();
                    
                    sampleCount++;
                    logger.debug("采集样本 " + sampleCount + ": CPU=" + 
                            String.format("%.2f%%", sample.cpuUsage * 100) + 
                            ", 内存=" + formatBytes(sample.memoryUsage));
                    
                    Thread.sleep(sampleInterval);
                } catch (InterruptedException e) {
                    logger.info("Profile任务被中断");
                    break;
                } catch (Exception e) {
                    logger.error("采集样本失败", e);
                }
            }
            
            logger.info("Profile任务完成，共采集 " + sampleCount + " 个样本");
        } finally {
            running.set(false);
        }
    }

    private ProfileManager.ProfileSample collectSample() {
        String timestamp = new SimpleDateFormat("HH:mm:ss.SSS").format(new Date());
        double cpuUsage = getCpuUsage();
        long memoryUsage = getMemoryUsage();
        int threadCount = getThreadCount();
        int processCount = getProcessCount();
        
        return new ProfileManager.ProfileSample(timestamp, cpuUsage, memoryUsage, threadCount, processCount);
    }

    private double getCpuUsage() {
        try {
            String cpuLine = readCpuStat();
            if (cpuLine != null) {
                String[] parts = cpuLine.split("\\s+");
                if (parts.length >= 8) {
                    long idle = Long.parseLong(parts[4]);
                    long total = 0;
                    for (int i = 2; i < parts.length; i++) {
                        total += Long.parseLong(parts[i]);
                    }
                    
                    return 1.0 - (double) idle / total;
                }
            }
        } catch (Exception e) {
            logger.error("获取CPU使用率失败", e);
        }
        return 0.0;
    }

    private String readCpuStat() {
        try (BufferedReader reader = new BufferedReader(new FileReader("/proc/stat"))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("cpu ")) {
                    return line;
                }
            }
        } catch (IOException e) {
            logger.error("读取CPU统计信息失败", e);
        }
        return null;
    }

    private long getMemoryUsage() {
        try {
            Runtime runtime = Runtime.getRuntime();
            return runtime.totalMemory() - runtime.freeMemory();
        } catch (Exception e) {
            logger.error("获取内存使用失败", e);
            return 0;
        }
    }

    private int getThreadCount() {
        try {
            Map<Thread, StackTraceElement[]> allStackTraces = Thread.getAllStackTraces();
            return allStackTraces.size();
        } catch (Exception e) {
            logger.error("获取线程数失败", e);
            return 0;
        }
    }

    private int getProcessCount() {
        try {
            int count = 0;
            try (BufferedReader reader = new BufferedReader(new FileReader("/proc"))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.matches("\\d+")) {
                        count++;
                    }
                }
            }
            return count;
        } catch (Exception e) {
            logger.error("获取进程数失败", e);
            return 0;
        }
    }

    private void collectProcessStats() {
        try {
            Context appContext = getApplicationContext();
            if (appContext == null) {
                return;
            }

            ActivityManager activityManager = (ActivityManager) appContext.getSystemService(Context.ACTIVITY_SERVICE);
            if (activityManager == null) {
                return;
            }

            java.util.List<android.app.ActivityManager.RunningAppProcessInfo> processes = activityManager.getRunningAppProcesses();
            if (processes != null) {
                for (android.app.ActivityManager.RunningAppProcessInfo process : processes) {
                    String packageName = process.processName;
                    double cpuUsage = 0.0;
                    long memoryUsage = 0;
                    int threadCount = 0;

                    if (process.pid > 0) {
                        try {
                            android.os.Debug.MemoryInfo[] memoryInfos = activityManager.getProcessMemoryInfo(new int[]{process.pid});
                            if (memoryInfos.length > 0) {
                                memoryUsage = memoryInfos[0].getTotalPss() * 1024L;
                            }
                        } catch (Exception e) {
                            logger.error("获取进程内存信息失败: " + packageName, e);
                        }
                    }

                    ProfileManager.ProcessStats stats = new ProfileManager.ProcessStats(packageName, cpuUsage, memoryUsage, threadCount);
                    manager.updateProcessStats(packageName, stats);
                }
            }
        } catch (Exception e) {
            logger.error("收集进程统计信息失败", e);
        }
    }

    private void collectThreadStats() {
        try {
            Map<Thread, StackTraceElement[]> allStackTraces = Thread.getAllStackTraces();
            
            for (Map.Entry<Thread, StackTraceElement[]> entry : allStackTraces.entrySet()) {
                Thread thread = entry.getKey();
                String threadName = thread.getName();
                Thread.State state = thread.getState();
                double cpuUsage = 0.0;

                ProfileManager.ThreadStats stats = new ProfileManager.ThreadStats(threadName, cpuUsage, state.toString());
                manager.updateThreadStats(threadName, stats);
            }
        } catch (Exception e) {
            logger.error("收集线程统计信息失败", e);
        }
    }

    @SuppressLint("PrivateApi")
    private static Context getApplicationContext() {
        try {
            Class<?> activityThreadClass = Class.forName("android.app.ActivityThread");
            java.lang.reflect.Method currentActivityThreadMethod = activityThreadClass.getMethod("currentActivityThread");
            Object activityThread = currentActivityThreadMethod.invoke(null);
            
            java.lang.reflect.Method getApplicationMethod = activityThreadClass.getMethod("getApplication");
            return (Context) getApplicationMethod.invoke(activityThread);
        } catch (Exception e) {
            return null;
        }
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

    public void stop() {
        running.set(false);
    }

    public boolean isRunning() {
        return running.get();
    }
}
