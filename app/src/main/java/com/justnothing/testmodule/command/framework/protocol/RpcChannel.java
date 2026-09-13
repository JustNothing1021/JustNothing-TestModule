package com.justnothing.testmodule.command.framework.protocol;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.justnothing.testmodule.command.framework.utils.GsonFactory;
import com.justnothing.testmodule.utils.logging.Logger;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * 统一 JSON-RPC 通道（逻辑多通道的物理承载）。
 *
 * <p>运行在单一帧类型（如 {@link InteractiveProtocol#TYPE_RPC}）之上，
 * 通过 method 命名空间分区承载多个逻辑通道（cmd.* / term.* / sys.*）。
 * 两端共用此类，天然一致。</p>
 *
 * <p>机制：</p>
 * <ul>
 *   <li>通知：{@link #sendNotification} / {@link #onNotification}（fire-and-forget 事件）</li>
 *   <li>请求：{@link #call} 同步阻塞（CompletableFuture）↔ {@link #onRequest} + {@link #sendResponse}</li>
 *   <li>错误：{@link #sendError} / error 响应自动 completeExceptionally</li>
 * </ul>
 */
public class RpcChannel {

    private static final Logger logger = Logger.getLoggerForName("RpcChannel");

    protected final OutputStream socketOutput;
    protected final Object writeLock;
    private final byte frameType;

    // ─── Notification / Request handler registries ──────────────────
    protected final Map<String, List<Consumer<JsonObject>>> notificationHandlers = new ConcurrentHashMap<>();
    protected final Map<String, BiConsumer<Integer, JsonObject>> requestHandlers = new ConcurrentHashMap<>();

    // ─── Pending RPC calls (id → future) ───────────────────────────
    protected final Map<Integer, CompletableFuture<JsonObject>> pendingCalls = new ConcurrentHashMap<>();
    protected final AtomicInteger nextCallId = new AtomicInteger(0);

    // ─── 关闭状态 ───────────────────────────────────────────────────
    protected volatile boolean closed = false;

    public RpcChannel(OutputStream socketOutput, Object writeLock, byte frameType) {
        this.socketOutput = socketOutput;
        this.writeLock = writeLock;
        this.frameType = frameType;
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
        msg.addProperty("jsonrpc", ProtocolMethods.JSONRPC_VERSION);
        msg.addProperty("method", method);
        if (params != null) msg.add("params", params);
        sendRpcMessage(msg);
    }

    /**
     * 发送请求并等待响应（可带超时）
     *
     * @param method    RPC 方法名
     * @param params    参数
     * @param timeoutMs 超时毫秒；&lt;=0 表示无限等待（客户端断开时 {@link #close()} 会 cancel 唤醒）
     * @return 响应结果，超时/失败/取消返回 null
     */
    public JsonObject call(String method, JsonObject params, long timeoutMs) {
        if (closed) return null;
        int id = nextCallId.incrementAndGet();
        JsonObject msg = new JsonObject();
        msg.addProperty("jsonrpc", ProtocolMethods.JSONRPC_VERSION);
        msg.addProperty("method", method);
        msg.addProperty("id", id);
        if (params != null) msg.add("params", params);

        CompletableFuture<JsonObject> future = new CompletableFuture<>();
        pendingCalls.put(id, future);
        sendRpcMessage(msg);

        try {
            if (timeoutMs > 0) {
                return future.get(timeoutMs, TimeUnit.MILLISECONDS);
            }
            return future.get();
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
     * 发送请求（异步，不阻塞等待响应）。
     *
     * <p>适用于"发出请求后由独立 reader 循环处理响应"的场景（如客户端发 cmd.executeWithResult）。
     * 响应到达时自动完成 future 并从 pendingCalls 移除；调用方可按需 {@code future.whenComplete(...)}
     * 或直接丢弃（仅以 cmd.done 等通知作为完成信号）。</p>
     *
     * @return 请求对应的 future（响应/错误到达时完成）
     */
    public CompletableFuture<JsonObject> sendRequest(String method, JsonObject params) {
        if (closed) {
            CompletableFuture<JsonObject> cancelled = new CompletableFuture<>();
            cancelled.cancel(true);
            return cancelled;
        }
        int id = nextCallId.incrementAndGet();
        JsonObject msg = new JsonObject();
        msg.addProperty("jsonrpc", ProtocolMethods.JSONRPC_VERSION);
        msg.addProperty("method", method);
        msg.addProperty("id", id);
        if (params != null) msg.add("params", params);

        CompletableFuture<JsonObject> future = new CompletableFuture<>();
        pendingCalls.put(id, future);
        sendRpcMessage(msg);
        return future;
    }

    /**
     * 发送响应给请求方
     */
    public void sendResponse(int id, JsonObject result) {
        if (closed) return;
        JsonObject msg = new JsonObject();
        msg.addProperty("jsonrpc", ProtocolMethods.JSONRPC_VERSION);
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
        msg.addProperty("jsonrpc", ProtocolMethods.JSONRPC_VERSION);
        msg.addProperty("id", id);
        JsonObject error = new JsonObject();
        error.addProperty("code", code);
        error.addProperty("message", message);
        msg.add("error", error);
        sendRpcMessage(msg);
    }

    protected void sendRpcMessage(JsonObject msg) {
        String json = GsonFactory.getInstance().toJson(msg);
        sendRawRpcMessage(json, String.valueOf(msg.get("method")));
    }

    /**
     * 发送已构造好的 JSON-RPC 文本（调用方负责保证其为合法 JSON）。
     *
     * <p>用于携带大结果的场景（如 cmd.done）：调用方直接把 Gson 生成的 result JSON
     * 拼进信封，省掉"序列化 → JsonParser 解析成树 → 再序列化"这一趟往返——
     * 结果对象越大，这趟往返越贵。</p>
     */
    public void sendRawMessage(String json) {
        if (closed) return;
        sendRawRpcMessage(json, null);
    }

    private void sendRawRpcMessage(String json, String methodForLog) {
        try {
            byte[] data = json.getBytes(StandardCharsets.UTF_8);
            synchronized (writeLock) {
                InteractiveProtocol.writeMessage(socketOutput, frameType, data);
            }
        } catch (IOException e) {
            logger.error("发送 RPC 消息失败: " + methodForLog, e);
        }
    }

    // =====================================================================
    // Receive RPC messages
    // =====================================================================

    /**
     * 处理收到的 RPC 帧数据（由帧分发循环调用）
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
    // Lifecycle
    // =====================================================================

    public void close() {
        closed = true;
        // 中断所有等待的 call
        for (CompletableFuture<JsonObject> future : pendingCalls.values()) {
            future.cancel(true);
        }
        pendingCalls.clear();
    }
}
