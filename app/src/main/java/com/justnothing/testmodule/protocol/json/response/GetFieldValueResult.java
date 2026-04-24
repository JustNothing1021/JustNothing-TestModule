package com.justnothing.testmodule.protocol.json.response;

import org.json.JSONObject;

public class GetFieldValueResult extends CommandResult {
    
    private String valueString;
    private String valueTypeName;
    private int valueHash;
    
    public GetFieldValueResult() {
        super();
    }
    
    public GetFieldValueResult(String requestId) {
        super(requestId);
    }
    
    public String getValueString() { return valueString; }
    public void setValueString(String valueString) { this.valueString = valueString; }
    public String getValueTypeName() { return valueTypeName; }
    public void setValueTypeName(String valueTypeName) { this.valueTypeName = valueTypeName; }
    public int getValueHash() { return valueHash; }
    public void setValueHash(int valueHash) { this.valueHash = valueHash; }
    
    @Override
    public JSONObject toJson() throws org.json.JSONException {
        JSONObject obj = super.toJson();
        if (valueString != null) {
            obj.put("valueString", valueString);
        }
        if (valueTypeName != null) {
            obj.put("valueTypeName", valueTypeName);
        }
        obj.put("valueHash", valueHash);
        return obj;
    }
    
    @Override
    public void fromJson(JSONObject obj) throws org.json.JSONException {
        super.fromJson(obj);
        valueString = obj.optString("valueString", null);
        valueTypeName = obj.optString("valueTypeName", null);
        valueHash = obj.optInt("valueHash", 0);
    }
}
