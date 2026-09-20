package com.justnothing.testmodule.command.functions.jank.util;

import com.justnothing.testmodule.command.functions.jank.model.JankFrame;
import com.justnothing.testmodule.command.functions.jank.model.ProcChurn;
import com.justnothing.testmodule.command.functions.jank.model.ProcSnapshot;
import com.justnothing.testmodule.command.functions.jank.model.ProcessEntry;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 跨帧有状态的采样器：把 {@link ProcReader} 的原始读数变成 {@link JankFrame}。
 *
 * <p>状态只有三样，都是"两帧之间"才成立的东西：上一帧的快照、上一帧的 pid→进程 索引、
 * 以及进程流水队列。除此之外全部现算。</p>
 *
 * <p>线程安全性：不提供。采样循环只会在一个线程里跑，为它加锁只是给手表加开销。</p>
 */
public class JankSampler {

    private final int topN;
    private final int churnLimit;

    private ProcSnapshot prev;
    private Map<Integer, ProcessEntry> prevIndex;

    /**
     * pid → 展示名（{@code cmdline} 全名）。只为"真要显示的那几个进程"填，每帧清一次。
     *
     * <p>存在的唯一理由是<b>消失事件</b>：进程一死 {@code /proc/<pid>} 就没了，cmdline 读不到，
     * 只能靠当初 {@code +} 那次记下的名字，才能让后来的 {@code -6114} 和 {@code +6114}
     * 显示成同一个名字。一帧一清是因为 pid 会被回收，留着旧名字会显示到别的进程头上。</p>
     */
    private final Map<Integer, String> displayNames = new HashMap<>();

    /** 进程流水，最新的在前。 */
    private final Deque<ProcChurn> churn = new ArrayDeque<>();

    /**
     * @param topN       TOP 榜单长度
     * @param churnLimit 流水最多留几条
     */
    public JankSampler(int topN, int churnLimit) {
        this.topN = Math.max(1, topN);
        this.churnLimit = Math.max(1, churnLimit);
    }

    /** 已经攒下来的流水（最新在前）。 */
    public List<ProcChurn> churn() {
        return new ArrayList<>(churn);
    }

    /**
     * 抓一帧。
     *
     * <p>第一次调用只建立基线：没有上一帧就没有增量，算出来的任何百分比都是假的，
     * 所以这一帧会带 {@link JankFrame#baseline} 标记，界面应该据此显示"正在建立基线"。</p>
     */
    public JankFrame next() {
        ProcSnapshot cur = ProcReader.snapshot();
        Map<Integer, ProcessEntry> curIndex = index(cur);

        JankFrame frame = new JankFrame();
        frame.costMs = cur.costNanos / 1_000_000L;
        fillAbsolute(frame, cur);

        if (prev == null) {
            frame.baseline = true;
            frame.topCpu = Collections.emptyList();
        } else {
            fillDelta(frame, prev, cur, prevIndex, curIndex);
        }

        updateChurn(prev, cur, prevIndex, curIndex);
        frame.churn = new ArrayList<>(churn);

        // 该用的都用完了：只留本帧还活着的 pid，免得回收后的 pid 显示出上一个进程的名字。
        displayNames.keySet().retainAll(curIndex.keySet());

        prev = cur;
        prevIndex = curIndex;
        return frame;
    }

    /** 核数（用 CPU 行数减掉总体那行；读不到时按 1 处理，避免除零）。 */
    public int cores() {
        ProcSnapshot snapshot = prev;
        if (snapshot == null || snapshot.cpus == null || snapshot.cpus.length <= 1) {
            return 1;
        }
        return snapshot.cpus.length - 1;
    }

    // =========================================================================
    // 绝对量
    // =========================================================================

