package com.justnothing.testmodule.command.framework.output;

import com.google.gson.JsonObject;
import com.google.gson.JsonArray;
import com.justnothing.testmodule.command.framework.model.CommandResult;
import com.justnothing.testmodule.command.framework.protocol.RemoteServerTerminal;
import com.justnothing.testmodule.utils.concurrent.ThreadPoolManager;
import com.justnothing.testmodule.utils.logging.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import com.justnothing.richconsole.console.Console;
import com.justnothing.testmodule.command.framework.protocol.ProtocolMethods;
import com.justnothing.testmodule.command.framework.protocol.TerminalRpcChannel;

import org.jline.terminal.Size;
import org.jline.terminal.Terminal;
import org.jline.terminal.impl.ExternalTerminal;


public class InteractiveOutputHandler implements ICommandOutputHandler {

    private static final Logger logger = Logger.getLoggerForName("InteractiveOutputHandler");

    private final StringBuilder buffer = new StringBuilder();
    private final OutputStream outputStream;
    private final AtomicBoolean closed = new AtomicBoolean(false);
    private final Object writeLock = new Object();
    // 会话关闭闩锁：close() 完成（cmd.done 已发出/发送失败）后释放，
    // 供 SocketClientHandler 主线程等待，替代 sleep 轮询，避免提前关闭 socket 导致 cmd.done 丢失
    private final CountDownLatch closedLatch = new CountDownLatch(1);
    private volatile boolean supportsInput = true;
    private volatile boolean isJsonMode = false;

    // 客户端终端信息
    private volatile int clientWidth;
    private volatile int clientHeight;
    private volatile boolean clientSupportsAnsi;
    private volatile byte clientColorSystem;

    // ExternalTerminal + Protocol 流适配（统一 TYPE_RPC 信封，cmd.* / term.* / sys.* 共用一个通道）
    private TerminalRpcChannel rpcChannel;
    private RemoteServerTerminal remoteTerminal;

    // 命令结束时的结构化结果（close() 时随 cmd.done 发给客户端）
    private volatile CommandResult pendingResult;

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

    public void setJsonMode(boolean jsonMode) {
        this.isJsonMode = jsonMode;
        if (jsonMode) {
            this.supportsInput = false;
        }
    }
    
    public boolean isJsonMode() {
        return isJsonMode;
    }

    /**
     * 设置客户端终端信息（由 {@link #applyClientRequirements} 调用）。
     *
     * <p>仅设置客户端信息字段（宽度、高度、ANSI 支持、颜色系统），
     * 终端是懒创建的（见 {@link #getConsole()}），这里只记录能力。</p>
     */
    public void setClientTerminalInfo(int width, int height, boolean supportsAnsi, byte colorSystem) {
        this.clientWidth = width;
        this.clientHeight = height;
        this.clientSupportsAnsi = supportsAnsi;
        this.clientColorSystem = colorSystem;
    }

    /**
     * 初始化 RPC 通道（统一 TYPE_RPC 信封帧）。
     *
     * <p>服务端与 agent 都先调用此方法；终端会在首次需要 RichConsole 渲染时懒创建
     * （见 {@link #getConsole()}）。</p>
     */
    public TerminalRpcChannel initRpcChannel() {
        if (rpcChannel != null) return rpcChannel;
        try {
            rpcChannel = new TerminalRpcChannel(outputStream, writeLock);
            logger.info("RPC 通道创建成功 (TYPE_RPC)");
        } catch (Exception e) {
            logger.error("创建 RPC 通道失败", e);
        }
        return rpcChannel;
    }

    /**
     * 应用客户端能力（sys.hello 握手后调用）。
     *
     * <p>设置 supportsInput/jsonMode/客户端终端信息。
     * RemoteServerTerminal 不在这里创建：它要 ~620ms（JLine ExternalTerminal 构造），
     * 而只用 output.print/println 的命令根本不需要它。</p>
     */
    public void applyClientRequirements(ClientRequirements requirements) {
        if (requirements == null) return;
        setSupportsInput(requirements.isSupportsInput());
        setJsonMode(requirements.isJsonMode());
        setClientTerminalInfo(
                requirements.getWidth(), requirements.getHeight(),
                requirements.isSupportsAnsi(), requirements.getColorSystem()
        );
    }

