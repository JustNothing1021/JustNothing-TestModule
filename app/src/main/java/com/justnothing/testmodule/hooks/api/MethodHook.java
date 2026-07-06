package com.justnothing.testmodule.hooks.api;

/**
 * 方法 Hook 回调基类，支持 before/after 两个阶段。
 * <p>
 * 替代 {@code XC_MethodHook}，
 * 使调用者无需直接依赖 Xposed API。
 */
public abstract class MethodHook {

    /**
     * 方法调用前触发。
     * 可以通过 {@code param.setResult()} 或 {@code param.setThrowable()} 阻止原始方法执行。
     */
    protected void beforeHookedMethod(HookParam param) throws Throwable {
    }

    /**
     * 方法调用后触发。
     * 可以读取或修改返回值。
     */
    protected void afterHookedMethod(HookParam param) throws Throwable {
    }
}
