package com.justnothing.testmodule.command.functions.threads;

import com.justnothing.testmodule.protocol.json.handler.RequestHandler;
import com.justnothing.testmodule.protocol.json.request.DeadlockDetectRequest;
import com.justnothing.testmodule.protocol.json.response.CommandResult;
import com.justnothing.testmodule.protocol.json.response.DeadlockDetectResult;
import com.justnothing.testmodule.protocol.json.response.ThreadInfoResult;
import com.justnothing.testmodule.utils.logging.Logger;

import org.json.JSONObject;

import java.util.Map;

public class DeadlockDetectRequestHandler implements RequestHandler<DeadlockDetectRequest, DeadlockDetectResult> {

    private static final Logger logger = Logger.getLoggerForName("DeadlockDetectReqHandler");

    @Override
    public String getCommandType() {
        return "DeadlockDetect";
    }

    @Override
    public DeadlockDetectRequest parseRequest(JSONObject obj) {
        return new DeadlockDetectRequest().fromJson(obj);
    }

    @Override
    public DeadlockDetectResult createResult(String requestId) {
        return new DeadlockDetectResult(requestId);
    }

    @Override
    public DeadlockDetectResult handle(DeadlockDetectRequest request) {
        logger.debug("处理死锁检测请求");

        DeadlockDetectResult result = new DeadlockDetectResult(request.getRequestId());
        result.setTimestamp(System.currentTimeMillis());

        try {
            Map<Thread, StackTraceElement[]> allStackTraces = Thread.getAllStackTraces();
            int blockedCount = 0;

            for (Map.Entry<Thread, StackTraceElement[]> entry : allStackTraces.entrySet()) {
                Thread thread = entry.getKey();
                if (thread.getState() == Thread.State.BLOCKED) {
                    blockedCount++;

                    ThreadInfoResult.ThreadDetail detail = new ThreadInfoResult.ThreadDetail();
                    detail.setThreadId(thread.getId());
                    detail.setName(thread.getName());
                    detail.setState(thread.getState().name());
                    detail.setPriority(thread.getPriority());
                    detail.setDaemon(thread.isDaemon());
                    detail.setInterrupted(thread.isInterrupted());
                    detail.setAlive(thread.isAlive());

                    StackTraceElement[] stackTrace = entry.getValue();
                    if (stackTrace != null && stackTrace.length > 0) {
                        int maxFrames = Math.min(stackTrace.length, 30);
                        for (int i = 0; i < maxFrames; i++) {
                            detail.addStackFrame("    at " + stackTrace[i].toString());
                        }
                        if (stackTrace.length > 30) {
                            detail.addStackFrame("    ... " + (stackTrace.length - 30) + " more");
                        }
                    }

                    result.addBlockedThread(detail);
                }
            }

            result.setBlockedCount(blockedCount);
            result.setHasDeadlock(blockedCount > 0);

            logger.info("死锁检测完成, BLOCKED线程数: " + blockedCount);

        } catch (Exception e) {
            logger.error("死锁检测失败", e);
            result.setError(new CommandResult.ErrorInfo("INTERNAL_ERROR", "死锁检测失败: " + e.getMessage()));
        }

        return result;
    }
}
