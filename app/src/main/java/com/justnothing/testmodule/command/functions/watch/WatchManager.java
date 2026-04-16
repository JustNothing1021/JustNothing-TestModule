package com.justnothing.testmodule.command.functions.watch;

import com.justnothing.testmodule.command.functions.intercept.AbstractInterceptManager;
import com.justnothing.testmodule.command.functions.intercept.TaskType;
import com.justnothing.testmodule.command.functions.intercept.WatchInterceptTask;
import com.justnothing.testmodule.utils.reflect.ClassResolver;

public class WatchManager extends AbstractInterceptManager<WatchInterceptTask> {

    private static final WatchManager instance = new WatchManager();
    private static final int MAX_OUTPUT_SIZE = 100;

    private WatchManager() {
        super();
    }

    public static WatchManager getInstance() {
        return instance;
    }

    @Override
    public TaskType getTaskType() {
        return TaskType.WATCH;
    }

    @Override
    public String getManagerName() {
        return "WatchManager";
    }

    public int addFieldWatch(ClassLoader classLoader, String className, String fieldName, long interval) {
        try {
            WatchInterceptTask task = new WatchInterceptTask(
                    0, className, fieldName, null, classLoader,
                    WatchInterceptTask.WatchType.FIELD, interval, MAX_OUTPUT_SIZE
            );
            return addTask(task);
        } catch (Exception e) {
            logger.error("添加字段watch任务失败", e);
            throw new RuntimeException("添加字段watch任务失败: " + e.getMessage(), e);
        }
    }

    public int addMethodWatch(ClassLoader classLoader, String className, String methodName, String signature, long interval) {
        try {
            logger.debug("尝试加载类: " + className + " 使用类加载器: " + classLoader);
            Class<?> targetClass = ClassResolver.findClassOrFail(className, classLoader);
            logger.debug("成功加载类: " + targetClass.getName());

            logger.info("创建方法watch任务: 类=" + className + ", 方法=" + methodName + 
                    (signature != null ? ", 签名=" + signature : "") + ", 间隔=" + interval + "ms");

            WatchInterceptTask task = new WatchInterceptTask(
                    0, className, methodName, signature, classLoader,
                    WatchInterceptTask.WatchType.METHOD, interval, MAX_OUTPUT_SIZE
            );
            return addTask(task);
        } catch (Exception e) {
            logger.error("添加方法watch任务失败", e);
            throw new RuntimeException("添加方法watch任务失败: " + e.getMessage(), e);
        }
    }

    public String getAllWatchOutput(int limit) {
        StringBuilder sb = new StringBuilder();
        sb.append("=== 所有Watch任务输出 ===\n\n");

        for (WatchInterceptTask task : listTasks()) {
            sb.append(task.getOutput(limit)).append("\n");
        }

        return sb.toString();
    }

    @Override
    protected String getTaskOutputInternal(WatchInterceptTask task, int limit) {
        return task.getOutput(limit);
    }
}
