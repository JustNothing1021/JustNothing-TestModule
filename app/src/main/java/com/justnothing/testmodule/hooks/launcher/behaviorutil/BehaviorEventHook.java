package com.justnothing.testmodule.hooks.launcher.behaviorutil;

import com.justnothing.testmodule.hooks.PackageHook;

public class BehaviorEventHook extends PackageHook {

    public final String TAG = "BehaviorEventHook";

    @Override
    protected void hookImplements() {
        setHookDisplayName("屏蔽桌面行为上报工具");
        setHookDescription("用来hook桌面的某些行为上报工具, 治标不治本, 弃用了, 建议安装改版桌面");
        setHookCondition(
            param -> param.packageName.equals("com.xtc.i3launcher")
        );
        hookCallback((param -> {
            warn("因为桌面的大多数类被混淆了，很多时候找不到具体的方法，更好的方法是安装修改版");
            warn("这里只简单hook几个可能上报数据的方法");
            return true;
        }));
        hookDoNothing("com.xtc.initservice.behavior.BehaviorEvent",
                    "notInWhiteListEvent");
        hookDoNothing("com.xtc.initservice.behavior.BehaviorEvent",
                "behaviorSignatureResult");
        hookDoNothing("com.xtc.initservice.behavior.BehaviorEvent",
                "whiteListUploadFail");
        hookDoNothing("com.xtc.initservice.behavior.BehaviorEvent",
                "chargePlayEvent");
    }

    @Override
    public String getTag() {
        return TAG;
    }
}
