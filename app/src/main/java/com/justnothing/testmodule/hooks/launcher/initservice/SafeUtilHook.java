package com.justnothing.testmodule.hooks.launcher.initservice;

import android.content.Context;

import com.justnothing.testmodule.hooks.PackageHook;

public class SafeUtilHook extends PackageHook {

    public final String TAG = "SafeUtilHook";
    public final String SAFEUTIL_CLASSNAME = "com.xtc.initservice.util.SafeUtil";
    public final String TARGET_PACKAGE = "com.xtc.i3launcher";

    @Override
    protected void hookImplements() {
        setHookDisplayName("破解桌面系统安全签名工具");
        setHookDescription("调试用, 用来让桌面以为所有软件都在白名单里");
        setHookCondition(
            param -> param.packageName.equals(TARGET_PACKAGE)
        );
        hookAlwaysTrue(SAFEUTIL_CLASSNAME, "isPkgSignLegal", Context.class, String.class);
    }

    @Override
    public String getTag() {
        return TAG;
    }
}
