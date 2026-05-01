package com.justnothing.methodsclient.executor;

import com.justnothing.methodsclient.model.ColoredSegment;
import com.justnothing.methodsclient.monitor.ClientPortManager;
import com.justnothing.methodsclient.StreamClient;
import com.justnothing.methodsclient.monitor.PerformanceMonitor;
import com.justnothing.testmodule.command.protocol.InteractiveProtocol;
import com.justnothing.testmodule.command.output.ClientRequirements;
import com.justnothing.testmodule.utils.concurrent.ThreadPoolManager;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Socket 命令行客户端。
 * <p>
 * 提供与本地 methods 服务端进行命令交互的能力，支持两种协议：
 * <ul>
 *   <li><b>文本协议</b>（Text）：普通命令行风格，通过 PrintWriter 发送命令，按行读取响应。</li>
 *   <li><b>交互式二进制协议</b>（Interactive）：使用自定义消息格式，支持能力协商、分块传输、彩色输出等高级特性。</li>
 * </ul>
 * </p>
 */
public class SocketCommandExecutor {

    private static final int CONNECT_TIMEOUT_MS = 5000;      // 连接超时
    private static final int EXEC_TIMEOUT_MS = 86400000;     // 命令执行超时（24小时）
    private static final int SOCKET_READ_TIMEOUT_MS = 30000; // Socket 读超时

    private static final StreamClient.ClientLogger logger = new StreamClient.ClientLogger();

    // ==================== 公共结果类 ====================

    public record ExecutionResult(boolean success, String output, String error) {}

    public record ColoredExecutionResult(boolean success, List<ColoredSegment> segments,
                                         String error) {

    }


    /**
     * 创建并配置到本地 methods 服务的 Socket 连接。
     */
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

