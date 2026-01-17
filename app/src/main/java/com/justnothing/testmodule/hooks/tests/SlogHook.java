package com.justnothing.testmodule.hooks.tests;

import com.justnothing.testmodule.hooks.XposedBasicHook;
import com.justnothing.testmodule.hooks.ZygoteHook;

import de.robv.android.xposed.XC_MethodHook;

public class SlogHook extends ZygoteHook {
    private static final String TARGET_PACKAGE = "android";
    private static final String TARGET_LOG_KEYWORD = "device has no apply for install permission";
    public static final String TAG = "SlogHook";

    @Override
    protected void hookImplements() {
        setHookDisplayName("SuperLog监听器");
        setHookDescription("以前用来研究为什么安不上软件的遗留物品, 尽量不要用");
        hookMethod(
            "android.util.Slog",
            "w",
            String.class, String.class,
            new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(XC_MethodHook.MethodHookParam param) throws Throwable {
                    try {
                        String tag = (String) param.args[0];
                        String msg = (String) param.args[1];
                        if (msg != null && msg.contains(TARGET_LOG_KEYWORD)) {
                            info("发现目标日志！");
                            info("Tag: " + tag + ", Msg: " + msg);
                            Exception stackTrace = new Exception("调用堆栈追踪");
                            info(stackTrace);
                        }
                    } catch (Exception e) {
                        error("beforeHookedMethod异常: " + e.getMessage(), e);
                    }
                }
            }
        );
    }

    @Override
    public String getTag() {
        return TAG;
    }
}
