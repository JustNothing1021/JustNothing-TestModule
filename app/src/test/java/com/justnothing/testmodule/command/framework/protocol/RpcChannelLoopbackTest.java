package com.justnothing.testmodule.command.framework.protocol;

import com.google.gson.JsonObject;

import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * {@link RpcChannel} 内存回环单测（纯 Java，无需 Android 运行时）。
 *
 * <p>用 PipedInputStream/PipedOutputStream 直连两端，模拟真实 Socket 的帧流动，
 * 覆盖逻辑多通道 RPC 的四个核心场景：</p>
 * <ul>
 *   <li>通知投递（fire-and-forget 事件）</li>
 *   <li>请求-响应（同步 call ↔ onRequest + sendResponse）</li>
 *   <li>错误响应（sendError → future 异常完成）</li>
 *   <li>调用超时（无响应方 → call 返回 null）</li>
 *   <li>未知方法（-32601 Method not found）</li>
 *   <li>信封帧类型（统一走 TYPE_RPC 0x21）</li>
 * </ul>
 */
public class RpcChannelLoopbackTest {

    /** 回环连接：client/server 两端经两条管道直连 */
    public static final class Loopback {
        public final RpcChannel client;
        public final RpcChannel server;
        private final PipedOutputStream clientOut;
        private final PipedOutputStream serverOut;
        private final Thread relayClientToServer;
        private final Thread relayServerToClient;

        Loopback(RpcChannel client, RpcChannel server,
                 PipedOutputStream clientOut, PipedOutputStream serverOut,
                 Thread relayClientToServer, Thread relayServerToClient) {
            this.client = client;
            this.server = server;
            this.clientOut = clientOut;
            this.serverOut = serverOut;
            this.relayClientToServer = relayClientToServer;
            this.relayServerToClient = relayServerToClient;
        }

        /** 关闭两端并断开管道，让 relay 线程退出 */
        public void close() throws IOException {
            client.close();
            server.close();
            clientOut.close();
            serverOut.close();
            relayClientToServer.interrupt();
            relayServerToClient.interrupt();
        }
    }

    /** 建立 client ↔ server 内存直连 */
    public static Loopback connect() throws IOException {
        PipedOutputStream clientOut = new PipedOutputStream();
        PipedInputStream clientIn = new PipedInputStream(clientOut, 65536);
        PipedOutputStream serverOut = new PipedOutputStream();
        PipedInputStream serverIn = new PipedInputStream(serverOut, 65536);

        RpcChannel client = new RpcChannel(clientOut, new Object(), InteractiveProtocol.TYPE_RPC);
        RpcChannel server = new RpcChannel(serverOut, new Object(), InteractiveProtocol.TYPE_RPC);

        Thread relayC2S = relayThread(clientIn, server);
        Thread relayS2C = relayThread(serverIn, client);
        return new Loopback(client, server, clientOut, serverOut, relayC2S, relayS2C);
    }

    /** 等待条件成立（最长 5 秒） */
    public static void await(Supplier<Boolean> condition) throws InterruptedException {
        long deadline = System.currentTimeMillis() + 5000;
        while (System.currentTimeMillis() < deadline) {
            if (Boolean.TRUE.equals(condition.get())) {
                return;
            }
            Thread.sleep(10);
        }
        fail("等待条件超时");
    }

    private static Thread relayThread(InputStream in, RpcChannel target) {
        Thread t = new Thread(() -> {
            try {
                while (true) {
                    Object[] packet = InteractiveProtocol.readMessage(in);
                    if (packet == null) {
                        break;
                    }
                    byte type = (byte) packet[0];
                    if (type == InteractiveProtocol.TYPE_RPC) {
                        target.handleMessage((byte[]) packet[1]);
                    }
                }
            } catch (IOException ignored) {
                // 管道关闭是正常的退出路径
            }
        }, "RpcLoopback-relay");
        t.setDaemon(true);
        t.start();
        return t;
    }

    // ==================== 通知投递 ====================

    @Test
    public void testNotificationDelivery() throws Exception {
        Loopback lb = connect();
        try {
            AtomicReference<JsonObject> received = new AtomicReference<>();
            lb.client.onNotification(ProtocolMethods.CMD_OUTPUT, received::set);

            JsonObject params = new JsonObject();
            params.addProperty("data", "hello");
            params.addProperty("color", 10);
            lb.server.sendNotification(ProtocolMethods.CMD_OUTPUT, params);

            await(() -> received.get() != null);
            assertNotNull(received.get());
            assertEquals("hello", received.get().get("data").getAsString());
            assertEquals(10, received.get().get("color").getAsInt());
        } finally {
            lb.close();
        }
    }

