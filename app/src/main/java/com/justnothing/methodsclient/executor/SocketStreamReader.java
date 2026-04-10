package com.justnothing.methodsclient.executor;


import com.justnothing.methodsclient.StreamClient;
import com.justnothing.testmodule.command.output.Colors;
import com.justnothing.testmodule.command.output.InteractiveProtocol;
import com.justnothing.testmodule.utils.concurrent.ThreadPoolManager;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

public class SocketStreamReader {

    private static final int BUFFER_SIZE = 8192;
    private static final int SERVER_RESPONSE_TIMEOUT_MS = 30000;
    private static final long PING_SERVER_INTERVAL_MS = 5000;

    private static final StreamClient.ClientLogger logger = new StreamClient.ClientLogger();

    public static boolean readTextProtocolSocketStream(InputStream input, AtomicBoolean reading,
                                                       AtomicLong bytesRead, AtomicLong charsRead) {
        try {
            InputStreamReader reader = new InputStreamReader(input, StandardCharsets.UTF_8);
            char[] buffer = new char[BUFFER_SIZE];
            int chars;

            long lastDataTime = System.currentTimeMillis();
            boolean connectionClosedByServer = false;

            while (reading.get() && !Thread.currentThread().isInterrupted()) {
                try {
                    chars = reader.read(buffer, 0, buffer.length);

                    if (chars == -1) {
                        logger.info("服务器已关闭连接，命令执行完成");
                        connectionClosedByServer = true;
                        break;
                    }

                    if (chars > 0) {
                        System.out.print(new String(buffer, 0, chars));
                        System.out.flush();

                        bytesRead.addAndGet(chars * 2L);
                        charsRead.addAndGet(chars);
                        lastDataTime = System.currentTimeMillis();
                    }

                } catch (SocketTimeoutException e) {
                    long idleTime = System.currentTimeMillis() - lastDataTime;
                    if (idleTime > 10000) {
                        logger.debug("10秒无新数据，检查连接状态...");
                    }
                }
            }

            return connectionClosedByServer;

        } catch (IOException e) {
            if (reading.get()) {
                if (e.getMessage() != null &&
                        (e.getMessage().contains("Connection reset") ||
                                e.getMessage().contains("Socket closed") ||
                                e.getMessage().contains("stream closed"))) {
                    logger.info("连接被关闭，命令执行完成");
                    return true;
                }
                logger.error("读取流失败: ", e);
            }
            return false;
        } finally {
            reading.set(false);
        }
    }

    public static boolean readInteractiveSocketStream(InputStream input, OutputStream output,
                                                       AtomicBoolean reading, AtomicLong bytesRead,
                                                       Socket socket) {
        try {
            AtomicLong lastResponseTime = new AtomicLong(System.currentTimeMillis());
            BlockingQueue<String> inputQueue = new LinkedBlockingQueue<>();
            Object writeLock = new Object();

            startConsoleReaderThread(reading, inputQueue);
            startPingThread(output, lastResponseTime, writeLock);

            return runMainLoop(input, output, reading, bytesRead, socket, lastResponseTime, inputQueue, writeLock);

        } catch (IOException e) {
            logger.error("读取流失败", e);
            System.err.println("读取流失败: " + e.getMessage());
            return false;
        } finally {
            reading.set(false);
        }
    }

    private static void startConsoleReaderThread(AtomicBoolean reading, BlockingQueue<String> inputQueue) {
        ThreadPoolManager.submitSocketRunnable(() -> {
            BufferedReader consoleReader = new BufferedReader(
                    new InputStreamReader(System.in, StandardCharsets.UTF_8));

            try {
                while (reading.get() && !Thread.currentThread().isInterrupted()) {
                    if (consoleReader.ready()) {
                        String line = consoleReader.readLine();
                        if (line != null) {
                            inputQueue.offer(line);
                        }
                    } else {
                        Thread.sleep(100);
                    }
                }
            } catch (Exception ignored) {
            }
        });
    }

