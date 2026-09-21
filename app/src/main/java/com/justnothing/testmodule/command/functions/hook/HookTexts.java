package com.justnothing.testmodule.command.functions.hook;

import com.justnothing.testmodule.command.framework.i18n.Text;

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

    // ==================== 族内复用输出文案 ====================
    // 判据只看「在本族里出现了两次以上」；只用一次的就地写 Text.zhEn。
    // 跨族复用的标签（「类名: 」这类）在 CliMessages 里，这里不重复。
    // LABEL_SIGNATURE / LABEL_METHOD_NAME / LABEL_STATUS / ERR_UNSUPPORTED_REQUEST_TYPE
    // 这几条在别的命令族里也有同款文案，先放在这里，等统一归位（见本族迁移报告）。

    /** Hook 展示信息里的字段标签（HookInfo 的两处渲染路径共用）。 */
    public static final Text LABEL_METHOD_NAME = Text.zhEn("方法名: ", "Method: ");
    public static final Text LABEL_SIGNATURE = Text.zhEn("签名: ", "Signature: ");
    public static final Text LABEL_CREATE_TIME = Text.zhEn("创建时间: ", "Created: ");
    public static final Text LABEL_CALL_COUNT = Text.zhEn("调用次数: ", "Calls: ");
    public static final Text LABEL_STATUS = Text.zhEn("状态: ", "Status: ");
    public static final Text LABEL_ENABLED = Text.zhEn("启用: ", "Enabled: ");

    /** Hook 的活跃状态取值。 */
    public static final Text STATUS_ACTIVE = Text.zhEn("活跃", "Active");
    public static final Text STATUS_INACTIVE = Text.zhEn("非活跃", "Inactive");

    /** 「启用: 」的取值。 */
    public static final Text VALUE_YES = Text.zhEn("是", "yes");
    public static final Text VALUE_NO = Text.zhEn("否", "no");

    /** 三个阶段各自的代码 / 代码文件标签。 */
    public static final Text LABEL_BEFORE_CODE = Text.zhEn("before阶段代码: ", "Before code: ");
    public static final Text LABEL_BEFORE_CODEBASE = Text.zhEn("before阶段代码文件: ", "Before code file: ");
    public static final Text LABEL_AFTER_CODE = Text.zhEn("after阶段代码: ", "After code: ");
    public static final Text LABEL_AFTER_CODEBASE = Text.zhEn("after阶段代码文件: ", "After code file: ");
    public static final Text LABEL_REPLACE_CODE = Text.zhEn("replace代码: ", "Replace code: ");
    public static final Text LABEL_REPLACE_CODEBASE = Text.zhEn("replace代码文件: ", "Replace code file: ");

    /** 「Hook不存在: 」——remove / info / enable / disable / output 五个入口共用。 */
    public static final Text ERR_HOOK_NOT_FOUND = Text.zhEn("Hook不存在: ", "Hook not found: ");
    /** 带 ID 的「未找到 Hook」：query 侧当异常抛，manage 侧当结果消息回。 */
    public static final Text ERR_HOOK_ID_NOT_FOUND = Text.zhEn("未找到Hook (ID: %s)", "Hook not found (ID: %s)");
    /** 「Hook代码验证失败」：行内打印补一个冒号，异常详情提示句直接用。 */
    public static final Text ERR_HOOK_CODE_VALIDATION_FAILED =
            Text.zhEn("Hook代码验证失败", "Hook code validation failed");
    /** 「Hook添加失败」：同上，一处补冒号、一处不加。 */
    public static final Text ERR_HOOK_ADD_FAILED = Text.zhEn("Hook添加失败", "Failed to add hook");
    /** codebase 文件加载不出来（before / after / replace 三处共用）。 */
    public static final Text ERR_CODEBASE_LOAD_FAILED =
            Text.zhEn("无法加载codebase文件: %s", "Failed to load codebase file: %s");
    /** 异常没有消息时当错误信息用的兜底值。 */
    public static final Text VALUE_NO_DETAILS = Text.zhEn("没有详细信息", "no details");
    /** 请求对象和处理器对不上时的护栏文案（query / manage 两个分发入口都用）。 */
    public static final Text ERR_UNSUPPORTED_REQUEST_TYPE =
            Text.zhEn("不支持的请求类型: %s", "Unsupported request type: %s");

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
