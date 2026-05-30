package com.justnothing.testmodule.command.functions.system;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import com.justnothing.testmodule.command.base.protocol.CommandResult;

import java.util.ArrayList;
import java.util.List;

public class SystemInfoResult extends CommandResult {

    @Expose @SerializedName("fields")
    private List<SystemFieldInfo> fields;

    public SystemInfoResult() {
        super();
    }

    public SystemInfoResult(String requestId) {
        super(requestId);
    }

    public List<SystemFieldInfo> getFields() {
        return fields;
    }

    public void setFields(List<SystemFieldInfo> fields) {
        this.fields = fields;
    }
}
