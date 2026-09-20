package com.justnothing.testmodule.command.functions.threads;

import java.util.Map;

/**
 * threads 命令族的 CLI 文案（id 常量 + 中英对照）。
 *
 * <p>命名与 id 规范见 {@link com.justnothing.testmodule.command.framework.i18n.CliTexts}。
 * 本类由 {@code CliTexts} 的静态块登记，新增条目只需在这里加常量 + 两行 put。</p>
 *
 * <p>英文允许缺失（只 put 中文），缺失时英文环境回落显示中文 —— 所以翻译可以一条一条补。</p>
 */
public final class ThreadsTexts {

    // ==================== @Cmd（主命令）====================
    public static final String CMD_THREADS_DESC = "cmd.threads.desc";

    // ==================== @CmdRoutes.Route（命令列表里那一行）====================
    public static final String ROUTE_THREADS_LIST_DESC = "route.threads.list.desc";
    public static final String ROUTE_THREADS_DEADLOCK_DESC = "route.threads.deadlock.desc";
    public static final String ROUTE_THREADS_PROFILE_START_DESC = "route.threads.profile.start.desc";
    public static final String ROUTE_THREADS_PROFILE_STOP_DESC = "route.threads.profile.stop.desc";
    public static final String ROUTE_THREADS_PROFILE_SHOW_DESC = "route.threads.profile.show.desc";
    public static final String ROUTE_THREADS_PROFILE_EXPORT_DESC = "route.threads.profile.export.desc";

    // ==================== @SubCommandInfo（帮助正文）====================
    public static final String SUB_THREADS_LIST_DESC = "sub.threads.list.desc";
    public static final String SUB_THREADS_DEADLOCK_DESC = "sub.threads.deadlock.desc";
    public static final String SUB_THREADS_PROFILE_START_DESC = "sub.threads.profile.start.desc";
    public static final String SUB_THREADS_PROFILE_STOP_DESC = "sub.threads.profile.stop.desc";
    public static final String SUB_THREADS_PROFILE_SHOW_DESC = "sub.threads.profile.show.desc";
    public static final String SUB_THREADS_PROFILE_EXPORT_DESC = "sub.threads.profile.export.desc";

    // ==================== @CmdParam（参数说明）====================
    public static final String PARAM_THREADS_LIST_ID_DESC = "param.threads.list.id.desc";
    public static final String PARAM_THREADS_LIST_NAME_DESC = "param.threads.list.name.desc";
    public static final String PARAM_THREADS_LIST_STATE_DESC = "param.threads.list.state.desc";
    public static final String PARAM_THREADS_LIST_FILTER_ID_DESC = "param.threads.list.filter-id.desc";
    public static final String PARAM_THREADS_LIST_FILTER_NAME_DESC = "param.threads.list.filter-name.desc";
    public static final String PARAM_THREADS_LIST_FILTER_STATE_DESC = "param.threads.list.filter-state.desc";
    public static final String PARAM_THREADS_LIST_DETAIL_LEVEL_DESC = "param.threads.list.detail-level.desc";

    public static final String PARAM_THREADS_PROFILE_START_DURATION_DESC = "param.threads.profile.start.duration.desc";
    public static final String PARAM_THREADS_PROFILE_START_TARGET_THREADS_DESC = "param.threads.profile.start.target-threads.desc";

    public static final String PARAM_THREADS_PROFILE_EXPORT_FILE_DESC = "param.threads.profile.export.file.desc";

    private ThreadsTexts() {
    }

