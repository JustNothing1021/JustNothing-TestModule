package com.justnothing.testmodule.command.functions.classcmd.response;

import com.justnothing.testmodule.command.functions.classcmd.ClassCommandResult;

import org.json.JSONException;
import org.json.JSONObject;

public class InvokeConstructorResult extends ClassCommandResult {
    
    private String resultString;
    private String resultTypeName;
    private int resultHash;
    
    public InvokeConstructorResult() {
        super();
    }
    
    public InvokeConstructorResult(String requestId) {
        super(requestId);
    }
    
    public String getResultString() { return resultString; }
    public void setResultString(String resultString) { this.resultString = resultString; }
    public String getResultTypeName() { return resultTypeName; }
    public void setResultTypeName(String resultTypeName) { this.resultTypeName = resultTypeName; }
    public int getResultHash() { return resultHash; }
    public void setResultHash(int resultHash) { this.resultHash = resultHash; }
    
    @Override
    public JSONObject toJson() throws JSONException {
        JSONObject obj = super.toJson();
        if (resultString != null) {
            obj.put("resultString", resultString);
        }
        if (resultTypeName != null) {
            obj.put("resultTypeName", resultTypeName);
        }
        obj.put("resultHash", resultHash);
        return obj;
    }
    
    @Override
    public void fromJson(JSONObject obj) throws JSONException {
        super.fromJson(obj);
        resultString = obj.optString("resultString", null);
        resultTypeName = obj.optString("resultTypeName", null);
        resultHash = obj.optInt("resultHash", 0);
    }
}
