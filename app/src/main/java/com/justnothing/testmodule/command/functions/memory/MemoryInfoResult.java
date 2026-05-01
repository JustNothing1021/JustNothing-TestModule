package com.justnothing.testmodule.command.functions.memory;

import com.justnothing.testmodule.command.base.CommandResult;

import org.json.JSONObject;
import org.json.JSONException;

public class MemoryInfoResult extends CommandResult {

    private long timestamp;

    private long javaMaxMemory;
    private long javaTotalMemory;
    private long javaFreeMemory;
    private long javaUsedMemory;
    private double javaUsagePercent;

    private long nativeAllocatedSize;
    private long nativeHeapSize;
    private long nativeFreeSize;

    private long processPss;
    private long processUss;
    private long processRss;

    private long systemAvailMem;
    private long systemTotalMem;
    private long systemThreshold;
    private boolean systemLowMemory;

    private int threadCount;
    private long threadStackTotalBytes;
    private int loadedClassCount;
    private int totalClassCount;
    private long bitmapTotalBytes;
    private int bitmapCount;
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

    @Override
    public JSONObject toJson() throws JSONException {
        JSONObject obj = super.toJson();
        obj.put("timestamp", timestamp);
        obj.put("javaMaxMemory", javaMaxMemory);
        obj.put("javaTotalMemory", javaTotalMemory);
        obj.put("javaFreeMemory", javaFreeMemory);
        obj.put("javaUsedMemory", javaUsedMemory);
        obj.put("javaUsagePercent", javaUsagePercent);

        JSONObject nativeObj = new JSONObject();
        nativeObj.put("allocatedSize", nativeAllocatedSize);
        nativeObj.put("heapSize", nativeHeapSize);
        nativeObj.put("freeSize", nativeFreeSize);
        obj.put("nativeHeap", nativeObj);

        if (processPss > 0 || processUss > 0 || processRss > 0) {
            JSONObject procObj = new JSONObject();
            procObj.put("pss", processPss);
            procObj.put("uss", processUss);
            procObj.put("rss", processRss);
            obj.put("processMemory", procObj);
        }

        if (systemTotalMem > 0) {
            JSONObject sysObj = new JSONObject();
            sysObj.put("availMem", systemAvailMem);
            sysObj.put("totalMem", systemTotalMem);
            sysObj.put("threshold", systemThreshold);
            sysObj.put("lowMemory", systemLowMemory);
            obj.put("systemMemory", sysObj);
        }

        JSONObject vmObj = new JSONObject();
        vmObj.put("threadCount", threadCount);
        vmObj.put("threadStackTotalBytes", threadStackTotalBytes);
        vmObj.put("loadedClassCount", loadedClassCount);
        vmObj.put("totalClassCount", totalClassCount);
        vmObj.put("bitmapTotalBytes", bitmapTotalBytes);
        vmObj.put("bitmapCount", bitmapCount);
        vmObj.put("databaseTotalBytes", databaseTotalBytes);
        obj.put("vmDetails", vmObj);

        return obj;
    }

    @Override
    public void fromJson(JSONObject obj) throws JSONException {
        super.fromJson(obj);
        timestamp = obj.optLong("timestamp", 0);
        javaMaxMemory = obj.optLong("javaMaxMemory", 0);
        javaTotalMemory = obj.optLong("javaTotalMemory", 0);
        javaFreeMemory = obj.optLong("javaFreeMemory", 0);
        javaUsedMemory = obj.optLong("javaUsedMemory", 0);
        javaUsagePercent = obj.optDouble("javaUsagePercent", 0);

        if (obj.has("nativeHeap")) {
            JSONObject nativeObj = obj.getJSONObject("nativeHeap");
            nativeAllocatedSize = nativeObj.optLong("allocatedSize", 0);
            nativeHeapSize = nativeObj.optLong("heapSize", 0);
            nativeFreeSize = nativeObj.optLong("freeSize", 0);
        }

        if (obj.has("processMemory")) {
            JSONObject procObj = obj.getJSONObject("processMemory");
            processPss = procObj.optLong("pss", 0);
            processUss = procObj.optLong("uss", 0);
            processRss = procObj.optLong("rss", 0);
        }

        if (obj.has("systemMemory")) {
            JSONObject sysObj = obj.getJSONObject("systemMemory");
            systemAvailMem = sysObj.optLong("availMem", 0);
            systemTotalMem = sysObj.optLong("totalMem", 0);
            systemThreshold = sysObj.optLong("threshold", 0);
            systemLowMemory = sysObj.optBoolean("lowMemory", false);
        }

        if (obj.has("vmDetails")) {
            JSONObject vmObj = obj.getJSONObject("vmDetails");
            threadCount = vmObj.optInt("threadCount", 0);
            threadStackTotalBytes = vmObj.optLong("threadStackTotalBytes", 0);
            loadedClassCount = vmObj.optInt("loadedClassCount", 0);
            totalClassCount = vmObj.optInt("totalClassCount", 0);
            bitmapTotalBytes = vmObj.optLong("bitmapTotalBytes", 0);
            bitmapCount = vmObj.optInt("bitmapCount", 0);
            databaseTotalBytes = vmObj.optLong("databaseTotalBytes", 0);
        }
    }
}
