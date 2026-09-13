package com.justnothing.testmodule.command.functions.threads.response;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;

public class ThreadListResult extends ThreadCommandResult {

    @Expose @SerializedName("timestamp")
    private long timestamp;
    @Expose @SerializedName("totalThreadCount")
    private int totalThreadCount;
    @Expose @SerializedName("blockedCount")
    private int blockedCount;
    @Expose @SerializedName("waitingCount")
    private int waitingCount;
    @Expose @SerializedName("timedWaitingCount")
    private int timedWaitingCount;
    @Expose @SerializedName("runnableCount")
    private int runnableCount;
    @Expose @SerializedName("terminatedCount")
    private int terminatedCount;
    @Expose @SerializedName("newCount")
    private int newCount;
    @Expose @SerializedName("threadDetails")
    private List<ThreadDetail> threadDetails = new ArrayList<>();

    public ThreadListResult() {
        super();
    }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public List<ThreadDetail> getThreadDetails() { return threadDetails; }
    public void setThreadDetails(List<ThreadDetail> threadDetails) { this.threadDetails = threadDetails; }
    public void addThreadDetail(ThreadDetail detail) { this.threadDetails.add(detail); }

    public int getTotalThreadCount() { return totalThreadCount; }
    public void setTotalThreadCount(int totalThreadCount) { this.totalThreadCount = totalThreadCount; }

    public int getBlockedCount() { return blockedCount; }
    public void setBlockedCount(int blockedCount) { this.blockedCount = blockedCount; }

    public int getWaitingCount() { return waitingCount; }
    public void setWaitingCount(int waitingCount) { this.waitingCount = waitingCount; }

    public int getTimedWaitingCount() { return timedWaitingCount; }
    public void setTimedWaitingCount(int timedWaitingCount) { this.timedWaitingCount = timedWaitingCount; }

    public int getRunnableCount() { return runnableCount; }
    public void setRunnableCount(int runnableCount) { this.runnableCount = runnableCount; }

    public int getTerminatedCount() { return terminatedCount; }
    public void setTerminatedCount(int terminatedCount) { this.terminatedCount = terminatedCount; }

    public int getNewCount() { return newCount; }
    public void setNewCount(int newValue) { this.newCount = newValue; }
}
