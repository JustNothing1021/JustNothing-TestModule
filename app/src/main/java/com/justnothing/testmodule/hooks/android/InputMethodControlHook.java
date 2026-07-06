package com.justnothing.testmodule.hooks.android;

import android.view.View;

import com.justnothing.testmodule.hooks.api.HookParam;
import com.justnothing.testmodule.hooks.api.MethodReplacement;
import com.justnothing.testmodule.hooks.base.PackageHook;


public class InputMethodControlHook extends PackageHook {
    
    private static final String TAG = "InputMethodControlHook";
    
    @Override
    protected void hookImplements() {
        setHookDisplayName("输入法控制");
        setHookDescription("解决文本框一出现输入法就会自动弹出的问题");
        hookMethod(
            "android.view.inputmethod.InputMethodManager",
            "onPostWindowFocus",
            View.class, View.class, int.class, boolean.class, int.class,
            new MethodReplacement() {
                @Override
                protected Object replaceHookedMethod(HookParam param) {
                    info("拦截输入法弹出");
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