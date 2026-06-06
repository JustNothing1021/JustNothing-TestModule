package com.justnothing.testmodule.constants;

/**
 * Agent 命令返回结果类型常量
 * <p>
 * 用于标识不同 Agent Handler 返回的 CommandResult 类型，
 * 与 CLI 命令的 @SerializeKeyName 注解保持一致的命名风格。
 */
public final class AgentResultTypes {

    private AgentResultTypes() {}

    // === SharedPreferences 操作 ===
    public static final String SP_LIST = "sp_list";
    public static final String SP_READ = "sp_read";
    public static final String SP_WRITE = "sp_write";

    // === 数据库操作 ===
    public static final String DB_LIST = "db_list";
    public static final String DB_QUERY = "db_query";
    public static final String DB_TABLES = "db_tables";
}
