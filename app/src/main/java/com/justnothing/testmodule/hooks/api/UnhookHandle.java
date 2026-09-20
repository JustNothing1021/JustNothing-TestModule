package com.justnothing.testmodule.hooks.api;

import java.lang.reflect.Member;

/**
 * Hook 取消句柄，用于移除已安装的 Hook。
 * <p>
 * 替代 {@code XC_MethodHook.Unhook}，
 * 使调用者无需直接依赖 Xposed API。
 */
public interface UnhookHandle {

    /**
     * 获取被 hook 的方法
     */
    Member getHookedMethod();

    /**
     * 取消 Hook
     */
    void unhook();
}
