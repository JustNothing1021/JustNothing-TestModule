package com.justnothing.testmodule.command.functions.memory;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import com.justnothing.testmodule.command.base.protocol.CommandResult;
import com.justnothing.testmodule.command.base.protocol.SerializeKeyName;

@SerializeKeyName("Gc")
public class GcResult extends CommandResult {

    @Expose @SerializedName("beforeUsedMemory")
    private long beforeUsedMemory;
    @Expose @SerializedName("afterUsedMemory")
    private long afterUsedMemory;
    @Expose @SerializedName("freedBytes")
    private long freedBytes;
    @Expose @SerializedName("beforeTotalMemory")
    private long beforeTotalMemory;
    @Expose @SerializedName("afterTotalMemory")
    private long afterTotalMemory;
    @Expose @SerializedName("beforeUsagePercent")
    private double beforeUsagePercent;
    @Expose @SerializedName("afterUsagePercent")
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
}
