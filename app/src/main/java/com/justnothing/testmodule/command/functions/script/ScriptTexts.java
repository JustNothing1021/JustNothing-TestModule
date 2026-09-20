package com.justnothing.testmodule.command.functions.script;

import java.util.Map;

/**
 * script 命令族的 CLI 文案（id 常量 + 中英对照）。
 *
 * <p>命名与 id 规范见 {@link com.justnothing.testmodule.command.framework.i18n.CliTexts}。
 * 本类由 {@code CliTexts} 的静态块登记，新增条目只需在这里加常量 + 两行 put。</p>
 *
 * <p>英文允许缺失（只 put 中文），缺失时英文环境回落显示中文 —— 所以翻译可以一条一条补。</p>
 */
public final class ScriptTexts {

    // ==================== @Cmd（主命令）====================
    public static final String CMD_SCRIPT_DESC = "cmd.script.desc";

    // ==================== @CmdRoutes.Route（命令列表里那一行）====================
    public static final String ROUTE_SCRIPT_CREATE_DESC = "route.script.create.desc";
    public static final String ROUTE_SCRIPT_LIST_DESC = "route.script.list.desc";
    public static final String ROUTE_SCRIPT_VARS_DESC = "route.script.vars.desc";
    public static final String ROUTE_SCRIPT_SHOW_DESC = "route.script.show.desc";
    public static final String ROUTE_SCRIPT_DELETE_DESC = "route.script.delete.desc";
    public static final String ROUTE_SCRIPT_RUN_DESC = "route.script.run.desc";
    public static final String ROUTE_SCRIPT_IMPORT_DESC = "route.script.import.desc";
    public static final String ROUTE_SCRIPT_EXPORT_DESC = "route.script.export.desc";
    public static final String ROUTE_SCRIPT_MANAGE_DESC = "route.script.manage.desc";
    public static final String ROUTE_SCRIPT_INTERACTIVE_DESC = "route.script.interactive.desc";
    public static final String ROUTE_SCRIPT_PERMISSION_GRANT_DESC = "route.script.permission.grant.desc";
    public static final String ROUTE_SCRIPT_PERMISSION_DENY_DESC = "route.script.permission.deny.desc";
    public static final String ROUTE_SCRIPT_PERMISSION_PRESET_DESC = "route.script.permission.preset.desc";
    public static final String ROUTE_SCRIPT_PERMISSION_RESET_DESC = "route.script.permission.reset.desc";
    public static final String ROUTE_SCRIPT_PERMISSION_LIST_DESC = "route.script.permission.list.desc";
    public static final String ROUTE_SCRIPT_PERMISSION_SHOW_CONFIG_DESC = "route.script.permission.show-config.desc";

    // ==================== @SubCommandInfo（帮助正文）====================
    public static final String SUB_SCRIPT_CRUD_DESC = "sub.script.crud.desc";
    public static final String SUB_SCRIPT_MANAGE_DESC = "sub.script.manage.desc";
    public static final String SUB_SCRIPT_EXEC_DESC = "sub.script.exec.desc";
    public static final String SUB_SCRIPT_PERMISSION_DESC = "sub.script.permission.desc";
    public static final String SUB_SCRIPT_PERMISSION_OPTIONS = "sub.script.permission.options";

    // ==================== @CmdParam（参数说明）====================
    public static final String PARAM_SCRIPT_CREATE_NAME_DESC = "param.script.create.name.desc";
    public static final String PARAM_SCRIPT_SHOW_NAME_DESC = "param.script.show.name.desc";
    public static final String PARAM_SCRIPT_DELETE_NAME_DESC = "param.script.delete.name.desc";
    public static final String PARAM_SCRIPT_RUN_NAME_DESC = "param.script.run.name.desc";
    public static final String PARAM_SCRIPT_IMPORT_FILEPATH_DESC = "param.script.import.filePath.desc";
    public static final String PARAM_SCRIPT_EXPORT_NAME_DESC = "param.script.export.name.desc";
    public static final String PARAM_SCRIPT_EXPORT_FILEPATH_DESC = "param.script.export.filePath.desc";
    public static final String PARAM_SCRIPT_PERMISSION_GRANT_PERMISSIONS_DESC = "param.script.permission.grant.permissions.desc";
    public static final String PARAM_SCRIPT_PERMISSION_DENY_PERMISSIONS_DESC = "param.script.permission.deny.permissions.desc";
    public static final String PARAM_SCRIPT_PERMISSION_PRESET_PRESETNAME_DESC = "param.script.permission.preset.presetName.desc";

