package com.justnothing.testmodule.command.functions.watch.response;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.util.List;
import java.util.Map;

public class WatchListResult extends WatchCommandResult {

    @Expose @SerializedName("tasks")
    private List<Map<String, Object>> tasks;

    public WatchListResult() {
        super();
    }

    public List<Map<String, Object>> getTasks() { return tasks; }
    public void setTasks(List<Map<String, Object>> tasks) { this.tasks = tasks; }
}
