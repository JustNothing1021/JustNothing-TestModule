package com.justnothing.testmodule.utils.sandbox;

import android.os.StrictMode;

import com.justnothing.engine.security.SandboxConfig;
import com.justnothing.testmodule.command.framework.protocol.InteractiveProtocol;
import com.justnothing.testmodule.utils.logging.Logger;

import org.jetbrains.annotations.NotNull;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;

public final class BlockGuardSandbox {

    private static final Logger logger = Logger.getLoggerForName("BlockGuardSandbox");

    private BlockGuardSandbox() {
    }

    private static final class WhitelistPattern {
        final String pattern;
        final Pattern regex;

        WhitelistPattern(String pattern) {
            this.pattern = pattern;
            this.regex = compilePattern(pattern);
        }

        private static Pattern compilePattern(String pattern) {
            StringBuilder sb = new StringBuilder();
            sb.append("^");
            for (int i = 0; i < pattern.length(); i++) {
                char c = pattern.charAt(i);
                switch (c) {
                    case '.' -> sb.append("\\.");
                    case '*' -> sb.append(".*");
                    case '$' -> sb.append("\\$");
                    default -> sb.append(c);
                }
            }
            sb.append("$");
            return Pattern.compile(sb.toString());
        }

        boolean matches(String className) {
            return regex.matcher(className).matches();
        }

        @Override
        public @NotNull String toString() {
            return pattern;
        }
    }

    private static final class WhitelistGroup {
        final String name;
        final List<WhitelistPattern> patterns;

        WhitelistGroup(String name, String... patternStrings) {
            this.name = name;
            this.patterns = new ArrayList<>();
            for (String p : patternStrings) {
                patterns.add(new WhitelistPattern(p));
            }
        }

        boolean matchesAny(String className) {
            for (WhitelistPattern p : patterns) {
                if (p.matches(className)) {
                    return true;
                }
            }
            return false;
        }
    }

    private static final WhitelistGroup DISK_READ_WHITELIST = new WhitelistGroup("DiskRead",
            "dalvik.system.*",
            "java.lang.ClassLoader",
            "java.lang.ClassLoader$*",
            "java.lang.Class",
            "java.lang.Class$*",
            "com.justnothing.testmodule.utils.logging.*"
    );

    private static final WhitelistGroup DISK_WRITE_WHITELIST = new WhitelistGroup("DiskWrite",
            "com.justnothing.testmodule.utils.logging.*"
    );

    private static final WhitelistGroup NETWORK_WHITELIST = new WhitelistGroup("Network",
            "com.justnothing.testmodule.command.output.*",
            "com.justnothing.testmodule.service.handler.*"
    );

