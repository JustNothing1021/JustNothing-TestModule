package com.justnothing.testmodule.command.functions.jank.model;

import java.util.List;

/**
 * 一帧的原始读数：只负责"读到了什么"，不做任何跨帧计算。
 *
 * <p>每个来源都可能读不到（权限、内核没编、文件不存在），所以每块都配一个 {@code *Ok} 标记。
 * 界面上要能区分"真的是 0"和"压根没读到" —— 这两者混在一起会让人得出完全相反的结论。</p>
 */
public class ProcSnapshot {

    /** {@code /proc/stat} 里的一行 cpu 统计（总体那行或某个核）。 */
    public static class Cpu {
        /** {@code cpu} 或 {@code cpu0}… */
        public String name;
        public long user;
        public long nice;
        public long system;
        public long idle;
        public long iowait;
        public long irq;
        public long softirq;

        /** 所有字段之和，单位 jiffies。 */
        public long total;

        /** {@code total - idle - iowait}，即真正在干活的时间。 */
        public long busy;

        public long userAll() {
            return user + nice;
        }

        /** 内核态：system + 硬中断 + 软中断。 */
        public long systemAll() {
            return system + irq + softirq;
        }
    }

    /** 索引 0 是总体（{@code cpu} 行），其后按 {@code cpu0}… 顺序；null 表示读不到。 */
    public Cpu[] cpus;

    public boolean memOk;
    public long memTotalKb;
    public long memAvailableKb;
    public long swapTotalKb;
    public long swapFreeKb;

    public boolean loadOk;
    public double load1;
    public double load5;
    public double load15;

    /** {@code /proc/loadavg} 第 4 字段斜杠左边：正在跑（含 D 状态）的内核调度实体数。 */
    public int runnable;

    /** 同上的斜杠右边：系统里所有线程数，交叉验证 {@link #threadCount} 用。 */
    public int schedulerEntities;

    /** 本帧能看到、并能读通 stat 的进程。 */
    public List<ProcessEntry> processes;

    /** {@code /proc} 下数字目录的个数 —— 也就是"这台机器上我们看得见多少进程"。 */
    public int pidDirCount;

    /** {@code processes} 里读通了 {@code stat} 的个数。 */
    public int statOkCount;

    /** 所有进程的线程数之和。 */
    public int threadCount;

    /** 处于 D（不可中断睡眠，通常在等 IO）状态的进程数。 */
    public int dStateCount;

    /** 抓这一帧花了多少纳秒，直接展示给用户：这个数字本身就能说明采样有没有干扰观察。 */
    public long costNanos;

    /** 进程数是否读到了（{@code /proc} 目录列表拿没拿到）。 */
    public boolean processesOk;
}
