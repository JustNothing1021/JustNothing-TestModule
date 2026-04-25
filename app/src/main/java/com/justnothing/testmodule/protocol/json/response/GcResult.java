package com.justnothing.testmodule.protocol.json.response;

import org.json.JSONException;
import org.json.JSONObject;

public class GcResult extends CommandResult {

    private long beforeUsedMemory;
    private long afterUsedMemory;
    private long freedBytes;
    private long beforeTotalMemory;
    private long afterTotalMemory;
    private double beforeUsagePercent;
    private double afterUsagePercent;

    public GcResult() {
        super();
    }

    public GcResult(String requestId) {
        super(requestId);
    }

    public long getBeforeUsedMemory() { return beforeUsedMemory; }
    public void setBeforeUsedMemory(long beforeUsedMemory) { this.beforeUsedMemory = beforeUsedMemory; }
    public long getAfterUsedMemory() { return afterUsedMemory; }
    public void setAfterUsedMemory(long afterUsedMemory) { this.afterUsedMemory = afterUsedMemory; }
    public long getFreedBytes() { return freedBytes; }
    public void setFreedBytes(long freedBytes) { this.freedBytes = freedBytes; }
    public long getBeforeTotalMemory() { return beforeTotalMemory; }
    public void setBeforeTotalMemory(long beforeTotalMemory) { this.beforeTotalMemory = beforeTotalMemory; }
    public long getAfterTotalMemory() { return afterTotalMemory; }
    public void setAfterTotalMemory(long afterTotalMemory) { this.afterTotalMemory = afterTotalMemory; }
    public double getBeforeUsagePercent() { return beforeUsagePercent; }
    public void setBeforeUsagePercent(double beforeUsagePercent) { this.beforeUsagePercent = beforeUsagePercent; }
    public double getAfterUsagePercent() { return afterUsagePercent; }
    public void setAfterUsagePercent(double afterUsagePercent) { this.afterUsagePercent = afterUsagePercent; }

    @Override
    public JSONObject toJson() throws JSONException {
        JSONObject obj = super.toJson();
        obj.put("beforeUsedMemory", beforeUsedMemory);
        obj.put("afterUsedMemory", afterUsedMemory);
        obj.put("freedBytes", freedBytes);
        obj.put("beforeTotalMemory", beforeTotalMemory);
        obj.put("afterTotalMemory", afterTotalMemory);
        obj.put("beforeUsagePercent", beforeUsagePercent);
        obj.put("afterUsagePercent", afterUsagePercent);
        return obj;
    }

    @Override
    public void fromJson(JSONObject obj) throws org.json.JSONException {
        super.fromJson(obj);
        beforeUsedMemory = obj.optLong("beforeUsedMemory", 0);
        afterUsedMemory = obj.optLong("afterUsedMemory", 0);
        freedBytes = obj.optLong("freedBytes", 0);
        beforeTotalMemory = obj.optLong("beforeTotalMemory", 0);
        afterTotalMemory = obj.optLong("afterTotalMemory", 0);
        beforeUsagePercent = obj.optDouble("beforeUsagePercent", 0);
        afterUsagePercent = obj.optDouble("afterUsagePercent", 0);
    }
}
