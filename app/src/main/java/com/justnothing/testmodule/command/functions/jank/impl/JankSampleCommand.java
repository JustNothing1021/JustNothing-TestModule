package com.justnothing.testmodule.command.functions.jank.impl;

import com.justnothing.testmodule.command.framework.CommandExecutor;
import com.justnothing.testmodule.command.framework.annotation.SubCommandInfo;
import com.justnothing.testmodule.command.framework.i18n.Text;
import com.justnothing.testmodule.command.functions.jank.model.JankFrame;
import com.justnothing.testmodule.command.functions.jank.model.ProcChurn;
import com.justnothing.testmodule.command.functions.jank.request.JankSampleRequest;
import com.justnothing.testmodule.command.functions.jank.response.JankResult;
import com.justnothing.testmodule.command.functions.jank.util.JankSampler;
import com.justnothing.testmodule.command.functions.jank.JankTexts;
import com.justnothing.richconsole.console.Console;
import com.justnothing.richconsole.live.Live;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 采样一段时间，实时显示卡顿相关指标。
 *
 * <p>每帧读的东西固定在 {@code /proc} 里（stat / meminfo / loadavg / 每进程 stat），
 * 不 hook 任何东西、不需要 root、也不写入设备 —— 它观察的就是"正常跑着的时候系统什么样"。</p>
 *
 * <h3>为什么全部用差分</h3>
 * <p>{@code /proc/stat} 给的是开机以来的累计 jiffies。直接拿它算占用，得到的是一小时、
 * 一天的平均值，瞬时卡顿会被彻底抹平（实测那台机器累计 9.8%，而采样时某些秒是满的）。
 * 所以第一帧只当基线，从第二帧起全部用"这一帧减上一帧"。</p>
 */
@SubCommandInfo(
    description = JankTexts.SUB_JANK_SAMPLE_DESC,
    usage = JankTexts.SUB_JANK_SAMPLE_USAGE,
    optionsDesc = JankTexts.SUB_JANK_SAMPLE_OPTIONS,
    examples = {
        "jank sample",
        "jank sample 30",
        "jank sample 60 -i 2000 -n 8",
    },
    seeAlso = {
        "threads list   看线程级别的占用",
        "memory info    看内存明细",
    }
)
public class JankSampleCommand extends AbstractJankCommand<JankSampleRequest> {

    /**
     * 采样间隔下限。
     *
     * <p>一帧要读四百多个 {@code /proc/<pid>/stat}。间隔压到 200ms 以下时，采样器占用的 CPU
     * 会和它要测的东西混在一起，量出来的"卡顿"有一半是自己造成的。真嫌分辨率不够，
     * 宁可拉长采样时长。</p>
     */
    private static final int MIN_INTERVAL_MS = 200;

    /** 汇总里最多列几个"最可疑"的进程。 */
    private static final int SUSPECT_LIMIT = 5;

    public JankSampleCommand() {
        super("jank sample", JankSampleRequest.class);
    }

