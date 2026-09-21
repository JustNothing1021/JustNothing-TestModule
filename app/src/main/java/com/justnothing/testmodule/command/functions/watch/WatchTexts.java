package com.justnothing.testmodule.command.functions.watch;

import com.justnothing.testmodule.command.framework.i18n.CliTexts;
import com.justnothing.testmodule.command.framework.i18n.Text;

import java.util.Map;

/**
 * watch 命令族的 CLI 文案（id 常量 + 中英对照）。
 *
 * <p>命名与 id 规范见 {@link CliTexts}。
 * 本类由 {@code CliTexts} 的静态块登记，新增条目只需在这里加常量 + 两行 put。</p>
 *
 * <p>英文允许缺失（只 put 中文），缺失时英文环境回落显示中文 —— 所以翻译可以一条一条补。</p>
 */
public final class WatchTexts {

    // ==================== @Cmd（主命令）====================
    public static final String CMD_WATCH_DESC = "cmd.watch.desc";

    // ==================== @CmdRoutes.Route（命令列表里那一行）====================
    public static final String ROUTE_WATCH_ADD_DESC = "route.watch.add.desc";
    public static final String ROUTE_WATCH_LIST_DESC = "route.watch.list.desc";
    public static final String ROUTE_WATCH_STOP_DESC = "route.watch.stop.desc";
    public static final String ROUTE_WATCH_CLEAR_DESC = "route.watch.clear.desc";
    public static final String ROUTE_WATCH_OUTPUT_DESC = "route.watch.output.desc";

    // ==================== @SubCommandInfo（帮助正文）====================
    public static final String SUB_WATCH_ADD_DESC = "sub.watch.add.desc";
    public static final String SUB_WATCH_LIST_DESC = "sub.watch.list.desc";
    public static final String SUB_WATCH_STOP_DESC = "sub.watch.stop.desc";
    public static final String SUB_WATCH_CLEAR_DESC = "sub.watch.clear.desc";
    public static final String SUB_WATCH_OUTPUT_DESC = "sub.watch.output.desc";

    // ==================== @CmdParam（参数说明）====================
    public static final String PARAM_WATCH_ADD_TARGETTYPE_DESC = "param.watch.add.targetType.desc";
    public static final String PARAM_WATCH_ADD_CLASSNAME_DESC = "param.watch.add.className.desc";
    public static final String PARAM_WATCH_ADD_MEMBERNAME_DESC = "param.watch.add.memberName.desc";
    public static final String PARAM_WATCH_ADD_SIGNATURE_DESC = "param.watch.add.signature.desc";
    public static final String PARAM_WATCH_ADD_INTERVAL_DESC = "param.watch.add.interval.desc";
    public static final String PARAM_WATCH_STOP_WATCHID_DESC = "param.watch.stop.watchId.desc";
    public static final String PARAM_WATCH_OUTPUT_TARGET_DESC = "param.watch.output.target.desc";
    public static final String PARAM_WATCH_OUTPUT_LIMIT_DESC = "param.watch.output.limit.desc";

    // ==================== 输出文案（族内复用）====================
    // 「参数不足」「未知类型: %s」「类: 」在本族里也出现多次，但它们在别的族同样在用，
    // 所以提到 CliMessages 了，这里不再各存一份。
    public static final Text HINT_VIEW_OUTPUT = Text.zhEn(
            "提示: 使用 'watch output %s' 查看输出",
            "Hint: run 'watch output %s' to view the output");
    public static final Text LABEL_INTERVAL = Text.zhEn("间隔: ", "Interval: ");

    private WatchTexts() {
    }

