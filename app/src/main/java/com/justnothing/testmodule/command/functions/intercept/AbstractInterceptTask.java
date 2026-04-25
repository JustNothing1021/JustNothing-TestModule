package com.justnothing.testmodule.command.functions.intercept;

import androidx.annotation.NonNull;

import com.justnothing.testmodule.hooks.HookAPI;
import com.justnothing.testmodule.utils.logging.Logger;
import com.justnothing.testmodule.utils.reflect.ClassResolver;
import com.justnothing.testmodule.utils.reflect.ReflectionUtils;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import de.robv.android.xposed.XC_MethodHook;

public abstract class AbstractInterceptTask implements InterceptTask {

    protected final Logger logger = Logger.getLoggerForName(getClass().getSimpleName());

    protected int id;
    protected final String className;
    protected final String methodName;
    protected final String signature;
    protected final ClassLoader classLoader;
    protected final TaskType taskType;

    protected volatile boolean enabled = true;
    protected final AtomicBoolean running = new AtomicBoolean(false);
    protected final AtomicInteger hitCount = new AtomicInteger(0);
    protected final List<XC_MethodHook.Unhook> activeHooks = new ArrayList<>();

    protected Class<?> targetClass;
    protected List<Method> targetMethods = new ArrayList<>();

    protected AbstractInterceptTask(int id, String className, String methodName,
                                    String signature, ClassLoader classLoader, TaskType taskType) {
        this.id = id;
        this.className = className;
        this.methodName = methodName;
        this.signature = signature;
        this.classLoader = classLoader;
        this.taskType = taskType;
    }

    protected void resolveTargetClass() throws ClassNotFoundException {
        targetClass = ClassResolver.findClassOrFail(className, classLoader);
        logger.debug("成功加载类: " + targetClass.getName());
    }

    protected void resolveTargetMethods() {
        try {
            Method[] methods = ReflectionUtils.findAllMethods(targetClass, methodName, signature, classLoader);
            targetMethods.addAll(Arrays.asList(methods));
            for (Method method : targetMethods) {
                method.setAccessible(true);
            }
            logger.debug("找到方法: " + targetMethods);
        } catch (Exception e) {
            logger.error("查找方法失败: " + methodName, e);
            throw new RuntimeException("查找方法失败: " + e.getMessage(), e);
        }
    }

    @Override
    public void start() {
        if (running.get()) {
            logger.warn("任务已经在运行: " + id);
            return;
        }

        running.set(true);
        logger.info("启动任务: " + id + " (" + getDisplayName() + ")");

        try {
            resolveTargetClass();
            resolveTargetMethods();
            installHooks();
            logger.info("任务启动成功: " + id);
        } catch (Exception e) {
            running.set(false);
            logger.error("启动任务失败: " + id, e);
            throw new RuntimeException("启动任务失败: " + e.getMessage(), e);
        }
    }

    protected void installHooks() {
        onInstall();
        for (Method method : targetMethods) {
            XC_MethodHook hook = createMethodHook();
            XC_MethodHook.Unhook unhook = HookAPI.findAndHookMethod(
                    targetClass,
                    methodName,
                    method.getParameterTypes(),
                    hook
            );
            activeHooks.add(unhook);
            logger.debug("Hook安装成功: " + method);
        }
        logger.info("共安装 " + activeHooks.size() + " 个Hook");
        onActivated();
    }

    protected abstract XC_MethodHook createMethodHook();

    protected HookContext createHookContext(XC_MethodHook.MethodHookParam param) {
        return new HookContext(this, param);
    }

    protected void executeWithHookContext(XC_MethodHook.MethodHookParam param, HookCallback callback) {
        if (!enabled || !running.get()) {
            return;
        }
        hitCount.incrementAndGet();
        HookContext ctx = createHookContext(param);
        callback.execute(ctx);
    }

    protected interface HookCallback {
        void execute(HookContext ctx);
    }

    @Override
    public void stop() {
        if (!running.get()) {
            logger.warn("任务未在运行: " + id);
            return;
        }

        onDeactivated();
        running.set(false);
        logger.info("停止任务: " + id);

        for (XC_MethodHook.Unhook unhook : activeHooks) {
            try {
                unhook.unhook();
                logger.debug("Hook已移除: " + unhook.getHookedMethod());
            } catch (Exception e) {
                logger.error("移除Hook失败", e);
            }
        }
        activeHooks.clear();
        onUninstall();
        logger.info("任务已停止: " + id);
    }

    @Override
    public void onHook(XC_MethodHook.MethodHookParam param) {
        if (!enabled || !running.get()) {
            return;
        }
        hitCount.incrementAndGet();
    }

    @Override
    public int getId() {
        return id;
    }

    @Override
    public void setId(int id) {
        if (this.id == 0) {
            this.id = id;
        }
    }

    @Override
    public String getClassName() {
        return className;
    }

    @Override
    public String getMethodName() {
        return methodName;
    }

    @Override
    public String getSignature() {
        return signature;
    }

    @Override
    public ClassLoader getClassLoader() {
        return classLoader;
    }

    @Override
    public TaskType getType() {
        return taskType;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public void setEnabled(boolean enabled) {
        boolean wasEnabled = this.enabled;
        this.enabled = enabled;
        logger.info("任务 " + id + " " + (enabled ? "已启用" : "已禁用"));
        
        if (running.get()) {
            if (!wasEnabled && enabled) {
                onActivated();
            } else if (wasEnabled && !enabled) {
                onDeactivated();
            }
        }
    }

    @Override
    public int getHitCount() {
        return hitCount.get();
    }

    @Override
    public boolean isRunning() {
        return running.get();
    }

    @Override
    public List<XC_MethodHook.Unhook> getActiveHooks() {
        return new ArrayList<>(activeHooks);
    }

    @NonNull
    @Override
    public String toString() {
        return String.format(
                Locale.getDefault(),
                "%s[%d] %s (命中: %d, 状态: %s)",
                taskType.getCommandName().toUpperCase(),
                id,
                getDisplayName(),
                hitCount.get(),
                running.get() ? (enabled ? "运行中" : "已暂停") : "已停止");
    }
}
