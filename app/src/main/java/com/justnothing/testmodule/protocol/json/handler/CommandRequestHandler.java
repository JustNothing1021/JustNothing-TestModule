package com.justnothing.testmodule.protocol.json.handler;

import com.justnothing.testmodule.command.functions.classcmd.ClassHierarchyRequestHandler;
import com.justnothing.testmodule.command.functions.classcmd.ClassInfoRequestHandler;
import com.justnothing.testmodule.command.functions.classcmd.InvokeConstructorRequestHandler;
import com.justnothing.testmodule.command.functions.classcmd.InvokeMethodRequestHandler;
import com.justnothing.testmodule.command.functions.classcmd.GetFieldValueRequestHandler;
import com.justnothing.testmodule.command.functions.classcmd.SetFieldValueRequestHandler;
import com.justnothing.testmodule.protocol.json.request.CommandRequest;
import com.justnothing.testmodule.protocol.json.response.CommandResult;
import com.justnothing.testmodule.utils.logging.Logger;
import com.justnothing.testmodule.utils.reflect.ClassLoaderManager;

public class CommandRequestHandler {
    
    private static final Logger logger = Logger.getLoggerForName("CommandRequestHandler");
    
    private static volatile boolean initialized = false;
    
    public static synchronized void ensureInitialized() {
        if (!initialized) {
            initialized = true;
            
            ClassLoaderManager.getApkClassLoader();
            
            RequestDispatcher.registerHandler(new ClassHierarchyRequestHandler());
            RequestDispatcher.registerHandler(new ClassInfoRequestHandler());
            RequestDispatcher.registerHandler(new InvokeConstructorRequestHandler());
            RequestDispatcher.registerHandler(new InvokeMethodRequestHandler());
            RequestDispatcher.registerHandler(new GetFieldValueRequestHandler());
            RequestDispatcher.registerHandler(new SetFieldValueRequestHandler());
            logger.info("默认请求处理器已注册完成");
        }
    }
    
    public static CommandResult handleRequest(CommandRequest request) {
        ensureInitialized();
        return RequestDispatcher.handleRequest(request);
    }
}
