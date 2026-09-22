package com.justnothing.testmodule.command.framework.output;

import com.justnothing.richconsole.console.Console;
import com.justnothing.richconsole.console.Group;
import com.justnothing.richconsole.errors.ConsoleError;
import com.justnothing.richconsole.layout.Layout;
import com.justnothing.richconsole.panel.Panel;
import com.justnothing.richconsole.segment.Segment;
import com.justnothing.richconsole.table.Table;
import com.justnothing.testmodule.command.framework.CommandExecutor;
import com.justnothing.testmodule.command.framework.model.CommandRequest;

import org.jline.terminal.Terminal;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * 文件模式（{@code COMMAND_LINE}）下的降级 Console。
 *
 * <p>这里守两件在真机上才会显形的事：</p>
 * <ol>
 *   <li><b>输出要真的出来。</b>降级 Console 的 Terminal 必须显式绑到 {@code System.out}，
 *       这样 {@link SystemOutputRedirector} 才能把渲染结果转发进 output handler。绑不上的话输出
 *       掉进虚空 —— RichConsole 自建的系统终端直接写真实 fd，绕过 {@code System.setOut}。</li>
 *   <li><b>输入要立刻失败、不能挂死。</b>文件模式没有交互能力，如果 Console 看起来"能问"，
 *       服务端一主动询问就会阻塞在那里。</li>
 * </ol>
 *
 * <p>构造顺序有讲究：Console 必须在重定向<b>装上之后</b>才取（真实执行就是这样，
 * {@code console()} 是执行期间懒取的）。反过来它捕获的是被换掉的原始流，测试会假绿。</p>
 */
public class PlainConsoleFallbackTest {

    @Test
    public void consoleIsUsableInCommandLineModeAndReachesCollector() {
        StringBuilderCollector collector = new StringBuilderCollector();
        SystemOutputRedirector redirector = new SystemOutputRedirector(collector);
        String captured = "";

        redirector.startRedirect();
        try {
            Console console = contextWith(collector).console();
            assertNotNull("文件模式下 console() 不该是 null，否则命令渲染只能 NPE 或静默跳过", console);

            console.print("hello from console");
            System.out.flush();
            captured = collector.getString();
        } finally {
            redirector.stopRedirect();
        }

        assertTrue("降级 Console 的输出没落进 collector，实际内容: "
                + (captured.isEmpty() ? "(空)" : captured), captured.contains("hello from console"));
        assertFalse("文件模式是纯文本，不该混进 ANSI 转义: " + captured, captured.contains("\u001B["));
    }

    /**
     * 文件模式不能"看起来能交互"。{@code timeout} 本身就是断言的一部分 —— 真要挂在这里，
     * 它会先超时失败，而不是让整个测试套件停住。
     */
    @Test(timeout = 5000)
    public void fileModeConsoleNeverBlocksOnInput() throws Exception {
        Console console = contextWith(new StringBuilderCollector()).console();

        assertFalse("文件模式没有交互能力，Console 不该自报可交互 —— 否则主动询问就没法被拦住",
                console.isInteractive());

        Terminal terminal = console.getTerminal();
        assertNotNull(terminal);
        assertEquals("输入流应当立刻 EOF，不能阻塞等待：读输入的代码靠这个失败，挂住就是死锁",
                -1, terminal.reader().read());
    }

    /**
     * 最要命的一条：文件模式下命令要是主动询问用户，必须<b>立刻失败</b>。
     *
     * <p>以前这里会偷偷退回读服务端进程的 {@code System.in}，而那个流没人管 —— 阻塞就把服务端挂死，
     * 返回空串则会让 Prompt 的 {@code while(true)} 重试循环死转。</p>
     */
    @Test(timeout = 5000)
    public void askingForInputFailsFastInsteadOfHanging() {
        Console console = contextWith(new StringBuilderCollector()).console();
        try {
            String answer = console.input("请输入点什么: ");
            fail("文件模式下主动询问必须立刻抛异常，实际返回了: " + answer);
        } catch (ConsoleError expected) {
            // 正是要它响亮地炸，而不是换个来源再赌一次
        }
    }

