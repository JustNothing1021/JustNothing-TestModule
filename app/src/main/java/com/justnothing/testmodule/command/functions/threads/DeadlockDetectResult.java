package com.justnothing.testmodule.command.functions.threads;

import com.justnothing.testmodule.command.base.CommandResult;

import org.json.JSONObject;
import org.json.JSONException;
import org.json.JSONArray;

import java.util.ArrayList;
import java.util.List;

public class DeadlockDetectResult extends CommandResult {

    private long timestamp;
    private int blockedCount;
    private boolean hasDeadlock;
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

    @Override
    public JSONObject toJson() throws JSONException {
        JSONObject obj = super.toJson();
        obj.put("timestamp", timestamp);
        obj.put("blockedCount", blockedCount);
        obj.put("hasDeadlock", hasDeadlock);

        if (blockedThreads != null && !blockedThreads.isEmpty()) {
            JSONArray arr = new JSONArray();
            for (ThreadInfoResult.ThreadDetail thread : blockedThreads) {
                arr.put(thread.toJson());
            }
            obj.put("blockedThreads", arr);
        }

        return obj;
    }

    @Override
    public void fromJson(JSONObject obj) throws JSONException {
        super.fromJson(obj);
        timestamp = obj.optLong("timestamp", 0);
        blockedCount = obj.optInt("blockedCount", 0);
        hasDeadlock = obj.optBoolean("hasDeadlock", blockedCount > 0);

        blockedThreads = new ArrayList<>();
        if (obj.has("blockedThreads")) {
            JSONArray arr = obj.getJSONArray("blockedThreads");
            for (int i = 0; i < arr.length(); i++) {
                ThreadInfoResult.ThreadDetail detail = new ThreadInfoResult.ThreadDetail();
                detail.fromJson(arr.getJSONObject(i));
                blockedThreads.add(detail);
            }
        }
    }
}
