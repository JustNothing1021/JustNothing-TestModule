package com.justnothing.testmodule.hooks;


import androidx.annotation.Nullable;

import com.justnothing.testmodule.utils.hooks.ClientHookConfig;
import com.justnothing.testmodule.utils.functions.Logger;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedHelpers;

public abstract class XposedBasicHook<ParamType> extends Logger {

    private Boolean initialized = false;
    private final List<BaseHook> hooks;
    private HookCondition<ParamType> hookCondition = null;
    protected static DefaultHookLogger defaultLogger = new DefaultHookLogger();


    private String hookName;
    private boolean enableCheckEnabled;
    protected String hookDisplayName;
    protected String hookDescription;
    protected boolean defaultEnable = true;

    public static class DefaultHookLogger extends Logger {
        public static final String TAG = "XposedBasicHook";
        @Override
        public String getTag() {
            return TAG;
        }
    }

    protected XposedBasicHook() {
        this.hooks = new ArrayList<>();
        this.enableCheckEnabled = true;
        hookName = getClass().getSimpleName();  // 放心，是继承的类的类名
        hookDisplayName = getClass().getSimpleName();
        if (hookDescription == null) {
            hookDescription = "该Hook没有详细描述...";
        }
        // useXposedLog(true);
    }

    protected void setHookName(String name) {
        this.hookName = name;
    }

    protected void setHookDescription(String desc) {
        this.hookDescription = desc;
    }



    protected void setEnableCheckEnabled(boolean enabled) {
        this.enableCheckEnabled = enabled;
    }

    protected boolean isHookEnabled() {
        if (!enableCheckEnabled) return true;
        return ClientHookConfig.isHookEnabled(hookName);
    }

    public interface HookCondition<T> {
        boolean shouldHook(T param);
    }

    public interface HookCallback<T> {
        boolean execute(T param);
    }

    /**
     * 用于寻找类的类。
     */
    public static class ClassFinder {


        private static final
            ConcurrentHashMap<ClassLoader,
                    ConcurrentHashMap<String, Class<?>>>
                classes = new ConcurrentHashMap<>();

        private static final ConcurrentHashMap<String, Class<?>> systemClasses = new ConcurrentHashMap<>();
        /**
         * 获取指定的类。(这个是静态方法，使用系统的ClassLoader)
         * @param className 类的名称
         * @see #withClassLoader(ClassLoader)
         * @return 寻找到的类
         */
        public static Class<?> find(String className) {
            return new ClassFinderImpl(null).find(className);
        }

        /**
         * 指定类加载器。
         * 但是我觉得你应该不会像打这么一长串的，所以:
         * @see #withCl(ClassLoader)
         * @param loader 我猜你应该知道
         * @return 寻找功能实现类
         */
        public static ClassFinderImpl withClassLoader(ClassLoader loader) {
            return new ClassFinderImpl(loader);
        }

        /**
         * 简化版，不用敲那么多字母了，可以直接肌肉记忆打出来
         * @param loader 依旧
         * @return 寻找功能实现类
         */
        public static ClassFinderImpl withCl(ClassLoader loader) {
            return withClassLoader(loader);
        }

        /**
         * 处理真正寻找类的逻辑的类。
         */
        public static class ClassFinderImpl {
            ClassLoader classLoader; // Implementor的classLoader
            private ClassFinderImpl(ClassLoader cl) {
                classLoader = cl;
            }

            /**
             * 寻找指定的类。
             * 带有缓存机制，虽然不知道到底起没起作用。
             * @param className 类名
             * @return 类
             */
            @Nullable
            public Class<?> find(String className) {
                ConcurrentHashMap<String, Class<?>> mapping = classLoader == null ?
                systemClasses : classes.computeIfAbsent(classLoader, key -> new ConcurrentHashMap<>());
                Class<?> clazz = mapping.get(className);
                if (clazz == null) {
                    clazz = XposedHelpers.findClassIfExists(className, classLoader);
                    if (clazz != null) mapping.put(className, clazz);
                }
                return clazz;
            }
        }
    }

    public static class MethodFinder {
        public static class MethodSignature {
            Object[] sign;
            String name;
            MethodSignature(String methodName, Object[] signature) {
                name = methodName;
                sign = signature;
            }

            @Override
            public boolean equals(Object o) {
                if (o == null || getClass() != o.getClass()) return false;
                MethodSignature that = (MethodSignature) o;
                return Objects.deepEquals(sign, that.sign) && Objects.equals(name, that.name);
            }

            @Override
            public int hashCode() {
                return Objects.hash(Arrays.hashCode(sign), name);
            }
        }
        private static final
        ConcurrentHashMap<ClassLoader,
                ConcurrentHashMap<String,
                        ConcurrentHashMap<MethodSignature, Method>>>
                methods = new ConcurrentHashMap<>();

        private static final ConcurrentHashMap<String,
                ConcurrentHashMap<MethodSignature, Method>>
                    systemMethods = new ConcurrentHashMap<>();

        public static Method find(String className, String methodName, Object... signature) {
            return new MethodFinderImpl(null).find(className, new MethodSignature(methodName, signature));
        }

