package com.justnothing.testmodule.command.functions.help;

import java.util.Map;

/**
 * help 命令族的 CLI 文案（id 常量 + 中英对照）。
 *
 * <p>命名与 id 规范见 {@link com.justnothing.testmodule.command.framework.i18n.CliTexts}。
 * 本类由 {@code CliTexts} 的静态块登记，新增条目只需在这里加常量 + 两行 put。</p>
 *
 * <p>英文允许缺失（只 put 中文），缺失时英文环境回落显示中文 —— 所以翻译可以一条一条补。</p>
 *
 * <p>该族的路由 {@code path} 为空（子命令即主命令本身），故 id 里省掉空的那一段，
 * 写作 {@code cmd.help.desc} / {@code route.help.desc}。</p>
 */
public final class HelpTexts {

    // ==================== @Cmd（主命令）====================
    public static final String CMD_HELP_DESC = "cmd.help.desc";

    // ==================== @CmdRoutes.Route（命令列表里那一行）====================
    public static final String ROUTE_HELP_DESC = "route.help.desc";

    private HelpTexts() {
    }

    /**
     * 由 {@code CliTexts} 的静态块调用。必须是 public —— 它在另一个包里。
     */
    public static void register(Map<String, String> zh, Map<String, String> en) {
        zh.put(CMD_HELP_DESC, "获取命令帮助信息");
        en.put(CMD_HELP_DESC, "Get help for commands");

        zh.put(ROUTE_HELP_DESC, "显示帮助信息");
        en.put(ROUTE_HELP_DESC, "Show help information");
    }
}
