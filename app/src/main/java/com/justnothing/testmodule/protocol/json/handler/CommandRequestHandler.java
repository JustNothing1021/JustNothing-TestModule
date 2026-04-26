package com.justnothing.testmodule.protocol.json.handler;

import com.justnothing.testmodule.command.functions.classcmd.ClassHierarchyRequestHandler;
import com.justnothing.testmodule.command.functions.classcmd.ClassInfoRequestHandler;
import com.justnothing.testmodule.command.functions.classcmd.InvokeConstructorRequestHandler;
import com.justnothing.testmodule.command.functions.classcmd.InvokeMethodRequestHandler;
import com.justnothing.testmodule.command.functions.classcmd.GetFieldValueRequestHandler;
import com.justnothing.testmodule.command.functions.classcmd.SetFieldValueRequestHandler;
import com.justnothing.testmodule.command.functions.memory.MemoryInfoRequestHandler;
import com.justnothing.testmodule.command.functions.memory.GcRequestHandler;
import com.justnothing.testmodule.command.functions.threads.ThreadInfoRequestHandler;
import com.justnothing.testmodule.command.functions.threads.DeadlockDetectRequestHandler;
import com.justnothing.testmodule.command.functions.hook.HookListRequestHandler;
import com.justnothing.testmodule.command.functions.hook.HookAddRequestHandler;
import com.justnothing.testmodule.command.functions.hook.HookActionRequestHandler;
import com.justnothing.testmodule.command.functions.packages.PackagesRequestHandler;
import com.justnothing.testmodule.command.functions.exportcontext.ExportContextRequestHandler;
import com.justnothing.testmodule.command.functions.system.SystemInfoRequestHandler;
import com.justnothing.testmodule.command.functions.alias.AliasRequestHandler;
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
            RequestDispatcher.registerHandler(new MemoryInfoRequestHandler());
            RequestDispatcher.registerHandler(new GcRequestHandler());
            RequestDispatcher.registerHandler(new ThreadInfoRequestHandler());
            RequestDispatcher.registerHandler(new DeadlockDetectRequestHandler());
            RequestDispatcher.registerHandler(new HookListRequestHandler());
            RequestDispatcher.registerHandler(new HookAddRequestHandler());
            RequestDispatcher.registerHandler(new HookActionRequestHandler());
            RequestDispatcher.registerHandler(new PackagesRequestHandler());
            RequestDispatcher.registerHandler(new ExportContextRequestHandler());
            RequestDispatcher.registerHandler(new SystemInfoRequestHandler());
            RequestDispatcher.registerHandler(new AliasRequestHandler());
            logger.info("默认请求处理器已注册完成");
        }
    }
    
    public static CommandResult handleRequest(CommandRequest request) {
        ensureInitialized();
        return RequestDispatcher.handleRequest(request);
    }
}
