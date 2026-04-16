package com.justnothing.testmodule.utils.sandbox;

import android.os.StrictMode;

import com.justnothing.javainterpreter.security.IPermissionChecker;
import com.justnothing.javainterpreter.security.SandboxConfig;
import com.justnothing.testmodule.utils.logging.Logger;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicInteger;

@SuppressWarnings("unused")
public final class BlockGuardSandbox {

    private static final Logger logger = Logger.getLoggerForName("BlockGuardSandbox");

    private BlockGuardSandbox() {
    }

    private static final Class<?> BLOCKGUARD_CLASS;
    private static final Class<?> POLICY_INTERFACE;
    private static final Method SET_THREAD_POLICY_METHOD;
    private static final Method GET_THREAD_POLICY_METHOD;
    private static final Method ON_READ_FROM_DISK_METHOD;
    private static final Method ON_WRITE_TO_DISK_METHOD;
    private static final Method ON_NETWORK_METHOD;
    private static final Method GET_POLICY_MASK_METHOD;

    private static final boolean BLOCKGUARD_AVAILABLE;

    static {
        Class<?> bgClass = null;
        Class<?> policyIf = null;
        Method setPolicy = null;
        Method getPolicy = null;
        Method onRead = null;
        Method onWrite = null;
        Method onNetwork = null;
        Method getMask = null;
        boolean available = false;

        try {
            bgClass = Class.forName("dalvik.system.BlockGuard");
            policyIf = Class.forName("dalvik.system.BlockGuard$Policy");
            setPolicy = bgClass.getMethod("setThreadPolicy", policyIf);
            getPolicy = bgClass.getMethod("getThreadPolicy");
            onRead = policyIf.getMethod("onReadFromDisk");
            onWrite = policyIf.getMethod("onWriteToDisk");
            onNetwork = policyIf.getMethod("onNetwork");
            getMask = policyIf.getMethod("getPolicyMask");
            available = true;
            logger.info("BlockGuard initialized successfully");
        } catch (Throwable t) {
            logger.error("BlockGuard initialization failed", t);
        }

        BLOCKGUARD_CLASS = bgClass;
        POLICY_INTERFACE = policyIf;
        SET_THREAD_POLICY_METHOD = setPolicy;
        GET_THREAD_POLICY_METHOD = getPolicy;
        ON_READ_FROM_DISK_METHOD = onRead;
        ON_WRITE_TO_DISK_METHOD = onWrite;
        ON_NETWORK_METHOD = onNetwork;
        GET_POLICY_MASK_METHOD = getMask;
        BLOCKGUARD_AVAILABLE = available;
    }

    private static final ThreadLocal<SandboxConfig> currentConfig = new ThreadLocal<>();
    private static final ThreadLocal<Object> savedPolicy = new ThreadLocal<>();
    private static final ThreadLocal<StrictMode.ThreadPolicy> savedStrictModePolicy = new ThreadLocal<>();
    private static final ThreadLocal<AtomicInteger> bypassDepth = new ThreadLocal<>();
    private static final ThreadLocal<Boolean> sandboxActive = new ThreadLocal<>();

    public static boolean isBlockGuardAvailable() {
        return BLOCKGUARD_AVAILABLE;
    }

