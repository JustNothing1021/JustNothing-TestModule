package com.justnothing.testmodule.command.functions.intercept;

import androidx.annotation.NonNull;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Locale;

import de.robv.android.xposed.XC_MethodHook;

public class HookContext {

    private final InterceptTask task;
    private final XC_MethodHook.MethodHookParam methodParam;
    private final long timestamp;
    private final Thread thread;
    private final int callDepth;

    public HookContext(InterceptTask task, XC_MethodHook.MethodHookParam methodParam) {
        this.task = task;
        this.methodParam = methodParam;
        this.timestamp = System.currentTimeMillis();
        this.thread = Thread.currentThread();
        this.callDepth = calculateCallDepth();
    }

    private int calculateCallDepth() {
        StackTraceElement[] stackTrace = thread.getStackTrace();
        int depth = 0;
        boolean foundAppFrame = false;

        for (StackTraceElement element : stackTrace) {
            String className = element.getClassName();

            if (className.startsWith("com.justnothing.testmodule") ||
                className.startsWith("de.robv.android.xposed")) {
                continue;
            }

            if (!foundAppFrame) {
                foundAppFrame = true;
            } else {
                depth++;
            }
        }

        return depth;
    }

    public InterceptTask getTask() {
        return task;
    }

    public XC_MethodHook.MethodHookParam getMethodParam() {
        return methodParam;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public Thread getThread() {
        return thread;
    }

    public int getCallDepth() {
        return callDepth;
    }

    public Object[] getArguments() {
        return methodParam.args;
    }

    public Object getThisObject() {
        return methodParam.thisObject;
    }

    public Object getReturnValue() {
        return methodParam.getResult();
    }

    public Throwable getThrowable() {
        return methodParam.getThrowable();
    }

    public void setReturnValue(Object value) {
        methodParam.setResult(value);
    }

    public void setThrowable(Throwable throwable) {
        methodParam.setThrowable(throwable);
    }

    public String getFormattedTimestamp() {
        return new SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault()).format(new Date(timestamp));
    }

    public String getFormattedTimestamp(String pattern) {
        return new SimpleDateFormat(pattern, Locale.getDefault()).format(new Date(timestamp));
    }

    public String getArgumentsString() {
        if (methodParam.args == null || methodParam.args.length == 0) {
            return "无参数";
        }

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < methodParam.args.length; i++) {
            Object arg = methodParam.args[i];
            sb.append("[").append(i).append("] ");
            if (arg == null) {
                sb.append("null");
            } else if (arg.getClass().isArray()) {
                sb.append(Arrays.toString((Object[]) arg));
            } else {
                sb.append(arg.toString());
            }
            if (i < methodParam.args.length - 1) {
                sb.append(", ");
            }
        }
        return sb.toString();
    }

    public String getStackTraceString(int maxLines) {
        StackTraceElement[] stackTrace = thread.getStackTrace();
        StringBuilder sb = new StringBuilder();
        int count = 0;

        for (StackTraceElement element : stackTrace) {
            if (count >= maxLines) break;

            String className = element.getClassName();
            if (className.startsWith("com.justnothing.testmodule") ||
                className.startsWith("de.robv.android.xposed")) {
                continue;
            }

            sb.append("  ").append(element).append("\n");
            count++;
        }

        return sb.toString();
    }


    @NonNull
    @Override
    public String toString() {
        return String.format(
                Locale.getDefault(),
                "HookContext[task=%d, method=%s, time=%s, depth=%d]",
                task.getId(),
                task.getDisplayName(),
                getFormattedTimestamp(),
                callDepth);
    }
}
