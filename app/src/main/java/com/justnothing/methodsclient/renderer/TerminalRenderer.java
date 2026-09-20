package com.justnothing.methodsclient.renderer;

import com.justnothing.methodsclient.model.ColoredSegment;
import com.justnothing.methodsclient.utils.TerminalManager;
import com.justnothing.testmodule.command.framework.output.Colors;
import org.jline.terminal.Terminal;

import java.util.List;

/**
 * 终端渲染器：把流式输出打印到本地终端。
 * 命令结束（cmd.done）后补一个换行分隔。
 *
 * <p>{@code colored} 为 true 时按颜色映射为 ANSI 转义序列（彩色模式），
 * 为 false 时忽略颜色直接打印纯文本（对应 {@code --interactive-plain}）。</p>
 *
 * <p>对应迁移自 {@link com.justnothing.methodsclient.executor.SocketStreamReader}
 * 的 printAsANSI / getANSICode 逻辑。</p>
 */
public class TerminalRenderer implements OutputRenderer {

    private static final String RESET = "\u001B[0m";

    private final boolean colored;

    /** 彩色渲染（默认）。 */
    public TerminalRenderer() {
        this(true);
    }

    /**
     * @param colored true 输出 ANSI 颜色；false 输出纯文本
     */
    public TerminalRenderer(boolean colored) {
        this.colored = colored;
    }

    @Override
    public void onOutput(byte color, String text) {
        write(render(color, text));
    }

    /**
     * 一批输出一次性渲染、只 flush 一次。
     *
     * <p>服务端会把连续输出合并进同一帧；若逐段 flush，10000 段输出就是 10000 次 pty 写，
     * 客户端会变成瓶颈（服务端随之因背压阻塞）。</p>
     */
    @Override
    public void onOutputBatch(List<ColoredSegment> segments) {
        StringBuilder text = new StringBuilder();
        for (ColoredSegment segment : segments) {
            text.append(render(segment.color(), segment.text()));
        }
        write(text.toString());
    }

    private String render(byte color, String text) {
        return colored ? getANSICode(color) + text + RESET : text;
    }

    private void write(String text) {
        Terminal term = TerminalManager.getTerminal();
        if (term != null) {
            term.writer().print(text);
            term.writer().flush();
        } else {
            System.out.print(text);
            System.out.flush();
        }
    }

    @Override
    public void onResult(String resultJson) {
        // 终端模式不渲染结构化结果——用户直接看到流式输出
    }

    @Override
    public void onDone() {
        Terminal term = TerminalManager.getTerminal();
        if (term != null) {
            term.writer().println();
        } else {
            System.out.println();
        }
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
}
