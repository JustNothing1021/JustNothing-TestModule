package com.justnothing.testmodule.command.protocol;

import com.justnothing.testmodule.command.base.protocol.GsonFactory;
import com.justnothing.testmodule.utils.logging.Logger;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * JSON-RPC 通道，运行在 InteractiveProtocol TYPE_TERMINAL_RPC 帧之上。
 *
 * <p>所有 Terminal 相关的通信（输出、输入、raw mode 切换、尺寸查询等）
 * 统一走这一个消息类型，通过 JSON-RPC method 分发，无需为每个功能新增协议类型。</p>
 *
 * <h3>RPC Methods 约定：</h3>
 * <table>
 *   <tr><th>Method</th><th>Direction</th><th>Description</th></tr>
 *   <tr><td>output</td><td>S→C</td><td>通知：终端输出数据</td></tr>
 *   <tr><td>input</td><td>C→S</td><td>通知：终端输入字节</td></tr>
 *   <tr><td>sizeUpdate</td><td>C→S</td><td>通知：终端尺寸变更</td></tr>
 *   <tr><td>querySize</td><td>S→C</td><td>请求：查询终端尺寸</td></tr>
 *   <tr><td>enterRawMode</td><td>S→C</td><td>通知：进入 raw mode</td></tr>
 *   <tr><td>exitRawMode</td><td>S→C</td><td>通知：退出 raw mode</td></tr>
 *   <tr><td>promptRequest</td><td>S→C</td><td>请求：交互式提示</td></tr>
 * </table>
 */
public class TerminalRpcChannel {

    private static final Logger logger = Logger.getLoggerForName("TerminalRpcChannel");

    private final OutputStream socketOutput;
    private final Object writeLock;

    // ─── Notification / Request handler registries ──────────────────
    private final Map<String, List<Consumer<JsonObject>>> notificationHandlers = new ConcurrentHashMap<>();
    private final Map<String, BiConsumer<Integer, JsonObject>> requestHandlers = new ConcurrentHashMap<>();

    // ─── Pending RPC calls (id → future) ───────────────────────────
    private final Map<Integer, CompletableFuture<JsonObject>> pendingCalls = new ConcurrentHashMap<>();
    private final AtomicInteger nextCallId = new AtomicInteger(0);

    // ─── Lambda InputStream 的数据队列 ─────────────────────────────
    private final BlockingQueue<Integer> inputQueue = new LinkedBlockingQueue<>();

    // ─── Lambda OutputStream 实例（由 createOutputStream 创建） ──────
    private RpcOutputStream rpcOutputStream;

    // ─── 关闭状态 ───────────────────────────────────────────────────
    private volatile boolean closed = false;

    // =====================================================================
    // Constructor
    // =====================================================================

    public TerminalRpcChannel(OutputStream socketOutput, Object writeLock) {
        this.socketOutput = socketOutput;
        this.writeLock = writeLock;

        // 默认注册 "input" 通知 → 喂入 inputQueue
        onNotification("input", params -> {
            if (params != null && params.has("bytes")) {
                JsonArray bytes = params.getAsJsonArray("bytes");
                for (int i = 0; i < bytes.size(); i++) {
                    inputQueue.offer(bytes.get(i).getAsInt());
                }
            }
        });
    }

    // =====================================================================
    // Send RPC messages
    // =====================================================================

    /**
     * 发送通知（不期望响应）
     */
    public void sendNotification(String method, JsonObject params) {
        if (closed) return;
        JsonObject msg = new JsonObject();
        msg.addProperty("jsonrpc", "2.0");
        msg.addProperty("method", method);
        if (params != null) msg.add("params", params);
        sendRpcMessage(msg);
    }

