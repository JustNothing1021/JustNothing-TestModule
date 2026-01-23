package com.justnothing.testmodule.utils.functions;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import com.justnothing.testmodule.constants.AppEnvironment;
import com.justnothing.testmodule.utils.data.LogWriter;

import java.util.ArrayList;
import java.util.List;

public abstract class Logger {

    public static final String MAIN_TAG = "JustNothing";
    public static final Boolean SILENT = false;
    public static final Boolean SILENT_IN_CONST_HOOK = false;
    public static final Boolean USE_ONE_LOGGER_ONLY = true;
    
    private static final LogWriter SHARED_LOG_WRITER = new LogWriter(!AppEnvironment.isHookEnv());
    public static final List<Logger> instances = new ArrayList<>();

    private Context context;
    private boolean bUseXPosedLog = false;

    public static class LoggerWrapper extends Logger {

        private final String tag;

        LoggerWrapper(String tag) {
            this.tag = tag;
        }

        @Override
        public String getTag() {
            return tag;
        }
        
    }

    public static Logger getLoggerForName(String name) {
        return new LoggerWrapper(name);
    }

    public void useXposedLog(boolean use) {
        bUseXPosedLog = use;
    }

    public abstract String getTag();

    public static void setContext(Context ctx) {
        for (Logger logger : instances) logger.context = ctx;
    }

    private static void showToast(String message, int duration) {
        try {
            if (!instances.isEmpty()) {
                Context context = instances.get(0).context;
                if (context != null) {
                    Toast.makeText(context, message, duration).show();
                }
            }
        } catch (RuntimeException ignored) {}
    }

    public static void handleWarn(String s) {
        showToast("(警告) " + s, Toast.LENGTH_SHORT);
    }

    public static void handleWarn(Throwable e) {
        showToast("(警告) 出现异常: " + e, Toast.LENGTH_SHORT);
    }

    public static void handleWarn(String s, Throwable e) {
        showToast("(警告) " + s + "\n异常信息: " + e, Toast.LENGTH_SHORT);
    }

    public static void handleError(String s) {
        showToast("(错误) " + s, Toast.LENGTH_LONG);
    }

    public static void handleError(Throwable e) {
        showToast("(错误) 出现异常: " + e, Toast.LENGTH_LONG);
    }

    public static void handleError(String s, Throwable e) {
        showToast("(错误) " + s + "\n错误信息: " + e, Toast.LENGTH_LONG);
    }
    
    protected boolean shouldUseSystemLogger() {
        return !USE_ONE_LOGGER_ONLY || !bUseXPosedLog;
    }

    public void xposedLog(String str) {
        if (!bUseXPosedLog) return;
        try {
            Class<?> xposedBridge = Class.forName("de.robv.android.xposed.XposedBridge");
            for (String line : str.split("\n")) {
                xposedBridge.getMethod("log", String.class).invoke(null,
                    MAIN_TAG + "[" + getTag() + "] " + line);
            }
        } catch (Exception ignored) {}
    }

    private void logInternal(String level, String message) {
        if (SILENT) return;
        long timestamp = System.currentTimeMillis();
        xposedLog(message);
        if (shouldUseSystemLogger()) {
            switch (level) {
                case "DEBUG":
                    Log.d(MAIN_TAG + "[" + getTag() + "]", message);
                    break;
                case "INFO":
                    Log.i(MAIN_TAG + "[" + getTag() + "]", message);
                    break;
                case "WARN":
                    Log.w(MAIN_TAG + "[" + getTag() + "]", message);
                    break;
                case "ERROR":
                    Log.e(MAIN_TAG + "[" + getTag() + "]", message);
                    break;
            }
        }
        Log.d("Test", "向logWriter添加一条日志");
        SHARED_LOG_WRITER.addLog(level, getTag(), message, timestamp);
    }

    private void logThrowable(String level, Throwable th) {
        String stackTrace = Log.getStackTraceString(th);
        logInternal(level, stackTrace);
    }

    public void debug(String str) {
        logInternal("DEBUG", str);
    }

    public void debug(Throwable th) {
        logThrowable("DEBUG", th);
    }

    public void debug(String str, Throwable th) {
        logInternal("DEBUG", str);
        logThrowable("DEBUG", th);
    }

    public void info(String str) {
        logInternal("INFO", str);
    }

    public void info(Throwable th) {
        logThrowable("INFO", th);
    }

    public void info(String str, Throwable th) {
        logInternal("INFO", str);
        logThrowable("INFO", th);
    }

    public void warn(String str) {
        handleWarn(str);
        logInternal("WARN", str);
    }

    public void warn(Throwable th) {
        handleWarn(th);
        logThrowable("WARN", th);
    }

    public void warn(String str, Throwable th) {
        handleWarn(str, th);
        logInternal("WARN", str);
        logThrowable("WARN", th);
    }

    public void error(String str) {
        handleError(str);
        logInternal("ERROR", str);
    }

    public void error(Throwable th) {
        handleError(th);
        logThrowable("ERROR", th);
    }

    public void error(String str, Throwable th) {
        handleError(str, th);
        logInternal("ERROR", str);
        logThrowable("ERROR", th);
    }
}
