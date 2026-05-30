package com.justnothing.testmodule.command.functions.threads.response;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class ThreadProfileStartResult extends ThreadCommandResult {

    @Expose @SerializedName("duration")
    private Integer duration;
    @Expose @SerializedName("success")
    private boolean success;

    public ThreadProfileStartResult() {
        super();
    }

    public Integer getDuration() { return duration; }
    public void setDuration(Integer duration) { this.duration = duration; }

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }
}
