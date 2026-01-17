package com.justnothing.testmodule.hooks.launcher.initservice;

import android.content.Context;
import android.net.Uri;

import com.justnothing.testmodule.hooks.PackageHook;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;

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
            new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    try {
                        String callingPackage = (String) XposedHelpers.callMethod(param.thisObject,
                                "getCallingPackage");
                        info(callingPackage + "尝试访问" + param.args[0].toString());
                        if (ADB_PKG_NAME.equals(callingPackage)) {
                            info("此次为shell访问，准备绕过...");

                            Context context = (Context) XposedHelpers.callMethod(param.thisObject,
                                    "getContext");
                            if (context != null) {
                                String targetPackageName = context.getPackageName();
                                XposedHelpers.setObjectField(param.thisObject,
                                        "callingPackage", targetPackageName);
                                info("已将包名从" + ADB_PKG_NAME + "修改为" + targetPackageName);
                            }
                        }
                    } catch (Exception e) {
                        error("beforeHookedMethod异常: " + e.getMessage(), e);
                    }
                }

                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
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
