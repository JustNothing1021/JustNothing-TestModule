package com.justnothing.testmodule.command.functions.jank;

import com.justnothing.testmodule.command.framework.i18n.CliTexts;

import java.util.Map;

/**
 * jank 命令族的 CLI 文案（id 常量 + 中英对照）。
 *
 * <p>命名与 id 规范见 {@link CliTexts}。
 * 本类由 {@code CliTexts} 的静态块登记，新增条目只需在这里加常量 + 两行 put。</p>
 *
 * <p>英文允许缺失（只 put 中文），缺失时英文环境回落显示中文 —— 所以翻译可以一条一条补。</p>
 */
public final class JankTexts {

    // ==================== @Cmd（主命令）====================
    public static final String CMD_JANK_DESC = "cmd.jank.desc";

    // ==================== @CmdRoutes.Route（命令列表里那一行）====================
    public static final String ROUTE_JANK_SAMPLE_DESC = "route.jank.sample.desc";
    public static final String ROUTE_JANK_WATCH_DESC = "route.jank.watch.desc";

    // ==================== @SubCommandInfo（帮助正文）====================
    public static final String SUB_JANK_SAMPLE_DESC = "sub.jank.sample.desc";
    public static final String SUB_JANK_SAMPLE_USAGE = "sub.jank.sample.usage";
    public static final String SUB_JANK_SAMPLE_OPTIONS = "sub.jank.sample.options";
    public static final String SUB_JANK_WATCH_DESC = "sub.jank.watch.desc";
    public static final String SUB_JANK_WATCH_USAGE = "sub.jank.watch.usage";
    public static final String SUB_JANK_WATCH_OPTIONS = "sub.jank.watch.options";

    // ==================== @CmdParam（参数说明）====================
    public static final String PARAM_JANK_SAMPLE_DURATION_DESC = "param.jank.sample.duration.desc";
    public static final String PARAM_JANK_SAMPLE_INTERVAL_DESC = "param.jank.sample.interval.desc";
    public static final String PARAM_JANK_SAMPLE_TOP_DESC = "param.jank.sample.top.desc";
    public static final String PARAM_JANK_WATCH_INTERVAL_DESC = "param.jank.watch.interval.desc";

    private JankTexts() {
    }

    /**
     * 由 {@code CliTexts} 的静态块调用。必须是 public —— 它在另一个包里。
     * 命名上刻意带 register 而不是「构造时自己注册」：登记动作集中在 CliTexts 一处，
     * 「哪些族登记了」才看得全，漏登记也能被守卫测试发现。
     */
    public static void register(Map<String, String> zh, Map<String, String> en) {
        zh.put(CMD_JANK_DESC, "检测系统卡顿（连续采样 CPU/内存/队列/进程变动）");
        en.put(CMD_JANK_DESC, "Detect system jank by sampling CPU, memory, run queue and process churn");

        zh.put(ROUTE_JANK_SAMPLE_DESC, "采样一段时间，实时显示卡顿指标");
        en.put(ROUTE_JANK_SAMPLE_DESC, "Sample for a while and show jank metrics live");

        zh.put(ROUTE_JANK_WATCH_DESC, "监视所有进程的出现与消失（差分流，长跑）");
        en.put(ROUTE_JANK_WATCH_DESC, "Watch processes appearing and disappearing (diff stream, long-running)");

        zh.put(SUB_JANK_SAMPLE_DESC, "采样一段时间，实时显示 CPU/内存/队列/进程变动等卡顿指标");
        en.put(SUB_JANK_SAMPLE_DESC, "Sample for a while and show jank metrics live: CPU, memory, run queue, process churn");

        zh.put(SUB_JANK_SAMPLE_USAGE, "jank sample [秒数] [-i 间隔毫秒] [-n TOP 行数]");
        en.put(SUB_JANK_SAMPLE_USAGE, "jank sample [seconds] [-i interval_ms] [-n top_rows]");

        // optionsDesc 是多行文本块，整块一个 id（含缩进，逐字节照抄原注解里的内容）。
        // 原注解是字符串拼接、末尾没有换行，所以这里 stripTrailing() 掉文本块自带的行尾换行。
        zh.put(SUB_JANK_SAMPLE_OPTIONS, """
                  秒数        采样时长，默认 20 秒（位置参数，可省略）
                  -i, --interval  采样间隔，默认 1000ms；低于 500ms 时采集本身会明显干扰结果
                  -n, --top       CPU TOP 榜单长度，默认 5
                """.stripTrailing());
        en.put(SUB_JANK_SAMPLE_OPTIONS, """
                  seconds         Sampling duration, 20s by default (positional, optional)
                  -i, --interval  Sampling interval, 1000ms by default; below 500ms the sampler itself clearly perturbs the result
                  -n, --top       Number of rows in the CPU TOP list, 5 by default
                """.stripTrailing());

        zh.put(SUB_JANK_WATCH_DESC, "监视所有进程的出现与消失（差分流，参照 Android Studio 的 process-tracker）");
        en.put(SUB_JANK_WATCH_DESC, "Watch processes appearing and disappearing (diff stream, like Android Studio's process-tracker)");

        zh.put(SUB_JANK_WATCH_USAGE, "jank watch [-i 间隔毫秒]");
        en.put(SUB_JANK_WATCH_USAGE, "jank watch [-i interval_ms]");

        // 同上：原注解是多行字符串拼接，末尾没有换行
        zh.put(SUB_JANK_WATCH_OPTIONS, """
                  -i, --interval  轮询间隔，默认 1000ms
                  结束            在客户端按 Ctrl-C（或断开连接），下一轮自动退出
                """.stripTrailing());
        en.put(SUB_JANK_WATCH_OPTIONS, """
                  -i, --interval  Polling interval, 1000ms by default
                  Exit            Press Ctrl-C on the client (or disconnect), and it quits on the next round
                """.stripTrailing());

        zh.put(PARAM_JANK_SAMPLE_DURATION_DESC, "采样多少秒");
        en.put(PARAM_JANK_SAMPLE_DURATION_DESC, "How many seconds to sample");

        zh.put(PARAM_JANK_SAMPLE_INTERVAL_DESC, "采样间隔（毫秒）；低于 500ms 时采集开销会明显干扰结果");
        en.put(PARAM_JANK_SAMPLE_INTERVAL_DESC, "Sampling interval in ms; below 500ms the sampling overhead clearly perturbs the result");

        zh.put(PARAM_JANK_SAMPLE_TOP_DESC, "CPU TOP 榜单长度");
        en.put(PARAM_JANK_SAMPLE_TOP_DESC, "Number of rows in the CPU TOP list");

        zh.put(PARAM_JANK_WATCH_INTERVAL_DESC, "轮询间隔（毫秒）；每轮只做一次 readdir，压到 100ms 也不会明显干扰系统");
        en.put(PARAM_JANK_WATCH_INTERVAL_DESC, "Polling interval in ms; each round is a single readdir, so even 100ms barely disturbs the system");
    }
}
