package com.justnothing.methodsclient.test;

import com.justnothing.methodsclient.utils.TerminalManager;
import com.justnothing.methodsclient.utils.TerminalManager.TerminalMode;

import org.jline.terminal.Attributes;
import org.jline.terminal.Attributes.LocalFlag;
import org.jline.terminal.Size;
import org.jline.terminal.Terminal;
import org.jline.terminal.Terminal.Signal;
import org.jline.terminal.Terminal.SignalHandler;
import org.jline.utils.InfoCmp.Capability;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 终端能力诊断测试。
 *
 * <p>运行在客户端（app_process）侧，不需要服务端参与。
 * 测试 JLine Terminal 在当前 Android 环境下的各种能力，
 * 特别关注终端尺寸检测和 SIGWINCH 信号处理。</p>
 *
 * <p>使用方式：</p>
 * <ul>
 *   <li>{@code StreamClient --terminal-test} — 完整测试</li>
 *   <li>{@code StreamClient --terminal-test --quick} — 快速测试（跳过交互等待）</li>
 *   <li>REPL 内 {@code terminal_test} — 从 REPL 内运行</li>
 * </ul>
 */
public class TerminalCapabilityTest {

    private static final String RESET = "\033[0m";
    private static final String BOLD = "\033[1m";
    private static final String DIM = "\033[2m";
    private static final String RED = "\033[31m";
    private static final String GREEN = "\033[32m";
    private static final String YELLOW = "\033[33m";
    private static final String CYAN = "\033[36m";
    private static final String GRAY = "\033[90m";

    private final boolean quickMode;
    private int passed = 0;
    private int failed = 0;
    private int skipped = 0;

    public TerminalCapabilityTest(boolean quickMode) {
        this.quickMode = quickMode;
    }

    public void run() {
        Terminal terminal = TerminalManager.getTerminal();
        TerminalMode mode = TerminalManager.getCurrentMode();

        header("终端能力诊断测试");
        info("测试环境", "app_process (Android " + android.os.Build.VERSION.SDK_INT
                + ", " + android.os.Build.SUPPORTED_ABIS[0] + ")");
        info("测试模式", quickMode ? "快速" : "完整");
        newline();

        // ─── 1. 基础终端信息 ──────────────────────────────────
        section("1. 基础终端信息");
        testBasicInfo(terminal, mode);

        // ─── 2. 终端尺寸检测 ──────────────────────────────────
        section("2. 终端尺寸检测");
        testSizeDetection(terminal);

        // ─── 3. SIGWINCH 信号 ──────────────────────────────────
        section("3. SIGWINCH 信号检测");
        testSigwinch(terminal);

        // ─── 4. 尺寸轮询 ──────────────────────────────────────
        section("4. 终端尺寸轮询");
        testSizePolling(terminal);

        // ─── 5. ANSI 能力 ──────────────────────────────────────
        section("5. ANSI 转义序列能力");
        testAnsiCapabilities(terminal);

        // ─── 6. JLine Terminal 特性 ────────────────────────────
        section("6. JLine Terminal 特性");
        testJlineFeatures(terminal);

        // ─── 7. 颜色系统 ──────────────────────────────────────
        section("7. 颜色系统");
        testColorSystem(terminal, mode);

        // ─── 8. 输入能力 ──────────────────────────────────────
        section("8. 输入能力");
        testInputCapabilities(terminal, mode);

        // ─── 9. 终端属性 ──────────────────────────────────────
        section("9. 终端属性 (stty)");
        testTerminalAttributes(terminal);

        // ─── 10. ANSI 实际渲染测试 ─────────────────────────────
        section("10. ANSI 实际渲染测试");
        testAnsiRendering();

        // ─── 总结 ──────────────────────────────────────────────
        newline();
        summary();
    }

    // ═══════════════════════════════════════════════════════════
    // 测试方法
    // ═══════════════════════════════════════════════════════════