        /**
         * 指定类加载器。
         * 但是我觉得你应该不会像打这么一长串的，所以:
         * @see #withCl(ClassLoader)
         * @param loader 我猜你应该知道
         * @return 寻找功能实现类
         */
        public static MethodFinderImpl withClassLoader(ClassLoader loader) {
            return new MethodFinderImpl(loader);
        }

        /**
         * 简化版，不用敲那么多字母了，可以直接肌肉记忆打出来
         * @param loader 依旧
         * @return 寻找功能实现类
         */
        public static MethodFinderImpl withCl(ClassLoader loader) {
            return withClassLoader(loader);
        }

        public static class MethodFinderImpl {
            ClassLoader classLoader;
            private MethodFinderImpl(ClassLoader cl) {
                classLoader = cl;
            }

            /**
             * 寻找指定的类。
             * 带有缓存机制，虽然不知道到底起没起作用。
             * @param className 类名
             * @param methodName 方法名
             * @param methodSignature 参数列表，作为可变参数形式
             * @return 类
             */
            @Nullable
            public Method find(String className, String methodName, Object... methodSignature) {
                return find(className, new MethodSignature(methodName, methodSignature));
            }

            /**
             * 寻找指定的类。（不过一般不用这个）
             * 带有缓存机制，虽然不知道到底起没起作用。
             * @param className 类名
             * @param signature 签名
             * @see #find(String, String, Object...)
             * @return 类
             */
            @Nullable
            public Method find(String className, MethodSignature signature) {
                if (classLoader == null) {
                    ConcurrentHashMap<MethodSignature, Method> m = systemMethods.get(className);
                    if (m != null) {
                        Method n = m.get(signature);
                        if (n != null) return n;
                    }
                } else {
                    ConcurrentHashMap<String, ConcurrentHashMap<MethodSignature, Method>>
                            m = methods.get(classLoader);
                    if (m != null) {
                        ConcurrentHashMap<MethodSignature, Method> n = m.get(className);
                        if (n != null) {
                            Method p = n.get(signature);
                            if (p != null) return p;
                        }
                    }
                }
                Class<?> clazz = ClassFinder.withCl(classLoader).find(className);
                if (clazz == null) return null;
                Method method = XposedHelpers.findMethodExactIfExists(clazz, signature.name, signature.sign);
                if (method != null)
                    (classLoader == null ?
                        systemMethods: methods.computeIfAbsent(classLoader, key -> new ConcurrentHashMap<>()))
                            .computeIfAbsent(className, key -> new ConcurrentHashMap<>())
                            .put(signature, method);
                return method;
            }

            /**
             * 找到一个类的所有符合名字的方法，不论参数列表是什么。
             * @param className 类名
             * @param methodName 方法名
             * @return 找到的所有方法
             */
            @Nullable
            public List<Method> findAll(String className, String methodName) {
                Class<?> clazz = ClassFinder.withCl(classLoader).find(className);
                if (clazz == null) return null;
                List<Method> methodArrayList = Arrays.asList(clazz.getDeclaredMethods());
                methodArrayList.removeIf(m -> !m.getName().equals(methodName));
                return methodArrayList;
            }
        }
    }

    public abstract class BaseHook {
        protected final HookCondition<ParamType> shouldHook;
        public BaseHook (HookCondition<ParamType> cond) {
            this.shouldHook = cond;
        }

        public abstract Boolean loads(ParamType param);
        public Boolean shouldLoad(ParamType param) {
            return shouldHook == null || shouldHook.shouldHook(param);
        }
    }

    public abstract class BaseMethodHook extends BaseHook {
        protected final String className;
        protected final String methodName;
        protected final Object[] signature;
        protected final XC_MethodHook hook;

        public BaseMethodHook(String className, String methodName, Object[] signature,
                              XC_MethodHook hook, HookCondition<ParamType> shouldHook) {
            super(shouldHook);
            this.className = className;
            this.methodName = methodName;
            this.signature = signature;
            this.hook = hook;
        }

        public BaseMethodHook(String className, String methodName, Object[] signature,
                              XC_MethodHook hook) {
            this(className, methodName, signature, hook, null);
        }

        public String getHookName() {
            return className + "." + methodName;
        }
    }

    public abstract class BaseFieldHook extends BaseHook {
        protected final String className;
        protected final String fieldName;
        protected final Object value;

        public BaseFieldHook(String className, String fieldName,
                             Object value, HookCondition<ParamType> shouldHook) {
            super(shouldHook);
            this.className = className;
            this.fieldName = fieldName;
            this.value = value;
        }

        public BaseFieldHook(String className, String fieldName, Object value) {
            this(className, fieldName, value, null);
        }

    }

    public abstract class BaseCallbackHook extends BaseHook {
        protected HookCallback<ParamType> callback;

        public BaseCallbackHook(HookCallback<ParamType> hookCallback,
                                HookCondition<ParamType> shouldLoad) {
            super(shouldLoad);
            callback = hookCallback;
        }

        public BaseCallbackHook(HookCallback<ParamType> hookCallback) {
            this(hookCallback, null);
        }

    }

