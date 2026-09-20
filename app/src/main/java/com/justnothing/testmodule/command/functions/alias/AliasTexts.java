package com.justnothing.testmodule.command.functions.alias;

import java.util.Map;

/**
 * alias 命令族的 CLI 文案（id 常量 + 中英对照）。
 *
 * <p>命名与 id 规范见 {@link com.justnothing.testmodule.command.framework.i18n.CliTexts}。
 * 本类由 {@code CliTexts} 的静态块登记，新增条目只需在这里加常量 + 两行 put。</p>
 *
 * <p>英文允许缺失（只 put 中文），缺失时英文环境回落显示中文 —— 所以翻译可以一条一条补。</p>
 *
 * <p>注意：id 里的 {@code alias add} / {@code alias remove} 两条 usage 含中文占位符
 * （{@code <别名>}），故一并拆成 {@code sub.*.usage}。</p>
 */
public final class AliasTexts {

    // ==================== @Cmd（主命令）====================
    public static final String CMD_ALIAS_DESC = "cmd.alias.desc";

    // ==================== @CmdRoutes.Route（命令列表里那一行）====================
    public static final String ROUTE_ALIAS_ADD_DESC = "route.alias.add.desc";
    public static final String ROUTE_ALIAS_LIST_DESC = "route.alias.list.desc";
    public static final String ROUTE_ALIAS_REMOVE_DESC = "route.alias.remove.desc";
    public static final String ROUTE_ALIAS_CLEAR_DESC = "route.alias.clear.desc";

    // ==================== @SubCommandInfo（帮助正文）====================
    public static final String SUB_ALIAS_ADD_DESC = "sub.alias.add.desc";
    public static final String SUB_ALIAS_ADD_USAGE = "sub.alias.add.usage";
    public static final String SUB_ALIAS_ADD_OPTIONS = "sub.alias.add.options";
    public static final String SUB_ALIAS_LIST_DESC = "sub.alias.list.desc";
    public static final String SUB_ALIAS_LIST_OPTIONS = "sub.alias.list.options";
    public static final String SUB_ALIAS_REMOVE_DESC = "sub.alias.remove.desc";
    public static final String SUB_ALIAS_REMOVE_USAGE = "sub.alias.remove.usage";
    public static final String SUB_ALIAS_REMOVE_OPTIONS = "sub.alias.remove.options";
    public static final String SUB_ALIAS_CLEAR_DESC = "sub.alias.clear.desc";
    public static final String SUB_ALIAS_CLEAR_OPTIONS = "sub.alias.clear.options";

    // ==================== @CmdParam（参数说明）====================
    public static final String PARAM_ALIAS_ADD_NAME_DESC = "param.alias.add.name.desc";
    public static final String PARAM_ALIAS_ADD_COMMAND_DESC = "param.alias.add.command.desc";
    public static final String PARAM_ALIAS_REMOVE_NAME_DESC = "param.alias.remove.name.desc";

    private AliasTexts() {
    }

