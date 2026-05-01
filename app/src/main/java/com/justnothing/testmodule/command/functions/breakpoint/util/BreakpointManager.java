package com.justnothing.testmodule.command.functions.breakpoint.util;

import com.justnothing.testmodule.command.functions.intercept.AbstractInterceptManager;
import com.justnothing.testmodule.command.functions.intercept.BreakpointInterceptTask;
import com.justnothing.testmodule.command.functions.intercept.TaskType;
import com.justnothing.testmodule.utils.reflect.ClassResolver;

public class BreakpointManager extends AbstractInterceptManager<BreakpointInterceptTask> {

    private static final BreakpointManager instance = new BreakpointManager();

    private BreakpointManager() {
        super();
    }

    public static BreakpointManager getInstance() {
        return instance;
    }

    @Override
    public TaskType getTaskType() {
        return TaskType.BREAKPOINT;
    }

    @Override
    public String getManagerName() {
        return "BreakpointManager";
    }

    public int addBreakpoint(String className, String methodName, String signature, ClassLoader classLoader) {
        try {
            logger.debug("尝试加载类: " + className + " 使用类加载器: " + classLoader);
            ClassResolver.findClassOrFail(className, classLoader);
            logger.debug("成功加载类: " + className);

            logger.info("创建断点: 类=" + className + ", 方法=" + methodName + 
                    (signature != null ? ", 签名=" + signature : ""));

            BreakpointInterceptTask task = new BreakpointInterceptTask(
                    0, className, methodName, signature, classLoader
            );
            return addTask(task);
        } catch (Exception e) {
            logger.error("添加断点失败", e);
            throw new RuntimeException("添加断点失败: " + e.getMessage(), e);
        }
    }

    @Override
    protected String getTaskOutputInternal(BreakpointInterceptTask task, int limit) {
        return task.getBreakpointInfo();
    }
}