    // ==================== 请求-响应 ====================

    @Test
    public void testRequestResponse() throws Exception {
        Loopback lb = connect();
        try {
            lb.server.onRequest(ProtocolMethods.CMD_PROMPT, (id, params) -> {
                JsonObject result = new JsonObject();
                result.addProperty("value", "typed-input");
                lb.server.sendResponse(id, result);
            });

            JsonObject params = new JsonObject();
            params.addProperty("type", "input");
            params.addProperty("title", "Name:");
            JsonObject result = lb.client.call(ProtocolMethods.CMD_PROMPT, params, 5000);

            assertNotNull("call 应同步收到响应", result);
            assertEquals("typed-input", result.get("value").getAsString());
        } finally {
            lb.close();
        }
    }

    @Test
    public void testSendRequestAsync() throws Exception {
        Loopback lb = connect();
        try {
            lb.server.onRequest("cmd.exec", (id, params) -> {
                JsonObject ack = new JsonObject();
                ack.addProperty("accepted", true);
                lb.server.sendResponse(id, ack);
            });

            CompletableFuture<JsonObject> future = lb.client.sendRequest("cmd.exec", null);
            JsonObject ack = future.get(5000, TimeUnit.MILLISECONDS);
            assertNotNull(ack);
            assertTrue("ack 应包含 accepted", ack.get("accepted").getAsBoolean());
        } finally {
            lb.close();
        }
    }

    // ==================== 错误响应 ====================

    @Test
    public void testErrorResponseCompletesExceptionally() throws Exception {
        Loopback lb = connect();
        try {
            lb.server.onRequest("cmd.fail", (id, params) ->
                    lb.server.sendError(id, -32000, "boom"));

            CompletableFuture<JsonObject> future = lb.client.sendRequest("cmd.fail", null);
            try {
                future.get(5000, TimeUnit.MILLISECONDS);
                fail("错误响应应让 future 异常完成");
            } catch (Exception e) {
                assertTrue("异常应携带服务端错误信息", e.getMessage().contains("boom"));
            }
        } finally {
            lb.close();
        }
    }

    // ==================== 超时 ====================

    @Test
    public void testCallTimeout() throws Exception {
        Loopback lb = connect();
        try {
            // 已注册 handler 但故意不响应 → call 应超时
            lb.server.onRequest("cmd.silent", (id, params) -> { /* 不响应 */ });

            long start = System.currentTimeMillis();
            JsonObject result = lb.client.call("cmd.silent", null, 200);
            long elapsed = System.currentTimeMillis() - start;

            assertNull("超时应返回 null", result);
            assertTrue("耗时应接近超时阈值（>=150ms），实际 " + elapsed + "ms", elapsed >= 150);
        } finally {
            lb.close();
        }
    }

    // ==================== 未知方法 ====================

    @Test
    public void testMethodNotFound() throws Exception {
        Loopback lb = connect();
        try {
            CompletableFuture<JsonObject> future = lb.client.sendRequest("cmd.unknown", null);
            try {
                future.get(5000, TimeUnit.MILLISECONDS);
                fail("未注册方法应返回 -32601 Method not found");
            } catch (Exception e) {
                assertTrue("异常应包含 -32601", e.getMessage().contains("-32601"));
            }
        } finally {
            lb.close();
        }
    }

    // ==================== 信封帧类型 ====================

    @Test
    public void testEnvelopeUsesTypeRpc() throws Exception {
        ByteArrayOutputStream captured = new ByteArrayOutputStream();
        RpcChannel channel = new RpcChannel(captured, new Object(), InteractiveProtocol.TYPE_RPC);
        try {
            JsonObject params = new JsonObject();
            params.addProperty("data", "x");
            channel.sendNotification(ProtocolMethods.CMD_OUTPUT, params);

            Object[] packet = InteractiveProtocol.decodeMessage(captured.toByteArray());
            assertNotNull(packet);
            assertEquals("RPC 信封应统一走 TYPE_RPC(0x21)",
                    (byte) InteractiveProtocol.TYPE_RPC, (byte) packet[0]);
        } finally {
            channel.close();
        }
    }
}
