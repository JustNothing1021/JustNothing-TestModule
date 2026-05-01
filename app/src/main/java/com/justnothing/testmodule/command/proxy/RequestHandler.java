package com.justnothing.testmodule.command.proxy;

import com.justnothing.testmodule.command.base.CommandRequest;
import com.justnothing.testmodule.command.base.CommandResult;

import org.json.JSONObject;
import org.json.JSONException;

public interface RequestHandler<Req extends CommandRequest, Res extends CommandResult> {

    String getCommandType();

    Req parseRequest(JSONObject obj) throws JSONException;

    Res createResult(String requestId);

    Res handle(Req request);

    default String getHelpText() {
        return "";
    }
}
