package com.justnothing.testmodule.command.functions.threads;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import com.justnothing.testmodule.command.base.protocol.CommandResult;

import java.util.ArrayList;
import java.util.List;

public class DeadlockDetectResult extends CommandResult {

    @Expose @SerializedName("timestamp")
    private long timestamp;
    @Expose @SerializedName("blockedCount")
    private int blockedCount;
    @Expose @SerializedName("hasDeadlock")
    private boolean hasDeadlock;
    @Expose @SerializedName("blockedThreads")
    private List<ThreadInfoResult.ThreadDetail> blockedThreads;

    public DeadlockDetectResult() {
        super();
        this.blockedThreads = new ArrayList<>();
    }

    public DeadlockDetectResult(String requestId) {
        super(requestId);
        this.blockedThreads = new ArrayList<>();
    }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public int getBlockedCount() { return blockedCount; }
    public void setBlockedCount(int blockedCount) { this.blockedCount = blockedCount; }

    public boolean isHasDeadlock() { return hasDeadlock; }
    public void setHasDeadlock(boolean hasDeadlock) { this.hasDeadlock = hasDeadlock; }

    public List<ThreadInfoResult.ThreadDetail> getBlockedThreads() { return blockedThreads; }
    public void setBlockedThreads(List<ThreadInfoResult.ThreadDetail> blockedThreads) { this.blockedThreads = blockedThreads; }
    public void addBlockedThread(ThreadInfoResult.ThreadDetail thread) { this.blockedThreads.add(thread); }
}
