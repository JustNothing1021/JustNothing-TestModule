package com.justnothing.testmodule.command.functions.alias.model;

import org.json.JSONException;
import org.json.JSONObject;

public class AliasInfo {

    private String name;
    private String command;

    public AliasInfo() {
    }

    public AliasInfo(String name, String command) {
        this.name = name;
        this.command = command;
    }

    public static AliasInfo fromJson(JSONObject obj) throws JSONException {
        AliasInfo info = new AliasInfo();
        info.setName(obj.optString("name"));
        info.setCommand(obj.optString("command"));
        return info;
    }

    public JSONObject toJson() throws JSONException {
        JSONObject obj = new JSONObject();
        obj.put("name", name);
        obj.put("command", command != null ? command : "");
        return obj;
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
}
