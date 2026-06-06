package com.justnothing.testmodule.command.functions.tui;

import com.justnothing.testmodule.command.CommandExecutor;
import com.justnothing.testmodule.command.base.MainCommand;
import com.justnothing.testmodule.command.base.command.Cmd;
import com.justnothing.testmodule.command.base.protocol.CommandResult;
import com.justnothing.testmodule.command.base.protocol.CommandRequest;
import com.justnothing.testmodule.command.output.Colors;
import com.justnothing.testmodule.command.tui.TuiWidgetData;
import com.justnothing.testmodule.command.tui.TuiWidgetType;
import com.justnothing.testmodule.command.tui.config.ProgressBarConfig;
import com.justnothing.testmodule.command.tui.config.SpinnerConfig;
import com.justnothing.testmodule.utils.logging.Logger;

@Cmd(name = "tui_test", description = "TUI Widget 协议测试（通过 socket 驱动客户端渲染）", defaultResultType = CommandResult.class)
public class TuiTestMain extends MainCommand<CommandResult> {

    private static final Logger logger = Logger.getLoggerForName("TuiTest");

    public TuiTestMain() {
        super("TuiTest", CommandResult.class);
    }

    @Override
    public String getHelpText() {
        return """
                ===== TUI Widget 协议测试 =====
                
                用法: tui_test <子命令>
                
                子命令:
                    bar              - 进度条测试（通过 socket 协议驱动客户端渲染）
                    spinner          - 转圈动画测试（socket 协议）
                    multi            - 进度条 + 转圈同时运行（socket 协议）
                
                工作原理:
                    服务端调用 context.createWidget() → 序列化为 JSON
                    → 通过 InteractiveProtocol TYPE_TUI_WIDGET_CREATE (0x18) 发送
                    → 客户端 SocketStreamReader 接收
                    → TuiClientManager 在本地 Android Terminal 上创建 AnsiProgressBar/AnsiSpinner
                
                注意: 此命令必须在交互式模式（--repl）下运行，
                      因为需要通过 socket 将 Widget 命令发送到客户端。
                
                """;
    }

    @Override
    public CommandResult runMain(CommandExecutor.CmdExecContext<CommandRequest> context) throws Exception {
        String[] args = context.args();

        if (args.length < 1) {
            context.println(getHelpText());
            return createErrorResult("参数不足，需要指定子命令");
        }

        switch (args[0]) {
            case "bar" -> testBarViaProtocol(context);
            case "spinner" -> testSpinnerViaProtocol(context);
            case "multi" -> testMultiWidgetViaProtocol(context);
            default -> {
                context.print("未知子命令: ", Colors.RED);
                context.println(args[0], Colors.YELLOW);
                context.println(getHelpText());
            }
        }
        return createSuccessResult("tui_test 执行完成");
    }

    // ==================== 进度条测试（协议驱动）====================

