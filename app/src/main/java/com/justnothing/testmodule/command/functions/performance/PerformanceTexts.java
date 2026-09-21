package com.justnothing.testmodule.command.functions.performance;

import com.justnothing.testmodule.command.framework.i18n.CliTexts;
import com.justnothing.testmodule.command.framework.i18n.Text;

import java.util.Map;

/**
 * performance 命令族的 CLI 文案（id 常量 + 中英对照）。
 *
 * <p>命名与 id 规范见 {@link CliTexts}。
 * 本类由 {@code CliTexts} 的静态块登记，新增条目只需在这里加常量 + 两行 put。</p>
 *
 * <p>英文允许缺失（只 put 中文），缺失时英文环境回落显示中文 —— 所以翻译可以一条一条补。</p>
 */
public final class PerformanceTexts {

    // ==================== @Cmd（主命令）====================
    public static final String CMD_PERFORMANCE_DESC = "cmd.performance.desc";

    // ==================== @CmdRoutes.Route（命令列表里那一行）====================
    public static final String ROUTE_PERFORMANCE_SAMPLE_START_DESC = "route.performance.sample.start.desc";
    public static final String ROUTE_PERFORMANCE_SAMPLE_STOP_DESC = "route.performance.sample.stop.desc";
    public static final String ROUTE_PERFORMANCE_SAMPLE_REPORT_DESC = "route.performance.sample.report.desc";
    public static final String ROUTE_PERFORMANCE_SAMPLE_EXPORT_DESC = "route.performance.sample.export.desc";
    public static final String ROUTE_PERFORMANCE_MULTITHREAD_START_DESC = "route.performance.multithread.start.desc";
    public static final String ROUTE_PERFORMANCE_MULTITHREAD_STOP_DESC = "route.performance.multithread.stop.desc";
    public static final String ROUTE_PERFORMANCE_MULTITHREAD_REPORT_DESC = "route.performance.multithread.report.desc";
    public static final String ROUTE_PERFORMANCE_MULTITHREAD_EXPORT_DESC = "route.performance.multithread.export.desc";
    public static final String ROUTE_PERFORMANCE_HIERARCHICAL_START_DESC = "route.performance.hierarchical.start.desc";
    public static final String ROUTE_PERFORMANCE_HIERARCHICAL_STOP_DESC = "route.performance.hierarchical.stop.desc";
    public static final String ROUTE_PERFORMANCE_HIERARCHICAL_REPORT_DESC = "route.performance.hierarchical.report.desc";
    public static final String ROUTE_PERFORMANCE_HIERARCHICAL_EXPORT_DESC = "route.performance.hierarchical.export.desc";
    public static final String ROUTE_PERFORMANCE_TRACE_START_DESC = "route.performance.trace.start.desc";
    public static final String ROUTE_PERFORMANCE_TRACE_STOP_DESC = "route.performance.trace.stop.desc";
    public static final String ROUTE_PERFORMANCE_TRACE_REPORT_DESC = "route.performance.trace.report.desc";
    public static final String ROUTE_PERFORMANCE_TRACE_EXPORT_DESC = "route.performance.trace.export.desc";
    public static final String ROUTE_PERFORMANCE_SYSTRACE_START_DESC = "route.performance.systrace.start.desc";
    public static final String ROUTE_PERFORMANCE_SYSTRACE_STOP_DESC = "route.performance.systrace.stop.desc";
    public static final String ROUTE_PERFORMANCE_SYSTRACE_REPORT_DESC = "route.performance.systrace.report.desc";
    public static final String ROUTE_PERFORMANCE_SYSTRACE_EXPORT_DESC = "route.performance.systrace.export.desc";
    public static final String ROUTE_PERFORMANCE_HOOK_START_DESC = "route.performance.hook.start.desc";
    public static final String ROUTE_PERFORMANCE_HOOK_STOP_DESC = "route.performance.hook.stop.desc";
    public static final String ROUTE_PERFORMANCE_HOOK_REPORT_DESC = "route.performance.hook.report.desc";
    public static final String ROUTE_PERFORMANCE_HOOK_EXPORT_DESC = "route.performance.hook.export.desc";
    public static final String ROUTE_PERFORMANCE_LIST_DESC = "route.performance.list.desc";
    public static final String ROUTE_PERFORMANCE_CLEAR_DESC = "route.performance.clear.desc";