    /**
     * 按需创建 RemoteServerTerminal。
     *
     * <p>不在握手时就建：只用 {@code output.print/println}（ICommandOutputHandler）的命令
     * 根本不需要 Console，而 JLine ExternalTerminal 的构造实测要 ~620ms。
     * 改为 {@link #getConsole()} 首次真正需要渲染时才建。</p>
     */
    private void createRemoteTerminal() {
        if (remoteTerminal != null || rpcChannel == null) return;
        try {
            String termType = clientSupportsAnsi ? "xterm-256color" : Terminal.TYPE_DUMB;
            long start = System.currentTimeMillis();
            remoteTerminal = new RemoteServerTerminal(rpcChannel, termType, clientWidth, clientHeight);
            long cost = System.currentTimeMillis() - start;
            logger.info("RemoteServerTerminal 创建成功: type=" + termType
                    + ", size=" + clientWidth + "x" + clientHeight + ", 耗时=" + cost + "ms");
        } catch (Exception e) {
            logger.error("创建 RemoteServerTerminal 失败，将使用 invalid console", e);
        }
    }

    /**
     * 获取 RPC 通道（供 SocketClientHandler 注册 cmd.* / sys.* 处理器）
     */
    public TerminalRpcChannel getRpcChannel() {
        return rpcChannel;
    }

    /**
     * 获取 Console 实例（供命令代码使用 RichConsole 渲染）。
     *
     * <ul>
     *   <li><b>交互模式</b>：Console 基于 RemoteServerTerminal，渲染结果通过 RPC 通道发送</li>
     *   <li><b>无终端（agent 等）</b>：invalid console，输出丢弃、输入报错</li>
     * </ul>
     */
    @Override
    public Console getConsole() {
        if (console == null) {
            createRemoteTerminal();
            console = remoteTerminal != null
                    ? Console.of(c -> c
                            .withTerminal(remoteTerminal)
                            .withForceTerminal(true)
                            .withColorSystem(mapColorSystem(clientColorSystem)))
                    : createInvalidConsole();
        }
        return console;
    }

    /**
     * 下游（最终客户端）能不能收到并显示渲染结果。
     *
     * <p>本地服务端场景下我们就是最后一层，默认 {@code true}。但 agent 代理执行时，
     * 我们只是中间的一环 —— 后面还有 CLI → 客户端。如果那一层断了，渲染结果发出去也没人看，
     * 此时必须让 {@link #supportsRichRendering()} 返回 false，命令才会降级成纯文本
     * 而不是把内容写进黑洞。</p>
     */
    private volatile boolean downstreamReachable = true;

    /** 由调用方告知"下游还有没有人接渲染结果"（agent 场景下由 CLI 的能力透传情况决定）。 */
    public void setDownstreamReachable(boolean reachable) {
        this.downstreamReachable = reachable;
    }

    /**
     * 有 rpcChannel 才建得出真终端；没有的话 {@link #getConsole()} 会退化成
     * "写进去就丢"的 invalid console（agent 在目标进程里执行就是这种情况）。
     * 另外还要下游真有人接 —— 见 {@link #setDownstreamReachable}。
     */
    @Override
    public boolean supportsRichRendering() {
        return downstreamReachable && (remoteTerminal != null || rpcChannel != null);
    }

    /**
     * 创建 invalid console：基于 ExternalTerminal 的 dumb 终端（不用 TerminalBuilder，
     * 规避 Android 无终端提供器问题）。输出丢弃、输入 read() 抛异常。
     */
    private Console createInvalidConsole() {
        try {
            // System.in 是 system_server 的 stdin，不可用；
            // 用一个读取就抛异常的 InputStream（无终端不支持输入）
            InputStream noInput = new InputStream() {
                @Override
                public int read() {
                    throw new UnsupportedOperationException("InvalidConsole 不支持终端输入");
                }
                @Override
                public int available() {
                    return 0;
                }
            };
            // 丢弃所有输出
            OutputStream discardOutput = new OutputStream() {
                @Override
                public void write(int b) throws IOException {
                }
            };
            ExternalTerminal dumbTerminal = new ExternalTerminal(
                    null,                          // provider
                    "invalid-console",             // name
                    Terminal.TYPE_DUMB,            // type
                    noInput, discardOutput,
                    StandardCharsets.UTF_8,
                    Terminal.SignalHandler.SIG_IGN,
                    false                          // paused
            );
            if (clientWidth > 0 && clientHeight > 0) {
                dumbTerminal.setSize(new Size(clientWidth, clientHeight));
            }
            Console c = Console.of(cfg -> cfg
                .withTerminal(dumbTerminal)
                .withNoColor(true)
            );
            logger.warn("已创建 invalid console (输出丢弃、输入报错)");
            return c;
        } catch (Exception e) {
            logger.error("创建 invalid console 失败", e);
            return null;
        }
    }

