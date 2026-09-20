package com.justnothing.testmodule.hooks.api;

/**
 * 方法替换 Hook，完全替代原始方法实现。
 * <p>
 * 替代 {@code XC_MethodReplacement}，
 * 使调用者无需直接依赖 Xposed API。
 */
public abstract class MethodReplacement extends MethodHook {

    /**
     * 替换原始方法的实现。
     * 返回值将作为原始方法的返回值。
     */
    protected abstract Object replaceHookedMethod(HookParam param) throws Throwable;

    @Override
    protected final void beforeHookedMethod(HookParam param) throws Throwable {
        param.setResult(replaceHookedMethod(param));
    }

    @Override
    protected final void afterHookedMethod(HookParam param) throws Throwable {
        // 替换模式不需要 after 回调
    }
}
