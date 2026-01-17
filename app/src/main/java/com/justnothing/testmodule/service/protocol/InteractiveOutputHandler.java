package com.justnothing.testmodule.service.protocol;

import com.justnothing.testmodule.command.output.IOutputHandler;
import com.justnothing.testmodule.utils.functions.Logger;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;


public class InteractiveOutputHandler implements IOutputHandler {

    public static final int PING_PONG_TIMEOUT = 30000;
    public static final int INPUT_PING_PONG_INTERVAL = 5000;

    private static class BinaryLogger extends Logger {
        @Override
        public String getTag() {
            return "BinaryInteractiveOutput";
        }
    }
    public static AtomicLong lastResponseTime = new AtomicLong(0);

    private static final BinaryLogger logger = new BinaryLogger();

    private final StringBuilder buffer = new StringBuilder();
    private final OutputStream outputStream;
    private final BlockingQueue<String> inputQueue = new LinkedBlockingQueue<>();
    private final AtomicBoolean closed = new AtomicBoolean(false);
    private final AtomicReference<Thread> pingThreadRef = new AtomicReference<>();
    private final Object writeLock = new Object();

    public InteractiveOutputHandler(OutputStream outputStream) {
        this.outputStream = outputStream;
    }

    @Override
    public void println(String line) {
        sendOutput(line + "\n");
    }

    @Override
    public void print(String text) {
        sendOutput(text);
    }

    @Override
    public void printf(String format, Object... args) {
        sendOutput(String.format(format, args));
    }

    @Override
    public void printStackTrace(Throwable t) {
        if (!closed.get() && t != null) {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            t.printStackTrace(pw);
            println(sw.toString());
        }
    }

