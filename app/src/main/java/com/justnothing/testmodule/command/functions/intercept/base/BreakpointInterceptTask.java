package com.justnothing.testmodule.command.functions.intercept.base;

import androidx.annotation.NonNull;

import com.justnothing.testmodule.command.framework.i18n.CliMessages;
import com.justnothing.testmodule.command.framework.i18n.Text;
import com.justnothing.testmodule.command.functions.intercept.InterceptTexts;
import com.justnothing.testmodule.hooks.api.HookParam;
import com.justnothing.testmodule.hooks.api.MethodHook;
import com.justnothing.testmodule.utils.expr.SignatureUtils;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Locale;

public class BreakpointInterceptTask extends AbstractInterceptTask {

    private volatile long lastHitAt = 0;

    public BreakpointInterceptTask(int id, String className, String methodName, String signature,
                                   ClassLoader classLoader) {
        super(id, className, methodName, signature, classLoader, TaskType.BREAKPOINT);
    }

    @Override
    protected MethodHook createMethodHook() {
        return new MethodHook() {
            @Override
            protected void beforeHookedMethod(HookParam param) {
                if (!enabled) return;

                lastHitAt = System.currentTimeMillis();
                hitCount.incrementAndGet();

                String timestamp = new SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault()).format(new Date());
                String actualSignature = SignatureUtils.formatReadableParamList(param.getHookedMethod());

                logger.info("=== 断点命中 ===");
                logger.info("ID: " + id);
                logger.info("时间: " + timestamp);
                logger.info("类: " + className);
                logger.info("方法: " + methodName);
                logger.info("签名: " + actualSignature);
                logger.info("命中次数: " + hitCount.get());

                Object[] args = param.getArgs();
                if (args != null && args.length > 0) {
                    logger.info("参数:");
                    for (int i = 0; i < args.length; i++) {
                        Object arg = args[i];
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
        sb.append(Text.zhEn("=== 断点信息 ===\n", "=== Breakpoint info ===\n").text());
        sb.append("ID: ").append(id).append("\n");
        sb.append(CliMessages.LABEL_CLASS.text()).append(className).append("\n");
        sb.append(CliMessages.LABEL_METHOD.text()).append(methodName).append("\n");
        if (signature != null) {
            sb.append(InterceptTexts.LABEL_SIGNATURE.text()).append(signature).append("\n");
        }
        sb.append(Text.zhEn("命中次数: ", "Hits: ").text()).append(hitCount.get()).append("\n");
        sb.append(Text.zhEn("状态: ", "Status: ").text()).append(running.get() ? (enabled ? InterceptTexts.STATUS_RUNNING.text() : InterceptTexts.STATUS_PAUSED.text()) : InterceptTexts.STATUS_STOPPED.text()).append("\n");
        if (lastHitAt > 0) {
            sb.append(Text.zhEn("最后命中: ", "Last hit: ").text()).append(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault())
                    .format(new Date(lastHitAt))).append("\n");
        }
        return sb.toString();
    }


    @NonNull
    @Override
    public String toString() {
        return Text.zhEn("Breakpoint[%d] %s (命中: %d, 状态: %s)",
                        "Breakpoint[%d] %s (hits: %d, status: %s)")
                .format(
                        id,
                        getDisplayName(),
                        hitCount.get(),
                        running.get() ? (enabled ? InterceptTexts.STATUS_RUNNING.text() : InterceptTexts.STATUS_PAUSED.text()) : InterceptTexts.STATUS_STOPPED.text());
    }
}
