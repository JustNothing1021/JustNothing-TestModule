package com.justnothing.testmodule.command.functions.jank.model;

/**
 * 一帧里某一个进程的关键信息，全部来自 {@code /proc/<pid>/stat} 一次读取。
 *
 * <p>只留了判卡顿用得上的四个字段，没有把整个 stat 都解析出来：这个对象每帧要为
 * 四百多个进程各建一个，字段多一个都是几百次多余解析。</p>
 */
public class ProcessEntry {

    /** {@code /proc/<pid>/stat} 字段 1。 */
    public int pid;

    /** 字段 2 的 comm，最多 15 个字符，可能带括号里的空格。 */
    public String name;

    /** 字段 3：R 运行 / D 不可中断睡眠 / S 睡眠 / Z 僵尸 / T 停止。 */
    public char state;

    /** 字段 14 + 15（utime + stime），单位 jiffies。 */
    public long cpu;

    /** 字段 20：这个进程的线程数。 */
    public int threads;

    @Override
    public String toString() {
        return pid + "(" + name + ") " + state + " cpu=" + cpu + " thr=" + threads;
    }
}