    @Override
    protected JankResult executeInternal(CommandExecutor.CmdExecContext<JankSampleRequest> context)
            throws Exception {
        JankSampleRequest request = context.getRequest();
        int durationSec = Math.max(1, request.getDuration());
        int intervalMs = Math.max(MIN_INTERVAL_MS, request.getInterval());
        int topN = Math.clamp(request.getTop(), 1, 20);
        int ticks = Math.max(1, durationSec * 1000 / intervalMs);

        JankSampler sampler = new JankSampler(topN, CHURN_HISTORY);
        JankFrame baseline = sampler.next();
        int cores = Math.max(1, sampler.cores());

        Console console = context.supportsRichOutput() ? context.console() : null;
        boolean rich = console != null;
        // 预算只算一次：每帧重算的话，某一帧读不到数据时行数会变，整屏跟着跳一下，盯着看很难受。
        Budget budget = Budget.of(rich ? consoleHeight(console) : 24, cores, topN, CHURN_HISTORY);

        List<JankFrame> frames = new ArrayList<>(ticks);
        Map<String, Integer> suspectCounter = new HashMap<>();
        StringBuilder series = new StringBuilder();

        Live live = null;
        if (rich) {
            // 只让数据驱动重画：我们的帧一秒才变一次，而 Live 默认还挂着一个 4 次/秒的自刷新线程，
            // 每一跳都会把整屏重新渲染一遍 —— 这块表在手表上渲染一次要两百多毫秒 CPU，
            // 4 次/秒就是整整一个核（实测能把 system_server 顶到 100%），而且每次都是
            // "光标上移 + 清行 + 整屏重画"，肉眼看到的就是仪表盘一直在闪。
            live = new Live(buildFrame(baseline, cores, budget), console);
            live.setAutoRefresh(false);
            live.start(true);
        }
        boolean interrupted = false;
        try {
            for (int tick = 1; tick <= ticks; tick++) {
                try {
                    Thread.sleep(intervalMs);
                } catch (InterruptedException e) {
                    // 用户按了 Ctrl-C：此刻手上已经采到的帧仍然有价值，别把它丢掉，
                    // 更别把它当成"命令失败"—— 中断是最常见的结束方式。
                    //
                    // 这里刻意<b>不</b>调用 Thread.currentThread().interrupt() 把标志位还回去：
                    // 中断标志留着，后面的 socket 写（NIO）会直接抛 ClosedByInterruptException，
                    // 结果就是这个"成功返回部分数据"的语义反而发不出去。
                    interrupted = true;
                    break;
                }
                JankFrame frame = sampler.next();
                frame.tick = tick;
                frame.totalTicks = ticks;

                frames.add(frame);
                countSuspects(suspectCounter, frame);
                series.append(plainLine(frame, cores)).append('\n');

                if (live != null) {
                    // refresh=true：这一帧采完就画，不等定时器（定时器已经关掉了，见上面）
                    live.update(buildFrame(frame, cores, budget), true);
                } else {
                    context.println(plainLine(frame, cores));
                }
            }
        } finally {
            if (live != null) {
                live.stop();
            }
        }

        List<String> suspects = rankSuspects(suspectCounter);
        String summary = summarize(frames, sampler, cores, intervalMs, durationSec, suspects);
        if (interrupted) {
            summary = summary + Text.zhEn("\n（采样被中断，以上是中断前采到的 %d 帧）",
                    "\n(Sampling was interrupted; the %d frames above were collected before that)")
                    .format(frames.size());
        }
        context.println(summary);

        JankResult result = new JankResult(context.getRequest().getRequestId());
        result.setSuccess(true);
        result.setSubCommand("sample");
        result.setOutput(summary + Text.zhEn("\n\n逐帧记录:\n", "\n\nPer-frame records:\n").text() + series);
        result.setSuspects(suspects);
        result.setChurn(churnText(sampler));
        fillStats(result, frames, cores);

        if (interrupted) {
            result.setMessage(Text.zhEn("采样被中断，已返回中断前采到的 %d 帧",
                    "Sampling was interrupted; returning the %d frames collected before it")
                    .format(frames.size()));
        }
        return result;
    }