    // ==================== @SubCommandInfo（帮助正文）====================
    public static final String SUB_PERFORMANCE_SAMPLE_DESC = "sub.performance.sample.desc";
    public static final String SUB_PERFORMANCE_SAMPLE_OPTIONS = "sub.performance.sample.options";
    public static final String SUB_PERFORMANCE_MULTITHREAD_DESC = "sub.performance.multithread.desc";
    public static final String SUB_PERFORMANCE_MULTITHREAD_OPTIONS = "sub.performance.multithread.options";
    public static final String SUB_PERFORMANCE_HIERARCHICAL_DESC = "sub.performance.hierarchical.desc";
    public static final String SUB_PERFORMANCE_HIERARCHICAL_OPTIONS = "sub.performance.hierarchical.options";
    public static final String SUB_PERFORMANCE_TRACE_DESC = "sub.performance.trace.desc";
    public static final String SUB_PERFORMANCE_TRACE_OPTIONS = "sub.performance.trace.options";
    public static final String SUB_PERFORMANCE_SYSTRACE_DESC = "sub.performance.systrace.desc";
    public static final String SUB_PERFORMANCE_SYSTRACE_OPTIONS = "sub.performance.systrace.options";
    public static final String SUB_PERFORMANCE_HOOK_DESC = "sub.performance.hook.desc";
    public static final String SUB_PERFORMANCE_HOOK_OPTIONS = "sub.performance.hook.options";

    // ==================== @CmdParam（参数说明）====================
    public static final String PARAM_PERFORMANCE_SAMPLE_START_RATE_DESC = "param.performance.sample.start.rate.desc";
    public static final String PARAM_PERFORMANCE_SAMPLE_START_EXCLUDE_DESC = "param.performance.sample.start.exclude.desc";
    public static final String PARAM_PERFORMANCE_SAMPLE_STOP_ID_DESC = "param.performance.sample.stop.id.desc";
    public static final String PARAM_PERFORMANCE_SAMPLE_REPORT_ID_DESC = "param.performance.sample.report.id.desc";
    public static final String PARAM_PERFORMANCE_SAMPLE_EXPORT_ID_DESC = "param.performance.sample.export.id.desc";
    public static final String PARAM_PERFORMANCE_SAMPLE_EXPORT_FILEPATH_DESC = "param.performance.sample.export.filePath.desc";
    public static final String PARAM_PERFORMANCE_MULTITHREAD_START_RATE_DESC = "param.performance.multithread.start.rate.desc";
    public static final String PARAM_PERFORMANCE_MULTITHREAD_START_EXCLUDE_DESC = "param.performance.multithread.start.exclude.desc";
    public static final String PARAM_PERFORMANCE_MULTITHREAD_STOP_ID_DESC = "param.performance.multithread.stop.id.desc";
    public static final String PARAM_PERFORMANCE_MULTITHREAD_REPORT_ID_DESC = "param.performance.multithread.report.id.desc";
    public static final String PARAM_PERFORMANCE_MULTITHREAD_EXPORT_ID_DESC = "param.performance.multithread.export.id.desc";
    public static final String PARAM_PERFORMANCE_MULTITHREAD_EXPORT_FILEPATH_DESC = "param.performance.multithread.export.filePath.desc";
    public static final String PARAM_PERFORMANCE_HIERARCHICAL_START_RATE_DESC = "param.performance.hierarchical.start.rate.desc";
    public static final String PARAM_PERFORMANCE_HIERARCHICAL_START_EXCLUDE_DESC = "param.performance.hierarchical.start.exclude.desc";
    public static final String PARAM_PERFORMANCE_HIERARCHICAL_STOP_ID_DESC = "param.performance.hierarchical.stop.id.desc";
    public static final String PARAM_PERFORMANCE_HIERARCHICAL_REPORT_ID_DESC = "param.performance.hierarchical.report.id.desc";
    public static final String PARAM_PERFORMANCE_HIERARCHICAL_EXPORT_ID_DESC = "param.performance.hierarchical.export.id.desc";
    public static final String PARAM_PERFORMANCE_HIERARCHICAL_EXPORT_FILEPATH_DESC = "param.performance.hierarchical.export.filePath.desc";
    public static final String PARAM_PERFORMANCE_TRACE_STOP_ID_DESC = "param.performance.trace.stop.id.desc";
    public static final String PARAM_PERFORMANCE_TRACE_REPORT_ID_DESC = "param.performance.trace.report.id.desc";
    public static final String PARAM_PERFORMANCE_TRACE_EXPORT_ID_DESC = "param.performance.trace.export.id.desc";
    public static final String PARAM_PERFORMANCE_TRACE_EXPORT_FILEPATH_DESC = "param.performance.trace.export.filePath.desc";
    public static final String PARAM_PERFORMANCE_SYSTRACE_START_DURATION_DESC = "param.performance.systrace.start.duration.desc";
    public static final String PARAM_PERFORMANCE_SYSTRACE_START_CATEGORIES_DESC = "param.performance.systrace.start.categories.desc";
    public static final String PARAM_PERFORMANCE_SYSTRACE_STOP_ID_DESC = "param.performance.systrace.stop.id.desc";
    public static final String PARAM_PERFORMANCE_SYSTRACE_REPORT_ID_DESC = "param.performance.systrace.report.id.desc";
    public static final String PARAM_PERFORMANCE_SYSTRACE_EXPORT_ID_DESC = "param.performance.systrace.export.id.desc";
    public static final String PARAM_PERFORMANCE_SYSTRACE_EXPORT_FILEPATH_DESC = "param.performance.systrace.export.filePath.desc";
    public static final String PARAM_PERFORMANCE_HOOK_START_CLASSNAME_DESC = "param.performance.hook.start.className.desc";
    public static final String PARAM_PERFORMANCE_HOOK_START_METHODNAME_DESC = "param.performance.hook.start.methodName.desc";
    public static final String PARAM_PERFORMANCE_HOOK_START_SIGNATURE_DESC = "param.performance.hook.start.signature.desc";
    public static final String PARAM_PERFORMANCE_HOOK_STOP_ID_DESC = "param.performance.hook.stop.id.desc";
    public static final String PARAM_PERFORMANCE_HOOK_REPORT_ID_DESC = "param.performance.hook.report.id.desc";
    public static final String PARAM_PERFORMANCE_HOOK_EXPORT_ID_DESC = "param.performance.hook.export.id.desc";
    public static final String PARAM_PERFORMANCE_HOOK_EXPORT_FILEPATH_DESC = "param.performance.hook.export.filePath.desc";

