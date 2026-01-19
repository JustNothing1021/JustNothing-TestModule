package com.justnothing.testmodule.utils.concurrent;

import com.justnothing.testmodule.utils.data.BootMonitor;
import com.justnothing.testmodule.utils.functions.Logger;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class ThreadPoolManager extends Logger {

    private static final Logger logger = new Logger() {
        @Override
        public String getTag() {
            return TAG;
        }
    };

    private static final String TAG = "ThreadPoolManager";
    private static volatile ThreadPoolManager instance = null;

    private static final int CPU_CORES = Runtime.getRuntime().availableProcessors();

    private static final int IO_POOL_SIZE = Math.max(4, CPU_CORES * 2);
    private static final int CPU_POOL_SIZE = CPU_CORES;
    private static final int FAST_POOL_SIZE = Math.max(8, CPU_CORES * 3);
    private static final int SOCKET_POOL_SIZE = Math.max(16, CPU_CORES * 4);

    private static final int IO_QUEUE_CAPACITY = 1000;
    private static final int CPU_QUEUE_CAPACITY = 100;
    private static final int FAST_QUEUE_CAPACITY = 500;
    private static final int SOCKET_QUEUE_CAPACITY = 2000;

    private static final long KEEP_ALIVE_TIME = 60L;

    private final ExecutorService ioExecutor;
    private final ExecutorService cpuExecutor;
    private final ExecutorService fastExecutor;
    private final ExecutorService socketExecutor;
    private final ScheduledExecutorService scheduledExecutor;

    private final AtomicInteger activeTasks = new AtomicInteger(0);
    private final AtomicInteger completedTasks = new AtomicInteger(0);
    private final AtomicInteger rejectedTasks = new AtomicInteger(0);

    private volatile boolean initialized = false;
    private volatile boolean shutdown = false;

    private ThreadPoolManager() {
        super();

        ThreadFactory ioThreadFactory = new NamedThreadFactory("IO-Pool", Thread.NORM_PRIORITY - 1);
        ThreadFactory cpuThreadFactory = new NamedThreadFactory("CPU-Pool", Thread.NORM_PRIORITY);
        ThreadFactory fastThreadFactory = new NamedThreadFactory("Fast-Pool", Thread.NORM_PRIORITY);
        ThreadFactory socketThreadFactory = new NamedThreadFactory("Socket-Pool", Thread.NORM_PRIORITY - 1);
        ThreadFactory scheduledThreadFactory = new NamedThreadFactory("Scheduled-Pool", Thread.NORM_PRIORITY);

        RejectionHandler rejectionHandler = new RejectionHandler();

        ioExecutor = new ThreadPoolExecutor(
                IO_POOL_SIZE,
                IO_POOL_SIZE * 2,
                KEEP_ALIVE_TIME, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(IO_QUEUE_CAPACITY),
                ioThreadFactory,
                rejectionHandler
        );

        cpuExecutor = new ThreadPoolExecutor(
                CPU_POOL_SIZE,
                CPU_POOL_SIZE,
                KEEP_ALIVE_TIME, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(CPU_QUEUE_CAPACITY),
                cpuThreadFactory,
                rejectionHandler
        );

        fastExecutor = new ThreadPoolExecutor(
                FAST_POOL_SIZE,
                FAST_POOL_SIZE * 2,
                30L, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(FAST_QUEUE_CAPACITY),
                fastThreadFactory,
                rejectionHandler
        );

        socketExecutor = new ThreadPoolExecutor(
                SOCKET_POOL_SIZE,
                SOCKET_POOL_SIZE * 2,
                KEEP_ALIVE_TIME, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(SOCKET_QUEUE_CAPACITY),
                socketThreadFactory,
                rejectionHandler
        );

        scheduledExecutor = Executors.newScheduledThreadPool(
                Math.max(2, CPU_CORES / 2),
                scheduledThreadFactory
        );

        initialized = true;
        info("ThreadPoolManager初始化完成 - CPU核心数: " + CPU_CORES +
                ", IO池: " + IO_POOL_SIZE + ", CPU池: " + CPU_POOL_SIZE +
                ", 快速池: " + FAST_POOL_SIZE + ", Socket池: " + SOCKET_POOL_SIZE);
    }

    @Override
    public String getTag() {
        return TAG;
    }

    public static ThreadPoolManager getInstance() {
        if (instance == null) {
            synchronized (ThreadPoolManager.class) {
                if (instance == null) {
                    if (BootMonitor.isZygotePhase()) {
                        logger.warn("Zygote阶段，延迟初始化ThreadPoolManager");
                        return null;
                    }
                    instance = new ThreadPoolManager();
                }
            }
        }
        return instance;
    }

    public static void initialize() {
        getInstance();
        if (instance != null && !instance.initialized) {
            logger.warn("ThreadPoolManager未初始化");
        }
    }

    public static void shutdown() {
        ThreadPoolManager mgr = getInstance();
        if (mgr != null) {
            mgr.shutdownInternal();
        }
    }

    private void shutdownInternal() {
        if (shutdown) {
            return;
        }

        shutdown = true;
        info("开始关闭ThreadPoolManager...");

        shutdownExecutor(ioExecutor, "IO");
        shutdownExecutor(cpuExecutor, "CPU");
        shutdownExecutor(fastExecutor, "Fast");
        shutdownExecutor(socketExecutor, "Socket");
        shutdownExecutor(scheduledExecutor, "Scheduled");

        info("ThreadPoolManager已关闭");
    }

    private void shutdownExecutor(ExecutorService executor, String name) {
        try {
            executor.shutdown();
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                warn(name + "线程池未在5秒内关闭，强制关闭");
                executor.shutdownNow();
                if (!executor.awaitTermination(3, TimeUnit.SECONDS)) {
                    error(name + "线程池强制关闭失败");
                }
            }
            info(name + "线程池已关闭");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            executor.shutdownNow();
            warn(name + "线程池关闭被中断");
        }
    }

    public static Future<?> submitIORunnable(Runnable task) {
        ThreadPoolManager mgr = getInstance();
        if (mgr == null) {
            return null;
        }
        return mgr.ioExecutor.submit(mgr.wrapTask(task));
    }

    public static <T> Future<T> submitIOCallable(Callable<T> task) {
        ThreadPoolManager mgr = getInstance();
        if (mgr == null) {
            return null;
        }
        return mgr.ioExecutor.submit(mgr.wrapTask(task));
    }

    public static Future<?> submitCPURunnable(Runnable task) {
        ThreadPoolManager mgr = getInstance();
        if (mgr == null) {
            return null;
        }
        return mgr.cpuExecutor.submit(mgr.wrapTask(task));
    }

    public static <T> Future<T> submitCPUCallable(Callable<T> task) {
        ThreadPoolManager mgr = getInstance();
        if (mgr == null) {
            return null;
        }
        return mgr.cpuExecutor.submit(mgr.wrapTask(task));
    }

    public static Future<?> submitFastRunnable(Runnable task) {
        ThreadPoolManager mgr = getInstance();
        if (mgr == null) {
            return null;
        }
        return mgr.fastExecutor.submit(mgr.wrapTask(task));
    }

    public static <T> Future<T> submitFastCallable(Callable<T> task) {
        ThreadPoolManager mgr = getInstance();
        if (mgr == null) {
            return null;
        }
        return mgr.fastExecutor.submit(mgr.wrapTask(task));
    }

    public static Future<?> submitSocketRunnable(Runnable task) {
        ThreadPoolManager mgr = getInstance();
        if (mgr == null) {
            return null;
        }
        return mgr.socketExecutor.submit(mgr.wrapTask(task));
    }

    public static <T> Future<T> submitSocketCallable(Callable<T> task) {
        ThreadPoolManager mgr = getInstance();
        if (mgr == null) {
            return null;
        }
        return mgr.socketExecutor.submit(mgr.wrapTask(task));
    }

    public static ScheduledFuture<?> schedule(Runnable command, long delay, TimeUnit unit) {
        ThreadPoolManager mgr = getInstance();
        if (mgr == null) {
            return null;
        }
        return mgr.scheduledExecutor.schedule(mgr.wrapTask(command), delay, unit);
    }

    public static ScheduledFuture<?> scheduleAtFixedRate(Runnable command, long initialDelay, long period, TimeUnit unit) {
        ThreadPoolManager mgr = getInstance();
        if (mgr == null) {
            return null;
        }
        return mgr.scheduledExecutor.scheduleAtFixedRate(mgr.wrapTask(command), initialDelay, period, unit);
    }

    public static ScheduledFuture<?> scheduleWithFixedDelay(Runnable command, long initialDelay, long delay, TimeUnit unit) {
        ThreadPoolManager mgr = getInstance();
        if (mgr == null) {
            return null;
        }
        return mgr.scheduledExecutor.scheduleWithFixedDelay(mgr.wrapTask(command), initialDelay, delay, unit);
    }

    public static void executeIO(Runnable task) {
        ThreadPoolManager mgr = getInstance();
        if (mgr == null) {
            return;
        }
        mgr.ioExecutor.execute(mgr.wrapTask(task));
    }

    public static void executeCPU(Runnable task) {
        ThreadPoolManager mgr = getInstance();
        if (mgr == null) {
            return;
        }
        mgr.cpuExecutor.execute(mgr.wrapTask(task));
    }

    public static void executeFast(Runnable task) {
        ThreadPoolManager mgr = getInstance();
        if (mgr == null) {
            return;
        }
        mgr.fastExecutor.execute(mgr.wrapTask(task));
    }

    public static void executeSocket(Runnable task) {
        ThreadPoolManager mgr = getInstance();
        if (mgr == null) {
            return;
        }
        mgr.socketExecutor.execute(mgr.wrapTask(task));
    }

    private Runnable wrapTask(Runnable task) {
        return () -> {
            if (shutdown) {
                return;
            }
            activeTasks.incrementAndGet();
            long startTime = System.currentTimeMillis();
            try {
                task.run();
                completedTasks.incrementAndGet();
            } catch (Throwable e) {
                error("任务执行异常", e);
            } finally {
                activeTasks.decrementAndGet();
                long duration = System.currentTimeMillis() - startTime;
                if (duration > 5000) {
                    warn("任务执行时间过长: " + duration + "ms");
                }
            }
        };
    }

    private <T> Callable<T> wrapTask(Callable<T> task) {
        return () -> {
            if (shutdown) {
                throw new RejectedExecutionException("ThreadPoolManager已关闭");
            }
            activeTasks.incrementAndGet();
            long startTime = System.currentTimeMillis();
            try {
                T result = task.call();
                completedTasks.incrementAndGet();
                return result;
            } finally {
                activeTasks.decrementAndGet();
                long duration = System.currentTimeMillis() - startTime;
                if (duration > 5000) {
                    warn("任务执行时间过长: " + duration + "ms");
                }
            }
        };
    }

    public static boolean isShutdown() {
        ThreadPoolManager mgr = getInstance();
        return mgr != null && mgr.shutdown;
    }

    public static int getActiveTaskCount() {
        ThreadPoolManager mgr = getInstance();
        return mgr != null ? mgr.activeTasks.get() : 0;
    }

    public static int getCompletedTaskCount() {
        ThreadPoolManager mgr = getInstance();
        return mgr != null ? mgr.completedTasks.get() : 0;
    }

    public static int getRejectedTaskCount() {
        ThreadPoolManager mgr = getInstance();
        return mgr != null ? mgr.rejectedTasks.get() : 0;
    }

    public static String getPoolStats() {
        ThreadPoolManager mgr = getInstance();
        if (mgr == null) {
            return "ThreadPoolManager[未初始化]";
        }
        return String.format(
                "ThreadPoolManager[active=%d, completed=%d, rejected=%d, shutdown=%s]",
                mgr.activeTasks.get(),
                mgr.completedTasks.get(),
                mgr.rejectedTasks.get(),
                mgr.shutdown
        );
    }

    private static class NamedThreadFactory implements ThreadFactory {
        private final AtomicInteger threadNumber = new AtomicInteger(1);
        private final String namePrefix;
        private final int priority;

        NamedThreadFactory(String namePrefix, int priority) {
            this.namePrefix = namePrefix;
            this.priority = priority;
        }

        @Override
        public Thread newThread(Runnable r) {
            Thread t = new Thread(r, namePrefix + "-" + threadNumber.getAndIncrement());
            t.setPriority(priority);
            t.setDaemon(true);
            return t;
        }
    }

    private class RejectionHandler implements RejectedExecutionHandler {
        @Override
        public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
            rejectedTasks.incrementAndGet();
            error("任务被拒绝执行，活跃线程: " + executor.getActiveCount() +
                    ", 队列大小: " + executor.getQueue().size() +
                    ", 已完成任务: " + executor.getCompletedTaskCount());

            if (!BootMonitor.isZygotePhase()) {
                try {
                    r.run();
                } catch (Throwable e) {
                    error("在调用线程中执行被拒绝的任务失败", e);
                }
            }
        }
    }
}
