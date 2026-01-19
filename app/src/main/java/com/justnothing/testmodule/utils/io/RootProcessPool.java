package com.justnothing.testmodule.utils.io;

import com.justnothing.testmodule.utils.concurrent.ThreadPoolManager;
import com.justnothing.testmodule.utils.data.BootMonitor;
import com.justnothing.testmodule.utils.functions.Logger;

import java.io.*;
import java.util.concurrent.*;
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
    private static final int MAX_RETRIES = 2;

    private final BlockingQueue<RootProcess> availableProcesses;
    private final AtomicInteger totalProcesses = new AtomicInteger(0);
    private final AtomicInteger activeCommands = new AtomicInteger(0);
    private final AtomicLong totalCommands = new AtomicLong(0);
    private final AtomicLong totalCommandTime = new AtomicLong(0);
    private final AtomicInteger failedCommands = new AtomicInteger(0);

    private final ReentrantLock poolLock = new ReentrantLock();
    private volatile boolean shutdown = false;

    private RootProcessPool() {
        super();
        this.availableProcesses = new LinkedBlockingQueue<>(MAX_POOL_SIZE);
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
                if (process != null) {
                    availableProcesses.offer(process);
                    totalProcesses.incrementAndGet();
                }
            } catch (Exception e) {
                error("初始化Root进程失败", e);
            }
        }
        info("Root进程池初始化完成，当前进程数: " + totalProcesses.get());
    }

    private void startMaintenanceTask() {
        ThreadPoolManager.scheduleAtFixedRate(() -> {
            if (shutdown) {
                return;
            }
            maintainPool();
        }, 10000, 10000, TimeUnit.MILLISECONDS);
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
                        if (process != null) {
                            availableProcesses.offer(process);
                            totalProcesses.incrementAndGet();
                        }
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

    private RootProcess createRootProcess() throws IOException {
        if (BootMonitor.isZygotePhase()) {
            throw new IOException("Zygote阶段，无法创建Root进程");
        }

        Process process = Runtime.getRuntime().exec("su");
        RootProcess rootProcess = new RootProcess(process);
        rootProcess.initialize();
        return rootProcess;
    }

    public static Future<IOManager.ProcessResult> executeCommandAsync(String command) {
        return executeCommandAsync(command, COMMAND_TIMEOUT_MS);
    }

    public static Future<IOManager.ProcessResult> executeCommandAsync(String command, long timeoutMs) {
        return ThreadPoolManager.submitIOCallable(() -> executeCommand(command, timeoutMs));
    }

    public static IOManager.ProcessResult executeCommand(String command) throws IOException, InterruptedException {
        return executeCommand(command, COMMAND_TIMEOUT_MS);
    }

    public static IOManager.ProcessResult executeCommand(String command, long timeoutMs) throws IOException, InterruptedException {
        RootProcessPool pool = getInstance();
        if (pool == null) {
            throw new IOException("RootProcessPool未初始化");
        }

        if (pool.shutdown) {
            throw new IOException("RootProcessPool已关闭");
        }

        if (BootMonitor.isZygotePhase()) {
            throw new IOException("Zygote阶段，无法执行Root命令");
        }

        RootProcess process = null;
        try {
            process = pool.acquireProcess(timeoutMs);
            if (process == null) {
                throw new IOException("无法获取Root进程");
            }

            IOManager.ProcessResult result = process.executeCommand(command, timeoutMs);

            if (result.isSuccess()) {
                pool.totalCommands.incrementAndGet();
                pool.totalCommandTime.addAndGet(result.executionTime);
            } else {
                pool.failedCommands.incrementAndGet();
            }

            return result;
        } finally {
            if (process != null) {
                pool.releaseProcess(process);
            }
            pool.activeCommands.decrementAndGet();
        }
    }

    private RootProcess acquireProcess(long timeoutMs) throws InterruptedException {
        activeCommands.incrementAndGet();

        RootProcess process = availableProcesses.poll(100, TimeUnit.MILLISECONDS);
        if (process != null) {
            return process;
        }

        poolLock.lock();
        try {
            if (totalProcesses.get() < MAX_POOL_SIZE) {
                try {
                    RootProcess newProcess = createRootProcess();
                    if (newProcess != null) {
                        availableProcesses.offer(newProcess);
                        totalProcesses.incrementAndGet();
                        return newProcess;
                    }
                } catch (Exception e) {
                    error("创建新Root进程失败", e);
                }
            }
        } finally {
            poolLock.unlock();
        }

        process = availableProcesses.poll(timeoutMs - 100, TimeUnit.MILLISECONDS);
        if (process == null) {
            throw new InterruptedException("获取Root进程超时");
        }

        return process;
    }

    private void releaseProcess(RootProcess process) {
        if (process == null) {
            return;
        }

        if (process.isHealthy()) {
            process.updateLastUsedTime();
            availableProcesses.offer(process);
        } else {
            process.close();
            poolLock.lock();
            try {
                totalProcesses.decrementAndGet();
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

        info("RootProcessPool已关闭");
    }

    public static String getStats() {
        RootProcessPool pool = getInstance();
        if (pool == null) {
            return "RootProcessPool[未初始化]";
        }
        return String.format(
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

        RootProcess(Process process) throws IOException {
            this.process = process;
            this.stdoutReader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), java.nio.charset.StandardCharsets.UTF_8));
            this.stderrReader = new BufferedReader(
                    new InputStreamReader(process.getErrorStream(), java.nio.charset.StandardCharsets.UTF_8));
            this.outputStream = new DataOutputStream(process.getOutputStream());
            this.lastUsedTime = System.currentTimeMillis();
            this.healthy = true;
        }

        void initialize() throws IOException {
            outputStream.writeBytes("echo 'ROOT_READY'\n");
            outputStream.flush();

            String line = stdoutReader.readLine();
            if (!"ROOT_READY".equals(line)) {
                throw new IOException("Root进程初始化失败");
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

            Thread stdoutThread = new Thread(() -> {
                try {
                    String line;
                    while ((line = stdoutReader.readLine()) != null) {
                        if (line.startsWith("COMMAND_EXIT_CODE:")) {
                            break;
                        }
                        stdout.append(line).append('\n');
                    }
                } catch (IOException e) {
                    healthy = false;
                }
            });

            Thread stderrThread = new Thread(() -> {
                try {
                    String line;
                    while ((line = stderrReader.readLine()) != null) {
                        stderr.append(line).append('\n');
                    }
                } catch (IOException e) {
                    healthy = false;
                }
            });

            stdoutThread.setDaemon(true);
            stderrThread.setDaemon(true);
            stdoutThread.start();
            stderrThread.start();

            long remainingTime = timeoutMs - (System.currentTimeMillis() - startTime);
            stdoutThread.join(Math.max(0, remainingTime));
            stderrThread.join(Math.max(0, remainingTime));

            if (stdoutThread.isAlive() || stderrThread.isAlive()) {
                stdoutThread.interrupt();
                stderrThread.interrupt();
                healthy = false;
                throw new InterruptedException("Root命令执行超时");
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
            } catch (Exception e) {
            }

            try {
                if (stdoutReader != null) {
                    stdoutReader.close();
                }
            } catch (Exception e) {
            }

            try {
                if (stderrReader != null) {
                    stderrReader.close();
                }
            } catch (Exception e) {
            }

            try {
                if (outputStream != null) {
                    outputStream.close();
                }
            } catch (Exception e) {
            }

            try {
                if (process != null) {
                    process.destroyForcibly();
                }
            } catch (Exception e) {
            }

            healthy = false;
        }
    }
}