    // ==================== 族内复用输出文案 ====================
    // 只放本族内部出现两次以上的输出文案（中英并排的 Text 常量，就地取 .text() / .format()）。
    // 跨族复用的标签（「类名: 」「无」「错误: 」…）在 CliMessages 里，这里不再各存一份；
    // 只出现一次的在调用点就地写 Text.zhEn(...)。

    // ---- 状态 / 通用提示 ----
    public static final Text UNKNOWN_REQUEST_TYPE = Text.zhEn("未知请求类型", "Unknown request type");
    public static final Text STATUS_RUNNING = Text.zhEn("运行中", "Running");
    public static final Text STATUS_STOPPED = Text.zhEn("已停止", "Stopped");
    public static final Text HINT_VIEW_TASKS = Text.zhEn("提示: 使用 'performance list' 查看当前任务",
            "Hint: run 'performance list' to list the current tasks");
    public static final Text USING_LATEST_ID = Text.zhEn("使用最新ID: ", "Using latest ID: ");
    public static final Text NO_ID_SEARCH_LATEST_SAMPLE = Text.zhEn("未指定ID，查找最新完成的采样...",
            "No ID given; looking for the latest completed sample...");

    // ---- 参数校验 ----
    public static final Text ERR_RATE_MUST_BE_POSITIVE = Text.zhEn("错误: 频率必须 > 0", "Error: rate must be greater than 0");
    public static final Text WARN_RATE_TOO_HIGH = Text.zhEn("警告: 频率过高", "Warning: rate too high");

    // ---- 查找 / 导出 ----
    public static final Text SAMPLER_STOPPED = Text.zhEn("采样器已停止", "Sampler stopped");
    public static final Text ERR_NO_COMPLETED_SAMPLE_DATA = Text.zhEn("错误: 没有已完成的采样数据",
            "Error: no completed sample data");
    public static final Text ERR_DATA_NOT_FOUND = Text.zhEn("错误: 数据不存在 (ID: %d)", "Error: data not found (ID: %d)");
    public static final Text AVAILABLE_REPORT_IDS = Text.zhEn("可用的报告ID: %s", "Available report IDs: %s");
    public static final Text AVAILABLE_IDS = Text.zhEn("可用的IDs: %s", "Available IDs: %s");
    public static final Text WARN_REPORT_DATA_EMPTY = Text.zhEn("警告: 报告数据为空 (ID: %d)", "Warning: report data is empty (ID: %d)");
    public static final Text NO_METHOD_CALLS_CAPTURED = Text.zhEn("该采样周期内没有捕获到任何方法调用",
            "No method calls were captured during this sampling window");
    public static final Text DATA_EXPORTED = Text.zhEn("数据已导出", "Data exported");
    public static final Text ERR_EXPORT_WRITE_FAILED = Text.zhEn("导出失败: 无法写入文件", "Export failed: cannot write the file");

