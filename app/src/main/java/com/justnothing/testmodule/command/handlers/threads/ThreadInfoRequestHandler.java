package com.justnothing.testmodule.command.handlers.threads;

import com.justnothing.testmodule.command.functions.threads.ThreadInfoRequest;
import com.justnothing.testmodule.command.functions.threads.ThreadInfoResult;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ThreadInfoRequestHandler {

    public ThreadInfoResult handle(ThreadInfoRequest request) {
        ThreadInfoResult result = new ThreadInfoResult();
        result.setRequestId(request.getRequestId());
        result.setTimestamp(System.currentTimeMillis());

        Map<Thread, StackTraceElement[]> allStackTraces = Thread.getAllStackTraces();

        int totalThreadCount = allStackTraces.size();
        int runnableCount = 0;
        int blockedCount = 0;
        int waitingCount = 0;
        int timedWaitingCount = 0;
        int terminatedCount = 0;
        int newCount = 0;

        List<ThreadInfoResult.ThreadDetail> threadDetails = new ArrayList<>();

        for (Map.Entry<Thread, StackTraceElement[]> entry : allStackTraces.entrySet()) {
            Thread thread = entry.getKey();
            StackTraceElement[] stackTrace = entry.getValue();

            Thread.State state = thread.getState();
            switch (state) {
                case BLOCKED -> blockedCount++;
                case WAITING -> waitingCount++;
                case TIMED_WAITING -> timedWaitingCount++;
                case RUNNABLE -> runnableCount++;
                case TERMINATED -> terminatedCount++;
                case NEW -> newCount++;
            }

            ThreadInfoResult.ThreadDetail detail = new ThreadInfoResult.ThreadDetail();
            detail.setThreadId(thread.getId());
            detail.setName(thread.getName());
            detail.setState(state.name());
            detail.setPriority(thread.getPriority());
            detail.setDaemon(thread.isDaemon());
            detail.setInterrupted(thread.isInterrupted());
            detail.setAlive(thread.isAlive());

            if (stackTrace != null && stackTrace.length > 0) {
                List<String> stackTraceList = new ArrayList<>();
                for (StackTraceElement element : stackTrace) {
                    stackTraceList.add(element.toString());
                }
                detail.setStackTrace(stackTraceList);
            }

            threadDetails.add(detail);
        }

        result.setTotalThreadCount(totalThreadCount);
        result.setRunnableCount(runnableCount);
        result.setBlockedCount(blockedCount);
        result.setWaitingCount(waitingCount);
        result.setTimedWaitingCount(timedWaitingCount);
        result.setTerminatedCount(terminatedCount);
        result.setNewCount(newCount);
        result.setThreadDetails(threadDetails);
        result.setSuccess(true);

        return result;
    }
}
