package com.justnothing.testmodule.command.functions.script.request;

import com.justnothing.testmodule.command.base.command.CmdParam;
import com.justnothing.testmodule.command.base.protocol.SerializeKeyName;
import org.json.JSONException;
import org.json.JSONObject;

@SerializeKeyName("script:import")
public class ScriptImportRequest extends ScriptBaseRequest {

    @CmdParam(name = "filePath", position = 1, required = true, description = "导入文件路径")
    private String filePath;

    public ScriptImportRequest() {
        super();
    }

    public String getFilePath() { return filePath; }
    public void setFilePath(String filePath) { this.filePath = filePath; }

    @Override
    public JSONObject toJson() throws JSONException {
        JSONObject obj = super.toJson();
        obj.put("filePath", filePath);
        return obj;
    }

    @Override
    public ScriptImportRequest fromJson(JSONObject obj) {
        setRequestId(obj.optString("requestId"));
        setFilePath(obj.optString("filePath"));
        return this;
    }
}
