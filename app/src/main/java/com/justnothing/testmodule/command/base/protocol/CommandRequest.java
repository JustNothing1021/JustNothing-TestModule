package com.justnothing.testmodule.command.base.protocol;

import com.justnothing.testmodule.command.base.IllegalCommandLineArgumentException;
import com.justnothing.testmodule.command.functions.classcmd.request.InvokeMethodRequest;
import com.justnothing.testmodule.command.utils.ParamParser;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.UUID;

public abstract class CommandRequest {
    private String requestId;
    private String commandType;
    
    public CommandRequest() {
        this.requestId = UUID.randomUUID().toString();
        this.commandType = extractTypeKey();
    }

    public CommandRequest(String requestId) {
        this.requestId = requestId;
        this.commandType = extractTypeKey();
    }

    private String extractTypeKey() {
        SerializeKeyName keyName = this.getClass().getAnnotation(SerializeKeyName.class);
        if (keyName != null) {
            return keyName.value();
        }
        throw new IllegalStateException(
            "Request类 " + this.getClass().getSimpleName() +
            " 缺少@SerializeKeyName注解!"
        );
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
    
    @SuppressWarnings("unchecked")
    public <T extends CommandRequest> T fromJson(JSONObject obj) throws JSONException {
        if (this.getClass().isAnnotationPresent(AutoSerializable.class)) {
            try {
                String jsonStr = obj.toString();
                CommandRequest deserialized = (CommandRequest) 
                        com.justnothing.testmodule.command.utils.AutoSerializer.fromJson(jsonStr, this.getClass());
                this.setRequestId(deserialized.getRequestId());
                this.setCommandType(deserialized.getCommandType());
                return (T) this;
            } catch (Exception e) {
                throw new JSONException("Auto-serialization failed: " + e.getMessage());
            }
        }
        
        this.setRequestId(obj.optString("requestId", this.getRequestId()));
        this.setCommandType(obj.optString("commandType", this.getCommandType()));
        return (T) this;
    }
    
    public CommandRequest fromCommandLine(String[] args) throws IllegalCommandLineArgumentException {
        return ParamParser.parse(getClass(), args);
    }

}
