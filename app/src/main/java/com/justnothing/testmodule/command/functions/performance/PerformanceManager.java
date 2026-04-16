package com.justnothing.testmodule.command.functions.performance;

import com.justnothing.testmodule.command.functions.intercept.InterceptTaskManager;
import com.justnothing.testmodule.command.functions.intercept.PerformanceInterceptTask;
import com.justnothing.testmodule.command.functions.intercept.TaskType;
import com.justnothing.testmodule.utils.logging.Logger;
import com.justnothing.testmodule.utils.reflect.ClassResolver;

import java.util.List;
import java.util.stream.Collectors;

public class PerformanceManager {

    private static final PerformanceManager instance = new PerformanceManager();
    private static final Logger logger = Logger.getLoggerForName("PerformanceManager");

    private final InterceptTaskManager taskManager;

    private PerformanceManager() {
        this.taskManager = InterceptTaskManager.getInstance();
    }

    public static PerformanceManager getInstance() {
        return instance;
    }

    public int addPerformanceHook(String className, String methodName, String signature, ClassLoader classLoader) {
        try {
            logger.debug("尝试加载类: " + className + " 使用类加载器: " + classLoader);
            ClassResolver.findClassOrFail(className, classLoader);
            logger.debug("成功加载类: " + className);

            logger.info("创建性能监控: 类=" + className + ", 方法=" + methodName + 
                    (signature != null ? ", 签名=" + signature : ""));

            PerformanceInterceptTask task = new PerformanceInterceptTask(
                    0, className, methodName, signature, classLoader
            );
            return taskManager.addAndStartTask(task);
        } catch (Exception e) {
            logger.error("添加性能监控失败", e);
            throw new RuntimeException("添加性能监控失败: " + e.getMessage(), e);
        }
    }

    public boolean stopPerformanceHook(int id) {
        return taskManager.stopTask(id);
    }

    public void clearAll() {
        taskManager.clearByType(TaskType.PERFORMANCE);
    }

    public List<PerformanceInterceptTask> listPerformanceHooks() {
        return taskManager.getTasksByType(TaskType.PERFORMANCE)
                .stream()
                .map(t -> (PerformanceInterceptTask) t)
                .collect(Collectors.toList());
    }

    public PerformanceInterceptTask getPerformanceHook(int id) {
        return taskManager.getTask(id, PerformanceInterceptTask.class);
    }

    public PerformanceInterceptTask.PerformanceStats getStats(int id) {
        PerformanceInterceptTask task = getPerformanceHook(id);
        return task != null ? task.getStats() : null;
    }

    public int getPerformanceHookCount() {
        return taskManager.getTaskCount(TaskType.PERFORMANCE);
    }

    public boolean hasPerformanceHook(int id) {
        return taskManager.getTask(id, PerformanceInterceptTask.class) != null;
    }

    public void shutdown() {
        logger.info("关闭PerformanceManager");
        clearAll();
    }
}
