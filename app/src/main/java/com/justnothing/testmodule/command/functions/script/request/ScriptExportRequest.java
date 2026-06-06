package com.justnothing.testmodule.command.functions.script.request;

import com.justnothing.testmodule.command.base.command.CmdParam;
import com.justnothing.testmodule.command.base.protocol.SerializeKeyName;
import org.json.JSONException;
import org.json.JSONObject;

@SerializeKeyName("script:export")
public class ScriptExportRequest extends ScriptBaseRequest {

    @CmdParam(name = "name", position = 1, required = true, description = "脚本名称")
    private String name;

    @CmdParam(name = "filePath", position = 2, required = true, description = "导出路径")
    private String filePath;

    public ScriptExportRequest() {
        super();
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getFilePath() { return filePath; }
    public void setFilePath(String filePath) { this.filePath = filePath; }

    @Override
    public JSONObject toJson() throws JSONException {
        JSONObject obj = super.toJson();
        obj.put("name", name);
        obj.put("filePath", filePath);
        return obj;
    }

    @Override
    public ScriptExportRequest fromJson(JSONObject obj) {
        setRequestId(obj.optString("requestId"));
        setName(obj.optString("name"));
        setFilePath(obj.optString("filePath"));
        return this;
    }
}
