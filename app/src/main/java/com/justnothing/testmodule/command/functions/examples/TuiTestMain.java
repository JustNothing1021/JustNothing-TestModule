package com.justnothing.testmodule.command.functions.examples;

import com.justnothing.testmodule.command.base.MainCommand;
import com.justnothing.testmodule.command.CommandExecutor;
import com.justnothing.testmodule.command.base.protocol.CommandResult;
import com.justnothing.testmodule.command.base.protocol.CommandRequest;
import com.justnothing.testmodule.command.output.ICommandOutputHandler;
import com.justnothing.methodsclient.tui.widget.TuiWidgetData;

import com.justnothing.testmodule.command.base.command.Cmd;

/**
 * TUI Widget 交互测试命令。
 * <p>
 * 通过 InteractiveProtocol 向客户端发送 TUI Widget 控制消息，
 * 演示进度条、日志面板、状态栏、表格、Spinner 五种组件的完整生命周期。
 * <p>
 * 客户端需运行 TuiManager（TUI REPL）才能看到可视化效果，
 * 非 TUI 客户端会安全忽略这些调用（接口默认空实现）。
 */
@Cmd(name = "tui_test", description = "TUI 组件交互测试", defaultResultType = CommandResult.class)
public class TuiTestMain extends MainCommand<CommandResult> {

    public TuiTestMain() {
        super("TuiTest", CommandResult.class);
    }

    @Override
    public String getHelpText() {
        return """
                语法: tui_test
                
                TUI Widget 协议交互测试，演示服务端通过 InteractiveProtocol
                驱动客户端 Lanterna 组件的完整流程。
                
                演示内容：
                  - ProgressBar：模拟扫描进度（0% → 100%）
                  - LogPanel：追加日志行 + FIFO 滚动
                  - StatusBar：状态切换 (running → success → error → warning)
                  - Table：动态添加数据行
                  - Spinner：不确定进度动画
                
                示例:
                    tui_test
                
                注意：需要在 TUI REPL 模式下执行才能看到可视化效果。
                
                (Submodule tui_test)
                """;
    }