    /**
     * 将客户端上报的 colorSystem byte 映射为 Console 的颜色系统名称。
     * 0=NONE, 1=STANDARD(16色), 2=EIGHT_BIT(256色), 3=TRUECOLOR(真彩色)
     */
    private static String mapColorSystem(byte colorSystem) {
        return switch (colorSystem) {
            case ClientRequirements.COLOR_TRUECOLOR -> "truecolor";
            case ClientRequirements.COLOR_EIGHT_BIT -> "256";
            case ClientRequirements.COLOR_STANDARD -> "standard";
            default -> null; // auto
        };
    }



    /**
     * 处理 TYPE_RPC 帧数据（由服务端/agent reader 线程调用）
     */
    public void handleRpcData(byte[] data) {
        if (rpcChannel != null) {
            rpcChannel.handleMessage(data);
        }
    }

    /**
     * 交互输入：通过 cmd.prompt RPC 请求（同步门面 over 异步通道）。
     *
     * <p>客户端收到 cmd.prompt 请求后在本地渲染提示（TerminalManager.readLine 等），
     * 以 cmd.prompt 响应返回。call() 内部用 CompletableFuture 阻塞等待。</p>
     *
     * @param promptType 提示类型: "input" | "password" | "confirm" | "list" | "checkbox"
     * @param title 提示标题
     * @param options 选项列表（list/checkbox 用），可为 null
     * @param defaultValue 默认值，可为 null
     * @param timeoutMs 超时毫秒；&lt;=0 表示无限等待（默认：等用户输入不设时限，
     *                  客户端断开时 RPC 通道 close() 会取消等待并返回 null）
     * @return 用户输入值；取消/空值/连接关闭返回 null；真超时（timeoutMs&gt;0）抛 RuntimeException
     */
    private String promptViaRpc(String promptType, String title, String[] options,
                                String defaultValue, long timeoutMs) {
        if (closed.get() || rpcChannel == null) {
            return null;
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

            logger.debug("发送 cmd.prompt RPC: " + promptType);
            JsonObject result = rpcChannel.call(ProtocolMethods.CMD_PROMPT, params, timeoutMs);

            if (result != null) {
                if (result.has("value") && !result.get("value").isJsonNull()) {
                    return result.get("value").getAsString();
                }
                // 用户取消或空值
                return null;
            }

            if (timeoutMs > 0) {
                logger.warn("cmd.prompt 请求超时: " + promptType + " (" + timeoutMs + "ms)");
                throw new RuntimeException("输入请求超时 (" + (timeoutMs / 1000) + "秒)");
            }
            // 无限等待模式下 result==null 只可能是连接关闭/取消
            logger.warn("cmd.prompt 连接已关闭，返回 null: " + promptType);
            return null;
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            logger.error("cmd.prompt 请求异常: " + promptType, e);
            throw new RuntimeException("输入请求失败", e);
        }
    }

    @Override
    public void switchInputMode(String mode) {
        if (closed.get() || mode == null || mode.isEmpty()) {
            return;
        }
        if (rpcChannel == null) {
            logger.error("switchInputMode: RPC 通道未初始化，丢弃模式切换: " + mode);
            return;
        }
        JsonObject params = new JsonObject();
        params.addProperty("mode", mode);
        rpcChannel.sendNotification(ProtocolMethods.TERM_HIGHLIGHT, params);
        logger.debug("已发送输入模式切换请求: " + mode + " (" + InputMode.getDescription(mode) + ")");
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

        // 提示前必须先排空待发输出，否则用户会先看到提问、后看到提问前的输出。
        flushPendingOutput();
        // 两条链路都要排：除了 cmd.output 批次，jline 终端流（term.output）走的是
        // TerminalRpcChannel 的合并窗口，窗口没到期时下面的 cmd.prompt 会先发出去，
        // 提示符就会插到上一批输出的中间。
        flushTerminalStream();

        // cmd.prompt RPC 请求（同步门面 over 异步通道；默认无限等待用户输入）
        return promptViaRpc("input", prompt, null, null, 0);
    }

