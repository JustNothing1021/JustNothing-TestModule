package com.justnothing.testmodule.command.framework.protocol;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.justnothing.testmodule.utils.concurrent.ThreadPoolManager;
import com.justnothing.testmodule.utils.logging.Logger;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * JSON-RPC 通道（终端域专用扩展）。
 *
 * <p>在 {@link RpcChannel} 基础上增加终端 I/O 能力（method 见 {@link ProtocolMethods}）：</p>
 * <ul>
 *   <li>{@link ProtocolMethods#TERM_OUTPUT} 通知 → RpcOutputStream 缓冲输出</li>
 *   <li>{@link ProtocolMethods#TERM_INPUT} 通知 → 喂入 Lambda InputStream（inputQueue）</li>
 *   <li>{@link ProtocolMethods#TERM_ENTER_RAW_MODE} / {@link ProtocolMethods#TERM_EXIT_RAW_MODE} /
 *       {@link ProtocolMethods#TERM_QUERY_SIZE} / {@link ProtocolMethods#TERM_SIZE_UPDATE}
 *       （由 RemoteServerTerminal/RemoteClientTerminal 注册）</li>
 * </ul>
 *
 * <p>统一走 {@link InteractiveProtocol#TYPE_RPC} 帧，命令域 / 终端域 / 系统域 method 共用一个通道实例。</p>
 */
public class TerminalRpcChannel extends RpcChannel {

    private static final Logger logger = Logger.getLoggerForName("TerminalRpcChannel");

    // ─── Lambda InputStream 的数据队列 ─────────────────────────────
    private final BlockingQueue<Integer> inputQueue = new LinkedBlockingQueue<>();

    // ─── Lambda OutputStream 实例（由 createOutputStream 创建） ──────
    private RpcOutputStream rpcOutputStream;

    public TerminalRpcChannel(OutputStream socketOutput, Object writeLock) {
        this(socketOutput, writeLock, InteractiveProtocol.TYPE_RPC);
    }

    public TerminalRpcChannel(OutputStream socketOutput, Object writeLock, byte frameType) {
        super(socketOutput, writeLock, frameType);

        // 默认注册 input 通知 → 喂入 inputQueue
        onNotification(ProtocolMethods.TERM_INPUT, params -> {
            if (params != null && params.has("bytes")) {
                JsonArray bytes = params.getAsJsonArray("bytes");
                for (int i = 0; i < bytes.size(); i++) {
                    inputQueue.offer(bytes.get(i).getAsInt());
                }
            }
        });
    }

    // =====================================================================
    // Lambda IOStream factories
    // =====================================================================

    /**
     * 创建 Lambda OutputStream：缓冲写入，flush 时发 "output" 通知。
     *
     * <p>Terminal.writer() 写出的数据经过此流，对 Terminal 完全透明。</p>
     *
     * <p>flush 后不会立即发送：数据先进入 {@link RpcOutputStream} 的合并窗口，
     * 窗口内到达的数据会合成一帧；距上次发送超过空闲阈值时立即发送，
     * 以保持交互式输出零延迟。会话收尾必须调用 {@link RpcOutputStream#flushNow()}
     * 同步排出，避免最后一批输出晚于 cmd.done 到达。</p>
     */
    public OutputStream createOutputStream() {
        if (rpcOutputStream != null) {
            throw new IllegalStateException("OutputStream already created");
        }
        rpcOutputStream = new RpcOutputStream();
        return rpcOutputStream;
    }

    /**
     * 创建 Lambda InputStream：从 inputQueue 读取字节。
     *
     * <p>客户端通过 "input" 通知发来的按键字节会进入 inputQueue，
     * 此流从中读取，对 Terminal 完全透明。</p>
     *
     * <p>注意：必须覆盖 {@code read(byte[], int, int)} 方法，
     * 因为 {@code ExternalTerminal.pump()} 调用 {@code masterInput.read(buf)} 批量读取，
     * 默认实现会逐字节调用 {@code read()} 并阻塞在 {@code inputQueue.take()} 上。</p>
     */
    public InputStream createInputStream() {
        return new InputStream() {
            @Override
            public int read() {
                if (closed) return -1;
                try {
                    return inputQueue.take();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return -1;
                }
            }

            @Override
            public int read(byte[] b, int off, int len) {
                if (closed) return -1;
                if (b == null || len == 0) return 0;

                try {
                    // 先阻塞等待第一个字节
                    Integer first = inputQueue.take();
                    if (first == -1) return -1;
                    b[off] = (byte) (int) first;

                    // 然后用 poll 非阻塞地读取队列中剩余字节
                    int i = 1;
                    while (i < len) {
                        Integer next = inputQueue.poll();
                        if (next == null) break;
                        if (next == -1) break;
                        b[off + i] = (byte) (int) next;
                        i++;
                    }
                    return i;
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return -1;
                }
            }

            @Override
            public int available() {
                return inputQueue.size();
            }
        };
    }

    /**
     * 获取 RpcOutputStream 实例
     */
    public RpcOutputStream getRpcOutputStream() {
        return rpcOutputStream;
    }

    // =====================================================================
    // Lifecycle
    // =====================================================================

    @Override
    public void close() {
        // 先同步排出剩余输出：必须早于 super.close()（后者会把通道置为 closed，
        // 之后 sendNotification 会直接丢弃数据）。
        if (rpcOutputStream != null) {
            try {
                rpcOutputStream.flushNow();
            } catch (Exception ignored) {}
        }
        super.close();
        // 中断 inputQueue 的等待
        inputQueue.offer(-1);
    }

    // =====================================================================
    // Inner class: Lambda OutputStream
    // =====================================================================

    /**
     * Lambda OutputStream：缓冲写入，打包成 "output" RPC 通知发送。
     *
     * <p><b>合并策略（关键性能点）</b></p>
     *
     * <p>JLine 的 FilteringOutputStream 几乎每次 write 之后都会 flush。若每次 flush 都发一帧，
     * 批量输出就退化成"每行一次跨进程帧"：每帧都要 Gson 序列化 + 两把锁 + 信封编码 + socket flush，
     * 对端还要逐帧解析并重绘。实测约 1.6ms/行。</p>
     *
     * <p>因此这里不再立即发送，而是先落入 {@code pending}：</p>
     * <ul>
     *   <li>距上次发送超过 {@link #IDLE_THRESHOLD_NANOS} → 视为空闲/交互式输出，立即发送（零额外延迟）；</li>
     *   <li>否则延迟 {@link #COALESCE_DELAY_MS} 作为合并窗口，窗口内到达的数据合成一帧。</li>
     * </ul>
     *
     * <p>会话收尾必须调用 {@link #flushNow()} 同步排出，否则最后一批输出可能晚于 cmd.done 到达。</p>
     */
    public class RpcOutputStream extends OutputStream {

        private static final int FLUSH_THRESHOLD = 8192;
        /** 连续输出时的合并窗口（毫秒）：取一帧时长，兼顾吞吐与实时感。 */
        private static final long COALESCE_DELAY_MS = 16;
        /** 距上次发送超过该间隔即视为空闲，此时立即发送以保持交互式输出零延迟。 */
        private static final long IDLE_THRESHOLD_NANOS = 10_000_000L;
        /** 待发数据达到该字节数时立即发送，不再等窗口——大输出时贴近原生"批量发送"的行为。 */
        private static final int MAX_PENDING_BYTES = 4096;

        private final ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        private final Object bufferLock = new Object();
        /** 已 flush 但尚未发出、等待合并窗口的数据 */
        private final ByteArrayOutputStream pending = new ByteArrayOutputStream();
        private final Object pendingLock = new Object();
        private boolean flushScheduled = false;
        private long lastEmitNanos = 0L;

        @Override
        public void write(int b) throws IOException {
            if (closed) return;
            boolean shouldFlush;
            synchronized (bufferLock) {
                buffer.write(b);
                shouldFlush = buffer.size() >= FLUSH_THRESHOLD;
            }
            if (shouldFlush) {
                doFlush();
            }
        }

        @Override
        public void write(byte[] b, int off, int len) throws IOException {
            if (closed) return;
            boolean shouldFlush;
            synchronized (bufferLock) {
                buffer.write(b, off, len);
                shouldFlush = buffer.size() >= FLUSH_THRESHOLD;
            }
            if (shouldFlush) {
                doFlush();
            }
        }

        @Override
        public void flush() throws IOException {
            doFlush();
        }

        private void doFlush() {
            byte[] data = drainBuffer();
            if (data == null) return;

            byte[] toEmit = null;
            boolean needSchedule = false;
            synchronized (pendingLock) {
                pending.write(data, 0, data.length);
                if (pending.size() >= MAX_PENDING_BYTES
                        || System.nanoTime() - lastEmitNanos >= IDLE_THRESHOLD_NANOS) {
                    toEmit = takePendingLocked();
                } else if (!flushScheduled) {
                    flushScheduled = true;
                    needSchedule = true;
                }
            }

            if (toEmit != null) {
                emit(toEmit);
            } else if (needSchedule) {
                ScheduledFuture<?> future = ThreadPoolManager.schedule(
                        this::emitPending, COALESCE_DELAY_MS, TimeUnit.MILLISECONDS);
                if (future == null) {
                    // 线程池不可用（Zygote 阶段/已关闭）→ 退化为同步发送，避免数据滞留
                    emitPending();
                }
            }
        }

        /**
         * 取出缓冲区数据；尾部不完整的 UTF-8 序列会被放回缓冲区等待下次拼接。
         *
         * @return 可发送的数据；null 表示无数据（或不完整序列已全部放回）
         */
        private byte[] drainBuffer() {
            byte[] data;
            synchronized (bufferLock) {
                if (buffer.size() == 0) return null;
                data = buffer.toByteArray();
                buffer.reset();
            }
            int incomplete = trailingIncompleteUtf8Bytes(data);
            if (incomplete == 0) return data;

            int completeLen = data.length - incomplete;
            if (completeLen == 0) {
                // 整个缓冲都是不完整序列，放回缓冲区等待更多数据
                synchronized (bufferLock) {
                    buffer.write(data, 0, data.length);
                }
                return null;
            }
            byte[] toSend = new byte[completeLen];
            System.arraycopy(data, 0, toSend, 0, completeLen);
            // 保留残余字节
            synchronized (bufferLock) {
                buffer.write(data, completeLen, incomplete);
            }
            return toSend;
        }

        /** 在 pendingLock 内取出全部待发数据并记录发送时刻。 */
        private byte[] takePendingLocked() {
            if (pending.size() == 0) return null;
            byte[] data = pending.toByteArray();
            pending.reset();
            lastEmitNanos = System.nanoTime();
            return data;
        }

        private void emitPending() {
            byte[] data;
            synchronized (pendingLock) {
                flushScheduled = false;
                data = takePendingLocked();
            }
            if (data != null) {
                emit(data);
            }
        }

        /**
         * 立即（同步）把缓冲区与待发数据全部发出，绕过合并窗口。
         *
         * <p>会话收尾时必须调用：否则最后一批输出可能晚于 cmd.done 到达对端。</p>
         */
        public void flushNow() {
            byte[] drained = drainBuffer();
            byte[] data;
            synchronized (pendingLock) {
                if (drained != null) {
                    pending.write(drained, 0, drained.length);
                }
                data = takePendingLocked();
            }
            if (data != null) {
                emit(data);
            }
        }

        /** 在锁外做实际发送，避免持锁期间做 I/O。 */
        private void emit(byte[] data) {
            JsonObject params = new JsonObject();
            params.addProperty("data", new String(data, StandardCharsets.UTF_8));
            TerminalRpcChannel.this.sendNotification(ProtocolMethods.TERM_OUTPUT, params);
        }

        @Override
        public void close() throws IOException {
            flushNow();
        }
    }

    /**
     * 检查字节数组末尾有多少字节属于不完整的 UTF-8 序列。
     * 用于防止多字节字符在 flush 边界被拆碎产生乱码。
     *
     * @return 末尾不完整序列的字节数（0 表示全部完整）
     */
    static int trailingIncompleteUtf8Bytes(byte[] data) {
        if (data.length == 0) return 0;
        int n = data.length;
        int last = data[n - 1] & 0xFF;
        if (last < 0x80) return 0; // ASCII，完整
        if (last >= 0xF0) return 1; // 4字节序列起始，缺续字节
        if (last >= 0xE0) return 1; // 3字节序列起始，缺续字节
        if (last >= 0xC0) return 1; // 2字节序列起始，缺续字节
        // 末尾是续字节 (0x80-0xBF)：向前查找起始字节
        int pos = n - 1;
        while (pos > 0 && (data[pos] & 0xC0) == 0x80 && (n - pos) < 4) {
            pos--;
        }
        int startByte = data[pos] & 0xFF;
        int expected;
        if (startByte >= 0xF0) expected = 4;
        else if (startByte >= 0xE0) expected = 3;
        else if (startByte >= 0xC0) expected = 2;
        else return 0;
        int actual = n - pos;
        return actual < expected ? actual : 0;
    }
}
