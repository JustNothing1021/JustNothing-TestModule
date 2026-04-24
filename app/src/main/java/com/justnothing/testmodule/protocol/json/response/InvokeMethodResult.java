package com.justnothing.testmodule.protocol.json.response;

import org.json.JSONException;
import org.json.JSONObject;

public class InvokeMethodResult extends CommandResult {
    
    private String resultString;
    private String resultTypeName;
    private int resultHash;
    private String instanceAfterInvocation;
    private int instanceHash;
    
    public InvokeMethodResult() {
        super();
    }
    
    public InvokeMethodResult(String requestId) {
        super(requestId);
    }
    
    public String getResultString() { return resultString; }
    public void setResultString(String resultString) { this.resultString = resultString; }
    public String getResultTypeName() { return resultTypeName; }
    public void setResultTypeName(String resultTypeName) { this.resultTypeName = resultTypeName; }
    public int getResultHash() { return resultHash; }
    public void setResultHash(int resultHash) { this.resultHash = resultHash; }
    public String getInstanceAfterInvocation() { return instanceAfterInvocation; }
    public void setInstanceAfterInvocation(String instanceAfterInvocation) { this.instanceAfterInvocation = instanceAfterInvocation; }
    public int getInstanceHash() { return instanceHash; }
    public void setInstanceHash(int instanceHash) { this.instanceHash = instanceHash; }
    
    @Override
    public JSONObject toJson() throws org.json.JSONException {
        JSONObject obj = super.toJson();
        if (resultString != null) {
            obj.put("resultString", resultString);
        }
        if (resultTypeName != null) {
            obj.put("resultTypeName", resultTypeName);
        }
        obj.put("resultHash", resultHash);
        if (instanceAfterInvocation != null) {
            obj.put("instanceAfterInvocation", instanceAfterInvocation);
        }
        obj.put("instanceHash", instanceHash);
        return obj;
    }
    
    @Override
    public void fromJson(JSONObject obj) throws JSONException {
        super.fromJson(obj);
        resultString = obj.optString("resultString", null);
        resultTypeName = obj.optString("resultTypeName", null);
        resultHash = obj.optInt("resultHash", 0);
        instanceAfterInvocation = obj.optString("instanceAfterInvocation", null);
        instanceHash = obj.optInt("instanceHash", 0);
    }
}
