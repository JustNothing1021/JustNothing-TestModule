package com.justnothing.testmodule.hooks.android.server.xgseserver;

import com.justnothing.testmodule.hooks.PackageHook;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;

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
            new XC_MethodReplacement() {
                @Override
                protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
                    String message = (String) param.args[0];
                    warn("已阻止system弹窗: " + message);
                    return null;
                }
            }
        );

        hookMethod(
                CLASSNAME,
                "showStrictModeOverlay",
                String.class,
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        warn("showStrictModeOverlay被调用，信息: " + param.args[0]);
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
