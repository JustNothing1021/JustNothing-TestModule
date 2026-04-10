package com.justnothing.testmodule.command.functions.breakpoint;

import com.justnothing.testmodule.command.functions.intercept.BreakpointInterceptTask;
import com.justnothing.testmodule.command.functions.intercept.InterceptTaskManager;
import com.justnothing.testmodule.command.functions.intercept.TaskType;
import com.justnothing.testmodule.utils.functions.Logger;
import com.justnothing.testmodule.utils.reflect.ClassResolver;

import java.util.List;
import java.util.stream.Collectors;

public class BreakpointManager {

    private static final BreakpointManager instance = new BreakpointManager();
    private static final Logger logger = Logger.getLoggerForName("BreakpointManager");

    private final InterceptTaskManager taskManager;

    private BreakpointManager() {
        this.taskManager = InterceptTaskManager.getInstance();
    }

    public static BreakpointManager getInstance() {
        return instance;
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
            return taskManager.addAndStartTask(task);
        } catch (Exception e) {
            logger.error("添加断点失败", e);
            throw new RuntimeException("添加断点失败: " + e.getMessage(), e);
        }
    }

    public boolean enableBreakpoint(int id) {
        BreakpointInterceptTask task = taskManager.getTask(id, BreakpointInterceptTask.class);
        if (task != null) {
            task.setEnabled(true);
            logger.info("启用断点: " + id);
            return true;
        }
        return false;
    }

    public boolean disableBreakpoint(int id) {
        BreakpointInterceptTask task = taskManager.getTask(id, BreakpointInterceptTask.class);
        if (task != null) {
            task.setEnabled(false);
            logger.info("禁用断点: " + id);
            return true;
        }
        return false;
    }

    public boolean removeBreakpoint(int id) {
        return taskManager.stopTask(id);
    }

    public void clearAll() {
        taskManager.clearByType(TaskType.BREAKPOINT);
    }

    public List<BreakpointInterceptTask> listBreakpoints() {
        return taskManager.getTasksByType(TaskType.BREAKPOINT)
                .stream()
                .map(t -> (BreakpointInterceptTask) t)
                .collect(Collectors.toList());
    }

    public BreakpointInterceptTask getBreakpoint(int id) {
        return taskManager.getTask(id, BreakpointInterceptTask.class);
    }

    public int getBreakpointCount() {
        return taskManager.getTaskCount(TaskType.BREAKPOINT);
    }

    public boolean hasBreakpoint(int id) {
        return taskManager.getTask(id, BreakpointInterceptTask.class) != null;
    }

    public void shutdown() {
        logger.info("关闭BreakpointManager");
        clearAll();
    }
}
