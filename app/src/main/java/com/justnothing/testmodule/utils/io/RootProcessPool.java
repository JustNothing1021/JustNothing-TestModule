package com.justnothing.testmodule.utils.io;

import com.justnothing.testmodule.utils.concurrent.ThreadPoolManager;
import com.justnothing.testmodule.utils.data.BootMonitor;
import com.justnothing.testmodule.utils.logging.Logger;

import java.io.*;
import java.util.Iterator;
import java.util.Locale;
import java.util.Optional;
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
    private static volatile RootProcessPool instance = null;

    private static final int MIN_POOL_SIZE = 2;
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

    private final ConcurrentLinkedQueue<CompletableFuture<Void>> activeCommandFutures = new ConcurrentLinkedQueue<>();

    private RootProcessPool() {
        super();
        this.availableProcesses = new LinkedBlockingQueue<>(MAX_POOL_SIZE);
        this.availableNonRootProcesses = new LinkedBlockingQueue<>(MAX_POOL_SIZE);
        initializePool();
        startMaintenanceTask();
        info("RootProcessPool初始化完成");
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

    private void initializePool() {
        for (int i = 0; i < MIN_POOL_SIZE; i++) {
            try {
                RootProcess process = createRootProcess();
                availableProcesses.offer(process);
                totalProcesses.incrementAndGet();
            } catch (Exception e) {
                warn("初始化Root进程失败，将在后台重试: " + e.getMessage());
            }
        }

        for (int i = 0; i < MIN_POOL_SIZE; i++) {
            try {
                RootProcess process = createNonRootProcess();
                availableNonRootProcesses.offer(process);
                totalNonRootProcesses.incrementAndGet();
            } catch (Exception e) {
                error("初始化非Root进程失败", e);
            }
        }

        info("Root进程池初始化完成，当前Root进程数: " + totalProcesses.get() + ", 非Root进程数: " + totalNonRootProcesses.get());

        if (totalProcesses.get() == 0) {
            info("Root进程池为空，将在后台尝试创建Root进程");
            startRootProcessRetryTask();
        }
    }

    private void startMaintenanceTask() {
        ThreadPoolManager.scheduleWithFixedDelay(() -> {
            if (shutdown) {
                return;
            }
            maintainPool();
        }, MAINTENANCE_INITIAL_DELAY_MS, MAINTENANCE_PERIOD_MS, TimeUnit.MILLISECONDS);
    }

    private void startRootProcessRetryTask() {
        ThreadPoolManager.scheduleWithFixedDelay(() -> {
            if (shutdown) {
                return;
            }

            int currentSize = totalProcesses.get();
            if (currentSize >= MIN_POOL_SIZE) {
                return;
            }

            int toCreate = MIN_POOL_SIZE - currentSize;
            for (int i = 0; i < toCreate; i++) {
                try {
                    RootProcess process = createRootProcess();
                    availableProcesses.offer(process);
                    totalProcesses.incrementAndGet();
                    info("后台重试：成功创建Root进程，当前进程数: " + totalProcesses.get());
                } catch (Exception e) {
                    debug("后台重试：创建Root进程失败 - " + e.getMessage());
                }
            }
        }, RETRY_INITIAL_DELAY_MS, RETRY_PERIOD_MS, TimeUnit.MILLISECONDS);
    }

    private void maintainPool() {
        poolLock.lock();
        try {
            int currentSize = totalProcesses.get();
            int availableSize = availableProcesses.size();

            if (currentSize < MIN_POOL_SIZE) {
                int toCreate = MIN_POOL_SIZE - currentSize;
                for (int i = 0; i < toCreate; i++) {
                    try {
                        RootProcess process = createRootProcess();
                        availableProcesses.offer(process);
                        totalProcesses.incrementAndGet();
                    } catch (Exception e) {
                        error("维护任务：创建Root进程失败", e);
                    }
                }
                info("维护任务：创建了 " + toCreate + " 个Root进程");
            } else if (currentSize > MAX_POOL_SIZE && availableSize > MIN_POOL_SIZE) {
                int toRemove = Math.min(currentSize - MAX_POOL_SIZE, availableSize - MIN_POOL_SIZE);
                for (int i = 0; i < toRemove; i++) {
                    RootProcess process = availableProcesses.poll();
                    if (process != null) {
                        process.close();
                        totalProcesses.decrementAndGet();
                    }
                }
                info("维护任务：移除了 " + toRemove + " 个Root进程");
            }

            // [优化] 移除所有超时空闲进程，而不仅仅是第一个
            long currentTime = System.currentTimeMillis();
            Iterator<RootProcess> iterator = availableProcesses.iterator();
            while (iterator.hasNext()) {
                RootProcess process = iterator.next();
                if (currentTime - process.getLastUsedTime() > PROCESS_IDLE_TIMEOUT) {
                    iterator.remove();
                    process.close();
                    totalProcesses.decrementAndGet();
                    debug("移除空闲Root进程");
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

        ProcessBuilder pb = new ProcessBuilder("su");
        Process process = pb.start();
        RootProcess rootProcess = new RootProcess(process);
        rootProcess.initialize();
        return rootProcess;
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
        if (process != null && process.isHealthy()) {
            return process;
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

        // 4. 最后再尝试从队列获取一次
        process = queue.poll(ACQUIRE_POLL_TIMEOUT_MS, TimeUnit.MILLISECONDS);
        if (process != null && process.isHealthy()) {
            return process;
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

    private static class RootProcess {
        private final Process process;
        private final BufferedReader stdoutReader;
        private final BufferedReader stderrReader;
        private final DataOutputStream outputStream;
        private volatile long lastUsedTime;
        private volatile boolean healthy;

        RootProcess(Process process) {
            this.process = process;
            this.stdoutReader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), java.nio.charset.StandardCharsets.UTF_8));
            this.stderrReader = new BufferedReader(
                    new InputStreamReader(process.getErrorStream(), java.nio.charset.StandardCharsets.UTF_8));
            this.outputStream = new DataOutputStream(process.getOutputStream());
            this.lastUsedTime = System.currentTimeMillis();
            this.healthy = true;
        }

        void initialize() throws IOException, InterruptedException {
            outputStream.writeBytes("echo 'ROOT_READY'\n");
            outputStream.flush();

            // [优化] 使用 CompletableFuture 替代 Future
            CompletableFuture<String> initFuture = CompletableFuture.supplyAsync(() -> {
                try {
                    return stdoutReader.readLine();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });

            try {
                String line = initFuture.get(PROCESS_INIT_TIMEOUT_MS, TimeUnit.MILLISECONDS);
                if (!"ROOT_READY".equals(line)) {
                    throw new IOException("Root进程初始化失败，收到: " + line);
                }
            } catch (TimeoutException e) {
                initFuture.cancel(true);
                throw new IOException("Root进程初始化超时（5秒），可能需要手动授权");
            } catch (ExecutionException e) {
                throw new IOException("Root进程初始化失败: " +
                        Optional.ofNullable(e.getCause())
                                .map(Throwable::getMessage)
                                .orElse("暂无详细信息"));
            }
        }

        // [优化] 简化 IO 读取，去掉 ready() + sleep 轮询，改为两个 Future 同时等待
        IOManager.ProcessResult executeCommand(String command, long timeoutMs) throws IOException, InterruptedException {
            long startTime = System.currentTimeMillis();
            updateLastUsedTime();

            StringBuilder stdout = new StringBuilder();
            StringBuilder stderr = new StringBuilder();

            outputStream.writeBytes(command + "\n");
            outputStream.writeBytes("echo 'COMMAND_EXIT_CODE:'$?\n");
            outputStream.flush();

            final long commandStartTime = System.currentTimeMillis();

            // 读取 stdout 的任务
            CompletableFuture<Void> stdoutFuture = CompletableFuture.runAsync(() -> {
                try {
                    String line;
                    while ((line = stdoutReader.readLine()) != null) {
                        if (line.startsWith("COMMAND_EXIT_CODE:")) {
                            break;
                        }
                        stdout.append(line).append('\n');
                        if (System.currentTimeMillis() - commandStartTime > timeoutMs) {
                            break;
                        }
                    }
                } catch (IOException e) {
                    healthy = false;
                    throw new RuntimeException(e);
                }
            });

            // 读取 stderr 的任务
            CompletableFuture<Void> stderrFuture = CompletableFuture.runAsync(() -> {
                try {
                    String line;
                    while ((line = stderrReader.readLine()) != null) {
                        stderr.append(line).append('\n');
                        if (System.currentTimeMillis() - commandStartTime > timeoutMs) {
                            break;
                        }
                    }
                } catch (IOException e) {
                    healthy = false;
                    throw new RuntimeException(e);
                }
            });

            // 等待 stdout 完成（超时则取消两个任务）
            try {
                stdoutFuture.get(timeoutMs, TimeUnit.MILLISECONDS);
            } catch (TimeoutException e) {
                stdoutFuture.cancel(true);
                stderrFuture.cancel(true);
                healthy = false;
                throw new InterruptedException("Root命令执行超时");
            } catch (ExecutionException e) {
                healthy = false;
                throw new IOException("I/O线程执行失败: " + e.getCause());
            }

            // stderr 不严格等待，但尝试等待一小段时间（可选）
            try {
                stderrFuture.get(100, TimeUnit.MILLISECONDS);
            } catch (TimeoutException | ExecutionException ignored) {
                // stderr 不是必须的，忽略
            }

            if (!process.isAlive()) {
                healthy = false;
                throw new InterruptedException("Root进程已终止");
            }

            int exitCode = 0;
            String stdoutStr = stdout.toString();
            try {
                int idx = stdoutStr.lastIndexOf("COMMAND_EXIT_CODE:");
                if (idx != -1) {
                    String exitCodeStr = stdoutStr.substring(idx + "COMMAND_EXIT_CODE:".length()).trim();
                    exitCode = Integer.parseInt(exitCodeStr);
                    // 删除末尾的退出码行
                    stdout.setLength(idx);
                }
            } catch (Exception e) {
                exitCode = -1;
            }

            long executionTime = System.currentTimeMillis() - startTime;
            return new IOManager.ProcessResult(exitCode, stdout.toString(), stderr.toString(), executionTime);
        }

        boolean isHealthy() {
            return healthy && process != null && process.isAlive();
        }

        long getLastUsedTime() {
            return lastUsedTime;
        }

        void updateLastUsedTime() {
            this.lastUsedTime = System.currentTimeMillis();
        }

        // [优化] 使用 closeQuietly 简化关闭
        void close() {
            // 优雅地要求进程退出
            if (outputStream != null) {
                try {
                    outputStream.writeBytes("exit\n");
                    outputStream.flush();
                } catch (Exception ignored) {
                }
            }
            // 关闭流
            closeQuietly(stdoutReader);
            closeQuietly(stderrReader);
            closeQuietly(outputStream);
            // 强制杀死进程
            destroyProcessQuietly(process);
            healthy = false;
        }
    }
}