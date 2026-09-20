package com.justnothing.testmodule.command.functions.network;

import java.util.Map;

/**
 * network 命令族的 CLI 文案（id 常量 + 中英对照）。
 *
 * <p>命名与 id 规范见 {@link com.justnothing.testmodule.command.framework.i18n.CliTexts}。
 * 本类由 {@code CliTexts} 的静态块登记，新增条目只需在这里加常量 + 两行 put。</p>
 *
 * <p>英文允许缺失（只 put 中文），缺失时英文环境回落显示中文 —— 所以翻译可以一条一条补。</p>
 */
public final class NetworkTexts {

    // ==================== @Cmd（主命令）====================
    public static final String CMD_NETWORK_DESC = "cmd.network.desc";

    // ==================== @CmdRoutes.Route（命令列表里那一行）====================
    public static final String ROUTE_NETWORK_INTERCEPT_DESC = "route.network.intercept.desc";
    public static final String ROUTE_NETWORK_RECORD_DESC = "route.network.record.desc";
    public static final String ROUTE_NETWORK_STATUS_DESC = "route.network.status.desc";
    public static final String ROUTE_NETWORK_LIST_DESC = "route.network.list.desc";
    public static final String ROUTE_NETWORK_INFO_DESC = "route.network.info.desc";
    public static final String ROUTE_NETWORK_FILTER_DESC = "route.network.filter.desc";
    public static final String ROUTE_NETWORK_MOCK_DESC = "route.network.mock.desc";
    public static final String ROUTE_NETWORK_HOOK_DESC = "route.network.hook.desc";
    public static final String ROUTE_NETWORK_WATCH_DESC = "route.network.watch.desc";
    public static final String ROUTE_NETWORK_EXPORT_DESC = "route.network.export.desc";
    public static final String ROUTE_NETWORK_CLEAR_DESC = "route.network.clear.desc";
    public static final String ROUTE_NETWORK_SHUTDOWN_DESC = "route.network.shutdown.desc";

    // ==================== @CmdParam（参数说明）====================
    public static final String PARAM_NETWORK_INTERCEPT_ENABLE_DESC = "param.network.intercept.enable.desc";
    public static final String PARAM_NETWORK_RECORD_ENABLE_DESC = "param.network.record.enable.desc";

    public static final String PARAM_NETWORK_LIST_METHOD_DESC = "param.network.list.method.desc";
    public static final String PARAM_NETWORK_LIST_HOST_DESC = "param.network.list.host.desc";
    public static final String PARAM_NETWORK_LIST_STATUS_DESC = "param.network.list.status.desc";
    public static final String PARAM_NETWORK_LIST_LIMIT_DESC = "param.network.list.limit.desc";

    public static final String PARAM_NETWORK_INFO_ID_DESC = "param.network.info.id.desc";

    public static final String PARAM_NETWORK_FILTER_HOST_DESC = "param.network.filter.host.desc";

    public static final String PARAM_NETWORK_MOCK_SUBCOMMAND_DESC = "param.network.mock.subCommand.desc";
    public static final String PARAM_NETWORK_MOCK_PATTERN_DESC = "param.network.mock.pattern.desc";
    public static final String PARAM_NETWORK_MOCK_RESPONSE_DESC = "param.network.mock.response.desc";
    public static final String PARAM_NETWORK_MOCK_STATUSCODE_DESC = "param.network.mock.statusCode.desc";
    public static final String PARAM_NETWORK_MOCK_HEADERNAME_DESC = "param.network.mock.headerName.desc";
    public static final String PARAM_NETWORK_MOCK_HEADERVALUE_DESC = "param.network.mock.headerValue.desc";

    public static final String PARAM_NETWORK_HOOK_SUBCOMMAND_DESC = "param.network.hook.subCommand.desc";

    public static final String PARAM_NETWORK_EXPORT_FILEPATH_DESC = "param.network.export.filePath.desc";

    private NetworkTexts() {
    }