    /**
     * 由 {@code CliTexts} 的静态块调用。必须是 public —— 它在另一个包里。
     * 命名上刻意带 register 而不是「构造时自己注册」：登记动作集中在 CliTexts 一处，
     * 「哪些族登记了」才看得全，漏登记也能被守卫测试发现。
     */
    public static void register(Map<String, String> zh, Map<String, String> en) {
        zh.put(CMD_THREADS_DESC, "线程管理和分析工具");
        en.put(CMD_THREADS_DESC, "Thread management and analysis tools");

        zh.put(ROUTE_THREADS_LIST_DESC, "列出所有线程及其状态");
        en.put(ROUTE_THREADS_LIST_DESC, "List all threads and their states");

        zh.put(ROUTE_THREADS_DEADLOCK_DESC, "检测Java应用程序中的死锁");
        en.put(ROUTE_THREADS_DEADLOCK_DESC, "Detect deadlocks in the Java application");

        zh.put(ROUTE_THREADS_PROFILE_START_DESC, "开始性能分析");
        en.put(ROUTE_THREADS_PROFILE_START_DESC, "Start profiling");

        zh.put(ROUTE_THREADS_PROFILE_STOP_DESC, "停止当前分析");
        en.put(ROUTE_THREADS_PROFILE_STOP_DESC, "Stop the current profiling session");

        zh.put(ROUTE_THREADS_PROFILE_SHOW_DESC, "显示分析结果");
        en.put(ROUTE_THREADS_PROFILE_SHOW_DESC, "Show the profiling results");

        zh.put(ROUTE_THREADS_PROFILE_EXPORT_DESC, "导出分析结果到文件");
        en.put(ROUTE_THREADS_PROFILE_EXPORT_DESC, "Export the profiling results to a file");

        zh.put(SUB_THREADS_LIST_DESC, "列出所有线程及其状态");
        en.put(SUB_THREADS_LIST_DESC, "List all threads and their states");

        zh.put(SUB_THREADS_DEADLOCK_DESC, "检测Java应用程序中的死锁");
        en.put(SUB_THREADS_DEADLOCK_DESC, "Detect deadlocks in the Java application");

        zh.put(SUB_THREADS_PROFILE_START_DESC, "开始性能分析");
        en.put(SUB_THREADS_PROFILE_START_DESC, "Start profiling");

        zh.put(SUB_THREADS_PROFILE_STOP_DESC, "停止当前的性能分析");
        en.put(SUB_THREADS_PROFILE_STOP_DESC, "Stop the current profiling session");

        zh.put(SUB_THREADS_PROFILE_SHOW_DESC, "显示性能分析结果");
        en.put(SUB_THREADS_PROFILE_SHOW_DESC, "Show the profiling results");

        zh.put(SUB_THREADS_PROFILE_EXPORT_DESC, "导出性能分析结果到文件");
        en.put(SUB_THREADS_PROFILE_EXPORT_DESC, "Export the profiling results to a file");

        zh.put(PARAM_THREADS_LIST_ID_DESC, "只显示指定ID的线程");
        en.put(PARAM_THREADS_LIST_ID_DESC, "Show only the thread with the given ID");

        zh.put(PARAM_THREADS_LIST_NAME_DESC, "只显示指定名称的线程");
        en.put(PARAM_THREADS_LIST_NAME_DESC, "Show only threads with the given name");

        zh.put(PARAM_THREADS_LIST_STATE_DESC, "只显示指定状态的线程");
        en.put(PARAM_THREADS_LIST_STATE_DESC, "Show only threads in the given state");

        zh.put(PARAM_THREADS_LIST_FILTER_ID_DESC, "按线程ID过滤");
        en.put(PARAM_THREADS_LIST_FILTER_ID_DESC, "Filter by thread ID");

        zh.put(PARAM_THREADS_LIST_FILTER_NAME_DESC, "按线程名称过滤");
        en.put(PARAM_THREADS_LIST_FILTER_NAME_DESC, "Filter by thread name");

        zh.put(PARAM_THREADS_LIST_FILTER_STATE_DESC, "按线程状态过滤");
        en.put(PARAM_THREADS_LIST_FILTER_STATE_DESC, "Filter by thread state");

        zh.put(PARAM_THREADS_LIST_DETAIL_LEVEL_DESC, "明细级别: basic=不含堆栈, full=含堆栈");
        en.put(PARAM_THREADS_LIST_DETAIL_LEVEL_DESC, "Detail level: basic = no stack trace, full = with stack trace");

        zh.put(PARAM_THREADS_PROFILE_START_DURATION_DESC, "分析时长(秒)");
        en.put(PARAM_THREADS_PROFILE_START_DURATION_DESC, "Profiling duration in seconds");

        zh.put(PARAM_THREADS_PROFILE_START_TARGET_THREADS_DESC, "目标线程ID列表");
        en.put(PARAM_THREADS_PROFILE_START_TARGET_THREADS_DESC, "List of target thread IDs");

        zh.put(PARAM_THREADS_PROFILE_EXPORT_FILE_DESC, "导出文件路径");
        en.put(PARAM_THREADS_PROFILE_EXPORT_FILE_DESC, "Path of the exported file");
    }
}
