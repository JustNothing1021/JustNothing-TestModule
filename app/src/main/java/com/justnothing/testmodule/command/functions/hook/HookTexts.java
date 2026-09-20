package com.justnothing.testmodule.command.functions.hook;

import java.util.Map;

/**
 * hook 命令族的 CLI 文案（id 常量 + 中英对照）。
 *
 * <p>命名与 id 规范见 {@code CliTexts}。
 * 本类由 {@code CliTexts} 的静态块登记，新增条目只需在这里加常量 + 两行 put。</p>
 *
 * <p>英文允许缺失（只 put 中文），缺失时英文环境回落显示中文 —— 所以翻译可以一条一条补。</p>
 */
public final class HookTexts {

    // ==================== @Cmd（主命令）====================
    public static final String CMD_HOOK_DESC = "cmd.hook.desc";

    // ==================== @CmdRoutes.Route（命令列表里那一行）====================
    public static final String ROUTE_HOOK_ADD_DESC = "route.hook.add.desc";
    public static final String ROUTE_HOOK_REMOVE_DESC = "route.hook.remove.desc";
    public static final String ROUTE_HOOK_LIST_DESC = "route.hook.list.desc";
    public static final String ROUTE_HOOK_INFO_DESC = "route.hook.info.desc";
    public static final String ROUTE_HOOK_OUTPUT_DESC = "route.hook.output.desc";
    public static final String ROUTE_HOOK_ENABLE_DESC = "route.hook.enable.desc";
    public static final String ROUTE_HOOK_DISABLE_DESC = "route.hook.disable.desc";
    public static final String ROUTE_HOOK_CLEAR_DESC = "route.hook.clear.desc";

    // ==================== @CmdParam（参数说明）====================
    public static final String PARAM_HOOK_ADD_CLASSNAME_DESC = "param.hook.add.className.desc";
    public static final String PARAM_HOOK_ADD_METHODNAME_DESC = "param.hook.add.methodName.desc";
    public static final String PARAM_HOOK_ADD_SIG_DESC = "param.hook.add.sig.desc";
    public static final String PARAM_HOOK_ADD_BEFORE_CODE_DESC = "param.hook.add.before-code.desc";
    public static final String PARAM_HOOK_ADD_BEFORE_CODEBASE_DESC = "param.hook.add.before-codebase.desc";
    public static final String PARAM_HOOK_ADD_AFTER_CODE_DESC = "param.hook.add.after-code.desc";
    public static final String PARAM_HOOK_ADD_AFTER_CODEBASE_DESC = "param.hook.add.after-codebase.desc";
    public static final String PARAM_HOOK_ADD_REPLACE_CODE_DESC = "param.hook.add.replace-code.desc";
    public static final String PARAM_HOOK_ADD_REPLACE_CODEBASE_DESC = "param.hook.add.replace-codebase.desc";

    public static final String PARAM_HOOK_REMOVE_HOOKID_DESC = "param.hook.remove.hookId.desc";
    public static final String PARAM_HOOK_ENABLE_HOOKID_DESC = "param.hook.enable.hookId.desc";
    public static final String PARAM_HOOK_DISABLE_HOOKID_DESC = "param.hook.disable.hookId.desc";
    public static final String PARAM_HOOK_INFO_HOOKID_DESC = "param.hook.info.hookId.desc";
    public static final String PARAM_HOOK_OUTPUT_HOOKID_DESC = "param.hook.output.hookId.desc";
    public static final String PARAM_HOOK_OUTPUT_COUNT_DESC = "param.hook.output.count.desc";

    private HookTexts() {
    }

