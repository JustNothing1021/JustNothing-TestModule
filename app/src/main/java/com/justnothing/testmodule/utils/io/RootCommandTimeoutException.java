package com.justnothing.testmodule.utils.io;

import java.io.IOException;

/**
 * Root 命令执行超时。
 *
 * <p>超时与「线程被中断」是两件完全不同的事：这里的超时是 {@link RootProcessPool}
 * 自己判定出来的，调用线程并没有被任何人中断。</p>
 *
 * <p>旧实现用 {@code InterruptedException} 表达超时，调用方（{@link RootShellExecutor}）
 * 又按「真中断」的惯例把中断标志重新设置到当前线程上，结果是：一次超时之后，
 * 这条线程上的每一次阻塞调用（{@code BlockingQueue.poll}、{@code Thread.sleep}…）
 * 都会立刻失败，连同一个 binder 线程被归还给线程池后还会继续污染后续请求。
 * 所以超时必须是一个独立的异常类型。</p>
 */
public class RootCommandTimeoutException extends IOException {

    private final long timeoutMs;

    public RootCommandTimeoutException(String message, long timeoutMs) {
        super(message);
        this.timeoutMs = timeoutMs;
    }

    /** 触发本次超时的超时阈值（毫秒）。 */
    public long getTimeoutMs() {
        return timeoutMs;
    }
}