    private String summarize(List<JankFrame> frames, JankSampler sampler, int cores,
                             int intervalMs, int durationSec, List<String> suspects) {
        if (frames.isEmpty()) {
            return Text.zhEn("采样被中断，没有拿到任何帧。",
                    "Sampling was interrupted; no frames were collected.").text();
        }

        double sumBusy = 0;
        double peakBusy = 0;
        int busyFrames = 0;
        int hotFrames = 0;
        double peakQueue = 0;
        double sumMem = 0;
        int memFrames = 0;
        int peakDState = 0;
        long sumCost = 0;
        JankFrame worst = null;

        for (JankFrame frame : frames) {
            sumCost += frame.costMs;
            if (frame.cpuOk && !frame.baseline) {
                sumBusy += frame.busyPct;
                busyFrames++;
                if (frame.busyPct > peakBusy) {
                    peakBusy = frame.busyPct;
                    worst = frame;
                }
                if (frame.busyPct >= 85) {
                    hotFrames++;
                }
            }
            if (frame.loadOk) {
                peakQueue = Math.max(peakQueue, frame.runQueuePerCore(cores));
            }
            if (frame.memOk) {
                sumMem += frame.memUsedPct;
                memFrames++;
            }
            if (frame.procOk) {
                peakDState = Math.max(peakDState, frame.dStateCount);
            }
        }

        JankFrame last = frames.get(frames.size() - 1);
        StringBuilder text = new StringBuilder();
        text.append(Text.zhEn("采样 %d 帧 / %ds（间隔 %dms，单帧平均耗时 %dms）\n",
                "Sampled %d frames / %ds (interval %dms, %dms average per frame)\n")
                .format(frames.size(), durationSec, intervalMs, sumCost / frames.size()));

        if (busyFrames > 0) {
            text.append(String.format(Locale.US,
                    Text.zhEn("CPU    平均 %.0f%%   峰值 %.0f%%   ≥85%% 的有 %d 帧\n",
                            "CPU    avg %.0f%%   peak %.0f%%   %d frames at ≥85%%\n").text(),
                    sumBusy / busyFrames, peakBusy, hotFrames));
        } else {
            text.append(Text.zhEn("CPU    不可用（无法读取 /proc/stat）\n",
                    "CPU    unavailable (cannot read /proc/stat)\n").text());
        }
        if (peakQueue > 0) {
            text.append(String.format(Locale.US,
                    Text.zhEn("队列   峰值 %.1f/核（共 %d 核）\n",
                            "Queue   peak %.1f/core (%d cores in total)\n").text(),
                    peakQueue, cores));
        }
        if (memFrames > 0) {
            text.append(String.format(Locale.US,
                    Text.zhEn("内存   平均 %.0f%%   当前 %s / %s\n",
                            "Memory   avg %.0f%%   now %s / %s\n").text(),
                    sumMem / memFrames, humanKb(last.memUsedKb), humanKb(last.memTotalKb)));
        }
        if (last.procOk) {
            text.append(Text.zhEn("进程   %d 个，线程 %d 个，D 状态峰值 %d\n",
                    "Processes   %d, threads %d, peak D-state %d\n")
                    .format(last.pidDirCount, last.threadCount, peakDState));
            if (last.pidDirCount < 40) {
                // 这一条很关键：hidepid=2 只放行 readproc 组，普通应用注入模式下看到的就是个位数，
                // 不说明的话用户会以为系统真的很干净
                text.append(Text.zhEn("       看得见的进程数偏少：当前进程不在 readproc 组，被 /proc 的 hidepid=2 挡住了。走 sinteractive（system_server，组里有 readproc）才能看到全量。\n",
                        "       Fewer processes are visible than expected: this process is not in the readproc group, so /proc's hidepid=2 hides them. Run via sinteractive (system_server, which has readproc in its group) to see all of them.\n").text());
            }
        }

        appendWorst(text, worst, cores);
        appendSuspects(text, suspects);
        appendChurn(text, sampler);
        return text.toString();
    }

    /** 峰值那一帧是谁在吃 CPU。 */
    private void appendWorst(StringBuilder text, JankFrame worst, int cores) {
        if (worst == null || worst.topCpu.isEmpty()) {
            return;
        }
        text.append(String.format(Locale.US,
                Text.zhEn("\nCPU 峰值那一帧（第 %d 帧，%.0f%%）主要是：\n",
                        "\nThe frame with the CPU peak (frame %d, %.0f%%):\n").text(),
                worst.tick, worst.busyPct));
        int shown = 0;
        for (JankFrame.TopEntry entry : worst.topCpu) {
            if (shown >= 3) {
                break;
            }
            text.append(String.format(Locale.US, "  %-16s %6.1f%%  (pid %d)\n",
                    clip(entry.name, 16), entry.pct, entry.pid));
            shown++;
        }
        if (worst.dStateCount > 0) {
            List<String> stuck = dStateNames(worst);
            if (!stuck.isEmpty()) {
                text.append(Text.zhEn("  同时有 %d 个进程卡在 D 状态：%s\n",
                        "  %d processes are also stuck in D state: %s\n")
                        .format(worst.dStateCount, join(stuck, JankTexts.LIST_SEPARATOR.text())));
            }
        }
    }