    /**
     * 由 {@code CliTexts} 的静态块调用。必须是 public —— 它在另一个包里。
     * 命名上刻意带 register 而不是「构造时自己注册」：登记动作集中在 CliTexts 一处，
     * 「哪些族登记了」才看得全，漏登记也能被守卫测试发现。
     */
    public static void register(Map<String, String> zh, Map<String, String> en) {
        zh.put(CMD_HOOK_DESC, "动态Hook注入器, 通过脚本实现Hook功能");
        en.put(CMD_HOOK_DESC, "Dynamic hook injector that hooks methods via scripts");

        zh.put(ROUTE_HOOK_ADD_DESC, "添加Hook");
        en.put(ROUTE_HOOK_ADD_DESC, "Add a hook");

        zh.put(ROUTE_HOOK_REMOVE_DESC, "移除指定Hook");
        en.put(ROUTE_HOOK_REMOVE_DESC, "Remove the specified hook");

        zh.put(ROUTE_HOOK_LIST_DESC, "列出所有Hook");
        en.put(ROUTE_HOOK_LIST_DESC, "List all hooks");

        zh.put(ROUTE_HOOK_INFO_DESC, "显示Hook详细信息");
        en.put(ROUTE_HOOK_INFO_DESC, "Show hook details");

        zh.put(ROUTE_HOOK_OUTPUT_DESC, "获取Hook输出");
        en.put(ROUTE_HOOK_OUTPUT_DESC, "Get hook output");

        zh.put(ROUTE_HOOK_ENABLE_DESC, "启用Hook");
        en.put(ROUTE_HOOK_ENABLE_DESC, "Enable a hook");

        zh.put(ROUTE_HOOK_DISABLE_DESC, "禁用Hook");
        en.put(ROUTE_HOOK_DISABLE_DESC, "Disable a hook");

        zh.put(ROUTE_HOOK_CLEAR_DESC, "清除所有Hook");
        en.put(ROUTE_HOOK_CLEAR_DESC, "Clear all hooks");

        zh.put(PARAM_HOOK_ADD_CLASSNAME_DESC, "目标类名");
        en.put(PARAM_HOOK_ADD_CLASSNAME_DESC, "Target class name");

        zh.put(PARAM_HOOK_ADD_METHODNAME_DESC, "目标方法名");
        en.put(PARAM_HOOK_ADD_METHODNAME_DESC, "Target method name");

        zh.put(PARAM_HOOK_ADD_SIG_DESC, "方法签名");
        en.put(PARAM_HOOK_ADD_SIG_DESC, "Method signature");

        zh.put(PARAM_HOOK_ADD_BEFORE_CODE_DESC, "before阶段内联代码");
        en.put(PARAM_HOOK_ADD_BEFORE_CODE_DESC, "Inline code for the before phase");

        zh.put(PARAM_HOOK_ADD_BEFORE_CODEBASE_DESC, "before阶段代码文件");
        en.put(PARAM_HOOK_ADD_BEFORE_CODEBASE_DESC, "Code file for the before phase");

        zh.put(PARAM_HOOK_ADD_AFTER_CODE_DESC, "after阶段内联代码");
        en.put(PARAM_HOOK_ADD_AFTER_CODE_DESC, "Inline code for the after phase");

        zh.put(PARAM_HOOK_ADD_AFTER_CODEBASE_DESC, "after阶段代码文件");
        en.put(PARAM_HOOK_ADD_AFTER_CODEBASE_DESC, "Code file for the after phase");

        zh.put(PARAM_HOOK_ADD_REPLACE_CODE_DESC, "replace阶段内联代码");
        en.put(PARAM_HOOK_ADD_REPLACE_CODE_DESC, "Inline code for the replace phase");

        zh.put(PARAM_HOOK_ADD_REPLACE_CODEBASE_DESC, "replace阶段代码文件");
        en.put(PARAM_HOOK_ADD_REPLACE_CODEBASE_DESC, "Code file for the replace phase");

        zh.put(PARAM_HOOK_REMOVE_HOOKID_DESC, "Hook ID");
        en.put(PARAM_HOOK_REMOVE_HOOKID_DESC, "Hook ID");

        zh.put(PARAM_HOOK_ENABLE_HOOKID_DESC, "Hook ID");
        en.put(PARAM_HOOK_ENABLE_HOOKID_DESC, "Hook ID");

        zh.put(PARAM_HOOK_DISABLE_HOOKID_DESC, "Hook ID");
        en.put(PARAM_HOOK_DISABLE_HOOKID_DESC, "Hook ID");

        zh.put(PARAM_HOOK_INFO_HOOKID_DESC, "Hook ID");
        en.put(PARAM_HOOK_INFO_HOOKID_DESC, "Hook ID");

        zh.put(PARAM_HOOK_OUTPUT_HOOKID_DESC, "Hook ID");
        en.put(PARAM_HOOK_OUTPUT_HOOKID_DESC, "Hook ID");

        zh.put(PARAM_HOOK_OUTPUT_COUNT_DESC, "输出条数");
        en.put(PARAM_HOOK_OUTPUT_COUNT_DESC, "Number of output entries");
    }
}
