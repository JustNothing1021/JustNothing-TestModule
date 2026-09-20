package com.justnothing.testmodule.command.functions.agent;

import java.util.Map;

/**
 * agent 命令族的 CLI 文案（id 常量 + 中英对照）。
 *
 * <p>命名与 id 规范见 {@link com.justnothing.testmodule.command.framework.i18n.CliTexts}。
 * 本类由 {@code CliTexts} 的静态块登记，新增条目只需在这里加常量 + 两行 put。</p>
 *
 * <p>英文允许缺失（只 put 中文），缺失时英文环境回落显示中文 —— 所以翻译可以一条一条补。</p>
 */
public final class AgentTexts {

    // ==================== @Cmd（主命令）====================
    public static final String CMD_AGENT_DESC = "cmd.agent.desc";

    // ==================== @CmdRoutes.Route（命令列表里那一行）====================
    public static final String ROUTE_AGENT_SP_LIST_DESC = "route.agent.sp-list.desc";
    public static final String ROUTE_AGENT_SP_READ_DESC = "route.agent.sp-read.desc";
    public static final String ROUTE_AGENT_SP_WRITE_DESC = "route.agent.sp-write.desc";
    public static final String ROUTE_AGENT_DB_LIST_DESC = "route.agent.db-list.desc";
    public static final String ROUTE_AGENT_DB_QUERY_DESC = "route.agent.db-query.desc";
    public static final String ROUTE_AGENT_DB_TABLES_DESC = "route.agent.db-tables.desc";
    public static final String ROUTE_AGENT_LIST_DESC = "route.agent.list.desc";
    public static final String ROUTE_AGENT_START_DESC = "route.agent.start.desc";
    public static final String ROUTE_AGENT_STOP_DESC = "route.agent.stop.desc";
    public static final String ROUTE_AGENT_RUN_DESC = "route.agent.run.desc";

    // ==================== @CmdParam（参数说明）====================
    public static final String PARAM_AGENT_SP_LIST_PKG_DESC = "param.agent.sp-list.pkg.desc";

    public static final String PARAM_AGENT_SP_READ_PKG_DESC = "param.agent.sp-read.pkg.desc";
    public static final String PARAM_AGENT_SP_READ_NAME_DESC = "param.agent.sp-read.name.desc";
    public static final String PARAM_AGENT_SP_READ_KEY_DESC = "param.agent.sp-read.key.desc";

    public static final String PARAM_AGENT_SP_WRITE_PKG_DESC = "param.agent.sp-write.pkg.desc";
    public static final String PARAM_AGENT_SP_WRITE_NAME_DESC = "param.agent.sp-write.name.desc";
    public static final String PARAM_AGENT_SP_WRITE_KEY_DESC = "param.agent.sp-write.key.desc";
    public static final String PARAM_AGENT_SP_WRITE_VALUE_DESC = "param.agent.sp-write.value.desc";
    public static final String PARAM_AGENT_SP_WRITE_TYPE_DESC = "param.agent.sp-write.type.desc";

    public static final String PARAM_AGENT_DB_LIST_PKG_DESC = "param.agent.db-list.pkg.desc";

    public static final String PARAM_AGENT_DB_QUERY_PKG_DESC = "param.agent.db-query.pkg.desc";
    public static final String PARAM_AGENT_DB_QUERY_DB_DESC = "param.agent.db-query.db.desc";
    public static final String PARAM_AGENT_DB_QUERY_SQL_DESC = "param.agent.db-query.sql.desc";
    public static final String PARAM_AGENT_DB_QUERY_LIMIT_DESC = "param.agent.db-query.limit.desc";

    public static final String PARAM_AGENT_DB_TABLES_PKG_DESC = "param.agent.db-tables.pkg.desc";
    public static final String PARAM_AGENT_DB_TABLES_DB_DESC = "param.agent.db-tables.db.desc";

    public static final String PARAM_AGENT_START_PKG_DESC = "param.agent.start.pkg.desc";

    public static final String PARAM_AGENT_STOP_PKG_DESC = "param.agent.stop.pkg.desc";

    public static final String PARAM_AGENT_RUN_PKG_DESC = "param.agent.run.pkg.desc";
    public static final String PARAM_AGENT_RUN_COMMAND_DESC = "param.agent.run.command.desc";

    private AgentTexts() {
    }

