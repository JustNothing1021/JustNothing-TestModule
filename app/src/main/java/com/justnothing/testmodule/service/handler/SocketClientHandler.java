package com.justnothing.testmodule.service.handler;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.justnothing.testmodule.command.framework.CommandExecutor;
import com.justnothing.testmodule.command.framework.CommandType;
import com.justnothing.testmodule.command.framework.model.CommandResult;
import com.justnothing.testmodule.command.framework.model.CommandRouter;
import com.justnothing.testmodule.command.framework.model.CommandRequest;
import com.justnothing.testmodule.command.framework.output.ClientRequirements;
import com.justnothing.testmodule.command.framework.output.InteractiveOutputHandler;
import com.justnothing.testmodule.command.framework.protocol.InteractiveProtocol;
import com.justnothing.testmodule.command.framework.protocol.ProtocolMethods;
import com.justnothing.testmodule.command.framework.protocol.TerminalRpcChannel;
import com.justnothing.testmodule.utils.logging.Logger;
import com.justnothing.testmodule.utils.concurrent.ThreadPoolManager;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PushbackInputStream;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

public class SocketClientHandler {
    private static final Logger logger = Logger.getLoggerForName("SocketClientHandler");

    public static final int CLIENT_CONNECT_SOCKET_TIMEOUT_MS = 5000;
    public static final int INTERACTIVE_PROTOCOL_SOCKET_TIMEOUT_MS = 10000;
    public static final int INTERACTIVE_PROTOCOL_REQUEST_TIMEOUT_MS = 30000;

    private final CommandExecutor commandExecutor;

    public SocketClientHandler(CommandExecutor commandExecutor) {
        this.commandExecutor = commandExecutor;
    }

    public void handleClient(Socket clientSocket) {
        ThreadPoolManager.submitSocketRunnable(() -> {
            try {
                InputStream input = clientSocket.getInputStream();
                OutputStream output = clientSocket.getOutputStream();

                clientSocket.setSoTimeout(CLIENT_CONNECT_SOCKET_TIMEOUT_MS);

                int firstByte = input.read();
                if (firstByte == -1) {
                    clientSocket.close();
                    return;
                }

                PushbackInputStream pushbackInput = new PushbackInputStream(input, 1);
                pushbackInput.unread(firstByte);

                if (firstByte != InteractiveProtocol.START_MARKER[0]) {
                    // 首字节不是协议起始标记，无法解析为 RPC 帧，直接拒绝
                    logger.warn("客户端首字节不是协议起始标记，拒绝连接");
                    clientSocket.close();
                    return;
                }

                logger.info("使用统一 RPC 协议");
                handleRpcClient(clientSocket, pushbackInput, output);

            } catch (Exception e) {
                logger.error("处理Socket客户端错误", e);
                try {
                    clientSocket.close();
                } catch (IOException ignored) {
                }
            }
        });
    }

    /**
     * 服务端 reader 线程：与命令执行线程分离，保证 cmd.prompt 的 channel.call() 不死锁。
     *
     * <p>只处理 {@link InteractiveProtocol#TYPE_RPC} 帧（统一 RPC 信封），
     * 每包更新会话级 {@code lastResponseTime}（最后在线时间），超时/EOF 关闭。</p>
     */
    private void runRpcServer(
            final InputStream input,
            final AtomicBoolean readerRunning,
            final Socket clientSocket,
            final AtomicLong lastResponseTime,
            final InteractiveOutputHandler finalOutputHandler
    ) {
        try {
            while (readerRunning.get() && !Thread.currentThread().isInterrupted()) {
                try {
                    clientSocket.setSoTimeout(INTERACTIVE_PROTOCOL_SOCKET_TIMEOUT_MS);

                    Object[] packet = InteractiveProtocol.readMessage(input);

                    if (packet == null) {
                        logger.info("客户端关闭连接");
                        break;
                    }

                    lastResponseTime.getAndSet(System.currentTimeMillis());

                    byte packetType = (byte) packet[0];
                    byte[] packetData = (byte[]) packet[1];

                    switch (packetType) {
                        case InteractiveProtocol.TYPE_RPC ->
                            finalOutputHandler.handleRpcData(packetData);

                        default ->
                            logger.warn("未知的客户端消息类型: " + packetType);
                    }

                } catch (SocketTimeoutException e) {
                    if (System.currentTimeMillis() - lastResponseTime.get() > INTERACTIVE_PROTOCOL_REQUEST_TIMEOUT_MS) {
                        logger.error("客户端响应超时 (" + (System.currentTimeMillis() - lastResponseTime.get()) + "ms)");
                        return;
                    }
                } catch (IOException e) {
                    if (!Objects.requireNonNull(e.getMessage()).contains("Socket closed") &&
                            !e.getMessage().contains("Read timed out") &&
                            !e.getMessage().contains("Connection reset")) {
                        logger.warn("读取客户端消息失败", e);
                    }
                    break;
                }
            }
        } finally {
            readerRunning.set(false);
            finalOutputHandler.close();
        }
    }

