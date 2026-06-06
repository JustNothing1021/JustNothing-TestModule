package com.justnothing.methodsclient.executor;


import com.justnothing.methodsclient.StreamClient;
import com.justnothing.methodsclient.model.ColoredSegment;
import com.justnothing.methodsclient.utils.TerminalManager;
import com.justnothing.methodsclient.highlighter.HighlighterManager;
import com.justnothing.methodsclient.tui.TuiManager;
import com.justnothing.methodsclient.tui.widget.TuiWidgetData;
import com.justnothing.testmodule.command.output.InputMode;
import com.justnothing.testmodule.command.output.Colors;
import com.justnothing.testmodule.command.protocol.InteractiveProtocol;
import com.justnothing.testmodule.utils.concurrent.ThreadPoolManager;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;


public class SocketStreamReader {

    private static final int BUFFER_SIZE = 8192;
    private static final int SERVER_RESPONSE_TIMEOUT_MS = 30000;
    private static final long PING_SERVER_INTERVAL_MS = 5000;


    private static final StreamClient.ClientLogger logger = new StreamClient.ClientLogger();

    /**
     * TUI 管理器引用（由 ReplClient 在启动时设置）。
     * Socket 收到 TUI Widget 消息时通过此引用驱动组件更新。
     */
    private static volatile TuiManager tuiManager = null;

    /** 设置 TUI 管理器（通常由 ReplClient 启动时调用） */
    public static void setTuiManager(TuiManager manager) { tuiManager = manager; }

