package com.justnothing.testmodule.command.functions.exportcontext;

import java.util.Map;

/**
 * export-context 命令族的 CLI 文案（id 常量 + 中英对照）。
 *
 * <p>命名与 id 规范见 {@link com.justnothing.testmodule.command.framework.i18n.CliTexts}。
 * 本类由 {@code CliTexts} 的静态块登记，新增条目只需在这里加常量 + 两行 put。</p>
 *
 * <p>英文允许缺失（只 put 中文），缺失时英文环境回落显示中文 —— 所以翻译可以一条一条补。</p>
 *
 * <p>该族的路由 {@code path} 为空（子命令即主命令本身），故 id 里省掉空的那一段，
 * 写作 {@code cmd.export-context.desc} / {@code route.export-context.desc} /
 * {@code param.export-context.*.desc}。</p>
 */
public final class ExportContextTexts {

    // ==================== @Cmd（主命令）====================
    public static final String CMD_EXPORT_CONTEXT_DESC = "cmd.export-context.desc";

    // ==================== @CmdRoutes.Route（命令列表里那一行）====================
    public static final String ROUTE_EXPORT_CONTEXT_DESC = "route.export-context.desc";

    // ==================== @CmdParam（参数说明）====================
    public static final String PARAM_EXPORT_CONTEXT_PRETTY_PRINTING_DESC =
        "param.export-context.prettyPrinting.desc";

    private ExportContextTexts() {
    }

    /**
     * 由 {@code CliTexts} 的静态块调用。必须是 public —— 它在另一个包里。
     */
    public static void register(Map<String, String> zh, Map<String, String> en) {
        zh.put(CMD_EXPORT_CONTEXT_DESC, "导出设备上下文信息, 包括HTTP配置, 设备标识等");
        en.put(CMD_EXPORT_CONTEXT_DESC, "Export device context (HTTP config, device identifiers, etc.)");

        zh.put(ROUTE_EXPORT_CONTEXT_DESC, "导出设备上下文信息");
        en.put(ROUTE_EXPORT_CONTEXT_DESC, "Export device context information");

        zh.put(PARAM_EXPORT_CONTEXT_PRETTY_PRINTING_DESC, "以表格格式输出 (默认为JSON原始数据)");
        en.put(PARAM_EXPORT_CONTEXT_PRETTY_PRINTING_DESC,
            "Print as a formatted table (raw JSON by default)");
    }
}
