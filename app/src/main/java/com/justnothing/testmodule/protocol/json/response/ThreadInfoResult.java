package com.justnothing.testmodule.protocol.json.response;

import org.json.JSONObject;
import org.json.JSONException;
import org.json.JSONArray;

import java.util.ArrayList;
import java.util.List;

public class ThreadInfoResult extends CommandResult {

    private long timestamp;

    private int totalThreadCount;
    private int runnableCount;
    private int blockedCount;
    private int waitingCount;
    private int timedWaitingCount;
    private int terminatedCount;
    private int newCount;

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

    @Override
    public JSONObject toJson() throws JSONException {
        JSONObject obj = super.toJson();
        obj.put("timestamp", timestamp);
        obj.put("totalThreadCount", totalThreadCount);

        JSONObject stateObj = new JSONObject();
        stateObj.put("RUNNABLE", runnableCount);
        stateObj.put("BLOCKED", blockedCount);
        stateObj.put("WAITING", waitingCount);
        stateObj.put("TIMED_WAITING", timedWaitingCount);
        stateObj.put("TERMINATED", terminatedCount);
        stateObj.put("NEW", newCount);
        obj.put("stateStats", stateObj);

        if (threadDetails != null && !threadDetails.isEmpty()) {
            JSONArray detailsArr = new JSONArray();
            for (ThreadDetail detail : threadDetails) {
                detailsArr.put(detail.toJson());
            }
            obj.put("threadDetails", detailsArr);
        }

        return obj;
    }

    @Override
    public void fromJson(JSONObject obj) throws JSONException {
        super.fromJson(obj);
        timestamp = obj.optLong("timestamp", 0);
        totalThreadCount = obj.optInt("totalThreadCount", 0);

        if (obj.has("stateStats")) {
            JSONObject stateObj = obj.getJSONObject("stateStats");
            runnableCount = stateObj.optInt("RUNNABLE", 0);
            blockedCount = stateObj.optInt("BLOCKED", 0);
            waitingCount = stateObj.optInt("WAITING", 0);
            timedWaitingCount = stateObj.optInt("TIMED_WAITING", 0);
            terminatedCount = stateObj.optInt("TERMINATED", 0);
            newCount = stateObj.optInt("NEW", 0);
        }

        threadDetails = new ArrayList<>();
        if (obj.has("threadDetails")) {
            JSONArray detailsArr = obj.getJSONArray("threadDetails");
            for (int i = 0; i < detailsArr.length(); i++) {
                ThreadDetail detail = new ThreadDetail();
                detail.fromJson(detailsArr.getJSONObject(i));
                threadDetails.add(detail);
            }
        }
    }

    public static class ThreadDetail {
        private long threadId;
        private String name;
        private String state;
        private int priority;
        private boolean daemon;
        private boolean interrupted;
        private boolean alive;
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

        public JSONObject toJson() throws JSONException {
            JSONObject obj = new JSONObject();
            obj.put("threadId", threadId);
            obj.put("name", name);
            obj.put("state", state);
            obj.put("priority", priority);
            obj.put("daemon", daemon);
            obj.put("interrupted", interrupted);
            obj.put("alive", alive);

            if (stackTrace != null && !stackTrace.isEmpty()) {
                JSONArray arr = new JSONArray();
                for (String frame : stackTrace) {
                    arr.put(frame);
                }
                obj.put("stackTrace", arr);
            }

            return obj;
        }

        public void fromJson(JSONObject obj) throws JSONException {
            threadId = obj.optLong("threadId", 0);
            name = obj.optString("name", "");
            state = obj.optString("state", "UNKNOWN");
            priority = obj.optInt("priority", 5);
            daemon = obj.optBoolean("daemon", false);
            interrupted = obj.optBoolean("interrupted", false);
            alive = obj.optBoolean("alive", false);

            stackTrace = new ArrayList<>();
            if (obj.has("stackTrace")) {
                JSONArray arr = obj.getJSONArray("stackTrace");
                for (int i = 0; i < arr.length(); i++) {
                    stackTrace.add(arr.getString(i));
                }
            }
        }
    }
}