    /**
     * 统一 RPC 信封路径（TYPE_RPC 单帧，逻辑多通道 cmd.* / term.* / sys.*）。
     *
     * <p>流程：读首帧（应为 {@code sys.hello}）→ 建通道 → 同步完成握手 →
     * 注册 cmd.executeWithResult / sys.ping → 起 reader 线程 → 等待命令完成。</p>
     */
    private void handleRpcClient(Socket clientSocket, InputStream input, OutputStream output) {
        try (clientSocket) {
            Object[] firstPacket = InteractiveProtocol.readMessage(input);
            if (firstPacket == null) {
                logger.warn("客户端连接已关闭");
                return;
            }
            byte firstType = (byte) firstPacket[0];
            byte[] firstData = (byte[]) firstPacket[1];
            if (firstType != InteractiveProtocol.TYPE_RPC) {
                logger.warn("首帧非 TYPE_RPC（旧协议已废弃），拒绝连接: "
                        + InteractiveProtocol.getMessageTypeName(firstType));
                return;
            }

            InteractiveOutputHandler outputHandler = new InteractiveOutputHandler(output);
            outputHandler.initRpcChannel();

            TerminalRpcChannel channel = outputHandler.getRpcChannel();
            if (channel == null) {
                logger.error("创建 RPC 通道失败，关闭连接");
                outputHandler.close();
                return;
            }

            final AtomicBoolean commandStarted = new AtomicBoolean(false);
            final AtomicReference<ClientRequirements> helloRequirements = new AtomicReference<>();

            // sys.hello：能力协商（ClientRequirements 参数）→ 建 RemoteServerTerminal → 回传版本
            channel.onRequest(ProtocolMethods.SYS_HELLO, (id, params) -> {
                ClientRequirements requirements = ClientRequirements.fromRpcParams(params);
                helloRequirements.set(requirements);
                // supportsInput / jsonMode 由 applyClientRequirements 统一设置，无需重复调用
                outputHandler.applyClientRequirements(requirements);
                JsonObject resp = new JsonObject();
                channel.sendResponse(id, resp);
                logger.info("sys.hello 握手完成: " + requirements);
            });

            // 同步完成握手（reader 尚未启动，安全；此后 reader 线程逐帧有序处理）
            channel.handleMessage(firstData);

            // cmd.executeWithResult：先回执 ack，再异步执行（绝不在 reader 线程同步执行，防 cmd.prompt 死锁）
            channel.onRequest(ProtocolMethods.CMD_EXECUTE, (id, params) -> {
                String command = params != null && params.has("command") && !params.get("command").isJsonNull()
                        ? params.get("command").getAsString() : null;
                String requestJson = params != null && params.has("request") && !params.get("request").isJsonNull()
                        ? params.get("request").getAsString() : null;

                if ((command == null || command.trim().isEmpty())
                        && (requestJson == null || requestJson.trim().isEmpty())) {
                    channel.sendError(id, -32602, "cmd.executeWithResult 缺少 command/request 参数");
                    return;
                }

                commandStarted.set(true);
                JsonObject ack = new JsonObject();
                ack.addProperty("accepted", true);
                channel.sendResponse(id, ack);
                logger.info("收到 cmd.executeWithResult: " + (command != null ? command : requestJson));

                ThreadPoolManager.submitSocketRunnable(() -> {
                    try {
                        ClientRequirements requirements = helloRequirements.get();
                        if (requirements == null) {
                            requirements = new ClientRequirements();
                        }
                        if (requestJson != null && !requestJson.trim().isEmpty()) {
                            executeJsonRequest(requestJson, outputHandler, requirements);
                        } else {
                            commandExecutor.execute(command, outputHandler, requirements);
                        }
                    } catch (Exception e) {
                        logger.error("cmd.executeWithResult 执行失败", e);
                        try {
                            outputHandler.printlnError("执行命令失败: " + e.getMessage());
                        } catch (Exception ignored) {}
                        outputHandler.close();
                    }
                });
            });

            // sys.ping → sys.pong（RPC 层活性，取代裸帧心跳）
            channel.onNotification(ProtocolMethods.SYS_PING,
                    p -> channel.sendNotification(ProtocolMethods.SYS_PONG, null));

            final AtomicBoolean readerRunning = new AtomicBoolean(true);
            final AtomicLong lastResponseTime = new AtomicLong(System.currentTimeMillis());
            ThreadPoolManager.submitSocketRunnable(() -> runRpcServer(
                    input, readerRunning, clientSocket, lastResponseTime, outputHandler));

            logger.info("RPC 协议客户端就绪");

            // 等待会话关闭（outputHandler.close() 完成即返回）：
            // - 命令执行完 → CommandExecutor finally 调 outputHandler.close()（已发出/尝试发出 cmd.done）
            // - 客户端断开 → reader 线程 finally 也会 close()
            // 用关闭闩锁替代 sleep 轮询，保证 cmd.done 发出/发送失败后才关闭 socket（避免 GUI 收不到结果）。
            // 注意：不能以 isClosed() 作为"是否已收尾完毕"的判据——close() 一开始就会把 closed 置 true，
            // 之后才发送 cmd.done；此处必须无条件等待闩锁，否则仍会提前关掉 socket。
            long waitStart = System.currentTimeMillis();
            while (!commandStarted.get() && !outputHandler.awaitClosed(100, TimeUnit.MILLISECONDS)) {
                if (System.currentTimeMillis() - waitStart > 60000) {
                    logger.warn("客户端长时间未发送 cmd.executeWithResult，关闭连接");
                    outputHandler.close();
                    break;
                }
            }
            outputHandler.awaitClosed();
            logger.info("RPC 协议命令执行完成");

        } catch (Throwable t) {
            logger.error("处理 RPC 协议客户端错误", t);
        }
    }

