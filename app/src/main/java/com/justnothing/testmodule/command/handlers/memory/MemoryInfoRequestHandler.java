package com.justnothing.testmodule.command.handlers.memory;

import android.app.ActivityManager;
import android.content.Context;
import android.os.Debug;

import com.justnothing.testmodule.command.functions.memory.MemoryInfoRequest;
import com.justnothing.testmodule.command.functions.memory.MemoryInfoResult;

import java.lang.reflect.Method;
import java.util.Map;

public class MemoryInfoRequestHandler {

    public MemoryInfoResult handle(MemoryInfoRequest request) {
        MemoryInfoResult result = new MemoryInfoResult(request.getRequestId());
        result.setTimestamp(System.currentTimeMillis());

        Runtime runtime = Runtime.getRuntime();
        long maxMemory = runtime.maxMemory();
        long totalMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();
        long usedMemory = totalMemory - freeMemory;

        result.setJavaMaxMemory(maxMemory);
        result.setJavaTotalMemory(totalMemory);
        result.setJavaFreeMemory(freeMemory);
        result.setJavaUsedMemory(usedMemory);

        if (maxMemory > 0) {
            double percent = (double) usedMemory / maxMemory * 100;
            result.setJavaUsagePercent(percent);
        }

        result.setNativeAllocatedSize(Debug.getNativeHeapAllocatedSize());
        result.setNativeHeapSize(Debug.getNativeHeapSize());
        result.setNativeFreeSize(Debug.getNativeHeapFreeSize());

        Context appContext = getApplicationContext();
        if (appContext != null) {
            ActivityManager activityManager =
                (ActivityManager) appContext.getSystemService(Context.ACTIVITY_SERVICE);
            if (activityManager != null) {
                ActivityManager.MemoryInfo memoryInfo = new ActivityManager.MemoryInfo();
                activityManager.getMemoryInfo(memoryInfo);

                result.setSystemAvailMem(memoryInfo.availMem);
                result.setTotalMem(memoryInfo.totalMem);
                result.setSystemThreshold(memoryInfo.threshold);
                result.setSystemLowMemory(memoryInfo.lowMemory);

                int pid = android.os.Process.myPid();
                Debug.MemoryInfo[] memoryInfos = activityManager.getProcessMemoryInfo(new int[]{pid});
                if (memoryInfos.length > 0) {
                    Debug.MemoryInfo processMemory = memoryInfos[0];
                    result.setProcessPss(processMemory.getTotalPss() * 1024L);
                    result.setProcessUss(processMemory.getTotalPrivateDirty() * 1024L);
                    result.setProcessRss(processMemory.getTotalSharedDirty() * 1024L);
                }
            }
        }

        Map<Thread, StackTraceElement[]> allStackTraces = Thread.getAllStackTraces();
        result.setThreadCount(allStackTraces.size());

        result.setSuccess(true);

        return result;
    }

    private Context getApplicationContext() {
        try {
            Class<?> activityThreadClass = Class.forName("android.app.ActivityThread");
            Method currentActivityThreadMethod = activityThreadClass.getMethod("currentActivityThread");
            Object activityThread = currentActivityThreadMethod.invoke(null);

            Method getApplicationMethod = activityThreadClass.getMethod("getApplication");
            return (Context) getApplicationMethod.invoke(activityThread);
        } catch (Exception e) {
            return null;
        }
    }
}
