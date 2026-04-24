package com.justnothing.testmodule.protocol.json.response;

import org.json.JSONException;
import org.json.JSONObject;

public class SetFieldValueResult extends CommandResult {
    
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
