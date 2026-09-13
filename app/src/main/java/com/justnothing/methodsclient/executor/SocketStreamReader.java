package com.justnothing.methodsclient.executor;


import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.justnothing.methodsclient.model.ColoredSegment;

import java.util.ArrayList;
import java.util.List;
import com.justnothing.methodsclient.StreamClient;
import com.justnothing.methodsclient.highlighter.HighlighterManager;
import com.justnothing.methodsclient.renderer.OutputRenderer;
import com.justnothing.methodsclient.utils.TerminalManager;
import com.justnothing.testmodule.command.framework.output.InputMode;
import com.justnothing.testmodule.command.framework.output.Colors;
import com.justnothing.testmodule.command.framework.output.ClientRequirements;
import com.justnothing.testmodule.command.framework.protocol.InteractiveProtocol;
import com.justnothing.testmodule.command.framework.protocol.ProtocolMethods;
import com.justnothing.testmodule.command.framework.protocol.RemoteClientTerminal;
import com.justnothing.testmodule.command.framework.protocol.RpcChannel;
import com.justnothing.testmodule.command.framework.protocol.TerminalRpcChannel;
import com.justnothing.testmodule.utils.concurrent.ThreadPoolManager;
import org.jline.terminal.Terminal;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;


public class SocketStreamReader {

    private static final int SERVER_RESPONSE_TIMEOUT_MS = 30000;
    private static final long PING_SERVER_INTERVAL_MS = 5000;


    private static final StreamClient.ClientLogger logger = new StreamClient.ClientLogger();


    private static boolean isServerTimeout(AtomicLong lastResponseTime) {
        long elapsed = System.currentTimeMillis() - lastResponseTime.get();
        if (elapsed > SERVER_RESPONSE_TIMEOUT_MS) {
            String msg = "服务端响应超时 (" + elapsed + "ms)";
            logger.error(msg);
            System.err.println(msg);
            return true;
        }
        return false;
    }

    // ==================== 客户端能力探测 ====================

    /**
     * 构建客户端能力（终端尺寸 / ANSI / 颜色系统探测）。
     *
     * <p>统一 RPC 路径下，
     * 能力经 {@link ProtocolMethods#SYS_HELLO} RPC 参数发送。</p>
     */
    public static ClientRequirements buildClientRequirements(boolean supportsInput, boolean isJsonMode) {
        ClientRequirements req = new ClientRequirements(supportsInput, isJsonMode);
        // JSON 模式（GUI）没有本地终端：探测一次会触发 TerminalManager 的静态初始化
        // （TerminalBuilder + 从 APK 解压 native 库，实测 ~1.8s），而结果必然是
        // width=0 / height=0 / supportsAnsi=false。直接跳过这次探测。
        if (!isJsonMode) {
            Terminal terminal = TerminalManager.getTerminal();
            if (terminal != null) {
                try {
                    org.jline.terminal.Size size = terminal.getSize();
                    if (size != null && size.getColumns() > 0) {
                        req.setWidth(size.getColumns());
                        req.setHeight(size.getRows());
                    }
                } catch (Exception ignored) {}
                req.setSupportsAnsi(!Terminal.TYPE_DUMB.equals(terminal.getType()));
            }
        }
        String colorTerm = System.getenv("COLORTERM");
        String term = System.getenv("TERM");
        if ("truecolor".equals(colorTerm) || "24bit".equals(colorTerm)) {
            req.setColorSystem(ClientRequirements.COLOR_TRUECOLOR);
        } else if (term != null && term.contains("256color")) {
            req.setColorSystem(ClientRequirements.COLOR_EIGHT_BIT);
        } else {
            req.setColorSystem(ClientRequirements.COLOR_STANDARD);
        }
        return req;
    }

    private static void startPingThread(RpcChannel rpcChannel, AtomicBoolean reading) {
        // 保活：只要会话未结束就一直周期性发 sys.ping（服务端回 sys.pong 维持 lastResponseTime）。
        // 注意：停止条件不能包含 lastResponseTime 新鲜度——否则空闲期（如等用户输入）会停止发 ping，
        // 30s 后误判"服务端响应超时"。停止只应发生在读取循环退出后。
        ThreadPoolManager.scheduleWithFixedDelayUntil(
            () -> {
                rpcChannel.sendNotification(ProtocolMethods.SYS_PING, null);
                logger.debug("向服务端发送 sys.ping");
            },
            PING_SERVER_INTERVAL_MS, PING_SERVER_INTERVAL_MS, TimeUnit.MILLISECONDS,
            () -> Thread.currentThread().isInterrupted() || !reading.get()
        );
    }

    // ==================== 统一 RPC 信封读取 ====================

