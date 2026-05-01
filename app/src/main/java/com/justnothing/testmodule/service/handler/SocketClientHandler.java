package com.justnothing.testmodule.service.handler;

import android.util.Log;

import com.justnothing.testmodule.command.CommandExecutor;
import com.justnothing.testmodule.command.CommandType;
import com.justnothing.testmodule.command.output.ClientRequirements;
import com.justnothing.testmodule.command.output.ICommandOutputHandler;
import com.justnothing.testmodule.command.output.InteractiveOutputHandler;
import com.justnothing.testmodule.command.output.GuiOutputHandler;
import com.justnothing.testmodule.command.protocol.InteractiveProtocol;
import com.justnothing.testmodule.command.protocol.JsonProtocol;
import com.justnothing.testmodule.command.base.CommandRequest;
import com.justnothing.testmodule.command.base.CommandResult;
import com.justnothing.testmodule.utils.logging.Logger;
import com.justnothing.testmodule.utils.concurrent.ThreadPoolManager;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.PushbackInputStream;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class SocketClientHandler {
    private static final Logger logger = Logger.getLoggerForName("SocketClientHandler");

    public static final long PING_CLIENT_INTERVAL_MS = 5000;
    public static final int CLIENT_CONNECT_SOCKET_TIMEOUT_MS = 5000;
    public static final int TEXT_PROTOCOL_SOCKET_TIMEOUT_MS = 10000;
    public static final int INTERACTIVE_PROTOCOL_SOCKET_TIMEOUT_MS = 10000;
    public static final int INTERACTIVE_PROTOCOL_REQUEST_TIMEOUT_MS = 30000;
    public static final int OUTPUT_HANDLER_CLOSE_TIMEOUT_MS = 5000;

    private final CommandExecutor commandExecutor;

    public SocketClientHandler(CommandExecutor commandExecutor) {
        this.commandExecutor = commandExecutor;
    }

    /**
     * 初始包读取结果
     */
    private static class InitialPacketResult {
        ClientRequirements requirements = new ClientRequirements();
        Object[] commandPacket = null;
    }

    /**
     * 读取初始包（CAPABILITY 和 COMMAND/COMMAND_REQUEST），不强制顺序。
     * 
     * <p>循环读取包直到：
     * <ul>
     *   <li>同时收到 CAPABILITY 和 COMMAND（或 COMMAND_REQUEST）</li>
     *   <li>连接关闭</li>
     *   <li>遇到非 CAPABILITY/COMMAND 类型的包（停止读取，但不丢弃）</li>
     * </ul>
     * </p>
     * 
     * @param input 输入流
     * @return 初始包读取结果
     */
    private InitialPacketResult readInitialPackets(InputStream input) throws IOException {
        InitialPacketResult result = new InitialPacketResult();
        boolean hasCapability = false;
        boolean hasCommand = false;
        
        while (!hasCommand) {
            Object[] packet = InteractiveProtocol.readMessage(input);
            if (packet == null) {
                logger.warn("客户端连接已关闭");
                break;
            }
            
            byte packetType = (byte) packet[0];
            byte[] packetData = (byte[]) packet[1];
            
            switch (packetType) {
                case InteractiveProtocol.TYPE_CLIENT_CAPABILITY:
                    if (!hasCapability) {
                        result.requirements = InteractiveProtocol.decodeCapability(packetData);
                        hasCapability = true;
                        logger.info("客户端能力: " + result.requirements);
                    } else {
                        logger.warn("收到重复的CAPABILITY包，忽略");
                    }
                    break;
                    
                case InteractiveProtocol.TYPE_CLIENT_COMMAND:
                case InteractiveProtocol.TYPE_JSON_COMMAND_REQUEST:
                    if (!hasCommand) {
                        result.commandPacket = packet;
                        hasCommand = true;
                        logger.debug("收到命令包: " + InteractiveProtocol.getMessageTypeName(packetType));
                    } else {
                        logger.warn("收到重复的COMMAND包，忽略");
                    }
                    break;
                    
                default:
                    logger.warn("收到非预期的初始包类型: " + InteractiveProtocol.getMessageTypeName(packetType) + 
                               "，停止读取初始包");
                    return result;
            }
        }
        
        return result;
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

                if (firstByte == InteractiveProtocol.START_MARKER[0]) {
                    logger.info("使用交互式协议");
                    handleInteractiveProtocolClient(clientSocket, pushbackInput, output);
                } else {
                    logger.info("使用纯文本协议");
                    handleTextProtocolClient(clientSocket, pushbackInput, output);
                }

            } catch (Exception e) {
                logger.error("处理Socket客户端错误", e);
                try {
                    clientSocket.close();
                } catch (IOException ignored) {
                }
            }
        });
    }

    private void handleTextProtocolClient(Socket clientSocket, PushbackInputStream input, OutputStream output) {
        try (clientSocket) {
            try {
                clientSocket.setSoTimeout(TEXT_PROTOCOL_SOCKET_TIMEOUT_MS);

                BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8));
                PrintWriter writer = new PrintWriter(new OutputStreamWriter(output, StandardCharsets.UTF_8), true);

                StringBuilder commandBuilder = new StringBuilder();
                int c;
                while ((c = reader.read()) != -1) {
                    if (c == '\n' || c == '\r') {
                        break;
                    }
                    commandBuilder.append((char) c);
                }

                String command = commandBuilder.toString();
                if (command.trim().isEmpty()) {
                    logger.warn("收到空命令");
                    return;
                }

                logger.debug("Socket客户端命令: " + command);


                executeCommandForTextProtocolSocket(command, writer);
            } catch (Exception e) {
                logger.error("处理Socket客户端错误", e);
            }
        } catch (IOException ignored) {
        }
    }

    private void runInteractiveProtocolServer(
            final InputStream input,
            final OutputStream output,
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
                        case InteractiveProtocol.TYPE_INPUT_RESPONSE:
                            if (packetData != null) {
                                String response = new String(packetData, StandardCharsets.UTF_8);
                                String[] parts = response.split(":", 2);
                                if (parts.length == 2) {
                                    logger.debug("收到输入响应: " + parts[0]);
                                    finalOutputHandler.handleInputResponse(parts[0], parts[1]);
                                }
                            }
                            break;

                        case InteractiveProtocol.TYPE_CLIENT_PONG:
                            lastResponseTime.getAndSet(System.currentTimeMillis());
                            logger.debug("收到客户端的CLIENT_PONG响应, 更新客户端最近响应时间: " + lastResponseTime.get());
                            break;

                        case InteractiveProtocol.TYPE_INPUT_PONG:
                            InteractiveOutputHandler.lastResponseTime.getAndSet(System.currentTimeMillis());
                            logger.debug("收到了输入端的INPUT_PONG响应，更新输入最近响应时间: " +
                                    InteractiveOutputHandler.lastResponseTime.get());
                            break;

                        case InteractiveProtocol.TYPE_CLIENT_PING:
                            logger.debug("收到了客户端的CLIENT_PING请求，发送SERVER_PONG");
                            InteractiveProtocol.writeMessage(output, InteractiveProtocol.TYPE_SERVER_PONG, null);
                            break;

                        default:
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

    private void runInteractiveProtocolPing(
        final OutputStream output
    ) {

        try {
            InteractiveProtocol.writeMessage(output,
                    InteractiveProtocol.TYPE_SERVER_PING,
                    null);
            logger.debug("向客户端发送SERVER_PING包");

        } catch (IOException e) {
            // Socket closed是正常的，因为客户端可能已经断开连接
            if (!Objects.requireNonNullElse(e.getMessage(), "").contains("Socket closed")) {
                logger.warn("发送SERVER_PING失败", e);
            }
        }
    }

    private void handleInteractiveProtocolClient(Socket clientSocket, InputStream input, OutputStream output) {
        try (clientSocket) {
            try {
                InitialPacketResult initialResult = readInitialPackets(input);
                ClientRequirements requirements = initialResult.requirements;
                Object[] commandPacket = initialResult.commandPacket;
                    
                if (commandPacket == null) {
                    logger.warn("客户端连接已关闭或无效, 没有收到需要执行的命令");
                    return;
                }

                byte type = (byte) commandPacket[0];
                byte[] data = (byte[]) commandPacket[1];

                if (data == null) {
                    logger.warn("命令包数据为空");
                    return;
                }

                InteractiveOutputHandler outputHandler = new InteractiveOutputHandler(output);
                outputHandler.setSupportsInput(requirements.isSupportsInput());
                outputHandler.setJsonMode(requirements.isJsonMode());

                final AtomicBoolean readerRunning = new AtomicBoolean(true);
                final AtomicLong lastResponseTime = new AtomicLong(System.currentTimeMillis());
                ScheduledFuture<?> future = null;
                
                if (!requirements.isJsonMode()) {
                    ThreadPoolManager.submitSocketRunnable(() -> runInteractiveProtocolServer(
                            input, output, readerRunning, clientSocket, lastResponseTime, outputHandler
                    ));

                    future = ThreadPoolManager.scheduleWithFixedDelay(
                        () -> runInteractiveProtocolPing(output),
                        PING_CLIENT_INTERVAL_MS, PING_CLIENT_INTERVAL_MS, TimeUnit.MILLISECONDS);
                }

                if (type == InteractiveProtocol.TYPE_CLIENT_COMMAND) {
                    String command = new String(data, StandardCharsets.UTF_8);
                    logger.debug("接收到的客户端命令: " + command);
                    outputHandler.setCommand(command);
                    
                    try {
                        commandExecutor.execute(command, outputHandler, requirements);
                    } catch (Exception e) {
                        logger.error("执行命令失败", e);
                        try {
                            outputHandler.println("执行命令失败: " + e.getMessage());
                        } catch (Exception ignored) {}
                    } finally {
                        outputHandler.close();
                    }
                } else if (type == InteractiveProtocol.TYPE_JSON_COMMAND_REQUEST) {
                    logger.debug("接收到的为JSON命令请求");
                    handleCommandRequest(data, output);
                }
                
                if (requirements.isJsonMode()) {
                    logger.info("使用JSON输出模式");
                }

                AtomicInteger waited = new AtomicInteger(0);
                ThreadPoolManager.scheduleWithFixedDelayUntil(
                    () -> waited.getAndAdd(100),
                    0,
                    100, TimeUnit.MILLISECONDS,
                    () -> waited.get() < OUTPUT_HANDLER_CLOSE_TIMEOUT_MS && !outputHandler.isClosed()
                );

                logger.info("命令执行完成");
                if (future != null) future.cancel(true);

            } catch (Throwable t) {
                logger.error("处理交互协议客户端错误", t);
            }
        } catch (IOException ignored) {
        }
    }

    private void executeCommandForTextProtocolSocket(String command, final PrintWriter writer) {
        ICommandOutputHandler socketOutput = new ICommandOutputHandler() {
            private volatile boolean closed = false;
            final StringBuilder sb = new StringBuilder();

            @Override
            public void println(String line) {
                if (closed) return;
                try {
                    writer.println(line);
                    sb.append(line).append('\n');
                } catch (Exception e) {
                    logger.warn("向Socket写入失败", e);
                    close();
                }
            }

            @Override
            public void print(String text) {
                if (closed) return;
                try {
                    writer.print(text);
                    writer.flush();
                    sb.append(text);
                } catch (Exception e) {
                    logger.warn("向Socket写入失败", e);
                    close();
                }
            }

            @Override
            public void printf(String format, Object... args) {
                print(String.format(format, args));
            }

            @Override
            public void printError(String text) {
                print("[ERROR] " + text);
            }

            @Override
            public void printlnError(String text) {
                println("[ERROR] " + text);
            }

            @Override
            public void printStackTrace(Throwable t) {
                if (t != null) {
                    String stacktrace = Log.getStackTraceString(t);
                    print(stacktrace);
                }
            }

            @Override
            public void flush() {
                if (closed) return;
                try {
                    writer.flush();
                } catch (Exception ignored) {
                }
            }

            @Override
            public void close() {
                closed = true;
            }

            @Override
            public boolean isClosed() {
                return closed;
            }

            @Override
            public void clear() {
                sb.setLength(0);
            }

            @Override
            public String getString() {
                return sb.toString();
            }

            @Override
            public String readLineFromClient(String prompt) {
                throw new RuntimeException("当前的执行方式不支持交互...");
            }

            @Override
            public String readPasswordFromClient(String prompt) {
                throw new RuntimeException("当前的执行方式不支持交互...");
            }

            @Override
            public boolean isInteractive() {
                return false;
            }
        };

        try {
            commandExecutor.execute(command, socketOutput, null);
        } catch (Exception e) {
            socketOutput.println("执行命令失败: " + e.getMessage());
            logger.error("执行Socket命令失败", e);
        } finally {
            socketOutput.close();
        }
    }
    
    private void handleCommandRequest(byte[] data, OutputStream output) {
        try {
            String jsonRequest = new String(data, StandardCharsets.UTF_8);
            logger.info("命令请求: " + jsonRequest);

            // 解析请求
            CommandRequest request = JsonProtocol.parseRequest(jsonRequest);

            if (request == null) {
                CommandResult errorResult = new CommandResult();
                errorResult.setError(new CommandResult.ErrorInfo(
                    "INVALID_REQUEST", "无法解析请求"
                ));
                sendCommandResponse(output, errorResult);
                return;
            }

            // 使用GuiOutputHandler吞掉CLI输出，只保留result.toJson()
            ICommandOutputHandler socketOutput = new ICommandOutputHandler() {
                @Override
                public void print(String text) {
                    try {
                        output.write(text.getBytes(StandardCharsets.UTF_8));
                    } catch (IOException e) {
                        logger.error("写入输出失败", e);
                    }
                }

                @Override
                public void println(String line) {
                    print(line + "\n");
                }

                @Override
                public void printf(String format, Object... args) {
                    print(String.format(format, args));
                }

                @Override
                public void printError(String text) {
                    print(text);
                }

                @Override
                public void printlnError(String text) {
                    println(text);
                }

                @Override
                public void printStackTrace(Throwable t) {
                    println(t.toString());
                }

                @Override
                public void flush() {
                    try {
                        output.flush();
                    } catch (IOException ignored) {}
                }

                @Override
                public void close() {
                    try {
                        output.close();
                    } catch (IOException ignored) {}
                }

                @Override
                public boolean isClosed() {
                    return false;
                }

                @Override
                public void clear() {}

                @Override
                public String getString() {
                    return "";
                }

                @Override
                public boolean isInteractive() {
                    return false;
                }
            };

            GuiOutputHandler guiOutput = new GuiOutputHandler(socketOutput);

            commandExecutor.execute(
                request,
                guiOutput,
                null,
                CommandType.USER_INTERFACE
            );

        } catch (Exception e) {
            logger.error("处理命令请求失败", e);
            CommandResult errorResult = new CommandResult();
            errorResult.setError(new CommandResult.ErrorInfo(
                "INTERNAL_ERROR", "处理请求失败: " + e.getMessage()
            ));
            try {
                sendCommandResponse(output, errorResult);
            } catch (Exception ignored) {}
        }
    }
    
    private void sendCommandResponse(OutputStream output, 
                                   CommandResult result) throws Exception {
        String jsonResponse = JsonProtocol.toJson(result);
        logger.info("命令响应: resultType=" + result.getResultType() + ", class=" + result.getClass().getSimpleName() + ", json=" + jsonResponse);
        
        byte[] data = InteractiveProtocol.encodeColoredOutput((byte) 0, jsonResponse);
        InteractiveProtocol.writeMessage(output, 
                InteractiveProtocol.TYPE_COLORED_OUTPUT, 
                data);
        
        InteractiveProtocol.writeMessage(output, 
                InteractiveProtocol.TYPE_COMMAND_END, 
                null);
        logger.debug("已发送COMMAND_END标记");
    }
}