    public static SandboxContext enter(SandboxConfig config) {
        currentConfig.set(config);
        bypassDepth.set(new AtomicInteger(0));
        sandboxActive.set(true);
        logger.info("进入了沙箱环境, BLOCKGUARD_AVAILABLE=" + BLOCKGUARD_AVAILABLE);

        boolean needSeccomp = !config.isProcessCreateAllowed() || !config.isThreadCreateAllowed();
        if (needSeccomp && SeccompNotifHandler.isSupported()) {
            if (!SeccompNotifHandler.isInitialized()) {
                SeccompNotifHandler.init();
            }
            if (SeccompNotifHandler.isInitialized()) {
                boolean allowed = config.isProcessCreateAllowed() && config.isThreadCreateAllowed();
                SeccompNotifHandler.registerCurrentThread(allowed);
                SeccompNotifHandler.installFilterForCurrentThread();
                logger.info("已安装 seccomp USER_NOTIF 过滤器");
            }
        }

        if (BLOCKGUARD_AVAILABLE) {
            try {
                Object currentPolicy = getThreadPolicy();
                savedPolicy.set(currentPolicy);
                logger.info("保存了当前策略: " + currentPolicy);
            } catch (Throwable e) {
                logger.error("获取当前策略时出现错误", e);
            }

            StrictMode.ThreadPolicy currentStrict = StrictMode.getThreadPolicy();
            savedStrictModePolicy.set(currentStrict);

            Object sandboxPolicy = createSandboxPolicyProxy(config);
            try {
                setThreadPolicy(sandboxPolicy);
                logger.info("成功设置了沙箱策略");

                Object verifyPolicy = getThreadPolicy();
                logger.debug("验证策略: " + verifyPolicy + " (代理: " + (verifyPolicy == sandboxPolicy) + ")");
                } catch (Throwable t) {
                    logger.error("设置沙箱策略时出现错误", t);
                    sandboxActive.set(false);
                    currentConfig.remove();
                    bypassDepth.remove();
                    throw new RuntimeException("设置BlockGuard策略时出现错误", t);
                }

            // try {
            // StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
            // .detectAll()
            // .penaltyLog()
            // .build());
            // } catch (Throwable ignored) {}

            // 不要调用StrictMode.setThreadPolicy()，它会覆盖我们设置的BlockGuard代理策略
            // StrictMode.setThreadPolicy()
            // 内部会调用BlockGuard.setThreadPolicy(AndroidBlockGuardPolicy)
            // 这会用AndroidBlockGuardPolicy覆盖我们的代理策略
            // 所以说有完整的系统源码还是太舒适了
        }

        return new SandboxContext(config);
    }

    public static void exit() {
        sandboxActive.set(false);

        if (SeccompNotifHandler.isInitialized()) {
            SeccompNotifHandler.unregisterCurrentThread();
            logger.info("已注销 seccomp 线程");
        }

        if (BLOCKGUARD_AVAILABLE) {
            Object original = savedPolicy.get();
            if (original != null) {
                try {
                    setThreadPolicy(original);
                    logger.debug("恢复原始策略");
                } catch (Throwable e) {
                    logger.error("恢复原始策略时出现错误", e);
                }
            } else {
                try {
                    Object emptyPolicy = createEmptyPolicyProxy();
                    setThreadPolicy(emptyPolicy);
                    logger.debug("设置了一个空的策略");
                } catch (Throwable e) {
                    logger.error("设置空策略时出现错误", e);
                }
            }

            StrictMode.ThreadPolicy originalStrict = savedStrictModePolicy.get();
            if (originalStrict != null) {
                try {
                    StrictMode.setThreadPolicy(originalStrict);
                } catch (Throwable ignored) {
                }
            }
        }

        currentConfig.remove();
        savedPolicy.remove();
        savedStrictModePolicy.remove();
        bypassDepth.remove();
        sandboxActive.remove();
    }

    public static boolean isActive() {
        Boolean active = sandboxActive.get();
        return active != null && active;
    }

    public static SandboxConfig getCurrentConfig() {
        return currentConfig.get();
    }

    public static <T> T execute(SandboxConfig config, Callable<T> action) throws Exception {
        SandboxContext ctx = enter(config);
        try {
            return action.call();
        } finally {
            ctx.close();
        }
    }

    public static void execute(SandboxConfig config, Runnable action) {
        SandboxContext ctx = enter(config);
        try {
            action.run();
        } finally {
            ctx.close();
        }
    }

    public static <T> T executeSandboxed(Callable<T> action) throws Exception {
        return execute(SandboxConfig.DEFAULT, action);
    }

    public static void executeSandboxed(Runnable action) {
        execute(SandboxConfig.DEFAULT, action);
    }

    public static <T> T executeExpressionOnly(Callable<T> action) throws Exception {
        return execute(SandboxConfig.EXPRESSION_ONLY, action);
    }

    public static void bypass(Runnable action) {
        AtomicInteger depth = bypassDepth.get();
        if (depth != null) {
            depth.incrementAndGet();
        }
        try {
            action.run();
        } finally {
            if (depth != null) {
                depth.decrementAndGet();
            }
        }
    }

    public static <T> T bypass(Callable<T> action) throws Exception {
        AtomicInteger depth = bypassDepth.get();
        if (depth != null) {
            depth.incrementAndGet();
        }
        try {
            return action.call();
        } finally {
            if (depth != null) {
                depth.decrementAndGet();
            }
        }
    }

