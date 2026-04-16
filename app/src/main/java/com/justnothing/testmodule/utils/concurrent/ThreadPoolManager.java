package com.justnothing.testmodule.utils.concurrent;

import com.justnothing.testmodule.utils.data.BootMonitor;
import com.justnothing.testmodule.utils.logging.Logger;

import java.util.Locale;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.Future;
import java.util.concurrent.Callable;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

public class ThreadPoolManager {
    private static final String TAG = "ThreadPoolManager";
    private static final Logger logger = Logger.getLoggerForName(TAG);
    private static volatile ThreadPoolManager instance = null;
    private static final int CPU_CORES = Runtime.getRuntime().availableProcessors();
    private static final boolean IS_LOW_END_DEVICE = CPU_CORES <= 4 || Runtime.getRuntime().maxMemory() < 128 * 1024 * 1024;
    private static final int IO_POOL_SIZE = Math.max(2, IS_LOW_END_DEVICE ? 2 : Math.max(4, CPU_CORES * 2));
    private static final int FAST_POOL_SIZE = Math.max(4, IS_LOW_END_DEVICE ? 4 : Math.max(6, CPU_CORES * 2));
    private static final int SOCKET_POOL_SIZE = Math.max(4, IS_LOW_END_DEVICE ? 4 : Math.max(6, CPU_CORES * 2));

    private static final int IO_QUEUE_CAPACITY = IS_LOW_END_DEVICE ? 100 : 500;
    private static final int FAST_QUEUE_CAPACITY = IS_LOW_END_DEVICE ? 100 : 300;
    private static final int SOCKET_QUEUE_CAPACITY = IS_LOW_END_DEVICE ? 200 : 1000;

    private static final long KEEP_ALIVE_TIME = 60L;

    private final ExecutorService ioExecutor;
    private final ExecutorService fastExecutor;
    private final ExecutorService socketExecutor;
    private final ScheduledExecutorService scheduledExecutor;

    private final AtomicInteger activeTasks = new AtomicInteger(0);
    private final AtomicInteger completedTasks = new AtomicInteger(0);
    private final AtomicInteger rejectedTasks = new AtomicInteger(0);
    private static boolean initialized = false;
    private volatile boolean shutdown = false;

