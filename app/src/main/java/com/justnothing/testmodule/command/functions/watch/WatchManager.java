package com.justnothing.testmodule.command.functions.watch;

import com.justnothing.testmodule.command.functions.intercept.InterceptTaskManager;
import com.justnothing.testmodule.command.functions.intercept.TaskType;
import com.justnothing.testmodule.command.functions.intercept.WatchInterceptTask;
import com.justnothing.testmodule.utils.functions.Logger;
import com.justnothing.testmodule.utils.reflect.ClassResolver;

import java.util.List;
import java.util.stream.Collectors;

public class WatchManager {

    private static final WatchManager instance = new WatchManager();
    private static final Logger logger = Logger.getLoggerForName("WatchManager");
    private static final int MAX_OUTPUT_SIZE = 100;

    private final InterceptTaskManager taskManager;

    private WatchManager() {
        this.taskManager = InterceptTaskManager.getInstance();
    }

    public static WatchManager getInstance() {
        return instance;
    }

    public int addFieldWatch(ClassLoader classLoader, String className, String fieldName, long interval) {
        try {
            WatchInterceptTask task = new WatchInterceptTask(
                    0, className, fieldName, null, classLoader,
                    WatchInterceptTask.WatchType.FIELD, interval, MAX_OUTPUT_SIZE
            );
            return taskManager.addAndStartTask(task);
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
            return taskManager.addAndStartTask(task);
        } catch (Exception e) {
            logger.error("添加方法watch任务失败", e);
            throw new RuntimeException("添加方法watch任务失败: " + e.getMessage(), e);
        }
    }

    public boolean stopWatch(int id) {
        return taskManager.stopTask(id);
    }

    public void clearAll() {
        taskManager.clearByType(TaskType.WATCH);
    }

    public String listWatches() {
        return taskManager.getTaskListString(TaskType.WATCH);
    }

    public String getWatchOutput(int id, int limit) {
        WatchInterceptTask task = taskManager.getTask(id, WatchInterceptTask.class);
        if (task == null) {
            return "未找到watch任务: " + id;
        }
        return task.getOutput(limit);
    }

    public String getAllWatchOutput(int limit) {
        List<WatchInterceptTask> tasks = taskManager.<WatchInterceptTask>getTasksByType(TaskType.WATCH)
                .stream()
                .map(t -> (WatchInterceptTask) t)
                .collect(Collectors.toList());

        if (tasks.isEmpty()) {
            return "当前没有活跃的watch任务";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("=== 所有Watch任务输出 ===\n\n");

        for (WatchInterceptTask task : tasks) {
            sb.append(task.getOutput(limit)).append("\n");
        }

        return sb.toString();
    }

    public int getWatchCount() {
        return taskManager.getTaskCount(TaskType.WATCH);
    }

    public boolean hasWatch(int id) {
        WatchInterceptTask task = taskManager.getTask(id, WatchInterceptTask.class);
        return task != null;
    }

    public void shutdown() {
        logger.info("关闭WatchManager");
        clearAll();
    }
}
