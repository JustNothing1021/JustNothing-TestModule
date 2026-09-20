package com.justnothing.methodsclient.executor;

import com.justnothing.methodsclient.model.ColoredSegment;
import com.justnothing.methodsclient.monitor.ClientPortManager;
import com.justnothing.methodsclient.StreamClient;
import com.justnothing.methodsclient.monitor.PerformanceMonitor;
import com.justnothing.methodsclient.renderer.JsonRenderer;
import com.justnothing.methodsclient.renderer.SegmentsRenderer;
import com.justnothing.methodsclient.renderer.TerminalRenderer;
import com.justnothing.testmodule.command.framework.output.ClientRequirements;
import com.justnothing.testmodule.utils.concurrent.ThreadPoolManager;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Socket 命令行客户端。
 *
 * <p>提供统一的命令执行入口，通过 {@link Format} 枚举选择输出格式：
 * <ul>
 *   <li>{@link Format#JSON} — JSON 协议，结构化输出</li>
 *   <li>{@link Format#COLORED} — 交互式协议，带 ANSI 颜色直接渲染到本地终端</li>
 *   <li>{@link Format#PLAIN} — 交互式协议，忽略颜色渲染纯文本到本地终端</li>
 * </ul>
 * </p>
 */
public class SocketCommandExecutor {

    private static final int CONNECT_TIMEOUT_MS = 5000;
    private static final int EXEC_TIMEOUT_MS = 86400000;
    private static final int SOCKET_READ_TIMEOUT_MS = 30000;

    private static final StreamClient.ClientLogger logger = new StreamClient.ClientLogger();

    // ==================== 输出格式 ====================

    public enum Format {
        /** JSON 协议：服务端输出结构化 JSON */
        JSON,
        /** 彩色模式：按颜色映射为 ANSI 转义序列渲染到本地终端 */
        COLORED,
        /** 纯文本模式：忽略颜色，直接把输出渲染到本地终端 */
        PLAIN
    }

    // ==================== 公共结果类 ====================

    public record ExecutionResult(boolean success, String output, String error) {}

    public record ColoredExecutionResult(boolean success, List<ColoredSegment> segments, String error) {}

    // ==================== 统一执行入口 ====================

    /**
     * 执行命令，根据指定的格式选择协议和输出处理方式。
     *
     * @param command 要执行的命令
     * @param format  输出格式
     * @return 执行结果
     */
    public ExecutionResult executeWithResult(String command, Format format) {
        return switch (format) {
            case JSON -> {
                String json = executeJson(command);
                yield new ExecutionResult(json != null && !json.isEmpty(), json != null ? json : "", "");
            }
            case COLORED -> executeToTerminal(command, true);
            case PLAIN -> executeToTerminal(command, false);
        };
    }

    // ==================== 内部实现 ====================

    private Socket createSocket() throws IOException {
        int port = ClientPortManager.getSocketPort();
        Socket socket = new Socket();
        socket.setTcpNoDelay(true);
        socket.setKeepAlive(true);
        socket.setSoTimeout(SOCKET_READ_TIMEOUT_MS);
        socket.connect(new InetSocketAddress("localhost", port), CONNECT_TIMEOUT_MS);
        logger.info("创建Socket连接，端口: " + port);
        return socket;
    }

    private boolean waitForReadFuture(Future<Boolean> future, AtomicBoolean readingFlag) {
        try {
            return future.get(EXEC_TIMEOUT_MS, TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            logger.error("命令执行超时（" + EXEC_TIMEOUT_MS + "ms）");
            readingFlag.set(false);
            future.cancel(true);
            return false;
        } catch (Exception e) {
            logger.error("命令执行异常", e);
            readingFlag.set(false);
            future.cancel(true);
            return false;
        }
    }

    private void recordMetrics(long startTime, long bytesRead, long charsRead, boolean success) {
        long duration = System.currentTimeMillis() - startTime;
        PerformanceMonitor.recordSocketCommand(duration, bytesRead, charsRead, success);
        if (success) {
            logger.info("命令执行成功，耗时: " + duration + "ms, 读取: " + charsRead + " 字符 / " + bytesRead + " 字节");
        } else {
            logger.error("命令执行失败，耗时: " + duration + "ms");
        }
    }

    private void closeSocketQuietly(Socket socket) {
        if (socket != null) {
            try {
                socket.close();
            } catch (IOException ignored) {
            }
        }
    }

    private void handleException(Exception e, long startTime, long bytesRead, long charsRead) {
        recordMetrics(startTime, bytesRead, charsRead, false);
        if (e instanceof SocketTimeoutException) {
            System.err.println("连接超时");
            logger.error("连接超时");
        } else if (e instanceof IOException) {
            System.err.println("Socket连接失败: " + e.getMessage());
            logger.error("Socket连接失败: ", e);
        } else {
            System.err.println("未知错误: " + e.getMessage());
            logger.error("未知错误", e);
        }
        System.err.flush();
    }

    private static ClientRequirements buildRequirements(boolean supportsInput, boolean isJsonMode) {
        // 能力经 sys.hello RPC 握手发送
        return SocketStreamReader.buildClientRequirements(supportsInput, isJsonMode);
    }

    // ==================== 统一执行实现 ====================

    /**
     * 以交互式协议执行命令，输出直接渲染到本地终端（不捕获）。
     *
     * @param command 要执行的命令
     * @param colored true 渲染 ANSI 颜色；false 渲染纯文本
     */
    private ExecutionResult executeToTerminal(String command, boolean colored) {
        long startTime = System.currentTimeMillis();
        AtomicBoolean reading = new AtomicBoolean(true);
        AtomicLong bytesRead = new AtomicLong(0);
        Socket socket = null;

        try {
            socket = createSocket();
            ClientRequirements requirements = buildRequirements(true, false);
            logger.info("命令已发送，开始读取响应...");

            Socket finalSocket = socket;
            TerminalRenderer renderer = new TerminalRenderer(colored);
            Future<Boolean> future = ThreadPoolManager.submitSocketCallable(() ->
                    SocketStreamReader.readRpcStream(finalSocket.getInputStream(), finalSocket.getOutputStream(),
                            reading, bytesRead, finalSocket, requirements, command, null, renderer));
            boolean success = waitForReadFuture(future, reading);
            recordMetrics(startTime, bytesRead.get(), bytesRead.get(), success);
            return new ExecutionResult(success, "", "");
        } catch (Exception e) {
            handleException(e, startTime, bytesRead.get(), bytesRead.get());
            return new ExecutionResult(false, "", e.getMessage());
        } finally {
            closeSocketQuietly(socket);
        }
    }

    /**
     * 以交互式彩色协议执行命令，收集彩色片段。
     */
    public ColoredExecutionResult executeWithResult(String command, boolean supportsInput) {
        long startTime = System.currentTimeMillis();
        AtomicBoolean reading = new AtomicBoolean(true);
        AtomicLong bytesRead = new AtomicLong(0);
        SegmentsRenderer renderer = new SegmentsRenderer();
        Socket socket = null;

        try {
            socket = createSocket();
            ClientRequirements requirements = buildRequirements(supportsInput, false);
            logger.info("命令已发送，开始读取响应...");

            Socket finalSocket = socket;
            Future<Boolean> future = ThreadPoolManager.submitSocketCallable(() ->
                    SocketStreamReader.readRpcStream(finalSocket.getInputStream(), finalSocket.getOutputStream(),
                            reading, bytesRead, finalSocket, requirements, command, null, renderer));
            boolean success = waitForReadFuture(future, reading);
            recordMetrics(startTime, bytesRead.get(), bytesRead.get(), success);
            return new ColoredExecutionResult(success, renderer.getSegments(), "");
        } catch (Exception e) {
            handleException(e, startTime, bytesRead.get(), bytesRead.get());
            return new ColoredExecutionResult(false, renderer.getSegments(), e.getMessage());
        } finally {
            closeSocketQuietly(socket);
        }
    }

    /**
     * 以 JSON 命令请求模式执行。
     */
    public String executeJson(String requestJson) {
        long startTime = System.currentTimeMillis();
        AtomicBoolean reading = new AtomicBoolean(true);
        AtomicLong bytesRead = new AtomicLong(0);
        Socket socket = null;

        try {
            socket = createSocket();
            ClientRequirements requirements = buildRequirements(false, true);
            logger.info("命令请求已发送，开始读取响应...");

            Socket finalSocket = socket;
            JsonRenderer renderer = new JsonRenderer();
            Future<Boolean> future = ThreadPoolManager.submitSocketCallable(() ->
                    SocketStreamReader.readRpcStream(finalSocket.getInputStream(), finalSocket.getOutputStream(),
                            reading, bytesRead, finalSocket, requirements, null, requestJson, renderer));
            boolean success = waitForReadFuture(future, reading);
            recordMetrics(startTime, bytesRead.get(), bytesRead.get(), success);

            if (success) {
                String resultJson = renderer.getResultJson();
                return resultJson != null ? resultJson : "";
            } else {
                return "";
            }
        } catch (Exception e) {
            handleException(e, startTime, bytesRead.get(), bytesRead.get());
            return "";
        } finally {
            closeSocketQuietly(socket);
        }
    }
}