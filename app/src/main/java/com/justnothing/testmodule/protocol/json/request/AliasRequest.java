package com.justnothing.testmodule.protocol.json.request;

import org.json.JSONException;
import org.json.JSONObject;

public class AliasRequest extends CommandRequest {

    public static final String ACTION_LIST = "list";
    public static final String ACTION_ADD = "add";
    public static final String ACTION_REMOVE = "remove";
    public static final String ACTION_CLEAR = "clear";

    private String action;
    private String name;
    private String command;

    public AliasRequest() {
        super();
    }

    public AliasRequest(String action) {
        super();
        this.action = action;
    }

    public AliasRequest(String action, String name, String command) {
        super();
        this.action = action;
        this.name = name;
        this.command = command;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
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
        obj.put("action", action != null ? action : ACTION_LIST);
        if (name != null) {
            obj.put("name", name);
        }
        if (command != null) {
            obj.put("command", command != null ? command : "");
        }
        return obj;
    }

    @Override
    public AliasRequest fromJson(JSONObject obj) {
        this.setRequestId(obj.optString("requestId"));
        this.action = obj.optString("action", ACTION_LIST);
        this.name = obj.optString("name", null);
        this.command = obj.optString("command", null);
        return this;
    }
}