    public static boolean isBypassing() {
        AtomicInteger depth = bypassDepth.get();
        return depth != null && depth.get() > 0;
    }

    private static Object getThreadPolicy() throws Exception {
        return GET_THREAD_POLICY_METHOD.invoke(null);
    }

    private static void setThreadPolicy(Object policy) throws Exception {
        SET_THREAD_POLICY_METHOD.invoke(null, policy);
    }

    private static Object createSandboxPolicyProxy(SandboxConfig config) {
        return Proxy.newProxyInstance(
                POLICY_INTERFACE.getClassLoader(),
                new Class<?>[] { POLICY_INTERFACE },
                new SandboxPolicyInvocationHandler(config));
    }

    private static Object createEmptyPolicyProxy() {
        return Proxy.newProxyInstance(
                POLICY_INTERFACE.getClassLoader(),
                new Class<?>[] { POLICY_INTERFACE },
                new EmptyPolicyInvocationHandler());
    }

    private record SandboxPolicyInvocationHandler(
            SandboxConfig config) implements InvocationHandler {

        private static final String[] LOCAL_SOCKET_CLASSES = {
                "java.net.SocketOutputStream",
                "java.net.SocketInputStream",
                "libcore.io.IoBridge",
                "java.net.PlainSocketImpl",
                "android.net.LocalSocket",
                "android.net.LocalSocketAddress",
                "android.net.LocalSocketImpl",
                com.justnothing.testmodule.command.output.InteractiveProtocol.class.getName(),
                com.justnothing.testmodule.service.handler.SocketClientHandler.class.getName()
        };

        private static boolean isLocalSocketOperation() {
            StackTraceElement[] stack = Thread.currentThread().getStackTrace();
            for (StackTraceElement element : stack) {
                String className = element.getClassName();
                for (String socketClass : LOCAL_SOCKET_CLASSES) {
                    if (className.equals(socketClass)) {
                        return true;
                    }
                }
            }
            return false;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) {
            String methodName = method.getName();
            logger.info("invoke() called: " + methodName + ", bypassing=" + isBypassing() +
                      ", diskRead=" + config.isDiskReadAllowed() + 
                      ", diskWrite=" + config.isDiskWriteAllowed());

            switch (methodName) {
                case "onReadFromDisk" -> {
                    if (!isBypassing() && !config.isDiskReadAllowed()) {
                        logger.warn("不允许读取存储空间!");
                        throw new SecurityException("BlockGuardSandbox: 磁盘读取操作被禁止");
                    }
                    return null;
                }
                case "onWriteToDisk" -> {
                    if (!isBypassing() && !config.isDiskWriteAllowed()) {
                        logger.warn("不允许写入存储空间!");
                        throw new SecurityException("BlockGuardSandbox: 磁盘写入操作被禁止");
                    }
                    return null;
                }
                case "onNetwork" -> {
                    if (!isBypassing() && !config.isNetworkAllowed()) {
                        if (config.isLocalSocketAllowed() && isLocalSocketOperation()) {
                            logger.debug("Allowing local socket operation");
                            return null;
                        }
                        logger.warn("不允许访问网络!");
                        throw new SecurityException("BlockGuardSandbox: 网络操作被禁止");
                    }
                    return null;
                }
                case "getPolicyMask" -> {
                    int mask = 0;
                    if (!config.isDiskReadAllowed())
                        mask |= 0x02;
                    if (!config.isDiskWriteAllowed())
                        mask |= 0x01;
                    if (!config.isNetworkAllowed())
                        mask |= 0x04;
                    return mask;
                }
            }
            return null;
        }
    }

    private static final class EmptyPolicyInvocationHandler implements InvocationHandler {
        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            if ("getPolicyMask".equals(method.getName())) {
                return 0;
            }
            return null;
        }
    }

    public static final class SandboxContext implements AutoCloseable {
        private final SandboxConfig config;
        private volatile boolean closed = false;

        SandboxContext(SandboxConfig config) {
            this.config = config;
        }

        public SandboxConfig getConfig() {
            return config;
        }

        public boolean isActive() {
            return !closed && BlockGuardSandbox.isActive();
        }

        @Override
        public void close() {
            if (!closed) {
                closed = true;
                exit();
            }
        }
    }
}
