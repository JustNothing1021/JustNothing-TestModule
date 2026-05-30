package com.justnothing.testmodule.command.functions.memory;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import com.justnothing.testmodule.command.base.protocol.CommandResult;
import com.justnothing.testmodule.command.base.protocol.SerializeKeyName;

@SerializeKeyName("Dump")
public class DumpResult extends CommandResult {

    @Expose
    @SerializedName("dumpContent")
    private String dumpContent;
    @Expose @SerializedName("filePath")
    private String filePath;
    @Expose @SerializedName("timestamp")
    private long timestamp;

    public DumpResult() {
        super();
    }

    public DumpResult(String requestId) {
        super(requestId);
    }

    public String getDumpContent() {
        return dumpContent;
    }

    public void setDumpContent(String dumpContent) {
        this.dumpContent = dumpContent;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}
