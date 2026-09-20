package com.justnothing.testmodule.command.functions.threads.response;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class ThreadProfileStopResult extends ThreadCommandResult {

    @Expose @SerializedName("success")
    private boolean success;
    @Expose @SerializedName("message")
    private String message;

    public ThreadProfileStopResult() {
        super();
    }

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}
