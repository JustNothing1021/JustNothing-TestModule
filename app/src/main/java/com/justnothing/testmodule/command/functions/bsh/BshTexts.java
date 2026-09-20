package com.justnothing.testmodule.command.functions.bsh;

import com.justnothing.testmodule.command.framework.i18n.CliTexts;

import java.util.Map;

/**
 * bsh 命令族的 CLI 文案（id 常量 + 中英对照）。
 *
 * <p>命名与 id 规范见 {@link CliTexts}。
 * 本类由 {@code CliTexts} 的静态块登记，新增条目只需在这里加常量 + 两行 put。</p>
 *
 * <p>英文允许缺失（只 put 中文），缺失时英文环境回落显示中文 —— 所以翻译可以一条一条补。</p>
 */
public final class BshTexts {

    // ==================== @Cmd（主命令）====================
    public static final String CMD_BSH_DESC = "cmd.bsh.desc";

    // ==================== @CmdRoutes.Route（命令列表里那一行）====================
    public static final String ROUTE_BSH_RUN_CODE_DESC = "route.bsh.run_code.desc";
    public static final String ROUTE_BSH_VARS_DESC = "route.bsh.vars.desc";
    public static final String ROUTE_BSH_CLEAR_DESC = "route.bsh.clear.desc";
    public static final String ROUTE_BSH_SCRIPT_DESC = "route.bsh.script.desc";
    public static final String ROUTE_BSH_SCRIPT_EDIT_DESC = "route.bsh.script.edit.desc";
    public static final String ROUTE_BSH_SCRIPT_LIST_DESC = "route.bsh.script.list.desc";
    public static final String ROUTE_BSH_SCRIPT_SHOW_DESC = "route.bsh.script.show.desc";
    public static final String ROUTE_BSH_SCRIPT_DELETE_DESC = "route.bsh.script.delete.desc";
    public static final String ROUTE_BSH_SCRIPT_RUN_DESC = "route.bsh.script.run.desc";
    public static final String ROUTE_BSH_SCRIPT_IMPORT_DESC = "route.bsh.script.import.desc";
    public static final String ROUTE_BSH_SCRIPT_EXPORT_DESC = "route.bsh.script.export.desc";

    // ==================== @CmdParam（参数说明）====================
    public static final String PARAM_BSH_RUN_CODE_CODE_DESC = "param.bsh.run_code.code.desc";
    public static final String PARAM_BSH_SCRIPT_NAME_DESC = "param.bsh.script.name.desc";
    public static final String PARAM_BSH_SCRIPT_EDIT_NAME_DESC = "param.bsh.script.edit.name.desc";
    public static final String PARAM_BSH_SCRIPT_SHOW_NAME_DESC = "param.bsh.script.show.name.desc";
    public static final String PARAM_BSH_SCRIPT_DELETE_NAME_DESC = "param.bsh.script.delete.name.desc";
    public static final String PARAM_BSH_SCRIPT_RUN_NAME_DESC = "param.bsh.script.run.name.desc";
    public static final String PARAM_BSH_SCRIPT_IMPORT_FILEPATH_DESC = "param.bsh.script.import.filePath.desc";
    public static final String PARAM_BSH_SCRIPT_EXPORT_NAME_DESC = "param.bsh.script.export.name.desc";
    public static final String PARAM_BSH_SCRIPT_EXPORT_EXPORTPATH_DESC = "param.bsh.script.export.exportPath.desc";

    private BshTexts() {
    }