    /**
     * 发送请求并等待响应（带超时）
     *
     * @param method    RPC 方法名
     * @param params    参数
     * @param timeoutMs 超时毫秒
     * @return 响应结果，超时或失败返回 null
     */
    public JsonObject call(String method, JsonObject params, long timeoutMs) {
        if (closed) return null;
        int id = nextCallId.incrementAndGet();
        JsonObject msg = new JsonObject();
        msg.addProperty("jsonrpc", "2.0");
        msg.addProperty("method", method);
        msg.addProperty("id", id);
        if (params != null) msg.add("params", params);

        CompletableFuture<JsonObject> future = new CompletableFuture<>();
        pendingCalls.put(id, future);
        sendRpcMessage(msg);

        try {
            return future.get(timeoutMs, TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            logger.warn("RPC call 超时: " + method + " (id=" + id + ", " + timeoutMs + "ms)");
        } catch (Exception e) {
            logger.error("RPC call 失败: " + method, e);
        } finally {
            pendingCalls.remove(id);
        }
        return null;
    }

    /**
     * 发送响应给请求方
     */
    public void sendResponse(int id, JsonObject result) {
        if (closed) return;
        JsonObject msg = new JsonObject();
        msg.addProperty("jsonrpc", "2.0");
        msg.addProperty("id", id);
        msg.add("result", result != null ? result : new JsonObject());
        sendRpcMessage(msg);
    }

    /**
     * 发送错误响应
     */
    public void sendError(int id, int code, String message) {
        if (closed) return;
        JsonObject msg = new JsonObject();
        msg.addProperty("jsonrpc", "2.0");
        msg.addProperty("id", id);
        JsonObject error = new JsonObject();
        error.addProperty("code", code);
        error.addProperty("message", message);
        msg.add("error", error);
        sendRpcMessage(msg);
    }

    private void sendRpcMessage(JsonObject msg) {
        try {
            byte[] data = GsonFactory.getInstance().toJson(msg).getBytes(StandardCharsets.UTF_8);
            synchronized (writeLock) {
                InteractiveProtocol.writeMessage(socketOutput,
                        InteractiveProtocol.TYPE_TERMINAL_RPC, data);
            }
        } catch (IOException e) {
            logger.error("发送 RPC 消息失败: " + msg.get("method"), e);
        }
    }

    // =====================================================================
    // Receive RPC messages
    // =====================================================================

    /**
     * 处理收到的 TYPE_TERMINAL_RPC 帧数据（由 SocketClientHandler / SocketStreamReader 调用）
     */
    public void handleMessage(byte[] data) {
        if (data == null || data.length == 0) return;
        try {
            JsonObject msg = JsonParser.parseString(new String(data, StandardCharsets.UTF_8)).getAsJsonObject();

            if (msg.has("method")) {
                String method = msg.get("method").getAsString();
                JsonObject params = msg.has("params") ? msg.getAsJsonObject("params") : null;

                if (msg.has("id")) {
                    // 这是请求（需要响应）
                    int id = msg.get("id").getAsInt();
                    BiConsumer<Integer, JsonObject> handler = requestHandlers.get(method);
                    if (handler != null) {
                        handler.accept(id, params);
                    } else {
                        sendError(id, -32601, "Method not found: " + method);
                    }
                } else {
                    // 这是通知（无需响应）
                    List<Consumer<JsonObject>> handlers = notificationHandlers.get(method);
                    if (handlers != null) {
                        for (Consumer<JsonObject> h : handlers) {
                            h.accept(params);
                        }
                    }
                }
            } else if (msg.has("result")) {
                // 这是响应
                int id = msg.get("id").getAsInt();
                CompletableFuture<JsonObject> future = pendingCalls.remove(id);
                if (future != null) {
                    future.complete(msg.getAsJsonObject("result"));
                }
            } else if (msg.has("error")) {
                // 这是错误响应
                int id = msg.get("id").getAsInt();
                CompletableFuture<JsonObject> future = pendingCalls.remove(id);
                if (future != null) {
                    future.completeExceptionally(new RuntimeException(
                            msg.getAsJsonObject("error").toString()));
                }
            }
        } catch (Exception e) {
            logger.error("处理 RPC 消息失败", e);
        }
    }

    // =====================================================================
    // Handler registration
    // =====================================================================

    public void onNotification(String method, Consumer<JsonObject> handler) {
        notificationHandlers.computeIfAbsent(method, k -> new CopyOnWriteArrayList<>()).add(handler);
    }

    public void onRequest(String method, BiConsumer<Integer, JsonObject> handler) {
        requestHandlers.put(method, handler);
    }

    // =====================================================================
    // Lambda IOStream factories
    // =====================================================================

    /**
     * 创建 Lambda OutputStream：缓冲写入，flush 时发 "output" 通知。
     *
     * <p>Terminal.writer() 写出的数据经过此流，对 Terminal 完全透明。</p>
     *
     * <p>不使用自动 flush 定时器，因为 JLine 的 FilteringOutputStream
     * 会对每个字节单独调用 write(int b)，定时器可能在两次 write 之间
     * 把部分 ANSI 转义序列单独 flush 出去，导致乱码。
     * FilteringOutputStream 在每个 write() 结束后会调用 flush()，
     * 因此数据不会积压。</p>
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
     * 获取 RpcOutputStream 实例（用于设置 autoFlush 等选项）
     */
    public RpcOutputStream getRpcOutputStream() {
        return rpcOutputStream;
    }

    /**
     * 直接向 inputQueue 喂入字节（供兼容旧协议的桥接使用）
     */
    public void feedInput(byte[] data) {
        if (data != null) {
            for (byte b : data) {
                inputQueue.offer((int) b & 0xFF);
            }
        }
    }

    // =====================================================================
    // Lifecycle
    // =====================================================================

    public void close() {
        closed = true;
        // 最后一次 flush
        if (rpcOutputStream != null) {
            try {
                rpcOutputStream.flush();
            } catch (IOException ignored) {}
        }
        // 中断所有等待的 call
        for (CompletableFuture<JsonObject> future : pendingCalls.values()) {
            future.cancel(true);
        }
        pendingCalls.clear();
        // 中断 inputQueue 的等待
        inputQueue.offer(-1);
    }

    public boolean isClosed() {
        return closed;
    }

    // =====================================================================
    // Inner class: Lambda OutputStream
    // =====================================================================

    /**
     * Lambda OutputStream：缓冲写入，flush 时打包成 "output" RPC 通知发送。
     *
     * <p>所有 buffer 操作（write / flush）通过 bufferLock 互斥，
     * 防止自动 flush 定时器与写入线程并发时丢失字节（特别是 ANSI 转义序列被拆碎）。</p>
     */
    public class RpcOutputStream extends OutputStream {

        private static final int FLUSH_THRESHOLD = 8192;
        private final ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        private final Object bufferLock = new Object();
        private volatile boolean autoFlushEveryWrite = false;

        @Override
        public void write(int b) throws IOException {
            if (closed) return;
            boolean shouldFlush;
            synchronized (bufferLock) {
                buffer.write(b);
                shouldFlush = autoFlushEveryWrite || buffer.size() >= FLUSH_THRESHOLD;
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
                shouldFlush = autoFlushEveryWrite || buffer.size() >= FLUSH_THRESHOLD;
            }
            if (shouldFlush) {
                doFlush();
            }
        }

        @Override
        public void flush() throws IOException {
            doFlush();
        }

        private void doFlush() throws IOException {
            byte[] data;
            synchronized (bufferLock) {
                if (closed || buffer.size() == 0) return;
                data = buffer.toByteArray();
                buffer.reset();
            }
            // 在锁外发送，避免持锁期间做 I/O
            JsonObject params = new JsonObject();
            params.addProperty("data", new String(data, StandardCharsets.UTF_8));
            TerminalRpcChannel.this.sendNotification("output", params);
        }

        @Override
        public void close() throws IOException {
            flush();
        }

        /**
         * 设置是否每次 write 都立即 flush（极端实时场景）。
         * 默认 false：缓冲到显式 flush()。
         */
        public void setAutoFlushEveryWrite(boolean auto) {
            this.autoFlushEveryWrite = auto;
        }

        public boolean isAutoFlushEveryWrite() {
            return autoFlushEveryWrite;
        }
    }
}