    /**
     * 由 {@code CliTexts} 的静态块调用。必须是 public —— 它在另一个包里。
     * 命名上刻意带 register 而不是「构造时自己注册」：登记动作集中在 CliTexts 一处，
     * 「哪些族登记了」才看得全，漏登记也能被守卫测试发现。
     */
    public static void register(Map<String, String> zh, Map<String, String> en) {
        zh.put(CMD_AGENT_DESC, "跨应用 InspectionAgent IPC 桥接命令");
        en.put(CMD_AGENT_DESC, "Cross-app InspectionAgent IPC bridge commands");

        zh.put(ROUTE_AGENT_SP_LIST_DESC, "列出目标应用的 SharedPreferences 文件");
        en.put(ROUTE_AGENT_SP_LIST_DESC, "List the target app's SharedPreferences files");

        zh.put(ROUTE_AGENT_SP_READ_DESC, "读取目标应用的 SharedPreferences");
        en.put(ROUTE_AGENT_SP_READ_DESC, "Read the target app's SharedPreferences");

        zh.put(ROUTE_AGENT_SP_WRITE_DESC, "写入目标应用的 SharedPreferences");
        en.put(ROUTE_AGENT_SP_WRITE_DESC, "Write the target app's SharedPreferences");

        zh.put(ROUTE_AGENT_DB_LIST_DESC, "列出目标应用的数据库文件");
        en.put(ROUTE_AGENT_DB_LIST_DESC, "List the target app's database files");

        zh.put(ROUTE_AGENT_DB_QUERY_DESC, "查询目标应用的 SQLite 数据库");
        en.put(ROUTE_AGENT_DB_QUERY_DESC, "Query the target app's SQLite database");

        zh.put(ROUTE_AGENT_DB_TABLES_DESC, "列出目标应用数据库的所有表");
        en.put(ROUTE_AGENT_DB_TABLES_DESC, "List all tables in the target app's database");

        zh.put(ROUTE_AGENT_LIST_DESC, "列出所有在线的 InspectionAgent（自动清理死文件）");
        en.put(ROUTE_AGENT_LIST_DESC, "List all online InspectionAgents (stale files are cleaned up automatically)");

        zh.put(ROUTE_AGENT_START_DESC, "请求启动目标应用的 InspectionAgent");
        en.put(ROUTE_AGENT_START_DESC, "Ask the target app to start its InspectionAgent");

        zh.put(ROUTE_AGENT_STOP_DESC, "停止目标应用的 InspectionAgent（关闭 ServerSocket + 清理文件）");
        en.put(ROUTE_AGENT_STOP_DESC, "Stop the target app's InspectionAgent (close the ServerSocket and clean up files)");

        zh.put(ROUTE_AGENT_RUN_DESC, "在目标应用上代理执行任意主服务命令");
        en.put(ROUTE_AGENT_RUN_DESC, "Run any main-service command on the target app");

        zh.put(PARAM_AGENT_SP_LIST_PKG_DESC, "目标应用包名");
        en.put(PARAM_AGENT_SP_LIST_PKG_DESC, "Package name of the target app");

        zh.put(PARAM_AGENT_SP_READ_PKG_DESC, "目标应用包名");
        en.put(PARAM_AGENT_SP_READ_PKG_DESC, "Package name of the target app");

        zh.put(PARAM_AGENT_SP_READ_NAME_DESC, "SharedPreferences 名称");
        en.put(PARAM_AGENT_SP_READ_NAME_DESC, "SharedPreferences file name");

        zh.put(PARAM_AGENT_SP_READ_KEY_DESC, "键名过滤（可选）");
        en.put(PARAM_AGENT_SP_READ_KEY_DESC, "Key name filter (optional)");

        zh.put(PARAM_AGENT_SP_WRITE_PKG_DESC, "目标应用包名");
        en.put(PARAM_AGENT_SP_WRITE_PKG_DESC, "Package name of the target app");

        zh.put(PARAM_AGENT_SP_WRITE_NAME_DESC, "SharedPreferences 名称");
        en.put(PARAM_AGENT_SP_WRITE_NAME_DESC, "SharedPreferences file name");

        zh.put(PARAM_AGENT_SP_WRITE_KEY_DESC, "键名");
        en.put(PARAM_AGENT_SP_WRITE_KEY_DESC, "Key name");

        zh.put(PARAM_AGENT_SP_WRITE_VALUE_DESC, "值");
        en.put(PARAM_AGENT_SP_WRITE_VALUE_DESC, "Value");

        zh.put(PARAM_AGENT_SP_WRITE_TYPE_DESC, "值类型（可选）");
        en.put(PARAM_AGENT_SP_WRITE_TYPE_DESC, "Value type (optional)");

        zh.put(PARAM_AGENT_DB_LIST_PKG_DESC, "目标应用包名");
        en.put(PARAM_AGENT_DB_LIST_PKG_DESC, "Package name of the target app");

        zh.put(PARAM_AGENT_DB_QUERY_PKG_DESC, "目标应用包名");
        en.put(PARAM_AGENT_DB_QUERY_PKG_DESC, "Package name of the target app");

        zh.put(PARAM_AGENT_DB_QUERY_DB_DESC, "数据库名称");
        en.put(PARAM_AGENT_DB_QUERY_DB_DESC, "Database name");

        zh.put(PARAM_AGENT_DB_QUERY_SQL_DESC, "SQL 查询语句");
        en.put(PARAM_AGENT_DB_QUERY_SQL_DESC, "SQL query statement");

        zh.put(PARAM_AGENT_DB_QUERY_LIMIT_DESC, "结果行数限制");
        en.put(PARAM_AGENT_DB_QUERY_LIMIT_DESC, "Maximum number of result rows");

        zh.put(PARAM_AGENT_DB_TABLES_PKG_DESC, "目标应用包名");
        en.put(PARAM_AGENT_DB_TABLES_PKG_DESC, "Package name of the target app");

        zh.put(PARAM_AGENT_DB_TABLES_DB_DESC, "数据库名称");
        en.put(PARAM_AGENT_DB_TABLES_DB_DESC, "Database name");

        zh.put(PARAM_AGENT_START_PKG_DESC, "目标应用包名");
        en.put(PARAM_AGENT_START_PKG_DESC, "Package name of the target app");

        zh.put(PARAM_AGENT_STOP_PKG_DESC, "目标应用包名");
        en.put(PARAM_AGENT_STOP_PKG_DESC, "Package name of the target app");

        zh.put(PARAM_AGENT_RUN_PKG_DESC, "目标应用包名");
        en.put(PARAM_AGENT_RUN_PKG_DESC, "Package name of the target app");

        zh.put(PARAM_AGENT_RUN_COMMAND_DESC, "要在目标应用上执行的命令（剩余所有参数）");
        en.put(PARAM_AGENT_RUN_COMMAND_DESC, "Command to run on the target app (all remaining arguments)");
    }
}
