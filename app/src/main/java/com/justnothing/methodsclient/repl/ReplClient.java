package com.justnothing.methodsclient.repl;

import com.justnothing.methodsclient.StreamClient;
import com.justnothing.methodsclient.completer.CommandCompleter;
import com.justnothing.methodsclient.executor.SocketStreamReader;
import com.justnothing.methodsclient.highlighter.HighlighterManager;
import com.justnothing.methodsclient.highlighter.SwitchableHighlighter;
import com.justnothing.methodsclient.monitor.ClientPortManager;
import com.justnothing.methodsclient.tailtip.TailTipManager;
import com.justnothing.methodsclient.utils.TerminalManager;
import com.justnothing.testmodule.command.protocol.InteractiveProtocol;
import com.justnothing.testmodule.command.output.ClientRequirements;

import org.jline.reader.Completer;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.EndOfFileException;
import org.jline.reader.UserInterruptException;
import org.jline.widget.TailTipWidgets;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;


public class ReplClient {

    private static final StreamClient.ClientLogger logger = new StreamClient.ClientLogger();

    private static final String PROMPT = "\u001B[32mmethods\u001B[0m\u001B[36m>\u001B[0m ";
    private static final String WELCOME = """
            ╔══════════════════════════════════════════════════╗
            ║   JustNothing Methods REPL - Interactive Shell   ║
            ╠══════════════════════════════════════════════════╣
            ║  Tab     补全命令/参数                           ║
            ║  ↑↓      历史记录                                ║
            ║  Ctrl+R  搜索历史                                ║
            ║  exit    退出                                    ║
            ╚══════════════════════════════════════════════════╝
            """;

    private final ExecutorService executor = Executors.newCachedThreadPool(r -> {
        Thread t = new Thread(r, "ReplWorker");
        t.setDaemon(true);
        return t;
    });

    /** 服务端端口（启动时获取一次） */
    private int serverPort;

    /**
     * 启动 REPL 主循环。此方法会阻塞直到用户退出。
     */
    public void start() {
        if (!ClientPortManager.checkSocketServer()) {
            System.err.println("错误: Socket 服务不可用，请确保模块已启动");
            return;
        }

        try {
            serverPort = ClientPortManager.getSocketPort();

            // 构建带命令补全的 LineReader
            LineReader reader = buildReplLineReader();

            System.out.println(WELCOME);

            // 主循环
            mainLoop(reader);

        } catch (Exception e) {
            logger.error("REPL 启动失败", e);
            System.err.println("REPL 错误: " + e.getMessage());
        } finally {
            executor.shutdown();
        }
    }

    // ==================== 核心：主循环 ====================

    private void mainLoop(LineReader reader) throws IOException {
        AtomicBoolean running = new AtomicBoolean(true);

        while (running.get()) {
            String line;
            try {
                line = reader.readLine(PROMPT);
            } catch (UserInterruptException e) {
                // Ctrl+C
                System.out.println("^C");
                continue;
            } catch (EndOfFileException e) {
                // Ctrl+D
                System.out.println("exit");
                break;
            }

            if (line == null) {
                break;
            }

            String trimmed = line.trim();
            if (trimmed.isEmpty()) {
                continue;
            }

            // 内置命令
            if (handleBuiltInCommand(trimmed, running)) {
                continue;
            }

            // 每条命令新建 socket 连接，执行完后服务端关闭该连接
            sendCommandAndWait(trimmed);
        }

        System.out.println("再见!");
    }

    // ==================== 内置命令 ====================

    private boolean handleBuiltInCommand(String input, AtomicBoolean running) {
        switch (input.toLowerCase()) {
            case "exit", "quit" -> {
                running.set(false);
                return true;
            }
            case "clear", "cls" -> {
                // ANSI 清屏
                System.out.print("\033[H\033[2J");
                System.out.flush();
                return true;
            }
            case "client_help" -> {
                printReplHelp();
                return true;
            }
            default -> {
                return false;
            }
        }
    }

    private void printReplHelp() {
        System.out.println("""
                内置命令:
                  client_help  - 显示客户端帮助（本信息）
                  exit/quit    - 退出 REPL
                  clear/cls    - 清屏
                
                其他输入将发送到服务端执行（包括 help 等服务端命令）。
                使用 Tab 补全命令和参数，↑↓ 浏览历史。
                """);
    }

    // ==================== 网络层（每次命令新建连接） ====================