    private ThreadPoolManager() {
        super();

        ThreadFactory ioThreadFactory = new NamedThreadFactory("IO-Pool", Thread.MAX_PRIORITY);
        ThreadFactory fastThreadFactory = new NamedThreadFactory("Fast-Pool", Thread.MAX_PRIORITY);
        ThreadFactory socketThreadFactory = new NamedThreadFactory("Socket-Pool", Thread.MAX_PRIORITY);
        ThreadFactory scheduledThreadFactory = new NamedThreadFactory("Scheduled-Pool", Thread.MAX_PRIORITY - 1);

        RejectionHandler rejectionHandler = new RejectionHandler();

        ioExecutor = new ThreadPoolExecutor(
                IO_POOL_SIZE,
                IO_POOL_SIZE * 2,
                KEEP_ALIVE_TIME, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(IO_QUEUE_CAPACITY),
                ioThreadFactory,
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

        logger.info("ThreadPoolManager初始化完成");
        logger.info("设备信息: CPU核心数: " + CPU_CORES +
                ", 低端设备: " + IS_LOW_END_DEVICE +
                ", 最大内存: " + (Runtime.getRuntime().maxMemory() / 1024 / 1024) + "MB");
        logger.info("线程池配置: IO池: " + IO_POOL_SIZE + "/" + IO_QUEUE_CAPACITY +
                ", 快速池: " + FAST_POOL_SIZE + "/" + FAST_QUEUE_CAPACITY +
                ", Socket池: " + SOCKET_POOL_SIZE + "/" + SOCKET_QUEUE_CAPACITY);
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

    public static void doNothing() {}

    public static void shutdown() {
        ThreadPoolManager mgr = getInstance();
        if (mgr != null) {
            mgr.shutdownInternal();
        }
    }

    public static void initialize() {
        if (!initialized) {
            getInstance();
            initialized = true;
        }
    }

    private void shutdownInternal() {
        if (shutdown) {
            return;
        }

        shutdown = true;
        logger.info("开始关闭ThreadPoolManager...");

        shutdownExecutor(ioExecutor, "IO");
        shutdownExecutor(fastExecutor, "Fast");
        shutdownExecutor(socketExecutor, "Socket");
        shutdownExecutor(scheduledExecutor, "Scheduled");

        logger.info("ThreadPoolManager已关闭");
    }

    private void shutdownExecutor(ExecutorService executor, String name) {
        try {
            executor.shutdown();
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                logger.warn(name + "线程池未在5秒内关闭，强制关闭");
                executor.shutdownNow();
                if (!executor.awaitTermination(3, TimeUnit.SECONDS)) {
                    logger.error(name + "线程池强制关闭失败");
                }
            }
            logger.info(name + "线程池已关闭");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            executor.shutdownNow();
            logger.warn(name + "线程池关闭被中断");
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

    // 强烈不建议用，可能导致未定义行为
    // Android Studio的解释：
    // Use of `scheduleAtFixedRate` is strongly discouraged
    // because it can lead to unexpected behavior when Android processes become cached
    //  (tasks may unexpectedly execute hundreds or thousands of times in quick succession
    //      when a process changes from cached to uncached);
    // prefer using `scheduleWithFixedDelay`
    @Deprecated(since = "0.4.7")
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


    /**
     * 以固定速率执行任务，直到指定条件满足。
     *
     * @param runnable        要执行的任务
     * @param initialDelay    首次执行延迟
     * @param period          固定周期（两次开始执行之间的间隔）
     * @param unit            时间单位
     * @param stopCondition   停止条件（返回true时停止后续调度）
     * @return ScheduledFuture<?> 可用于手动取消
     */
    @Deprecated(since = "0.4.7")
    public static ScheduledFuture<?> scheduleAtFixedRateUntil(
            Runnable runnable,
            long initialDelay,
            long period,
            TimeUnit unit,
            Supplier<Boolean> stopCondition) {
        
        ThreadPoolManager mgr = getInstance();
        if (mgr == null)  return null;

        AtomicReference<ScheduledFuture<?>> futureRef = new AtomicReference<>();


        Runnable wrapped = () -> {
            // 先检查停止条件
            if (stopCondition.get()) {
                Future<?> f = futureRef.get();
                if (f != null && !f.isCancelled()) {
                    f.cancel(false);   // 取消后续调度
                }
                return;
            }
            // 执行用户任务
            runnable.run();
        };

        ScheduledFuture<?> future = mgr.scheduledExecutor.scheduleAtFixedRate(
                mgr.wrapTask(wrapped), initialDelay, period, unit);
        futureRef.set(future);
        return future;
    }


    /**
     * 以固定速率执行任务，直到系统时间达到指定的截止时间戳。
     *
     * @param runnable            要执行的任务
     * @param initialDelay        首次执行延迟
     * @param period              固定周期（两次开始执行之间的间隔）
     * @param unit                时间单位
     * @param stopTimeMillis      停止时间（毫秒时间戳），当System.currentTimeMillis() >= stopTimeMillis时停止后续调度
     * @return ScheduledFuture<?> 可用于手动取消
     */
    @Deprecated(since = "0.4.7")
    public static ScheduledFuture<?> scheduleAtFixedRateUntil(
            Runnable runnable,
            long initialDelay,
            long period,
            TimeUnit unit,
            long stopTimeMillis) {
        return scheduleAtFixedRateUntil(runnable, initialDelay, period, unit, () -> System.currentTimeMillis() >= stopTimeMillis);
    }

    /**
     * 以固定延迟的方式执行任务，直到指定的停止条件满足。
     *
     * <p>固定延迟的含义：上一次任务执行<strong>结束</strong>后，等待 delay 时间，
     * 再开始下一次任务。因此两次任务开始之间的间隔 = 任务执行时间 + delay。
     *
     * <p>这与 scheduleAtFixedRate 不同：
     * <ul>
     *   <li>scheduleAtFixedRate：固定速率，两次任务<strong>开始</strong>之间的间隔固定为 period。</li>
     *   <li>scheduleWithFixedDelay：固定延迟，任务结束到下次开始之间的间隔固定为 delay。</li>
     * </ul>
     *
     * @param runnable            要周期执行的任务
     * @param initialDelay        首次执行的延迟时间
     * @param delay               每次任务结束后，到下一次开始之间的等待时间（固定延迟）
     * @param unit                时间单位（如TimeUnit.SECONDS）
     * @param stopCondition       停止条件：当它返回true时，不再调度后续执行。
     *                            条件会在每次任务<strong>开始之前</strong>检查。
     *                            如果条件已满足，当前任务不会执行，且后续调度被取消。
     * @return ScheduledFuture<?> 可用于手动取消任务（如果还没有被停止条件取消）
     *
     * @see #scheduleAtFixedRateUntil(Runnable, long, long, TimeUnit, Supplier)
     * @see ScheduledExecutorService#scheduleWithFixedDelay(Runnable, long, long, TimeUnit)
     */
    public static ScheduledFuture<?> scheduleWithFixedDelayUntil(
            Runnable runnable,
            long initialDelay,
            long delay,
            TimeUnit unit,
            Supplier<Boolean> stopCondition) {

        ThreadPoolManager mgr = getInstance();
        if (mgr == null) return null;

        AtomicReference<ScheduledFuture<?>> futureRef = new AtomicReference<>();


        Runnable wrappedTask = () -> {
            // 检查停止条件，如果满足则取消后续所有调度
            if (stopCondition.get()) {
                ScheduledFuture<?> f = futureRef.get();
                if (f != null && !f.isCancelled()) {
                    f.cancel(false);
                }
                return;
            }
            runnable.run();
        };

        ScheduledFuture<?> future = mgr.scheduledExecutor.scheduleWithFixedDelay(
                mgr.wrapTask(wrappedTask),
                initialDelay,
                delay,
                unit
        );

        futureRef.set(future);

        return future;
    }

    public static ScheduledFuture<?> scheduleWithFixedDelayWhile(
            Runnable runnable,
            long initialDelay,
            long delay,
            TimeUnit unit,
            Supplier<Boolean> stopCondition) {
        return scheduleWithFixedDelayUntil(runnable, initialDelay, delay, unit, () -> !stopCondition.get());
    }

    /**
     * 以固定延迟的方式执行任务，直到系统时间达到指定的截止时间。
     *
     * @param runnable            要周期执行的任务
     * @param initialDelay        首次执行的延迟时间
     * @param delay               固定延迟（任务结束后等待delay再开始下一次）
     * @param unit                时间单位
     * @param stopTimeMillis      停止时间戳（毫秒），当System.currentTimeMillis() >= stopTimeMillis时停止后续调度
     * @return ScheduledFuture<?> 可用于手动取消
     */
    public static ScheduledFuture<?> scheduleWithFixedDelayUntil(
            Runnable runnable,
            long initialDelay,
            long delay,
            TimeUnit unit,
            long stopTimeMillis) {
        return scheduleWithFixedDelayUntil(runnable, initialDelay, delay, unit,
                () -> System.currentTimeMillis() >= stopTimeMillis);
    }

    public static void executeIO(Runnable task) {
        ThreadPoolManager mgr = getInstance();
        if (mgr == null) {
            return;
        }
        mgr.ioExecutor.execute(mgr.wrapTask(task));
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
                logger.error("任务执行异常", e);
            } finally {
                activeTasks.decrementAndGet();
                long duration = System.currentTimeMillis() - startTime;
                if (duration > 5000) {
                    logger.warn("任务执行时间过长: " + duration + "ms");
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
                    logger.warn("任务执行时间过长: " + duration + "ms");
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
        
        ThreadPoolExecutor ioPool = (ThreadPoolExecutor) mgr.ioExecutor;
        ThreadPoolExecutor fastPool = (ThreadPoolExecutor) mgr.fastExecutor;
        ThreadPoolExecutor socketPool = (ThreadPoolExecutor) mgr.socketExecutor;
        
        return String.format(
                Locale.getDefault(),
                """
                        ThreadPoolManager[active=%d, completed=%d, rejected=%d, shutdown=%s]
                          IO池: active=%d, queued=%d, completed=%d
                          快速池: active=%d, queued=%d, completed=%d
                          Socket池: active=%d, queued=%d, completed=%d
                        """,
                mgr.activeTasks.get(),
                mgr.completedTasks.get(),
                mgr.rejectedTasks.get(),
                mgr.shutdown,
                ioPool.getActiveCount(), ioPool.getQueue().size(), ioPool.getCompletedTaskCount(),
                fastPool.getActiveCount(), fastPool.getQueue().size(), fastPool.getCompletedTaskCount(),
                socketPool.getActiveCount(), socketPool.getQueue().size(), socketPool.getCompletedTaskCount()
        );
    }

    public static void logDetailedPoolStats() {
        ThreadPoolManager mgr = getInstance();
        if (mgr == null) {
            logger.warn("ThreadPoolManager未初始化，无法获取详细状态");
            return;
        }
        
        ThreadPoolExecutor ioPool = (ThreadPoolExecutor) mgr.ioExecutor;
        ThreadPoolExecutor fastPool = (ThreadPoolExecutor) mgr.fastExecutor;
        ThreadPoolExecutor socketPool = (ThreadPoolExecutor) mgr.socketExecutor;
        
        logger.info("=== 线程池详细状态 ===");
        logger.info("IO池 - 活跃线程: " + ioPool.getActiveCount() + 
                ", 队列大小: " + ioPool.getQueue().size() + "/" + IO_QUEUE_CAPACITY +
                ", 已完成任务: " + ioPool.getCompletedTaskCount() +
                ", 核心线程: " + ioPool.getCorePoolSize() + 
                ", 最大线程: " + ioPool.getMaximumPoolSize());
        
        logger.info("快速池 - 活跃线程: " + fastPool.getActiveCount() +
                ", 队列大小: " + fastPool.getQueue().size() + "/" + FAST_QUEUE_CAPACITY +
                ", 已完成任务: " + fastPool.getCompletedTaskCount() +
                ", 核心线程: " + fastPool.getCorePoolSize() + 
                ", 最大线程: " + fastPool.getMaximumPoolSize());
        
        logger.info("Socket池 - 活跃线程: " + socketPool.getActiveCount() + 
                ", 队列大小: " + socketPool.getQueue().size() + "/" + SOCKET_QUEUE_CAPACITY +
                ", 已完成任务: " + socketPool.getCompletedTaskCount() +
                ", 核心线程: " + socketPool.getCorePoolSize() + 
                ", 最大线程: " + socketPool.getMaximumPoolSize());
        
        logger.info("总计 - 活跃任务: " + mgr.activeTasks.get() + 
                ", 已完成任务: " + mgr.completedTasks.get() + 
                ", 被拒绝任务: " + mgr.rejectedTasks.get());
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
            Thread t = new Thread(r, namePrefix + "(id=" + threadNumber.getAndIncrement() + ")");
            t.setPriority(priority);
            t.setDaemon(true);
            return t;
        }
    }

    private class RejectionHandler implements RejectedExecutionHandler {
        @Override
        public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
            rejectedTasks.incrementAndGet();
            logger.error("任务被拒绝执行，活跃线程: " + executor.getActiveCount() +
                    ", 队列大小: " + executor.getQueue().size() +
                    ", 已完成任务: " + executor.getCompletedTaskCount());

            if (!BootMonitor.isZygotePhase()) {
                try {
                    r.run();
                } catch (Throwable e) {
                    logger.error("在调用线程中执行被拒绝的任务失败", e);
                }
            }
        }
    }

    /**
     * 延迟执行 Callable 任务并返回 ScheduledFuture。
     *
     * <p>这个方法允许你在指定的延迟后执行任务并获取结果，
     * 而不需要阻塞当前线程等待。
     *
     * <p>使用示例：
     * <pre>{@code
     * ScheduledFuture<String> future = ThreadPoolManager.scheduleCallable(() -> {
     *     Thread.sleep(1000);
     *     return "完成";
     * }, 500, TimeUnit.MILLISECONDS);
     *
     * // 做其他事情...
     *
     * // 当需要结果时
     * String result = future.get(); // 或者 future.get(1, TimeUnit.SECONDS) 带超时
     * }</pre>
     *
     * @param task  要执行的任务
     * @param delay 延迟时间
     * @param unit  时间单位
     * @param <T>   返回值类型
     * @return ScheduledFuture 可用于获取结果或取消任务
     */
    public static <T> ScheduledFuture<T> scheduleCallable(Callable<T> task, long delay, TimeUnit unit) {
        ThreadPoolManager mgr = getInstance();
        if (mgr == null) {
            return null;
        }
        return mgr.scheduledExecutor.schedule(mgr.wrapTask(task), delay, unit);
    }

    /**
     * 提交一个任务，延迟指定时间后返回预设的结果。
     *
     * <p>这个方法非常适合用于替代 Thread.sleep() 场景，
     * 它不会阻塞调用线程，而是在延迟后异步返回结果。
     *
     * <p>使用示例：
     * <pre>{@code
     * // 替代 Thread.sleep(1000)
     * ScheduledFuture<Void> future = ThreadPoolManager.scheduleDelay(1, TimeUnit.SECONDS);
     *
     * // 继续做其他事情...
     *
     * // 当需要等待延迟完成时
     * future.get(); // 会阻塞直到延迟结束
     * }</pre>
     *
     * @param delay 延迟时间
     * @param unit  时间单位
     * @return ScheduledFuture 在延迟结束后完成
     */
    public static ScheduledFuture<Void> scheduleDelay(long delay, TimeUnit unit) {
        return scheduleCallable(() -> null, delay, unit);
    }

    /**
     * 等待指定时间（什么都不做，只是等待）。
     *
     * <p>这是 scheduleDelay 的别名，语义更清晰。
     * 返回的 Future 在指定时间后完成，调用 get() 会阻塞直到时间到达。
     *
     * <p>使用示例：
     * <pre>{@code
     * // 异步等待1秒
     * ScheduledFuture<Void> wait = ThreadPoolManager.waitFor(1, TimeUnit.SECONDS);
     *
     * // 做其他事情...
     *
     * // 需要等待时
     * wait.get();
     * }</pre>
     *
     * @param duration 等待时长
     * @param unit     时间单位
     * @return ScheduledFuture 在等待时间后完成
     */
    public static ScheduledFuture<Void> waitFor(long duration, TimeUnit unit) {
        return scheduleDelay(duration, unit);
    }

    /**
     * 等待直到条件满足。
     *
     * <p>这个方法会周期性地检查条件，直到条件返回 true 或超时。
     * 返回的 Future 在条件满足或超时后完成。
     *
     * <p>使用示例：
     * <pre>{@code
     * AtomicBoolean ready = new AtomicBoolean(false);
     *
     * // 等待条件满足，最多等10秒，每100毫秒检查一次
     * ScheduledFuture<Boolean> future = ThreadPoolManager.waitUntil(
     *     ready::get,
     *     10, TimeUnit.SECONDS,
     *     100, TimeUnit.MILLISECONDS
     * );
     *
     * // 在其他地方设置条件
     * ready.set(true);
     *
     * // 获取结果：true 表示条件满足，false 表示超时
     * boolean success = future.get();
     * }</pre>
     *
     * @param condition       等待条件，返回 true 时停止等待
     * @param timeout         最大等待时间
     * @param timeoutUnit     超时时间单位
     * @param checkInterval   检查条件的间隔
     * @param checkUnit       检查间隔时间单位
     * @return ScheduledFuture<Boolean> 结果为 true 表示条件满足，false 表示超时
     */
    public static ScheduledFuture<Boolean> waitUntil(
            Supplier<Boolean> condition,
            long timeout,
            TimeUnit timeoutUnit,
            long checkInterval,
            TimeUnit checkUnit) {

        ThreadPoolManager mgr = getInstance();
        if (mgr == null) {
            return null;
        }

        long timeoutMillis = timeoutUnit.toMillis(timeout);
        long checkMillis = checkUnit.toMillis(checkInterval);
        long startTime = System.currentTimeMillis();

        AtomicReference<ScheduledFuture<Boolean>> futureRef = new AtomicReference<>();

        Callable<Boolean> task = () -> {
            if (condition.get()) {
                return true;
            }
            if (System.currentTimeMillis() - startTime >= timeoutMillis) {
                return false;
            }
            ScheduledFuture<Boolean> next = mgr.scheduledExecutor.schedule(
                    mgr.wrapTask(() -> {
                        if (condition.get()) {
                            return true;
                        }
                        if (System.currentTimeMillis() - startTime >= timeoutMillis) {
                            return false;
                        }
                        ScheduledFuture<Boolean> f = futureRef.get();
                        if (f != null && !f.isCancelled()) {
                            futureRef.set(mgr.scheduledExecutor.schedule(
                                    mgr.wrapTask(() -> condition.get() || 
                                            System.currentTimeMillis() - startTime >= timeoutMillis),
                                    checkMillis, TimeUnit.MILLISECONDS));
                        }
                        return condition.get();
                    }),
                    checkMillis, TimeUnit.MILLISECONDS);
            futureRef.set(next);
            return false;
        };

        return mgr.scheduledExecutor.schedule(mgr.wrapTask(task), 0, TimeUnit.MILLISECONDS);
    }

    /**
     * 等待直到条件满足（使用默认检查间隔 100ms）。
     *
     * @param condition   等待条件
     * @param timeout     最大等待时间
     * @param timeoutUnit 超时时间单位
     * @return ScheduledFuture<Boolean> 结果为 true 表示条件满足，false 表示超时
     */
    public static ScheduledFuture<Boolean> waitUntil(
            Supplier<Boolean> condition,
            long timeout,
            TimeUnit timeoutUnit) {
        return waitUntil(condition, timeout, timeoutUnit, 100, TimeUnit.MILLISECONDS);
    }

    /**
     * 提交一个任务，延迟指定时间后执行回调。
     *
     * <p>这个方法允许你在延迟后执行操作，而不需要阻塞当前线程。
     *
     * <p>使用示例：
     * <pre>{@code
     * ThreadPoolManager.scheduleCallback(() -> {
     *     System.out.println("1秒后执行");
     * }, 1, TimeUnit.SECONDS);
     * }</pre>
     *
     * @param callback 延迟后执行的回调
     * @param delay    延迟时间
     * @param unit     时间单位
     * @return ScheduledFuture 可用于取消任务
     */
    public static ScheduledFuture<?> scheduleCallback(Runnable callback, long delay, TimeUnit unit) {
        return schedule(callback, delay, unit);
    }
}
