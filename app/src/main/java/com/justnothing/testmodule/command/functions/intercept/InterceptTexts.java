package com.justnothing.testmodule.command.functions.intercept;

import com.justnothing.testmodule.command.framework.i18n.Text;

/**
 * intercept 命令族的 CLI 文案。
 *
 * <p>只放本族内部出现两次以上的输出文案（就中英并排的 {@link Text} 常量）。
 * 只出现一次的在调用点就地写 {@code Text.zhEn(...)}；跨族复用的（「类名: 」那些）在
 * {@code CliMessages} 里。</p>
 *
 * <p>本族没有注解文案，所以不像其它族的 XxxTexts 那样带 id 常量和 register 表。</p>
 */
public final class InterceptTexts {

    // ==================== 任务列表 / 状态（族内复用）====================
    public static final Text LABEL_TOTAL_TASKS = Text.zhEn("总计: %s 个任务", "Total: %s tasks");
    public static final Text NO_ACTIVE_TASKS = Text.zhEn("当前没有活跃的%s任务", "No active %s tasks");
    public static final Text ACTIVE_TASKS_HEADER = Text.zhEn("=== 活跃的%s任务 ===", "=== Active %s tasks ===");
    public static final Text TASK_NOT_FOUND = Text.zhEn("未找到任务: %s", "Task not found: %s");
    public static final Text NO_TASK_TYPE_FOUND = Text.zhEn("没有找到对应的任务类型: %s", "No task type found for: %s");
    public static final Text STATUS_RUNNING = Text.zhEn("运行中", "Running");
    public static final Text STATUS_PAUSED = Text.zhEn("已暂停", "Paused");
    public static final Text STATUS_STOPPED = Text.zhEn("已停止", "Stopped");

    // ==================== 通用标签（族内复用）====================
    public static final Text LABEL_SIGNATURE = Text.zhEn("签名: ", "Signature: ");

    // ==================== Trace 输出（族内复用）====================
    public static final Text TRACE_NO_RECORDS = Text.zhEn("暂无调用记录", "No call records yet");
    public static final Text TRACE_TASK_ID = Text.zhEn("任务ID: ", "Task ID: ");
    public static final Text TRACE_TARGET_METHOD = Text.zhEn("目标方法: ", "Target method: ");
    public static final Text TRACE_TOTAL_CALLS = Text.zhEn("总调用次数: ", "Total calls: ");
    public static final Text TRACE_CALL_TREE_HEADER = Text.zhEn("=== 调用树 ===\n", "=== Call tree ===\n");

    private InterceptTexts() {
    }
}
