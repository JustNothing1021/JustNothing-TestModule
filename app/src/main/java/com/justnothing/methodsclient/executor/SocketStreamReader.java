package com.justnothing.methodsclient.executor;


import com.justnothing.methodsclient.StreamClient;
import com.justnothing.testmodule.service.protocol.InteractiveProtocol;
import com.justnothing.testmodule.utils.concurrent.ThreadPoolManager;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

public class SocketStreamReader {

    private static final int BUFFER_SIZE = 8192;
    private static final int SERVER_RESPONSE_TIMEOUT = 30000;
    private static final long PING_SERVER_INTERVAL = 5000;

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
            startPingThread(reading, output, lastResponseTime, writeLock);

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

    private static void startPingThread(AtomicBoolean reading, OutputStream output,
                                         AtomicLong lastResponseTime, Object writeLock) {
        ThreadPoolManager.submitSocketRunnable(() -> {
            while (!Thread.currentThread().isInterrupted() &&
                    (System.currentTimeMillis() - lastResponseTime.get()) < SERVER_RESPONSE_TIMEOUT) {
                try {
                    Thread.sleep(PING_SERVER_INTERVAL);
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
        if (elapsed > SERVER_RESPONSE_TIMEOUT) {
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
}