    /**
     * 由 {@code CliTexts} 的静态块调用。必须是 public —— 它在另一个包里。
     * 命名上刻意带 register 而不是「构造时自己注册」：登记动作集中在 CliTexts 一处，
     * 「哪些族登记了」才看得全，漏登记也能被守卫测试发现。
     */
    public static void register(Map<String, String> zh, Map<String, String> en) {
        zh.put(CMD_NETWORK_DESC, "网络请求监控和调试工具");
        en.put(CMD_NETWORK_DESC, "Network request monitoring and debugging toolkit");

        zh.put(ROUTE_NETWORK_INTERCEPT_DESC, "开启/关闭网络拦截");
        en.put(ROUTE_NETWORK_INTERCEPT_DESC, "Enable or disable network interception");

        zh.put(ROUTE_NETWORK_RECORD_DESC, "开启/关闭请求记录");
        en.put(ROUTE_NETWORK_RECORD_DESC, "Enable or disable request recording");

        zh.put(ROUTE_NETWORK_STATUS_DESC, "显示当前状态");
        en.put(ROUTE_NETWORK_STATUS_DESC, "Show the current status");

        zh.put(ROUTE_NETWORK_LIST_DESC, "列出请求记录");
        en.put(ROUTE_NETWORK_LIST_DESC, "List recorded requests");

        zh.put(ROUTE_NETWORK_INFO_DESC, "查看请求详情");
        en.put(ROUTE_NETWORK_INFO_DESC, "Show details of a request");

        zh.put(ROUTE_NETWORK_FILTER_DESC, "过滤特定主机的请求");
        en.put(ROUTE_NETWORK_FILTER_DESC, "Filter requests by host");

        zh.put(ROUTE_NETWORK_MOCK_DESC, "Mock 规则管理");
        en.put(ROUTE_NETWORK_MOCK_DESC, "Manage mock rules");

        zh.put(ROUTE_NETWORK_HOOK_DESC, "Hook 管理");
        en.put(ROUTE_NETWORK_HOOK_DESC, "Manage hooks");

        zh.put(ROUTE_NETWORK_WATCH_DESC, "实时监控");
        en.put(ROUTE_NETWORK_WATCH_DESC, "Watch traffic in real time");

        zh.put(ROUTE_NETWORK_EXPORT_DESC, "导出请求记录");
        en.put(ROUTE_NETWORK_EXPORT_DESC, "Export recorded requests");

        zh.put(ROUTE_NETWORK_CLEAR_DESC, "清除请求记录");
        en.put(ROUTE_NETWORK_CLEAR_DESC, "Clear recorded requests");

        zh.put(ROUTE_NETWORK_SHUTDOWN_DESC, "关闭网络监控");
        en.put(ROUTE_NETWORK_SHUTDOWN_DESC, "Shut down network monitoring");

        zh.put(PARAM_NETWORK_INTERCEPT_ENABLE_DESC, "开启或关闭拦截");
        en.put(PARAM_NETWORK_INTERCEPT_ENABLE_DESC, "Enable or disable interception");

        zh.put(PARAM_NETWORK_RECORD_ENABLE_DESC, "开启或关闭记录");
        en.put(PARAM_NETWORK_RECORD_ENABLE_DESC, "Enable or disable recording");

        zh.put(PARAM_NETWORK_LIST_METHOD_DESC, "按请求方法过滤 (GET/POST/...)");
        en.put(PARAM_NETWORK_LIST_METHOD_DESC, "Filter by request method (GET/POST/...)");

        zh.put(PARAM_NETWORK_LIST_HOST_DESC, "按主机过滤");
        en.put(PARAM_NETWORK_LIST_HOST_DESC, "Filter by host");

        zh.put(PARAM_NETWORK_LIST_STATUS_DESC, "按状态码过滤 (200, 404, 5xx)");
        en.put(PARAM_NETWORK_LIST_STATUS_DESC, "Filter by status code (200, 404, 5xx)");

        zh.put(PARAM_NETWORK_LIST_LIMIT_DESC, "限制显示数量");
        en.put(PARAM_NETWORK_LIST_LIMIT_DESC, "Limit how many entries are shown");

        zh.put(PARAM_NETWORK_INFO_ID_DESC, "请求 ID");
        en.put(PARAM_NETWORK_INFO_ID_DESC, "Request ID");

        zh.put(PARAM_NETWORK_FILTER_HOST_DESC, "主机名");
        en.put(PARAM_NETWORK_FILTER_HOST_DESC, "Host name");

        zh.put(PARAM_NETWORK_MOCK_SUBCOMMAND_DESC, "子命令 (add/header/remove/list/clear)");
        en.put(PARAM_NETWORK_MOCK_SUBCOMMAND_DESC, "Subcommand (add/header/remove/list/clear)");

        zh.put(PARAM_NETWORK_MOCK_PATTERN_DESC, "匹配模式");
        en.put(PARAM_NETWORK_MOCK_PATTERN_DESC, "Match pattern");

        zh.put(PARAM_NETWORK_MOCK_RESPONSE_DESC, "响应内容");
        en.put(PARAM_NETWORK_MOCK_RESPONSE_DESC, "Response body");

        zh.put(PARAM_NETWORK_MOCK_STATUSCODE_DESC, "状态码");
        en.put(PARAM_NETWORK_MOCK_STATUSCODE_DESC, "Status code");

        zh.put(PARAM_NETWORK_MOCK_HEADERNAME_DESC, "头部名称");
        en.put(PARAM_NETWORK_MOCK_HEADERNAME_DESC, "Header name");

        zh.put(PARAM_NETWORK_MOCK_HEADERVALUE_DESC, "头部值");
        en.put(PARAM_NETWORK_MOCK_HEADERVALUE_DESC, "Header value");

        zh.put(PARAM_NETWORK_HOOK_SUBCOMMAND_DESC, "子命令 (add/remove/list/clear)");
        en.put(PARAM_NETWORK_HOOK_SUBCOMMAND_DESC, "Subcommand (add/remove/list/clear)");

        zh.put(PARAM_NETWORK_EXPORT_FILEPATH_DESC, "导出文件路径");
        en.put(PARAM_NETWORK_EXPORT_FILEPATH_DESC, "Path of the exported file");
    }
}
