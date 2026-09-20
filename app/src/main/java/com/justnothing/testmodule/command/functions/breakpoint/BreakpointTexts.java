package com.justnothing.testmodule.command.functions.breakpoint;

import java.util.Map;

/**
 * breakpoint 命令族的 CLI 文案（id 常量 + 中英对照）。
 *
 * <p>命名与 id 规范见 {@code CliTexts}。
 * 本类由 {@code CliTexts} 的静态块登记，新增条目只需在这里加常量 + 两行 put。</p>
 *
 * <p>英文允许缺失（只 put 中文），缺失时英文环境回落显示中文 —— 所以翻译可以一条一条补。</p>
 */
public final class BreakpointTexts {

    // ==================== @Cmd（主命令）====================
    public static final String CMD_BREAKPOINT_DESC = "cmd.breakpoint.desc";

    // ==================== @CmdRoutes.Route（命令列表里那一行）====================
    public static final String ROUTE_BREAKPOINT_ADD_DESC = "route.breakpoint.add.desc";
    public static final String ROUTE_BREAKPOINT_LIST_DESC = "route.breakpoint.list.desc";
    public static final String ROUTE_BREAKPOINT_ENABLE_DESC = "route.breakpoint.enable.desc";
    public static final String ROUTE_BREAKPOINT_DISABLE_DESC = "route.breakpoint.disable.desc";
    public static final String ROUTE_BREAKPOINT_REMOVE_DESC = "route.breakpoint.remove.desc";
    public static final String ROUTE_BREAKPOINT_CLEAR_DESC = "route.breakpoint.clear.desc";
    public static final String ROUTE_BREAKPOINT_HITS_DESC = "route.breakpoint.hits.desc";

    // ==================== @SubCommandInfo（帮助正文）====================
    public static final String SUB_BREAKPOINT_MANAGE_DESC = "sub.breakpoint.manage.desc";
    public static final String SUB_BREAKPOINT_QUERY_DESC = "sub.breakpoint.query.desc";

    // ==================== @CmdParam（参数说明）====================
    public static final String PARAM_BREAKPOINT_ADD_CLASSNAME_DESC = "param.breakpoint.add.className.desc";
    public static final String PARAM_BREAKPOINT_ADD_METHODNAME_DESC = "param.breakpoint.add.methodName.desc";
    public static final String PARAM_BREAKPOINT_ADD_SIGNATURE_DESC = "param.breakpoint.add.signature.desc";
    public static final String PARAM_BREAKPOINT_ENABLE_ID_DESC = "param.breakpoint.enable.id.desc";
    public static final String PARAM_BREAKPOINT_DISABLE_ID_DESC = "param.breakpoint.disable.id.desc";
    public static final String PARAM_BREAKPOINT_REMOVE_ID_DESC = "param.breakpoint.remove.id.desc";

    private BreakpointTexts() {
    }

    /**
     * 由 {@code CliTexts} 的静态块调用。必须是 public —— 它在另一个包里。
     * 命名上刻意带 register 而不是「构造时自己注册」：登记动作集中在 CliTexts 一处，
     * 「哪些族登记了」才看得全，漏登记也能被守卫测试发现。
     */
    public static void register(Map<String, String> zh, Map<String, String> en) {
        zh.put(CMD_BREAKPOINT_DESC, "设置和管理断点");
        en.put(CMD_BREAKPOINT_DESC, "Set and manage breakpoints");

        zh.put(ROUTE_BREAKPOINT_ADD_DESC, "添加断点");
        en.put(ROUTE_BREAKPOINT_ADD_DESC, "Add a breakpoint");

        zh.put(ROUTE_BREAKPOINT_LIST_DESC, "列出所有断点");
        en.put(ROUTE_BREAKPOINT_LIST_DESC, "List all breakpoints");

        zh.put(ROUTE_BREAKPOINT_ENABLE_DESC, "启用断点");
        en.put(ROUTE_BREAKPOINT_ENABLE_DESC, "Enable a breakpoint");

        zh.put(ROUTE_BREAKPOINT_DISABLE_DESC, "禁用断点");
        en.put(ROUTE_BREAKPOINT_DISABLE_DESC, "Disable a breakpoint");

        zh.put(ROUTE_BREAKPOINT_REMOVE_DESC, "移除断点");
        en.put(ROUTE_BREAKPOINT_REMOVE_DESC, "Remove a breakpoint");

        zh.put(ROUTE_BREAKPOINT_CLEAR_DESC, "清除所有断点");
        en.put(ROUTE_BREAKPOINT_CLEAR_DESC, "Clear all breakpoints");

        zh.put(ROUTE_BREAKPOINT_HITS_DESC, "显示断点命中统计");
        en.put(ROUTE_BREAKPOINT_HITS_DESC, "Show breakpoint hit statistics");

        zh.put(SUB_BREAKPOINT_MANAGE_DESC, "断点管理命令（添加、启用、禁用、移除、清除）");
        en.put(SUB_BREAKPOINT_MANAGE_DESC, "Breakpoint management: add, enable, disable, remove, clear");

        zh.put(SUB_BREAKPOINT_QUERY_DESC, "断点查询命令（列表、命中统计）");
        en.put(SUB_BREAKPOINT_QUERY_DESC, "Breakpoint queries: list and hit statistics");

        zh.put(PARAM_BREAKPOINT_ADD_CLASSNAME_DESC, "类名");
        en.put(PARAM_BREAKPOINT_ADD_CLASSNAME_DESC, "Class name");

        zh.put(PARAM_BREAKPOINT_ADD_METHODNAME_DESC, "方法名");
        en.put(PARAM_BREAKPOINT_ADD_METHODNAME_DESC, "Method name");

        zh.put(PARAM_BREAKPOINT_ADD_SIGNATURE_DESC, "方法签名");
        en.put(PARAM_BREAKPOINT_ADD_SIGNATURE_DESC, "Method signature");

        zh.put(PARAM_BREAKPOINT_ENABLE_ID_DESC, "断点ID");
        en.put(PARAM_BREAKPOINT_ENABLE_ID_DESC, "Breakpoint ID");

        zh.put(PARAM_BREAKPOINT_DISABLE_ID_DESC, "断点ID");
        en.put(PARAM_BREAKPOINT_DISABLE_ID_DESC, "Breakpoint ID");

        zh.put(PARAM_BREAKPOINT_REMOVE_ID_DESC, "断点ID");
        en.put(PARAM_BREAKPOINT_REMOVE_ID_DESC, "Breakpoint ID");
    }
}
