package com.justnothing.testmodule.protocol.json.request;

import org.json.JSONException;
import org.json.JSONObject;

public class AliasAddRequest extends CommandRequest {

    private String name;
    private String command;

    public AliasAddRequest() {
        super();
    }

    public AliasAddRequest(String name, String command) {
        super();
        this.name = name;
        this.command = command;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCommand() {
        return command;
    }

    public void setCommand(String command) {
        this.command = command;
    }

    @Override
    public JSONObject toJson() throws JSONException {
        JSONObject obj = super.toJson();
        obj.put("name", name);
        obj.put("command", command != null ? command : "");
        return obj;
    }

    @Override
    public AliasAddRequest fromJson(JSONObject obj) {
        this.setRequestId(obj.optString("requestId"));
        this.name = obj.optString("name");
        this.command = obj.optString("command");
        return this;
    }
}
