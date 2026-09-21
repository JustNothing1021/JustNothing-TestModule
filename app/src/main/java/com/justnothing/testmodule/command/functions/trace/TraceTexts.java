package com.justnothing.testmodule.command.functions.trace;

import com.justnothing.testmodule.command.framework.i18n.CliTexts;
import com.justnothing.testmodule.command.framework.i18n.Text;

import java.util.Map;

/**
 * trace 命令族的 CLI 文案（id 常量 + 中英对照）。
 *
 * <p>命名与 id 规范见 {@link CliTexts}。
 * 本类由 {@code CliTexts} 的静态块登记，新增条目只需在这里加常量 + 两行 put。</p>
 *
 * <p>英文允许缺失（只 put 中文），缺失时英文环境回落显示中文 —— 所以翻译可以一条一条补。</p>
 */
public final class TraceTexts {

    // ==================== @Cmd（主命令）====================
    public static final String CMD_TRACE_DESC = "cmd.trace.desc";

    // ==================== @CmdRoutes.Route（命令列表里那一行）====================
    public static final String ROUTE_TRACE_ADD_DESC = "route.trace.add.desc";
    public static final String ROUTE_TRACE_LIST_DESC = "route.trace.list.desc";
    public static final String ROUTE_TRACE_SHOW_DESC = "route.trace.show.desc";
    public static final String ROUTE_TRACE_EXPORT_DESC = "route.trace.export.desc";
    public static final String ROUTE_TRACE_STOP_DESC = "route.trace.stop.desc";
    public static final String ROUTE_TRACE_CLEAR_DESC = "route.trace.clear.desc";

    // ==================== @SubCommandInfo（帮助正文）====================
    public static final String SUB_TRACE_MANAGE_DESC = "sub.trace.manage.desc";
    public static final String SUB_TRACE_QUERY_DESC = "sub.trace.query.desc";

    // ==================== @CmdParam（参数说明）====================
    public static final String PARAM_TRACE_ADD_CLASSNAME_DESC = "param.trace.add.className.desc";
    public static final String PARAM_TRACE_ADD_METHODNAME_DESC = "param.trace.add.methodName.desc";
    public static final String PARAM_TRACE_ADD_SIG_DESC = "param.trace.add.sig.desc";
    public static final String PARAM_TRACE_SHOW_ID_DESC = "param.trace.show.id.desc";
    public static final String PARAM_TRACE_EXPORT_ID_DESC = "param.trace.export.id.desc";
    public static final String PARAM_TRACE_EXPORT_FILEPATH_DESC = "param.trace.export.filePath.desc";
    public static final String PARAM_TRACE_STOP_ID_DESC = "param.trace.stop.id.desc";

    // ==================== 族内复用输出文案 ====================
    // 本节只放本族内部出现两次以上的输出文案（中英并排的 Text 常量）。
    // 只出现一次的在调用点就地写 Text.zhEn(...)；跨族复用的（「错误: 」那些）在 CliMessages 里。

    /** 请求类型分发兜底（query / manage 各一处）。 */
    public static final Text UNSUPPORTED_REQUEST_TYPE =
            Text.zhEn("不支持的请求类型: %s", "Unsupported request type: %s");

    /** 按 ID 取任务失败（show / export 各一处）。 */
    public static final Text TRACE_TASK_NOT_FOUND =
            Text.zhEn("未找到trace任务 (ID: %s)", "Trace task not found (ID: %s)");

    private TraceTexts() {
    }

    /**
     * 由 {@code CliTexts} 的静态块调用。必须是 public —— 它在另一个包里。
     * 命名上刻意带 register 而不是「构造时自己注册」：登记动作集中在 CliTexts 一处，
     * 「哪些族登记了」才看得全，漏登记也能被守卫测试发现。
     */
    public static void register(Map<String, String> zh, Map<String, String> en) {
        zh.put(CMD_TRACE_DESC, "跟踪方法调用链，生成调用树");
        en.put(CMD_TRACE_DESC, "Trace method call chains and build a call tree");

        zh.put(ROUTE_TRACE_ADD_DESC, "添加trace任务");
        en.put(ROUTE_TRACE_ADD_DESC, "Add a trace task");

        zh.put(ROUTE_TRACE_LIST_DESC, "列出所有任务");
        en.put(ROUTE_TRACE_LIST_DESC, "List all trace tasks");

        zh.put(ROUTE_TRACE_SHOW_DESC, "显示调用树");
        en.put(ROUTE_TRACE_SHOW_DESC, "Show the call tree");

        zh.put(ROUTE_TRACE_EXPORT_DESC, "导出结果");
        en.put(ROUTE_TRACE_EXPORT_DESC, "Export the result");

        zh.put(ROUTE_TRACE_STOP_DESC, "停止任务");
        en.put(ROUTE_TRACE_STOP_DESC, "Stop a trace task");

        zh.put(ROUTE_TRACE_CLEAR_DESC, "清除所有任务");
        en.put(ROUTE_TRACE_CLEAR_DESC, "Clear all trace tasks");

        zh.put(SUB_TRACE_MANAGE_DESC, "Trace 管理操作 - 添加/停止/清除");
        en.put(SUB_TRACE_MANAGE_DESC, "Trace management: add, stop and clear");

        zh.put(SUB_TRACE_QUERY_DESC, "Trace 查询操作 - 列表/显示/导出");
        en.put(SUB_TRACE_QUERY_DESC, "Trace queries: list, show and export");

        zh.put(PARAM_TRACE_ADD_CLASSNAME_DESC, "目标类名");
        en.put(PARAM_TRACE_ADD_CLASSNAME_DESC, "Target class name");

        zh.put(PARAM_TRACE_ADD_METHODNAME_DESC, "目标方法名");
        en.put(PARAM_TRACE_ADD_METHODNAME_DESC, "Target method name");

        zh.put(PARAM_TRACE_ADD_SIG_DESC, "方法签名");
        en.put(PARAM_TRACE_ADD_SIG_DESC, "Method signature");

        zh.put(PARAM_TRACE_SHOW_ID_DESC, "跟踪任务 ID");
        en.put(PARAM_TRACE_SHOW_ID_DESC, "Trace task ID");

        zh.put(PARAM_TRACE_EXPORT_ID_DESC, "跟踪任务 ID");
        en.put(PARAM_TRACE_EXPORT_ID_DESC, "Trace task ID");

        zh.put(PARAM_TRACE_EXPORT_FILEPATH_DESC, "导出文件路径");
        en.put(PARAM_TRACE_EXPORT_FILEPATH_DESC, "Export file path");

        zh.put(PARAM_TRACE_STOP_ID_DESC, "跟踪任务 ID");
        en.put(PARAM_TRACE_STOP_ID_DESC, "Trace task ID");
    }
}
