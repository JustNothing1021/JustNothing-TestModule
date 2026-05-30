package com.justnothing.testmodule.command.functions.network.request;

import com.justnothing.testmodule.command.base.command.CmdParam;
import com.justnothing.testmodule.command.base.protocol.CommandRequest;
import com.justnothing.testmodule.command.base.protocol.SerializeKeyName;
import org.json.JSONException;
import org.json.JSONObject;

@SerializeKeyName("network:export")
public class NetworkExportRequest extends CommandRequest {

    @CmdParam(
        name = "filePath",
        position = 1,
        required = false,
        defaultValue = "/sdcard/network_log.json",
        description = "导出文件路径",
        serializedName = "filePath"
    )
    private String filePath = "/sdcard/network_log.json";

    public NetworkExportRequest() {
        super();
    }

    public String getFilePath() { return filePath; }
    public void setFilePath(String filePath) { this.filePath = filePath; }

    @Override
    public JSONObject toJson() throws JSONException {
        JSONObject obj = super.toJson();
        if (filePath != null) obj.put("filePath", filePath);
        return obj;
    }

    @Override
    public NetworkExportRequest fromJson(JSONObject obj) {
        setRequestId(obj.optString("requestId"));
        setFilePath(obj.optString("filePath", "/sdcard/network_log.json"));
        return this;
    }
}
