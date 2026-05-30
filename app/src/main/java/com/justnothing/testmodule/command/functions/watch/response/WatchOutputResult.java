package com.justnothing.testmodule.command.functions.watch.response;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class WatchOutputResult extends WatchCommandResult {

    @Expose @SerializedName("taskId")
    private Integer taskId;
    @Expose @SerializedName("output")
    private String output;
    @Expose @SerializedName("limit")
    private Integer limit;

    public WatchOutputResult() {
        super();
    }

    public Integer getTaskId() { return taskId; }
    public void setTaskId(Integer taskId) { this.taskId = taskId; }

    public String getOutput() { return output; }
    public void setOutput(String output) { this.output = output; }

    public Integer getLimit() { return limit; }
    public void setLimit(Integer limit) { this.limit = limit; }
}
