package com.justnothing.testmodule.protocol.json.request;

import org.json.JSONObject;

public abstract class CommandRequest {
    
    private String requestId;
    private String commandType;
    
    public CommandRequest() {
        this.requestId = java.util.UUID.randomUUID().toString();
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
    
    public abstract JSONObject toJson() throws org.json.JSONException;
    
    public abstract CommandRequest fromJson(JSONObject obj) throws org.json.JSONException;
}
