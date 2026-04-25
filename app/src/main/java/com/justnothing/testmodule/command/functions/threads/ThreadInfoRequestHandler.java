package com.justnothing.testmodule.command.functions.threads;

import com.justnothing.testmodule.protocol.json.handler.RequestHandler;
import com.justnothing.testmodule.protocol.json.request.ThreadInfoRequest;
import com.justnothing.testmodule.protocol.json.response.CommandResult;
import com.justnothing.testmodule.protocol.json.response.ThreadInfoResult;
import com.justnothing.testmodule.utils.logging.Logger;

import org.json.JSONObject;

import java.util.Map;

public class ThreadInfoRequestHandler implements RequestHandler<ThreadInfoRequest, ThreadInfoResult> {

    private static final Logger logger = Logger.getLoggerForName("ThreadInfoReqHandler");

    @Override
    public String getCommandType() {
        return "ThreadInfo";
    }

    @Override
    public ThreadInfoRequest parseRequest(JSONObject obj) {
        return new ThreadInfoRequest().fromJson(obj);
    }

    @Override
    public ThreadInfoResult createResult(String requestId) {
        return new ThreadInfoResult(requestId);
    }

    @Override
    public ThreadInfoResult handle(ThreadInfoRequest request) {
        logger.debug("处理线程信息请求, detailLevel=" + request.getDetailLevel());

        ThreadInfoResult result = new ThreadInfoResult(request.getRequestId());
        result.setTimestamp(System.currentTimeMillis());

        try {
            Map<Thread, StackTraceElement[]> allStackTraces = Thread.getAllStackTraces();
            String filterState = request.getFilterState();

            int runnableCount = 0;
            int blockedCount = 0;
            int waitingCount = 0;
            int timedWaitingCount = 0;
            int terminatedCount = 0;
            int newCount = 0;

            for (Thread thread : allStackTraces.keySet()) {
                Thread.State state = thread.getState();

                if (filterState != null && !filterState.isEmpty() && !state.name().equals(filterState)) {
                    continue;
                }

                switch (state) {
                    case RUNNABLE -> runnableCount++;
                    case BLOCKED -> blockedCount++;
                    case WAITING -> waitingCount++;
                    case TIMED_WAITING -> timedWaitingCount++;
                    case TERMINATED -> terminatedCount++;
                    case NEW -> newCount++;
                }
            }

            result.setTotalThreadCount(allStackTraces.size());
            result.setRunnableCount(runnableCount);
            result.setBlockedCount(blockedCount);
            result.setWaitingCount(waitingCount);
            result.setTimedWaitingCount(timedWaitingCount);
            result.setTerminatedCount(terminatedCount);
            result.setNewCount(newCount);

            boolean detailed = !ThreadInfoRequest.LEVEL_BASIC.equals(request.getDetailLevel());
            if (detailed) {
                fillThreadDetails(result, allStackTraces, filterState);
            }

            logger.info("线程信息查询成功, 总数: " + allStackTraces.size() + ", BLOCKED: " + blockedCount);

        } catch (Exception e) {
            logger.error("获取线程信息失败", e);
            result.setError(new CommandResult.ErrorInfo("INTERNAL_ERROR", "获取线程信息失败: " + e.getMessage()));
        }

        return result;
    }

    private void fillThreadDetails(ThreadInfoResult result, Map<Thread, StackTraceElement[]> allStackTraces, String filterState) {
        for (Map.Entry<Thread, StackTraceElement[]> entry : allStackTraces.entrySet()) {
            Thread thread = entry.getKey();
            StackTraceElement[] stackTrace = entry.getValue();

            if (filterState != null && !filterState.isEmpty() && !thread.getState().name().equals(filterState)) {
                continue;
            }

            ThreadInfoResult.ThreadDetail detail = new ThreadInfoResult.ThreadDetail();
            detail.setThreadId(thread.getId());
            detail.setName(thread.getName());
            detail.setState(thread.getState().name());
            detail.setPriority(thread.getPriority());
            detail.setDaemon(thread.isDaemon());
            detail.setInterrupted(thread.isInterrupted());
            detail.setAlive(thread.isAlive());

            if (stackTrace != null && stackTrace.length > 0) {
                int maxFrames = Math.min(stackTrace.length, 50);
                for (int i = 0; i < maxFrames; i++) {
                    detail.addStackFrame("    at " + stackTrace[i].toString());
                }
                if (stackTrace.length > 50) {
                    detail.addStackFrame("    ... " + (stackTrace.length - 50) + " more");
                }
            }

            result.addThreadDetail(detail);
        }
    }
}