    protected BaseMethodHook createMethodHook(
            String className, String methodName, Object[] signature, XC_MethodHook hook) {
        return createMethodHook(className, methodName, signature, hook, null);
    }

    protected BaseFieldHook createFieldHook(String className, String fieldName, Object value) {
        return createFieldHook(className, fieldName,  value, null);
    }


    protected BaseCallbackHook createCallbackHook(HookCallback<ParamType> hookCallback) {
        return createCallbackHook(hookCallback, null);
    }

    protected abstract BaseMethodHook createMethodHook(
            String className, String methodName, Object[] signature, XC_MethodHook hook,
            HookCondition<ParamType> shouldLoad
    );

    protected abstract BaseFieldHook createFieldHook(
            String className, String fieldName, Object value,
            HookCondition<ParamType> shouldLoad
    );


    protected abstract BaseCallbackHook createCallbackHook(
        HookCallback<ParamType> hookCallback,
        HookCondition<ParamType> shouldLoad
    );


    protected void hookMethod(String className, String methodName,
                              Object... sigAndHook) {
        if (sigAndHook.length < 1) {
            error("hookMethod参数不足");
            return;
        }

        Object[] sig = new Object[sigAndHook.length - 1];
        System.arraycopy(sigAndHook, 0, sig, 0, sig.length);
        XC_MethodHook hook = (XC_MethodHook) sigAndHook[sigAndHook.length - 1];
        addHook(createMethodHook(className, methodName, sig, hook));
    }

    protected void hookField(String className, String fieldName, Object value) {
        addHook(createFieldHook(className, fieldName, value));
    }

    protected void hookCallback(HookCallback<ParamType> callback) {
        addHook(createCallbackHook(callback));
    }

    protected BaseMethodHook createAlwaysTrueHook(
            String className, String methodName, Object... sig) {
        XC_MethodHook hook = new XC_MethodReplacement() {
            @Override
            protected Object replaceHookedMethod(MethodHookParam methodHookParam) {
                if (!SILENT_IN_CONST_HOOK)
                    info(className + "." + methodName + "被调用，强制返回true");
                return true;
            }
        };
        return createMethodHook(className, methodName, sig, hook);
    }

    protected BaseMethodHook createAlwaysFalseHook(
            String className, String methodName, Object... sig) {
        XC_MethodHook hook = new XC_MethodReplacement() {
            @Override
            protected Object replaceHookedMethod(MethodHookParam methodHookParam) throws Throwable {
                if (!SILENT_IN_CONST_HOOK)
                    info(className + "." + methodName + "被调用，强制返回false");
                return false;
            }
        };
        return createMethodHook(className, methodName, sig, hook);
    }

    protected BaseMethodHook createDoNothingHook(
            String className, String methodName, Object... sig) {
        XC_MethodHook hook = new XC_MethodReplacement() {
            @Override
            protected Object replaceHookedMethod(MethodHookParam methodHookParam) throws Throwable {
                if (!SILENT_IN_CONST_HOOK) {
                    info(className + "." + methodName + "被调用，忽略该操作");
                    info("参数列表：" + Arrays.toString(methodHookParam.args));
                }
                return null;
            }
        };
        return createMethodHook(className, methodName, sig, hook);
    }

    protected void hookAlwaysTrue(String className, String methodName, Object... sig) {
        addHook(createAlwaysTrueHook(className, methodName, sig));
    }

    protected void hookAlwaysFalse(String className, String methodName, Object... sig) {
        addHook(createAlwaysFalseHook(className, methodName, sig));
    }

    protected void hookDoNothing(String className, String methodName, Object... sig) {
        addHook(createDoNothingHook(className, methodName, sig));
    }

    protected void hookStaticField(String className, String memberName, Object value) {
        addHook(createFieldHook(className, memberName, value));
    }

    public boolean shouldLoad(ParamType param) {
        return hookCondition == null || hookCondition.shouldHook(param);
    }

    public final void setupHooks() {
        if (initialized) return;
        hookImplements();
        initialized = true;
    }

    public final Integer getHookCount() {
        return hooks.size();
    }

    public String getHookName() {
        return hookName;
    }

    public String getHookDisplayName() {
        return hookDisplayName;
    }

    public String getHookDescription() {
        return hookDescription;
    }

    public Boolean isInitialized() {
        return initialized;
    }

    protected void setHookCondition(HookCondition<ParamType> cond) {
        this.hookCondition = cond;
    }

    protected void setDefaultEnable(boolean state) {
        defaultEnable = state;
    }

    protected boolean getDefaultEnable() {
        return defaultEnable;
    }

    protected void setHookDisplayName(String name) {
        this.hookDisplayName = name;
    }

    protected void addHook(BaseHook hook) {
        hooks.add(hook);
    }

    protected List<BaseHook> getHooks() {
        return hooks;
    }

    protected abstract boolean installHooks(ParamType param);

    protected final boolean installHooksWithCheck(ParamType param) {
        if (!isHookEnabled()) {
            info("Hook已禁用，跳过安装: " + (hookName != null ? hookName : getClass().getSimpleName()));
            return false;
        }
        return installHooks(param);
    }

    protected abstract void hookImplements();
}