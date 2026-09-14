package com.justnothing.testmodule.hooks.android;

import android.content.ContentValues;

import com.justnothing.testmodule.hooks.api.HookParam;
import com.justnothing.testmodule.hooks.api.MethodReplacement;
import com.justnothing.testmodule.hooks.PackageHook;

public class BehaviorUtilHook extends PackageHook {

    public final String TAG = "BehaviorUtilHook";
    // public final List<String> whiteList = List.of("app_activity_time"); // TODO
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
                new MethodReplacement() {
                    @Override
                    protected Object replaceHookedMethod(HookParam param) {
                        try {
                            ContentValues content = (ContentValues) param.getArgs()[0];
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