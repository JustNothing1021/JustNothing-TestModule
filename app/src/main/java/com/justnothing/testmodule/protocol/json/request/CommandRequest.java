package com.justnothing.testmodule.protocol.json.request;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.UUID;

public abstract class CommandRequest {
    
    private String requestId;
    private String commandType;
    
    public CommandRequest() {
        this.requestId = UUID.randomUUID().toString();
        this.commandType = getClass().getSimpleName().replace("Request", "");
    }
    
    public CommandRequest(String requestId) {
        this.requestId = requestId;
        this.commandType = getClass().getSimpleName().replace("Request", "");
    }
    
    public String getRequestId() {
        return requestId;
    }
    
    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }
    
    public String getCommandType() {
        return commandType;
    }
    
    public void setCommandType(String commandType) {
        this.commandType = commandType;
    }
    
    public JSONObject toJson() throws JSONException {
        JSONObject obj = new JSONObject();
        obj.put("requestId", getRequestId());
        obj.put("commandType", getCommandType());
        return obj;
    }
    
    public abstract CommandRequest fromJson(JSONObject obj) throws JSONException;
}
