package com.justnothing.testmodule.command.output;

import androidx.annotation.NonNull;

import com.justnothing.testmodule.utils.logging.Logger;
import com.justnothing.testmodule.utils.concurrent.ThreadPoolManager;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;


public class InteractiveOutputHandler implements ICommandOutputHandler {

    public static final int PING_PONG_TIMEOUT = 30000;
    public static final int INPUT_PING_PONG_INTERVAL = 5000;
    private static final long PASSWORD_PING_INTERVAL = 5000;

    public static AtomicLong lastResponseTime = new AtomicLong(0);

    private static final Logger logger = Logger.getLoggerForName("InteractiveOutputHandler");

    private final StringBuilder buffer = new StringBuilder();
    private final OutputStream outputStream;
    private final BlockingQueue<String> inputQueue = new LinkedBlockingQueue<>();
    private final AtomicBoolean closed = new AtomicBoolean(false);
    private final AtomicReference<ScheduledFuture<?>> pingFutureRef = new AtomicReference<>();
    private final Object writeLock = new Object();
    private volatile boolean supportsInput = true;

    public InteractiveOutputHandler(OutputStream outputStream) {
        this.outputStream = outputStream;
    }

    public void setSupportsInput(boolean supportsInput) {
        this.supportsInput = supportsInput;
    }