    /**
     * 等待异步读取任务完成，处理超时和异常。
     *
     * @param future      异步任务
     * @param readingFlag 用于通知读取线程停止的标志
     * @return true 表示任务正常完成（未超时、无异常），false 表示超时或异常
     */
    private boolean waitForReadFuture(Future<Boolean> future, AtomicBoolean readingFlag) {
        try {
            return future.get(SocketCommandExecutor.EXEC_TIMEOUT_MS, TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            logger.error("命令执行超时（" + (long) SocketCommandExecutor.EXEC_TIMEOUT_MS + "ms）");
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

    /**
     * 记录性能指标并输出日志。
     */
    private void recordMetrics(long startTime, long bytesRead, long charsRead, boolean success) {
        long duration = System.currentTimeMillis() - startTime;
        PerformanceMonitor.recordSocketCommand(duration, bytesRead, charsRead, success);
        if (success) {
            logger.info("命令执行成功，耗时: " + duration + "ms, 读取: " + charsRead + " 字符 / " + bytesRead + " 字节");
        } else {
            logger.error("命令执行失败，耗时: " + duration + "ms");
        }
    }

    /**
     * 静默关闭 Socket，忽略异常。
     */
    private void closeSocketQuietly(Socket socket) {
        if (socket != null) {
            try {
                socket.close();
            } catch (IOException ignored) {
            }
        }
    }

    /**
     * 统一处理各类异常，记录错误并输出到控制台。
     */
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

    // ==================== 公共 API ====================

    /**
     * 执行文本协议命令，输出直接打印到控制台（不捕获）。
     *
     * @param command 要执行的命令
     * @return true 表示执行成功，false 表示失败
     */
    public boolean executeTextSocket(String command) {
        long startTime = System.currentTimeMillis();
        AtomicBoolean reading = new AtomicBoolean(true);
        AtomicLong bytesRead = new AtomicLong(0);
        AtomicLong charsRead = new AtomicLong(0);
        Socket socket = null;

        try {
            socket = createSocket();
            // 发送文本命令
            PrintWriter writer = new PrintWriter(socket.getOutputStream(), true);
            writer.println(command);
            writer.flush();
            logger.info("命令已发送，开始读取响应...");

            Socket finalSocket = socket;
            Future<Boolean> future = ThreadPoolManager.submitSocketCallable(() ->
                    SocketStreamReader.readTextProtocolStream(finalSocket.getInputStream(), reading, bytesRead, charsRead));
            boolean success = waitForReadFuture(future, reading);
            recordMetrics(startTime, bytesRead.get(), charsRead.get(), success);
            return success;
        } catch (Exception e) {
            handleException(e, startTime, bytesRead.get(), charsRead.get());
            return false;
        } finally {
            closeSocketQuietly(socket);
        }
    }

    /**
     * 执行文本协议命令，并返回输出字符串。
     *
     * @param command 要执行的命令
     * @return ExecutionResult 包含执行状态和输出内容
     */
    public ExecutionResult executeTextSocketWithOutput(String command) {
        long startTime = System.currentTimeMillis();
        AtomicBoolean reading = new AtomicBoolean(true);
        AtomicLong bytesRead = new AtomicLong(0);
        AtomicLong charsRead = new AtomicLong(0);
        StringBuilder outputBuilder = new StringBuilder();
        Socket socket = null;

        try {
            socket = createSocket();
            PrintWriter writer = new PrintWriter(socket.getOutputStream(), true);
            writer.println(command);
            writer.flush();
            logger.info("命令已发送，开始读取响应...");

            Socket finalSocket = socket;
            Future<Boolean> future = ThreadPoolManager.submitSocketCallable(() ->
                    SocketStreamReader.readTextStreamToString(finalSocket.getInputStream(), reading, bytesRead, charsRead, outputBuilder));
            boolean success = waitForReadFuture(future, reading);
            recordMetrics(startTime, bytesRead.get(), charsRead.get(), success);
            return new ExecutionResult(success, outputBuilder.toString(), "");
        } catch (Exception e) {
            handleException(e, startTime, bytesRead.get(), charsRead.get());
            return new ExecutionResult(false, "", e.getMessage());
        } finally {
            closeSocketQuietly(socket);
        }
    }

    /**
     * 执行交互式协议命令（无颜色输出，输出直接打印到控制台）。
     *
     * @param command 要执行的命令
     * @return true 表示成功，false 表示失败
     */
    public boolean executeInteractiveSocket(String command) {
        long startTime = System.currentTimeMillis();
        AtomicBoolean reading = new AtomicBoolean(true);
        AtomicLong bytesRead = new AtomicLong(0);
        Socket socket = null;

        try {
            socket = createSocket();
            // 发送能力协商
            ClientRequirements requirements = new ClientRequirements(true, false);
            InteractiveProtocol.writeMessage(socket.getOutputStream(), InteractiveProtocol.TYPE_CLIENT_CAPABILITY,
                    InteractiveProtocol.encodeCapability(requirements));
            // 发送交互式命令
            InteractiveProtocol.writeMessage(socket.getOutputStream(), InteractiveProtocol.TYPE_CLIENT_COMMAND,
                    command.getBytes(StandardCharsets.UTF_8));
            logger.info("命令已发送，开始读取响应...");

            Socket finalSocket = socket;
            Future<Boolean> future = ThreadPoolManager.submitSocketCallable(() ->
                    SocketStreamReader.readInteractiveStream(finalSocket.getInputStream(), finalSocket.getOutputStream(), reading, bytesRead, finalSocket));
            boolean success = waitForReadFuture(future, reading);
            recordMetrics(startTime, bytesRead.get(), bytesRead.get(), success);
            return success;
        } catch (Exception e) {
            handleException(e, startTime, bytesRead.get(), bytesRead.get());
            return false;
        } finally {
            closeSocketQuietly(socket);
        }
    }

    /**
     * 执行交互式协议命令，返回带颜色片段的结果。
     *
     * @param command       要执行的命令
     * @param supportsInput 是否支持用户输入（用于交互式命令）
     * @return ColoredExecutionResult 包含执行状态和彩色输出片段
     */
    public ColoredExecutionResult executeInteractiveWithColoredOutput(String command, boolean supportsInput) {
        long startTime = System.currentTimeMillis();
        AtomicBoolean reading = new AtomicBoolean(true);
        AtomicLong bytesRead = new AtomicLong(0);
        List<ColoredSegment> segments = new ArrayList<>();
        Socket socket = null;

        try {
            socket = createSocket();
            // 发送能力协商
            ClientRequirements requirements = new ClientRequirements(supportsInput, false);
            InteractiveProtocol.writeMessage(socket.getOutputStream(), InteractiveProtocol.TYPE_CLIENT_CAPABILITY,
                    InteractiveProtocol.encodeCapability(requirements));
            // 发送命令
            InteractiveProtocol.writeMessage(socket.getOutputStream(), InteractiveProtocol.TYPE_CLIENT_COMMAND,
                    command.getBytes(StandardCharsets.UTF_8));


            logger.info("命令已发送，开始读取响应...");

            Socket finalSocket = socket;
            Future<Boolean> future = ThreadPoolManager.submitSocketCallable(() ->
                    SocketStreamReader.readColoredInteractiveStream(finalSocket.getInputStream(), finalSocket.getOutputStream(), reading, bytesRead, finalSocket, segments));
            boolean success = waitForReadFuture(future, reading);
            recordMetrics(startTime, bytesRead.get(), bytesRead.get(), success);
            return new ColoredExecutionResult(success, segments, "");
        } catch (Exception e) {
            handleException(e, startTime, bytesRead.get(), bytesRead.get());
            return new ColoredExecutionResult(false, segments, e.getMessage());
        } finally {
            closeSocketQuietly(socket);
        }
    }

    /**
     * 执行命令请求并返回结果，从segments[0].text中提取内容。
     * 
     * @param requestJson 请求JSON字符串
     * @return 从segments[0].text中提取的内容
     */
    public String executeCommandRequest(String requestJson) {
        long startTime = System.currentTimeMillis();
        AtomicBoolean reading = new AtomicBoolean(true);
        AtomicLong bytesRead = new AtomicLong(0);
        List<ColoredSegment> segments = new ArrayList<>();
        Socket socket = null;

        try {
            socket = createSocket();
            // 发送能力协商
            ClientRequirements requirements = new ClientRequirements(false, false);
            InteractiveProtocol.writeMessage(socket.getOutputStream(), InteractiveProtocol.TYPE_CLIENT_CAPABILITY,
                    InteractiveProtocol.encodeCapability(requirements));
            // 发送命令请求
            InteractiveProtocol.writeMessage(socket.getOutputStream(), InteractiveProtocol.TYPE_JSON_COMMAND_REQUEST,
                    requestJson.getBytes(StandardCharsets.UTF_8));


            logger.info("命令请求已发送，开始读取响应...");

            Socket finalSocket = socket;
            Future<Boolean> future = ThreadPoolManager.submitSocketCallable(() ->
                    SocketStreamReader.readColoredInteractiveStream(finalSocket.getInputStream(), finalSocket.getOutputStream(), reading, bytesRead, finalSocket, segments));
            boolean success = waitForReadFuture(future, reading);
            recordMetrics(startTime, bytesRead.get(), bytesRead.get(), success);
            
            // 从segments[0].text中提取内容
            if (success && !segments.isEmpty()) {
                return segments.get(0).text();
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