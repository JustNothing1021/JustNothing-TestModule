package com.justnothing.testmodule.command.functions.watch.response;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class WatchStopResult extends WatchCommandResult {

    @Expose @SerializedName("taskId")
    private Integer taskId;
    @Expose @SerializedName("success")
    private Boolean success;

    public WatchStopResult() {
        super();
    }

    public Integer getTaskId() { return taskId; }
    public void setTaskId(Integer taskId) { this.taskId = taskId; }

    public Boolean getSuccess() { return success; }
    public void setSuccess(Boolean success) { this.success = success; }
}
