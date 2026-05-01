package com.justnothing.testmodule.command.protocol;

import com.justnothing.testmodule.command.proxy.CommandRequestHandler;
import com.justnothing.testmodule.command.proxy.RequestDispatcher;
import com.justnothing.testmodule.command.base.CommandRequest;
import com.justnothing.testmodule.command.base.CommandResult;
import com.justnothing.testmodule.utils.logging.Logger;

public class JsonProtocol {
    
    private static final Logger logger = Logger.getLoggerForName("JsonProtocol");

    
    public static CommandRequest parseRequest(String json) {
        CommandRequestHandler.ensureInitialized();
        return RequestDispatcher.parseRequest(json);
    }
    
    public static CommandResult parseResponse(String json) {
        CommandRequestHandler.ensureInitialized();
        return RequestDispatcher.parseResponse(json);
    }
    
    public static String toJson(Object obj) {
        if (obj instanceof CommandResult) {
            return RequestDispatcher.serializeResponse((CommandResult) obj);
        } else if (obj instanceof CommandRequest) {
            return RequestDispatcher.serializeRequest((CommandRequest) obj);
        } else {
            logger.error("不支持的对象类型: " + obj.getClass().getName());
            return "{\"success\":false,\"error\":{\"code\":\"INVALID_OBJECT\",\"message\":\"不支持的对象类型\"}}";
        }
    }
}