    private static void fillAbsolute(JankFrame frame, ProcSnapshot cur) {
        if (cur.cpus != null) {
            frame.cpuOk = true;
            frame.corePct = new double[Math.max(0, cur.cpus.length - 1)];
        }

        if (cur.memOk) {
            frame.memOk = true;
            frame.memTotalKb = cur.memTotalKb;
            frame.memUsedKb = Math.max(0, cur.memTotalKb - cur.memAvailableKb);
            frame.memUsedPct = pct(frame.memUsedKb, cur.memTotalKb);
        }
        if (cur.swapTotalKb > 0) {
            frame.swapOk = true;
            frame.swapTotalKb = cur.swapTotalKb;
            frame.swapUsedKb = Math.max(0, cur.swapTotalKb - cur.swapFreeKb);
            frame.swapUsedPct = pct(frame.swapUsedKb, cur.swapTotalKb);
        }

        if (cur.loadOk) {
            frame.loadOk = true;
            frame.load1 = cur.load1;
            frame.load5 = cur.load5;
            frame.load15 = cur.load15;
            frame.runnable = cur.runnable;
            frame.schedulerEntities = cur.schedulerEntities;
        }

        if (cur.processesOk) {
            frame.procOk = true;
            frame.pidDirCount = cur.pidDirCount;
            frame.statOkCount = cur.statOkCount;
            frame.threadCount = cur.threadCount;
            frame.dStateCount = cur.dStateCount;
            frame.dStateTop = dStateTop(cur.processes);
        }
    }

    /** D 状态进程的前几个，按 pid 从小到大 —— 挑不出"最重要的"，那就稳定地挑。 */
    private static List<ProcessEntry> dStateTop(List<ProcessEntry> processes) {
        if (processes == null) {
            return Collections.emptyList();
        }
        List<ProcessEntry> stuck = new ArrayList<>(4);
        for (ProcessEntry entry : processes) {
            if (entry.state == 'D') {
                stuck.add(entry);
                if (stuck.size() >= 4) {
                    break;
                }
            }
        }
        return stuck;
    }

    // =========================================================================
    // 增量
    // =========================================================================

    private void fillDelta(JankFrame frame, ProcSnapshot prev, ProcSnapshot cur,
                           Map<Integer, ProcessEntry> prevIndex,
                           Map<Integer, ProcessEntry> curIndex) {
        if (prev.cpus != null && cur.cpus != null && cur.cpus.length == prev.cpus.length) {
            diffCpu(frame, prev.cpus[0], cur.cpus[0]);
            for (int i = 1; i < cur.cpus.length; i++) {
                ProcSnapshot.Cpu before = prev.cpus[i];
                ProcSnapshot.Cpu after = cur.cpus[i];
                long total = after.total - before.total;
                frame.corePct[i - 1] = total > 0 ? pct(after.busy - before.busy, total) : 0;
            }
        }
        diffProcesses(frame, prevIndex, curIndex, cur, prev, topN);
    }

    private static void diffCpu(JankFrame frame, ProcSnapshot.Cpu before, ProcSnapshot.Cpu after) {
        long total = after.total - before.total;
        if (total <= 0) {
            // 两次采样之间一个 jiffy 都没走：要么间隔太短，要么时钟没动。保持 0 并留 cpuOk。
            return;
        }
        frame.busyPct = pct(after.busy - before.busy, total);
        frame.userPct = pct(after.userAll() - before.userAll(), total);
        frame.systemPct = pct(after.systemAll() - before.systemAll(), total);
        frame.iowaitPct = pct(after.iowait - before.iowait, total);
        frame.idlePct = pct(after.idle - before.idle, total);
    }

    /**
     * 按进程算 CPU 增量。
     *
     * <p>口径与 {@code top} 一致：<b>相对单个核</b>。四核机上把一个核跑满显示 100%，
     * 四个核跑满显示 400%。用总体增量当分母会让"跑满一个核"看起来只有 25%，
     * 恰恰把最该被看见的那种进程给缩小了。</p>
     */
    private void diffProcesses(JankFrame frame, Map<Integer, ProcessEntry> prevIndex,
                               Map<Integer, ProcessEntry> curIndex,
                               ProcSnapshot cur, ProcSnapshot prev, int topN) {
        int cores = Math.max(1, cur.cpus != null ? cur.cpus.length - 1 : 1);
        long totalDelta = 0;
        if (prev.cpus != null && cur.cpus != null && cur.cpus.length == prev.cpus.length) {
            totalDelta = cur.cpus[0].total - prev.cpus[0].total;
        }
        double perCore = totalDelta > 0 ? (double) totalDelta / cores : 0;

        long sum = 0;
        List<JankFrame.TopEntry> ranking = new ArrayList<>(curIndex.size());
        for (Map.Entry<Integer, ProcessEntry> e : curIndex.entrySet()) {
            ProcessEntry before = prevIndex.get(e.getKey());
            if (before == null) {
                continue; // 这一帧才出现的进程没有"上一帧"可比，算不出去占用
            }
            long delta = e.getValue().cpu - before.cpu;
            if (delta < 0) {
                continue; // pid 被回收了，计数器归零，差值没有意义
            }
            sum += delta;
            if (delta > 0 && perCore > 0) {
                ranking.add(new JankFrame.TopEntry(e.getKey(), e.getValue().name,
                        100.0 * delta / perCore));
            }
        }
        frame.procCpuDelta = sum;

        Collections.sort(ranking, new Comparator<JankFrame.TopEntry>() {
            @Override
            public int compare(JankFrame.TopEntry a, JankFrame.TopEntry b) {
                return Double.compare(b.pct, a.pct);
            }
        });
        frame.topCpu = ranking.size() > topN ? new ArrayList<>(ranking.subList(0, topN)) : ranking;
        filterZero(frame);
        // 榜单定下来了才去查全名：这要额外读一次 /proc/<pid>/cmdline，只有真上屏的这几行才值得，
        // 四百多个进程全查会让"采样耗时"翻近一倍。
        for (int i = 0; i < frame.topCpu.size(); i++) {
            JankFrame.TopEntry entry = frame.topCpu.get(i);
            frame.topCpu.set(i, new JankFrame.TopEntry(entry.pid, entry.name, entry.pct,
                    displayName(entry.pid, entry.name)));
        }
    }

