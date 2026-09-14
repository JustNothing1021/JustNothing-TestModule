package com.justnothing.testmodule.command.functions.jank.model;

import java.util.Collections;
import java.util.List;

/**
 * 已经算好的一帧：界面直接照着它画，不再做任何计算。
 *
 * <p>所有比率都是"这一帧相对上一帧"的增量算出来的，不是开机以来的平均值 ——
 * 开机以来累计的 13 万 jiffies 会把任何瞬时抖动都抹平（实测总占比 9.8%，
 * 而那一瞬间可能有整整一秒是 100%）。</p>
 */
public class JankFrame {

    /** 某个进程这一帧的 CPU 占用（相对单核的百分比，和 top 的口径一致）。 */
    public static class TopEntry {
        public final int pid;
        public final String name;
        public final double pct;

        public TopEntry(int pid, String name, double pct) {
            this.pid = pid;
            this.name = name;
            this.pct = pct;
        }
    }

    /** 第几帧，从 1 开始；0 表示只用来建立基线的第一帧。 */
    public int tick;

    /** 总共要采多少帧，用来画进度条。 */
    public int totalTicks;

    // ---- CPU ----
    public boolean cpuOk;
    /** 总体占用百分比。 */
    public double busyPct;
    /** 各核占用百分比，长度与 CPU 核数一致。 */
    public double[] corePct;
    /** 总体时间分布，四项之和约等于 100。 */
    public double userPct;
    public double systemPct;
    public double iowaitPct;
    public double idlePct;

    // ---- 内存 ----
    public boolean memOk;
    public double memUsedPct;
    public long memUsedKb;
    public long memTotalKb;
    public boolean swapOk;
    public double swapUsedPct;
    public long swapUsedKb;
    public long swapTotalKb;

    // ---- 负载 ----
    public boolean loadOk;
    public double load1;
    public double load5;
    public double load15;
    /** 正在跑 / 不可中断睡眠的调度实体数，除以核数就是"每核排了几个"。 */
    public int runnable;
    /** 系统里的线程数（loadavg 口径），用来交叉验证 {@link #threadCount}。 */
    public int schedulerEntities;

    // ---- 进程 ----
    public boolean procOk;
    /** {@code /proc} 下看得见的数字目录数。这个数字太小就说明被 hidepid 挡住了，别的都是假象。 */
    public int pidDirCount;
    /** 其中 stat 读成功的个数。 */
    public int statOkCount;
    /** 所有进程线程数之和。 */
    public int threadCount;
    /** D 状态（在等 IO）的进程数。 */
    public int dStateCount;
    /** 上面那些 D 状态进程里的前几个，直接指出"谁在卡 IO"。 */
    public List<ProcessEntry> dStateTop = Collections.emptyList();
    /** 按本帧 CPU 增量排序的前几名。 */
    public List<TopEntry> topCpu = Collections.emptyList();
    /** 本帧所有进程 CPU 增量之和，拿它和 {@code /proc/stat} 的增量对比能看出有没有漏掉进程。 */
    public long procCpuDelta;

    /** 进程流水，最新的在前。 */
    public List<ProcChurn> churn = Collections.emptyList();

    /** 抓这一帧的耗时（毫秒）。这个数本身就该展示 —— 采样器自己太贵的话，测出来的卡顿是它造成的。 */
    public long costMs;

    /** 只建立了基线、还没有差分可用。 */
    public boolean baseline;

    /** 每核平均排了几个可运行的调度实体。 */
    public double runQueuePerCore(int cores) {
        return cores > 0 ? (double) runnable / cores : runnable;
    }
}