    private static final WhitelistGroup LOCAL_SOCKET_WHITELIST = new WhitelistGroup("LocalSocket",
            "java.net.SocketOutputStream",
            "java.net.SocketInputStream",
            "libcore.io.IoBridge",
            "java.net.PlainSocketImpl",
            "android.net.LocalSocket",
            "android.net.LocalSocketAddress",
            "android.net.LocalSocketImpl",
            InteractiveProtocol.class.getName(),
            com.justnothing.testmodule.service.handler.SocketClientHandler.class.getName()
    );

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
        installSeccomp(config);
        return enterJavaPolicy(config);
    }

    /**
     * 只装 seccomp（值级规则，装在当前线程上，被子线程与子进程继承）。
     *
     * <p>和 {@link #enterJavaPolicy} 分开是因为两者的"作用范围"不同：seccomp 装上就随线程走，
     * 后代天然受限，只需在首次进入时装一次；BlockGuard 策略是纯 per-thread 且不继承，
     * 每一条线程都得单独装。{@link #wrap} 只补后者。
     */
    private static void installSeccomp(SandboxConfig config) {
        if (!seccompNeeded(config)) {
            return;
        }
        boolean blockProcess = !config.isProcessCreateAllowed();
        boolean blockThread = !config.isThreadCreateAllowed();
        boolean blockExec = !config.isExecAllowed();

        int flags = 0;
        if (blockProcess) flags |= SeccompSandbox.BLOCK_PROCESS_CREATE;
        if (blockThread) flags |= SeccompSandbox.BLOCK_THREAD_CREATE;
        if (blockExec) flags |= SeccompSandbox.BLOCK_EXEC;

        // 只下沉这三条值级规则。文件写/删与网络刻意留在 Java 层：
        // seccomp 那三条没有 bypass 通道，装上后连本类的 logger.info 都会 EPERM。
        int err = SeccompSandbox.install(flags);
        if (err == SeccompSandbox.OK) {
            logger.info("已装载 seccomp 过滤器（进程=" + blockProcess
                    + " 线程=" + blockThread + " 执行=" + blockExec + "）");
        } else {
            logger.warn("seccomp 装载失败（" + SeccompSandbox.describe(err)
                    + "），进程/线程/执行拦截不可用");
        }
    }

    /**
     * 进入沙箱的 Java 层部分：登记线程状态 + 给**当前线程**装上 BlockGuard 策略代理。
     */
    private static SandboxContext enterJavaPolicy(SandboxConfig config) {
        currentConfig.set(config);
        if (!isBypassing()) {
            bypassDepth.set(new AtomicInteger(0));
        }
        sandboxActive.set(true);
        logger.info("进入了沙箱环境, BLOCKGUARD_AVAILABLE=" + BLOCKGUARD_AVAILABLE);

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

    public static Object proxyObjectMethods(Object proxy, Method method, Object[] args) {
        String name = method.getName();
        return switch (name) {
            case "equals" -> proxy == args[0];
            case "hashCode" -> System.identityHashCode(proxy);
            case "toString" ->
                    "BlockGuardSandbox$PolicyProxy@" + Integer.toHexString(System.identityHashCode(proxy));
            default -> null;
        };
    }

    /** 该配置是否需要 seccomp（进程创建 / 线程创建 / 执行命令 三者任一被禁）。 */
    private static boolean seccompNeeded(SandboxConfig config) {
        return config != null
                && (!config.isProcessCreateAllowed()
                || !config.isThreadCreateAllowed()
                || !config.isExecAllowed());
    }

    public static <T> T execute(SandboxConfig config, Callable<T> action) throws Exception {
        // 已经在沙箱线程里（嵌套调用）时不要再开新线程：当前线程本身就带着过滤器，
        // 而且此时"禁止创建线程"可能已经生效，新建 Sandbox-Worker 会直接失败。
        if (seccompNeeded(config) && !isActive()) {
            return executeOnOwnThread(config, action);
        }
        try (SandboxContext ctx = enter(config)) {
            return action.call();
        }
    }

    public static void execute(SandboxConfig config, Runnable action) {
        try {
            execute(config, () -> {
                action.run();
                return null;
            });
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 在**一次性线程**上执行沙箱动作。
     *
     * <p>seccomp 过滤器是按线程生效的，而且装上就<b>不能卸载</b> —— 只跟着线程消亡。
     * 如果借用线程池的 worker，那个 worker 之后永远不能再建进程/线程，池子会被逐次污染。
     * 因此需要 seccomp 时开一条专属线程：过滤器随它一起结束。
     *
     * <p>副作用（正面的）：BlockGuard 的策略也从"池化线程"挪到了这条专属线程上，
     * 沙箱期间脚本在同一线程里做的事都受策略约束。
     */
    private static <T> T executeOnOwnThread(SandboxConfig config, Callable<T> action) throws Exception {
        Object[] result = new Object[1];
        Throwable[] error = new Throwable[1];

        Thread worker = new Thread(() -> {
            try (SandboxContext ignored = enter(config)) {
                result[0] = action.call();
            } catch (Throwable t) {
                error[0] = t;
            }
        }, "Sandbox-Worker");
        worker.start();
        worker.join();

        if (error[0] != null) {
            if (error[0] instanceof Error err) throw err;
            if (error[0] instanceof Exception e) throw e;
            throw new RuntimeException(error[0]);
        }
        @SuppressWarnings("unchecked")
        T value = (T) result[0];
        return value;
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

    /**
     * 把任务包成"继承当前沙箱策略"的形式，交给子线程执行。
     *
     * <p><b>为什么非包不可</b>：libcore 的 BlockGuard 策略是纯 per-thread 的，而且
     * <b>不继承</b>。实测（Android 8.1 / API 27）：父线程 {@code setThreadPolicy} 之后，
     * 子线程拿到的永远是默认的宽松策略 {@code dalvik.system.BlockGuard$1}，
     * 无论"先装策略后建线程"还是"先建线程后装策略"都一样 —— 父线程那个代理对子线程完全不可见。
     * 于是"策略随线程继承"只能这么实现：在子线程上把 Java 层策略再装一遍。
     *
     * <p>seccomp 不用重复装：它是随线程继承的，子线程天然带着父线程的过滤器，
     * 这也是 {@link #wrap} 只调 {@link #enterJavaPolicy} 而不调 {@link #installSeccomp} 的原因。
     *
     * <p>用法：
     * <pre>{@code
     * new Thread(BlockGuardSandbox.wrap(() -> writeSomething())).start();
     * }</pre>
     * 不在沙箱里时（{@link #isActive()} 为 false）原样返回，包一层没有副作用。
     *
     * <p><b>覆盖不到的地方</b>：沙箱外的代码、以及绕过本方法直接 {@code new Thread(...)}
     * 起的线程仍然不受 Java 层约束。后者由 seccomp 的
     * {@link SeccompSandbox#BLOCK_THREAD_CREATE} 兜底（受限模式下禁线程创建）。
     */
    public static Runnable wrap(Runnable task) {
        SandboxConfig config = currentConfig.get();
        if (config == null || !isActive()) {
            return task;
        }
        return () -> {
            try (SandboxContext ignored = enterJavaPolicy(config)) {
                task.run();
            }
        };
    }

    /** {@link #wrap(Runnable)} 的返回值版本。 */
    public static <T> Callable<T> wrap(Callable<T> task) {
        SandboxConfig config = currentConfig.get();
        if (config == null || !isActive()) {
            return task;
        }
        return () -> {
            try (SandboxContext ignored = enterJavaPolicy(config)) {
                return task.call();
            }
        };
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

        private static boolean matchesWhitelist(WhitelistGroup whitelist) {
            StackTraceElement[] stack = Thread.currentThread().getStackTrace();
            for (StackTraceElement element : stack) {
                if (whitelist.matchesAny(element.getClassName())) {
                    return true;
                }
            }
            return false;
        }

        private static boolean isLocalSocketOperation() {
            return matchesWhitelist(LOCAL_SOCKET_WHITELIST);
        }

        private static boolean isWhitelistedDiskRead() {
            return matchesWhitelist(DISK_READ_WHITELIST);
        }

        private static boolean isWhitelistedDiskWrite() {
            return matchesWhitelist(DISK_WRITE_WHITELIST);
        }

        private static boolean isWhitelistedNetworkOperation() {
            return matchesWhitelist(NETWORK_WHITELIST);
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) {
            String methodName = method.getName();

            switch (methodName) {
                case "onReadFromDisk" -> {
                    if (!isBypassing() && !config.isDiskReadAllowed() && !isWhitelistedDiskRead()) {
                        throw new SecurityException("BlockGuardSandbox: 磁盘读取操作被禁止");
                    }
                    return null;
                }
                case "onWriteToDisk" -> {
                    if (!isBypassing() && !config.isDiskWriteAllowed() && !isWhitelistedDiskWrite()) {
                        throw new SecurityException("BlockGuardSandbox: 磁盘写入操作被禁止");
                    }
                    return null;
                }
                case "onNetwork" -> {
                    if (!isBypassing() && !config.isNetworkAllowed() && !isWhitelistedNetworkOperation()) {
                        if (config.isLocalSocketAllowed() && isLocalSocketOperation()) {
                            return null;
                        }
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
            return proxyObjectMethods(proxy, method, args);
        }
    }

    private static final class EmptyPolicyInvocationHandler implements InvocationHandler {
        @Override
        public Object invoke(Object proxy, Method method, Object[] args) {
            if ("getPolicyMask".equals(method.getName())) {
                return 0;
            }
            return proxyObjectMethods(proxy, method, args);
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
