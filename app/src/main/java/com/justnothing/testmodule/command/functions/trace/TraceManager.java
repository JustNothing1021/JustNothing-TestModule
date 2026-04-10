package com.justnothing.testmodule.command.functions.trace;

import com.justnothing.testmodule.command.functions.intercept.InterceptTaskManager;
import com.justnothing.testmodule.command.functions.intercept.TaskType;
import com.justnothing.testmodule.command.functions.intercept.TraceInterceptTask;
import com.justnothing.testmodule.utils.functions.Logger;
import com.justnothing.testmodule.utils.reflect.ClassResolver;

import java.util.List;
import java.util.stream.Collectors;

public class TraceManager {

    private static final TraceManager instance = new TraceManager();
    private static final Logger logger = Logger.getLoggerForName("TraceManager");
    private static final int MAX_CALL_RECORDS = 1000;

    private final InterceptTaskManager taskManager;

    private TraceManager() {
        this.taskManager = InterceptTaskManager.getInstance();
    }

    public static TraceManager getInstance() {
        return instance;
    }

    public int addTraceTask(String className, String methodName, String signature, ClassLoader classLoader) {
        try {
            logger.debug("尝试加载类: " + className + " 使用类加载器: " + classLoader);
            ClassResolver.findClassOrFail(className, classLoader);
            logger.debug("成功加载类: " + className);

            logger.info("创建trace任务: 类=" + className + ", 方法=" + methodName + 
                    (signature != null ? ", 签名=" + signature : ""));

            TraceInterceptTask task = new TraceInterceptTask(
                    0, className, methodName, signature, classLoader, MAX_CALL_RECORDS
            );
            return taskManager.addAndStartTask(task);
        } catch (Exception e) {
            logger.error("添加trace任务失败", e);
            throw new RuntimeException("添加trace任务失败: " + e.getMessage(), e);
        }
    }

    public boolean stopTask(int id) {
        return taskManager.stopTask(id);
    }

    public void clearAllTasks() {
        taskManager.clearByType(TaskType.TRACE);
    }

    public List<TraceInterceptTask> listTasks() {
        return taskManager.getTasksByType(TaskType.TRACE)
                .stream()
                .map(t -> (TraceInterceptTask) t)
                .collect(Collectors.toList());
    }

    public TraceInterceptTask getTask(int id) {
        return taskManager.getTask(id, TraceInterceptTask.class);
    }

    public int getTraceCount() {
        return taskManager.getTaskCount(TaskType.TRACE);
    }

    public boolean hasTrace(int id) {
        return taskManager.getTask(id, TraceInterceptTask.class) != null;
    }

    public void shutdown() {
        logger.info("关闭TraceManager");
        clearAllTasks();
    }
}
