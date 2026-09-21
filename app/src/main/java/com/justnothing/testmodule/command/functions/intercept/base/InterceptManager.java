package com.justnothing.testmodule.command.functions.intercept.base;

import com.justnothing.testmodule.command.framework.output.ICommandOutputHandler;
import com.justnothing.testmodule.command.functions.intercept.InterceptTexts;

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
            return InterceptTexts.TASK_NOT_FOUND.format(id);
        }
        return task.toString();
    }

    default void printTaskInfo(int id, ICommandOutputHandler output) {
        output.println(getTaskInfo(id));
    }
}