    private void testBarViaProtocol(CommandExecutor.CmdExecContext<?> context) throws InterruptedException {
        logger.info("===== testBarViaProtocol START =====");
        logger.info("output handler type: %s", context.output().getClass().getName());
        logger.info("isInteractive: %s", context.output().isInteractive());
        context.println("===== 进度条测试（socket 协议）=====", Colors.CYAN);
        context.println("  正在通过协议发送 CREATE 命令到客户端...", Colors.GRAY);

        // 1. 创建进度条（发送到客户端）
        ProgressBarConfig config = new ProgressBarConfig();
        config.setTaskName("Downloading");
        config.setMax(100);
        config.setStyle("unicode");
        config.setShowPercent(true);
        config.setShowEta(true);
        config.setText("initializing...");

        TuiWidgetData createData = TuiWidgetData.create(
            "test-bar-1", TuiWidgetType.PROGRESS_BAR, "Download Progress", config);
        logger.info("calling output().createWidget() for widgetId=test-bar-1");
        context.output().createWidget(createData);
        logger.info("createWidget() returned, sleeping 200ms for client to process");

        Thread.sleep(200);  // 等客户端创建完毕

        // 2. 循环更新进度
        String[] phases = {"connecting...", "downloading metadata...", "fetching data...",
                           "verifying...", "finalizing..."};
        for (int i = 0; i <= 100; i++) {
            ProgressBarConfig updateCfg = new ProgressBarConfig();
            updateCfg.setCurrent(i);
            updateCfg.setText(phases[Math.min(i / 20, phases.length - 1)]);
            if (i == 50) updateCfg.setExtraMessage("halfway there!");
            if (i == 90) updateCfg.setExtraMessage("almost done...");

            TuiWidgetData updateData = TuiWidgetData.update("test-bar-1", TuiWidgetType.PROGRESS_BAR, updateCfg);

            if (i == 0 || i == 25 || i == 50 || i == 75 || i == 100) {
                logger.info("calling output().updateWidget() for test-bar-1, current=%d", i);
            }
            context.output().updateWidget(updateData);

            // 通过协议打日志（也会显示在客户端）
            if (i % 25 == 0 && i > 0) {
                context.printf(Colors.BLUE, "[INFO] Progress: %d%%\n", i);
            }

            Thread.sleep(50);
        }

        // 3. 完成
        logger.info("calling output().destroyWidget() for test-bar-1");
        context.output().destroyWidget("test-bar-1");
        logger.info("destroyWidget() returned");
        Thread.sleep(200);
        context.println("", Colors.WHITE);
        context.println("  ✓ 进度条测试完成（数据通过 socket 协议传输）", Colors.GREEN);
        logger.info("===== testBarViaProtocol END =====");
    }

    // ==================== 转圈测试（协议驱动）====================

    private void testSpinnerViaProtocol(CommandExecutor.CmdExecContext<?> context) throws InterruptedException {
        context.println("===== 转圈动画测试（socket 协议）=====", Colors.CYAN);

        // Phase 1: Dots 转圈
        SpinnerConfig cfg1 = new SpinnerConfig();
        cfg1.setText("Loading with dots...");
        cfg1.setFrameStyle("dots");
        cfg1.setIntervalMs(80);
        cfg1.setColor(36);  // CYAN

        context.output().createWidget(TuiWidgetData.create("test-spin-1", TuiWidgetType.SPINNER, "Phase 1", cfg1));
        Thread.sleep(300);

        for (int i = 0; i < 30; i++) {
            SpinnerConfig u1 = new SpinnerConfig();
            u1.setDynamicText("Phase 1/3 (" + (i + 1) + "/30)");
            context.output().updateWidget(TuiWidgetData.update("test-spin-1", TuiWidgetType.SPINNER, u1));
            Thread.sleep(80);
        }

        // 销毁 Phase 1，开始 Phase 2
        context.output().destroyWidget("test-spin-1");
        Thread.sleep(200);

        // Phase 2: Line 转圈
        SpinnerConfig cfg2 = new SpinnerConfig();
        cfg2.setText("");
        cfg2.setFrameStyle("line");
        cfg2.setIntervalMs(120);
        cfg2.setColor(33);  // YELLOW

        context.output().createWidget(TuiWidgetData.create("test-spin-2", TuiWidgetType.SPINNER, "Phase 2", cfg2));
        Thread.sleep(300);

        for (int i = 0; i < 20; i++) {
            SpinnerConfig u2 = new SpinnerConfig();
            u2.setDynamicText("Phase 2/3 (" + (i + 1) + "/20)");
            context.output().updateWidget(TuiWidgetData.update("test-spin-2", TuiWidgetType.SPINNER, u2));
            Thread.sleep(120);
        }

        context.output().destroyWidget("test-spin-2");
        Thread.sleep(200);

        // Phase 3: Arrows 转圈
        SpinnerConfig cfg3 = new SpinnerConfig();
        cfg3.setText("");
        cfg3.setFrameStyle("arrows");
        cfg3.setIntervalMs(150);
        cfg3.setColor(35);  // MAGENTA

        context.output().createWidget(TuiWidgetData.create("test-spin-3", TuiWidgetType.SPINNER, "Phase 3", cfg3));
        Thread.sleep(300);

        for (int i = 0; i < 15; i++) {
            SpinnerConfig u3 = new SpinnerConfig();
            u3.setDynamicText("Phase 3/3 (" + (i + 1) + "/15)");
            context.output().updateWidget(TuiWidgetData.update("test-spin-3", TuiWidgetType.SPINNER, u3));
            Thread.sleep(150);
        }

        context.output().destroyWidget("test-spin-3");
        Thread.sleep(200);

        context.println("", Colors.WHITE);
        context.println("  ✓ 转圈测试完成（3 个阶段，全部通过协议驱动）", Colors.GREEN);
    }