    /**
     * 发送命令到服务端并读取完整响应。
     * 每次调用创建新的 socket 连接，服务端执行完毕后关闭连接。
     */
    private void sendCommandAndWait(String command) throws IOException {
        Socket socket = null;
        try {
            socket = new Socket();
            socket.setTcpNoDelay(true);
            socket.setSoTimeout(10000);
            socket.connect(new InetSocketAddress("localhost", serverPort), 5000);

            // 能力协商
            negotiateCapability(socket.getOutputStream());

            // 发送命令
            InteractiveProtocol.writeMessage(socket.getOutputStream(),
                    InteractiveProtocol.TYPE_CLIENT_COMMAND,
                    command.getBytes(StandardCharsets.UTF_8));

            // 同步读取响应直到 COMMAND_END
            AtomicBoolean reading = new AtomicBoolean(true);
            AtomicLong bytesRead = new AtomicLong(0);

            boolean ok = SocketStreamReader.readColoredInteractiveStream(
                    socket.getInputStream(), socket.getOutputStream(),
                    reading, bytesRead, socket, null);
            if (!ok) {
                logger.warn("命令执行异常或连接中断: " + command);
            }

        } catch (IOException e) {
            if (e.getMessage() != null && e.getMessage().contains("Connection reset")) {
                // 服务端关闭了连接（正常情况）
                logger.debug("连接被重置（命令执行完毕）");
            } else {
                throw e;
            }
        } finally {
            closeQuietly(socket);
        }

        // 命令结束后多输出一个换行分隔
        System.out.println();
    }

    private void negotiateCapability(OutputStream output) throws IOException {
        ClientRequirements req = new ClientRequirements(true, false);
        InteractiveProtocol.writeMessage(output,
                InteractiveProtocol.TYPE_CLIENT_CAPABILITY,
                InteractiveProtocol.encodeCapability(req));
    }

    // ==================== JLine 构建 ====================

    private LineReader buildReplLineReader() {
        CommandCompleter completer = new CommandCompleter();

        var currentMode = TerminalManager.getCurrentMode();
        com.justnothing.testmodule.utils.logging.Logger.getLoggerForName("ReplClient")
                .info("构建 REPL LineReader, 终端模式: " + currentMode
                        + ", 终端类型: " + TerminalManager.getTerminal().getType());

        // 创建可切换代理高亮器（运行时可通过 HighlighterManager 切换模式）
        SwitchableHighlighter switchableHighlighter =
                HighlighterManager.createSwitchableHighlighter();

        LineReaderBuilder builder = LineReaderBuilder.builder()
                .terminal(TerminalManager.getTerminal())
                .completer(completer)
                .option(LineReader.Option.AUTO_FRESH_LINE, true)
                .option(LineReader.Option.DISABLE_EVENT_EXPANSION, true);

        // 非 dumb 模式下启用高亮和 TailTip
        if (currentMode != TerminalManager.TerminalMode.DUMB) {
            builder.highlighter(switchableHighlighter);
        } else {
            logger.warn("DUMB 模式，TailTip/高亮 将被禁用");
        }

        LineReader reader = builder.build();

        // 绑定高亮管理器（传入代理，允许服务端动态切换高亮模式）
        HighlighterManager.setLineReader(reader, switchableHighlighter);

        // TailTip + Autopair：创建实例但不立即 enable（必须在 readLine 上下文中调用）。
        // 通过 CommandCompleter 的 firstUseCallback 延迟到首次 Tab/输入时触发，
        // 此时一定处于 readLine 上下文内。
        if (TerminalManager.getCurrentMode() != TerminalManager.TerminalMode.DUMB) {
            TailTipWidgets tailTips = TailTipManager.setupCommandTailTips(reader, completer);

            final TailTipWidgets capturedTips = tailTips;
            completer.setFirstUseCallback(() -> {
                try {
                    capturedTips.enable();
                    logger.info("TailTip 已启用");
                } catch (IllegalStateException e) {
                    logger.warn("TailTip 启用失败（非致命）: " + e.getMessage());
                }
                // 同时尝试启用 AutopairWidgets
                try {
                    new org.jline.widget.AutopairWidgets(reader).enable();
                    logger.info("AutopairWidgets 已启用");
                } catch (IllegalStateException e) {
                    logger.warn("AutopairWidgets 启用失败（非致命）: " + e.getMessage());
                }
            });
        }

        // 方向键绑定（Android 终端兼容）
        bindArrowKeys(reader);

        return reader;
    }

    private void bindArrowKeys(LineReader reader) {
        var keyMap = reader.getKeyMaps().get(LineReader.MAIN);
        if (keyMap == null) return;

        keyMap.bind(new org.jline.reader.Reference("up-line-or-history"), "\033[A");
        keyMap.bind(new org.jline.reader.Reference("up-line-or-history"), "\033OA");
        keyMap.bind(new org.jline.reader.Reference("down-line-or-history"), "\033[B");
        keyMap.bind(new org.jline.reader.Reference("down-line-or-history"), "\033OB");
        keyMap.bind(new org.jline.reader.Reference("backward-char"), "\033[D");
        keyMap.bind(new org.jline.reader.Reference("backward-char"), "\033OD");
        keyMap.bind(new org.jline.reader.Reference("forward-char"), "\033[C");
        keyMap.bind(new org.jline.reader.Reference("forward-char"), "\033OC");
        keyMap.bind(new org.jline.reader.Reference("beginning-of-line"), "\033[H");
        keyMap.bind(new org.jline.reader.Reference("end-of-line"), "\033[F");
        keyMap.bind(new org.jline.reader.Reference("delete-char"), "\033[3~");

        keyMap.setAmbiguousTimeout(10);
    }

    // ==================== 工具方法 ====================

    private static void closeQuietly(Socket socket) {
        if (socket != null) {
            try { socket.close(); } catch (IOException ignored) {}
        }
    }
}
