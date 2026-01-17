package com.justnothing.testmodule.service.handler;

import android.util.Log;

import com.justnothing.testmodule.command.CommandExecutor;
import com.justnothing.testmodule.command.output.IOutputHandler;
import com.justnothing.testmodule.service.protocol.InteractiveOutputHandler;
import com.justnothing.testmodule.service.protocol.InteractiveProtocol;
import com.justnothing.testmodule.utils.functions.Logger;

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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

public class SocketClientHandler {
    private static final Logger logger = new Logger() {
        @Override
        public String getTag() {
            return "SocketClientHandler";
        }
    };

    private static final long PING_CLIENT_INTERVAL = 5000;
    public static final int PROTOCOL_REQUEST_TIMEOUT = 30000;

    private final CommandExecutor commandExecutor;
    private final ExecutorService socketExecutor = Executors.newCachedThreadPool();

    public SocketClientHandler(CommandExecutor commandExecutor) {
        this.commandExecutor = commandExecutor;
    }

    public void handleClient(Socket clientSocket) {
        socketExecutor.submit(() -> {
            try {
                InputStream input = clientSocket.getInputStream();
                OutputStream output = clientSocket.getOutputStream();

                clientSocket.setSoTimeout(5000);

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
        socketExecutor.submit(() -> {
            try {
                clientSocket.setSoTimeout(30000);

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

                executeCommandForSocket(command, writer);
            } catch (Exception e) {
                logger.error("处理Socket客户端错误", e);
            } finally {
                try {
                    clientSocket.close();
                } catch (IOException e) {
                }
            }
        });
    }

    private void handleInteractiveProtocolClient(Socket clientSocket, InputStream input, OutputStream output) {
        socketExecutor.submit(() -> {
            InteractiveOutputHandler outputHandler;

            try {
                Object[] commandPacket = InteractiveProtocol.readMessage(input);
                if (commandPacket == null) {
                    logger.warn("客户端连接已关闭或无效, 没有收到需要执行的命令");
                    return;
                }

                byte type = (byte) commandPacket[0];
                byte[] data = (byte[]) commandPacket[1];

                if (type != InteractiveProtocol.TYPE_CLIENT_COMMAND || data == null) {
                    logger.warn("第一个包不是有效的命令");
                    return;
                }

                String command = new String(data, StandardCharsets.UTF_8);
                logger.debug("接收到的客户端命令: " + command);

                outputHandler = new InteractiveOutputHandler(output);

                final AtomicBoolean readerRunning = new AtomicBoolean(true);
                final InteractiveOutputHandler finalOutputHandler = outputHandler;
                final AtomicLong lastResponseTime = new AtomicLong(System.currentTimeMillis());

                Thread readerThread = new Thread(() -> {
                    try {
                        while (readerRunning.get() && !Thread.currentThread().isInterrupted()) {
                            try {
                                clientSocket.setSoTimeout(1000);

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
                                if (System.currentTimeMillis() - lastResponseTime.get() > PROTOCOL_REQUEST_TIMEOUT) {
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
                });

                Object writeLock = new Object();

                Thread pingThread = new Thread(() -> {
                    while (!Thread.currentThread().isInterrupted() &&
                            (System.currentTimeMillis() - lastResponseTime.get()) < PROTOCOL_REQUEST_TIMEOUT) {
                        try {
                            Thread.sleep(PING_CLIENT_INTERVAL);
                            synchronized (writeLock) {
                                InteractiveProtocol.writeMessage(output,
                                        InteractiveProtocol.TYPE_SERVER_PING,
                                        null);
                            }
                            logger.debug("向客户端发送SERVER_PING包");

                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            break;
                        } catch (IOException e) {
                            logger.warn("发送SERVER_PING失败", e);
                            break;
                        }
                    }
                });

                readerThread.setDaemon(true);
                pingThread.setDaemon(true);
                readerThread.start();
                pingThread.start();

                try {
                    commandExecutor.execute(command, outputHandler);
                } catch (Exception e) {
                    logger.error("执行命令失败", e);
                    try {
                        outputHandler.println("执行命令失败: " + e.getMessage());
                    } catch (Exception ex) {
                    }
                }

                try {
                    int maxWait = 5000;
                    int waited = 0;
                    while (waited < maxWait && !outputHandler.isClosed()) {
                        Thread.sleep(100);
                        waited += 100;
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }

                logger.info("命令执行完成");

            } catch (Exception e) {
                logger.error("处理交互协议客户端错误", e);
            } finally {
                try {
                    clientSocket.close();
                } catch (IOException ignored) {
                }
            }
        });
    }

    private void executeCommandForSocket(String command, final PrintWriter writer) {
        IOutputHandler socketOutput = new IOutputHandler() {
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
                } catch (Exception e) {
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
            commandExecutor.execute(command, socketOutput);
        } catch (Exception e) {
            socketOutput.println("执行命令失败: " + e.getMessage());
            logger.error("执行Socket命令失败", e);
        } finally {
            socketOutput.close();
        }
    }
}