    // ---- 报告表格标签（族内复用）----
    // 「路径: 」「任务ID: 」这类其实跨族都在用，但 CliMessages 里还没有对应成员，
    // 先落在本族；等提到框架层时这两条要跟着搬走（见交付报告）。
    public static final Text LABEL_PATH = Text.zhEn("路径: ", "Path: ");
    public static final Text LABEL_TASK_ID = Text.zhEn("任务ID: ", "Task ID: ");
    public static final Text LABEL_RATE = Text.zhEn("频率: ", "Rate: ");
    public static final Text LABEL_DURATION = Text.zhEn("持续时间: ", "Duration: ");
    public static final Text LABEL_SAMPLE_RATE = Text.zhEn("采样率: ", "Sample rate: ");
    public static final Text LABEL_TOTAL_SAMPLES = Text.zhEn("总采样数: ", "Total samples: ");
    public static final Text LABEL_TOTAL_SAMPLE_COUNT = Text.zhEn("总采样次数: ", "Total samples: ");
    public static final Text LABEL_TOTAL_SAMPLES_SHORT = Text.zhEn("总采样: ", "Total samples: ");
    public static final Text LABEL_CAPTURED_METHODS = Text.zhEn("捕获方法数: ", "Methods captured: ");
    public static final Text LABEL_METHOD_COUNT = Text.zhEn("方法数: ", "Methods: ");
    public static final Text LABEL_DETECTED_THREADS = Text.zhEn("检测线程数: ", "Threads detected: ");
    public static final Text LABEL_THREAD_COUNT = Text.zhEn("线程数: ", "Threads: ");
    public static final Text LABEL_STATUS_INDENTED = Text.zhEn("    状态: ", "    Status: ");
    public static final Text LABEL_SAMPLE_COUNT_INDENTED = Text.zhEn("    采样数量: ", "    Samples: ");
    public static final Text HOT_METHODS_TOP = Text.zhEn("热点方法 TOP-%d:", "Hot methods TOP-%d:");

    // ---- Hook 报告（族内复用）----
    public static final Text ERR_HOOK_DATA_NOT_FOUND = Text.zhEn("错误: Hook数据不存在 (ID: %d)",
            "Error: hook data not found (ID: %d)");
    public static final Text AVAILABLE_HOOK_IDS = Text.zhEn("可用的Hook IDs: %s", "Available hook IDs: %s");
    public static final Text LABEL_TARGET_CLASS = Text.zhEn("目标类: ", "Target class: ");
    public static final Text LABEL_TARGET_METHOD = Text.zhEn("目标方法: ", "Target method: ");
    public static final Text LABEL_CALL_COUNT = Text.zhEn("调用次数: ", "Call count: ");
    public static final Text LABEL_TOTAL_TIME = Text.zhEn("总耗时: ", "Total time: ");
    public static final Text LABEL_AVG_MIN_MAX = Text.zhEn("平均/最小/最大: ", "Avg/min/max: ");

    // ---- Trace / Systrace（族内复用）----
    public static final Text LABEL_TRACE_COUNT = Text.zhEn("追踪数: ", "Traces: ");
    public static final Text LABEL_TRACE_CATEGORIES = Text.zhEn("追踪类别: ", "Categories: ");
    public static final Text LABEL_OUTPUT_FILE = Text.zhEn("输出文件: ", "Output file: ");
    public static final Text LABEL_SOURCE_FILE = Text.zhEn("源文件: ", "Source file: ");
    public static final Text ERR_START_SYSTRACE_FAILED = Text.zhEn("启动 Systrace 失败: %s", "Failed to start systrace: %s");

    // ---- 时长单位后缀（族内复用）----
    // 中文侧带前导空格、英文侧同样带，保证「10.00 秒」和「10.00 s」的排版一致。
    public static final Text UNIT_SECONDS = Text.zhEn(" 秒", " s");
    public static final Text UNIT_MINUTES = Text.zhEn(" 分 ", " min ");

    private PerformanceTexts() {
    }

