package com.justnothing.testmodule.hooks.api;

import java.lang.reflect.Member;
import java.util.HashMap;
import java.util.Map;

/**
 * Hook 回调参数，封装被 hook 方法的调用信息。
 * <p>
 * 替代 {@code XC_MethodHook.MethodHookParam}，
 * 使调用者无需直接依赖 Xposed API。
 */
public class HookParam {

    private final Member hookedMethod;
    private final Object thisObject;
    private Object[] args;
    private Object result;
    private Throwable throwable;
    private boolean resultSet = false;
    private Map<String, Object> extras;

    public HookParam(Member hookedMethod, Object thisObject, Object[] args) {
        this.hookedMethod = hookedMethod;
        this.thisObject = thisObject;
        this.args = args;
    }

    public Member getHookedMethod() {
        return hookedMethod;
    }

    public Object getThisObject() {
        return thisObject;
    }

    public Object[] getArgs() {
        return args;
    }

    public void setArgs(Object[] args) {
        this.args = args;
    }

    public Object getResult() {
        return result;
    }

    public void setResult(Object result) {
        this.result = result;
        this.resultSet = true;
        this.throwable = null;
    }

    public Throwable getThrowable() {
        return throwable;
    }

    public void setThrowable(Throwable throwable) {
        this.throwable = throwable;
        this.resultSet = true;
        this.result = null;
    }

    public boolean isResultSet() {
        return resultSet;
    }

    /**
     * 重置 result 状态（用于 afterHookedMethod 中恢复原始行为）
     */
    public void resetResult() {
        this.result = null;
        this.throwable = null;
        this.resultSet = false;
    }

    public void setObjectExtra(String key, Object value) {
        if (extras == null) {
            extras = new HashMap<>();
        }
        extras.put(key, value);
    }

    public Object getObjectExtra(String key) {
        if (extras == null) return null;
        return extras.get(key);
    }
}
