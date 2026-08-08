package com.justnothing.testmodule.command.functions.exportcontext;

import com.justnothing.testmodule.command.framework.annotation.CmdParam;
import com.justnothing.testmodule.command.framework.model.CommandRequest;
import com.justnothing.testmodule.command.framework.annotation.SerializeKeyName;
import org.json.JSONException;
import org.json.JSONObject;

@SerializeKeyName("export_context")
public class ExportContextRequest extends CommandRequest {

    @CmdParam(
        name = "prettyPrinting",
        aliases = {"-p", "--pretty-printing"},
        required = false,
        description = "以表格格式输出 (默认为JSON原始数据)"
    )
    private Boolean prettyPrinting = false;

    public ExportContextRequest() {
        super();
    }

    public boolean isPrettyPrinting() {
        return prettyPrinting != null && prettyPrinting;
    }

    public void setPrettyPrinting(boolean prettyPrinting) {
        this.prettyPrinting = prettyPrinting;
    }

    @Override
    public JSONObject toJson() throws JSONException {
        JSONObject obj = super.toJson();
        obj.put("prettyPrinting", prettyPrinting);
        return obj;
    }

    @Override
    public ExportContextRequest fromJson(JSONObject obj) throws JSONException {
        super.fromJson(obj);
        setPrettyPrinting(obj.optBoolean("prettyPrinting", false));
        return this;
    }
}
