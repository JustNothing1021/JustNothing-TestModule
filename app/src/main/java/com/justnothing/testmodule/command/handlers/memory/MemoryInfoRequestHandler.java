package com.justnothing.testmodule.command.handlers.memory;

import android.app.ActivityManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.Debug;
import android.annotation.SuppressLint;

import com.justnothing.testmodule.command.proxy.RequestHandler;
import com.justnothing.testmodule.command.functions.memory.MemoryInfoRequest;
import com.justnothing.testmodule.command.base.CommandResult;
import com.justnothing.testmodule.command.functions.memory.MemoryInfoResult;
import com.justnothing.testmodule.utils.logging.Logger;

import org.json.JSONObject;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Locale;
import java.util.Map;

public class MemoryInfoRequestHandler implements RequestHandler<MemoryInfoRequest, MemoryInfoResult> {

    private static final Logger logger = Logger.getLoggerForName("MemoryInfoReqHandler");

    @Override
    public String getCommandType() {
        return "MemoryInfo";
    }

    @Override
    public MemoryInfoRequest parseRequest(JSONObject obj) {
        return new MemoryInfoRequest().fromJson(obj);
    }

    @Override
    public MemoryInfoResult createResult(String requestId) {
        return new MemoryInfoResult(requestId);
    }

    @Override
    public MemoryInfoResult handle(MemoryInfoRequest request) {
        logger.debug("处理内存信息请求, detailLevel=" + request.getDetailLevel());

        MemoryInfoResult result = new MemoryInfoResult(request.getRequestId());
        result.setTimestamp(System.currentTimeMillis());

        try {
            Runtime runtime = Runtime.getRuntime();

            long maxMemory = runtime.maxMemory();
            long totalMemory = runtime.totalMemory();
            long freeMemory = runtime.freeMemory();
            long usedMemory = totalMemory - freeMemory;
            double usagePercent = maxMemory > 0 ? (double) usedMemory / maxMemory * 100 : 0;

            result.setJavaMaxMemory(maxMemory);
            result.setJavaTotalMemory(totalMemory);
            result.setJavaFreeMemory(freeMemory);
            result.setJavaUsedMemory(usedMemory);
            result.setJavaUsagePercent(usagePercent);

            result.setNativeAllocatedSize(Debug.getNativeHeapAllocatedSize());
            result.setNativeHeapSize(Debug.getNativeHeapSize());
            result.setNativeFreeSize(Debug.getNativeHeapFreeSize());

            boolean detailed = !MemoryInfoRequest.LEVEL_BASIC.equals(request.getDetailLevel());
            if (detailed) {
                fillProcessMemory(result);
                fillVmDetails(result);
            }

            if (MemoryInfoRequest.LEVEL_FULL.equals(request.getDetailLevel())) {
                fillSystemMemory(result);
            }

            logger.info("内存信息查询成功, Java使用率: " + String.format(Locale.US, "%.1f%%", usagePercent));

        } catch (Exception e) {
            logger.error("获取内存信息失败", e);
            result.setError(new CommandResult.ErrorInfo("INTERNAL_ERROR", "获取内存信息失败: " + e.getMessage()));
        }

        return result;
    }

    private void fillProcessMemory(MemoryInfoResult result) {
        try {
            Context appContext = getApplicationContext();
            if (appContext == null) return;

            ActivityManager activityManager = (ActivityManager) appContext.getSystemService(Context.ACTIVITY_SERVICE);
            if (activityManager == null) return;

            int pid = android.os.Process.myPid();
            Debug.MemoryInfo[] memoryInfos = activityManager.getProcessMemoryInfo(new int[]{pid});
            if (memoryInfos.length > 0) {
                Debug.MemoryInfo processMemory = memoryInfos[0];
                result.setProcessPss(processMemory.getTotalPss() * 1024L);
                result.setProcessUss(processMemory.getTotalPrivateDirty() * 1024L);
                result.setProcessRss(processMemory.getTotalSharedDirty() * 1024L);
            }
        } catch (Exception e) {
            logger.warn("获取进程内存失败", e);
        }
    }

