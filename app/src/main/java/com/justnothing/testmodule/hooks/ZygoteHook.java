package com.justnothing.testmodule.hooks;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.IXposedHookZygoteInit;
import de.robv.android.xposed.XposedHelpers;

public abstract class ZygoteHook extends XposedBasicHook<IXposedHookZygoteInit.StartupParam> {


    protected ZygoteHook() {
        super();
    }

    public class ZygoteMethodHook extends BaseMethodHook {

        public ZygoteMethodHook(String className, String methodName,
                                Object[] signature, XC_MethodHook hook,
                                HookCondition<IXposedHookZygoteInit.StartupParam> shouldLoad) {
            super(className, methodName, signature, hook, shouldLoad);
        }

        public ZygoteMethodHook(String className, String methodName,
                                Object[] signature, XC_MethodHook hook) {
            super(className, methodName, signature, hook);
        }

        @Override
        public Boolean loads(IXposedHookZygoteInit.StartupParam param) {
            try {
                if (!shouldLoad(param)) return false;
                Class<?> clazz = ClassFinder.find(className);
                if (clazz == null) {
                    warn("未找到类" + className);
                    return false;
                }
                Method method = MethodFinder.find(className, methodName);
                if (method == null) {
                    warn("未找到类方法" + className + "." + methodName);
                    return false;
                }
                Object[] sigWithHook = new Object[signature.length + 1];
                System.arraycopy(signature, 0, sigWithHook, 0, signature.length);
                sigWithHook[signature.length] = hook;
                XposedHelpers.findAndHookMethod(clazz, methodName, sigWithHook);
                info("已hook " + className + "." + methodName);
                return true;
            } catch (Throwable e) {
                error("方法" + className + "." + methodName + "hook失败:", e);
                return false;
            }
        }
    }


    public class ZygoteFieldHook extends BaseFieldHook {

        public ZygoteFieldHook(String className, String fieldName, Object value,
                               HookCondition<IXposedHookZygoteInit.StartupParam> shouldLoad) {
            super(className, fieldName, value, shouldLoad);
        }
        public ZygoteFieldHook(String className, String fieldName, Object value) {
            super(className, fieldName, value, null);
        }

        @Override
        public Boolean loads(IXposedHookZygoteInit.StartupParam param) {
            try {
                Class<?> clazz = ClassFinder.find(className);
                if (clazz == null) {
                    warn("没有找到类 " + className);
                    return false;
                }
                Field field = XposedHelpers.findFieldIfExists(clazz, fieldName);
                if (field == null) {
                    warn("没有找到" + className + "的静态量" + fieldName);
                    return false;
                }
                XposedHelpers.setStaticObjectField(clazz, fieldName, value);
                info("已hook静态成员 " + className + "." + fieldName);
                return true;
            } catch (Throwable e) {
                error("的常量" + className + "." + fieldName + "hook失败:", e);
                return false;
            }
        }
    }


    public class ZygoteCallbackHook extends BaseCallbackHook {

        public ZygoteCallbackHook(HookCallback<IXposedHookZygoteInit.StartupParam> hookCallback,
                                  HookCondition<IXposedHookZygoteInit.StartupParam> shouldLoad) {
            super(hookCallback, shouldLoad);
        }
        public ZygoteCallbackHook(HookCallback<IXposedHookZygoteInit.StartupParam> hookCallback) {
            super(hookCallback, null);
        }

        @Override
        public Boolean loads(IXposedHookZygoteInit.StartupParam param) {
            try {
                boolean stat = callback.execute(param);
                info("自定义Zygote任务" + (stat ? "执行成功" : "执行失败"));
                return stat;
            } catch (Throwable e) {
                error("执行自定义任务时出错", e);
                return false;
            }
        }
    }

    @Override
    protected BaseMethodHook createMethodHook(
            String className, String methodName, Object[] signature,
            XC_MethodHook hook, HookCondition<IXposedHookZygoteInit.StartupParam> shouldLoad) {
        return new ZygoteMethodHook(className, methodName, signature, hook, shouldLoad);
    }

    @Override
    protected ZygoteFieldHook createFieldHook(
            String className, String fieldName, Object value,
            HookCondition<IXposedHookZygoteInit.StartupParam> shouldLoad) {
        return new ZygoteFieldHook(className, fieldName, value, shouldLoad);
    }

    @Override
    protected ZygoteCallbackHook createCallbackHook(
            HookCallback<IXposedHookZygoteInit.StartupParam> hookCallback,
            HookCondition<IXposedHookZygoteInit.StartupParam> shouldLoad
    ) {
        return new ZygoteCallbackHook(hookCallback, shouldLoad);
    }


    protected BaseMethodHook createHookConstructor(String className, XC_MethodHook hook,
                                                   Object... paramTypes) {
        // 构造函数的方法名是固定的 "<init>"
        return createMethodHook(className, "<init>", paramTypes, hook);
    }

    protected void hookConstructor(String className, XC_MethodHook hook, Object... paramTypes) {
        addHook(createHookConstructor(className, hook, paramTypes));
    }

    @Override
    protected boolean installHooks(IXposedHookZygoteInit.StartupParam param) {
        if (!shouldLoad(param)) return false;

        int successCount = 0;
        int failureCount = 0;
        boolean hasFailures = false;

        for (BaseHook hook : getHooks()) {
            try {
                if (hook.loads(param)) {
                    successCount++;
                } else {
                    failureCount++;
                    hasFailures = true;
                }
            } catch (Exception e) {
                error("Hook执行异常", e);
                failureCount++;
                hasFailures = true;
            }
        }

        info(String.format("执行完成，结果: 成功: %d, 失败: %d, 总计: %d",
                successCount, failureCount, getHookCount()));

        return !hasFailures;
    }

    @Override
    protected abstract void hookImplements();

}