    // 修改 readLine 方法，同步超时时间
    @Override
    public String readLineFromClient(String prompt) {
        if (closed.get()) {
            return null;
        }

        lastResponseTime.getAndSet(System.currentTimeMillis());

        final String requestId = UUID.randomUUID().toString();
        logger.debug("发送输入请求: " + requestId + " - " + prompt);

        try {
            // 发送输入请求
            String requestData = requestId + ":" + prompt;
            synchronized (writeLock) {
                InteractiveProtocol.writeMessage(outputStream,
                        InteractiveProtocol.TYPE_SERVER_INPUT_REQUEST,
                        requestData.getBytes(StandardCharsets.UTF_8));
            }

            // 启动心跳线程
            Thread pingThread = new Thread(() -> {
                while (!Thread.currentThread().isInterrupted() &&
                        !closed.get() &&
                        (System.currentTimeMillis() - lastResponseTime.get()) < PING_PONG_TIMEOUT) {
                    try {
                        Thread.sleep(INPUT_PING_PONG_INTERVAL);
                        synchronized (writeLock) {
                            InteractiveProtocol.writeMessage(outputStream,
                                    InteractiveProtocol.TYPE_INPUT_PING,
                                    null);
                        }
                        logger.debug("向客户端发送INPUT_PING包");

                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    } catch (IOException e) {
                        logger.warn("发送INPUT_PING失败", e);
                        break;
                    }
                }
            });

            pingThread.setDaemon(true);
            pingThread.start();

            while (!closed.get() && (System.currentTimeMillis() - lastResponseTime.get()) < PING_PONG_TIMEOUT) {
                String response = inputQueue.poll(500, TimeUnit.MILLISECONDS);
                if (response != null) {
                    logger.debug("收到输入响应: " + requestId + " - " + response);

                    pingThread.interrupt();
                    try {
                        pingThread.join(1000);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                    return response;
                }

                // 每10秒记录一次
                long elapsed = System.currentTimeMillis() - lastResponseTime.get();
                if (elapsed > 10000 && elapsed % 10000 < 500) {
                    logger.debug("输入请求 " + requestId +
                            " 已经有 " + (elapsed / 1000f) + " 秒没有进行PING-PONG通信了");
                }
            }

            if (!closed.get()) {
                logger.warn("输入请求超时: " + requestId + " (" + PING_PONG_TIMEOUT + "秒)");
                throw new RuntimeException("输入请求" + requestId + "超时 (" + PING_PONG_TIMEOUT + "秒)");
            }

            return null;

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.warn("输入请求被中断: " + requestId);
            throw new RuntimeException("输入被中断");
        } catch (IOException e) {
            logger.error("发送输入请求失败", e);
            throw new RuntimeException("通信失败");
        }
    }

    // 改进 close 方法
    @Override
    public void close() {
        if (closed.compareAndSet(false, true)) {
            try {
                synchronized (writeLock) {
                    // 发送命令结束标记
                    InteractiveProtocol.writeMessage(outputStream,
                            InteractiveProtocol.TYPE_COMMAND_END,
                            null);
                    outputStream.flush();
                    logger.debug("发送命令结束标记");
                }
            } catch (IOException e) {
                // 连接可能已关闭，忽略
                logger.debug("发送命令结束标记失败: " + e.getMessage());
            }
        }
    }

    @Override
    public String readPasswordFromClient(String prompt) {
        if (closed.get()) {
            return null;
        }

        final String requestId = UUID.randomUUID().toString();

        try {
            // 对于密码输入，可以发送特殊标志
            String requestData = requestId + ":PASSWORD:" + prompt;
            synchronized (writeLock) {
                InteractiveProtocol.writeMessage(outputStream,
                        InteractiveProtocol.TYPE_SERVER_INPUT_REQUEST,
                        requestData.getBytes(StandardCharsets.UTF_8));
            }

            long startTime = System.currentTimeMillis();
            Thread pingThread = new Thread(() -> {
                while (!Thread.currentThread().isInterrupted() &&
                        !closed.get() &&
                        (System.currentTimeMillis() - startTime) < 60000) {
                    try {
                        Thread.sleep(5000);

                        synchronized (writeLock) {
                            InteractiveProtocol.writeMessage(outputStream,
                                    InteractiveProtocol.TYPE_SERVER_PING,
                                    null);
                        }
                        logger.debug("发送心跳包");

                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    } catch (IOException e) {
                        logger.warn("发送心跳失败", e);
                        break;
                    }
                }
            });

            pingThread.setDaemon(true);
            pingThread.start();
            pingThreadRef.set(pingThread);

            while (!closed.get() && (System.currentTimeMillis() - startTime) < 60000) {
                String response = inputQueue.poll(1, TimeUnit.SECONDS);
                if (response != null) {
                    return response;
                }
            }

            if (!closed.get()) {
                throw new RuntimeException("密码输入超时");
            }

            return null;

        } catch (Exception e) {
            throw new RuntimeException("密码输入失败", e);
        } finally {
            stopPingThread();
        }
    }


    @Override
    public boolean isInteractive() {
        return true;
    }

    // 处理客户端发送的输入响应
    public void handleInputResponse(String requestId, String response) {
        logger.debug("处理输入响应: " + requestId);
        inputQueue.offer(response);
    }

    @Override
    public void flush() {
        // 发送命令结束标记
        if (!closed.get()) {
            try {
                synchronized (writeLock) {
                    InteractiveProtocol.writeMessage(outputStream,
                            InteractiveProtocol.TYPE_COMMAND_END,
                            null);
                }
            } catch (IOException e) {
                logger.error("发送命令结束标记失败", e);
            }
        }
    }


    @Override
    public boolean isClosed() {
        return closed.get();
    }

    @Override
    public void clear() {
        buffer.setLength(0);
    }

    @Override
    public String getString() {
        return buffer.toString();
    }

    private void sendOutput(String text) {
        if (!closed.get() && text != null && !text.isEmpty()) {
            try {
                synchronized (writeLock) {
                    InteractiveProtocol.writeMessage(outputStream,
                            InteractiveProtocol.TYPE_SERVER_OUTPUT,
                            text.getBytes(StandardCharsets.UTF_8));
                }
                buffer.append(text);
            } catch (IOException e) {
                logger.error("发送输出失败", e);
                close();
            }
        }
    }

    private void sendError(String text) {
        if (!closed.get() && text != null && !text.isEmpty()) {
            try {
                synchronized (writeLock) {
                    InteractiveProtocol.writeMessage(outputStream,
                            InteractiveProtocol.TYPE_SERVER_ERROR,
                            text.getBytes(StandardCharsets.UTF_8));
                }
                buffer.append(text);
            } catch (IOException e) {
                logger.error("发送错误输出失败", e);
                close();
            }
        }
    }

    private void stopPingThread() {
        Thread pingThread = pingThreadRef.getAndSet(null);
        if (pingThread != null && pingThread.isAlive()) {
            pingThread.interrupt();
            try {
                pingThread.join(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
}