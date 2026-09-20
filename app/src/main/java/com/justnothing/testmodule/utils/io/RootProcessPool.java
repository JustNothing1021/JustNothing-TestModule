package com.justnothing.testmodule.utils.io;

import com.justnothing.testmodule.utils.concurrent.ThreadPoolManager;
import com.justnothing.testmodule.utils.data.BootMonitor;
import com.justnothing.testmodule.utils.logging.Logger;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.Locale;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;

public class RootProcessPool extends Logger {
    private static final String TAG = "RootProcessPool";
    private static final Logger logger = Logger.getLoggerForName(TAG);
    private static volatile RootProcessPool instance = null;

    /** 池内最多保留多少个 shell 进程；纯按需创建，不再启动时预创建。 */
    private static final int MAX_POOL_SIZE = 5;
    private static final long PROCESS_IDLE_TIMEOUT = 30000;
    private static final long COMMAND_TIMEOUT_MS = 30000;

    private static final long ACQUIRE_POLL_TIMEOUT_MS = 100;
    private static final long MAINTENANCE_INITIAL_DELAY_MS = 10000;
    private static final long MAINTENANCE_PERIOD_MS = 10000;
    private static final long RETRY_INITIAL_DELAY_MS = 5000;
    private static final long RETRY_PERIOD_MS = 5000;
    private static final long PROCESS_INIT_TIMEOUT_MS = 5000;
    private static final long SHUTDOWN_WAIT_ACTIVE_MS = 5000;


    private static final int ROOT_CREATE_MAX_FAILURES = 3;

    /** 判定 root 不可用后的冷却时间；冷却结束允许再探测一次（例如用户之后补了授权）。 */
    private static final long ROOT_UNAVAILABLE_COOLDOWN_MS = 5 * 60 * 1000;

    private final BlockingQueue<RootProcess> availableProcesses;
    private final BlockingQueue<RootProcess> availableNonRootProcesses;
    private final AtomicInteger totalProcesses = new AtomicInteger(0);
    private final AtomicInteger totalNonRootProcesses = new AtomicInteger(0);
    private final AtomicInteger activeCommands = new AtomicInteger(0);
    private final AtomicLong totalCommands = new AtomicLong(0);
    private final AtomicLong totalCommandTime = new AtomicLong(0);
    private final AtomicInteger failedCommands = new AtomicInteger(0);

    private final ReentrantLock poolLock = new ReentrantLock();
    private volatile boolean shutdown = false;

    /** root 创建连续失败次数；达到上限后进入冷却期，期间不再 spawn su。 */
    private final AtomicInteger rootCreateFailures = new AtomicInteger(0);

    /** 进入「root 不可用」冷却期的时刻（0 = 未进入冷却）。 */
    private volatile long rootUnavailableSince = 0;

    private final ConcurrentLinkedQueue<CompletableFuture<Void>> activeCommandFutures = new ConcurrentLinkedQueue<>();

    private RootProcessPool() {
        super();
        this.availableProcesses = new LinkedBlockingQueue<>(MAX_POOL_SIZE);
        this.availableNonRootProcesses = new LinkedBlockingQueue<>(MAX_POOL_SIZE);
        startMaintenanceTask();
        // info("RootProcessPool初始化完成");
    }

    @Override
    public String getTag() {
        return TAG;
    }

    public static RootProcessPool getInstance() {
        if (instance == null) {
            synchronized (RootProcessPool.class) {
                if (instance == null) {
                    if (BootMonitor.isZygotePhase()) {
                        return null;
                    }
                    instance = new RootProcessPool();
                }
            }
        }
        return instance;
    }

    private void startMaintenanceTask() {
        ThreadPoolManager.scheduleWithFixedDelay(() -> {
            if (shutdown) {
                return;
            }
            maintainPool();
        }, MAINTENANCE_INITIAL_DELAY_MS, MAINTENANCE_PERIOD_MS, TimeUnit.MILLISECONDS);
    }