    /** 获取当前 TUI 管理器 */
    public static TuiManager getTuiManager() { return tuiManager; }


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
            InteractiveProtocol.writeMessage(output,
                    InteractiveProtocol.TYPE_INPUT_PONG, null);
        } catch (IOException e) {
            logger.error("发送INPUT_PONG响应失败", e);
        }
    }

    /**
     * 处理服务端发来的高亮模式切换请求。
     * data 内容为 {@link InputMode} 中定义的模式名称（UTF-8），如 "java"、"command"、"none"
     */
    private static void handleSetHighlightMode(byte[] data) {
        if (data == null || data.length == 0) {
            logger.warn("收到 SET_HIGHLIGHT_MODE 但数据为空");
            return;
        }
        String modeName = new String(data, StandardCharsets.UTF_8);
        boolean success = HighlighterManager.switchMode(modeName);
        if (success) {
            logger.info("高亮模式已切换: " + modeName + " (" + InputMode.getDescription(modeName) + ")");
        } else {
            logger.warn("未知的高亮模式: " + modeName +
                    " (预定义模式: " + String.join(", ", InputMode.allModes()) + ")");
        }
    }

    // ==================== TUI Widget 消息处理 ====================

    /**
     * 处理 TYPE_TUI_WIDGET_CREATE：创建新组件
     */
    private static void handleTuiWidgetCreate(byte[] data) {
        if (tuiManager == null || !tuiManager.isRunning()) return;
        try {
            TuiWidgetData widgetData = TuiWidgetData.fromJsonBytes(data);
            tuiManager.handleWidgetCreate(widgetData);
        } catch (Exception e) {
            logger.error("解析 TUI_WIDGET_CREATE 数据失败", e);
        }
    }

    /**
     * 处理 TYPE_TUI_WIDGET_UPDATE：更新组件
     */
    private static void handleTuiWidgetUpdate(byte[] data) {
        if (tuiManager == null || !tuiManager.isRunning()) return;
        try {
            TuiWidgetData widgetData = TuiWidgetData.fromJsonBytes(data);
            tuiManager.handleWidgetUpdate(widgetData);
        } catch (Exception e) {
            logger.error("解析 TUI_WIDGET_UPDATE 数据失败", e);
        }
    }

    /**
     * 处理 TYPE_TUI_WIDGET_DESTROY：销毁组件
     */
    private static void handleTuiWidgetDestroy(byte[] data) {
        if (tuiManager == null || !tuiManager.isRunning()) return;
        if (data != null && data.length > 0) {
            String widgetId = new String(data, StandardCharsets.UTF_8);
            tuiManager.handleWidgetDestroy(widgetId);
        }
    }

    /**
     * 处理 TYPE_TUI_CLEAR_ALL：清除所有组件（命令结束时）
     */
    private static void handleTuiClearAll() {
        if (tuiManager == null || !tuiManager.isRunning()) return;
        tuiManager.clearAllWidgets();
    }



    private static void startPingThread(OutputStream output, AtomicLong lastResponseTime, Object writeLock,
                                        AtomicBoolean reading) {
        ThreadPoolManager.scheduleWithFixedDelayUntil(
            () -> {
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
                } catch (IOException e) {
                    logger.warn("发送CLIENT_PING失败", e);
                }
            },
            0, PING_SERVER_INTERVAL_MS, TimeUnit.MILLISECONDS,
            () -> Thread.currentThread().isInterrupted()
                || System.currentTimeMillis() - lastResponseTime.get() < SERVER_RESPONSE_TIMEOUT_MS
                || !reading.get()
        );
    }

    // ==================== 文本协议读取 ====================

    public static boolean readTextProtocolStream(InputStream input, AtomicBoolean reading,
                                                 AtomicLong bytesRead, AtomicLong charsRead) {
        return readSocketStreamInternal(input, reading, bytesRead, charsRead, null);
    }

    public static boolean readTextStreamToString(InputStream input, AtomicBoolean reading,
                                                 AtomicLong bytesRead, AtomicLong charsRead,
                                                 StringBuilder outputBuilder) {
        return readSocketStreamInternal(input, reading, bytesRead, charsRead, outputBuilder);
    }

    private static boolean readSocketStreamInternal(InputStream input, AtomicBoolean reading, 
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
                        
                        if (outputBuilder != null) {
                            outputBuilder.append(text);
                        } else {
                            System.out.print(text);
                            System.out.flush();
                        }

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

    public static boolean readInteractiveStream(InputStream input, OutputStream output,
                                                AtomicBoolean reading, AtomicLong bytesRead,
                                                Socket socket) {
        try {
            AtomicLong lastResponseTime = new AtomicLong(System.currentTimeMillis());
            Object writeLock = new Object();

            startPingThread(output, lastResponseTime, writeLock, reading);

            return runInteractiveMainLoop(input, output, reading, bytesRead, socket, 
                                        lastResponseTime, writeLock, null);

        } catch (IOException e) {
            logger.error("读取流失败", e);
            System.err.println("读取流失败: " + e.getMessage());
            return false;
        } finally {
            reading.set(false);
        }
    }

    public static boolean readColoredInteractiveStream(InputStream input, OutputStream output,
                                                       AtomicBoolean reading, AtomicLong bytesRead,
                                                       Socket socket, List<ColoredSegment> segments) {
        try {
            AtomicLong lastResponseTime = new AtomicLong(System.currentTimeMillis());
            Object writeLock = new Object();

            startPingThread(output, lastResponseTime, writeLock, reading);

            return runInteractiveMainLoop(input, output, reading, bytesRead, socket, 
                                        lastResponseTime, writeLock, segments);

        } catch (IOException e) {
            logger.error("读取流失败", e);
            return false;
        } finally {
            reading.set(false);
        }
    }

    private static boolean runInteractiveMainLoop(InputStream input, OutputStream output, 
                                                AtomicBoolean reading, AtomicLong bytesRead, 
                                                Socket socket, AtomicLong lastResponseTime,
                                                  Object writeLock, List<ColoredSegment> segments) throws IOException {
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
//                logger.debug("处理包: " + InteractiveProtocol.getMessageTypeName(type));
                Boolean result = handleInteractivePacket(type, data, output, bytesRead,
                        reading, writeLock, segments);
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

    private static Boolean handleInteractivePacket(byte type, byte[] data, OutputStream output, 
                                                 AtomicLong bytesRead, AtomicBoolean reading, Object writeLock,
                                                 List<ColoredSegment> segments) {
        switch (type) {
            case InteractiveProtocol.TYPE_SERVER_OUTPUT:
                handleServerOutput(data, bytesRead, segments);
                return null;

            case InteractiveProtocol.TYPE_SERVER_ERROR:
                handleServerError(data, bytesRead, segments);
                return null;

            case InteractiveProtocol.TYPE_COLORED_OUTPUT:
                handleColoredOutput(data, bytesRead, segments);
                return null;

            case InteractiveProtocol.TYPE_SERVER_INPUT_REQUEST:
                handleInputRequest(data, output);
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

            case InteractiveProtocol.TYPE_SET_HIGHLIGHT_MODE:
                handleSetHighlightMode(data);
                return null;

            case InteractiveProtocol.TYPE_TUI_WIDGET_CREATE:
                handleTuiWidgetCreate(data);
                return null;

            case InteractiveProtocol.TYPE_TUI_WIDGET_UPDATE:
                handleTuiWidgetUpdate(data);
                return null;

            case InteractiveProtocol.TYPE_TUI_WIDGET_DESTROY:
                handleTuiWidgetDestroy(data);
                return null;

            case InteractiveProtocol.TYPE_TUI_CLEAR_ALL:
                handleTuiClearAll();
                return null;

            case InteractiveProtocol.TYPE_COMMAND_END:
                logger.info("收到COMMAND_END标记，退出程序");
                if (segments == null) {
                    System.out.println();
                }
                reading.set(false);
                return true;

            default:
                logger.warn("未知的消息类型: " + type);
                return null;
        }
    }

    // ==================== 输出处理 ====================

    private static void handleServerOutput(byte[] data, AtomicLong bytesRead, List<ColoredSegment> segments) {
        if (data != null) {
            String text = new String(data, StandardCharsets.UTF_8);
            
            if (segments != null) {
                segments.add(new ColoredSegment(Colors.DEFAULT, text));
            }
            
            printAsANSI(Colors.DEFAULT, text);
            bytesRead.addAndGet(data.length);
        }
    }

    private static void handleServerError(byte[] data, AtomicLong bytesRead, List<ColoredSegment> segments) {
        if (data != null) {
            String text = new String(data, StandardCharsets.UTF_8);
            
            if (segments != null) {
                segments.add(new ColoredSegment(Colors.RED, text));
            }
            
            printAsANSI(Colors.RED, text);
            bytesRead.addAndGet(data.length);
        }
    }

    private static void handleColoredOutput(byte[] data, AtomicLong bytesRead, List<ColoredSegment> segments) {
        if (data == null || data.length == 0) {
            return;
        }

        Object[] decoded = InteractiveProtocol.decodeColoredOutput(data);
        byte color = (byte) decoded[0];
        String text = (String) decoded[1];
        
        if (segments != null) {
            segments.add(new ColoredSegment(color, text));
        }
        
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

    // ==================== 输入处理 ====================


    private static void handleInputRequest(byte[] data, OutputStream output) {
        ThreadPoolManager.submitFastRunnable(() -> {
            logger.debug("尝试调起用户输入");
            if (data == null) {
                logger.warn("处理输入请求时遇到请求内容为null");
            }
            String request = new String(data, StandardCharsets.UTF_8);
            String[] parts = request.split(":", 2);
            if (parts.length != 2)  {
                logger.info("处理输入请求时请求内容格式不正确; 没有提供请求id和/或prompt");
                return;
            }
            String requestId = parts[0];
            String prompt = parts[1];
            boolean isPassword = prompt.startsWith("PASSWORD:");
            if (isPassword) {
                prompt = prompt.substring(9);
            }
            try {
                System.out.flush();

                String userInput;
                if (isPassword) {
                    userInput = TerminalManager.readLine(prompt, '*');
                } else {
                    userInput = TerminalManager.readLine(prompt);
                }


                String response = requestId + ":" + (userInput == null ? "" : userInput);
                InteractiveProtocol.writeMessage(output,
                        InteractiveProtocol.TYPE_INPUT_RESPONSE,
                        response.getBytes(StandardCharsets.UTF_8));
            } catch (Exception e) {
                logger.error("处理输入请求时出错", e);
            }
        });
    }


}
