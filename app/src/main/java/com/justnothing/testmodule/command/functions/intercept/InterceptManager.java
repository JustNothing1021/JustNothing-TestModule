package com.justnothing.testmodule.command.functions.intercept;

import com.justnothing.testmodule.command.output.ICommandOutputHandler;

import java.util.List;

public interface InterceptManager<T extends InterceptTask> {

    TaskType getTaskType();

    String getManagerName();

    int addTask(T task);

    boolean removeTask(int id);

    boolean enableTask(int id);

    boolean disableTask(int id);

    void clearAll();

    List<T> listTasks();

    T getTask(int id);

    int getTaskCount();

    boolean hasTask(int id);

    String getTaskListString();

    void printTaskList(ICommandOutputHandler output);

    String getTaskOutput(int id, int limit);

    void shutdown();

    default String getTaskInfo(int id) {
        T task = getTask(id);
        if (task == null) {
            return "未找到任务: " + id;
        }
        return task.toString();
    }

    default void printTaskInfo(int id, ICommandOutputHandler output) {
        output.println(getTaskInfo(id));
    }
}
