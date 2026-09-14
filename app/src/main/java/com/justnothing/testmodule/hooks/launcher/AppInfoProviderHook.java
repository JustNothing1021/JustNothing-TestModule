package com.justnothing.testmodule.hooks.launcher;

import android.content.Context;
import android.net.Uri;

import com.justnothing.testmodule.hooks.api.HookAPI;
import com.justnothing.testmodule.hooks.api.HookParam;
import com.justnothing.testmodule.hooks.api.MethodHook;
import com.justnothing.testmodule.hooks.PackageHook;
import com.justnothing.testmodule.utils.reflect.ReflectionUtils;

public class AppInfoProviderHook extends PackageHook {

    public static final String TAG = "AppInfoProviderHook";
    private static final String TARGET_PACKAGE = "com.xtc.i3launcher";
    private static final String ADB_PKG_NAME = "com.android.shell";
    private static final String PROVIDER_CLASSNAME = "com.xtc.initservice.provider.AppInfoProvider";

    public void hookImplements() {
        setHookDisplayName("破解桌面ContentProvider");
        setHookDescription("调试用, 可开可不开, 开了可以让其他软件允许访问initservice的数据");
        setHookCondition(
            param -> param.packageName.equals(TARGET_PACKAGE)
        );
        hookMethod(
            PROVIDER_CLASSNAME,
            "query",
            Uri.class, String[].class, String.class, String[].class, String.class,
            new MethodHook() {
                @Override
                protected void beforeHookedMethod(HookParam param) {
                    try {
                        String callingPackage = (String) ReflectionUtils.callMethod(param.getThisObject(),
                                "getCallingPackage");
                        info(callingPackage + " 尝试访问 " + param.getArgs()[0].toString());
                        if (ADB_PKG_NAME.equals(callingPackage)) {
                            info("此次为shell访问，准备绕过...");

                            Context context = (Context) ReflectionUtils.callMethod(param.getThisObject(),
                                    "getContext");
                            if (context != null) {
                                String targetPackageName = context.getPackageName();
                                HookAPI.setObjectField(param.getThisObject(),
                                        "callingPackage", targetPackageName);
                                info("已将包名从 " + ADB_PKG_NAME + " 修改为 " + targetPackageName);
                            }
                        }
                    } catch (Exception e) {
                        error("beforeHookedMethod异常: " + e.getMessage(), e);
                    }
                }

                @Override
                protected void afterHookedMethod(HookParam param) {
                    try {
                        if (param.getResult() == null) {
                            warn("查询依旧返回null");
                        }
                    } catch (Exception e) {
                        error("afterHookedMethod异常: " + e.getMessage(), e);
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
