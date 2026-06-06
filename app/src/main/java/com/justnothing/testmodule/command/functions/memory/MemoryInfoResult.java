package com.justnothing.testmodule.command.functions.memory;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import com.justnothing.testmodule.command.base.protocol.CommandResult;
import com.justnothing.testmodule.command.base.protocol.SerializeKeyName;

@SerializeKeyName("MemoryInfo")
public class MemoryInfoResult extends CommandResult {

    @Expose @SerializedName("timestamp")
    private long timestamp;

    @Expose @SerializedName("javaMaxMemory")
    private long javaMaxMemory;
    @Expose @SerializedName("javaTotalMemory")
    private long javaTotalMemory;
    @Expose @SerializedName("javaFreeMemory")
    private long javaFreeMemory;
    @Expose @SerializedName("javaUsedMemory")
    private long javaUsedMemory;
    @Expose @SerializedName("javaUsagePercent")
    private double javaUsagePercent;

    @Expose @SerializedName("nativeAllocatedSize")
    private long nativeAllocatedSize;
    @Expose @SerializedName("nativeHeapSize")
    private long nativeHeapSize;
    @Expose @SerializedName("nativeFreeSize")
    private long nativeFreeSize;

    @Expose @SerializedName("processPss")
    private long processPss;
    @Expose @SerializedName("processUss")
    private long processUss;
    @Expose @SerializedName("processRss")
    private long processRss;

    @Expose @SerializedName("systemAvailMem")
    private long systemAvailMem;
    @Expose @SerializedName("systemTotalMem")
    private long systemTotalMem;
    @Expose @SerializedName("systemThreshold")
    private long systemThreshold;
    @Expose @SerializedName("systemLowMemory")
    private boolean systemLowMemory;

    @Expose @SerializedName("threadCount")
    private int threadCount;
    @Expose @SerializedName("threadStackTotalBytes")
    private long threadStackTotalBytes;
    @Expose @SerializedName("loadedClassCount")
    private int loadedClassCount;
    @Expose @SerializedName("totalClassCount")
    private int totalClassCount;
    @Expose @SerializedName("bitmapTotalBytes")
    private long bitmapTotalBytes;
    @Expose @SerializedName("bitmapCount")
    private int bitmapCount;
    @Expose @SerializedName("databaseTotalBytes")
    private long databaseTotalBytes;

    public MemoryInfoResult() {
        super();
    }

    public MemoryInfoResult(String requestId) {
        super(requestId);
    }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public long getJavaMaxMemory() { return javaMaxMemory; }
    public void setJavaMaxMemory(long javaMaxMemory) { this.javaMaxMemory = javaMaxMemory; }
    public long getJavaTotalMemory() { return javaTotalMemory; }
    public void setJavaTotalMemory(long javaTotalMemory) { this.javaTotalMemory = javaTotalMemory; }
    public long getJavaFreeMemory() { return javaFreeMemory; }
    public void setJavaFreeMemory(long javaFreeMemory) { this.javaFreeMemory = javaFreeMemory; }
    public long getJavaUsedMemory() { return javaUsedMemory; }
    public void setJavaUsedMemory(long javaUsedMemory) { this.javaUsedMemory = javaUsedMemory; }
    public double getJavaUsagePercent() { return javaUsagePercent; }
    public void setJavaUsagePercent(double javaUsagePercent) { this.javaUsagePercent = javaUsagePercent; }

    public long getNativeAllocatedSize() { return nativeAllocatedSize; }
    public void setNativeAllocatedSize(long nativeAllocatedSize) { this.nativeAllocatedSize = nativeAllocatedSize; }
    public long getNativeHeapSize() { return nativeHeapSize; }
    public void setNativeHeapSize(long nativeHeapSize) { this.nativeHeapSize = nativeHeapSize; }
    public long getNativeFreeSize() { return nativeFreeSize; }
    public void setNativeFreeSize(long nativeFreeSize) { this.nativeFreeSize = nativeFreeSize; }

    public long getProcessPss() { return processPss; }
    public void setProcessPss(long processPss) { this.processPss = processPss; }
    public long getProcessUss() { return processUss; }
    public void setProcessUss(long processUss) { this.processUss = processUss; }
    public long getProcessRss() { return processRss; }
    public void setProcessRss(long processRss) { this.processRss = processRss; }

    public long getSystemAvailMem() { return systemAvailMem; }
    public void setSystemAvailMem(long systemAvailMem) { this.systemAvailMem = systemAvailMem; }
    public long getTotalMem() { return systemTotalMem; }
    public void setTotalMem(long systemTotalMem) { this.systemTotalMem = systemTotalMem; }
    public long getSystemThreshold() { return systemThreshold; }
    public void setSystemThreshold(long systemThreshold) { this.systemThreshold = systemThreshold; }
    public boolean isSystemLowMemory() { return systemLowMemory; }
    public void setSystemLowMemory(boolean systemLowMemory) { this.systemLowMemory = systemLowMemory; }

    public int getThreadCount() { return threadCount; }
    public void setThreadCount(int threadCount) { this.threadCount = threadCount; }
    public long getThreadStackTotalBytes() { return threadStackTotalBytes; }
    public void setThreadStackTotalBytes(long threadStackTotalBytes) { this.threadStackTotalBytes = threadStackTotalBytes; }
    public int getLoadedClassCount() { return loadedClassCount; }
    public void setLoadedClassCount(int loadedClassCount) { this.loadedClassCount = loadedClassCount; }
    public int getTotalClassCount() { return totalClassCount; }
    public void setTotalClassCount(int totalClassCount) { this.totalClassCount = totalClassCount; }
    public long getBitmapTotalBytes() { return bitmapTotalBytes; }
    public void setBitmapTotalBytes(long bitmapTotalBytes) { this.bitmapTotalBytes = bitmapTotalBytes; }
    public int getBitmapCount() { return bitmapCount; }
    public void setBitmapCount(int bitmapCount) { this.bitmapCount = bitmapCount; }
    public long getDatabaseTotalBytes() { return databaseTotalBytes; }
    public void setDatabaseTotalBytes(long databaseTotalBytes) { this.databaseTotalBytes = databaseTotalBytes; }
}
