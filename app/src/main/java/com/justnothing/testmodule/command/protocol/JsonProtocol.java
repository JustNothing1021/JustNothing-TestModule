package com.justnothing.testmodule.command.protocol;

import com.justnothing.testmodule.command.base.protocol.CommandRequest;
import com.justnothing.testmodule.command.base.protocol.CommandResult;
import com.justnothing.testmodule.command.utils.AutoSerializer;

public class JsonProtocol {

    public static CommandRequest parseRequest(String json) {
        return AutoSerializer.parseRequest(json);
    }

    public static CommandResult parseResponse(String json) {
        return AutoSerializer.parseResponse(json);
    }

    public static String toJson(Object obj) {
        return AutoSerializer.toJson(obj);
    }
}