    /**
     * 由 {@code CliTexts} 的静态块调用。必须是 public —— 它在另一个包里。
     * 命名上刻意带 register 而不是「构造时自己注册」：登记动作集中在 CliTexts 一处，
     * 「哪些族登记了」才看得全，漏登记也能被守卫测试发现。
     */
    public static void register(Map<String, String> zh, Map<String, String> en) {
        zh.put(CMD_BSH_DESC, "用BeanShell解释器执行代码");
        en.put(CMD_BSH_DESC, "Execute code with the BeanShell interpreter");

        zh.put(ROUTE_BSH_RUN_CODE_DESC, "执行BeanShell代码");
        en.put(ROUTE_BSH_RUN_CODE_DESC, "Execute BeanShell code");

        zh.put(ROUTE_BSH_VARS_DESC, "显示BeanShell执行器的变量列表");
        en.put(ROUTE_BSH_VARS_DESC, "Show the BeanShell executor's variables");

        zh.put(ROUTE_BSH_CLEAR_DESC, "清空BeanShell执行器的所有变量");
        en.put(ROUTE_BSH_CLEAR_DESC, "Clear all BeanShell executor variables");

        zh.put(ROUTE_BSH_SCRIPT_DESC, "BeanShell脚本管理");
        en.put(ROUTE_BSH_SCRIPT_DESC, "Manage BeanShell scripts");

        zh.put(ROUTE_BSH_SCRIPT_EDIT_DESC, "编辑脚本");
        en.put(ROUTE_BSH_SCRIPT_EDIT_DESC, "Edit a script");

        zh.put(ROUTE_BSH_SCRIPT_LIST_DESC, "列出所有脚本");
        en.put(ROUTE_BSH_SCRIPT_LIST_DESC, "List all scripts");

        zh.put(ROUTE_BSH_SCRIPT_SHOW_DESC, "显示脚本内容");
        en.put(ROUTE_BSH_SCRIPT_SHOW_DESC, "Show script contents");

        zh.put(ROUTE_BSH_SCRIPT_DELETE_DESC, "删除脚本");
        en.put(ROUTE_BSH_SCRIPT_DELETE_DESC, "Delete a script");

        zh.put(ROUTE_BSH_SCRIPT_RUN_DESC, "执行脚本");
        en.put(ROUTE_BSH_SCRIPT_RUN_DESC, "Run a script");

        zh.put(ROUTE_BSH_SCRIPT_IMPORT_DESC, "导入脚本文件");
        en.put(ROUTE_BSH_SCRIPT_IMPORT_DESC, "Import a script file");

        zh.put(ROUTE_BSH_SCRIPT_EXPORT_DESC, "导出脚本文件");
        en.put(ROUTE_BSH_SCRIPT_EXPORT_DESC, "Export a script file");

        zh.put(PARAM_BSH_RUN_CODE_CODE_DESC, "BeanShell代码");
        en.put(PARAM_BSH_RUN_CODE_CODE_DESC, "BeanShell code");

        zh.put(PARAM_BSH_SCRIPT_NAME_DESC, "脚本名称");
        en.put(PARAM_BSH_SCRIPT_NAME_DESC, "Script name");

        zh.put(PARAM_BSH_SCRIPT_EDIT_NAME_DESC, "脚本名称");
        en.put(PARAM_BSH_SCRIPT_EDIT_NAME_DESC, "Script name");

        zh.put(PARAM_BSH_SCRIPT_SHOW_NAME_DESC, "脚本名称");
        en.put(PARAM_BSH_SCRIPT_SHOW_NAME_DESC, "Script name");

        zh.put(PARAM_BSH_SCRIPT_DELETE_NAME_DESC, "脚本名称");
        en.put(PARAM_BSH_SCRIPT_DELETE_NAME_DESC, "Script name");

        zh.put(PARAM_BSH_SCRIPT_RUN_NAME_DESC, "脚本名称");
        en.put(PARAM_BSH_SCRIPT_RUN_NAME_DESC, "Script name");

        zh.put(PARAM_BSH_SCRIPT_IMPORT_FILEPATH_DESC, "导入文件路径");
        en.put(PARAM_BSH_SCRIPT_IMPORT_FILEPATH_DESC, "Import file path");

        zh.put(PARAM_BSH_SCRIPT_EXPORT_NAME_DESC, "脚本名称");
        en.put(PARAM_BSH_SCRIPT_EXPORT_NAME_DESC, "Script name");

        zh.put(PARAM_BSH_SCRIPT_EXPORT_EXPORTPATH_DESC, "导出路径");
        en.put(PARAM_BSH_SCRIPT_EXPORT_EXPORTPATH_DESC, "Export path");
    }
}
