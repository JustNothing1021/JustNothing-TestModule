package com.justnothing.testmodule.command.functions.exportcontext;

import com.justnothing.testmodule.command.base.CommandResult;
import com.justnothing.testmodule.command.functions.exportcontext.ContextFieldInfo;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class ExportContextResult extends CommandResult {

    private List<ContextFieldInfo> fields;

    public ExportContextResult() {
        super();
    }

    public ExportContextResult(String requestId) {
        super(requestId);
    }

    public List<ContextFieldInfo> getFields() {
        return fields;
    }

    public void setFields(List<ContextFieldInfo> fields) {
        this.fields = fields;
    }

    @Override
    public JSONObject toJson() throws JSONException {
        JSONObject obj = super.toJson();
        if (fields != null) {
            JSONArray arr = new JSONArray();
            for (ContextFieldInfo field : fields) {
                arr.put(field.toJson());
            }
            obj.put("fields", arr);
        }
        return obj;
    }

    @Override
    public void fromJson(JSONObject obj) throws JSONException {
        super.fromJson(obj);
        if (obj.has("fields")) {
            JSONArray arr = obj.getJSONArray("fields");
            fields = new ArrayList<>();
            for (int i = 0; i < arr.length(); i++) {
                fields.add(ContextFieldInfo.fromJson(arr.getJSONObject(i)));
            }
        }
    }
}
