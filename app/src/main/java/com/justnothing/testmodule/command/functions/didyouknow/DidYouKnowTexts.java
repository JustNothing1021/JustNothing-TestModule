package com.justnothing.testmodule.command.functions.didyouknow;

import java.util.Map;

/**
 * did-you-know 命令族的 CLI 文案（id 常量 + 中英对照）。
 *
 * <p>命名与 id 规范见 {@link com.justnothing.testmodule.command.framework.i18n.CliTexts}。
 * 本类由 {@code CliTexts} 的静态块登记，新增条目只需在这里加常量 + 两行 put。</p>
 *
 * <p>英文允许缺失（只 put 中文），缺失时英文环境回落显示中文 —— 所以翻译可以一条一条补。</p>
 *
 * <p>该族的路由 {@code path} 为空（子命令即主命令本身），故 id 里省掉空的那一段，
 * 写作 {@code cmd.did-you-know.*} / {@code route.did-you-know.*} / {@code sub.did-you-know.*} /
 * {@code param.did-you-know.*}。</p>
 */
public final class DidYouKnowTexts {

    // ==================== @Cmd（主命令）====================
    public static final String CMD_DID_YOU_KNOW_DESC = "cmd.did-you-know.desc";

    // ==================== @CmdRoutes.Route（命令列表里那一行）====================
    public static final String ROUTE_DID_YOU_KNOW_DESC = "route.did-you-know.desc";

    // ==================== @SubCommandInfo（帮助正文）====================
    public static final String SUB_DID_YOU_KNOW_DESC = "sub.did-you-know.desc";
    public static final String SUB_DID_YOU_KNOW_OPTIONS = "sub.did-you-know.options";

    // ==================== @CmdParam（参数说明）====================
    public static final String PARAM_DID_YOU_KNOW_ID_DESC = "param.did-you-know.id.desc";
    public static final String PARAM_DID_YOU_KNOW_LIST_DESC = "param.did-you-know.list.desc";
    public static final String PARAM_DID_YOU_KNOW_COUNT_DESC = "param.did-you-know.count.desc";
    public static final String PARAM_DID_YOU_KNOW_SEARCH_DESC = "param.did-you-know.search.desc";
    public static final String PARAM_DID_YOU_KNOW_SPECIAL_DESC = "param.did-you-know.special.desc";
    public static final String PARAM_DID_YOU_KNOW_SPECIAL_LIST_DESC = "param.did-you-know.special-list.desc";
    public static final String PARAM_DID_YOU_KNOW_HELP_DESC = "param.did-you-know.help.desc";

    private DidYouKnowTexts() {
    }

    /**
     * 由 {@code CliTexts} 的静态块调用。必须是 public —— 它在另一个包里。
     */
    public static void register(Map<String, String> zh, Map<String, String> en) {
        zh.put(CMD_DID_YOU_KNOW_DESC, "你知道吗？显示有趣的冷知识和彩蛋");
        en.put(CMD_DID_YOU_KNOW_DESC, "\"Did you know?\" — interesting trivia and easter eggs");

        zh.put(ROUTE_DID_YOU_KNOW_DESC, "显示一条随机的'你知道吗'提示");
        en.put(ROUTE_DID_YOU_KNOW_DESC, "Show a random \"did you know\" tip");

        zh.put(SUB_DID_YOU_KNOW_DESC, "显示\"你知道吗\"冷知识提示");
        en.put(SUB_DID_YOU_KNOW_DESC, "Show \"did you know\" trivia tips");

        // optionsDesc 是多行文本块，整块一个 id（含缩进，逐字节照抄原注解里的内容）
        zh.put(SUB_DID_YOU_KNOW_OPTIONS, """
                选项:
                  -i, --id <N>       显示第 N 条提示
                  -l, --list         列出所有提示（带编号）
                  -c, --count        显示统计信息
                  -s, --search <K>   搜索包含关键词的提示
                      --special      显示今日特殊提示（生日/节日）
                      --special-list 列出所有特殊提示
                  -h, --help         显示帮助信息
                """);
        en.put(SUB_DID_YOU_KNOW_OPTIONS, """
                Options:
                  -i, --id <N>       Show the tip with index N
                  -l, --list         List all tips (with numbers)
                  -c, --count        Show statistics
                  -s, --search <K>   Search tips containing a keyword
                      --special      Show today's special tip (birthday/holiday)
                      --special-list List all special tips
                  -h, --help         Show help
                """);

        zh.put(PARAM_DID_YOU_KNOW_ID_DESC, "显示指定序号的提示");
        en.put(PARAM_DID_YOU_KNOW_ID_DESC, "Show the tip with the given index");

        zh.put(PARAM_DID_YOU_KNOW_LIST_DESC, "列出所有提示（带编号）");
        en.put(PARAM_DID_YOU_KNOW_LIST_DESC, "List all tips (with numbers)");

        zh.put(PARAM_DID_YOU_KNOW_COUNT_DESC, "显示提示统计信息");
        en.put(PARAM_DID_YOU_KNOW_COUNT_DESC, "Show tip statistics");

        zh.put(PARAM_DID_YOU_KNOW_SEARCH_DESC, "搜索包含关键词的提示");
        en.put(PARAM_DID_YOU_KNOW_SEARCH_DESC, "Search tips containing a keyword");

        zh.put(PARAM_DID_YOU_KNOW_SPECIAL_DESC, "显示今日特殊提示（生日/节日等）");
        en.put(PARAM_DID_YOU_KNOW_SPECIAL_DESC, "Show today's special tip (birthday/holiday, etc.)");

        zh.put(PARAM_DID_YOU_KNOW_SPECIAL_LIST_DESC, "列出所有特殊提示（节日/生日等）");
        en.put(PARAM_DID_YOU_KNOW_SPECIAL_LIST_DESC, "List all special tips (holidays/birthdays, etc.)");

        zh.put(PARAM_DID_YOU_KNOW_HELP_DESC, "显示帮助信息");
        en.put(PARAM_DID_YOU_KNOW_HELP_DESC, "Show help information");
    }
}
