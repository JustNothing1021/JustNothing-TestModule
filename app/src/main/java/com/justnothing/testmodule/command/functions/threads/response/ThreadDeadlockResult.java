package com.justnothing.testmodule.command.functions.threads.response;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class ThreadDeadlockResult extends ThreadCommandResult {

    @Expose @SerializedName("blockedThreadCount")
    private int blockedThreadCount;
    @Expose @SerializedName("hasDeadlock")
    private boolean hasDeadlock;

    public ThreadDeadlockResult() {
        super();
    }

    public int getBlockedThreadCount() { return blockedThreadCount; }
    public void setBlockedThreadCount(int blockedThreadCount) { this.blockedThreadCount = blockedThreadCount; }

    public boolean isHasDeadlock() { return hasDeadlock; }
    public void setHasDeadlock(boolean hasDeadlock) { this.hasDeadlock = hasDeadlock; }
}
