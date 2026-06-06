package com.justnothing.testmodule.command.agent;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import com.justnothing.testmodule.command.base.protocol.CommandResult;
import com.justnothing.testmodule.command.base.protocol.SerializeKeyName;

@SerializeKeyName("SpWriteResult")
public class SpWriteResult extends CommandResult {

    @Expose @SerializedName("spName")
    private String spName;

    @Expose @SerializedName("key")
    private String key;

    @Expose @SerializedName("committed")
    private boolean committed;

    public SpWriteResult() {
        super();
    }

    public SpWriteResult(boolean committed) {
        super();
        this.committed = committed;
        setSuccess(committed);
    }

    public String getSpName() { return spName; }
    public void setSpName(String spName) { this.spName = spName; }

    public String getKey() { return key; }
    public void setKey(String key) { this.key = key; }

    public boolean isCommitted() { return committed; }
    public void setCommitted(boolean committed) { this.committed = committed; }
}
