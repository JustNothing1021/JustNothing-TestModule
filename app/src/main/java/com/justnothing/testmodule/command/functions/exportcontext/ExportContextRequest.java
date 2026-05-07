package com.justnothing.testmodule.command.functions.exportcontext;

import com.justnothing.testmodule.command.base.parser.FlagParam;
import com.justnothing.testmodule.command.base.protocol.AutoSerializable;
import com.justnothing.testmodule.command.base.protocol.CommandRequest;
import com.justnothing.testmodule.command.base.protocol.SerializeKeyName;
import com.justnothing.testmodule.command.base.command.SubCommand;

import org.json.JSONException;
import org.json.JSONObject;

@SerializeKeyName("export_context")
@SubCommand("export-context")
@AutoSerializable
public class ExportContextRequest extends CommandRequest {

    @FlagParam(names = {"-p", "--pretty-printing"}, description = "以表格格式输出 (默认为JSON原始数据)")
    private boolean prettyPrinting = false;

    public ExportContextRequest() {
        super();
        setCommandType("export_context");
    }

    public boolean isPrettyPrinting() {
        return prettyPrinting;
    }

    public void setPrettyPrinting(boolean prettyPrinting) {
        this.prettyPrinting = prettyPrinting;
    }

    @Override
    public ExportContextRequest fromCommandLine(String[] args) {
        return this;
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
