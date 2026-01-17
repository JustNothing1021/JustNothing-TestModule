package com.justnothing.testmodule.hooks.android.behavior;

import android.content.ContentValues;

import com.justnothing.testmodule.hooks.PackageHook;

import java.util.List;

import de.robv.android.xposed.XC_MethodReplacement;

public class BehaviorUtilHook extends PackageHook {

    public final String TAG = "BehaviorUtilHook";
    public final List<String> whiteList = List.of("app_activity_time");
    @Override
    protected void hookImplements() {
        setHookDisplayName("屏蔽行为上报工具");
        setHookDescription("用来防止手表上报信息给云端, 防止云控");
        setHookCondition(
                param -> param.packageName.contains("xtc") || param.packageName.equals("android")
        );
        hookMethod(
                "com.xtc.behavior.XtcBehaviorManager",
                "sendData",
                ContentValues.class,
                new XC_MethodReplacement() {
                    @Override
                    protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
                        try {
                            ContentValues content = (ContentValues) param.args[0];
                            String data = content.valueSet().toString();
                            info("XtcBehaviorManager.sendData被调用，接收数据如下");
                            info(data);
                            return null;
                        } catch (Exception e) {
                            error("replaceHookedMethod异常: " + e.getMessage(), e);
                            return null;
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