    private static void startPingThread(OutputStream output,
                                        AtomicLong lastResponseTime, Object writeLock) {
        ThreadPoolManager.submitSocketRunnable(() -> {
            while (!Thread.currentThread().isInterrupted() &&
                    (System.currentTimeMillis() - lastResponseTime.get()) < SERVER_RESPONSE_TIMEOUT_MS) {
                try {
                    Thread.sleep(PING_SERVER_INTERVAL_MS);
                    synchronized (writeLock) {
                        InteractiveProtocol.writeMessage(output,
                                InteractiveProtocol.TYPE_CLIENT_PING,
                                null);
                    }
                    logger.debug("向服务端发送CLIENT_PING包");

                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (IOException e) {
                    logger.warn("发送CLIENT_PING失败", e);
                    break;
                }
            }
        });
    }

    private static boolean runMainLoop(InputStream input, OutputStream output, AtomicBoolean reading,
                                        AtomicLong bytesRead, Socket socket, AtomicLong lastResponseTime,
                                        BlockingQueue<String> inputQueue, Object writeLock) throws IOException {
        while (reading.get() && !Thread.currentThread().isInterrupted()) {
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

                Boolean result = handlePacket(type, data, output, bytesRead, inputQueue, reading, writeLock);
                if (result != null) {
                    return result;
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
    }

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

    private static Boolean handlePacket(byte type, byte[] data, OutputStream output,
                                         AtomicLong bytesRead, BlockingQueue<String> inputQueue,
                                         AtomicBoolean reading, Object writeLock) {
        switch (type) {
            case InteractiveProtocol.TYPE_SERVER_OUTPUT:
                handleServerOutput(data, bytesRead);
                return null;

            case InteractiveProtocol.TYPE_SERVER_ERROR:
                handleServerError(data, bytesRead);
                return null;

            case InteractiveProtocol.TYPE_COLORED_OUTPUT:
                handleColoredOutput(data, bytesRead);
                return null;

            case InteractiveProtocol.TYPE_SERVER_INPUT_REQUEST:
                handleInputRequest(data, output, inputQueue, reading);
                return null;

            case InteractiveProtocol.TYPE_SERVER_PING:
                handleServerPing(output, writeLock);
                return null;

            case InteractiveProtocol.TYPE_SERVER_PONG:
                logger.debug("收到了服务端的PONG");
                return null;

            case InteractiveProtocol.TYPE_INPUT_PING:
                handleInputPing(output);
                return null;

            case InteractiveProtocol.TYPE_COMMAND_END:
                logger.info("收到COMMAND_END标记，退出程序");
                System.out.println();
                return true;

            default:
                logger.warn("未知的消息类型: " + type);
                return null;
        }
    }

    private static void handleServerOutput(byte[] data, AtomicLong bytesRead) {
        if (data != null) {
            String text = new String(data, StandardCharsets.UTF_8);
            System.out.print(text);
            System.out.flush();
            bytesRead.addAndGet(data.length);
        }
    }

    private static void handleServerError(byte[] data, AtomicLong bytesRead) {
        if (data != null) {
            String text = new String(data, StandardCharsets.UTF_8);
            System.err.print(text);
            System.err.flush();
            bytesRead.addAndGet(data.length);
        }
    }

    private static void handleColoredOutput(byte[] data, AtomicLong bytesRead) {
        if (data == null || data.length == 0) {
            return;
        }
        
        Object[] decoded = InteractiveProtocol.decodeColoredOutput(data);
        byte color = (byte) decoded[0];
        String text = (String) decoded[1];
        
        printAsANSI(color, text);
        bytesRead.addAndGet(data.length);
    }

    private static void printAsANSI(byte color, String text) {
        String ansiCode = getANSICode(color);
        String resetCode = "\u001B[0m";
        
        System.out.print(ansiCode + text + resetCode);
        System.out.flush();
    }

    private static String getANSICode(byte color) {
        return switch (color) {
            case Colors.BLACK -> "\u001B[30m";
            case Colors.RED -> "\u001B[31m";
            case Colors.GREEN -> "\u001B[32m";
            case Colors.YELLOW -> "\u001B[33m";
            case Colors.BLUE -> "\u001B[34m";
            case Colors.MAGENTA -> "\u001B[35m";
            case Colors.CYAN -> "\u001B[36m";
            case Colors.WHITE -> "\u001B[37m";
            case Colors.GRAY -> "\u001B[38;5;245m";
            case Colors.LIGHT_GRAY -> "\u001B[90m";
            case Colors.LIGHT_RED -> "\u001B[91m";
            case Colors.LIGHT_GREEN -> "\u001B[92m";
            case Colors.LIGHT_YELLOW -> "\u001B[93m";
            case Colors.LIGHT_BLUE -> "\u001B[94m";
            case Colors.LIGHT_MAGENTA -> "\u001B[95m";
            case Colors.LIGHT_CYAN -> "\u001B[96m";
            case Colors.DARK_GRAY -> "\u001B[90m";
            case Colors.ORANGE -> "\u001B[38;5;208m";
            case Colors.PINK -> "\u001B[38;5;218m";
            case Colors.BROWN -> "\u001B[38;5;130m";
            case Colors.GOLD -> "\u001B[38;5;220m";
            case Colors.SILVER -> "\u001B[38;5;250m";
            case Colors.LIME -> "\u001B[38;5;154m";
            case Colors.TEAL -> "\u001B[38;5;37m";
            case Colors.NAVY -> "\u001B[38;5;17m";
            case Colors.MAROON -> "\u001B[38;5;124m";
            case Colors.OLIVE -> "\u001B[38;5;142m";
            case Colors.AQUA -> "\u001B[38;5;87m";
            case Colors.CORAL -> "\u001B[38;5;209m";
            case Colors.SALMON -> "\u001B[38;5;210m";
            case Colors.INDIGO -> "\u001B[38;5;93m";
            case Colors.VIOLET -> "\u001B[38;5;177m";
            default -> "";
        };
    }

    private static void handleInputRequest(byte[] data, OutputStream output,
                                            BlockingQueue<String> inputQueue, AtomicBoolean reading) {
        ThreadPoolManager.submitFastRunnable(() -> {
            if (data == null) return;
            
            try {
                String request = new String(data, StandardCharsets.UTF_8);
                String[] parts = request.split(":", 2);
                if (parts.length != 2) return;

                String requestId = parts[0];
                String prompt = parts[1];

                boolean isPassword = prompt.startsWith("PASSWORD:");
                if (isPassword) {
                    prompt = prompt.substring(9);
                }

                System.out.print(prompt);
                System.out.flush();

                String userInput = waitForUserInput(inputQueue, reading);
                if (userInput == null) {
                    userInput = "";
                }

                String response = requestId + ":" + userInput;
                InteractiveProtocol.writeMessage(output,
                        InteractiveProtocol.TYPE_INPUT_RESPONSE,
                        response.getBytes(StandardCharsets.UTF_8));

            } catch (Exception e) {
                logger.error("处理输入请求时出错", e);
                System.err.println("处理输入请求时出错: " + e);
            }
        });
    }

    private static String waitForUserInput(BlockingQueue<String> inputQueue, AtomicBoolean reading) 
            throws InterruptedException {
        String userInput = null;
        while (reading.get()) {
            userInput = inputQueue.poll(100, TimeUnit.MILLISECONDS);
            if (userInput != null) break;
        }
        return userInput;
    }

    private static void handleServerPing(OutputStream output, Object writeLock) {
        try {
            synchronized (writeLock) {
                InteractiveProtocol.writeMessage(output, InteractiveProtocol.TYPE_CLIENT_PONG, null);
            }
            logger.debug("接收到了服务端的PING，发送PONG响应");
        } catch (IOException e) {
            logger.error("发送CLIENT_PONG响应失败", e);
        }
    }

    private static void handleInputPing(OutputStream output) {
        logger.info("收到INPUT_PING，返回INPUT_PONG");
        try {
            InteractiveProtocol.writeMessage(output, InteractiveProtocol.TYPE_INPUT_PONG, null);
        } catch (IOException e) {
            logger.error("发送INPUT_PONG响应失败", e);
        }
    }

    public static boolean readSocketStreamToString(InputStream input, AtomicBoolean reading,
                                                     AtomicLong bytesRead, AtomicLong charsRead,
                                                     StringBuilder outputBuilder) {
        try {
            InputStreamReader reader = new InputStreamReader(input, StandardCharsets.UTF_8);
            char[] buffer = new char[BUFFER_SIZE];
            int chars;

            long lastDataTime = System.currentTimeMillis();
            boolean connectionClosedByServer = false;

            while (reading.get() && !Thread.currentThread().isInterrupted()) {
                try {
                    chars = reader.read(buffer, 0, buffer.length);

                    if (chars == -1) {
                        logger.info("服务器已关闭连接，命令执行完成");
                        connectionClosedByServer = true;
                        break;
                    }

                    if (chars > 0) {
                        String text = new String(buffer, 0, chars);
                        outputBuilder.append(text);

                        bytesRead.addAndGet(chars * 2L);
                        charsRead.addAndGet(chars);
                        lastDataTime = System.currentTimeMillis();
                    }

                } catch (SocketTimeoutException e) {
                    long idleTime = System.currentTimeMillis() - lastDataTime;
                    if (idleTime > 10000) {
                        logger.debug("10秒无新数据，检查连接状态...");
                    }
                }
            }

            return connectionClosedByServer;

        } catch (IOException e) {
            if (reading.get()) {
                if (e.getMessage() != null &&
                        (e.getMessage().contains("Connection reset") ||
                                e.getMessage().contains("Socket closed") ||
                                e.getMessage().contains("stream closed"))) {
                    logger.info("连接被关闭，命令执行完成");
                    return true;
                }
                logger.error("读取流失败: ", e);
            }
            return false;
        } finally {
            reading.set(false);
        }
    }

    public static boolean readInteractiveWithColoredOutput(InputStream input, OutputStream output,
                                                            AtomicBoolean reading, AtomicLong bytesRead,
                                                            Socket socket, List<ColoredSegment> segments) {
        try {
            AtomicLong lastResponseTime = new AtomicLong(System.currentTimeMillis());
            BlockingQueue<String> inputQueue = new LinkedBlockingQueue<>();
            Object writeLock = new Object();

            startPingThread(output, lastResponseTime, writeLock);

            while (reading.get() && !Thread.currentThread().isInterrupted()) {
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

                    Boolean result = handlePacketForColoredOutput(type, data, output, bytesRead, writeLock, segments);
                    if (result != null) {
                        return result;
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

    private static Boolean handlePacketForColoredOutput(byte type, byte[] data, OutputStream output,
                                                         AtomicLong bytesRead,
                                                        Object writeLock,
                                                         List<ColoredSegment> segments) {
        return switch (type) {
            case InteractiveProtocol.TYPE_SERVER_OUTPUT -> {
                if (data != null) {
                    String text = new String(data, StandardCharsets.UTF_8);
                    segments.add(new ColoredSegment(Colors.DEFAULT, text));
                    bytesRead.addAndGet(data.length);
                }
                yield null;
            }
            case InteractiveProtocol.TYPE_SERVER_ERROR -> {
                if (data != null) {
                    String text = new String(data, StandardCharsets.UTF_8);
                    segments.add(new ColoredSegment(Colors.RED, text));
                    bytesRead.addAndGet(data.length);
                }
                yield null;
            }
            case InteractiveProtocol.TYPE_COLORED_OUTPUT -> {
                if (data != null && data.length > 0) {
                    Object[] decoded = InteractiveProtocol.decodeColoredOutput(data);
                    byte color = (byte) decoded[0];
                    String text = (String) decoded[1];
                    segments.add(new ColoredSegment(color, text));
                    bytesRead.addAndGet(data.length);
                }
                yield null;
            }
            case InteractiveProtocol.TYPE_SERVER_PING -> {
                try {
                    synchronized (writeLock) {
                        InteractiveProtocol.writeMessage(output, InteractiveProtocol.TYPE_CLIENT_PONG, null);
                    }
                    logger.debug("接收到了服务端的PING，发送PONG响应");
                } catch (IOException e) {
                    logger.error("发送CLIENT_PONG响应失败", e);
                }
                yield null;
            }
            case InteractiveProtocol.TYPE_SERVER_PONG -> {
                logger.debug("收到了服务端的PONG");
                yield null;
            }
            case InteractiveProtocol.TYPE_COMMAND_END -> {
                logger.info("收到COMMAND_END标记，退出程序");
                yield true;
            }
            default -> {
                logger.warn("未知的消息类型: " + type);
                yield null;
            }
        };
    }
}