    private ScriptTexts() {
    }

    /**
     * 由 {@code CliTexts} 的静态块调用。必须是 public —— 它在另一个包里。
     * 命名上刻意带 register 而不是「构造时自己注册」：登记动作集中在 CliTexts 一处，
     * 「哪些族登记了」才看得全，漏登记也能被守卫测试发现。
     */
    public static void register(Map<String, String> zh, Map<String, String> en) {
        zh.put(CMD_SCRIPT_DESC, "JustNothing 脚本解释器 - 执行/管理 Java 脚本");
        en.put(CMD_SCRIPT_DESC, "JustNothing script interpreter - run and manage Java scripts");

        zh.put(ROUTE_SCRIPT_CREATE_DESC, "创建新脚本");
        en.put(ROUTE_SCRIPT_CREATE_DESC, "Create a new script");

        zh.put(ROUTE_SCRIPT_LIST_DESC, "列出所有脚本");
        en.put(ROUTE_SCRIPT_LIST_DESC, "List all scripts");

        zh.put(ROUTE_SCRIPT_VARS_DESC, "列出脚本执行器变量");
        en.put(ROUTE_SCRIPT_VARS_DESC, "List interpreter variables");

        zh.put(ROUTE_SCRIPT_SHOW_DESC, "显示脚本内容");
        en.put(ROUTE_SCRIPT_SHOW_DESC, "Show script content");

        zh.put(ROUTE_SCRIPT_DELETE_DESC, "删除脚本");
        en.put(ROUTE_SCRIPT_DELETE_DESC, "Delete a script");

        zh.put(ROUTE_SCRIPT_RUN_DESC, "执行脚本");
        en.put(ROUTE_SCRIPT_RUN_DESC, "Run a script");

        zh.put(ROUTE_SCRIPT_IMPORT_DESC, "导入脚本文件");
        en.put(ROUTE_SCRIPT_IMPORT_DESC, "Import a script file");

        zh.put(ROUTE_SCRIPT_EXPORT_DESC, "导出脚本文件");
        en.put(ROUTE_SCRIPT_EXPORT_DESC, "Export a script file");

        zh.put(ROUTE_SCRIPT_MANAGE_DESC, "交互式脚本管理器");
        en.put(ROUTE_SCRIPT_MANAGE_DESC, "Interactive script manager");

        zh.put(ROUTE_SCRIPT_INTERACTIVE_DESC, "启动交互REPL执行器");
        en.put(ROUTE_SCRIPT_INTERACTIVE_DESC, "Start the interactive REPL");

        zh.put(ROUTE_SCRIPT_PERMISSION_GRANT_DESC, "授予权限");
        en.put(ROUTE_SCRIPT_PERMISSION_GRANT_DESC, "Grant permissions");

        zh.put(ROUTE_SCRIPT_PERMISSION_DENY_DESC, "拒绝权限");
        en.put(ROUTE_SCRIPT_PERMISSION_DENY_DESC, "Deny permissions");

        zh.put(ROUTE_SCRIPT_PERMISSION_PRESET_DESC, "应用权限预设");
        en.put(ROUTE_SCRIPT_PERMISSION_PRESET_DESC, "Apply a permission preset");

        zh.put(ROUTE_SCRIPT_PERMISSION_RESET_DESC, "重置权限配置");
        en.put(ROUTE_SCRIPT_PERMISSION_RESET_DESC, "Reset the permission configuration");

        zh.put(ROUTE_SCRIPT_PERMISSION_LIST_DESC, "列出所有权限类型");
        en.put(ROUTE_SCRIPT_PERMISSION_LIST_DESC, "List all permission types");

        zh.put(ROUTE_SCRIPT_PERMISSION_SHOW_CONFIG_DESC, "显示当前权限配置");
        en.put(ROUTE_SCRIPT_PERMISSION_SHOW_CONFIG_DESC, "Show the current permission configuration");

        zh.put(SUB_SCRIPT_CRUD_DESC, "脚本 CRUD 操作 - 创建/显示/删除");
        en.put(SUB_SCRIPT_CRUD_DESC, "Script CRUD operations - create, show and delete");

        zh.put(SUB_SCRIPT_MANAGE_DESC, "脚本列表与交互式管理器");
        en.put(SUB_SCRIPT_MANAGE_DESC, "Script listing and the interactive manager");

        zh.put(SUB_SCRIPT_EXEC_DESC, "脚本执行引擎 - run/import/export/interactive");
        en.put(SUB_SCRIPT_EXEC_DESC, "Script execution engine - run/import/export/interactive");

        zh.put(SUB_SCRIPT_PERMISSION_DESC, "管理脚本执行权限配置, 支持授权/拒绝/预设/重置/列表/查看");
        en.put(SUB_SCRIPT_PERMISSION_DESC, "Manage script execution permissions: grant, deny, preset, reset, list and view");

        // optionsDesc 是多行文本块，整块一个 id（含缩进，逐字节照抄原注解里的内容）
        zh.put(SUB_SCRIPT_PERMISSION_OPTIONS, """
                子命令:
                  grant <PERM1,PERM2,...>  - 授予指定权限
                  deny <PERM1,PERM2,...>   - 拒绝指定权限
                  preset <name>            - 应用预设配置
                  reset                    - 重置为默认(无限制)
                  list                     - 列出所有可用权限和预设
                  show-config              - 显示当前权限配置状态
                """);
        en.put(SUB_SCRIPT_PERMISSION_OPTIONS, """
                Subcommands:
                  grant <PERM1,PERM2,...>  - Grant the listed permissions
                  deny <PERM1,PERM2,...>   - Deny the listed permissions
                  preset <name>            - Apply a preset configuration
                  reset                    - Reset to the default (unrestricted)
                  list                     - List all available permissions and presets
                  show-config              - Show the current permission configuration
                """);

        zh.put(PARAM_SCRIPT_CREATE_NAME_DESC, "脚本名称");
        en.put(PARAM_SCRIPT_CREATE_NAME_DESC, "Script name");

        zh.put(PARAM_SCRIPT_SHOW_NAME_DESC, "脚本名称");
        en.put(PARAM_SCRIPT_SHOW_NAME_DESC, "Script name");

        zh.put(PARAM_SCRIPT_DELETE_NAME_DESC, "脚本名称");
        en.put(PARAM_SCRIPT_DELETE_NAME_DESC, "Script name");

        zh.put(PARAM_SCRIPT_RUN_NAME_DESC, "脚本名称");
        en.put(PARAM_SCRIPT_RUN_NAME_DESC, "Script name");

        zh.put(PARAM_SCRIPT_IMPORT_FILEPATH_DESC, "导入文件路径");
        en.put(PARAM_SCRIPT_IMPORT_FILEPATH_DESC, "Path of the file to import");

        zh.put(PARAM_SCRIPT_EXPORT_NAME_DESC, "脚本名称");
        en.put(PARAM_SCRIPT_EXPORT_NAME_DESC, "Script name");

        zh.put(PARAM_SCRIPT_EXPORT_FILEPATH_DESC, "导出路径");
        en.put(PARAM_SCRIPT_EXPORT_FILEPATH_DESC, "Export path");

        zh.put(PARAM_SCRIPT_PERMISSION_GRANT_PERMISSIONS_DESC, "权限列表(逗号分隔)");
        en.put(PARAM_SCRIPT_PERMISSION_GRANT_PERMISSIONS_DESC, "Permission list (comma-separated)");

        zh.put(PARAM_SCRIPT_PERMISSION_DENY_PERMISSIONS_DESC, "权限列表(逗号分隔)");
        en.put(PARAM_SCRIPT_PERMISSION_DENY_PERMISSIONS_DESC, "Permission list (comma-separated)");

        zh.put(PARAM_SCRIPT_PERMISSION_PRESET_PRESETNAME_DESC, "预设名称(sandbox/expression/minimal/full)");
        en.put(PARAM_SCRIPT_PERMISSION_PRESET_PRESETNAME_DESC, "Preset name (sandbox/expression/minimal/full)");
    }
}