    /** root 是否正处在「连续创建失败」的冷却期（期间不该再去 spawn su）。 */
    private boolean isRootTemporarilyUnavailable() {
        long since = rootUnavailableSince;
        if (since == 0) {
            return false;
        }
        if (System.currentTimeMillis() - since < ROOT_UNAVAILABLE_COOLDOWN_MS) {
            return true;
        }
        synchronized (this) {
            if (rootUnavailableSince == since) {
                rootUnavailableSince = 0;
                rootCreateFailures.set(0);
                info("root 不可用的冷却期已结束，允许再次尝试创建 su");
            }
        }
        return false;
    }

    private void recordRootCreateFailure() {
        int failures = rootCreateFailures.incrementAndGet();
        if (failures >= ROOT_CREATE_MAX_FAILURES && rootUnavailableSince == 0) {
            rootUnavailableSince = System.currentTimeMillis();
            error("连续 " + failures + " 次创建 Root 进程失败，判定 root 不可用；"
                    + "接下来 " + (ROOT_UNAVAILABLE_COOLDOWN_MS / 60000) + " 分钟内不再尝试拉起 su。"
                    + "（反复拉起 su 会不停触发 Magisk 授权弹窗，进而拖垮系统）");
        }
    }

    private void recordRootCreateSuccess() {
        if (rootCreateFailures.get() != 0 || rootUnavailableSince != 0) {
            info("Root 进程创建成功，重置失败计数");
        }
        rootCreateFailures.set(0);
        rootUnavailableSince = 0;
    }

    /**
     * 探测 root 是否可用，供 {@code ShellExecutorProvider} 选择执行器使用。
     *
     * <p>有界探测：</p>
     * <ul>
     *   <li>池里已有健康的 root 进程 → 直接可用；</li>
     *   <li>处在失败冷却期 → 直接返回 false，<b>不会</b> spawn su；</li>
     *   <li>否则尝试创建一个，成功即可用；失败会计入失败计数。</li>
     * </ul>
     */
    public boolean probeRootAvailability() {
        if (shutdown) {
            return false;
        }
        for (RootProcess process : availableProcesses) {
            if (process.isHealthy()) {
                return true;
            }
        }
        if (isRootTemporarilyUnavailable()) {
            return false;
        }

        RootProcess created;
        try {
            created = createRootProcess();
        } catch (Exception e) {
            warn("root 可用性探测失败: " + e.getMessage());
            return false;
        }

        poolLock.lock();
        try {
            if (totalProcesses.get() < MAX_POOL_SIZE) {
                availableProcesses.offer(created);
                totalProcesses.incrementAndGet();
                return true;
            }
        } finally {
            poolLock.unlock();
        }
        created.close();
        return true;
    }

    private void maintainPool() {
        poolLock.lock();
        
        try {
            int currentSize = totalProcesses.get();

            // 维护任务只负责「回收」，不再负责「补足」。
            if (currentSize > MAX_POOL_SIZE) {
                int toRemove = currentSize - MAX_POOL_SIZE;
                for (int i = 0; i < toRemove; i++) {
                    RootProcess process = availableProcesses.poll();
                    if (process != null) {
                        process.close();
                        totalProcesses.decrementAndGet();
                    }
                }
                info("维护任务：移除了 " + toRemove + " 个超出上限的Root进程");
            }

            long currentTime = System.currentTimeMillis();
            Iterator<RootProcess> iterator = availableProcesses.iterator();
            while (iterator.hasNext()) {
                RootProcess process = iterator.next();

                // 检查进程是否不健康或超时空闲
                if (!process.isHealthy() || currentTime - process.getLastUsedTime() > PROCESS_IDLE_TIMEOUT) {
                    iterator.remove();
                    process.close();
                    totalProcesses.decrementAndGet();
                }
            }
        } finally {
            poolLock.unlock();
        }
    }

    private RootProcess createRootProcess() throws IOException, InterruptedException {
        if (BootMonitor.isZygotePhase()) {
            throw new IOException("Zygote阶段，无法创建Root进程");
        }

        // 冷却期内直接拒绝：此期间绝不能再拉起 su，否则会不停触发 Magisk 授权弹窗
        if (isRootTemporarilyUnavailable()) {
            throw new IOException("root 暂不可用（连续创建失败已进入冷却期），本次跳过 su 创建");
        }

        ProcessBuilder pb = new ProcessBuilder("su");
        try {
            Process process = pb.start();
            RootProcess rootProcess = new RootProcess(process);
            rootProcess.initialize();
            recordRootCreateSuccess();
            return rootProcess;
        } catch (IOException | InterruptedException e) {
            recordRootCreateFailure();
            throw e;
        }
    }

