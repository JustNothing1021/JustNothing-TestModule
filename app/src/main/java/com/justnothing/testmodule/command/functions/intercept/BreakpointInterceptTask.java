package com.justnothing.testmodule.command.functions.intercept;

import androidx.annotation.NonNull;

import com.justnothing.testmodule.utils.reflect.SignatureUtils;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Locale;

import de.robv.android.xposed.XC_MethodHook;

public class BreakpointInterceptTask extends AbstractInterceptTask {

    private volatile long lastHitAt = 0;

    public BreakpointInterceptTask(int id, String className, String methodName, String signature,
                                   ClassLoader classLoader) {
        super(id, className, methodName, signature, classLoader, TaskType.BREAKPOINT);
    }

    @Override
    protected XC_MethodHook createMethodHook() {
        return new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) {
                if (!enabled) return;

                lastHitAt = System.currentTimeMillis();
                hitCount.incrementAndGet();

                String timestamp = new SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault()).format(new Date());
                String actualSignature = SignatureUtils.formatReadableParamList(param.method);

                logger.info("=== 断点命中 ===");
                logger.info("ID: " + id);
                logger.info("时间: " + timestamp);
                logger.info("类: " + className);
                logger.info("方法: " + methodName);
                logger.info("签名: " + actualSignature);
                logger.info("命中次数: " + hitCount.get());

                if (param.args != null && param.args.length > 0) {
                    logger.info("参数:");
                    for (int i = 0; i < param.args.length; i++) {
                        Object arg = param.args[i];
                        String argStr = arg != null ? arg.toString() : "null";
                        if (arg != null && arg.getClass().isArray()) {
                            argStr = Arrays.toString((Object[]) arg);
                        }
                        logger.info("  [" + i + "] " + (arg != null ? arg.getClass().getName() : "null") + " = " + argStr);
                    }
                }

                logger.info("调用栈:");
                StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
                for (int i = 0; i < stackTrace.length && i < 20; i++) {
                    logger.info("  " + stackTrace[i]);
                }
                logger.info("================");
            }
        };
    }

    public long getLastHitAt() {
        return lastHitAt;
    }

    public String getBreakpointInfo() {
        StringBuilder sb = new StringBuilder();
        sb.append("=== 断点信息 ===\n");
        sb.append("ID: ").append(id).append("\n");
        sb.append("类: ").append(className).append("\n");
        sb.append("方法: ").append(methodName).append("\n");
        if (signature != null) {
            sb.append("签名: ").append(signature).append("\n");
        }
        sb.append("命中次数: ").append(hitCount.get()).append("\n");
        sb.append("状态: ").append(running.get() ? (enabled ? "运行中" : "已暂停") : "已停止").append("\n");
        if (lastHitAt > 0) {
            sb.append("最后命中: ").append(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault())
                    .format(new Date(lastHitAt))).append("\n");
        }
        return sb.toString();
    }


    @NonNull
    @Override
    public String toString() {
        return String.format(
                Locale.getDefault(),
                "Breakpoint[%d] %s (命中: %d, 状态: %s)",
                id,
                getDisplayName(),
                hitCount.get(),
                running.get() ? (enabled ? "运行中" : "已暂停") : "已停止");
    }
}
