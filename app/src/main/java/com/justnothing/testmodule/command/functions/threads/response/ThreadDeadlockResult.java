package com.justnothing.testmodule.command.functions.threads.response;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;

public class ThreadDeadlockResult extends ThreadCommandResult {

    @Expose @SerializedName("timestamp")
    private long timestamp;
    @Expose @SerializedName("blockedThreadCount")
    private int blockedThreadCount;
    @Expose @SerializedName("hasDeadlock")
    private boolean hasDeadlock;
    @Expose @SerializedName("blockedThreads")
    private List<ThreadDetail> blockedThreads = new ArrayList<>();

    public ThreadDeadlockResult() {
        super();
    }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public int getBlockedThreadCount() { return blockedThreadCount; }
    public void setBlockedThreadCount(int blockedThreadCount) { this.blockedThreadCount = blockedThreadCount; }

    public boolean isHasDeadlock() { return hasDeadlock; }
    public void setHasDeadlock(boolean hasDeadlock) { this.hasDeadlock = hasDeadlock; }

    public List<ThreadDetail> getBlockedThreads() { return blockedThreads; }
    public void setBlockedThreads(List<ThreadDetail> blockedThreads) { this.blockedThreads = blockedThreads; }
    public void addBlockedThread(ThreadDetail thread) { this.blockedThreads.add(thread); }
}
