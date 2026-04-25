package com.justnothing.testmodule.protocol.json.model;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class ContextFieldInfo {

    private String category;
    private String label;
    private String value;

    public ContextFieldInfo() {
    }

    public ContextFieldInfo(String category, String label, String value) {
        this.category = category;
        this.label = label;
        this.value = value;
    }

    public static ContextFieldInfo fromJson(JSONObject obj) throws JSONException {
        ContextFieldInfo info = new ContextFieldInfo();
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
