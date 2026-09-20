package com.justnothing.testmodule.command.functions.jank.util;

import com.justnothing.testmodule.command.functions.jank.model.ProcSnapshot;
import com.justnothing.testmodule.command.functions.jank.model.ProcessEntry;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 读 {@code /proc} 的一层薄封装，不缓存任何东西。
 *
 * <p>这个命令本身就是在观察系统，缓存会让它观察到的东西失真 —— 每次要数据都重新读一遍。</p>
 *
 * <h3>关于看得见多少进程</h3>
 * <p>本机的 {@code /proc} 是 {@code rw,relatime,gid=3009,hidepid=2} 挂载的。{@code hidepid=2}
 * 会让进程只能看到<b>自己这个 uid</b> 的进程，而 {@code gid=3009}（AID_READPROC）的成员是例外。
 * system_server 的组里就带着 readproc（{@code service system_server … group system readproc}），
 * 所以走 sinteractive 能看到全部四百多个进程；而 {@code agent run} 注入普通应用时，
 * 对方的组里没有 readproc，看到的进程数会掉到个位数。</p>
 *
 * <p>这种情况下拿到的数据不会报错，只是"看起来特别干净"—— 所以
 * {@link ProcSnapshot#pidDirCount} 一定要展示出来：它是"隐身"和"真的没进程"的唯一区分依据。</p>
 */
public final class ProcReader {

    private static final File PROC = new File("/proc");

    /** {@code /proc/<pid>/stat} 的第一行只有三百多字节，读这么多足够，省掉整份读的开销。 */
    private static final int FIRST_LINE_LIMIT = 1024;

    private ProcReader() {
    }

    /**
     * 抓一帧。
     *
     * <p>顺序是有讲究的：{@code /proc/stat} 读得越早，算出来的 CPU 占比越贴近"调用方以为的
     * 那个时刻"。进程表最慢（四百多次文件读），放最后，它花的时间只会摊到帧尾，
     * 不会把 CPU 采样的时间点往后拖。</p>
     */
    public static ProcSnapshot snapshot() {
        long start = System.nanoTime();
        ProcSnapshot snapshot = new ProcSnapshot();
        readCpu(snapshot);
        readMem(snapshot);
        readLoad(snapshot);
        readProcesses(snapshot);
        snapshot.costNanos = System.nanoTime() - start;
        return snapshot;
    }

    /**
     * 当前 {@code /proc} 下的所有 pid：只做一次 readdir，一个文件都不读。
     *
     * <p>{@code jank watch} 每轮只需要知道"进程表变没变"。为这点信息去读四百多份
     * {@code /proc/<pid>/stat} 是纯浪费：监视器每轮的开销越小，它给被观察对象带来的扰动就越小，
     * 长时间挂着才有可能 —— 这也正是它和 {@link #snapshot()} 分开的原因。</p>
     *
     * <p>读不到（{@code /proc} 不可访问）返回空表：上层会把它当成"这一轮没有进程"，
     * 于是所有进程都被报成消失 —— 所以调用方要配合 {@link ProcSnapshot#pidDirCount}
     * 那套"看得见多少"的判断来看。</p>
     */
    public static List<Integer> pids() {
        String[] names = PROC.list();
        if (names == null) {
            return Collections.emptyList();
        }
        List<Integer> pids = new ArrayList<>(names.length);
        for (String name : names) {
            int pid = parsePid(name);
            if (pid >= 0) {
                pids.add(pid);
            }
        }
        return pids;
    }

    // =========================================================================
    // /proc/stat
    // =========================================================================

    /**
     * 读 {@code /proc/stat} 开头的 cpu 行。
     *
     * <p>字段顺序固定为 user nice system idle iowait irq softirq steal guest guest_nice
     * （本机是 4.9 内核，实测每行 10 个字段）。只解析前 8 个：guest 那两列是
     * user/nice 的<b>子集</b>，加进 total 会重复计数。</p>
     */
    private static void readCpu(ProcSnapshot snapshot) {
        List<ProcSnapshot.Cpu> rows = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader("/proc/stat"), 512)) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.startsWith("cpu")) {
                    break; // cpu 行是连续的，遇到 intr/ctxt 就可以收手了
                }
                ProcSnapshot.Cpu cpu = parseCpuRow(line);
                if (cpu != null) {
                    rows.add(cpu);
                }
            }
        } catch (IOException e) {
            return; // 读不到就保持 cpus == null，上层按"不可用"处理
        }
        if (!rows.isEmpty()) {
            snapshot.cpus = rows.toArray(new ProcSnapshot.Cpu[0]);
        }
    }

    private static ProcSnapshot.Cpu parseCpuRow(String line) {
        String[] parts = line.trim().split("\\s+");
        if (parts.length < 6) {
            return null;
        }
        ProcSnapshot.Cpu cpu = new ProcSnapshot.Cpu();
        cpu.name = parts[0];
        cpu.user = parseLong(parts, 1);
        cpu.nice = parseLong(parts, 2);
        cpu.system = parseLong(parts, 3);
        cpu.idle = parseLong(parts, 4);
        cpu.iowait = parseLong(parts, 5);
        cpu.irq = parseLong(parts, 6);
        cpu.softirq = parseLong(parts, 7);

        long total = 0;
        for (int i = 1; i < parts.length; i++) {
            total += parseLong(parts, i);
        }
        cpu.total = total;
        cpu.busy = total - cpu.idle - cpu.iowait;
        return cpu;
    }

    // =========================================================================
    // /proc/meminfo
    // =========================================================================

    /**
     * 只挑要用的行。
     *
     * <p>用 {@code MemAvailable} 而不是 {@code MemFree}：Android 上 Free 常年在 20MB 上下，
     * 看着像要爆了，其实大部分是可回收的 page cache。{@code MemAvailable} 是内核 3.14+
     * 给出的"还能拿出多少"的估算，本机 4.9 有。</p>
     */
    private static void readMem(ProcSnapshot snapshot) {
        boolean sawTotal = false;
        boolean sawAvailable = false;
        try (BufferedReader reader = new BufferedReader(new FileReader("/proc/meminfo"), 2048)) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("MemTotal:")) {
                    snapshot.memTotalKb = parseKb(line);
                    sawTotal = true;
                } else if (line.startsWith("MemAvailable:")) {
                    snapshot.memAvailableKb = parseKb(line);
                    sawAvailable = true;
                } else if (line.startsWith("SwapTotal:")) {
                    snapshot.swapTotalKb = parseKb(line);
                } else if (line.startsWith("SwapFree:")) {
                    snapshot.swapFreeKb = parseKb(line);
                }
            }
        } catch (IOException e) {
            return;
        }
        snapshot.memOk = sawTotal && sawAvailable;
    }

    /** 形如 {@code MemTotal:  894168 kB} → 894168。 */
    private static long parseKb(String line) {
        String[] parts = line.trim().split("\\s+");
        return parts.length >= 2 ? parseLong(parts, 1) : 0;
    }

    // =========================================================================
    // /proc/loadavg
    // =========================================================================

    /**
     * 形如 {@code 1.54 1.65 1.80 1/1655 8596}。
     *
     * <p>第 4 字段是最有价值的一个数：斜杠左边是<b>正在跑或不可中断</b>的调度实体数，
     * 右边是系统里所有线程数。左边远大于核数就说明 CPU 在排队 —— 这正是"卡顿"的定义本身，
     * 而且它不需要 PSI（内核 4.20+）就能拿到。</p>
     */
    private static void readLoad(ProcSnapshot snapshot) {
        String line = readFirstLine(new File("/proc/loadavg"));
        if (line == null) {
            return;
        }
        String[] parts = line.trim().split("\\s+");
        if (parts.length < 4) {
            return;
        }
        try {
            snapshot.load1 = Double.parseDouble(parts[0]);
            snapshot.load5 = Double.parseDouble(parts[1]);
            snapshot.load15 = Double.parseDouble(parts[2]);
        } catch (NumberFormatException e) {
            return;
        }
        int slash = parts[3].indexOf('/');
        if (slash > 0) {
            snapshot.runnable = (int) parseLong(parts[3].substring(0, slash));
            snapshot.schedulerEntities = (int) parseLong(parts[3].substring(slash + 1));
        }
        snapshot.loadOk = true;
    }

    // =========================================================================
    // /proc/<pid>
    // =========================================================================

    /**
     * 扫一遍进程表。
     *
     * <p>一次 pass 同时拿到三样东西：每个进程的 CPU jiffies（差分算占用）、状态
     * （数 D 状态）、线程数（求和）。分开扫三遍在手表上就是三倍代价。</p>
     */
    private static void readProcesses(ProcSnapshot snapshot) {
        String[] names = PROC.list();
        if (names == null) {
            return;
        }
        List<ProcessEntry> processes = new ArrayList<>(names.length);
        int pidDirs = 0;
        int statOk = 0;
        int threads = 0;
        int dState = 0;

        for (String name : names) {
            int pid = parsePid(name);
            if (pid < 0) {
                continue;
            }
            pidDirs++;
            ProcessEntry entry = readProcessStat(pid);
            if (entry == null) {
                continue; // 目录看得见但 stat 读不到：进程刚死，或者权限不够
            }
            statOk++;
            threads += entry.threads;
            if (entry.state == 'D') {
                dState++;
            }
            processes.add(entry);
        }

        snapshot.processes = processes;
        snapshot.pidDirCount = pidDirs;
        snapshot.statOkCount = statOk;
        snapshot.threadCount = threads;
        snapshot.dStateCount = dState;
        snapshot.processesOk = true;
    }

    /**
     * 读单个 {@code /proc/<pid>/stat}。
     *
     * <p>{@code comm} 里可能有空格和括号（{@code (Binder:1234_5)}、{@code (tmux: server)}），
     * 按空格 split 一定会把字段整体错位。标准做法是取<b>第一个 '{@code (}' 到最后一个
     * '{@code )}'</b> 之间的内容当 comm，剩下的从 {@code ')'} 后面开始按空格切 ——
     * 切出来的第 0 个就是 state（字段 3）。</p>
     *
     * @return 读不到返回 null（进程已退出是最常见的原因，不是错误）
     */
    public static ProcessEntry readProcessStat(int pid) {
        String line = readFirstLine(new File("/proc/" + pid + "/stat"));
        if (line == null) {
            return null;
        }
        int open = line.indexOf('(');
        int close = line.lastIndexOf(')');
        if (open < 0 || close <= open) {
            return null;
        }
        String[] rest = line.substring(close + 1).trim().split("\\s+");
        // state 之后还需要 utime/stime/num_threads，即字段 20 之前一个都不能少
        if (rest.length < 18) {
            return null;
        }
        ProcessEntry entry = new ProcessEntry();
        entry.pid = pid;
        entry.name = line.substring(open + 1, close);
        entry.state = rest[0].charAt(0);
        entry.cpu = parseLong(rest, 11) + parseLong(rest, 12);
        entry.threads = (int) parseLong(rest, 17);
        return entry;
    }

    // =========================================================================
    // 小工具
    // =========================================================================

    /**
     * 进程的展示名：优先 {@code /proc/<pid>/cmdline} 的 argv[0]，取不到才退回内核 comm。
     *
     * <p>{@code comm} 受内核 {@code TASK_COMM_LEN}(16) 限制，最多 15 个字符 ——
     * {@code android.hardware.wifi@1.0-service} 在面板上只剩 {@code android.hardwar}。
     * cmdline 存的是真正的 argv[0]：应用是完整进程名（{@code com.xtc.i3launcher:wallpaper}），
     * 原生服务是可执行文件路径，取 basename 正好是服务名 —— Android Studio 推上来的
     * {@code process-tracker} 就是这么显示的。内核线程不是 exec 出来的、根本没有 cmdline，
     * 于是自然回退到 comm（{@code kworker/u8:8} 这种本来就该那么短）。</p>
     *
     * <p>只在真要显示这个名字的时候调用：多读一个 procfs 文件本身不贵，但整表四百多个进程
     * 每个都多读一次，就会让"采样耗时"明显变长 —— 而那个数字是给用户看采样器清不清白的。</p>
     */
    public static String displayName(int pid, String comm) {
        String argv0 = readFirstLine(new File("/proc/" + pid + "/cmdline"));
        if (argv0 == null || argv0.isEmpty()) {
            return comm;
        }
        int slash = argv0.lastIndexOf('/');
        String base = slash >= 0 ? argv0.substring(slash + 1) : argv0;
        return base.isEmpty() ? comm : base;
    }

    /**
     * 读一个 procfs 文件的第一段。读不到（不存在 / 权限不够 / 目录）返回 null。
     *
     * <p>同时以 {@code '\n'} 和 {@code '\0'} 为界：{@code /proc/<pid>/cmdline} 的参数是
     * NUL 分隔的，只认换行会把 argv[1..] 一起读进来。</p>
     */
    private static String readFirstLine(File file) {
        try (InputStream in = new FileInputStream(file)) {
            byte[] buffer = new byte[FIRST_LINE_LIMIT];
            int read = in.read(buffer);
            if (read <= 0) {
                return null;
            }
            int end = 0;
            while (end < read && buffer[end] != '\n' && buffer[end] != '\0') {
                end++;
            }
            return new String(buffer, 0, end, StandardCharsets.UTF_8);
        } catch (IOException e) {
            return null;
        }
    }

    /** {@code "/proc"} 下的条目名 → pid；不是纯数字返回 -1。 */
    private static int parsePid(String name) {
        if (name == null || name.isEmpty() || name.length() > 7) {
            return -1;
        }
        int value = 0;
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            if (c < '0' || c > '9') {
                return -1;
            }
            value = value * 10 + (c - '0');
        }
        return value;
    }

    private static long parseLong(String[] parts, int index) {
        if (index >= parts.length) {
            return 0;
        }
        return parseLong(parts[index]);
    }

    private static long parseLong(String text) {
        try {
            return Long.parseLong(text);
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