    /**
     * 统一 RPC 信封读取循环（TYPE_RPC 单帧，逻辑多通道 cmd.* / term.* / sys.*）。
     *
     * <p>流程：建 TYPE_RPC 通道 → 注册 Listener（cmd.output / cmd.done / cmd.prompt /
     * term.highlight / sys.pong，term.* 由 RemoteClientTerminal 注册）→ 发 sys.hello 握手
     * → 发 cmd.executeWithResult → 循环读帧直到 cmd.done。</p>
     *
     * @param input        输入流
     * @param output       输出流
     * @param reading      读取标志
     * @param bytesRead    字节计数
     * @param socket       连接 Socket
     * @param requirements 客户端能力（经 sys.hello 发送）
     * @param command      纯命令串（与 requestJson 二选一）
     * @param requestJson  JSON 命令请求（与 command 二选一）
     * @param renderer     输出渲染器（cmd.output → onOutput，cmd.done 结果 → onResult/onDone）
     */
    public static boolean readRpcStream(InputStream input, OutputStream output,
                                        AtomicBoolean reading, AtomicLong bytesRead,
                                        Socket socket, ClientRequirements requirements,
                                        String command, String requestJson,
                                        OutputRenderer renderer) {
        try {
            AtomicLong lastResponseTime = new AtomicLong(System.currentTimeMillis());
            Object writeLock = new Object();

            // 统一 RPC 信封通道
            TerminalRpcChannel rpcChannel = new TerminalRpcChannel(output, writeLock, InteractiveProtocol.TYPE_RPC);
            // term.* 是"本地终端"能力，JSON 客户端（GUI）没有本地终端：
            // 这里取终端会触发 TerminalManager 静态初始化（实测 ~1.8s），且 term.* 对 GUI 无意义。
            if (requirements == null || !requirements.isJsonMode()) {
                Terminal term = TerminalManager.getTerminal();
                if (term != null) {
                    new RemoteClientTerminal(term, rpcChannel); // term.* 处理器
                }
            }

            AtomicBoolean commandDone = new AtomicBoolean(false);

            // cmd.output → 流式输出交给渲染器
            rpcChannel.onNotification(ProtocolMethods.CMD_OUTPUT, params -> {
                if (params == null) return;
                // 服务端会把连续输出合并成一帧的 segments；单段仍是旧的 {data,color} 形式。
                if (params.has("segments") && params.get("segments").isJsonArray()) {
                    List<ColoredSegment> batch = new ArrayList<>();
                    for (JsonElement element : params.getAsJsonArray("segments")) {
                        JsonObject segment = element.getAsJsonObject();
                        String segText = segment.has("data") && !segment.get("data").isJsonNull()
                                ? segment.get("data").getAsString() : "";
                        byte segColor = segment.has("color")
                                ? (byte) segment.get("color").getAsInt() : Colors.DEFAULT;
                        batch.add(new ColoredSegment(segColor, segText));
                        bytesRead.addAndGet(segText.getBytes(StandardCharsets.UTF_8).length);
                    }
                    if (renderer != null) {
                        renderer.onOutputBatch(batch);
                    }
                    return;
                }
                String data = params.has("data") && !params.get("data").isJsonNull()
                        ? params.get("data").getAsString() : "";
                byte color = params.has("color") ? (byte) params.get("color").getAsInt() : Colors.DEFAULT;
                if (renderer != null) {
                    renderer.onOutput(color, data);
                }
                bytesRead.addAndGet(data.getBytes(StandardCharsets.UTF_8).length);
            });

            // cmd.done → 命令结束（携带结构化结果 → onResult，随后 onDone）
            rpcChannel.onNotification(ProtocolMethods.CMD_DONE, params -> {
                logger.info("收到 cmd.done，命令执行完成");
                commandDone.set(true);
                if (renderer != null) {
                    if (params != null && params.has("result") && !params.get("result").isJsonNull()) {
                        renderer.onResult(params.get("result").toString());
                    }
                    renderer.onDone();
                }
            });

            // sys.ping → sys.pong（RPC 层活性）
            rpcChannel.onNotification(ProtocolMethods.SYS_PING,
                    p -> rpcChannel.sendNotification(ProtocolMethods.SYS_PONG, null));

            // sys.pong（仅日志）
            rpcChannel.onNotification(ProtocolMethods.SYS_PONG,
                    p -> logger.debug("收到了服务端的 sys.pong"));

            // term.highlight → 高亮模式切换
            rpcChannel.onNotification(ProtocolMethods.TERM_HIGHLIGHT, params -> {
                if (params == null || !params.has("mode")) return;
                String modeName = params.get("mode").getAsString();
                boolean success = HighlighterManager.switchMode(modeName);
                if (success) {
                    logger.info("高亮模式已切换: " + modeName + " (" + InputMode.getDescription(modeName) + ")");
                } else {
                    logger.warn("未知的高亮模式: " + modeName +
                            " (预定义模式: " + String.join(", ", InputMode.allModes()) + ")");
                }
            });

            // cmd.prompt → 本地渲染输入（submitFastRunnable，不占 reader 循环）
            rpcChannel.onRequest(ProtocolMethods.CMD_PROMPT, (id, params) -> {
                ThreadPoolManager.submitFastRunnable(() -> {
                    JsonObject response = new JsonObject();
                    try {
                        String type = params != null && params.has("type") ? params.get("type").getAsString() : "input";
                        String title = params != null && params.has("title") && !params.get("title").isJsonNull()
                                ? params.get("title").getAsString() : "";
                        String defaultValue = params != null && params.has("defaultValue") && !params.get("defaultValue").isJsonNull()
                                ? params.get("defaultValue").getAsString() : null;

                        Terminal t = TerminalManager.getTerminal();
                        if (t != null) {
                            t.writer().flush();
                        } else {
                            System.out.flush();
                        }

                        if ("confirm".equals(type)) {
                            boolean def = defaultValue != null && "true".equalsIgnoreCase(defaultValue);
                            String userInput = TerminalManager.readLine(title + (def ? " (Y/n): " : " (y/N): "));
                            boolean confirmed = userInput == null || userInput.trim().isEmpty()
                                    ? def : "y".equalsIgnoreCase(userInput.trim()) || "yes".equalsIgnoreCase(userInput.trim());
                            response.addProperty("value", confirmed);
                        } else {
                            boolean isPassword = "password".equals(type);
                            String userInput = isPassword
                                    ? TerminalManager.readLine(title, '*')
                                    : TerminalManager.readLine(title);
                            if (userInput != null) {
                                response.addProperty("value", userInput);
                            } else {
                                response.addProperty("cancelled", true);
                            }
                        }
                    } catch (Exception e) {
                        logger.error("处理 cmd.prompt 出错: " + id, e);
                        response.addProperty("cancelled", true);
                    }
                    rpcChannel.sendResponse(id, response);
                });
            });

            // sys.hello 握手：能力协商进 RPC（服务端逐帧有序处理，无需阻塞等待响应）
            rpcChannel.sendRequest(ProtocolMethods.SYS_HELLO, ClientRequirements.toRpcParams(requirements))
                    .whenComplete((resp, err) -> {
                        if (err != null) {
                            logger.warn("sys.hello 握手失败: " + err.getMessage());
                        }
                    });

            // 发送 cmd.executeWithResult（异步 request；服务器回执 ack，完成信号是 cmd.done）
            if (command != null && !command.trim().isEmpty()) {
                JsonObject params = new JsonObject();
                params.addProperty("command", command);
                rpcChannel.sendRequest(ProtocolMethods.CMD_EXECUTE, params);
            } else if (requestJson != null && !requestJson.trim().isEmpty()) {
                JsonObject params = new JsonObject();
                params.addProperty("request", requestJson);
                rpcChannel.sendRequest(ProtocolMethods.CMD_EXECUTE, params);
            } else {
                logger.warn("readRpcStream: command 与 requestJson 均为空");
                return false;
            }
            logger.info("已发送 cmd.executeWithResult: " + (command != null ? command : requestJson));

            startPingThread(rpcChannel, reading);

            // 读取循环：仅 TYPE_RPC 帧
            while (reading.get() && !commandDone.get() && !Thread.currentThread().isInterrupted()) {
                try {
                    socket.setSoTimeout(1000);
                    Object[] packet = InteractiveProtocol.readMessage(input);

                    if (packet == null) {
                        logger.info("服务器已关闭连接");
                        return true;
                    }

                    if (isServerTimeout(lastResponseTime)) {
                        return false;
                    }

                    byte type = (byte) packet[0];
                    byte[] data = (byte[]) packet[1];
                    lastResponseTime.set(System.currentTimeMillis());

                    switch (type) {
                        case InteractiveProtocol.TYPE_RPC:
                            rpcChannel.handleMessage(data);
                            break;

                        default:
                            logger.warn("未知的消息类型: " + type);
                            break;
                    }
                } catch (SocketTimeoutException e) {
                    if (isServerTimeout(lastResponseTime)) {
                        return false;
                    }
                } catch (IOException e) {
                    if (e.getMessage() != null && e.getMessage().contains("Connection reset")) {
                        logger.info("连接被重置");
                        return true;
                    }
                    throw e;
                }
            }
            return true;

        } catch (IOException e) {
            logger.error("读取流失败", e);
            return false;
        } finally {
            reading.set(false);
        }
    }

}