    private void fillVmDetails(MemoryInfoResult result) {
        try {
            Map<Thread, StackTraceElement[]> allStackTraces = Thread.getAllStackTraces();
            result.setThreadCount(allStackTraces.size());

            long totalStackBytes = 0;
            for (Map.Entry<Thread, StackTraceElement[]> entry : allStackTraces.entrySet()) {
                Thread thread = entry.getKey();
                totalStackBytes += thread.getStackTrace().length * 64L;
            }
            result.setThreadStackTotalBytes(totalStackBytes);

        } catch (Exception e) {
            logger.warn("获取线程信息失败", e);
        }

        try {
            Class<?> vmClass = Class.forName("dalvik.system.VMRuntime");
            Method getRuntimeMethod = vmClass.getMethod("getRuntime");
            Object vmRuntime = getRuntimeMethod.invoke(null);
            Method getLoadedClassCountMethod = vmClass.getDeclaredMethod("getLoadedClassCount");
            getLoadedClassCountMethod.setAccessible(true);
            int loadedCount = (int) getLoadedClassCountMethod.invoke(vmRuntime);
            result.setLoadedClassCount(loadedCount);
            result.setTotalClassCount(loadedCount);
        } catch (Exception e) {
            logger.warn("获取类加载信息失败(部分设备不支持)", e);
        }

        try {
            long bitmapTotal = 0;
            int bitmapCount = 0;
            Field mBufferField = Bitmap.class.getDeclaredField("mBuffer");
            mBufferField.setAccessible(true);
            for (Map.Entry<Thread, StackTraceElement[]> entry : Thread.getAllStackTraces().entrySet()) {
                for (StackTraceElement element : entry.getValue()) {
                    if (element.getClassName().contains("Bitmap") || element.getMethodName().equals("createBitmap")) {
                        break;
                    }
                }
            }
            Class<?> bitmapConfigClass = Class.forName("android.graphics.Bitmap$Config");
            Field nativeBitmapField = Bitmap.class.getDeclaredField("mNativeBitmap");
            nativeBitmapField.setAccessible(true);
            Field densityField = Bitmap.class.getDeclaredField("mDensity");
            densityField.setAccessible(true);
            result.setBitmapTotalBytes(bitmapTotal);
            result.setBitmapCount(bitmapCount);
        } catch (Exception e) {
            logger.warn("获取Bitmap信息失败(部分设备不支持)", e);
        }

        try {
            Context appContext = getApplicationContext();
            if (appContext != null) {
                File dbDir = appContext.getDatabasePath("").getParentFile();
                if (dbDir != null && dbDir.exists()) {
                    long dbTotal = 0;
                    File[] dbFiles = dbDir.listFiles();
                    if (dbFiles != null) {
                        for (File file : dbFiles) {
                            if (file.isFile() && file.getName().endsWith(".db") || file.getName().endsWith("-journal") || file.getName().endsWith("-wal")) {
                                dbTotal += file.length();
                            }
                        }
                    }
                    result.setDatabaseTotalBytes(dbTotal);
                }
            }
        } catch (Exception e) {
            logger.warn("获取数据库大小失败", e);
        }
    }

    private void fillSystemMemory(MemoryInfoResult result) {
        try {
            Context appContext = getApplicationContext();
            if (appContext == null) return;

            ActivityManager activityManager = (ActivityManager) appContext.getSystemService(Context.ACTIVITY_SERVICE);
            if (activityManager == null) return;

            ActivityManager.MemoryInfo memoryInfo = new ActivityManager.MemoryInfo();
            activityManager.getMemoryInfo(memoryInfo);

            result.setSystemAvailMem(memoryInfo.availMem);
            result.setTotalMem(memoryInfo.totalMem);
            result.setSystemThreshold(memoryInfo.threshold);
            result.setSystemLowMemory(memoryInfo.lowMemory);
        } catch (Exception e) {
            logger.warn("获取系统内存失败", e);
        }
    }

    @SuppressLint("PrivateApi")
    private static Context getApplicationContext() {
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
