package com.justnothing.testmodule.protocol.json.model;

import org.json.JSONException;
import org.json.JSONObject;

public class SystemFieldInfo {

    private String category;
    private String label;
    private String value;

    public SystemFieldInfo() {
    }

    public SystemFieldInfo(String category, String label, String value) {
        this.category = category;
        this.label = label;
        this.value = value;
    }

    public static SystemFieldInfo fromJson(JSONObject obj) throws JSONException {
        SystemFieldInfo info = new SystemFieldInfo();
        info.setCategory(obj.optString("category"));
        info.setLabel(obj.optString("label"));
        info.setValue(obj.optString("value"));
        return info;
    }

    public JSONObject toJson() throws JSONException {
        JSONObject obj = new JSONObject();
        obj.put("category", category);
        obj.put("label", label);
        obj.put("value", value != null ? value : "");
        return obj;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}