    @Override
    public void finish(CommandResult result) {
        this.pendingResult = result;
    }

    @Override
    public void close() {
        if (!closed.compareAndSet(false, true)) {
            return;
        }
        try {
            // 先把批量窗口里待发的输出同步排空：必须严格早于 cmd.done，
            // 否则客户端可能先收到"命令结束"再收到最后一批输出，顺序颠倒。
            flushPendingOutput();
            if (rpcChannel != null) {
                // 再把终端流合并窗口里待发的输出排空，同样必须早于 cmd.done。
                // 否则客户端可能先收到"命令结束"再收到最后一批输出，顺序颠倒。
                flushTerminalStream();

                // cmd.done 通知（命令结束）：携带结构化结果（cmd.output 流式输出的结尾）。
                // 这里手工拼 JSON 而不再走 JsonObject：结果可能是几百 KB 的大对象，
                // 若先 toJsonString() → JsonParser.parseString() 建树 → 最后 Gson 再序列化一次，
                // 同样的内容会被完整遍历三遍（实测光这一步就 ~480ms）。result 本身已是
                // Gson 产出的合法 JSON，直接嵌入即可。
                long buildStart = System.currentTimeMillis();
                StringBuilder json = beginEnvelope(ProtocolMethods.CMD_DONE, 256);
                json.append("\"success\":")
                        .append(pendingResult == null || pendingResult.isSuccess());
                if (pendingResult != null) {
                    String resultJson = pendingResult.toJsonString();
                    json.append(",\"result\":")
                            .append(resultJson == null || resultJson.isEmpty() ? "null" : resultJson);
                }
                String frame = endEnvelope(json);
                long buildEnd = System.currentTimeMillis();
                rpcChannel.sendRawMessage(frame);
                long sendEnd = System.currentTimeMillis();

                if (buildEnd - buildStart > 100 || sendEnd - buildEnd > 100) {
                    logger.info("cmd.done 构造成本偏高: 构造=" + (buildEnd - buildStart)
                            + "ms, 发送=" + (sendEnd - buildEnd) + "ms");
                }
                logger.debug("发送 cmd.done 通知" + (pendingResult != null ? "（携带结构化结果）" : ""));
            } else {
                logger.error("close: RPC 通道未初始化，跳过 cmd.done");
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
        } finally {
            // 必须无条件释放闩锁：否则等待方会永久阻塞。
            // 位置放在 finally 中同时保证"cmd.done 已发出/发送失败"之后才放行。
            closedLatch.countDown();
        }
    }

    /**
     * 阻塞等待会话关闭（close() 完成即返回）。
     *
     * <p>供 {@code SocketClientHandler} 主线程等待"命令完成（close() 已尝试发送 cmd.done）
     * 或客户端断开（reader 线程 finally 也会 close()）"，替代 sleep 轮询，
     * 消除 cmd.done 与 socket 关闭之间的竞态。中断时恢复中断标志并继续等待。</p>
     */
    public void awaitClosed() {
        boolean interrupted = false;
        while (true) {
            try {
                closedLatch.await();
                return;
            } catch (InterruptedException e) {
                interrupted = true;
            }
        }
    }

    /**
     * 带超时的等待关闭。
     *
     * @return 已关闭返回 true；超时（或等待被中断）返回 false
     */
    public boolean awaitClosed(long timeout, TimeUnit unit) {
        try {
            return closedLatch.await(timeout, unit);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return closed.get();
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

        // 提示前必须先排空待发输出，否则用户会先看到提问、后看到提问前的输出。
        flushPendingOutput();
        flushTerminalStream();

        // cmd.prompt RPC 请求（password 类型；默认无限等待用户输入）
        return promptViaRpc("password", prompt, null, null, 0);
    }


    @Override
    public boolean isInteractive() {
        return true;
    }

    @Override
    public void flush() {
        flushPendingOutput();
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

    // ─── cmd.output 批量合并 ───
    // 逐条发 cmd.output 的代价几乎全在"每次调用"的固定开销上（构造 JSON + Gson 序列化 +
    // getBytes + 抢锁），而不是字节数：实测 10000 次输出共 20.2s，其中 write+flush 仅 3.4s，
    // Gson 序列化 11.5s，其余构造/编码/抢锁约 5.5s。把连续输出攒成一批发一帧，
    // 这些按调用次数计费的开销会一起被摊薄。
    private static final char[] HEX_DIGITS = "0123456789abcdef".toCharArray();

    private static final long OUTPUT_IDLE_NANOS = 10_000_000L;   // 空闲 ≥10ms 立刻发，保交互手感
    private static final int OUTPUT_BATCH_MAX_SEGMENTS = 256;
    private static final int OUTPUT_BATCH_MAX_CHARS = 8192;
    private static final long OUTPUT_FLUSH_DELAY_MS = 8;

    static final class OutputSegment {
        final byte color;
        final boolean hasColor;
        final String text;

        OutputSegment(byte color, boolean hasColor, String text) {
            this.color = color;
            this.hasColor = hasColor;
            this.text = text;
        }
    }

    private final List<OutputSegment> pendingOutput = new ArrayList<>();
    private final Object pendingOutputLock = new Object();
    private int pendingOutputChars = 0;
    private long lastOutputEmitNanos = 0L;
    private ScheduledFuture<?> pendingOutputFlush;

    /** 把一段输出放进待发批次：空闲 / 攒满就立刻发，否则排一个短延迟。 */
    private void enqueueOutput(byte color, boolean hasColor, String text) {
        if (closed.get() || text == null || text.isEmpty()) {
            return;
        }
        buffer.append(text);
        if (rpcChannel == null) {
            logger.error("RPC 通道未初始化，丢弃输出: " + text);
            return;
        }
        boolean flushNow;
        synchronized (pendingOutputLock) {
            pendingOutput.add(new OutputSegment(color, hasColor, text));
            pendingOutputChars += text.length();
            long now = System.nanoTime();
            flushNow = pendingOutput.size() >= OUTPUT_BATCH_MAX_SEGMENTS
                    || pendingOutputChars >= OUTPUT_BATCH_MAX_CHARS
                    || now - lastOutputEmitNanos >= OUTPUT_IDLE_NANOS;
            if (!flushNow && pendingOutputFlush == null) {
                pendingOutputFlush = ThreadPoolManager.schedule(
                        this::flushPendingOutput, OUTPUT_FLUSH_DELAY_MS, TimeUnit.MILLISECONDS);
            }
        }
        if (flushNow) {
            flushPendingOutput();
        }
    }

    /**
     * 排空待发批次并发成一帧 cmd.output。
     *
     * <p>单段编码为 {@code {data,color}}；多段编码为
     * {@code {segments:[{data,color},...]}}，客户端一次渲染、只 flush 一次。</p>
     */
    private void flushPendingOutput() {
        if (rpcChannel == null) {
            return;
        }
        synchronized (pendingOutputLock) {
            if (pendingOutput.isEmpty()) {
                return;
            }
            List<OutputSegment> batch = new ArrayList<>(pendingOutput);
            pendingOutput.clear();
            pendingOutputChars = 0;
            lastOutputEmitNanos = System.nanoTime();
            pendingOutputFlush = null;

            // 取批次与发送必须在同一临界区内完成。
            // 否则被延迟 flush 线程取走的批次，可能在 close() 已发出 cmd.done 之后才发送，
            // 被客户端（收到 cmd.done 即停读并关连接）或 RpcChannel 的 closed 守卫静默丢弃，
            // 表现为输出的中间一段凭空消失（前后的批次都完好）。
            rpcChannel.sendRawMessage(buildOutputFrame(batch));
        }
    }

    /**
     * 排空 jline 终端流（{@code term.output}）合并窗口里待发的数据。
     *
     * <p>{@link TerminalRpcChannel.RpcOutputStream} 会把一次爆发攒到 16ms 的窗口里再发，
     * 而 {@code cmd.prompt} / {@code cmd.done} 是立刻发的 —— 中间必须夹一次同步排空，
     * 才能保证"先输出完，再提问 / 再结束"。缺了它，用户会先看到 {@code manage> } 提示符、
     * 后看到提示符之前那批输出（它们还压在窗口里）。</p>
     */
    private void flushTerminalStream() {
        if (rpcChannel == null) {
            return;
        }
        TerminalRpcChannel.RpcOutputStream stream = rpcChannel.getRpcOutputStream();
        if (stream != null) {
            stream.flushNow();
        }
    }

    /**
     * 手工拼装 RPC 信封的开头：{@code {"jsonrpc":"<version>","method":"<method>","params":{}（未闭合）。
     *
     * <p>与 {@link #endEnvelope} 配对，供 cmd.done / cmd.output 等热路径共用：这些帧必须绕开
     * Gson 的高开销，保持 StringBuilder 手拼。字段顺序与 Gson 信封一致。</p>
     *
     * <p>这里是有意为之的，因为在某些（特指某款手表，不说）设备上，
     * Gson 的序列化效率极低，可能得 2-3 ms/serialization。</p>
     *
     * @param capacity StringBuilder 初始容量（各调用点沿用原有的预估容量）
     */
    private static StringBuilder beginEnvelope(String method, int capacity) {
        return new StringBuilder(capacity)
                .append("{\"jsonrpc\":\"").append(ProtocolMethods.JSONRPC_VERSION)
                .append("\",\"method\":\"").append(method)
                .append("\",\"params\":{");
    }

    /** 闭合 {@link #beginEnvelope} 拼出的信封（补上 params 与根对象的收尾），返回 JSON 文本。 */
    private static String endEnvelope(StringBuilder json) {
        return json.append("}}").toString();
    }

    /**
     * 把一批输出拼成 cmd.output 帧的 JSON 文本。
     *
     * <p>单段编码为 {@code {data,color}}，多段编码为
     * {@code {segments:[{data,color},...]}}。文本必须经 {@link #appendJsonString} 转义。</p>
     */
    static String buildOutputFrame(List<OutputSegment> batch) {
        StringBuilder json = beginEnvelope(ProtocolMethods.CMD_OUTPUT, 128 + batch.size() * 24);
        if (batch.size() == 1) {
            OutputSegment only = batch.get(0);
            json.append("\"data\":");
            appendJsonString(json, only.text);
            if (only.hasColor) {
                json.append(",\"color\":").append(only.color);
            }
        } else {
            json.append("\"segments\":[");
            for (int i = 0; i < batch.size(); i++) {
                OutputSegment segment = batch.get(i);
                if (i > 0) {
                    json.append(',');
                }
                json.append("{\"data\":");
                appendJsonString(json, segment.text);
                if (segment.hasColor) {
                    json.append(",\"color\":").append(segment.color);
                }
                json.append('}');
            }
            json.append(']');
        }
        return endEnvelope(json);
    }

    /**
     * 按 JSON 规则转义后追加（等价于 Gson JsonWriter.string 的行为）。
     *
     * <p>转义必须完整：输出文本是命令产生的任意内容，漏掉任何一种都会让整个 RPC 帧坏掉。
     * U+2028/U+2029 也一并转义，避免消费端用 JS 解析时出问题。</p>
     */
    static void appendJsonString(StringBuilder out, String value) {
        out.append('"');
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '"' -> out.append("\\\"");
                case '\\' -> out.append("\\\\");
                case '\n' -> out.append("\\n");
                case '\r' -> out.append("\\r");
                case '\t' -> out.append("\\t");
                case '\b' -> out.append("\\b");
                case '\f' -> out.append("\\f");
                default -> {
                    if (c < 0x20 || c == 0x2028 || c == 0x2029) {
                        appendUnicodeEscape(out, c);
                    } else {
                        out.append(c);
                    }
                }
            }
        }
        out.append('"');
    }

    private static void appendUnicodeEscape(StringBuilder out, char c) {
        out.append('\\').append('u');
        out.append(HEX_DIGITS[(c >> 12) & 0xF]);
        out.append(HEX_DIGITS[(c >> 8) & 0xF]);
        out.append(HEX_DIGITS[(c >> 4) & 0xF]);
        out.append(HEX_DIGITS[c & 0xF]);
    }

    private void sendOutput(String text) {
        enqueueOutput(Colors.DEFAULT, false, text);
    }


    private void sendColoredOutput(byte color, String text) {
        enqueueOutput(color, true, text);
    }
}
