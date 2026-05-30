package com.justnothing.testmodule.command.functions.threads;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import com.justnothing.testmodule.command.base.protocol.CommandResult;

import java.util.ArrayList;
import java.util.List;

public class ThreadInfoResult extends CommandResult {

    @Expose @SerializedName("timestamp")
    private long timestamp;

    @Expose @SerializedName("totalThreadCount")
    private int totalThreadCount;
    @Expose @SerializedName("runnableCount")
    private int runnableCount;
    @Expose @SerializedName("blockedCount")
    private int blockedCount;
    @Expose @SerializedName("waitingCount")
    private int waitingCount;
    @Expose @SerializedName("timedWaitingCount")
    private int timedWaitingCount;
    @Expose @SerializedName("terminatedCount")
    private int terminatedCount;
    @Expose @SerializedName("newCount")
    private int newCount;

    @Expose @SerializedName("threadDetails")
    private List<ThreadDetail> threadDetails;

    public ThreadInfoResult() {
        super();
        this.threadDetails = new ArrayList<>();
    }

    public ThreadInfoResult(String requestId) {
        super(requestId);
        this.threadDetails = new ArrayList<>();
    }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public int getTotalThreadCount() { return totalThreadCount; }
    public void setTotalThreadCount(int totalThreadCount) { this.totalThreadCount = totalThreadCount; }

    public int getRunnableCount() { return runnableCount; }
    public void setRunnableCount(int runnableCount) { this.runnableCount = runnableCount; }

    public int getBlockedCount() { return blockedCount; }
    public void setBlockedCount(int blockedCount) { this.blockedCount = blockedCount; }

    public int getWaitingCount() { return waitingCount; }
    public void setWaitingCount(int waitingCount) { this.waitingCount = waitingCount; }

    public int getTimedWaitingCount() { return timedWaitingCount; }
    public void setTimedWaitingCount(int timedWaitingCount) { this.timedWaitingCount = timedWaitingCount; }

    public int getTerminatedCount() { return terminatedCount; }
    public void setTerminatedCount(int terminatedCount) { this.terminatedCount = terminatedCount; }

    public int getNewCount() { return newCount; }
    public void setNewCount(int newCount) { this.newCount = newCount; }

    public List<ThreadDetail> getThreadDetails() { return threadDetails; }
    public void setThreadDetails(List<ThreadDetail> threadDetails) { this.threadDetails = threadDetails; }
    public void addThreadDetail(ThreadDetail detail) { this.threadDetails.add(detail); }

    public static class ThreadDetail {
        @Expose @SerializedName("threadId")
        private long threadId;
        @Expose @SerializedName("name")
        private String name;
        @Expose @SerializedName("state")
        private String state;
        @Expose @SerializedName("priority")
        private int priority;
        @Expose @SerializedName("daemon")
        private boolean daemon;
        @Expose @SerializedName("interrupted")
        private boolean interrupted;
        @Expose @SerializedName("alive")
        private boolean alive;
        @Expose @SerializedName("stackTrace")
        private List<String> stackTrace;

        public ThreadDetail() {
            this.stackTrace = new ArrayList<>();
        }

        public long getThreadId() { return threadId; }
        public void setThreadId(long threadId) { this.threadId = threadId; }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public String getState() { return state; }
        public void setState(String state) { this.state = state; }

        public int getPriority() { return priority; }
        public void setPriority(int priority) { this.priority = priority; }

        public boolean isDaemon() { return daemon; }
        public void setDaemon(boolean daemon) { this.daemon = daemon; }

        public boolean isInterrupted() { return interrupted; }
        public void setInterrupted(boolean interrupted) { this.interrupted = interrupted; }

        public boolean isAlive() { return alive; }
        public void setAlive(boolean alive) { this.alive = alive; }

        public List<String> getStackTrace() { return stackTrace; }
        public void setStackTrace(List<String> stackTrace) { this.stackTrace = stackTrace; }
        public void addStackFrame(String frame) { this.stackTrace.add(frame); }
    }
}
