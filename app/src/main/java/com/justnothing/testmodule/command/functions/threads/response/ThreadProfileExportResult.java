package com.justnothing.testmodule.command.functions.threads.response;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class ThreadProfileExportResult extends ThreadCommandResult {

    @Expose @SerializedName("filePath")
    private String filePath;
    @Expose @SerializedName("success")
    private boolean success;

    public ThreadProfileExportResult() {
        super();
    }

    public String getFilePath() { return filePath; }
    public void setFilePath(String filePath) { this.filePath = filePath; }

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }
}
