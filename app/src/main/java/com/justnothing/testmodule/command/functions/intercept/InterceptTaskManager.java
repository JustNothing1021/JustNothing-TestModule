package com.justnothing.testmodule.command.functions.intercept;

import com.justnothing.testmodule.utils.concurrent.ThreadPoolManager;
import com.justnothing.testmodule.utils.functions.Logger;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class InterceptTaskManager {

    private static final InterceptTaskManager instance = new InterceptTaskManager();

    private final ConcurrentHashMap<Integer, InterceptTask> allTasks = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<TaskType, ConcurrentHashMap<Integer, InterceptTask>> tasksByType = new ConcurrentHashMap<>();
    private final AtomicInteger nextId = new AtomicInteger(1);
    private final Logger logger = Logger.getLoggerForName("InterceptTaskManager");

    private InterceptTaskManager() {
        for (TaskType type : TaskType.values()) {
            tasksByType.put(type, new ConcurrentHashMap<>());
        }
    }

    public static InterceptTaskManager getInstance() {
        return instance;
    }

    public int addTask(InterceptTask task) {
        int id = nextId.getAndIncrement();
        task.setId(id);
        allTasks.put(id, task);
        Objects.requireNonNull(tasksByType.get(task.getType()), "没有找到对应的任务类型: " + task.getType().getCommandName()).put(id, task);
        logger.info("添加任务: " + id + " (" + task.getType().getCommandName() + ") " + task.getDisplayName());
        return id;
    }

    public int addAndStartTask(InterceptTask task) {
        int id = addTask(task);
        ThreadPoolManager.submitFastRunnable(() -> {
            try {
                task.start();
            } catch (Exception e) {
                logger.error("启动任务失败: " + id, e);
            }
        });
        return id;
    }

    public boolean stopTask(int id) {
        InterceptTask task = allTasks.remove(id);
        if (task != null) {
            Objects.requireNonNull(tasksByType.get(task.getType()), "没有找到对应的任务类型: " + task.getType().getCommandName()).remove(id);
            task.stop();
            logger.info("停止任务: " + id);
            return true;
        }
        logger.warn("未找到任务: " + id);
        return false;
    }

    public void pauseTask(int id) {
        InterceptTask task = allTasks.get(id);
        if (task != null) {
            task.setEnabled(false);
            logger.info("暂停任务: " + id);
        }
    }

    public void resumeTask(int id) {
        InterceptTask task = allTasks.get(id);
        if (task != null) {
            task.setEnabled(true);
            logger.info("恢复任务: " + id);
        }
    }

    public void clearAll() {
        logger.info("清除所有任务，共 " + allTasks.size() + " 个");
        for (InterceptTask task : allTasks.values()) {
            task.stop();
        }
        allTasks.clear();
        for (ConcurrentHashMap<Integer, InterceptTask> typeMap : tasksByType.values()) {
            typeMap.clear();
        }
        nextId.set(1);
    }

    public void clearByType(TaskType type) {
        ConcurrentHashMap<Integer, InterceptTask> typeTasks = tasksByType.get(type);
        if (typeTasks != null) {
            logger.info("清除" + type.getDescription() + "任务，共 " + typeTasks.size() + " 个");
            for (InterceptTask task : typeTasks.values()) {
                task.stop();
                allTasks.remove(task.getId());
            }
            typeTasks.clear();
        }
    }

    @SuppressWarnings("unchecked")
    public <T extends InterceptTask> T getTask(int id) {
        return (T) allTasks.get(id);
    }

    @SuppressWarnings("unchecked")
    public <T extends InterceptTask> T getTask(int id, Class<T> taskClass) {
        InterceptTask task = allTasks.get(id);
        if (taskClass.isInstance(task)) {
            return (T) task;
        }
        return null;
    }

    public boolean hasTask(int id) {
        return allTasks.containsKey(id);
    }

    public int getTaskCount() {
        return allTasks.size();
    }

    public int getTaskCount(TaskType type) {
        ConcurrentHashMap<Integer, InterceptTask> typeTasks = tasksByType.get(type);
        return typeTasks != null ? typeTasks.size() : 0;
    }

    public Collection<InterceptTask> getAllTasks() {
        return allTasks.values();
    }

    public List<InterceptTask> getTasksByType(TaskType type) {
        ConcurrentHashMap<Integer, InterceptTask> typeTasks = tasksByType.get(type);
        return typeTasks != null ? new ArrayList<>(typeTasks.values()) : new ArrayList<>();
    }

    public List<Map<String, Object>> getTaskSummaryList() {
        List<Map<String, Object>> summaryList = new ArrayList<>();
        for (InterceptTask task : allTasks.values()) {
            Map<String, Object> summary = new ConcurrentHashMap<>();
            summary.put("id", task.getId());
            summary.put("className", task.getClassName());
            summary.put("methodName", task.getMethodName());
            summary.put("signature", task.getSignature());
            summary.put("enabled", task.isEnabled());
            summary.put("running", task.isRunning());
            summary.put("hitCount", task.getHitCount());
            summary.put("type", task.getType().getCommandName());
            summaryList.add(summary);
        }
        return summaryList;
    }

    public String getTaskListString() {
        if (allTasks.isEmpty()) {
            return "当前没有活跃的拦截任务";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("=== 活跃的拦截任务 ===\n");
        sb.append("总计: ").append(allTasks.size()).append(" 个任务\n\n");

        for (TaskType type : TaskType.values()) {
            List<InterceptTask> typeTasks = getTasksByType(type);
            if (!typeTasks.isEmpty()) {
                sb.append("【").append(type.getDescription()).append("】\n");
                for (InterceptTask task : typeTasks) {
                    sb.append("  ").append(task.toString()).append("\n");
                }
                sb.append("\n");
            }
        }

        return sb.toString();
    }

    public String getTaskListString(TaskType type) {
        List<InterceptTask> typeTasks = getTasksByType(type);
        if (typeTasks.isEmpty()) {
            return "当前没有活跃的" + type.getDescription() + "任务";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("=== 活跃的").append(type.getDescription()).append("任务 ===\n");
        sb.append("总计: ").append(typeTasks.size()).append(" 个任务\n\n");

        for (InterceptTask task : typeTasks) {
            sb.append(task.toString()).append("\n");
        }

        return sb.toString();
    }

    public void shutdown() {
        logger.info("关闭InterceptTaskManager");
        clearAll();
    }
}
