package com.justnothing.testmodule.hooks.api;


import java.lang.reflect.Field;
import java.lang.reflect.Method;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;


/**
 * Hook API 适配层。
 * <p>
 * 对外暴露自定义的 {@link MethodHook}/{@link MethodReplacement}/{@link UnhookHandle}/{@link HookParam}，
 * 内部委托给 Xposed 实现。调用者无需直接 import 任何 {@code de.robv.android.xposed} 类型。
 */
public class HookAPI {


    static XC_MethodHook toXposed(MethodHook hook) {
        return new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                HookParam hp = fromXposed(param);
                hook.beforeHookedMethod(hp);
                applyToXposed(hp, param);
            }

            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                HookParam hp = fromXposed(param);
                hook.afterHookedMethod(hp);
                applyToXposed(hp, param);
            }
        };
    }

    static HookParam fromXposed(XC_MethodHook.MethodHookParam param) {
        return new HookParam(param.method, param.thisObject, param.args);
    }

    static void applyToXposed(HookParam hp, XC_MethodHook.MethodHookParam param) {
        if (hp.isResultSet()) {
            if (hp.getThrowable() != null) {
                param.setThrowable(hp.getThrowable());
            } else {
                param.setResult(hp.getResult());
            }
        }
        param.args = hp.getArgs();
    }

    static UnhookHandle fromXposedUnhook(XC_MethodHook.Unhook unhook) {
        return new UnhookHandle() {
            @Override
            public java.lang.reflect.Member getHookedMethod() {
                return unhook.getHookedMethod();
            }

            @Override
            public void unhook() {
                unhook.unhook();
            }
        };
    }

    // ─── 对外 API ───────────────────────────────────────────────────────

    public static UnhookHandle findAndHookMethod(
            Class<?> clazz,
            String methodName,
            Object... parameterTypesAndHook
    ) {
        Object[] xposedArgs = toXposedArgs(parameterTypesAndHook);
        XC_MethodHook.Unhook unhook = XposedHelpers.findAndHookMethod(clazz, methodName, xposedArgs);
        return fromXposedUnhook(unhook);
    }

    public static UnhookHandle findAndHookMethod(
            String className,
            ClassLoader cl,
            String methodName,
            Object... parameterTypesAndHook
    ) {
        Object[] xposedArgs = toXposedArgs(parameterTypesAndHook);
        XC_MethodHook.Unhook unhook = XposedHelpers.findAndHookMethod(className, cl, methodName, xposedArgs);
        return fromXposedUnhook(unhook);
    }

    public static UnhookHandle findAndHookConstructor(
            Class<?> clazz,
            Object... parameterTypesAndHook
    ) {
        Object[] xposedArgs = toXposedArgs(parameterTypesAndHook);
        XC_MethodHook.Unhook unhook = XposedHelpers.findAndHookConstructor(clazz, xposedArgs);
        return fromXposedUnhook(unhook);
    }

    public static Class<?> findClassIfExists(String className, ClassLoader cl) {
        return XposedHelpers.findClassIfExists(className, cl);
    }

    public static Method findMethodExact(Class<?> clazz, String methodName, Class<?>... parameterTypes) throws NoSuchMethodException {
        try {
            return XposedHelpers.findMethodExact(clazz, methodName, parameterTypes);
        } catch (NoSuchMethodError e) {
            throw new NoSuchMethodException(clazz.getName() + "." + methodName);
        }
    }

    public static Method findMethodExactIfExists(Class<?> clazz, String methodName, Class<?>... parameterTypes) {
        try {
            return XposedHelpers.findMethodExact(clazz, methodName, parameterTypes);
        } catch (NoSuchMethodError e) {
            return null;
        }
    }

    public static Field findFieldIfExists(Class<?> clazz, String fieldName) {
        try {
            return XposedHelpers.findFieldIfExists(clazz, fieldName);
        } catch (NoSuchFieldError e) {
            return null;
        }
    }

    public static void setStaticObjectField(Class<?> clazz, String fieldName, Object value) throws NoSuchFieldException {
        try {
            XposedHelpers.setStaticObjectField(clazz, fieldName, value);
        } catch (NoSuchFieldError e) {
            throw new NoSuchFieldException(clazz.getName() + "." + fieldName);
        }
    }

    public static Object getStaticObjectField(Class<?> clazz, String fieldName) throws NoSuchFieldException {
        try {
            return XposedHelpers.getStaticObjectField(clazz, fieldName);
        } catch (NoSuchFieldError e) {
            throw new NoSuchFieldException(clazz.getName() + "." + fieldName);
        }
    }

    public static void setObjectField(Object object, String fieldName, Object value) throws NoSuchFieldException {
        try {
            XposedHelpers.setObjectField(object, fieldName, value);
        } catch (NoSuchFieldError e) {
            throw new NoSuchFieldException(object.getClass().getName() + "." + fieldName);
        }
    }

    public static Object getObjectField(Object object, String fieldName) throws NoSuchFieldException {
        try {
            return XposedHelpers.getObjectField(object, fieldName);
        } catch (NoSuchFieldError e) {
            throw new NoSuchFieldException(object.getClass().getName() + "." + fieldName);
        }
    }

    // ─── 内部工具方法 ───────────────────────────────────────────────────

    /**
     * 将自定义 MethodHook 转换为 Xposed 可接受的参数数组。
     * parameterTypesAndHook 的最后一个元素可能是 MethodHook 或 XC_MethodHook，
     * 其余是 Class<?> 参数类型。
     */
    private static Object[] toXposedArgs(Object[] parameterTypesAndHook) {
        if (parameterTypesAndHook.length == 0) return parameterTypesAndHook;
        Object last = parameterTypesAndHook[parameterTypesAndHook.length - 1];
        if (last instanceof MethodHook) {
            Object[] result = new Object[parameterTypesAndHook.length];
            System.arraycopy(parameterTypesAndHook, 0, result, 0, parameterTypesAndHook.length - 1);
            result[parameterTypesAndHook.length - 1] = toXposed((MethodHook) last);
            return result;
        }
        // 已经是 XC_MethodHook（兼容旧调用方式）
        return parameterTypesAndHook;
    }
}
