package com.justnothing.testmodule.command.proxy;

import com.justnothing.testmodule.command.base.CommandRequest;
import com.justnothing.testmodule.command.functions.classcmd.response.ClassHierarchyResult;
import com.justnothing.testmodule.command.functions.classcmd.response.ClassInfoResult;
import com.justnothing.testmodule.command.base.CommandResult;
import com.justnothing.testmodule.command.functions.threads.DeadlockDetectResult;
import com.justnothing.testmodule.command.functions.exportcontext.ExportContextResult;
import com.justnothing.testmodule.command.functions.memory.GcResult;
import com.justnothing.testmodule.command.functions.classcmd.response.GetFieldValueResult;
import com.justnothing.testmodule.command.functions.hook.HookAddResult;
import com.justnothing.testmodule.command.functions.hook.HookListResult;
import com.justnothing.testmodule.command.functions.classcmd.response.InvokeConstructorResult;
import com.justnothing.testmodule.command.functions.classcmd.response.InvokeMethodResult;
import com.justnothing.testmodule.command.functions.memory.MemoryInfoResult;
import com.justnothing.testmodule.command.functions.classcmd.response.SetFieldValueResult;
import com.justnothing.testmodule.command.functions.system.SystemInfoResult;
import com.justnothing.testmodule.command.functions.threads.ThreadInfoResult;
import com.justnothing.testmodule.utils.logging.Logger;

import org.json.JSONObject;

import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

public class RequestDispatcher {

    private static boolean initialized = false;
    
    private static final Logger logger = Logger.getLoggerForName("RequestDispatcher");
    private static final ConcurrentHashMap<String, RequestHandler<?, ?>> handlers = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, Supplier<CommandResult>> resultTypes = new ConcurrentHashMap<>();

    public static void ensureInitialized() {
        if (!initialized) {
            registerResultType(ClassInfoResult.class);
            registerResultType(ClassHierarchyResult.class);
            registerResultType(InvokeConstructorResult.class);
            registerResultType(InvokeMethodResult.class);
            registerResultType(GetFieldValueResult.class);
            registerResultType(SetFieldValueResult.class);
            registerResultType(MemoryInfoResult.class);
            registerResultType(GcResult.class);
            registerResultType(ThreadInfoResult.class);
            registerResultType(DeadlockDetectResult.class);
            registerResultType(HookListResult.class);
            registerResultType(HookAddResult.class);
            registerResultType(SystemInfoResult.class);
            registerResultType(ExportContextResult.class);
            initialized = true;
        }
    }

    static {
        ensureInitialized();
    }
    
    public static void registerHandler(RequestHandler<?, ?> handler) {
        String commandType = handler.getCommandType();
        handlers.put(commandType, handler);
        
        CommandResult prototype = handler.createResult(null);
        String resultType = prototype.getResultType();
        resultTypes.put(resultType, () -> handler.createResult(null));
        
        logger.info("注册请求处理器: " + commandType + " → " + resultType);
    }
    
    public static void registerResultType(String resultType, Supplier<CommandResult> supplier) {
        resultTypes.put(resultType, supplier);
        logger.info("注册响应类型: " + resultType);
    }

    public static void registerResultType(Class<? extends CommandResult> resultClass) {
        String resultType;
        try {
            resultType = resultClass.newInstance().getResultType();
        } catch (IllegalAccessException | InstantiationException e) {
            logger.error("获取resultType失败", e);
            throw new RuntimeException("注册响应类型" + resultClass.getSimpleName() + "失败", e);
        }
        registerResultType(resultType, () -> {
            try {
                return resultClass.newInstance();
            } catch (ReflectiveOperationException e) {
                logger.error("创建响应实例失败", e);
                throw new RuntimeException("创建响应实例" + resultClass.getSimpleName() + "失败", e);
            }
        });
    }

    @SuppressWarnings("unchecked")
    public static CommandResult handleRequest(CommandRequest request) {
        if (request == null) {
            logger.error("请求为null");
            return createErrorResponse("null", "请求为null");
        }
        
        try {
            String commandType = request.getCommandType();
            logger.debug("处理请求: " + commandType + ", requestId: " + request.getRequestId());
            
            RequestHandler<CommandRequest, CommandResult> handler = 
                (RequestHandler<CommandRequest, CommandResult>) handlers.get(commandType);
            
            if (handler != null) {
                CommandResult result = handler.handle(request);
                logger.debug("请求处理完成: " + commandType + ", success: " + result.isSuccess());
                return result;
            } else {
                logger.error("未知的命令类型: " + commandType);
                return createErrorResponse(request.getRequestId(), "未知的命令类型: " + commandType);
            }
        } catch (Exception e) {
            logger.error("处理请求失败", e);
            return createErrorResponse(request.getRequestId(), "处理请求失败: " + e.getMessage());
        }
    }
    
    public static CommandRequest parseRequest(String json) {
        try {
            JSONObject obj = new JSONObject(json);
            String commandType = obj.optString("commandType");
            
            RequestHandler<?, ?> handler = handlers.get(commandType);
            if (handler != null) {
                return handler.parseRequest(obj);
            }
            
            logger.warn("未知的命令类型: " + commandType);
            return null;
        } catch (Exception e) {
            logger.error("解析请求失败", e);
            return null;
        }
    }
    
    public static CommandResult parseResponse(String json) {
        try {
            JSONObject obj = new JSONObject(json);
            String resultType = obj.optString("resultType", "");
            logger.debug("解析响应: resultType=" + resultType + ", 已注册类型=" + resultTypes.keySet() + ", JSON=" + json);
            
            if (!resultType.isEmpty()) {
                Supplier<CommandResult> supplier = resultTypes.get(resultType);
                if (supplier != null) {
                    CommandResult result = supplier.get();
                    result.fromJson(obj);
                    logger.debug("解析响应成功: " + result.getClass().getSimpleName());
                    return result;
                } else {
                    logger.warn("未注册的resultType: " + resultType);
                }
            } else {
                logger.warn("响应中缺少resultType字段");
            }
            
            CommandResult result = new CommandResult();
            result.fromJson(obj);
            return result;
        } catch (Exception e) {
            logger.error("解析响应失败", e);
            CommandResult errorResult = new CommandResult();
            errorResult.setError(new CommandResult.ErrorInfo("PARSE_ERROR", "解析响应失败: " + e.getMessage()));
            return errorResult;
        }
    }
    
    public static String serializeRequest(CommandRequest request) {
        try {
            return request.toJson().toString();
        } catch (Exception e) {
            logger.error("序列化请求失败", e);
            return "{\"commandType\":\"Unknown\",\"error\":\"序列化失败\"}";
        }
    }
    
    public static String serializeResponse(CommandResult result) {
        try {
            return result.toJson().toString();
        } catch (Exception e) {
            logger.error("序列化响应失败", e);
            return "{\"success\":false,\"error\":{\"code\":\"JSON_ERROR\",\"message\":\"序列化失败\"}}";
        }
    }
    
    private static CommandResult createErrorResponse(String requestId, String message) {
        CommandResult result = new CommandResult();
        result.setRequestId(requestId);
        result.setError(new CommandResult.ErrorInfo("ERROR", message));
        return result;
    }
}