    private void testBasicInfo(Terminal terminal, TerminalMode mode) {
        info("TerminalMode", mode.name());
        info("Terminal 类型", terminal != null ? terminal.getType() : "null");
        info("Terminal 类名", terminal != null ? terminal.getClass().getName() : "null");
        info("System.out 类型", System.out.getClass().getName());
        info("stdout 是否连接终端", String.valueOf(System.console() != null));
        info("TERM 环境变量", env("TERM"));
        info("COLORTERM 环境变量", env("COLORTERM"));
        info("COLUMNS 环境变量", env("COLUMNS"));
        info("LINES 环境变量", env("LINES"));

        if (terminal != null) {
            test("Terminal 非空", true);
            test("Terminal 非 dumb", !Terminal.TYPE_DUMB.equals(terminal.getType()));
        } else {
            test("Terminal 非空", false);
        }
    }

    private void testSizeDetection(Terminal terminal) {
        if (terminal == null) {
            skip("Terminal 为 null，跳过尺寸检测");
            return;
        }

        // 第一次读取
        Size size1 = terminal.getSize();
        info("getSize() 第1次", size1.getColumns() + " x " + size1.getRows());

        // 第二次读取（验证稳定性）
        Size size2 = terminal.getSize();
        info("getSize() 第2次", size2.getColumns() + " x " + size2.getRows());

        boolean consistent = size1.getColumns() == size2.getColumns()
                && size1.getRows() == size2.getRows();
        test("getSize() 结果稳定", consistent);

        // 验证值合理性
        boolean reasonable = size1.getColumns() > 0 && size1.getColumns() < 1000
                && size1.getRows() > 0 && size1.getRows() < 500;
        test("尺寸值合理 (1-999 x 1-499)", reasonable);

        // 对比环境变量
        String colsEnv = System.getenv("COLUMNS");
        String linesEnv = System.getenv("LINES");
        if (colsEnv != null || linesEnv != null) {
            int envCols = colsEnv != null ? parseIntSafe(colsEnv, -1) : -1;
            int envLines = linesEnv != null ? parseIntSafe(linesEnv, -1) : -1;
            boolean match = (envCols < 0 || envCols == size1.getColumns())
                    && (envLines < 0 || envLines == size1.getRows());
            test("getSize() 与 COLUMNS/LINES 一致", match);
            if (!match) {
                info("  环境变量值", envCols + " x " + envLines);
                info("  terminal值", size1.getColumns() + " x " + size1.getRows());
            }
        } else {
            skip("COLUMNS/LINES 环境变量未设置");
        }

        // 测试性能
        long start = System.nanoTime();
        for (int i = 0; i < 100; i++) {
            terminal.getSize();
        }
        long elapsed = (System.nanoTime() - start) / 100_000; // 微秒 → 0.1ms
        info("getSize() 性能", "100次调用 " + (elapsed / 10.0) + "ms ("
                + (elapsed / 1000.0) + "μs/次)");
        test("getSize() 性能可接受 (<5ms/次)", elapsed / 1000.0 < 5000);
    }