    /** 这段时间里反复上 TOP 榜的进程 —— 卡顿的惯犯基本都在这里。 */
    private void appendSuspects(StringBuilder text, List<String> suspects) {
        if (suspects.isEmpty()) {
            return;
        }
        text.append(Text.zhEn("\n反复上榜的进程（上榜 = 那一帧进了 CPU TOP）：\n",
                "\nProcesses that keep showing up (a hit means they made that frame's CPU TOP list):\n").text());
        for (String suspect : suspects) {
            text.append("  ").append(suspect).append('\n');
        }
    }

    private void appendChurn(StringBuilder text, JankSampler sampler) {
        List<ProcChurn> churn = sampler.churn();
        if (churn.isEmpty()) {
            return;
        }
        int shown = Math.min(6, churn.size());
        text.append(Text.zhEn("\n采样期间进程变动 %d 次，最近 %d 条：",
                "\n%d process changes during sampling, the latest %d:")
                .format(churn.size(), shown));
        for (int i = 0; i < shown; i++) {
            text.append(i == 0 ? " " : JankTexts.LIST_SEPARATOR.text()).append(churn.get(i));
        }
        text.append('\n');
    }

    // =========================================================================
    // 结构化字段
    // =========================================================================

    private void fillStats(JankResult result, List<JankFrame> frames, int cores) {
        result.setSamples(frames.size());

        if (frames.isEmpty()) {
            return;
        }
        double sumBusy = 0;
        double peakBusy = 0;
        int busyFrames = 0;
        int hotFrames = 0;
        double peakQueue = 0;
        double sumMem = 0;
        int memFrames = 0;
        int peakDState = 0;
        long sumCost = 0;

        for (JankFrame frame : frames) {
            sumCost += frame.costMs;
            if (frame.cpuOk && !frame.baseline) {
                sumBusy += frame.busyPct;
                busyFrames++;
                peakBusy = Math.max(peakBusy, frame.busyPct);
                if (frame.busyPct >= 85) {
                    hotFrames++;
                }
            }
            if (frame.loadOk) {
                peakQueue = Math.max(peakQueue, frame.runQueuePerCore(cores));
            }
            if (frame.memOk) {
                sumMem += frame.memUsedPct;
                memFrames++;
            }
            if (frame.procOk) {
                peakDState = Math.max(peakDState, frame.dStateCount);
            }
        }

        JankFrame last = frames.get(frames.size() - 1);
        result.setAvgBusyPct(busyFrames > 0 ? sumBusy / busyFrames : 0);
        result.setPeakBusyPct(peakBusy);
        result.setHotFrames(hotFrames);
        result.setPeakRunQueuePerCore(peakQueue);
        result.setAvgMemUsedPct(memFrames > 0 ? sumMem / memFrames : 0);
        result.setProcCount(last.pidDirCount);
        result.setThreadCount(last.threadCount);
        result.setPeakDState(peakDState);
        result.setAvgSampleCostMs(sumCost / frames.size());
    }

    /** TOP 榜单出现次数统计；key 是进程名。 */
    private static void countSuspects(Map<String, Integer> counter, JankFrame frame) {
        for (JankFrame.TopEntry entry : frame.topCpu) {
            String name = clip(entry.name, 20);
            counter.compute(name, (k, count) -> count == null ? 1 : count + 1);
        }
    }

    /** 进程流水转成字符串列表，给结构化字段用。 */
    private static List<String> churnText(JankSampler sampler) {
        List<String> lines = new ArrayList<>();
        for (ProcChurn event : sampler.churn()) {
            lines.add(event.toString());
        }
        return lines;
    }

    private static List<String> rankSuspects(Map<String, Integer> counter) {
        List<Map.Entry<String, Integer>> entries = new ArrayList<>(counter.entrySet());
        Collections.sort(entries, (a, b) -> b.getValue().compareTo(a.getValue()));
        List<String> ranked = new ArrayList<>();
        for (int i = 0; i < entries.size() && i < SUSPECT_LIMIT; i++) {
            ranked.add(Text.zhEn("%s ×%d 帧", "%s ×%d frames")
                    .format(entries.get(i).getKey(), entries.get(i).getValue()));
        }
        return ranked;
    }

    private static String join(List<String> items, String separator) {
        StringBuilder text = new StringBuilder();
        for (int i = 0; i < items.size(); i++) {
            if (i > 0) {
                text.append(separator);
            }
            text.append(items.get(i));
        }
        return text.toString();
    }
}
