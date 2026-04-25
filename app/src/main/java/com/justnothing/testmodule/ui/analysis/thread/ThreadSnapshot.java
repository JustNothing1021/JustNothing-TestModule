package com.justnothing.testmodule.ui.analysis.thread;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public record ThreadSnapshot(
        long timestamp,
        int totalCount,
        StateStats stateStats,
        List<ThreadItem> threads
) implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    public static ThreadSnapshot fromResult(com.justnothing.testmodule.protocol.json.response.ThreadInfoResult result) {
        List<ThreadItem> items = new ArrayList<>();
        if (result.getThreadDetails() != null) {
            for (com.justnothing.testmodule.protocol.json.response.ThreadInfoResult.ThreadDetail d : result.getThreadDetails()) {
                items.add(new ThreadItem(
                        d.getThreadId(), d.getName(), d.getState(),
                        d.getPriority(), d.isDaemon(), d.isInterrupted(), d.isAlive(),
                        d.getStackTrace() != null ? d.getStackTrace() : new ArrayList<>()
                ));
            }
        }
        return new ThreadSnapshot(
                result.getTimestamp(),
                result.getTotalThreadCount(),
                new StateStats(result.getRunnableCount(), result.getBlockedCount(),
                        result.getWaitingCount(), result.getTimedWaitingCount(),
                        result.getTerminatedCount(), result.getNewCount()),
                items
        );
    }

    public record StateStats(
            int runnable, int blocked, int waiting,
            int timedWaiting, int terminated, int nnew
    ) implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;

        public int total() { return runnable + blocked + waiting + timedWaiting + terminated + nnew; }
        public double runnablePercent() { return total() > 0 ? (double) runnable / total() * 100 : 0; }
        public double blockedPercent() { return total() > 0 ? (double) blocked / total() * 100 : 0; }
    }

    public record ThreadItem(
            long threadId, String name, String state,
            int priority, boolean daemon, boolean interrupted, boolean alive,
            List<String> stackTrace
    ) implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;

        public boolean isBlocked() { return "BLOCKED".equals(state); }
        public boolean isRunnable() { return "RUNNABLE".equals(state); }
        public boolean isWaiting() { return "WAITING".equals(state) || "TIMED_WAITING".equals(state); }

        public String shortInfo() {
            return "[" + state + "] " +
                    "ID=" + threadId +
                    " pri=" + priority +
                    " daemon=" + (daemon ? "Y" : "N");
        }
    }
}
