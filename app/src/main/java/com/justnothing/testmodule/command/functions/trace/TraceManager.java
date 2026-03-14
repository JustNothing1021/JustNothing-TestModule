package com.justnothing.testmodule.command.functions.trace;

import com.justnothing.testmodule.utils.reflect.ClassResolver;
import com.justnothing.testmodule.utils.functions.Logger;
import com.justnothing.testmodule.utils.concurrent.ThreadPoolManager;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;


public class TraceManager {

    private static final TraceManager instance = new TraceManager();
    private static final Logger logger = Logger.getLoggerForName("TraceManager");
    
    private final ConcurrentHashMap<Integer, TraceTask> traceTasks;
    private final AtomicInteger nextId;
    private final int maxCallRecords;
    
    private TraceManager() {
        this.traceTasks = new ConcurrentHashMap<>();
        this.nextId = new AtomicInteger(1);
        this.maxCallRecords = 1000;
    }
    
    public static TraceManager getInstance() {
        return instance;
    }
    
    public int addTraceTask(String className, String methodName, String signature, ClassLoader classLoader) {
        try {
            logger.debug("尝试加载类: " + className + " 使用类加载器: " + classLoader);
            Class<?> targetClass = ClassResolver.findClassOrFail(className, classLoader);
            logger.debug("成功加载类: " + targetClass.getName());
            
            int id = nextId.getAndIncrement();
            logger.info("创建trace任务: ID=" + id + ", 类=" + className + ", 方法=" + methodName + (signature != null ? ", 签名=" + signature : ""));
            
            TraceTask task = new TraceTask(id, targetClass, methodName, signature, maxCallRecords, classLoader);
            traceTasks.put(id, task);
            ThreadPoolManager.submitFastRunnable(task);
            logger.info("成功添加trace任务: " + id);
            return id;
        } catch (Exception e) {
            logger.error("添加trace任务失败", e);
            throw new RuntimeException("添加trace任务失败: " + e.getMessage(), e);
        }
    }
    
    public boolean stopTask(int id) {
        TraceTask task = traceTasks.get(id);
        if (task != null) {
            task.stop();
            traceTasks.remove(id);
            logger.info("停止trace任务: " + id);
            return true;
        }
        logger.warn("未找到trace任务: " + id);
        return false;
    }
    
    public void clearAllTasks() {
        logger.info("清除所有trace任务，共 " + traceTasks.size() + " 个");
        for (TraceTask task : traceTasks.values()) {
            task.stop();
        }
        traceTasks.clear();
    }
    
    public java.util.List<TraceTask> listTasks() {
        return new java.util.ArrayList<>(traceTasks.values());
    }
    
    public TraceTask getTask(int id) {
        return traceTasks.get(id);
    }
    
    public int getTraceCount() {
        return traceTasks.size();
    }
    
    public boolean hasTrace(int id) {
        return traceTasks.containsKey(id);
    }
    
    public void shutdown() {
        logger.info("关闭TraceManager");
        clearAllTasks();
    }
}
