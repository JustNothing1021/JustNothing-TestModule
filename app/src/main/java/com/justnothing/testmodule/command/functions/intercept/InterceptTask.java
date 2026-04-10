package com.justnothing.testmodule.command.functions.intercept;

import java.util.List;

import de.robv.android.xposed.XC_MethodHook;

public interface InterceptTask {

    int getId();

    void setId(int id);

    String getClassName();

    String getMethodName();

    String getSignature();

    ClassLoader getClassLoader();

    TaskType getType();

    boolean isEnabled();

    void setEnabled(boolean enabled);

    int getHitCount();

    boolean isRunning();

    void start();

    void stop();

    void onHook(XC_MethodHook.MethodHookParam param);


    default String getDisplayName() {
        String sig = getSignature();
        return getClassName() + "." + getMethodName() + (sig != null ? "(" + sig + ")" : "");
    }

    default void onInstall() {}

    default void onActivated() {}

    default void onDeactivated() {}

    default void onUninstall() {}

    List<XC_MethodHook.Unhook> getActiveHooks();
}
