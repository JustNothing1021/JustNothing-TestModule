package com.justnothing.testmodule.utils.io;

import com.justnothing.testmodule.utils.concurrent.ThreadPoolManager;
import com.justnothing.testmodule.utils.data.BootMonitor;
import com.justnothing.testmodule.utils.functions.Logger;

import java.io.*;
import java.util.Locale;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
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

    private static final ExecutorService IO_THREAD_POOL = Executors.newCachedThreadPool(r -> {
        Thread t = new Thread(r, "RootProcess-IO");
        t.setDaemon(true);
        return t;
    });

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
        ThreadPoolManager.scheduleAtFixedRate(() -> {
            if (shutdown) {
                return;
            }
            maintainPool();
        }, 10000, 10000, TimeUnit.MILLISECONDS);
    }

    private void startRootProcessRetryTask() {
        ThreadPoolManager.scheduleAtFixedRate(() -> {
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
        }, 5000, 5000, TimeUnit.MILLISECONDS);
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

            long currentTime = System.currentTimeMillis();
            for (RootProcess process : availableProcesses) {
                if (currentTime - process.getLastUsedTime() > PROCESS_IDLE_TIMEOUT) {
                    availableProcesses.remove(process);
                    process.close();
                    totalProcesses.decrementAndGet();
                    debug("移除空闲Root进程");
                    break;
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

    public static Future<IOManager.ProcessResult> executeCommandAsync(String command) {
        return executeCommandAsync(command, COMMAND_TIMEOUT_MS);
    }

    public static Future<IOManager.ProcessResult> executeCommandAsync(String command, long timeoutMs) {
        return executeCommandAsync(command, timeoutMs, true);
    }

    public static Future<IOManager.ProcessResult> executeCommandAsync(String command, long timeoutMs, boolean useRoot) {
        return ThreadPoolManager.submitIOCallable(() -> executeCommand(command, timeoutMs, useRoot));
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

    private RootProcess acquireProcess(long timeoutMs, boolean useRoot) throws IOException, InterruptedException {
        activeCommands.incrementAndGet();

        BlockingQueue<RootProcess> queue = useRoot ? availableProcesses : availableNonRootProcesses;
        AtomicInteger totalCounter = useRoot ? totalProcesses : totalNonRootProcesses;
        String processType = useRoot ? "Root" : "非Root";

        RootProcess process = queue.poll(100, TimeUnit.MILLISECONDS);
        if (process != null && process.isHealthy()) {
            return process;
        }

        poolLock.lock();
        try {
            if (totalCounter.get() < MAX_POOL_SIZE) {
                try {
                    RootProcess newProcess = useRoot ? createRootProcess() : createNonRootProcess();
                    queue.offer(newProcess);
                    totalCounter.incrementAndGet();
                    info("按需创建" + processType + "进程，当前进程数: " + totalCounter.get());
                    return newProcess;
                } catch (Exception e) {
                    warn("按需创建" + processType + "进程失败: " + e.getMessage());
                }
            }
        } finally {
            poolLock.unlock();
        }

        process = queue.poll(100, TimeUnit.MILLISECONDS);
        if (process != null && process.isHealthy()) {
            return process;
        }

        activeCommands.decrementAndGet();
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

    private void shutdownInternal() {
        if (shutdown) {
            return;
        }

        shutdown = true;
        info("开始关闭RootProcessPool...");

        poolLock.lock();
        try {
            for (RootProcess process : availableProcesses) {
                process.close();
            }
            availableProcesses.clear();
            totalProcesses.set(0);
        } finally {
            poolLock.unlock();
        }

        try {
            IO_THREAD_POOL.shutdown();
            if (!IO_THREAD_POOL.awaitTermination(5000, TimeUnit.MILLISECONDS)) {
                IO_THREAD_POOL.shutdownNow();
            }
        } catch (InterruptedException e) {
            IO_THREAD_POOL.shutdownNow();
            Thread.currentThread().interrupt();
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

            Future<String> initFuture = CompletableFuture.supplyAsync(() -> {
                try {
                    return stdoutReader.readLine();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }, IO_THREAD_POOL);

            try {
                String line = initFuture.get(5000, TimeUnit.MILLISECONDS);
                if (!"ROOT_READY".equals(line)) {
                    throw new IOException("Root进程初始化失败，收到: " + line);
                }
            } catch (TimeoutException e) {
                initFuture.cancel(true);
                throw new IOException("Root进程初始化超时（5秒），可能需要手动授权");
            } catch (ExecutionException e) {
                throw new IOException("Root进程初始化失败: " + e.getCause().getMessage());
            }
        }

        IOManager.ProcessResult executeCommand(String command, long timeoutMs) throws IOException, InterruptedException {
            long startTime = System.currentTimeMillis();
            updateLastUsedTime();

            StringBuilder stdout = new StringBuilder();
            StringBuilder stderr = new StringBuilder();

            outputStream.writeBytes(command + "\n");
            outputStream.writeBytes("echo 'COMMAND_EXIT_CODE:'$?\n");
            outputStream.flush();

            final long commandStartTime = System.currentTimeMillis();
            final AtomicBoolean ioComplete = new AtomicBoolean(false);

            CompletableFuture<String> stdoutFuture = CompletableFuture.supplyAsync(() -> {
                try {
                    StringBuilder localStdout = new StringBuilder();
                    String line;
                    while ((line = stdoutReader.readLine()) != null) {
                        if (line.startsWith("COMMAND_EXIT_CODE:")) {
                            break;
                        }
                        localStdout.append(line).append('\n');
                        if (System.currentTimeMillis() - commandStartTime > timeoutMs) {
                            break;
                        }
                    }
                    synchronized (ioComplete) {
                        if (!ioComplete.get()) {
                            ioComplete.set(true);
                            stdout.append(localStdout);
                        }
                    }
                    return "stdout_done";
                } catch (IOException e) {
                    healthy = false;
                    return "stdout_error";
                }
            }, IO_THREAD_POOL);

            CompletableFuture<String> stderrFuture = CompletableFuture.supplyAsync(() -> {
                try {
                    StringBuilder localStderr = new StringBuilder();
                    String line;
                    while ((line = stderrReader.readLine()) != null) {
                        localStderr.append(line).append('\n');
                        if (System.currentTimeMillis() - commandStartTime > timeoutMs) {
                            break;
                        }
                    }
                    synchronized (ioComplete) {
                        if (!ioComplete.get()) {
                            stderr.append(localStderr);
                        }
                    }
                    return "stderr_done";
                } catch (IOException e) {
                    healthy = false;
                    return "stderr_error";
                }
            }, IO_THREAD_POOL);

            try {
                CompletableFuture<Void> allFutures = CompletableFuture.allOf(stdoutFuture, stderrFuture);
                allFutures.get(timeoutMs, TimeUnit.MILLISECONDS);
            } catch (TimeoutException e) {
                healthy = false;
                stdoutFuture.cancel(true);
                stderrFuture.cancel(true);
                throw new InterruptedException("Root命令执行超时");
            } catch (ExecutionException e) {
                healthy = false;
                throw new IOException("I/O线程执行失败: " + e.getCause());
            }

            if (!process.isAlive()) {
                healthy = false;
                throw new InterruptedException("Root进程已终止");
            }

            int exitCode = 0;
            try {
                String lastLine = stdout.toString().trim();
                if (lastLine.contains("COMMAND_EXIT_CODE:")) {
                    int idx = lastLine.indexOf("COMMAND_EXIT_CODE:");
                    String exitCodeStr = lastLine.substring(idx + "COMMAND_EXIT_CODE:".length()).trim();
                    exitCode = Integer.parseInt(exitCodeStr);
                    stdout.setLength(stdout.length() - lastLine.length() - 1);
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

        void close() {
            try {
                if (outputStream != null) {
                    outputStream.writeBytes("exit\n");
                    outputStream.flush();
                }
            } catch (Exception ignored) {
            }

            try {
                if (stdoutReader != null) {
                    stdoutReader.close();
                }
            } catch (Exception ignored) {
            }

            try {
                if (stderrReader != null) {
                    stderrReader.close();
                }
            } catch (Exception ignored) {
            }

            try {
                if (outputStream != null) {
                    outputStream.close();
                }
            } catch (Exception ignored) {
            }

            try {
                if (process != null) {
                    process.destroyForcibly();
                }
            } catch (Exception ignored) {
            }

            healthy = false;
        }
    }
}
