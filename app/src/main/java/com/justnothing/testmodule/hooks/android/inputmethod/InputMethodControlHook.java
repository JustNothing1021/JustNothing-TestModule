package com.justnothing.testmodule.hooks.android.inputmethod;

import android.content.ContentValues;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import com.justnothing.testmodule.hooks.PackageHook;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.callbacks.XC_LoadPackage;


public class InputMethodControlHook extends PackageHook {
    
    private static final String TAG = "InputMethodControlHook";
    
    @Override
    protected void hookImplements() {
        setHookDisplayName("输入法控制");
        setHookDescription("解决文本框一出现输入法就会自动弹出的问题");
        hookInputMethodManagerOnPostWindowFocus();
        info("输入法控制Hook初始化完成");
    }
    

    private void hookInputMethodManagerOnPostWindowFocus() {
        try {
            hookMethod(
                "android.view.inputmethod.InputMethodManager",
                "onPostWindowFocus",
                View.class, View.class, int.class, boolean.class, int.class,
                new XC_MethodReplacement() {
                    @Override
                    protected Object replaceHookedMethod(MethodHookParam param) {
                        info("拦截输入法弹出");
                        return null;
                    }
                }
            );
            debug("成功Hook InputMethodManager.onPostWindowFocus方法");
        } catch (Exception e) {
            error("Hook InputMethodManager.onPostWindowFocus失败: " + e.getMessage());
        }
    }
    
    @Override
    public String getTag() {
        return TAG;
    }
}