    /**
     * 由 {@code CliTexts} 的静态块调用。必须是 public —— 它在另一个包里。
     */
    public static void register(Map<String, String> zh, Map<String, String> en) {
        zh.put(CMD_ALIAS_DESC, "管理命令别名，用于简化常用命令");
        en.put(CMD_ALIAS_DESC, "Manage command aliases that shorten frequently used commands");

        zh.put(ROUTE_ALIAS_ADD_DESC, "添加新的命令别名");
        en.put(ROUTE_ALIAS_ADD_DESC, "Add a new command alias");

        zh.put(ROUTE_ALIAS_LIST_DESC, "列出所有已定义的别名");
        en.put(ROUTE_ALIAS_LIST_DESC, "List all defined aliases");

        zh.put(ROUTE_ALIAS_REMOVE_DESC, "删除指定的别名");
        en.put(ROUTE_ALIAS_REMOVE_DESC, "Remove the specified alias");

        zh.put(ROUTE_ALIAS_CLEAR_DESC, "清空所有别名");
        en.put(ROUTE_ALIAS_CLEAR_DESC, "Clear all aliases");

        zh.put(SUB_ALIAS_ADD_DESC, "添加新的命令别名，用于简化常用命令");
        en.put(SUB_ALIAS_ADD_DESC, "Add a new command alias to shorten frequently used commands");

        zh.put(SUB_ALIAS_ADD_USAGE, "alias add <别名> <命令>");
        en.put(SUB_ALIAS_ADD_USAGE, "alias add <alias> <command>");

        // optionsDesc 是多行文本块，整块一个 id（含缩进，逐字节照抄原注解里的内容）
        zh.put(SUB_ALIAS_ADD_OPTIONS, """
                参数:
                  <别名>   简短的替代名称（建议 2-6 个字符）
                  <命令>   完整的原始命令（支持多词命令）

                示例:
                  alias add bi class info                    # bi → class info
                  alias add ls class list -v                 # ls → class list -v
                  alias add pkg packages                     # pkg → packages
                """);
        en.put(SUB_ALIAS_ADD_OPTIONS, """
                Parameters:
                  <alias>    Short replacement name (2-6 characters recommended)
                  <command>  The full original command (multi-word commands supported)

                Examples:
                  alias add bi class info                    # bi -> class info
                  alias add ls class list -v                 # ls -> class list -v
                  alias add pkg packages                     # pkg -> packages
                """);

        zh.put(SUB_ALIAS_LIST_DESC, "列出所有已定义的命令别名");
        en.put(SUB_ALIAS_LIST_DESC, "List all defined command aliases");

        zh.put(SUB_ALIAS_LIST_OPTIONS, """
                显示所有已保存的别名，包括名称和对应的完整命令。

                示例:
                  alias list                    # 列出所有别名
                  alias                        # 默认就是 list
                """);
        en.put(SUB_ALIAS_LIST_OPTIONS, """
                Show all saved aliases, including their names and full commands.

                Examples:
                  alias list                    # list all aliases
                  alias                        # defaults to list
                """);

        zh.put(SUB_ALIAS_REMOVE_DESC, "删除指定的命令别名");
        en.put(SUB_ALIAS_REMOVE_DESC, "Remove the specified command alias");

        zh.put(SUB_ALIAS_REMOVE_USAGE, "alias remove <别名>");
        en.put(SUB_ALIAS_REMOVE_USAGE, "alias remove <alias>");

        zh.put(SUB_ALIAS_REMOVE_OPTIONS, """
                参数:
                  <别名>   要删除的别名名称

                示例:
                  alias remove bi     # 删除 bi 别名
                  alias rm ls        # 删除 ls 别名
                """);
        en.put(SUB_ALIAS_REMOVE_OPTIONS, """
                Parameters:
                  <alias>   Name of the alias to remove

                Examples:
                  alias remove bi     # remove the bi alias
                  alias rm ls        # remove the ls alias
                """);

        zh.put(SUB_ALIAS_CLEAR_DESC, "清空所有已定义的命令别名");
        en.put(SUB_ALIAS_CLEAR_DESC, "Clear all defined command aliases");

        zh.put(SUB_ALIAS_CLEAR_OPTIONS, """
                清除所有已保存的别名，恢复到初始状态。

                警告: 此操作不可撤销！

                示例:
                  alias clear                   # 清空所有别名
                """);
        en.put(SUB_ALIAS_CLEAR_OPTIONS, """
                Clear all saved aliases and restore the initial state.

                Warning: this action cannot be undone!

                Examples:
                  alias clear                   # clear all aliases
                """);

        zh.put(PARAM_ALIAS_ADD_NAME_DESC, "简短的替代名称（建议 2-6 个字符）");
        en.put(PARAM_ALIAS_ADD_NAME_DESC, "Short replacement name (2-6 characters recommended)");

        zh.put(PARAM_ALIAS_ADD_COMMAND_DESC, "完整的原始命令（支持多词命令）");
        en.put(PARAM_ALIAS_ADD_COMMAND_DESC, "The full original command (multi-word commands supported)");

        zh.put(PARAM_ALIAS_REMOVE_NAME_DESC, "要删除的别名名称");
        en.put(PARAM_ALIAS_REMOVE_NAME_DESC, "Name of the alias to remove");
    }
}
