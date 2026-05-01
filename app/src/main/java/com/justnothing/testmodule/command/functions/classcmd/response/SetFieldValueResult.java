package com.justnothing.testmodule.command.functions.classcmd.response;

import com.justnothing.testmodule.command.functions.classcmd.ClassCommandResult;

import org.json.JSONException;
import org.json.JSONObject;

public class SetFieldValueResult extends ClassCommandResult {
    
    public SetFieldValueResult() {
        super();
    }
    
    public SetFieldValueResult(String requestId) {
        super(requestId);
    }
    
    @Override
    public JSONObject toJson() throws JSONException {
        return super.toJson();
    }
    
    @Override
    public void fromJson(JSONObject obj) throws JSONException {
        super.fromJson(obj);
    }
}
