package com.justnothing.testmodule.protocol.json.handler;

import com.justnothing.testmodule.protocol.json.request.CommandRequest;
import com.justnothing.testmodule.protocol.json.response.CommandResult;

import org.json.JSONObject;
import org.json.JSONException;

public interface RequestHandler<Req extends CommandRequest, Res extends CommandResult> {
    
    String getCommandType();
    
    Req parseRequest(JSONObject obj) throws JSONException;

    Res createResult(String requestId);
    
    Res handle(Req request);
}