    public boolean isSupportsInput() {
        return supportsInput;
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
    public void print(String text, byte color) {
        sendColoredOutput(color, text);
    }
    
    @Override
    public void println(String text, byte color) {
        sendColoredOutput(color, text + "\n");
    }
    
    @Override
    public void printf(byte color, String format, Object... args) {
        sendColoredOutput(color, String.format(format, args));
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

    @Override
    public void printStackTrace(Throwable t, byte color) {
        if (!closed.get() && t != null) {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            t.printStackTrace(pw);
            sendColoredOutput(color, sw.toString());
        }
    }

    @Override
    public void printError(String text) {
        sendColoredOutput(Colors.RED, text);
    }

    @Override
    public void printlnError(String text) {
        sendColoredOutput(Colors.RED, text + "\n");
    }
    
    @Override
    public void printSuccess(String text) {
        sendColoredOutput(Colors.GREEN, text);
    }
    
    @Override
    public void printlnSuccess(String text) {
        sendColoredOutput(Colors.GREEN, text + "\n");
    }
    
    @Override
    public void printWarning(String text) {
        sendColoredOutput(Colors.YELLOW, text);
    }
    
    @Override
    public void printlnWarning(String text) {
        sendColoredOutput(Colors.YELLOW, text + "\n");
    }
    
    @Override
    public void printInfo(String text) {
        sendColoredOutput(Colors.BLUE, text);
    }
    
    @Override
    public void printlnInfo(String text) {
        sendColoredOutput(Colors.BLUE, text + "\n");
    }
    
    @Override
    public void printDebug(String text) {
        sendColoredOutput(Colors.CYAN, text);
    }
    
    @Override
    public void printlnDebug(String text) {
        sendColoredOutput(Colors.CYAN, text + "\n");
    }


    @Override
    public String readLineFromClient(String prompt) {
        if (!supportsInput) {
            throw new RuntimeException(getClass().getName() + " 并不支持readLineFromClient...");
        }
        if (closed.get()) {
            return null;
        }

        lastResponseTime.getAndSet(System.currentTimeMillis());

        final String requestId = UUID.randomUUID().toString();
        logger.debug("发送输入请求: " + requestId + " - " + prompt);

        try {
            String requestData = requestId + ":" + prompt;
            synchronized (writeLock) {
                InteractiveProtocol.writeMessage(outputStream,
                        InteractiveProtocol.TYPE_SERVER_INPUT_REQUEST,
                        requestData.getBytes(StandardCharsets.UTF_8));
            }

            ScheduledFuture<?> pingFuture = ThreadPoolManager.scheduleWithFixedDelay(
                    this::runInputPing,
                    INPUT_PING_PONG_INTERVAL, INPUT_PING_PONG_INTERVAL, TimeUnit.MILLISECONDS);
            pingFutureRef.set(pingFuture);

            while (!closed.get() && (System.currentTimeMillis() - lastResponseTime.get()) < PING_PONG_TIMEOUT) {
                String response = inputQueue.poll(500, TimeUnit.MILLISECONDS);
                if (response != null) {
                    logger.debug("收到输入响应: " + requestId + " - " + response);
                    return response;
                }

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
            logger.warn("输入请求被中断: " + requestId, e);
            throw new RuntimeException("输入被中断");
        } catch (IOException e) {
            logger.error("发送输入请求失败", e);
            throw new RuntimeException("通信失败");
        } finally {
            stopPingFuture();
        }
    }

    @Override
    public void close() {
        if (closed.compareAndSet(false, true)) {
            try {
                synchronized (writeLock) {
                    InteractiveProtocol.writeMessage(outputStream,
                            InteractiveProtocol.TYPE_COMMAND_END,
                            null);
                    outputStream.flush();
                    logger.debug("发送命令结束标记");
                }
            } catch (IOException e) {
                logger.debug("发送命令结束标记失败: " + e.getMessage());
            }
        }
    }

    @Override
    public String readPasswordFromClient(String prompt) {
        if (!supportsInput) {
            throw new RuntimeException(getClass().getName() + " 并不支持readPasswordFromClient...");
        }
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
            ScheduledFuture<?> pingFuture = getPingFuture();
            pingFutureRef.set(pingFuture);

            while (!closed.get() && (System.currentTimeMillis() - startTime) < 60000) {
                String response = inputQueue.poll(1, TimeUnit.SECONDS);
                if (response != null) {
                    pingFuture.cancel(true);
                    pingFutureRef.set(null);
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
            stopPingFuture();
        }
    }

    @NonNull
    private ScheduledFuture<?> getPingFuture() {
        return Objects.requireNonNull(ThreadPoolManager.scheduleWithFixedDelay(
                this::runServerPing,
                PASSWORD_PING_INTERVAL, PASSWORD_PING_INTERVAL, TimeUnit.MILLISECONDS));
    }


    @Override
    public boolean isInteractive() {
        return true;
    }

    public void handleInputResponse(String requestId, String response) {
        logger.debug("处理输入响应: " + requestId);
        inputQueue.offer(response);
    }

    @Override
    public void flush() {
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
    
    private void sendColoredOutput(byte color, String text) {
        if (!closed.get() && text != null && !text.isEmpty()) {
            try {
                synchronized (writeLock) {
                    byte[] data = InteractiveProtocol.encodeColoredOutput(color, text);
                    InteractiveProtocol.writeMessage(outputStream,
                            InteractiveProtocol.TYPE_COLORED_OUTPUT,
                            data);
                }
                buffer.append(text);
            } catch (IOException e) {
                logger.error("发送颜色输出失败", e);
                close();
            }
        }
    }

    private void stopPingFuture() {
        ScheduledFuture<?> pingFuture = pingFutureRef.getAndSet(null);
        if (pingFuture != null && !pingFuture.isCancelled()) {
            pingFuture.cancel(true);
        }
    }

    private void runInputPing() {
        try {
            synchronized (writeLock) {
                InteractiveProtocol.writeMessage(outputStream,
                        InteractiveProtocol.TYPE_INPUT_PING,
                        null);
            }
            logger.debug("向客户端发送INPUT_PING包");
        } catch (IOException e) {
            logger.warn("发送INPUT_PING失败", e);
        }
    }

    private void runServerPing() {
        try {
            synchronized (writeLock) {
                InteractiveProtocol.writeMessage(outputStream,
                        InteractiveProtocol.TYPE_SERVER_PING,
                        null);
            }
            logger.debug("发送心跳包");
        } catch (IOException e) {
            logger.warn("发送心跳失败", e);
        }
    }
}