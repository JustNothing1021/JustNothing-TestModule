package com.justnothing.testmodule.protocol.json;

import com.justnothing.testmodule.protocol.json.handler.CommandRequestHandler;
import com.justnothing.testmodule.protocol.json.handler.RequestDispatcher;
import com.justnothing.testmodule.protocol.json.request.CommandRequest;
import com.justnothing.testmodule.protocol.json.response.ClassInfoResult;
import com.justnothing.testmodule.protocol.json.response.ClassHierarchyResult;
import com.justnothing.testmodule.protocol.json.response.CommandResult;
import com.justnothing.testmodule.protocol.json.response.InvokeConstructorResult;
import com.justnothing.testmodule.protocol.json.response.InvokeMethodResult;
import com.justnothing.testmodule.protocol.json.response.GetFieldValueResult;
import com.justnothing.testmodule.protocol.json.response.SetFieldValueResult;
import com.justnothing.testmodule.utils.logging.Logger;

public class JsonProtocol {
    
    private static final Logger logger = Logger.getLoggerForName("JsonProtocol");
    
    static {
        RequestDispatcher.registerResultType("ClassInfo", ClassInfoResult::new);
        RequestDispatcher.registerResultType("ClassHierarchy", ClassHierarchyResult::new);
        RequestDispatcher.registerResultType("InvokeConstructor", InvokeConstructorResult::new);
        RequestDispatcher.registerResultType("InvokeMethod", InvokeMethodResult::new);
        RequestDispatcher.registerResultType("GetFieldValue", GetFieldValueResult::new);
        RequestDispatcher.registerResultType("SetFieldValue", SetFieldValueResult::new);
    }
    
    public static CommandRequest parseRequest(String json) {
        CommandRequestHandler.ensureInitialized();
        return RequestDispatcher.parseRequest(json);
    }
    
    public static CommandResult parseResponse(String json) {
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
