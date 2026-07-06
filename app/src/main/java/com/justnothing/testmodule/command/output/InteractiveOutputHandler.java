package com.justnothing.testmodule.command.output;

import androidx.annotation.NonNull;

import com.google.gson.JsonObject;
import com.google.gson.JsonArray;
import com.justnothing.testmodule.command.output.InputMode;
import com.justnothing.testmodule.command.protocol.InteractiveProtocol;
import com.justnothing.testmodule.utils.logging.Logger;
import com.justnothing.testmodule.utils.concurrent.ThreadPoolManager;

import java.io.IOException;
import java.io.InputStream;
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

import com.justnothing.richconsole.console.Console;
import com.justnothing.testmodule.command.protocol.TerminalRpcChannel;
import org.jline.terminal.Size;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;


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
    private volatile boolean isJsonMode = false;
    private volatile String command;

    // 客户端终端信息
    private volatile int clientWidth;
    private volatile int clientHeight;
    private volatile boolean clientSupportsAnsi;
    private volatile byte clientColorSystem;

    // ExternalTerminal + Protocol 流适配
    private TerminalRpcChannel rpcChannel;
    private RemoteServerTerminal remoteTerminal;

    // Console 实例（RichConsole 渲染用，基于 ExternalTerminal）
    private volatile Console console;

    public InteractiveOutputHandler(OutputStream outputStream) {
        this.outputStream = outputStream;
    }

    public void setSupportsInput(boolean supportsInput) {
        if (!isJsonMode) {
            this.supportsInput = supportsInput;
        }
    }

    public boolean isSupportsInput() {
        return supportsInput && !isJsonMode;
    }
    
    public void setJsonMode(boolean jsonMode) {
        this.isJsonMode = jsonMode;
        if (jsonMode) {
            this.supportsInput = false;
        }
    }
    
    public boolean isJsonMode() {
        return isJsonMode;
    }
    
    public void setCommand(String command) {
        this.command = command;
    }
    
    public String getCommand() {
        return command;
    }

    /**
     * 设置客户端终端信息（由 SocketClientHandler 在能力协商后调用）。
     *
     * <p>仅设置客户端信息字段（宽度、高度、ANSI 支持、颜色系统），
     * 不创建 RemoteServerTerminal。交互模式需额外调用 {@link #initRemoteTerminal()}。</p>
     */
    public void setClientTerminalInfo(int width, int height, boolean supportsAnsi, byte colorSystem) {
        this.clientWidth = width;
        this.clientHeight = height;
        this.clientSupportsAnsi = supportsAnsi;
        this.clientColorSystem = colorSystem;
        if (remoteTerminal != null) {
            remoteTerminal.updateCachedSize(width, height);
        }
    }

    /**
     * 初始化 RemoteServerTerminal（交互模式专用）。
     *
     * <p>创建 TerminalRpcChannel + RemoteServerTerminal，
     * 使 Console 输出通过 JSON-RPC 通道发送给客户端终端。
     * 文件模式（JSON 命令请求）不需要调用此方法，
     * {@link #getConsole()} 会自动创建基于 System.out 的 fallback Console。</p>
     */
    public void initRemoteTerminal() {
        if (remoteTerminal != null) return;
        try {
            rpcChannel = new TerminalRpcChannel(outputStream, writeLock);
            String termType = clientSupportsAnsi ? "xterm-256color" : Terminal.TYPE_DUMB;
            remoteTerminal = new RemoteServerTerminal(rpcChannel, termType, clientWidth, clientHeight);
            logger.info("RemoteServerTerminal 创建成功: type=" + termType + ", size=" + clientWidth + "x" + clientHeight);
        } catch (Exception e) {
            logger.error("创建 RemoteServerTerminal 失败，将使用 fallback Console", e);
        }
    }

    /**
     * 获取 RemoteServerTerminal 实例（供 JLine LineReader 等使用）
     */
    public RemoteServerTerminal getRemoteTerminal() {
        return remoteTerminal;
    }

    /**
     * 获取 Console 实例（供命令代码使用 RichConsole 渲染）。
     *
     * <p>有两种路径：</p>
     * <ul>
     *   <li><b>交互模式</b>：Console 基于 RemoteServerTerminal，渲染结果通过 JSON-RPC 通道发送</li>
     *   <li><b>文件模式/JSON 模式</b>：Fallback Console 基于 System.out-backed DumbTerminal + noColor，
     *       输出经 SystemOutputRedirector 自动转发到 OutputHandler</li>
     * </ul>
     */
    @Override
    public Console getConsole() {
        if (console == null && remoteTerminal != null) {
            // 交互模式：通过 RPC 通道发送
            console = Console.of(c -> c
                    .withTerminal(remoteTerminal)
                    .withForceTerminal(true)
                    .withColorSystem(mapColorSystem(clientColorSystem))
            );
        }
        if (console == null && remoteTerminal == null) {
            // 文件模式：通过 System.out (已被 SystemOutputRedirector 捕获) 发送，禁用 ANSI
            console = createFallbackConsole();
        }
        return console;
    }

    /**
     * 创建 fallback Console：基于 System.out 的 DumbTerminal + noColor。
     *
     * <p>在 CommandExecutor 执行期间，System.out 已被 SystemOutputRedirector 重定向到
     * 本 InteractiveOutputHandler，所以 Console 的输出会自动走 TYPE_SERVER_OUTPUT 协议。</p>
     */
    private Console createFallbackConsole() {
        try {
            // System.in 是 system_server 的 stdin，不可用；
            // 用一个读取就抛异常的 InputStream，因为文件模式不需要输入
            InputStream noInput = new InputStream() {
                @Override
                public int read() {
                    throw new UnsupportedOperationException("文件模式不支持终端输入");
                }
                @Override
                public int available() {
                    return 0;
                }
            };
            Terminal dumbTerminal = TerminalBuilder.builder()
                .system(false)
                .dumb(true)
                .streams(noInput, System.out)
                .type(Terminal.TYPE_DUMB)
                .build();
            Console c = Console.of(cfg -> cfg
                .withTerminal(dumbTerminal)
                .withNoColor(true)
                .withWidth(clientWidth > 0 ? clientWidth : null)
                .withHeight(clientHeight > 0 ? clientHeight : null)
            );
            logger.info("已创建 fallback Console (System.out + noColor), size=" + clientWidth + "x" + clientHeight);
            return c;
        } catch (Exception e) {
            logger.error("创建 fallback Console 失败", e);
            return null;
        }
    }

    public int getClientWidth() { return clientWidth; }
    public int getClientHeight() { return clientHeight; }
    public boolean isClientSupportsAnsi() { return clientSupportsAnsi; }

    /**
     * 将客户端上报的 colorSystem byte 映射为 Console 的颜色系统名称。
     * 0=NONE, 1=STANDARD(16色), 2=EIGHT_BIT(256色), 3=TRUECOLOR(真彩色)
     */
    private static String mapColorSystem(byte colorSystem) {
        switch (colorSystem) {
            case ClientRequirements.COLOR_TRUECOLOR: return "truecolor";
            case ClientRequirements.COLOR_EIGHT_BIT: return "256";
            case ClientRequirements.COLOR_STANDARD: return "standard";
            default: return null; // auto
        }
    }



    /**
     * 处理 TYPE_TERMINAL_RPC 帧数据（由 SocketClientHandler 调用）
     */
    public void handleTerminalRpc(byte[] data) {
        if (rpcChannel != null) {
            rpcChannel.handleMessage(data);
        }
    }

    /**
     * 向客户端发送交互式提示请求，等待客户端响应。
     * 通过 RPC call 实现请求-响应模式。
     *
     * @param promptType 提示类型: "input" | "confirm" | "list" | "checkbox"
     * @param title 提示标题
     * @param options 选项列表（list/checkbox 用），可为 null
     * @param defaultValue 默认值，可为 null
     * @param timeoutSeconds 超时时间（秒）
     * @return PromptResult 包含取消状态、值、选中索引
     */
    public PromptResult promptClient(String promptType, String title, String[] options,
                                     String defaultValue, int timeoutSeconds) {
        if (closed.get() || rpcChannel == null) {
            return PromptResult.cancelled();
        }

        try {
            // 构造请求参数
            JsonObject params = new JsonObject();
            params.addProperty("type", promptType);
            if (title != null) params.addProperty("title", title);
            if (options != null && options.length > 0) {
                JsonArray arr = new JsonArray();
                for (String opt : options) arr.add(opt);
                params.add("options", arr);
            }
            if (defaultValue != null) params.addProperty("defaultValue", defaultValue);

            logger.debug("发送 promptRequest RPC: " + promptType);
            JsonObject result = rpcChannel.call("promptRequest", params, timeoutSeconds * 1000L);

            if (result != null) {
                logger.debug("收到 promptRequest 响应");
                return PromptResult.fromJsonObject(result);
            }

            logger.warn("Prompt 请求超时: " + promptType);
            return PromptResult.cancelled();

        } catch (Exception e) {
            logger.error("Prompt 请求异常: " + promptType, e);
            return PromptResult.cancelled();
        }
    }

    /**
     * Prompt 结果封装
     */
    public static class PromptResult {
        public final boolean cancelled;
        public final String value;
        public final int[] selectedIndices;

        private PromptResult(boolean cancelled, String value, int[] selectedIndices) {
            this.cancelled = cancelled;
            this.value = value;
            this.selectedIndices = selectedIndices;
        }

        public static PromptResult cancelled() {
            return new PromptResult(true, null, null);
        }

        /** 从 RPC 响应 JsonObject 解析结果 */
        public static PromptResult fromJsonObject(JsonObject obj) {
            boolean cancelled = obj.has("cancelled") && obj.get("cancelled").getAsBoolean();
            String value = obj.has("value") && !obj.get("value").isJsonNull()
                    ? obj.get("value").getAsString() : null;
            int[] indices = null;
            if (obj.has("selectedIndices") && obj.get("selectedIndices").isJsonArray()) {
                JsonArray arr = obj.getAsJsonArray("selectedIndices");
                indices = new int[arr.size()];
                for (int i = 0; i < arr.size(); i++) {
                    indices[i] = arr.get(i).getAsInt();
                }
            }
            return new PromptResult(cancelled, value, indices);
        }
    }

    @Override
    public void switchInputMode(String mode) {
        if (closed.get() || mode == null || mode.isEmpty()) {
            return;
        }
        try {
            synchronized (writeLock) {
                InteractiveProtocol.writeMessage(outputStream,
                        InteractiveProtocol.TYPE_SET_HIGHLIGHT_MODE,
                        mode.getBytes(StandardCharsets.UTF_8));
            }
            logger.debug("已发送输入模式切换请求: " + mode + " (" + InputMode.getDescription(mode) + ")");
        } catch (IOException e) {
            logger.error("发送输入模式切换失败: " + mode, e);
        }
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
                logger.debug("关闭输出处理器失败: " + e.getMessage());
            }
            // 关闭 RemoteServerTerminal
            if (remoteTerminal != null) {
                try {
                    remoteTerminal.close();
                } catch (Exception e) {
                    logger.debug("关闭 RemoteServerTerminal 失败: " + e.getMessage());
                }
            }
            // 关闭 RPC 通道
            if (rpcChannel != null) {
                rpcChannel.close();
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