    /** 走真实路径造 context：默认就是 {@code COMMAND_LINE}，且没有 consoleSupplier。 */
    private static CommandExecutor.CmdExecContext<CommandRequest<?>> contextWith(
            StringBuilderCollector collector) {
        return new CommandExecutor.CmdExecContext<>("help", new String[0], null, null, collector, null);
    }

    /**
     * RichConsole 的 {@code Layout} 必须真的渲染出内容。
     *
     * <p>Layout 曾经把子容器（{@code Group}）交回来的对象当 Segment 过滤掉 ——
     * {@code Group.richConsole} 只是把子对象原样交回来，指望 {@code Console.render} 去摊平，
     * 而 Layout 绕过了它。结果是整块布局静默渲染成 0 个 Segment：真机上仪表盘整个消失，
     * 日志里却一句错都没有。这里复现的就是那条路径（jank 的帧就是一个 Layout(Group(Panel))）。</p>
     */
    @Test
    public void layoutRenderableActuallyRendersContent() {
        StringBuilderCollector collector = new StringBuilderCollector();
        SystemOutputRedirector redirector = new SystemOutputRedirector(collector);
        String captured = "";

        redirector.startRedirect();
        try {
            Console console = contextWith(collector).console();
            Table table = Table.of(cfg -> cfg.expand(true).showHeader(false));
            table.addColumn("Item", "cyan", null);
            table.addRow("Overall", "12%");

            Layout layout = new Layout(new Group(Arrays.asList(
                    Panel.of(table, cfg -> cfg.title("CPU").expand(true)))));
            console.print(layout);
            System.out.flush();
            captured = collector.getString();
        } finally {
            redirector.stopRedirect();
        }

        assertTrue("Layout 渲染出来是空的，真机上就是「仪表盘不见了」，实际内容: "
                + (captured.isEmpty() ? "(空)" : captured),
                captured.contains("Overall") && captured.contains("CPU"));
    }

    /**
     * Table 渲染出来不能超过它自己拿到的最大宽度。
     *
     * <p>算列宽时只把「各列宽之和」控制在上限内，没扣表框自身的开销 —— 左右边 2 格加上
     * {@code numCols-1} 格列间分隔。整表因此比 maxWidth 宽出 {@code numCols+1} 格。
     * 裸着用时看不出来（终端自动折行），一旦外面套着 {@code Panel} / {@code Layout}，
     * 多出来的那截就被裁掉：真机上的样子是 <b>Panel 里那张表丢了右边框</b>（jank 的每个面板都是
     * {@code Panel(Table)}）。</p>
     */
    @Test
    public void tableStaysWithinItsMaxWidth() {
        Console console = contextWith(new StringBuilderCollector()).console();
        Table table = Table.of(cfg -> cfg.expand(true).showHeader(false));
        table.addColumn("Item", "cyan", null);
        table.addColumn("", null, null);
        table.addColumn("Value", null, "right");
        table.addRow("Overall", "---", "12%");
        table.addRow("Core #0", "---+", "30%");

        int width = console.getWidth();
        for (String line : renderToLines(console, table)) {
            assertTrue("表格渲染出 " + line.length() + " 格，超过了可用宽度 " + width
                    + "，超出的部分会被外层裁掉（就是「右边框消失」）：|" + line + "|",
                    line.length() <= width);
        }
    }

    /** 走 Segment 层取字符：这里只关心宽度，不经过终端编码。 */
    private static List<String> renderToLines(Console console, Object renderable) {
        List<String> lines = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        for (Segment segment : console.render(renderable, console.getOptions())) {
            for (char c : segment.getText().toCharArray()) {
                if (c == '\n') {
                    lines.add(current.toString());
                    current.setLength(0);
                } else {
                    current.append(c);
                }
            }
        }
        return lines;
    }
}
