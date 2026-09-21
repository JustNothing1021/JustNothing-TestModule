package com.justnothing.testmodule.command.functions.jank.impl;

import com.justnothing.testmodule.command.framework.CommandExecutor;
import com.justnothing.testmodule.command.framework.annotation.SubCommandInfo;
import com.justnothing.testmodule.command.framework.i18n.Text;
import com.justnothing.testmodule.command.framework.output.Colors;
import com.justnothing.testmodule.command.functions.jank.model.ProcSnapshot;
import com.justnothing.testmodule.command.functions.jank.model.ProcessEntry;
import com.justnothing.testmodule.command.functions.jank.request.JankWatchRequest;
import com.justnothing.testmodule.command.functions.jank.response.JankResult;
import com.justnothing.testmodule.command.functions.jank.util.ProcReader;
import com.justnothing.testmodule.command.functions.jank.JankTexts;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * 监视所有进程的出现与消失（差分流）。
 *
 * <h3>和 {@code jank sample} 的分工</h3>
 * <p>sample 回答"系统忙不忙"：每帧要读 {@code /proc/stat}、meminfo、loadavg 和四百多份
 * {@code /proc/<pid>/stat}，一帧一百多毫秒，所以它有固定时长。watch 只回答"进程表变没变"，
 * 每轮只做一次 readdir，只有新出现的 pid 才去读名字 —— 轻到可以一直挂着，
 * 也正因为它轻，它给被观察对象带来的扰动才最小。</p>
 *
 * <h3>输出格式</h3>
 * <p>参照 Android Studio 推到设备上的 {@code process-tracker}：第一屏把当前活着的进程
 * 全列一遍（{@code + pid 名字}），之后每轮只打印变动 —— 新增 {@code + pid 名字}、
 * 消失 {@code - pid 名字}。消失那条比它多带一个名字：那个名字在我们的表里本来就有，
 * 只报 pid 的话事后根本认不出是谁退了。</p>
 *
 * <h3>什么时候结束</h3>
 * <p>客户端断开（Ctrl-C 或关掉会话）后下一轮就会退出 —— 不然它会永远挂在那里扫 {@code /proc}。</p>
 */
@SubCommandInfo(
    description = JankTexts.SUB_JANK_WATCH_DESC,
    usage = JankTexts.SUB_JANK_WATCH_USAGE,
    optionsDesc = JankTexts.SUB_JANK_WATCH_OPTIONS,
    examples = {
        "jank watch",
        "jank watch -i 200",
    },
    seeAlso = {
        "jank sample   连续采样 CPU/内存/队列，看\"卡不卡\"",
        "threads list  看某个进程的线程",
    }
)
public class JankWatchCommand extends AbstractJankCommand<JankWatchRequest> {

    /** 间隔下限：再密下去，监视器自己的 readdir 也会挤进它自己的流水里。 */
    private static final int MIN_INTERVAL_MS = 100;

    /** 结构化结果里最多回带几条变动；完整的已经在终端里流过了，不该再往回塞一份。 */
    private static final int RECENT_LIMIT = 32;

    /** 看得见的 pid 目录少于这个数，基本就是被 hidepid 挡住了（正常是四百多）。 */
    private static final int BLIND_THRESHOLD = 40;

    public JankWatchCommand() {
        super("jank watch", JankWatchRequest.class);
    }

    @Override
    protected JankResult executeInternal(CommandExecutor.CmdExecContext<JankWatchRequest> context) {
        int intervalMs = Math.max(MIN_INTERVAL_MS, context.getRequest().getInterval());

        // 第一屏：把此刻活着的进程全列一遍。只扫这一次 —— 从这里往后，每轮只认 pid 集合。
        ProcSnapshot first = ProcReader.snapshot();
        Map<Integer, String> alive = new HashMap<>();
        for (ProcessEntry entry : first.processes) {
            String name = ProcReader.displayName(entry.pid, entry.name);
            alive.put(entry.pid, name);
            context.println("+ " + entry.pid + " " + name);
        }
        context.println("");
        context.println(Text.zhEn("（初始 %d 个进程，间隔 %dms；之后只打印变动，Ctrl-C 结束）",
                "(initially %d processes, interval %dms; only changes are printed from now on, Ctrl-C to exit)")
                .format(alive.size(), intervalMs));
        if (first.pidDirCount < BLIND_THRESHOLD) {
            context.println(Text.zhEn("注意：只看得见 %d 个 pid 目录，当前进程不在 readproc 组，被 /proc 的 hidepid=2 挡住了 —— 走 sinteractive（system_server）才看得见全量。",
                    "Note: only %d pid directories are visible; this process is not in the readproc group, so /proc's hidepid=2 hides them — run via sinteractive (system_server) to see all of them.")
                    .format(first.pidDirCount),
                    Colors.YELLOW);
        }

        long startNanos = System.nanoTime();
        List<String> recent = new ArrayList<>();
        int rounds = 0;
        int added = 0;
        int removed = 0;

        while (true) {
            try {
                Thread.sleep(intervalMs);
            } catch (InterruptedException e) {
                // 用户按了 Ctrl-C：把已经统计到的账结清再返回，中断不是失败。
                // 刻意不把中断标志还原：会话收尾还要往 socket 写，标志留着会让写直接抛异常。
                break;
            }
            if (context.output().isClosed()) {
                // 客户端已经断开：再采下去只是替一块手表白烧电。
                break;
            }
            rounds++;

            Set<Integer> current = new HashSet<>(ProcReader.pids());
            for (Integer pid : current) {
                if (alive.containsKey(pid)) {
                    continue;
                }
                ProcessEntry entry = ProcReader.readProcessStat(pid);
                if (entry == null) {
                    continue; // 刚起来又没了，不值得单独报一条
                }
                String name = ProcReader.displayName(pid, entry.name);
                alive.put(pid, name);
                added++;
                record(recent, "+ " + pid + " " + name);
                context.print("+ " + pid, Colors.GREEN);
                context.println(" " + name);
            }
            for (Iterator<Map.Entry<Integer, String>> it = alive.entrySet().iterator(); it.hasNext(); ) {
                Map.Entry<Integer, String> gone = it.next();
                if (current.contains(gone.getKey())) {
                    continue;
                }
                it.remove();
                removed++;
                record(recent, "- " + gone.getKey() + " " + gone.getValue());
                context.print("- " + gone.getKey(), Colors.RED);
                context.println(" " + gone.getValue());
            }
        }

        long seconds = Math.round((System.nanoTime() - startNanos) / 1_000_000_000.0);
        String summary = String.format(Locale.US,
                Text.zhEn("监视 %d s / %d 轮（间隔 %dms）：出现 %d 个，消失 %d 个，当前 %d 个进程",
                        "Watched %d s / %d rounds (interval %dms): %d appeared, %d disappeared, %d processes now").text(),
                seconds, rounds, intervalMs, added, removed, alive.size());
        if (!context.output().isClosed()) {
            context.println(summary);
        }

        JankResult result = new JankResult(context.getRequest().getRequestId());
        result.setSuccess(true);
        result.setSubCommand("watch");
        result.setOutput(summary);
        result.setSamples(rounds);
        result.setProcCount(alive.size());
        result.setChurn(recent);
        return result;
    }

    /** 只留最近几条给结构化结果：输出是流式的，结果里再放一份完整流水没有意义。 */
    private static void record(List<String> recent, String event) {
        recent.add(event);
        while (recent.size() > RECENT_LIMIT) {
            recent.remove(0);
        }
    }
}