    private RootProcess createNonRootProcess() throws IOException, InterruptedException {
        if (BootMonitor.isZygotePhase()) {
            throw new IOException("Zygote阶段，无法创建非Root进程");
        }

        ProcessBuilder pb = new ProcessBuilder("/system/bin/sh");
        Process process = pb.start();
        RootProcess rootProcess = new RootProcess(process);
        rootProcess.initialize();
        return rootProcess;
    }

    public static CompletableFuture<IOManager.ProcessResult> executeCommandAsync(String command) {
        return executeCommandAsync(command, COMMAND_TIMEOUT_MS);
    }

    public static CompletableFuture<IOManager.ProcessResult> executeCommandAsync(String command, long timeoutMs) {
        return executeCommandAsync(command, timeoutMs, true);
    }

    public static CompletableFuture<IOManager.ProcessResult> executeCommandAsync(String command, long timeoutMs, boolean useRoot) {
        RootProcessPool pool = getInstance();
        if (pool == null || pool.shutdown) {
            CompletableFuture<IOManager.ProcessResult> failed = new CompletableFuture<>();
            failed.completeExceptionally(new IOException("RootProcessPool未初始化或已关闭"));
            return failed;
        }

        CompletableFuture<IOManager.ProcessResult> future = CompletableFuture.supplyAsync(() -> {
            try {
                return executeCommand(command, timeoutMs, useRoot);
            } catch (IOException | InterruptedException e) {
                throw new RuntimeException(e);
            }
        });

        CompletableFuture<Void> trackingFuture = future.thenAccept(r -> {});
        pool.activeCommandFutures.add(trackingFuture);

        trackingFuture.whenComplete((v, ex) -> pool.activeCommandFutures.remove(trackingFuture));

        return future;
    }

    public static IOManager.ProcessResult executeCommand(String command) throws IOException, InterruptedException {
        return executeCommand(command, COMMAND_TIMEOUT_MS);
    }

    public static IOManager.ProcessResult executeCommand(String command, long timeoutMs) throws IOException, InterruptedException {
        return executeCommand(command, timeoutMs, true);
    }

    public static IOManager.ProcessResult executeCommand(String command, long timeoutMs, boolean useRoot) throws IOException, InterruptedException {
        RootProcessPool pool = getInstance();
        if (pool == null) {
            throw new IOException("RootProcessPool未初始化");
        }

        if (pool.shutdown) {
            throw new IOException("RootProcessPool已关闭");
        }

        if (BootMonitor.isZygotePhase()) {
            throw new IOException("Zygote阶段，无法执行命令");
        }

        String processType = useRoot ? "Root" : "非Root";
        pool.info("执行" + processType + "命令: " + command + " (超时: " + timeoutMs + "ms)");

        // [优化] 仅在 executeCommand 中管理 activeCommands 计数
        pool.activeCommands.incrementAndGet();
        RootProcess process = null;
        try {
            process = pool.acquireProcess(timeoutMs, useRoot);

            IOManager.ProcessResult result = process.executeCommand(command, timeoutMs);

            if (result.isSuccess()) {
                pool.totalCommands.incrementAndGet();
                pool.totalCommandTime.addAndGet(result.executionTime());

                String stdout = result.stdout();
                if (stdout != null && !stdout.trim().isEmpty()) {
                    pool.debug("命令输出(stdout): " + stdout.trim());
                }

                pool.info("命令执行成功, 退出码: " + result.exitCode() + ", 耗时: " + result.executionTime() + "ms");
            } else {
                pool.failedCommands.incrementAndGet();

                String stderr = result.stderr();
                String stdout = result.stdout();

                pool.error("命令执行失败, 退出码: " + result.exitCode() + ", 耗时: " + result.executionTime() + "ms");
                if (stdout != null && !stdout.trim().isEmpty()) {
                    pool.error("命令输出(stdout): " + stdout.trim());
                }
                if (stderr != null && !stderr.trim().isEmpty()) {
                    pool.error("错误输出(stderr): " + stderr.trim());
                }
            }

            return result;
        } catch (Exception e) {
            pool.error("命令执行异常: " + command, e);
            throw e;
        } finally {
            if (process != null) {
                pool.releaseProcess(process, useRoot);
            }
            pool.activeCommands.decrementAndGet();
        }
    }

