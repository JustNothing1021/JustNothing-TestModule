package com.justnothing.testmodule.hooks.android;

import com.justnothing.testmodule.hooks.api.HookParam;
import com.justnothing.testmodule.hooks.api.MethodHook;
import com.justnothing.testmodule.hooks.base.PackageHook;

public class SystemExceptionManagerHook extends PackageHook {

    public final String TAG = "SystemExceptionManagerHook";

    public final String CLASSNAME = "com.android.server.xgseserver.xss.SystemExceptionManager";

    @Override
    protected void hookImplements() {
        setHookDisplayName("屏蔽系统反破解提示");
        setHookDescription("去掉烦人的水印, 防止一些奇怪的弹窗之类的");
        setHookCondition(
            param -> param.packageName.equals("android")
        );
        hookMethod(
            CLASSNAME,
            "messageDialog",
            String.class,
            new MethodHook() {
                @Override
                protected void beforeHookedMethod(HookParam param) {
                    warn("messageDialog被调用，信息: " + param.getArgs()[0]);
                    warn("由于已经hook了BehaviorUtils，不会上报数据，将会继续执行该方法");
                }
            }
        );

        hookMethod(
                CLASSNAME,
                "showStrictModeOverlay",
                String.class,
                new MethodHook() {
                    @Override
                    protected void beforeHookedMethod(HookParam param) {
                        warn("showStrictModeOverlay被调用，信息: " + param.getArgs()[0]);
                        warn("由于已经hook了BehaviorUtils，不会上报数据，将会继续执行该方法");
                    }
                }
        );
        hookDoNothing(CLASSNAME, "displaySPEUimode"); // 不会只有我觉得这玩意还有点好看吧（
        hookDoNothing(CLASSNAME, "doUninstall", String.class);
        hookDoNothing(CLASSNAME, "uninstall", String.class);
        hookDoNothing(CLASSNAME, "doDenyNetworkAccessApp", String.class);
        hookDoNothing(CLASSNAME, "denyNetworkAccessApp", String.class);
        hookDoNothing(CLASSNAME, "denyNetworkAccessAddress", String.class);
        hookDoNothing(CLASSNAME, "doDisableApp", String.class);
        hookDoNothing(CLASSNAME, "disableApp", String.class);
    }

    @Override
    public String getTag() {
        return TAG;
    }
}
