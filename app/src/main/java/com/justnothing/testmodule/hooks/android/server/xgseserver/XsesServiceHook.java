package com.justnothing.testmodule.hooks.android.server.xgseserver;

import android.content.Intent;

import com.justnothing.testmodule.hooks.PackageHook;

import de.robv.android.xposed.XC_MethodReplacement;

public class XsesServiceHook extends PackageHook {

    public final String TAG = "XsesServiceHook";
    public final String XSES_SERVICE_CLASSNAME = "com.android.server.xgseserver.xss.xsesService";
    public final String TARGET_PACKAGE = "android";

    @Override
    protected void hookImplements() {
        setHookDisplayName("屏蔽反破解模块");
        setHookDescription("用来防止系统检测到破解, 导致一直重启到bootloader (一般来说都要开, 关了可能会趋势)");
        setHookCondition(
                param -> param.packageName.equals(TARGET_PACKAGE)
        );
        hookDoNothing(XSES_SERVICE_CLASSNAME, "onStart");
        hookDoNothing(XSES_SERVICE_CLASSNAME, "onBootPhase", int.class);
        hookDoNothing(XSES_SERVICE_CLASSNAME, "updateModuleSwitch");
        hookDoNothing(XSES_SERVICE_CLASSNAME, "prepareRequest");
        hookDoNothing(XSES_SERVICE_CLASSNAME, "realRequest");
        hookDoNothing(XSES_SERVICE_CLASSNAME, "prepareSecurityCheck");
        hookDoNothing(XSES_SERVICE_CLASSNAME, "checkRoot");
        hookDoNothing(XSES_SERVICE_CLASSNAME, "checkBVC");
        hookDoNothing(XSES_SERVICE_CLASSNAME, "keepPushStatus");
        hookDoNothing(XSES_SERVICE_CLASSNAME, "checkPFStatus");
        hookDoNothing(XSES_SERVICE_CLASSNAME, "checkSystemStatus");
        hookAlwaysFalse(XSES_SERVICE_CLASSNAME, "isDRed");
        hookAlwaysFalse(XSES_SERVICE_CLASSNAME, "xsesCR");
        hookAlwaysFalse(XSES_SERVICE_CLASSNAME, "checkRootMethod2");
        hookDoNothing(XSES_SERVICE_CLASSNAME, "doCheckUsbStatus");
        hookDoNothing(XSES_SERVICE_CLASSNAME, "doCheckUsbStatus", Intent.class);
        hookDoNothing(XSES_SERVICE_CLASSNAME, "disableAdbFromUsb", String.class, String.class);
        // 虽然没必要但是还是写一下吧
        hookDoNothing(XSES_SERVICE_CLASSNAME, "saveRootInfo", String.class);
        hookDoNothing(XSES_SERVICE_CLASSNAME, "saveUsbInfo");

        hookMethod(XSES_SERVICE_CLASSNAME, "processServerPush",
                String.class,
                new XC_MethodReplacement() {
                    @Override
                    protected Object replaceHookedMethod(MethodHookParam methodHookParam) throws Throwable {
                        String data = (String) methodHookParam.args[0];
                        if (data == null || data.trim().isEmpty()) {
                            warn("processServerPush接收到空数据");
                            return null;
                        }
                        info("processServerPush被调用，数据长度: " + data.length());
                        info(data);
                        return null;
                    }
                }
        );
    }

    @Override
    public String getTag() {
        return TAG;
    }
}