    // [优化] acquireProcess 不再触碰 activeCommands，只负责获取进程
    private RootProcess acquireProcess(long timeoutMs, boolean useRoot) throws IOException, InterruptedException {
        BlockingQueue<RootProcess> queue = useRoot ? availableProcesses : availableNonRootProcesses;
        AtomicInteger totalCounter = useRoot ? totalProcesses : totalNonRootProcesses;
        String processType = useRoot ? "Root" : "非Root";

        // 1. 快速从队列获取
        RootProcess process = queue.poll(ACQUIRE_POLL_TIMEOUT_MS, TimeUnit.MILLISECONDS);

        // 修复：检查并清理不健康的进程（防止资源泄漏！）
        if (process != null) {
            if (process.isHealthy()) {
                return process;
            } else {
                // 关键修复：关闭不健康的进程，防止 su 进程、流对象等资源泄漏！
                warn("发现不健康的" + processType + "进程, 正在关闭");
                process.close();
                totalCounter.decrementAndGet();
            }
        }

        // 2. 尝试创建新进程（不在锁内做重 IO）
        RootProcess newProcess = null;
        boolean needCreate = false;
        poolLock.lock();
        try {
            if (totalCounter.get() < MAX_POOL_SIZE) {
                needCreate = true;
            }
        } finally {
            poolLock.unlock();
        }

        if (needCreate) {
            try {
                newProcess = useRoot ? createRootProcess() : createNonRootProcess();
            } catch (Exception e) {
                warn("按需创建" + processType + "进程失败: " + e.getMessage());
            }
        }

        // 3. 如果创建成功，尝试放入队列（加锁检查容量）
        if (newProcess != null) {
            poolLock.lock();
            try {
                if (totalCounter.get() < MAX_POOL_SIZE) {
                    queue.offer(newProcess);
                    totalCounter.incrementAndGet();
                    info("按需创建" + processType + "进程，当前进程数: " + totalCounter.get());
                    return newProcess;
                } else {
                    // 池已满，直接关闭新进程
                    newProcess.close();
                }
            } finally {
                poolLock.unlock();
            }
        }

        // 4. 最后再尝试从队列获取一次（带健康检查）
        process = queue.poll(timeoutMs, TimeUnit.MILLISECONDS);
        if (process != null && process.isHealthy()) {
            return process;
        } else if (process != null && !process.isHealthy()) {
            // 同样需要清理不健康的进程
            warn("最后获取时发现不健康的" + processType + "进程，正在关闭");
            process.close();
            totalCounter.decrementAndGet();
        }

        throw new IOException("没有可用的" + processType + "进程，请稍后重试");
    }

    private void releaseProcess(RootProcess process, boolean useRoot) {
        if (process == null) {
            return;
        }

        BlockingQueue<RootProcess> queue = useRoot ? availableProcesses : availableNonRootProcesses;
        AtomicInteger totalCounter = useRoot ? totalProcesses : totalNonRootProcesses;

        if (process.isHealthy()) {
            process.updateLastUsedTime();
            queue.offer(process);
        } else {
            process.close();
            poolLock.lock();
            try {
                totalCounter.decrementAndGet();
            } finally {
                poolLock.unlock();
            }
        }
    }

    public static void shutdown() {
        RootProcessPool pool = getInstance();
        if (pool != null) {
            pool.shutdownInternal();
        }
    }

    // [优化] 使用 CompletableFuture 等待活动命令完成
    private void shutdownInternal() {
        if (shutdown) {
            return;
        }

        shutdown = true;
        info("开始关闭RootProcessPool...");

        // 使用 CompletableFuture 等待所有活动命令完成
        CompletableFuture<Void> allCommands = CompletableFuture.allOf(
                activeCommandFutures.toArray(new CompletableFuture[0])
        );

        try {
            allCommands.get(SHUTDOWN_WAIT_ACTIVE_MS, TimeUnit.MILLISECONDS);
            info("所有活动命令已完成");
        } catch (TimeoutException e) {
            warn("关闭时仍有 " + activeCommands.get() + " 个活动命令未完成");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            warn("关闭等待被中断");
        } catch (ExecutionException e) {
            warn("关闭等待异常: " + e.getCause());
        }

        poolLock.lock();
        try {
            // 关闭所有空闲的 Root 进程
            for (RootProcess process : availableProcesses) {
                process.close();
            }
            availableProcesses.clear();
            totalProcesses.set(0);

            // 关闭所有空闲的非 Root 进程
            for (RootProcess process : availableNonRootProcesses) {
                process.close();
            }
            availableNonRootProcesses.clear();
            totalNonRootProcesses.set(0);
        } finally {
            poolLock.unlock();
        }

        info("RootProcessPool已关闭");
    }

