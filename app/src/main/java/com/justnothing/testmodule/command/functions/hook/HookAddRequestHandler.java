package com.justnothing.testmodule.command.functions.hook;

import com.justnothing.testmodule.command.CommandExecutor;
import com.justnothing.testmodule.protocol.json.handler.RequestHandler;
import com.justnothing.testmodule.protocol.json.request.HookAddRequest;
import com.justnothing.testmodule.protocol.json.response.HookAddResult;
import com.justnothing.testmodule.utils.logging.Logger;

import org.json.JSONObject;

public class HookAddRequestHandler implements RequestHandler<HookAddRequest, HookAddResult> {

    private static final Logger logger = Logger.getLoggerForName("HookAddReqHandler");

    @Override
    public String getCommandType() {
        return "HookAdd";
    }

    @Override
    public HookAddRequest parseRequest(JSONObject obj) {
        return new HookAddRequest().fromJson(obj);
    }

    @Override
    public HookAddResult createResult(String requestId) {
        return new HookAddResult(requestId);
    }

    @Override
    public HookAddResult handle(HookAddRequest request) {
        logger.debug("处理Hook添加请求: " + request.getClassName() + "." + request.getMethodName());

        HookAddResult result = new HookAddResult(request.getRequestId());

        try {
            CommandExecutor.CmdExecContext ctx = HookManager.createDummyContext();
            HookManager.AddHookResult addResult = HookManager.addHook(
                    request.getClassName(),
                    request.getMethodName(),
                    request.getSignature(),
                    request.getBeforeCode(),
                    request.getAfterCode(),
                    request.getReplaceCode(),
                    request.getBeforeCodebase(),
                    request.getAfterCodebase(),
                    request.getReplaceCodebase(),
                    ctx
            );

            if (addResult.success()) {
                result.setSuccessAction(true);
                result.setHookId(addResult.hookId());
                result.setMessage("Hook added successfully: " + addResult.hookId());
                logger.info("Hook添加成功: " + addResult.hookId());
            } else {
                result.setSuccessAction(false);
                result.setMessage(addResult.errorMessage());
                logger.error("Hook添加失败: " + addResult.errorMessage());
            }

        } catch (Exception e) {
            logger.error("添加Hook失败", e);
            result.setSuccessAction(false);
            result.setMessage("添加Hook失败: " + e.getMessage());
        }

        return result;
    }
}
