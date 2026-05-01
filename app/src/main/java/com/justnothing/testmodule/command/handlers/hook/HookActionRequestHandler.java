package com.justnothing.testmodule.command.handlers.hook;

import com.justnothing.testmodule.command.CommandExecutor;
import com.justnothing.testmodule.command.functions.hook.HookManager;
import com.justnothing.testmodule.command.proxy.RequestHandler;
import com.justnothing.testmodule.command.functions.hook.HookActionRequest;
import com.justnothing.testmodule.command.functions.hook.HookAddResult;
import com.justnothing.testmodule.utils.logging.Logger;

import org.json.JSONObject;

import java.util.List;

public class HookActionRequestHandler implements RequestHandler<HookActionRequest, HookAddResult> {

    private static final Logger logger = Logger.getLoggerForName("HookActionReqHandler");

    @Override
    public String getCommandType() {
        return "HookAction";
    }

    @Override
    public HookActionRequest parseRequest(JSONObject obj) {
        return new HookActionRequest().fromJson(obj);
    }

    @Override
    public HookAddResult createResult(String requestId) {
        return new HookAddResult(requestId);
    }

    @Override
    public HookAddResult handle(HookActionRequest request) {
        logger.debug("处理Hook操作请求: action=" + request.getAction() + ", hookId=" + request.getHookId());

        HookAddResult result = new HookAddResult(request.getRequestId());
        result.setHookId(request.getHookId());

        try {
            CommandExecutor.CmdExecContext ctx = HookManager.createDummyContext();
            String action = request.getAction();

            switch (action) {
                case HookActionRequest.ACTION_REMOVE -> {
                    HookManager.removeHook(request.getHookId(), ctx);
                    result.setSuccessAction(true);
                    result.setMessage("Hook removed");
                }
                case HookActionRequest.ACTION_INFO -> {
                    List<HookAddResult.HookDetailInfo> detailList = HookManager.getHookInfoDetail(request.getHookId());
                    if (detailList != null) {
                        result.setDetail(detailList);
                        result.setSuccessAction(true);
                    } else {
                        result.setSuccessAction(false);
                        result.setMessage("Hook not found: " + request.getHookId());
                    }
                }
                case HookActionRequest.ACTION_ENABLE -> {
                    HookManager.enableHook(request.getHookId(), ctx);
                    result.setSuccessAction(true);
                    result.setMessage("Hook enabled");
                }
                case HookActionRequest.ACTION_DISABLE -> {
                    HookManager.disableHook(request.getHookId(), ctx);
                    result.setSuccessAction(true);
                    result.setMessage("Hook disabled");
                }
                case HookActionRequest.ACTION_OUTPUT -> {
                    List<HookAddResult.HookDetailInfo> outputDetail = HookManager.getHookOutputDetail(request.getHookId(), request.getOutputCount());
                    if (outputDetail != null) {
                        result.setDetail(outputDetail);
                        result.setSuccessAction(true);
                    } else {
                        result.setSuccessAction(false);
                        result.setMessage("Hook not found: " + request.getHookId());
                    }
                }
                case HookActionRequest.ACTION_CLEAR -> {
                    int count = HookManager.getHookCount();
                    HookManager.clearAllHooks();
                    result.setSuccessAction(true);
                    result.setMessage("Cleared " + count + " hooks");
                }
                default -> {
                    result.setSuccessAction(false);
                    result.setMessage("Unknown action: " + action);
                }
            }

            logger.info("Hook操作成功: " + action);

        } catch (Exception e) {
            logger.error("Hook操作失败: " + request.getAction(), e);
            result.setSuccessAction(false);
            result.setMessage("操作失败: " + e.getMessage());
        }

        return result;
    }
}