    /** 榜单里一个进程都没动过时清空列表，避免界面上出现整列 0.0%。 */
    private static void filterZero(JankFrame frame) {
        if (frame.topCpu.isEmpty()) {
            return;
        }
        for (JankFrame.TopEntry entry : frame.topCpu) {
            if (entry.pct >= 0.05) {
                return;
            }
        }
        frame.topCpu = Collections.emptyList();
    }

    // =========================================================================
    // 进程流水
    // =========================================================================

    /**
     * 对比两帧的 pid 集合，把出现/消失记进流水。
     *
     * <p>名字统一走 {@link #displayName(ProcessEntry)}：出现事件当场查 cmdline 拿全名，
     * 消失事件用当初缓存下来的那个 —— 进程一死 {@code /proc/<pid>} 就没了，
     * 这时候再去读只会读到 null，流水里就只剩一串光秃秃的 pid。</p>
     */
    private void updateChurn(ProcSnapshot prev, ProcSnapshot cur,
                             Map<Integer, ProcessEntry> prevIndex,
                             Map<Integer, ProcessEntry> curIndex) {
        if (prev == null) {
            return; // 第一帧是基线：此刻系统里所有进程都"是新出现的"，全记下来没有意义
        }
        boolean changed = false;

        for (Map.Entry<Integer, ProcessEntry> e : curIndex.entrySet()) {
            if (!prevIndex.containsKey(e.getKey())) {
                push(new ProcChurn(true, e.getKey(), displayName(e.getValue())));
                changed = true;
            }
        }
        for (Map.Entry<Integer, ProcessEntry> e : prevIndex.entrySet()) {
            if (!curIndex.containsKey(e.getKey())) {
                push(new ProcChurn(false, e.getKey(), displayName(e.getValue())));
                changed = true;
            }
        }
        if (changed) {
            while (churn.size() > churnLimit) {
                churn.removeLast();
            }
        }
    }

    private void push(ProcChurn event) {
        churn.addFirst(event);
    }

    /**
     * 展示名，带本帧缓存：优先 {@code cmdline} 全名（见 {@link ProcReader#displayName}），
     * 查过就记住，好让同一个 pid 的 {@code +} / {@code -} 两条流水显示成同一个名字。
     */
    private String displayName(ProcessEntry entry) {
        if (entry == null) {
            return "?";
        }
        return displayName(entry.pid, entry.name != null ? entry.name : "?");
    }

    private String displayName(int pid, String comm) {
        String cached = displayNames.get(pid);
        if (cached != null) {
            return cached;
        }
        String resolved = ProcReader.displayName(pid, comm);
        displayNames.put(pid, resolved);
        return resolved;
    }

    // =========================================================================
    // 工具
    // =========================================================================

    /** 把一帧的进程列表变成 pid→进程 索引。 */
    private static Map<Integer, ProcessEntry> index(ProcSnapshot snapshot) {
        if (snapshot.processes == null || snapshot.processes.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<Integer, ProcessEntry> map = new HashMap<>(snapshot.processes.size() * 2);
        for (ProcessEntry entry : snapshot.processes) {
            map.put(entry.pid, entry);
        }
        return map;
    }

    private static double pct(long part, long whole) {
        return whole > 0 ? 100.0 * part / whole : 0;
    }
}
