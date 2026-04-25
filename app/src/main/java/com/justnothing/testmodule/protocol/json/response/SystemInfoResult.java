package com.justnothing.testmodule.protocol.json.response;

import com.justnothing.testmodule.protocol.json.model.SystemFieldInfo;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class SystemInfoResult extends CommandResult {

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

    @Override
    public JSONObject toJson() throws JSONException {
        JSONObject obj = super.toJson();
        if (fields != null) {
            JSONArray arr = new JSONArray();
            for (SystemFieldInfo field : fields) {
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
                fields.add(SystemFieldInfo.fromJson(arr.getJSONObject(i)));
            }
        }
    }
}
