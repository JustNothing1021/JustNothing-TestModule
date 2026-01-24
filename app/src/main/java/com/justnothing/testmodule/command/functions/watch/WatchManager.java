package com.justnothing.testmodule.command.functions.watch;

import com.justnothing.testmodule.utils.functions.Logger;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import de.robv.android.xposed.XposedHelpers;

public class WatchManager {
    
    public static class WatchManagerLogger extends Logger {
        @Override
        public String getTag() {
            return "WatchManager";
        }
    }
    
    private static final WatchManager instance = new WatchManager();
    private static final WatchManagerLogger logger = new WatchManagerLogger();
    
    private final ConcurrentHashMap<Integer, WatchTask> watchTasks;
    private final AtomicInteger nextId;
    private final ExecutorService executor;
    private final int maxOutputSize;
    
    private WatchManager() {
        this.watchTasks = new ConcurrentHashMap<>();
        this.nextId = new AtomicInteger(1);
        this.executor = Executors.newCachedThreadPool(r -> {
            Thread thread = new Thread(r, "WatchTask-" + nextId.get());
            thread.setDaemon(true);
            return thread;
        });
        this.maxOutputSize = 100;
    }
    
    public static WatchManager getInstance() {
        return instance;
    }
    
    public int addFieldWatch(ClassLoader classLoader, String className, String fieldName, long interval) {
        try {
            Class<?> targetClass = XposedHelpers.findClass(className, classLoader);
            int id = nextId.getAndIncrement();
            WatchTask task = new WatchTask(id, WatchTask.WatchType.FIELD, targetClass, fieldName, null, interval, maxOutputSize, classLoader);
            watchTasks.put(id, task);
            executor.submit(task);
            logger.info("添加字段watch任务: " + id);
            return id;
        } catch (Exception e) {
            logger.error("添加字段watch任务失败", e);
            throw new RuntimeException("添加字段watch任务失败: " + e.getMessage(), e);
        }
    }
    
    public int addMethodWatch(ClassLoader classLoader, String className, String methodName, String signature, long interval) {
        try {
            logger.debug("尝试加载类: " + className + " 使用类加载器: " + classLoader);
            Class<?> targetClass = XposedHelpers.findClass(className, classLoader);
            logger.debug("成功加载类: " + targetClass.getName());
            
            int id = nextId.getAndIncrement();
            logger.info("创建方法watch任务: ID=" + id + ", 类=" + className + ", 方法=" + methodName + (signature != null ? ", 签名=" + signature : "") + ", 间隔=" + interval + "ms");
            
            WatchTask task = new WatchTask(id, WatchTask.WatchType.METHOD, targetClass, methodName, signature, interval, maxOutputSize, classLoader);
            watchTasks.put(id, task);
            executor.submit(task);
            logger.info("成功添加方法watch任务: " + id);
            return id;
        } catch (Exception e) {
            logger.error("添加方法watch任务失败", e);
            throw new RuntimeException("添加方法watch任务失败: " + e.getMessage(), e);
        }
    }
    
    public boolean stopWatch(int id) {
        WatchTask task = watchTasks.get(id);
        if (task != null) {
            task.stop();
            watchTasks.remove(id);
            logger.info("停止watch任务: " + id);
            return true;
        }
        logger.warn("未找到watch任务: " + id);
        return false;
    }
    
    public void clearAll() {
        logger.info("清除所有watch任务，共 " + watchTasks.size() + " 个");
        for (WatchTask task : watchTasks.values()) {
            task.stop();
        }
        watchTasks.clear();
    }
    
    public String listWatches() {
        if (watchTasks.isEmpty()) {
            return "当前没有活跃的watch任务";
        }
        
        StringBuilder sb = new StringBuilder();
        sb.append("=== 活跃的Watch任务 ===\n");
        sb.append("总计: ").append(watchTasks.size()).append(" 个任务\n\n");
        
        for (WatchTask task : watchTasks.values()) {
            sb.append(task.toString()).append("\n");
        }
        
        return sb.toString();
    }
    
    public String getWatchOutput(int id, int limit) {
        WatchTask task = watchTasks.get(id);
        if (task == null) {
            return "未找到watch任务: " + id;
        }
        return task.getOutput(limit);
    }
    
    public String getAllWatchOutput(int limit) {
        if (watchTasks.isEmpty()) {
            return "当前没有活跃的watch任务";
        }
        
        StringBuilder sb = new StringBuilder();
        sb.append("=== 所有Watch任务输出 ===\n\n");
        
        for (WatchTask task : watchTasks.values()) {
            sb.append(task.getOutput(limit)).append("\n");
        }
        
        return sb.toString();
    }
    
    public int getWatchCount() {
        return watchTasks.size();
    }
    
    public boolean hasWatch(int id) {
        return watchTasks.containsKey(id);
    }
    
    public void shutdown() {
        logger.info("关闭WatchManager");
        clearAll();
        executor.shutdown();
    }
}
