package com.justnothing.testmodule.command.proxy;

import com.justnothing.testmodule.command.handlers.classcmd.ClassHierarchyRequestHandler;
import com.justnothing.testmodule.command.handlers.classcmd.ClassInfoRequestHandler;
import com.justnothing.testmodule.command.handlers.classcmd.InvokeConstructorRequestHandler;
import com.justnothing.testmodule.command.handlers.classcmd.InvokeMethodRequestHandler;
import com.justnothing.testmodule.command.handlers.classcmd.GetFieldValueRequestHandler;
import com.justnothing.testmodule.command.handlers.classcmd.SetFieldValueRequestHandler;
import com.justnothing.testmodule.command.handlers.memory.MemoryInfoRequestHandler;
import com.justnothing.testmodule.command.handlers.memory.GcRequestHandler;
import com.justnothing.testmodule.command.handlers.threads.ThreadInfoRequestHandler;
import com.justnothing.testmodule.command.handlers.threads.DeadlockDetectRequestHandler;
import com.justnothing.testmodule.command.handlers.hook.HookListRequestHandler;
import com.justnothing.testmodule.command.handlers.hook.HookAddRequestHandler;
import com.justnothing.testmodule.command.handlers.hook.HookActionRequestHandler;
import com.justnothing.testmodule.command.handlers.packages.PackagesRequestHandler;
import com.justnothing.testmodule.command.handlers.exportcontext.ExportContextRequestHandler;
import com.justnothing.testmodule.command.handlers.system.SystemInfoRequestHandler;
import com.justnothing.testmodule.command.handlers.alias.AliasRequestHandler;
import com.justnothing.testmodule.command.base.CommandRequest;
import com.justnothing.testmodule.command.base.CommandResult;
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
