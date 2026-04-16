package com.justnothing.testmodule.command.functions.intercept;

import androidx.annotation.NonNull;

import com.justnothing.testmodule.utils.io.IOManager;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import de.robv.android.xposed.XC_MethodHook;

public class TraceInterceptTask extends AbstractInterceptTask {

    private final int maxCallRecords;
    private final LinkedList<CallRecord> callRecords = new LinkedList<>();
    private final Map<String, CallNode> callTree = new HashMap<>();

    public TraceInterceptTask(int id, String className, String methodName, String signature,
                              ClassLoader classLoader, int maxCallRecords) {
        super(id, className, methodName, signature, classLoader, TaskType.TRACE);
        this.maxCallRecords = maxCallRecords;
    }

    @Override
    protected XC_MethodHook createMethodHook() {
        return new XC_MethodHook() {
            private long startTime;

            @Override
            protected void beforeHookedMethod(MethodHookParam param) {
                startTime = System.currentTimeMillis();
                String timestamp = new SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault()).format(new Date());

                int depth = calculateCallDepth();
                CallRecord record = new CallRecord(
                        timestamp,
                        targetClass.getName(),
                        methodName,
                        depth,
                        param.args,
                        null,
                        null,
                        0
                );

                addCallRecord(record);
                updateCallTree(record);
                hitCount.incrementAndGet();
            }

            @Override
            protected void afterHookedMethod(MethodHookParam param) {
                long endTime = System.currentTimeMillis();
                long duration = endTime - startTime;
                String timestamp = new SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault()).format(new Date());

                int depth = calculateCallDepth();

                CallRecord record = new CallRecord(
                        timestamp,
                        targetClass.getName(),
                        methodName,
                        depth,
                        param.args,
                        param.getResult(),
                        param.getThrowable(),
                        duration
                );

                addCallRecord(record);
            }
        };
    }

    private int calculateCallDepth() {
        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
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

    private void addCallRecord(CallRecord record) {
        synchronized (callRecords) {
            callRecords.addLast(record);
            if (callRecords.size() > maxCallRecords) {
                callRecords.removeFirst();
            }
        }
    }

    private void updateCallTree(CallRecord record) {
        synchronized (callTree) {
            String key = record.className + "." + record.methodName;
            CallNode node = callTree.get(key);
            if (node == null) {
                node = new CallNode(record.className, record.methodName);
                callTree.put(key, node);
            }
            node.incrementCallCount();
            node.updateMaxDepth(record.depth);

            if (record.exception != null) {
                node.incrementExceptionCount();
            }

            if (record.duration > 0) {
                node.updateDuration(record.duration);
            }

            if (record.returnValue != null) {
                node.addReturnValue(record.returnValue);
            }
        }
    }

    public int getCallCount() {
        return hitCount.get();
    }

    public String getTraceOutput(int limit) {
        synchronized (callRecords) {
            if (callRecords.isEmpty()) {
                return "暂无调用记录";
            }

            StringBuilder sb = new StringBuilder();
            sb.append("=== Trace 输出 ===\n");
            sb.append("任务ID: ").append(id).append("\n");
            sb.append("目标方法: ").append(className).append(".").append(methodName).append("\n");
            sb.append("总调用次数: ").append(hitCount.get()).append("\n\n");

            int count = 0;
            for (CallRecord record : callRecords) {
                if (limit > 0 && count >= limit) break;
                sb.append(record.toString()).append("\n");
                count++;
            }

            return sb.toString();
        }
    }

    public String getCallTree() {
        synchronized (callTree) {
            if (callTree.isEmpty()) return "暂无调用记录";

            StringBuilder sb = new StringBuilder();
            sb.append("=== 调用树 ===\n");
            sb.append("总调用次数: ").append(hitCount.get()).append("\n\n");

            for (CallNode node : callTree.values()) {
                sb.append(node.toString()).append("\n");
            }

            return sb.toString();
        }
    }

    public boolean exportToFile(String filePath) {
        synchronized (callRecords) {
            try {
                StringBuilder content = new StringBuilder();
                content.append("=== Trace 调用记录 ===\n");
                content.append("任务ID: ").append(id).append("\n");
                content.append("目标方法: ").append(className).append(".").append(methodName).append("\n");
                content.append("签名: ").append(signature != null ? signature : "所有").append("\n");
                content.append("总调用次数: ").append(hitCount.get()).append("\n");
                content.append("记录时间: ").append(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                        .format(new Date())).append("\n\n");

                content.append("=== 调用树 ===\n");
                for (CallNode node : callTree.values()) {
                    content.append(node.toString()).append("\n");
                }
                content.append("\n");

                content.append("=== 详细调用记录 ===\n");
                for (CallRecord record : callRecords) {
                    content.append(record.toString()).append("\n");
                }

                IOManager.writeFile(filePath, content.toString());
                return true;
            } catch (IOException e) {
                logger.error("导出trace记录失败", e);
                return false;
            }
        }
    }

    public record CallRecord(String timestamp, String className, String methodName, int depth,
                             Object[] args, Object returnValue, Throwable exception,
                             long duration) {

        @NonNull
        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("[").append(timestamp).append("] ");
            sb.append(className).append(".").append(methodName);
            sb.append(" (深度: ").append(depth).append(")");

            if (args != null && args.length > 0) {
                sb.append(" 参数: [");
                for (int i = 0; i < args.length; i++) {
                    sb.append(args[i] != null ? args[i].toString() : "null");
                    if (i < args.length - 1) {
                        sb.append(", ");
                    }
                }
                sb.append("]");
            }

            if (exception != null) {
                sb.append(" 异常: ").append(exception.getClass().getSimpleName())
                        .append(": ").append(exception.getMessage());
            } else if (returnValue != null) {
                sb.append(" 返回值: ").append(returnValue);
            }

            if (duration > 0) {
                sb.append(" 耗时: ").append(duration).append("ms");
            }

            return sb.toString();
        }
    }

    public static class CallNode {
        private final String className;
        private final String methodName;
        private final AtomicInteger callCount;
        private final AtomicInteger exceptionCount;
        private final List<Object> returnValues;
        private long totalDuration;
        private long maxDuration;
        private long minDuration;
        private int maxDepth;

        public CallNode(String className, String methodName) {
            this.className = className;
            this.methodName = methodName;
            this.callCount = new AtomicInteger(0);
            this.exceptionCount = new AtomicInteger(0);
            this.returnValues = new ArrayList<>();
            this.totalDuration = 0;
            this.maxDuration = 0;
            this.minDuration = Long.MAX_VALUE;
            this.maxDepth = 0;
        }

        public void incrementCallCount() {
            callCount.incrementAndGet();
        }

        public void incrementExceptionCount() {
            exceptionCount.incrementAndGet();
        }

        public void updateDuration(long duration) {
            totalDuration += duration;
            if (duration > maxDuration) {
                maxDuration = duration;
            }
            if (duration < minDuration) {
                minDuration = duration;
            }
        }

        public void updateMaxDepth(int depth) {
            if (depth > maxDepth) {
                maxDepth = depth;
            }
        }

        public void addReturnValue(Object value) {
            if (returnValues.size() < 10) {
                returnValues.add(value);
            }
        }

        public int getCallCount() {
            return callCount.get();
        }

        public int getExceptionCount() {
            return exceptionCount.get();
        }

        public int getMaxDepth() {
            return maxDepth;
        }

        public long getAvgDuration() {
            int count = callCount.get();
            return count > 0 ? totalDuration / count : 0;
        }

        public long getMaxDuration() {
            return maxDuration;
        }

        public long getMinDuration() {
            return minDuration == Long.MAX_VALUE ? 0 : minDuration;
        }

        @NonNull
        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append(className).append(".").append(methodName);
            sb.append(" (调用次数: ").append(callCount.get());

            if (exceptionCount.get() > 0) {
                sb.append(", 异常次数: ").append(exceptionCount.get());
            }

            sb.append(", 最大深度: ").append(maxDepth);

            if (totalDuration > 0) {
                sb.append(", 平均耗时: ").append(getAvgDuration()).append("ms");
                sb.append(", 最大耗时: ").append(maxDuration).append("ms");
                sb.append(", 最小耗时: ").append(getMinDuration()).append("ms");
            }

            sb.append(")");

            if (!returnValues.isEmpty()) {
                sb.append("\n  返回值示例: ");
                for (int i = 0; i < returnValues.size(); i++) {
                    sb.append(returnValues.get(i));
                    if (i < returnValues.size() - 1) {
                        sb.append(", ");
                    }
                }
            }

            return sb.toString();
        }
    }
}