    @Override
    public CommandResult runMain(CommandExecutor.CmdExecContext<CommandRequest> context) throws Exception {
        ICommandOutputHandler output = context.output();

        output.println("=== TUI Widget 交互测试 ===");
        output.println("正在通过 InteractiveProtocol 发送组件控制消息...\n");

        // ==================== Phase 1: 创建所有组件 ====================
        output.printlnInfo("[Phase 1] 创建 5 种 TUI 组件...");

        // 1. 进度条
        output.createWidget(TuiWidgetData.createProgressBar("scan-pb")
                .title("扫描进度")
                .label("准备中...")
                .progress(0)
                .total(100));

        // 2. 日志面板
        output.createWidget(TuiWidgetData.createLogPanel("log-panel")
                .title("扫描日志"));

        // 3. 状态栏
        output.createWidget(TuiWidgetData.createStatusBar("status")
                .status("初始化中...")
                .statusType("info"));

        // 4. 表格
        output.createWidget(TuiWidgetData.createTable("result-table")
                .title("扫描结果")
                .headers("文件名", "大小", "状态"));

        // 5. Spinner
        output.createWidget(TuiWidgetData.createSpinner("worker-spinner")
                .title("后台任务")
                .spinnerText("初始化扫描引擎..."));

        Thread.sleep(500);

        // ==================== Phase 2: 模拟扫描过程（进度条 + 日志 + Spinner） ====================
        output.printlnInfo("[Phase 2] 模拟扫描过程...");

        output.updateWidget(TuiWidgetData.createProgressBar("scan-pb")
                .label("扫描类文件...")
                .color("cyan"));
        output.updateWidget(TuiWidgetData.createStatusBar("status")
                .status("正在扫描")
                .statusType("running"));
        output.updateWidget(TuiWidgetData.createSpinner("worker-spinner")
                .spinnerText("分析字节码..."));

        // 模拟逐步推进的扫描
        String[] scanTargets = {
            "com/example/MainActivity.class",
            "com/example/network/ApiClient.class",
            "com/example/database/RoomHelper.class",
            "com/example/ui/adapter/RecyclerAdapter.class",
            "com/example/utils/StringUtils.class",
            "com/example/service/BackgroundService.class",
            "com/example/model/UserModel.class",
            "com/example/fragment/HomeFragment.class",
        };

        int total = scanTargets.length;
        for (int i = 0; i < total; i++) {
            int percent = (int) ((i + 1) * 100.0 / total);
            String target = scanTargets[i];

            // 更新进度条
            output.updateWidget(TuiWidgetData.createProgressBar("scan-pb")
                    .progress(percent)
                    .total(100)
                    .label(target)
                    .speed(percent * 1.0));

            // 追加日志
            String logLevel = (i % 3 == 0) ? "[SCAN]" : (i % 3 == 1) ? "[INFO]" : "[DEBUG]";
            output.updateWidget(TuiWidgetData.createLogPanel("log-panel")
                    .appendLine(logLevel + " 正在处理: " + target));

            // 更新 Spinner 文本
            output.updateWidget(TuiWidgetData.createSpinner("worker-spinner")
                    .spinnerText("检查依赖关系..."));

            // 往表格里加一行
            int sizeKB = 2000 + (int) (Math.random() * 15000);
            String status = Math.random() > 0.1 ? "OK" : "WARN";
            output.updateWidget(TuiWidgetData.createTable("result-table")
                    .addRow(target.substring(target.lastIndexOf('/') + 1),
                            sizeKB + " KB", status));

            Thread.sleep(400 + (long) (Math.random() * 300));
        }

        // 扫描完成
        output.updateWidget(TuiWidgetData.createProgressBar("scan-pb")
                .progress(100).total(100).label("扫描完成!")
                .showPercent(true));
        output.updateWidget(TuiWidgetData.createSpinner("worker-spinner")
                .spinnerText("生成报告..."));
        Thread.sleep(600);

        // ==================== Phase 3: 状态切换演示 ====================
        output.printlnInfo("[Phase 3] 状态栏状态切换演示...");

        // success
        output.updateWidget(TuiWidgetData.createStatusBar("status")
                .status("所有文件扫描完成").statusType("success"));
        output.updateWidget(TuiWidgetData.createLogPanel("log-panel")
                .appendLine("[DONE] 扫描完成, 共处理 " + total + " 个文件"));
        Thread.sleep(800);

        // warning
        output.updateWidget(TuiWidgetData.createStatusBar("status")
                .status("发现 2 个潜在问题").statusType("warning"));
        output.updateWidget(TuiWidgetData.createLogPanel("log-panel")
                .appendLine("[WARN] 发现未使用的 import: java.util.Vector")
                .lineColor("yellow"));
        Thread.sleep(800);

        // error
        output.updateWidget(TuiWidgetData.createStatusBar("status")
                .status("发现 1 个严重问题!").statusType("error"));
        output.updateWidget(TuiWidgetData.createLogPanel("log-panel")
                .appendLine("[ERROR] 类文件损坏: com/example/BrokenClass.class")
                .lineColor("red"));
        Thread.sleep(800);

        // 回到 info
        output.updateWidget(TuiWidgetData.createStatusBar("status")
                .status("等待下一步操作...").statusType("info"));
        output.updateWidget(TuiWidgetData.createSpinner("worker-spinner")
                .spinnerText("空闲中..."));

        Thread.sleep(500);

        // ==================== Phase 4: 清理演示 ====================
        output.printlnInfo("[Phase 4] 销毁组件演示...");

        // 单独销毁 spinner
        output.destroyWidget("worker-spinner");
        output.println("  -> 已销毁 Spinner (id=worker-spinner)");
        Thread.sleep(400);

        // 单独销毁状态栏
        output.destroyWidget("status");
        output.println("  -> 已销毁 StatusBar (id=status)");
        Thread.sleep(400);

        // 清空日志面板内容（不销毁）
        output.updateWidget(TuiWidgetData.createLogPanel("log-panel").clear());
        output.println("  -> 已清空 LogPanel 内容");
        Thread.sleep(400);

        // 清空表格行
        output.updateWidget(TuiWidgetData.createTable("result-table").clearRows());
        output.println("  -> 已清空 Table 行数据");
        Thread.sleep(400);

        // 最后：clearAll 一次性清除剩余组件
        output.clearAllWidgets();
        output.println("  -> clearAllWidgets(): 剩余组件已全部清除");

        Thread.sleep(300);

        // ==================== 完成 ====================
        output.println("");
        output.printlnSuccess("=== TUI Widget 测试完成 ===");
        output.println("以上操作通过 InteractiveProtocol (0x18-0x1B) 发送到客户端。");
        output.println("如果客户端运行了 TuiManager，你应该能看到完整的动画效果。\n");

        return createSuccessResult("TUI 测试命令执行完成");
    }
}