    public static String getStats() {
        RootProcessPool pool = getInstance();
        if (pool == null) {
            return "RootProcessPool[未初始化]";
        }
        return String.format(
                Locale.getDefault(),
                "RootProcessPool[total=%d, available=%d, active=%d, totalCommands=%d, failed=%d, avgTime=%dms]",
                pool.totalProcesses.get(),
                pool.availableProcesses.size(),
                pool.activeCommands.get(),
                pool.totalCommands.get(),
                pool.failedCommands.get(),
                pool.totalCommands.get() > 0 ? pool.totalCommandTime.get() / pool.totalCommands.get() : 0
        );
    }

    // [优化] 添加 closeQuietly 工具方法，简化资源关闭
    private static void closeQuietly(AutoCloseable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (Exception ignored) {
            }
        }
    }

    private static void destroyProcessQuietly(Process process) {
        if (process != null) {
            try {
                process.destroyForcibly();
            } catch (Exception ignored) {
            }
        }
    }

    private static final class RootProcess {
        /** 初始化握手：su 起来后回显这一行才算可用。 */
        private static final String READY = "ROOT_READY";
        /** 命令结束哨兵的前缀，后面接退出码。 */
        private static final String EXIT_CODE_PREFIX = "COMMAND_EXIT_CODE:";
        /** 读线程遇到 EOF（进程结束）时投入队列的结束标记。 */
        private static final Object EOF = new Object();

        private static final AtomicInteger PUMP_NUMBER = new AtomicInteger(0);

        private final Process process;
        private final DataOutputStream outputStream;
        /** stdout 行队列；元素是 String（一行）或 {@link #EOF}。 */
        private final BlockingQueue<Object> stdoutLines = new LinkedBlockingQueue<>();
        /** stderr 行队列；元素是 String（一行）或 {@link #EOF}。 */
        private final BlockingQueue<Object> stderrLines = new LinkedBlockingQueue<>();
        private volatile long lastUsedTime;
        private volatile boolean healthy;
        private volatile boolean closed;

        RootProcess(Process process) {
            this.process = process;
            this.outputStream = new DataOutputStream(process.getOutputStream());
            startPump("stdout", process.getInputStream(), stdoutLines);
            startPump("stderr", process.getErrorStream(), stderrLines);
            this.lastUsedTime = System.currentTimeMillis();
            this.healthy = true;
        }

        /**
         * 起一个常驻读线程，把某个流按行投进队列。
         *
         * <p>读线程与进程同生共死，而不是每条命令发一个一次性任务：这样阻塞在
         * {@code readLine()} 上属于「设计如此」而非泄漏，线程数上限 = 存活进程数 × 2，
         * 也不会出现「同一个 {@code BufferedReader} 被不同命令的不同线程先后/并发读取」。</p>
         */
        private void startPump(String streamName, InputStream source, BlockingQueue<Object> sink) {
            Thread pump = new Thread(() -> {
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(source, StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        sink.put(line);
                    }
                } catch (IOException ignored) {
                    // 流被关闭（进程结束或池关闭）
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    sink.offer(EOF);
                }
            }, "RootProcessPool-" + streamName + "-" + PUMP_NUMBER.getAndIncrement());
            pump.setDaemon(true);
            pump.start();
        }

        void initialize() throws IOException, InterruptedException {
            outputStream.writeBytes("echo '" + READY + "'\n");
            outputStream.flush();

            Object item;
            try {
                item = stdoutLines.poll(PROCESS_INIT_TIMEOUT_MS, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                close();
                throw e;
            }

            if (item == null) {
                // 此时 su 极可能正卡在等 Magisk 授权，必须把进程干掉：留着它就多一个
                // 永远挂起的进程（实测就见过 5 个挂在 system_server 下的 su）。
                close();
                throw new IOException("Root进程初始化超时（5秒），可能需要手动授权");
            }
            if (item == EOF) {
                close();
                throw new IOException("Root进程初始化失败：su 进程已退出（可能需要手动授权）");
            }
            if (!READY.equals(item)) {
                // 也走 close()：旧实现只在超时分支销毁进程，这条路径会连 su 进程和它的三个流一起漏掉
                close();
                throw new IOException("Root进程初始化失败，收到: " + item);
            }
        }

        IOManager.ProcessResult executeCommand(String command, long timeoutMs) throws IOException, InterruptedException {
            long startTime = System.currentTimeMillis();
            updateLastUsedTime();

            // 丢掉上一条命令可能残留在队列里的输出，保证下面读到的一定属于本条命令
            stdoutLines.clear();
            stderrLines.clear();

            outputStream.writeBytes(command + "\n");
            outputStream.writeBytes("echo '" + EXIT_CODE_PREFIX + "'$?\n");
            outputStream.flush();

            StringBuilder stdout = new StringBuilder();
            StringBuilder stderr = new StringBuilder();

            String exitCodeLine = null;
            boolean processEnded = false;
            long deadline = System.currentTimeMillis() + timeoutMs;
            while (true) {
                long remaining = deadline - System.currentTimeMillis();
                if (remaining <= 0) {
                    break; // 超时
                }
                Object item = stdoutLines.poll(remaining, TimeUnit.MILLISECONDS);
                if (item == null) {
                    break; // 超时
                }
                if (item == EOF) {
                    processEnded = true;
                    break;
                }
                String line = (String) item;
                if (line.startsWith(EXIT_CODE_PREFIX)) {
                    exitCodeLine = line;
                    break;
                }
                stdout.append(line).append('\n');
            }

            // stderr 只用于诊断：这一轮已经到达的全部收走，拿多少算多少
            for (Object item; (item = stderrLines.poll()) != null; ) {
                if (item == EOF) {
                    break;
                }
                stderr.append((String) item).append('\n');
            }

            if (exitCodeLine == null) {
                // 超时或进程中途结束：管道里可能还留着半截输出，继续复用这条连接会让
                // 下一条命令读到别人的输出与退出码，所以直接销毁进程。
                close();
                if (processEnded) {
                    throw new IOException("Root进程已终止");
                }
                logger.warn("Root command timeout (" + timeoutMs + "ms), forcibly terminated");
                throw new RootCommandTimeoutException("Root命令执行超时 (" + timeoutMs + "ms)", timeoutMs);
            }

            int exitCode;
            try {
                exitCode = Integer.parseInt(exitCodeLine.substring(EXIT_CODE_PREFIX.length()).trim());
            } catch (NumberFormatException e) {
                exitCode = -1;
            }

            long executionTime = System.currentTimeMillis() - startTime;
            return new IOManager.ProcessResult(exitCode, stdout.toString(), stderr.toString(), executionTime);
        }

        boolean isHealthy() {
            return healthy && !closed && process != null && process.isAlive();
        }

        long getLastUsedTime() {
            return lastUsedTime;
        }

        void updateLastUsedTime() {
            this.lastUsedTime = System.currentTimeMillis();
        }

        /**
         * 关闭进程并释放资源（可重复调用）。
         *
         * <p>顺序很重要：先杀进程，让读线程自己从 {@code readLine()} 里带着 EOF/IOException
         * 退出（它们用 try-with-resources 关自己的 reader）。绝不能在这里 close 那两个
         * {@code BufferedReader} —— {@code readLine()} 全程持有 reader 的内部锁，外部
         * {@code close()} 会一直等锁，把调用方（乃至持有 poolLock 的维护线程）挂死。</p>
         */
        void close() {
            if (closed) {
                return;
            }
            closed = true;
            healthy = false;
            try {
                outputStream.writeBytes("exit\n");
                outputStream.flush();
            } catch (Exception ignored) {
                // 进程可能已经没了
            }
            destroyProcessQuietly(process);
            closeQuietly(outputStream);
        }
    }
}