    /**
     * 由 {@code CliTexts} 的静态块调用。必须是 public —— 它在另一个包里。
     * 命名上刻意带 register 而不是「构造时自己注册」：登记动作集中在 CliTexts 一处，
     * 「哪些族登记了」才看得全，漏登记也能被守卫测试发现。
     */
    public static void register(Map<String, String> zh, Map<String, String> en) {
        zh.put(CMD_WATCH_DESC, "监控字段或方法的变化, 非阻塞执行.");
        en.put(CMD_WATCH_DESC, "Monitor field and method changes, non-blocking");

        zh.put(ROUTE_WATCH_ADD_DESC, "添加字段或方法监控任务");
        en.put(ROUTE_WATCH_ADD_DESC, "Add a field or method watch task");

        zh.put(ROUTE_WATCH_LIST_DESC, "列出所有监控任务");
        en.put(ROUTE_WATCH_LIST_DESC, "List all watch tasks");

        zh.put(ROUTE_WATCH_STOP_DESC, "停止指定的监控任务");
        en.put(ROUTE_WATCH_STOP_DESC, "Stop the given watch task");

        zh.put(ROUTE_WATCH_CLEAR_DESC, "清除所有监控任务");
        en.put(ROUTE_WATCH_CLEAR_DESC, "Clear all watch tasks");

        zh.put(ROUTE_WATCH_OUTPUT_DESC, "获取监控任务的输出");
        en.put(ROUTE_WATCH_OUTPUT_DESC, "Get the output of a watch task");

        zh.put(SUB_WATCH_ADD_DESC, "添加字段或方法监控任务");
        en.put(SUB_WATCH_ADD_DESC, "Add a field or method watch task");

        zh.put(SUB_WATCH_LIST_DESC, "列出所有监控任务");
        en.put(SUB_WATCH_LIST_DESC, "List all watch tasks");

        zh.put(SUB_WATCH_STOP_DESC, "停止指定的监控任务");
        en.put(SUB_WATCH_STOP_DESC, "Stop the given watch task");

        zh.put(SUB_WATCH_CLEAR_DESC, "清除所有监控任务");
        en.put(SUB_WATCH_CLEAR_DESC, "Clear all watch tasks");

        zh.put(SUB_WATCH_OUTPUT_DESC, "获取监控任务的输出");
        en.put(SUB_WATCH_OUTPUT_DESC, "Get the output of a watch task");

        zh.put(PARAM_WATCH_ADD_TARGETTYPE_DESC, "监控类型 (field/method)");
        en.put(PARAM_WATCH_ADD_TARGETTYPE_DESC, "Watch target type (field/method)");

        zh.put(PARAM_WATCH_ADD_CLASSNAME_DESC, "类名");
        en.put(PARAM_WATCH_ADD_CLASSNAME_DESC, "Class name");

        zh.put(PARAM_WATCH_ADD_MEMBERNAME_DESC, "成员名（字段名或方法名）");
        en.put(PARAM_WATCH_ADD_MEMBERNAME_DESC, "Member name (field or method)");

        zh.put(PARAM_WATCH_ADD_SIGNATURE_DESC, "方法签名（仅method类型有效）");
        en.put(PARAM_WATCH_ADD_SIGNATURE_DESC, "Method signature (only used when the target type is method)");

        zh.put(PARAM_WATCH_ADD_INTERVAL_DESC, "检查间隔(ms)，默认1000");
        en.put(PARAM_WATCH_ADD_INTERVAL_DESC, "Check interval in ms, 1000 by default");

        zh.put(PARAM_WATCH_STOP_WATCHID_DESC, "要停止的Watch ID");
        en.put(PARAM_WATCH_STOP_WATCHID_DESC, "ID of the watch task to stop");

        zh.put(PARAM_WATCH_OUTPUT_TARGET_DESC, "目标 (ID或all)");
        en.put(PARAM_WATCH_OUTPUT_TARGET_DESC, "Target (a task ID or all)");

        zh.put(PARAM_WATCH_OUTPUT_LIMIT_DESC, "输出行数");
        en.put(PARAM_WATCH_OUTPUT_LIMIT_DESC, "Number of output lines");
    }
}
