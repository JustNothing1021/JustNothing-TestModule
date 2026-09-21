package com.justnothing.testmodule.command.functions.intercept.base;

import com.justnothing.testmodule.command.framework.i18n.Text;
import com.justnothing.testmodule.command.framework.output.Colors;
import com.justnothing.testmodule.command.framework.output.ICommandOutputHandler;
import com.justnothing.testmodule.command.functions.intercept.InterceptTexts;
import com.justnothing.testmodule.command.functions.intercept.util.InterceptTaskManager;
import com.justnothing.testmodule.utils.logging.Logger;

import java.util.List;
import java.util.stream.Collectors;

public abstract class AbstractInterceptManager<T extends InterceptTask> implements InterceptManager<T> {

    protected final Logger logger;
    protected final InterceptTaskManager taskManager;

    protected AbstractInterceptManager() {
        this.logger = Logger.getLoggerForName(getManagerName());
        this.taskManager = InterceptTaskManager.getInstance();
    }

    @Override
    public int addTask(T task) {
        try {
            logger.info("添加" + getTaskType().getDescription() + "任务: " + task.getDisplayName());
            return taskManager.addAndStartTask(task);
        } catch (Exception e) {
            logger.error("添加任务失败", e);
            throw new RuntimeException(Text.zhEn("添加任务失败: %s", "Failed to add task: %s")
                    .format(e.getMessage()), e);
        }
    }

    @Override
    public boolean removeTask(int id) {
        T task = getTask(id);
        if (task != null) {
            logger.info("移除任务: " + id);
            return taskManager.stopTask(id);
        }
        logger.warn("未找到任务: " + id);
        return false;
    }

    @Override
    public boolean enableTask(int id) {
        T task = getTask(id);
        if (task != null) {
            task.setEnabled(true);
            logger.info("启用任务: " + id);
            return true;
        }
        return false;
    }

    @Override
    public boolean disableTask(int id) {
        T task = getTask(id);
        if (task != null) {
            task.setEnabled(false);
            logger.info("禁用任务: " + id);
            return true;
        }
        return false;
    }

    @Override
    public void clearAll() {
        taskManager.clearByType(getTaskType());
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<T> listTasks() {
        return taskManager.getTasksByType(getTaskType())
                .stream()
                .map(t -> (T) t)
                .collect(Collectors.toList());
    }

    @Override
    @SuppressWarnings("unchecked")
    public T getTask(int id) {
        InterceptTask task = taskManager.getTask(id);
        if (task != null && task.getType() == getTaskType()) {
            return (T) task;
        }
        return null;
    }

    @Override
    public int getTaskCount() {
        return taskManager.getTaskCount(getTaskType());
    }

    @Override
    public boolean hasTask(int id) {
        return getTask(id) != null;
    }

    @Override
    public String getTaskListString() {
        return taskManager.getTaskListString(getTaskType());
    }

    @Override
    public void printTaskList(ICommandOutputHandler output) {
        List<T> tasks = listTasks();
        if (tasks.isEmpty()) {
            output.println(InterceptTexts.NO_ACTIVE_TASKS.format(getTaskType().getDescription()), Colors.GRAY);
            return;
        }

        output.println(InterceptTexts.ACTIVE_TASKS_HEADER.format(getTaskType().getDescription()), Colors.CYAN);
        output.println(InterceptTexts.LABEL_TOTAL_TASKS.format(tasks.size()), Colors.WHITE);
        output.println("", Colors.DEFAULT);

        for (T task : tasks) {
            output.print("  ", Colors.DEFAULT);
            output.print(getTaskType().getCommandName().toUpperCase(), Colors.YELLOW);
            output.print("[", Colors.GRAY);
            output.print(String.valueOf(task.getId()), Colors.LIGHT_GREEN);
            output.print("] ", Colors.GRAY);
            output.print(task.getDisplayName(), Colors.WHITE);
            output.print(Text.zhEn(" (命中: ", " (hits: ").text(), Colors.GRAY);
            output.print(String.valueOf(task.getHitCount()), Colors.CYAN);
            output.print(Text.zhEn(", 状态: ", ", status: ").text(), Colors.GRAY);
            if (task.isRunning()) {
                output.print(task.isEnabled() ? InterceptTexts.STATUS_RUNNING.text() : InterceptTexts.STATUS_PAUSED.text(), task.isEnabled() ? Colors.LIGHT_GREEN : Colors.ORANGE);
            } else {
                output.print(InterceptTexts.STATUS_STOPPED.text(), Colors.RED);
            }
            output.println(")", Colors.DEFAULT);
        }
    }

    @Override
    public String getTaskOutput(int id, int limit) {
        T task = getTask(id);
        if (task == null) {
            return InterceptTexts.TASK_NOT_FOUND.format(id);
        }
        return getTaskOutputInternal(task, limit);
    }

    protected abstract String getTaskOutputInternal(T task, int limit);

    @Override
    public void shutdown() {
        logger.info("关闭" + getManagerName());
        clearAll();
    }
}
