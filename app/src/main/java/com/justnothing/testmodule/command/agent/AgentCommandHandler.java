package com.justnothing.testmodule.command.agent;

import android.content.Context;

import org.json.JSONObject;

public abstract class AgentCommandHandler<T> {

    public abstract T handle(JSONObject params, Context context) throws Exception;
    public abstract String getCommandType();
}