    // ==================== 多 Widget 同时运行 ====================

    /**
     * 最关键的测试：进度条和转圈同时存在，验证 Engine 的调度能力
     */
    private void testMultiWidgetViaProtocol(CommandExecutor.CmdExecContext<?> context) throws InterruptedException {
        context.println("===== 多 Widget 共存测试（socket 协议）=====", Colors.CYAN);
        context.println("  进度条 + 转圈同时通过协议驱动", Colors.GRAY);
        Thread.sleep(300);

        // 创建进度条
        ProgressBarConfig barCfg = new ProgressBarConfig();
        barCfg.setTaskName("Download");
        barCfg.setMax(100);
        barCfg.setStyle("unicode");
        barCfg.setShowPercent(true);
        barCfg.setShowCount(true);
        barCfg.setShowEta(true);

        context.output().createWidget(TuiWidgetData.create(
            "multi-bar", TuiWidgetType.PROGRESS_BAR, "Main Progress", barCfg));

        // 创建转圈
        SpinnerConfig spinCfg = new SpinnerConfig();
        spinCfg.setText("Parsing metadata...");
        spinCfg.setFrameStyle("dots");
        spinCfg.setIntervalMs(100);
        spinCfg.setColor(35);  // MAGENTA

        context.output().createWidget(TuiWidgetData.create(
            "multi-spin", TuiWidgetType.SPINNER, "Background Task", spinCfg));

        Thread.sleep(500);  // 等客户端两个 Widget 都创建好

        // 同时更新两个 Widget
        int fileCount = 0;
        for (int i = 0; i <= 100; i++) {
            // 更新进度条
            ProgressBarConfig barUpdate = new ProgressBarConfig();
            barUpdate.setCurrent(i);
            if (i == 40) barUpdate.setExtraMessage("speeding up...");
            if (i == 80) barUpdate.setExtraMessage("final stretch!");
            context.output().updateWidget(TuiWidgetData.update("multi-bar", TuiWidgetType.PROGRESS_BAR, barUpdate));

            // 更新转圈
            SpinnerConfig spinUpdate = new SpinnerConfig();
            spinUpdate.setDynamicText("Parsed file " + Math.min(fileCount++, 47) + "/47");
            context.output().updateWidget(TuiWidgetData.update("multi-spin", TuiWidgetType.SPINNER, spinUpdate));

            // 定期打日志（通过 socket 显示在客户端）
            if (i % 20 == 0 && i > 0) {
                context.printf(Colors.BLUE, "[LOG] Downloaded chunk %d/5\n", i / 20);
            }

            Thread.sleep(60);
        }

        // 销毁所有 Widget
        context.output().destroyWidget("multi-bar");
        context.output().destroyWidget("multi-spin");
        Thread.sleep(300);

        context.println("", Colors.WHITE);
        context.println("  ✓ 多 Widget 共存测试完成", Colors.GREEN);
    }
}