    /**
     * 由 {@code CliTexts} 的静态块调用。必须是 public —— 它在另一个包里。
     * 命名上刻意带 register 而不是「构造时自己注册」：登记动作集中在 CliTexts 一处，
     * 「哪些族登记了」才看得全，漏登记也能被守卫测试发现。
     */
    public static void register(Map<String, String> zh, Map<String, String> en) {
        zh.put(CMD_PERFORMANCE_DESC, "性能分析命令, 支持多种分析方式（采样, 多线程, 分层, Trace, Systrace, Hook)");
        en.put(CMD_PERFORMANCE_DESC, "Performance profiling commands: sampling, multithreading, hierarchical, trace, systrace and hook");

        zh.put(ROUTE_PERFORMANCE_SAMPLE_START_DESC, "开始单线程采样");
        en.put(ROUTE_PERFORMANCE_SAMPLE_START_DESC, "Start single-threaded sampling");

        zh.put(ROUTE_PERFORMANCE_SAMPLE_STOP_DESC, "停止单线程采样");
        en.put(ROUTE_PERFORMANCE_SAMPLE_STOP_DESC, "Stop single-threaded sampling");

        zh.put(ROUTE_PERFORMANCE_SAMPLE_REPORT_DESC, "查看采样报告");
        en.put(ROUTE_PERFORMANCE_SAMPLE_REPORT_DESC, "Show the sampling report");

        zh.put(ROUTE_PERFORMANCE_SAMPLE_EXPORT_DESC, "导出采样数据");
        en.put(ROUTE_PERFORMANCE_SAMPLE_EXPORT_DESC, "Export sampling data");

        zh.put(ROUTE_PERFORMANCE_MULTITHREAD_START_DESC, "开始多线程采样");
        en.put(ROUTE_PERFORMANCE_MULTITHREAD_START_DESC, "Start multi-threaded sampling");

        zh.put(ROUTE_PERFORMANCE_MULTITHREAD_STOP_DESC, "停止多线程采样");
        en.put(ROUTE_PERFORMANCE_MULTITHREAD_STOP_DESC, "Stop multi-threaded sampling");

        zh.put(ROUTE_PERFORMANCE_MULTITHREAD_REPORT_DESC, "查看多线程采样报告");
        en.put(ROUTE_PERFORMANCE_MULTITHREAD_REPORT_DESC, "Show the multi-threaded sampling report");

        zh.put(ROUTE_PERFORMANCE_MULTITHREAD_EXPORT_DESC, "导出多线程采样数据");
        en.put(ROUTE_PERFORMANCE_MULTITHREAD_EXPORT_DESC, "Export multi-threaded sampling data");

        zh.put(ROUTE_PERFORMANCE_HIERARCHICAL_START_DESC, "开始分层采样");
        en.put(ROUTE_PERFORMANCE_HIERARCHICAL_START_DESC, "Start hierarchical sampling");

        zh.put(ROUTE_PERFORMANCE_HIERARCHICAL_STOP_DESC, "停止分层采样");
        en.put(ROUTE_PERFORMANCE_HIERARCHICAL_STOP_DESC, "Stop hierarchical sampling");

        zh.put(ROUTE_PERFORMANCE_HIERARCHICAL_REPORT_DESC, "查看分层采样报告");
        en.put(ROUTE_PERFORMANCE_HIERARCHICAL_REPORT_DESC, "Show the hierarchical sampling report");

        zh.put(ROUTE_PERFORMANCE_HIERARCHICAL_EXPORT_DESC, "导出分层数据");
        en.put(ROUTE_PERFORMANCE_HIERARCHICAL_EXPORT_DESC, "Export hierarchical data");

        zh.put(ROUTE_PERFORMANCE_TRACE_START_DESC, "开始 Trace");
        en.put(ROUTE_PERFORMANCE_TRACE_START_DESC, "Start a trace");

        zh.put(ROUTE_PERFORMANCE_TRACE_STOP_DESC, "停止 Trace");
        en.put(ROUTE_PERFORMANCE_TRACE_STOP_DESC, "Stop the trace");

        zh.put(ROUTE_PERFORMANCE_TRACE_REPORT_DESC, "查看 Trace 报告");
        en.put(ROUTE_PERFORMANCE_TRACE_REPORT_DESC, "Show the trace report");

        zh.put(ROUTE_PERFORMANCE_TRACE_EXPORT_DESC, "导出 Trace 数据");
        en.put(ROUTE_PERFORMANCE_TRACE_EXPORT_DESC, "Export trace data");

        zh.put(ROUTE_PERFORMANCE_SYSTRACE_START_DESC, "开始 Systrace");
        en.put(ROUTE_PERFORMANCE_SYSTRACE_START_DESC, "Start systrace");

        zh.put(ROUTE_PERFORMANCE_SYSTRACE_STOP_DESC, "停止 Systrace");
        en.put(ROUTE_PERFORMANCE_SYSTRACE_STOP_DESC, "Stop systrace");

        zh.put(ROUTE_PERFORMANCE_SYSTRACE_REPORT_DESC, "查看 Systrace 报告");
        en.put(ROUTE_PERFORMANCE_SYSTRACE_REPORT_DESC, "Show the systrace report");

        zh.put(ROUTE_PERFORMANCE_SYSTRACE_EXPORT_DESC, "导出 Systrace 数据");
        en.put(ROUTE_PERFORMANCE_SYSTRACE_EXPORT_DESC, "Export systrace data");

        zh.put(ROUTE_PERFORMANCE_HOOK_START_DESC, "添加性能 Hook");
        en.put(ROUTE_PERFORMANCE_HOOK_START_DESC, "Add a performance hook");

        zh.put(ROUTE_PERFORMANCE_HOOK_STOP_DESC, "停止 Hook");
        en.put(ROUTE_PERFORMANCE_HOOK_STOP_DESC, "Stop a hook");

        zh.put(ROUTE_PERFORMANCE_HOOK_REPORT_DESC, "查看 Hook 报告");
        en.put(ROUTE_PERFORMANCE_HOOK_REPORT_DESC, "Show the hook report");

        zh.put(ROUTE_PERFORMANCE_HOOK_EXPORT_DESC, "导出 Hook 数据");
        en.put(ROUTE_PERFORMANCE_HOOK_EXPORT_DESC, "Export hook data");

        zh.put(ROUTE_PERFORMANCE_LIST_DESC, "列出所有任务");
        en.put(ROUTE_PERFORMANCE_LIST_DESC, "List all tasks");

        zh.put(ROUTE_PERFORMANCE_CLEAR_DESC, "清除所有任务");
        en.put(ROUTE_PERFORMANCE_CLEAR_DESC, "Clear all tasks");

        zh.put(SUB_PERFORMANCE_SAMPLE_DESC, "单线程方法采样分析，统计方法调用频率和热点");
        en.put(SUB_PERFORMANCE_SAMPLE_DESC, "Single-threaded method sampling that counts call frequency and finds hot spots");

        // optionsDesc 是多行文本块，整块一个 id（含缩进，逐字节照抄原注解里的内容）
        zh.put(SUB_PERFORMANCE_SAMPLE_OPTIONS, """
                Actions:
                    start [rate] [--exclude pattern]   开始采样 (默认100Hz)
                    stop <id>                          停止采样
                    report [id]                        查看报告 (默认最新)
                    export <id> <path>                 导出数据""");
        en.put(SUB_PERFORMANCE_SAMPLE_OPTIONS, """
                Actions:
                    start [rate] [--exclude pattern]   Start sampling (100 Hz by default)
                    stop <id>                          Stop sampling
                    report [id]                        Show the report (latest by default)
                    export <id> <path>                 Export data""");

        zh.put(SUB_PERFORMANCE_MULTITHREAD_DESC, "多线程采样分析，分别统计各线程的方法调用情况");
        en.put(SUB_PERFORMANCE_MULTITHREAD_DESC, "Multi-threaded sampling that counts method calls per thread");

        zh.put(SUB_PERFORMANCE_MULTITHREAD_OPTIONS, """
                Actions:
                    start [rate] [--exclude pattern]   开始多线程采样
                    stop <id>                          停止采样
                    report [id]                        查看报告 (默认最新)
                    export <id> <path>                 导出数据""");
        en.put(SUB_PERFORMANCE_MULTITHREAD_OPTIONS, """
                Actions:
                    start [rate] [--exclude pattern]   Start multi-threaded sampling
                    stop <id>                          Stop sampling
                    report [id]                        Show the report (latest by default)
                    export <id> <path>                 Export data""");

        zh.put(SUB_PERFORMANCE_HIERARCHICAL_DESC, "分层采样分析，按调用层次展示方法耗时分布");
        en.put(SUB_PERFORMANCE_HIERARCHICAL_DESC, "Hierarchical sampling that breaks down method durations by call depth");

        zh.put(SUB_PERFORMANCE_HIERARCHICAL_OPTIONS, """
                Actions:
                    start [rate] [--exclude pattern]   开始分层采样
                    stop <id>                          停止采样
                    report [id]                        查看报告 (默认最新)
                    export <id> <path>                 导出数据""");
        en.put(SUB_PERFORMANCE_HIERARCHICAL_OPTIONS, """
                Actions:
                    start [rate] [--exclude pattern]   Start hierarchical sampling
                    stop <id>                          Stop sampling
                    report [id]                        Show the report (latest by default)
                    export <id> <path>                 Export data""");

        zh.put(SUB_PERFORMANCE_TRACE_DESC, "方法调用链追踪，记录完整的方法进入/退出链路");
        en.put(SUB_PERFORMANCE_TRACE_DESC, "Method call tracing that records the full method entry/exit chain");

        zh.put(SUB_PERFORMANCE_TRACE_OPTIONS, """
                Actions:
                    start                              开始 Trace 追踪
                    stop <id>                           停止追踪
                    report [id]                         查看报告 (默认最新)
                    export <id> <path>                  导出数据""");
        en.put(SUB_PERFORMANCE_TRACE_OPTIONS, """
                Actions:
                    start                              Start tracing
                    stop <id>                          Stop tracing
                    report [id]                        Show the report (latest by default)
                    export <id> <path>                 Export data""");

        zh.put(SUB_PERFORMANCE_SYSTRACE_DESC, "系统级 Systrace 性能追踪，捕获系统-wide 性能数据");
        en.put(SUB_PERFORMANCE_SYSTRACE_DESC, "System-level systrace profiling that captures system-wide performance data");

        zh.put(SUB_PERFORMANCE_SYSTRACE_OPTIONS, """
                Actions:
                    start [duration] [categories...]     开始 Systrace (默认10秒)
                    stop <id>                             停止追踪
                    report [id]                           查看报告 (默认最新)
                    export <id> <path>                    导出 HTML 报告""");
        en.put(SUB_PERFORMANCE_SYSTRACE_OPTIONS, """
                Actions:
                    start [duration] [categories...]     Start systrace (10 seconds by default)
                    stop <id>                            Stop tracing
                    report [id]                          Show the report (latest by default)
                    export <id> <path>                   Export an HTML report""");

        zh.put(SUB_PERFORMANCE_HOOK_DESC, "性能 Hook 注入，在目标方法前后插入计时探针");
        en.put(SUB_PERFORMANCE_HOOK_DESC, "Performance hook injection that inserts timing probes before and after target methods");

        zh.put(SUB_PERFORMANCE_HOOK_OPTIONS, """
                Actions:
                    start <class> <method> [sig]         添加性能 Hook
                    stop <id>                            停止 Hook
                    report [id]                          查看报告 (默认最新)
                    export <id> <path>                   导出数据""");
        en.put(SUB_PERFORMANCE_HOOK_OPTIONS, """
                Actions:
                    start <class> <method> [sig]         Add a performance hook
                    stop <id>                            Stop a hook
                    report [id]                          Show the report (latest by default)
                    export <id> <path>                   Export data""");

        zh.put(PARAM_PERFORMANCE_SAMPLE_START_RATE_DESC, "采样频率（Hz）");
        en.put(PARAM_PERFORMANCE_SAMPLE_START_RATE_DESC, "Sampling rate (Hz)");

        zh.put(PARAM_PERFORMANCE_SAMPLE_START_EXCLUDE_DESC, "排除的类/方法模式");
        en.put(PARAM_PERFORMANCE_SAMPLE_START_EXCLUDE_DESC, "Class/method pattern to exclude");

        zh.put(PARAM_PERFORMANCE_SAMPLE_STOP_ID_DESC, "任务 ID");
        en.put(PARAM_PERFORMANCE_SAMPLE_STOP_ID_DESC, "Task ID");

        zh.put(PARAM_PERFORMANCE_SAMPLE_REPORT_ID_DESC, "任务 ID（可选，默认显示最新）");
        en.put(PARAM_PERFORMANCE_SAMPLE_REPORT_ID_DESC, "Task ID (optional, defaults to the latest)");

        zh.put(PARAM_PERFORMANCE_SAMPLE_EXPORT_ID_DESC, "任务 ID");
        en.put(PARAM_PERFORMANCE_SAMPLE_EXPORT_ID_DESC, "Task ID");

        zh.put(PARAM_PERFORMANCE_SAMPLE_EXPORT_FILEPATH_DESC, "导出文件路径");
        en.put(PARAM_PERFORMANCE_SAMPLE_EXPORT_FILEPATH_DESC, "Export file path");

        zh.put(PARAM_PERFORMANCE_MULTITHREAD_START_RATE_DESC, "采样频率（Hz）");
        en.put(PARAM_PERFORMANCE_MULTITHREAD_START_RATE_DESC, "Sampling rate (Hz)");

        zh.put(PARAM_PERFORMANCE_MULTITHREAD_START_EXCLUDE_DESC, "排除的类/方法模式");
        en.put(PARAM_PERFORMANCE_MULTITHREAD_START_EXCLUDE_DESC, "Class/method pattern to exclude");

        zh.put(PARAM_PERFORMANCE_MULTITHREAD_STOP_ID_DESC, "任务 ID");
        en.put(PARAM_PERFORMANCE_MULTITHREAD_STOP_ID_DESC, "Task ID");

        zh.put(PARAM_PERFORMANCE_MULTITHREAD_REPORT_ID_DESC, "任务 ID（可选，默认显示最新）");
        en.put(PARAM_PERFORMANCE_MULTITHREAD_REPORT_ID_DESC, "Task ID (optional, defaults to the latest)");

        zh.put(PARAM_PERFORMANCE_MULTITHREAD_EXPORT_ID_DESC, "任务 ID");
        en.put(PARAM_PERFORMANCE_MULTITHREAD_EXPORT_ID_DESC, "Task ID");

        zh.put(PARAM_PERFORMANCE_MULTITHREAD_EXPORT_FILEPATH_DESC, "导出文件路径");
        en.put(PARAM_PERFORMANCE_MULTITHREAD_EXPORT_FILEPATH_DESC, "Export file path");

        zh.put(PARAM_PERFORMANCE_HIERARCHICAL_START_RATE_DESC, "采样频率（Hz）");
        en.put(PARAM_PERFORMANCE_HIERARCHICAL_START_RATE_DESC, "Sampling rate (Hz)");

        zh.put(PARAM_PERFORMANCE_HIERARCHICAL_START_EXCLUDE_DESC, "排除的类/方法模式");
        en.put(PARAM_PERFORMANCE_HIERARCHICAL_START_EXCLUDE_DESC, "Class/method pattern to exclude");

        zh.put(PARAM_PERFORMANCE_HIERARCHICAL_STOP_ID_DESC, "任务 ID");
        en.put(PARAM_PERFORMANCE_HIERARCHICAL_STOP_ID_DESC, "Task ID");

        zh.put(PARAM_PERFORMANCE_HIERARCHICAL_REPORT_ID_DESC, "任务 ID（可选，默认显示最新）");
        en.put(PARAM_PERFORMANCE_HIERARCHICAL_REPORT_ID_DESC, "Task ID (optional, defaults to the latest)");

        zh.put(PARAM_PERFORMANCE_HIERARCHICAL_EXPORT_ID_DESC, "任务 ID");
        en.put(PARAM_PERFORMANCE_HIERARCHICAL_EXPORT_ID_DESC, "Task ID");

        zh.put(PARAM_PERFORMANCE_HIERARCHICAL_EXPORT_FILEPATH_DESC, "导出文件路径");
        en.put(PARAM_PERFORMANCE_HIERARCHICAL_EXPORT_FILEPATH_DESC, "Export file path");

        zh.put(PARAM_PERFORMANCE_TRACE_STOP_ID_DESC, "任务 ID");
        en.put(PARAM_PERFORMANCE_TRACE_STOP_ID_DESC, "Task ID");

        zh.put(PARAM_PERFORMANCE_TRACE_REPORT_ID_DESC, "任务 ID（可选，默认显示最新）");
        en.put(PARAM_PERFORMANCE_TRACE_REPORT_ID_DESC, "Task ID (optional, defaults to the latest)");

        zh.put(PARAM_PERFORMANCE_TRACE_EXPORT_ID_DESC, "任务 ID");
        en.put(PARAM_PERFORMANCE_TRACE_EXPORT_ID_DESC, "Task ID");

        zh.put(PARAM_PERFORMANCE_TRACE_EXPORT_FILEPATH_DESC, "导出文件路径");
        en.put(PARAM_PERFORMANCE_TRACE_EXPORT_FILEPATH_DESC, "Export file path");

        zh.put(PARAM_PERFORMANCE_SYSTRACE_START_DURATION_DESC, "持续时间(ms)");
        en.put(PARAM_PERFORMANCE_SYSTRACE_START_DURATION_DESC, "Duration (ms)");

        zh.put(PARAM_PERFORMANCE_SYSTRACE_START_CATEGORIES_DESC, "跟踪类别");
        en.put(PARAM_PERFORMANCE_SYSTRACE_START_CATEGORIES_DESC, "Trace categories");

        zh.put(PARAM_PERFORMANCE_SYSTRACE_STOP_ID_DESC, "任务 ID");
        en.put(PARAM_PERFORMANCE_SYSTRACE_STOP_ID_DESC, "Task ID");

        zh.put(PARAM_PERFORMANCE_SYSTRACE_REPORT_ID_DESC, "任务 ID（可选，默认显示最新）");
        en.put(PARAM_PERFORMANCE_SYSTRACE_REPORT_ID_DESC, "Task ID (optional, defaults to the latest)");

        zh.put(PARAM_PERFORMANCE_SYSTRACE_EXPORT_ID_DESC, "任务 ID");
        en.put(PARAM_PERFORMANCE_SYSTRACE_EXPORT_ID_DESC, "Task ID");

        zh.put(PARAM_PERFORMANCE_SYSTRACE_EXPORT_FILEPATH_DESC, "导出文件路径");
        en.put(PARAM_PERFORMANCE_SYSTRACE_EXPORT_FILEPATH_DESC, "Export file path");

        zh.put(PARAM_PERFORMANCE_HOOK_START_CLASSNAME_DESC, "目标类名");
        en.put(PARAM_PERFORMANCE_HOOK_START_CLASSNAME_DESC, "Target class name");

        zh.put(PARAM_PERFORMANCE_HOOK_START_METHODNAME_DESC, "方法名");
        en.put(PARAM_PERFORMANCE_HOOK_START_METHODNAME_DESC, "Method name");

        zh.put(PARAM_PERFORMANCE_HOOK_START_SIGNATURE_DESC, "方法签名");
        en.put(PARAM_PERFORMANCE_HOOK_START_SIGNATURE_DESC, "Method signature");

        zh.put(PARAM_PERFORMANCE_HOOK_STOP_ID_DESC, "任务 ID");
        en.put(PARAM_PERFORMANCE_HOOK_STOP_ID_DESC, "Task ID");

        zh.put(PARAM_PERFORMANCE_HOOK_REPORT_ID_DESC, "任务 ID（可选，默认显示最新）");
        en.put(PARAM_PERFORMANCE_HOOK_REPORT_ID_DESC, "Task ID (optional, defaults to the latest)");

        zh.put(PARAM_PERFORMANCE_HOOK_EXPORT_ID_DESC, "任务 ID");
        en.put(PARAM_PERFORMANCE_HOOK_EXPORT_ID_DESC, "Task ID");

        zh.put(PARAM_PERFORMANCE_HOOK_EXPORT_FILEPATH_DESC, "导出文件路径");
        en.put(PARAM_PERFORMANCE_HOOK_EXPORT_FILEPATH_DESC, "Export file path");
    }
}
