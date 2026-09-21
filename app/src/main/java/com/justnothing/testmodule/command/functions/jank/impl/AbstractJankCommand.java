package com.justnothing.testmodule.command.functions.jank.impl;

import com.justnothing.testmodule.command.framework.CommandExecutor;
import com.justnothing.testmodule.command.framework.i18n.Text;
import com.justnothing.testmodule.command.framework.model.AbstractCommand;
import com.justnothing.testmodule.command.framework.model.CommandRequest;
import com.justnothing.testmodule.command.framework.output.Colors;
import com.justnothing.testmodule.command.functions.jank.model.JankFrame;
import com.justnothing.testmodule.command.functions.jank.model.ProcChurn;
import com.justnothing.testmodule.command.functions.jank.model.ProcessEntry;
import com.justnothing.testmodule.command.functions.jank.response.JankResult;
import com.justnothing.testmodule.command.functions.jank.JankTexts;
import com.justnothing.richconsole.console.Console;
import com.justnothing.richconsole.console.Group;
import com.justnothing.richconsole.layout.Layout;
import com.justnothing.richconsole.panel.Panel;
import com.justnothing.richconsole.progressbar.ProgressBar;
import com.justnothing.richconsole.table.Table;
import com.justnothing.testmodule.utils.logging.Logger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public abstract class AbstractJankCommand<Req extends CommandRequest<?>>
        extends AbstractCommand<Req, JankResult> {

    protected static final Logger logger = Logger.getLoggerForName("JankCmd");

    /** 进度条列宽：固定值，不随终端缩放 —— 缩放会让每帧的条形长度跳来跳去，反而更难比较。 */
    private static final int BAR_WIDTH = 12;

    /** 右列状态面板里的条要窄得多，理由见 {@link #statusTable}。 */
    private static final int STATUS_BAR_WIDTH = 6;

    /** 进程流水最多留几条（真正显示几条另外按高度算）。 */
    protected static final int CHURN_HISTORY = 8;

    protected AbstractJankCommand(String commandName, Class<Req> requestType) {
        super(commandName, requestType, JankResult.class);
    }

    protected abstract JankResult executeInternal(CommandExecutor.CmdExecContext<Req> context) throws Exception;

    protected void out(CommandExecutor.CmdExecContext<?> context, String message) {
        context.println(message, Colors.DEFAULT);
    }

    protected void out(CommandExecutor.CmdExecContext<?> context, String message, byte color) {
        context.println(message, color);
    }

    // =========================================================================
    // 分档上色
    // =========================================================================

    protected static String levelCpu(double pct) {
        if (pct >= 85) {
            return "red";
        }
        if (pct >= 60) {
            return "yellow";
        }
        return "green";
    }

    protected static String levelMem(double pct) {
        if (pct >= 92) {
            return "red";
        }
        if (pct >= 80) {
            return "yellow";
        }
        return "green";
    }

    protected static String style(String styleName, String text) {
        return "[" + styleName + "]" + text + "[/" + styleName + "]";
    }

    /**
     * 一条百分比进度条。
     */
    protected static Object bar(double pct) {
        return bar(pct, BAR_WIDTH);
    }

    /**
     * 指定宽度的进度条。
     */
    protected static Object bar(double pct, int width) {
        double completed = Math.max(0, Math.min(100, pct));
        return ProgressBar.of(cfg -> cfg
                .total(100.0)
                .completed(completed)
                .width(width)
                .style("bar.back")
                .completeStyle(levelCpu(pct)));
    }

    /** 按分档给百分比上色。 */
    protected static String coloredPct(double pct, String level) {
        return style(level, String.format(Locale.US, "%.0f%%", pct));
    }

    // =========================================================================
    // 组帧
    // =========================================================================

    protected Object buildFrame(JankFrame frame, int cores, Budget budget) {
        Layout left = new Layout(new Group(Arrays.asList(
                Panel.of(cpuTable(frame, cores), cfg -> cfg.title("CPU").expand(true)),
                Panel.of(memTable(frame), cfg -> cfg.title(Text.zhEn("内存", "Memory").text()).expand(true)),
                Panel.of(topTable(frame, budget.topRows),
                        cfg -> cfg.title("CPU TOP").expand(true)))));

        Layout right = new Layout(new Group(Arrays.asList(
                Panel.of(churnTable(frame, budget.churnRows),
                        cfg -> cfg.title(Text.zhEn("进程变动", "Process churn").text()).expand(true)),
                Panel.of(statusTable(frame, cores), cfg -> cfg.title(Text.zhEn("状态", "Status").text()).expand(true)))));

        Layout root = new Layout((Object) null);
        root.splitColumn(left, right);
        left.setRatio(2);
        right.setRatio(1);
        return root;
    }

    private static Table cpuTable(JankFrame frame, int cores) {
        Table table = plainTable();
        table.addColumn(JankTexts.TABLE_ITEM.text(), "cyan", null);
        table.addColumn("", null, null);
        table.addColumn(JankTexts.TABLE_VALUE.text(), null, "right");

        if (!frame.cpuOk) {
            table.addRow(style("red", Text.zhEn("无法读取 /proc/stat", "Cannot read /proc/stat").text()),
                    "", "");
            return table;
        }
        if (frame.baseline) {
            table.addRow(Text.zhEn("基线", "Baseline").text(), bar(0), style("dim", "…"));
            return table;
        }
        table.addRow(Text.zhEn("总状态", "Overall").text(), bar(frame.busyPct),
                coloredPct(frame.busyPct, levelCpu(frame.busyPct)));
        for (int i = 0; i < frame.corePct.length && i < cores; i++) {
            double pct = frame.corePct[i];
            table.addRow(Text.zhEn("核 #%d", "Core #%d").format(i), bar(pct), coloredPct(pct, levelCpu(pct)));
        }
        return table;
    }

    private static Table memTable(JankFrame frame) {
        Table table = plainTable();
        table.addColumn(JankTexts.TABLE_ITEM.text(), "cyan", null);
        table.addColumn("", null, null);
        table.addColumn(JankTexts.TABLE_VALUE.text(), null, "right");

        if (!frame.memOk) {
            table.addRow(style("red", Text.zhEn("无法读取 /proc/meminfo", "Cannot read /proc/meminfo").text()),
                    "", "");
            return table;
        }
        table.addRow(Text.zhEn("已用", "Used").text(), bar(frame.memUsedPct),
                coloredPct(frame.memUsedPct, levelMem(frame.memUsedPct)));
        table.addRow(Text.zhEn("可用", "Available").text(), "", style(levelMem(frame.memUsedPct), humanKb(
                Math.max(0, frame.memTotalKb - frame.memUsedKb))));
        if (frame.swapOk) {
            table.addRow(Text.zhEn("交换空间", "Swap").text(), bar(frame.swapUsedPct),
                    style(levelMem(frame.swapUsedPct), humanKb(frame.swapUsedKb)));
        }
        return table;
    }


    private static Table topTable(JankFrame frame, int topRows) {
        Table table = plainTable();
        table.addColumn(Text.zhEn("占用", "Usage").text(), null, "right");
        noWrap(table.addColumn(JankTexts.TABLE_PROCESS.text(), null, null));

        if (!frame.procOk) {
            table.addRow(style("dim", "—"), style("red", Text.zhEn("无法读取 /proc（hidepid？）",
                    "Cannot read /proc (hidepid?)").text()));
            return table;
        }
        if (frame.baseline) {
            table.addRow("", style("dim", Text.zhEn("正在建立基线…", "Establishing baseline…").text()));
            return table;
        }
        if (frame.topCpu.isEmpty()) {
            table.addRow("", style("dim", Text.zhEn("（这一帧没有进程在吃 CPU）",
                    "(no process is burning CPU this frame)").text()));
            return table;
        }
        int rows = 0;
        for (JankFrame.TopEntry entry : frame.topCpu) {
            if (rows >= topRows) {
                break;
            }
            String pctText = String.format(Locale.US, "%.1f%%", entry.pct);
            // 用 displayName 而不是 comm：内核 comm 只有 15 个字符，HAL 服务名一律被截断
            // （android.hardware.wifi@1.0-service → android.hardwar）。
            table.addRow(style(levelCpu(entry.pct), pctText), entry.displayName);
            rows++;
        }
        return table;
    }

    private static Table churnTable(JankFrame frame, int churnRows) {
        // 单列而不是"变动 + 进程"两列：右列本来就窄，多一列就多一条竖线加两侧 padding，
        // 名字列会从十几个字符缩到七八个 —— 而进程名（comm）最长 15 个字符，
        // 缩到 8 个就只能显示 "com.topj…"，等于白给。合成一列后 "+PID 名字" 正好一行。
        Table table = plainTable();
        noWrap(table.addColumn(Text.zhEn("变动", "Change").text(), null, null));

        if (frame.churn.isEmpty()) {
            table.addRow(style("dim", Text.zhEn("（暂无变动）", "(no changes yet)").text()));
            return table;
        }
        int rows = 0;
        for (ProcChurn event : frame.churn) {
            if (rows >= churnRows) {
                break;
            }
            String prefix = (event.added ? "+" : "-") + event.pid;
            String color = event.added ? "green" : "red";
            // 颜色只包住 +PID 那段（整行都染色反而更花）
            table.addRow(style(color, prefix) + " " + (event.name != null ? event.name : "?"));
            rows++;
        }
        return table;
    }

    private static void noWrap(Table.TableColumn column) {
        column.setNoWrap(true);
        column.setOverflow("ellipsis");
    }

    private static Table statusTable(JankFrame frame, int cores) {
        Table table = plainTable();
        table.addColumn(JankTexts.TABLE_ITEM.text(), "cyan", null);
        table.addColumn("", null, null);
        table.addColumn(JankTexts.TABLE_VALUE.text(), null, "right");

        double progress = frame.totalTicks > 0 ? 100.0 * frame.tick / frame.totalTicks : 0;
        table.addRow(Text.zhEn("采样", "Sampling").text(), bar(progress, STATUS_BAR_WIDTH),
                String.format(Locale.US, "%.0f%%", progress));

        if (frame.loadOk) {
            double queuePct = 100.0 * frame.runQueuePerCore(cores);
            table.addRow(Text.zhEn("队列", "Queue").text(), bar(queuePct, STATUS_BAR_WIDTH),
                    style(levelCpu(queuePct), String.format(Locale.US, "%.1f", frame.runQueuePerCore(cores))));
        }
        if (frame.cpuOk && !frame.baseline) {
            table.addRow("user", "", String.format(Locale.US, "%.0f%%", frame.userPct));
            table.addRow("sys", "", String.format(Locale.US, "%.0f%%", frame.systemPct));
            // iowait 高本身就是卡顿的常见原因（等 IO 的进程全是 D 状态），单独按 CPU 的档上色
            table.addRow("io", "", coloredPct(frame.iowaitPct, levelCpu(frame.iowaitPct)));
        }
        if (frame.procOk) {
            table.addRow(JankTexts.TABLE_PROCESS.text(), "", String.valueOf(frame.pidDirCount));
            table.addRow(Text.zhEn("线程", "Thread").text(), "", String.valueOf(frame.threadCount));
            table.addRow("D", "", frame.dStateCount > 0
                    ? style("yellow", String.valueOf(frame.dStateCount))
                    : String.valueOf(frame.dStateCount));
        }
        table.addRow(Text.zhEn("耗时", "Time").text(), "", frame.costMs + "ms");
        return table;
    }

    /** 所有面板内的小表统一：撑满宽度、不要表头（表头那两行在手表上纯属浪费）。 */
    private static Table plainTable() {
        return Table.of(cfg -> cfg.expand(true).showHeader(false));
    }


    protected static class Budget {
        public final int topRows;
        public final int churnRows;

        private Budget(int topRows, int churnRows) {
            this.topRows = topRows;
            this.churnRows = churnRows;
        }


        public static Budget of(int screenHeight, int cores, int wantedTop, int wantedChurn) {
            // 左列 = CPU 面板（2 边框 + 上下表框 2 + 表头 1 + 核数）+ 内存面板（2 + 2 + 3）
            //        + TOP 面板（2 + 2 + 行数）；末尾再留 1 行余量
            int topRows = Math.clamp(screenHeight - (16 + cores) - 1, 1, wantedTop);
            // 右列 = 流水面板（2 + 2 + 行数）+ 状态面板（2 + 2 + 9 行）
            int churnRows = Math.clamp(screenHeight - (4 + 13) - 1, 1, wantedChurn);
            return new Budget(topRows, churnRows);
        }
    }

    protected static int consoleHeight(Console console) {
        try {
            int height = console.getHeight();
            return height > 0 ? height : 24;
        } catch (Throwable t) {
            return 24;
        }
    }

    // =========================================================================
    // 纯文本降级
    // =========================================================================

    /**
     * 一帧压缩成一行。
     */
    protected static String plainLine(JankFrame frame, int cores) {
        StringBuilder line = new StringBuilder();
        line.append(String.format(Locale.US, "[%2d/%d] ", frame.tick, frame.totalTicks));
        if (frame.cpuOk && !frame.baseline) {
            line.append(String.format(Locale.US, "CPU %5.1f%%", frame.busyPct));
            if (frame.corePct.length > 0) {
                line.append(Text.zhEn(" (核", " (cores ").text());
                for (int i = 0; i < frame.corePct.length && i < cores; i++) {
                    if (i > 0) {
                        line.append('/');
                    }
                    line.append(String.format(Locale.US, "%.0f", frame.corePct[i]));
                }
                line.append(')');
            }
        } else if (frame.baseline) {
            line.append(Text.zhEn("CPU   基线", "CPU   baseline").text());
        } else {
            // "读不到"和"基线"必须分开说：都写成"基线"的话，用户会以为再等一帧就有了
            line.append(Text.zhEn("CPU 不可用", "CPU unavailable").text());
        }
        if (frame.memOk) {
            line.append(String.format(Locale.US,
                    Text.zhEn(" | 内存 %.0f%% %s/%s", " | Mem %.0f%% %s/%s").text(),
                    frame.memUsedPct, humanKb(frame.memUsedKb), humanKb(frame.memTotalKb)));
        }
        if (frame.loadOk) {
            line.append(String.format(Locale.US,
                    Text.zhEn(" | 队列 %.1f/核", " | Queue %.1f/core").text(),
                    frame.runQueuePerCore(cores)));
        }
        if (frame.procOk) {
            line.append(String.format(Locale.US,
                    Text.zhEn(" | 进程 %d 线程 %d D %d", " | Process %d Thread %d D %d").text(),
                    frame.pidDirCount, frame.threadCount, frame.dStateCount));
        }
        line.append(String.format(Locale.US, " | %dms", frame.costMs));
        return line.toString();
    }

    /** 某一帧里最值得说的那一件事，没有就返回 null。 */
    protected static String hotspot(JankFrame frame, int cores) {
        if (frame.cpuOk && !frame.baseline && frame.busyPct >= 85) {
            return String.format(Locale.US,
                    Text.zhEn("CPU 占用 %.0f%%", "CPU usage %.0f%%").text(), frame.busyPct);
        }
        if (frame.loadOk && frame.runQueuePerCore(cores) >= 2) {
            return String.format(Locale.US,
                    Text.zhEn("每核排队 %.1f", "Queue per core %.1f").text(), frame.runQueuePerCore(cores));
        }
        if (frame.dStateCount >= 3) {
            return Text.zhEn("%d 个进程卡在 D 状态（等 IO）",
                    "%d processes are stuck in D state (waiting on I/O)").format(frame.dStateCount);
        }
        if (frame.memOk && frame.memUsedPct >= 92) {
            return String.format(Locale.US,
                    Text.zhEn("内存占用 %.0f%%", "Memory usage %.0f%%").text(), frame.memUsedPct);
        }
        return null;
    }

    protected static String humanKb(long kb) {
        if (kb >= 1024 * 1024) {
            return String.format(Locale.US, "%.1f GB", kb / 1024.0 / 1024.0);
        }
        if (kb >= 1024) {
            return String.format(Locale.US, "%.0f MB", kb / 1024.0);
        }
        return kb + " kB";
    }

    /** 截断过长的进程名，避免折行把面板顶下去（真机上 name 最长 15 字符，这里留余量）。 */
    protected static String clip(String text, int max) {
        if (text == null) {
            return "?";
        }
        if (text.length() <= max) {
            return text;
        }
        return text.substring(0, max - 1) + "…";
    }

    /** 一组 D 状态进程的展示文本。 */
    protected static List<String> dStateNames(JankFrame frame) {
        List<String> names = new ArrayList<>();
        for (ProcessEntry entry : frame.dStateTop) {
            names.add(entry.pid + " " + clip(entry.name, 16));
        }
        return names;
    }
}
