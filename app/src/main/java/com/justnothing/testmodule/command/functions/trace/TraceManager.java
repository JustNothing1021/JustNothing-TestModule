package com.justnothing.testmodule.command.functions.trace;

import com.justnothing.testmodule.command.functions.intercept.AbstractInterceptManager;
import com.justnothing.testmodule.command.functions.intercept.TaskType;
import com.justnothing.testmodule.command.functions.intercept.TraceInterceptTask;
import com.justnothing.testmodule.utils.reflect.ClassResolver;

public class TraceManager extends AbstractInterceptManager<TraceInterceptTask> {

    private static final TraceManager instance = new TraceManager();
    private static final int MAX_CALL_RECORDS = 1000;

    private TraceManager() {
        super();
    }

    public static TraceManager getInstance() {
        return instance;
    }

    @Override
    public TaskType getTaskType() {
        return TaskType.TRACE;
    }

    @Override
    public String getManagerName() {
        return "TraceManager";
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
            return addTask(task);
        } catch (Exception e) {
            logger.error("添加trace任务失败", e);
            throw new RuntimeException("添加trace任务失败: " + e.getMessage(), e);
        }
    }

    @Override
    protected String getTaskOutputInternal(TraceInterceptTask task, int limit) {
        return task.getTraceOutput(limit);
    }
}