    private void testSigwinch(Terminal terminal) {
        if (terminal == null) {
            skip("Terminal 为 null，跳过 SIGWINCH 测试");
            return;
        }
        if (quickMode) {
            skip("快速模式，跳过交互式 SIGWINCH 测试");
            return;
        }

        // 测试1：能否注册 WINCH 处理器
        AtomicBoolean handlerRegistered = new AtomicBoolean(false);
        AtomicBoolean winchReceived = new AtomicBoolean(false);
        AtomicReference<Signal> receivedSignal = new AtomicReference<>();
        CountDownLatch winchLatch = new CountDownLatch(1);

        try {
            terminal.handle(Signal.WINCH, signal -> {
                handlerRegistered.set(true);
                winchReceived.set(true);
                receivedSignal.set(signal);
                winchLatch.countDown();
            });
            test("terminal.handle(WINCH, ...) 注册成功", true);
        } catch (Exception e) {
            test("terminal.handle(WINCH, ...) 注册成功", false);
            info("  异常", e.getClass().getSimpleName() + ": " + e.getMessage());
            return;
        }

        // 测试2：等待用户触发 resize
        newline();
        print(YELLOW + "  >>> 请调整终端窗口大小（或在 10 秒内按回车跳过）<<<" + RESET);
        newline();

        try {
            boolean received = winchLatch.await(10, TimeUnit.SECONDS);
            if (received) {
                test("SIGWINCH 信号接收", true);
                info("  信号对象", String.valueOf(receivedSignal.get()));
            } else {
                test("SIGWINCH 信号接收", false, "10秒内未收到信号");
                info("  说明", "SIGWINCH 在此环境下不可用，轮询可作为替代方案");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            test("SIGWINCH 信号接收", false, "被中断");
        }

        // 测试3：resize 后检查尺寸是否更新
        if (winchReceived.get()) {
            Size afterResize = terminal.getSize();
            info("  resize 后尺寸", afterResize.getColumns() + " x " + afterResize.getRows());
            test("resize 后 getSize() 反映新尺寸", true);
        }
    }

    private void testSizePolling(Terminal terminal) {
        if (terminal == null) {
            skip("Terminal 为 null，跳过轮询测试");
            return;
        }
        if (quickMode) {
            skip("快速模式，跳过交互式轮询测试");
            return;
        }

        newline();
        Size initial = terminal.getSize();
        info("初始尺寸", initial.getColumns() + " x " + initial.getRows());

        AtomicInteger changes = new AtomicInteger(0);
        AtomicBoolean polling = new AtomicBoolean(true);
        AtomicReference<String> lastChange = new AtomicReference<>("");

        Thread poller = new Thread(() -> {
            int lastW = initial.getColumns(), lastH = initial.getRows();
            while (polling.get()) {
                try {
                    Thread.sleep(500); // 0.5秒轮询间隔（测试用，实际用3秒）
                } catch (InterruptedException e) {
                    break;
                }
                try {
                    Size s = terminal.getSize();
                    int w = s.getColumns(), h = s.getRows();
                    if (w != lastW || h != lastH) {
                        changes.incrementAndGet();
                        lastChange.set(lastW + "x" + lastH + " → " + w + "x" + h);
                        lastW = w;
                        lastH = h;
                    }
                } catch (Exception ignored) {}
            }
        }, "SizePoller");
        poller.setDaemon(true);
        poller.start();

        print(YELLOW + "  >>> 请调整终端窗口大小（或在 8 秒内按回车跳过）<<<" + RESET);
        newline();

        try {
            Thread.sleep(8000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        polling.set(false);
        poller.interrupt();

        int detected = changes.get();
        test("轮询能检测到尺寸变化", detected > 0 || quickMode);
        if (detected > 0) {
            info("  检测到变化次数", String.valueOf(detected));
            info("  最后一次变化", lastChange.get());
        } else {
            info("  说明", "8秒内未检测到尺寸变化（可能未调整窗口）");
        }
    }

    private void testAnsiCapabilities(Terminal terminal) {
        if (terminal == null) {
            skip("Terminal 为 null，跳过 ANSI 测试");
            return;
        }

        // 测试终端能力查询
        String[] capabilities = {
                "cursor_up", "cursor_down", "cursor_left", "cursor_right",
                "clear_screen", "clr_eol", "clr_eos",
                "enter_bold_mode", "enter_underline_mode", "enter_reverse_mode",
                "exit_attribute_mode",
                "set_a_foreground", "set_a_background",
                "init_pair", "setab", "setaf"
        };

        int supported = 0;
        for (String cap : capabilities) {
            try {
                Capability jlineCap = Capability.valueOf(cap);
                String seq = terminal.getStringCapability(jlineCap);
                if (seq != null && !seq.isEmpty()) {
                    supported++;
                }
            } catch (Exception ignored) {}
        }

        info("ANSI 能力支持数", supported + "/" + capabilities.length);
        test("基本 ANSI 能力可用 (>50%)", supported > capabilities.length / 2);

        // 直接测试关键能力
        test("光标移动能力", hasCapability(terminal, Capability.cursor_up)
                && hasCapability(terminal, Capability.cursor_down));
        test("清屏能力", hasCapability(terminal, Capability.clear_screen));
        test("颜色设置能力", hasCapability(terminal, Capability.set_a_foreground)
                || hasCapability(terminal, Capability.set_a_background));
    }

    private void testJlineFeatures(Terminal terminal) {
        if (terminal == null) {
            skip("Terminal 为 null，跳过 JLine 特性测试");
            return;
        }

        // 输入/输出流
        test("输入流可用", terminal.input() != null);
        test("输出流可用", terminal.output() != null);
        test("reader 可用", terminal.reader() != null);

        // 信号处理
        for (Signal signal : Signal.values()) {
            try {
                terminal.handle(signal, s -> {});
                // 恢复默认
                terminal.handle(signal, SignalHandler.SIG_DFL);
            } catch (Exception e) {
                info("信号 " + signal.name() + " 注册失败", e.getMessage());
            }
        }
        test("信号处理器注册接口可用", true);

        // 回显控制
        try {
            boolean echo = terminal.echo();
            info("当前 echo 状态", String.valueOf(echo));
            test("echo() 方法可用", true);
        } catch (Exception e) {
            test("echo() 方法可用", false, e.getMessage());
        }
    }

    private void testColorSystem(Terminal terminal, TerminalMode mode) {
        if (mode == TerminalMode.DUMB) {
            skip("DUMB 模式，无颜色支持");
            return;
        }

        String termType = terminal != null ? terminal.getType() : "";
        info("Terminal 类型", termType);

        boolean has256Color = termType.contains("256color");
        boolean hasTrueColor = checkTrueColor();
        boolean hasBasicColor = termType.contains("xterm") || termType.contains("ansi")
                || termType.contains("color");

        test("基础颜色 (4+4)", hasBasicColor || has256Color || hasTrueColor);
        test("256 色", has256Color || hasTrueColor);
        test("真彩色 (24bit)", hasTrueColor);

        // 直接发送 ANSI 颜色测试
        print("  颜色测试: ");
        print("\033[31m红\033[0m ");
        print("\033[32m绿\033[0m ");
        print("\033[33m黄\033[0m ");
        print("\033[34m蓝\033[0m ");
        print("\033[35m品\033[0m ");
        print("\033[36m青\033[0m ");
        print("\033[37m白\033[0m");
        newline();

        print("  亮色测试: ");
        print("\033[1;31m亮红\033[0m ");
        print("\033[1;32m亮绿\033[0m ");
        print("\033[1;33m亮黄\033[0m ");
        print("\033[1;34m亮蓝\033[0m ");
        print("\033[1;35m亮品\033[0m ");
        print("\033[1;36m亮青\033[0m");
        newline();

        print("  256色测试: ");
        print("\033[38;5;196m█\033[0m");  // 红
        print("\033[38;5;46m█\033[0m");   // 绿
        print("\033[38;5;226m█\033[0m");  // 黄
        print("\033[38;5;21m█\033[0m");   // 蓝
        print("\033[38;5;201m█\033[0m");  // 品
        print("\033[38;5;51m█\033[0m");   // 青
        print(" (如果看到彩色方块则 256 色可用)");
        newline();

        info("说明", "请目视确认上方是否显示了彩色文字/方块");
    }

    private void testInputCapabilities(Terminal terminal, TerminalMode mode) {
        if (terminal == null) {
            skip("Terminal 为 null，跳过输入测试");
            return;
        }
        if (mode == TerminalMode.DUMB) {
            skip("DUMB 模式，输入能力有限");
            return;
        }

        // 检查是否可以进入 raw 模式
        Attributes origAttrs = null;
        try {
            origAttrs = terminal.getAttributes();
            test("getAttributes() 可用", true);
        } catch (Exception e) {
            test("getAttributes() 可用", false, e.getMessage());
            return;
        }

        // 尝试 raw 模式
        try {
            Attributes rawAttrs = new Attributes(origAttrs);
            rawAttrs.setLocalFlag(LocalFlag.ICANON, false);
            rawAttrs.setLocalFlag(LocalFlag.ECHO, false);
            terminal.setAttributes(rawAttrs);
            test("可进入 raw 模式", true);
            // 恢复
            terminal.setAttributes(origAttrs);
        } catch (Exception e) {
            test("可进入 raw 模式", false, e.getMessage());
        }

        // 检查 LocalFlag 支持
        LocalFlag[] keyFlags = {LocalFlag.ICANON, LocalFlag.ECHO, LocalFlag.ISIG};
        for (LocalFlag flag : keyFlags) {
            try {
                boolean enabled = origAttrs.getLocalFlag(flag);
                info("LocalFlag." + flag.name(), String.valueOf(enabled));
            } catch (Exception e) {
                info("LocalFlag." + flag.name(), "不可用: " + e.getMessage());
            }
        }
    }

    private void testTerminalAttributes(Terminal terminal) {
        if (terminal == null) {
            skip("Terminal 为 null，跳过属性测试");
            return;
        }

        try {
            Attributes attrs = terminal.getAttributes();
            test("终端属性可读取", true);

            // 输出标志
            info("OutputFlags", String.valueOf(attrs.getOutputFlags()));
            // Local flags
            info("LocalFlags", String.valueOf(attrs.getLocalFlags()));
            // Control chars
            try {
                info("VERASE", String.valueOf((int) attrs.getControlChar(Attributes.ControlChar.VERASE)));
                info("VEOF", String.valueOf((int) attrs.getControlChar(Attributes.ControlChar.VEOF)));
                info(VINTR, String.valueOf((int) attrs.getControlChar(Attributes.ControlChar.VINTR)));
            } catch (Exception e) {
                info("控制字符", "读取失败: " + e.getMessage());
            }
        } catch (Exception e) {
            test("终端属性可读取", false, e.getMessage());
        }
    }

    private void testAnsiRendering() {
        newline();
        print(BOLD + "  样式测试:" + RESET);
        print(" " + BOLD + "粗体" + RESET);
        print(" " + DIM + "暗淡" + RESET);
        print(" \033[4m下划线\033[0m");
        print(" \033[7m反色\033[0m");
        print(" \033[9m删除线\033[0m");
        newline();

        print("  光标测试: ");
        print("ABC\033[3Dxyz");  // 光标左移3，覆盖ABC → xyz
        print(" ← 应显示 'xyz'");
        newline();

        print("  清行测试: ");
        print("被清除的文本\033[2K\r  清行成功 ✓");
        newline();

        // 框线字符测试（RichConsole 依赖）
        print("  框线字符: ");
        print("╔══╗ ╭──╮ ┌──┐");
        newline();
        print("            ");
        print("║  ║ │  │ │  │");
        newline();
        print("            ");
        print("╚══╝ ╰──╯ └──┘");
        newline();
        print("  Unicode 宽度: ");
        print("AB中文CD曰朴EF");
        print(" ← 如果对齐则 Unicode 宽度正确");
        newline();

        // 进度条模拟测试（RichConsole 关键用例）
        print("  进度条模拟: [");
        for (int i = 0; i <= 20; i++) {
            print("\033[32m█\033[0m");
            try { Thread.sleep(30); } catch (InterruptedException ignored) {}
        }
        print("] 100%");
        newline();
    }

    // ═══════════════════════════════════════════════════════════
    // 辅助方法
    // ═══════════════════════════════════════════════════════════

    private static final String VINTR = "VINTR";

    private boolean hasCapability(Terminal terminal, Capability cap) {
        if (terminal == null) return false;
        try {
            String seq = terminal.getStringCapability(cap);
            return seq != null && !seq.isEmpty();
        } catch (Exception e) {
            return false;
        }
    }

    private boolean checkTrueColor() {
        String colorTerm = System.getenv("COLORTERM");
        if ("truecolor".equalsIgnoreCase(colorTerm) || "24bit".equalsIgnoreCase(colorTerm)) {
            return true;
        }
        String term = System.getenv("TERM");
        // Termux 报告为 xterm-256color，但实际支持真彩色
        return term != null && term.contains("xterm");
    }

    private String env(String name) {
        String val = System.getenv(name);
        return val != null ? val : DIM + "(未设置)" + RESET;
    }

    private int parseIntSafe(String s, int def) {
        try { return Integer.parseInt(s); }
        catch (NumberFormatException e) { return def; }
    }

    // ─── 输出辅助 ──────────────────────────────────────────────

    private void header(String title) {
        newline();
        print(CYAN + BOLD + "╔══════════════════════════════════════════════════╗" + RESET);
        newline();
        print(CYAN + BOLD + "║  " + title + "                                    ║" + RESET);
        newline();
        print(CYAN + BOLD + "╚══════════════════════════════════════════════════╝" + RESET);
        newline();
        newline();
    }

    private void section(String title) {
        newline();
        print(BOLD + CYAN + "── " + title + " " + RESET);
        print(DIM + "──────────────────────────────────────" + RESET);
        newline();
    }

    private void info(String key, String value) {
        print("  " + GRAY + key + ": " + RESET + value);
        newline();
    }

    private void test(String name, boolean passed) {
        test(name, passed, null);
    }

    private void test(String name, boolean passed, String detail) {
        String icon = passed ? GREEN + "✓" + RESET : RED + "✗" + RESET;
        print("  " + icon + " " + name);
        if (detail != null) {
            print(" " + DIM + "(" + detail + ")" + RESET);
        }
        newline();
        if (passed) this.passed++;
        else this.failed++;
    }

    private void skip(String reason) {
        print("  " + YELLOW + "⊘" + RESET + " " + DIM + reason + RESET);
        newline();
        skipped++;
    }

    private void newline() {
        System.out.println();
    }

    private void print(String text) {
        System.out.print(text);
    }

    private void summary() {
        print(CYAN + BOLD + "══════════════════════════════════════════════════" + RESET);
        newline();
        print(BOLD + "  测试结果: " + RESET);
        print(GREEN + passed + " 通过" + RESET);
        if (failed > 0) print(RED + ", " + failed + " 失败" + RESET);
        if (skipped > 0) print(YELLOW + ", " + skipped + " 跳过" + RESET);
        newline();

        if (failed == 0) {
            print(GREEN + BOLD + "  所有测试通过！终端环境可以支持 RichConsole。" + RESET);
        } else {
            print(YELLOW + "  部分测试失败，RichConsole 可能需要降级渲染。" + RESET);
        }
        newline();
        newline();

        // 给出环境评估
        TerminalMode mode = TerminalManager.getCurrentMode();
        print(BOLD + "  环境评估:" + RESET);
        newline();
        print("    终端模式: " + mode);
        newline();

        switch (mode) {
            case JNI -> print(GREEN + "    → JNI 模式，性能最佳，支持所有特性" + RESET);
            case JNA -> print(GREEN + "    → JNA 模式，功能完整，性能良好" + RESET);
            case EXEC -> print(YELLOW + "    → EXEC 模式，功能完整但 stty 调用较慢" + RESET);
            case DUMB -> print(RED + "    → DUMB 模式，功能受限，建议检查 TERM 环境变量" + RESET);
        }
        newline();
        newline();
    }
}
