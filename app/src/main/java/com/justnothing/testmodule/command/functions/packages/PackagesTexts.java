package com.justnothing.testmodule.command.functions.packages;

import java.util.Map;

/**
 * packages 命令族的 CLI 文案（id 常量 + 中英对照）。
 *
 * <p>命名与 id 规范见 {@link com.justnothing.testmodule.command.framework.i18n.CliTexts}。
 * 本类由 {@code CliTexts} 的静态块登记，新增条目只需在这里加常量 + 两行 put。</p>
 *
 * <p>英文允许缺失（只 put 中文），缺失时英文环境回落显示中文 —— 所以翻译可以一条一条补。</p>
 */
public final class PackagesTexts {

    // ==================== @Cmd（主命令）====================
    public static final String CMD_PACKAGES_DESC = "cmd.packages.desc";

    // ==================== @CmdRoutes.Route（命令列表里那一行）====================
    public static final String ROUTE_PACKAGES_LIST_DESC = "route.packages.list.desc";

    private PackagesTexts() {
    }

    /**
     * 由 {@code CliTexts} 的静态块调用。必须是 public —— 它在另一个包里。
     */
    public static void register(Map<String, String> zh, Map<String, String> en) {
        zh.put(CMD_PACKAGES_DESC, "列出当前进程的所有已知包名");
        en.put(CMD_PACKAGES_DESC, "List all package names known to the current process");

        zh.put(ROUTE_PACKAGES_LIST_DESC, "列出所有包名");
        en.put(ROUTE_PACKAGES_LIST_DESC, "List all package names");
    }
}
