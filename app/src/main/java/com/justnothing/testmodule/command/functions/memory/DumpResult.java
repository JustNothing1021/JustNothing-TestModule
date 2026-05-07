package com.justnothing.testmodule.command.functions.memory;

import com.justnothing.testmodule.command.base.protocol.AutoSerializable;
import com.justnothing.testmodule.command.base.protocol.CommandResult;
import com.justnothing.testmodule.command.base.protocol.SerializeKeyName;

import org.json.JSONException;
import org.json.JSONObject;

@SerializeKeyName("Dump")
@AutoSerializable
public class DumpResult extends CommandResult {

    private String dumpContent;
    private String filePath;
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

    @Override
    public JSONObject toJson() throws JSONException {
        JSONObject obj = super.toJson();
        obj.put("timestamp", timestamp);
        if (filePath != null) {
            obj.put("filePath", filePath);
        }
        if (dumpContent != null) {
            obj.put("dumpContent", dumpContent);
        }
        return obj;
    }

    @Override
    public void fromJson(JSONObject obj) throws JSONException {
        super.fromJson(obj);
        timestamp = obj.optLong("timestamp", 0);
        filePath = obj.optString("filePath", null);
        dumpContent = obj.optString("dumpContent", null);
    }
}
