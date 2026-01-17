package com.justnothing.testmodule.hooks;


import java.lang.reflect.Field;
import java.lang.reflect.Method;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public abstract class PackageHook extends XposedBasicHook<XC_LoadPackage.LoadPackageParam> {
    protected Integer hookSucceedMethods = 0;
    protected Integer hookSucceedPackages = 0;
    protected Integer hookFailureMethods = 0;
    protected Integer hookFailurePackages = 0;

    protected PackageHook() {
        super();
    }

    public class PackageMethodHook extends BaseMethodHook {
        public PackageMethodHook(String className, String methodName,
                                 Object[] signature, XC_MethodHook hook,
                                 HookCondition<XC_LoadPackage.LoadPackageParam> shouldLoad) {
            super(className, methodName, signature, hook, shouldLoad);
        }

        public PackageMethodHook(String className, String methodName,
                                 Object[] signature, XC_MethodHook hook) {
            super(className, methodName, signature, hook);
        }

        @Override
        public Boolean loads(XC_LoadPackage.LoadPackageParam param) {
            try {
                if (!shouldLoad(param)) return false;
                Class<?> clazz = ClassFinder.withCl(param.classLoader).find(className);
                if (clazz == null) {
                    warn("未找到类" + className);
                    return false;
                }
                Method method = MethodFinder.withCl(param.classLoader).find(className, methodName, signature);
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
                error(className + "." + methodName + "hook失败:", e);
                return false;
            }
        }
    }


    public class PackageFieldHook extends BaseFieldHook {

        public PackageFieldHook(String className, String fieldName, Object value,
                                HookCondition<XC_LoadPackage.LoadPackageParam> shouldLoad) {
            super(className, fieldName, value, shouldLoad);
        }
        public PackageFieldHook(String className, String fieldName, Object value) {
            super(className, fieldName, value, null);
        }

        @Override
        public Boolean loads(XC_LoadPackage.LoadPackageParam param) {
            try {
                Class<?> clazz = ClassFinder.withCl(param.classLoader).find(className);
                if (clazz == null) {
                    warn( "没有找到类 " + className);
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
                error("常量" + className + "." + fieldName + "hook失败:", e);
                return false;
            }

        }
    }


    public class PackageCallbackHook extends BaseCallbackHook {

        public PackageCallbackHook(HookCallback<XC_LoadPackage.LoadPackageParam> hookCallback,
                                   HookCondition<XC_LoadPackage.LoadPackageParam> shouldLoad) {
            super(hookCallback, shouldLoad);
        }
        public PackageCallbackHook(HookCallback<XC_LoadPackage.LoadPackageParam> hookCallback) {
            super(hookCallback, null);
        }

        @Override
        public Boolean loads(XC_LoadPackage.LoadPackageParam param) {
            try {
                boolean stat = callback.execute(param);
                info("自定义任务处理" + param.packageName + "完成，"
                        + (stat ? "执行成功" : "执行失败"));
                return stat;
            } catch (Throwable e) {
                error("执行自定义任务时出错", e);
                return false;
            }
        }
    }
    @Override
    protected PackageMethodHook createMethodHook(
            String className, String methodName, Object[] signature,
            XC_MethodHook hook, HookCondition<XC_LoadPackage.LoadPackageParam> shouldLoad) {
        return new PackageMethodHook(className, methodName, signature, hook, shouldLoad);
    }

    @Override
    protected PackageFieldHook createFieldHook(
            String className, String fieldName, Object value,
            HookCondition<XC_LoadPackage.LoadPackageParam> shouldLoad) {
        return new PackageFieldHook(className, fieldName, value, shouldLoad);
    }

    @Override
    protected PackageCallbackHook createCallbackHook(
            HookCallback<XC_LoadPackage.LoadPackageParam> hookCallback,
            HookCondition<XC_LoadPackage.LoadPackageParam> shouldLoad
    ) {
        return new PackageCallbackHook(hookCallback, shouldLoad);
    }

    @Override
    protected final boolean installHooks(XC_LoadPackage.LoadPackageParam param) {
        info("正在尝试为" + param.packageName + "加载" + getTag());
        if (!shouldLoad(param)) return false;

        boolean hasFailures = false;
        
        java.util.Set<String> loadedClasses = new java.util.HashSet<>();
        int preloadSuccessCount = 0;
        int preloadFailCount = 0;
        
        for (BaseHook hook : getHooks()) {
            if (hook.shouldLoad(param)) {
                if (hook instanceof PackageMethodHook) {
                    PackageMethodHook methodHook = (PackageMethodHook) hook;
                    if (!loadedClasses.contains(methodHook.className)) {
                        try {
                            Class<?> clazz = ClassFinder.withCl(param.classLoader).find(methodHook.className);
                            if (clazz == null) {
                                preloadFailCount++;
                                loadedClasses.add(methodHook.className);
                                continue;
                            }
                            loadedClasses.add(methodHook.className);
                            preloadSuccessCount++;
                        } catch (Throwable e) {
                            preloadFailCount++;
                            loadedClasses.add(methodHook.className);
                            continue;
                        }
                    }
                }
            }
        }
        
        if (preloadSuccessCount > 0 || preloadFailCount > 0) {
            info("类预加载完成: 成功" + preloadSuccessCount + "个，失败" + preloadFailCount + "个");
        }
        
        for (BaseHook hook : getHooks()) {
            if (hook.shouldLoad(param)) {
                if (hook.loads(param)) {
                    hookSucceedMethods++;
                } else {
                    hookFailureMethods++;
                    hasFailures = true;
                }
            }
        }

        if (hasFailures) hookFailurePackages++;
        else hookSucceedPackages++;

        info(String.format("执行完成，结果: 成功: %d, 失败: %d, 总计: %d",
                hookSucceedMethods, hookFailureMethods, getHookCount()));

        return !hasFailures;
    }

    @Override
    protected abstract void hookImplements();
}