    /**
     * 执行 JSON 命令请求（cmd.executeWithResult 的 request 参数路径，USER_INTERFACE 模式）
     */
    private void executeJsonRequest(String requestJson, InteractiveOutputHandler outputHandler,
                                    ClientRequirements requirements) {
        CommandRequest<?> request = CommandRouter.getInstance().resolveRequestFromJson(requestJson);
        if (request == null) {
            logger.warn("cmd.executeWithResult 请求解析失败: " + requestJson);
            outputHandler.printlnError("无法解析请求: " + requestJson);
            // 必须随 cmd.done 回传结构化错误，否则客户端只会拿到一个空响应（result 缺失），
            // 界面上就只能显示"查询失败: %s"这种没有信息量的兜底文案。
            outputHandler.finish(buildRequestErrorResult(requestJson));
            outputHandler.close();
            return;
        }
        commandExecutor.execute(request, outputHandler, requirements, CommandType.USER_INTERFACE);
    }

    /**
     * 构造"请求解析失败"的结构化错误结果（随 cmd.done 回传）。
     *
     * <p>优先带上 commandType，便于直接看出是哪一个命令类型没有注册。</p>
     */
    private static CommandResult buildRequestErrorResult(String requestJson) {
        String detail = "服务端未注册该命令类型或请求格式错误";
        try {
            JsonObject obj = JsonParser.parseString(requestJson).getAsJsonObject();
            if (obj.has("commandType") && !obj.get("commandType").isJsonNull()) {
                detail = "服务端未注册的命令类型: " + obj.get("commandType").getAsString();
            }
        } catch (Exception ignored) {
            // 请求本身不是合法 JSON，沿用上面的通用描述
        }

        CommandResult result = new CommandResult();
        result.setSuccess(false);
        result.setMessage(detail);
        result.setError(new CommandResult.ErrorInfo("REQUEST_NOT_REGISTERED", detail));
        return result;
    }
}
