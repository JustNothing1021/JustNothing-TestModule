package com.justnothing.testmodule.command.functions.examples;

import com.justnothing.testmodule.command.base.MainCommand;
import com.justnothing.testmodule.command.CommandExecutor;
import com.justnothing.testmodule.command.base.CommandResult;
import com.justnothing.testmodule.command.base.CommandRequest;
import com.justnothing.testmodule.command.output.Colors;
import com.justnothing.testmodule.command.output.ICommandOutputHandler;

import com.justnothing.testmodule.command.base.RegisterCommand;

@RegisterCommand("output_test")
public class OutputExampleMain extends MainCommand<CommandRequest, CommandResult> {

    public OutputExampleMain() {
        super("OutputExample", CommandResult.class);
    }

    @Override
    public String getHelpText() {
        return """
                语法: output_test
                
                输出测试命令，演示如何使用输出处理器。
                
                示例:
                    output_test
                
                (Submodule output_test)
                """;
    }

    @Override
    public CommandResult runMain(CommandExecutor.CmdExecContext<CommandRequest> context) throws Exception {
        ICommandOutputHandler output = context.output();

        output.println("===== 输出处理器测试 =====");
        output.println("");

        output.println(">>> 预定义颜色方法测试：");
        output.printSuccess("这是成功消息（绿色）");
        output.printlnSuccess("这是成功消息带换行（绿色）");

        output.printError("这是错误消息（红色）");
        output.printlnError("这是错误消息带换行（红色）");

        output.printWarning("这是警告消息（黄色）");
        output.printlnWarning("这是警告消息带换行（黄色）");

        output.printInfo("这是信息消息（蓝色）");
        output.printlnInfo("这是信息消息带换行（蓝色）");

        output.printDebug("这是调试消息（青色）");
        output.printlnDebug("这是调试消息带换行（青色）");

        output.println("");
        output.println(">>> 自定义颜色参数测试：");

        output.print("自定义红色文本\n", Colors.RED);
        output.println("自定义绿色文本", Colors.GREEN);
        output.println("自定义黄色文本", Colors.YELLOW);
        output.println("自定义蓝色文本", Colors.BLUE);
        output.println("自定义青色文本", Colors.CYAN);
        output.println("自定义紫色文本", Colors.MAGENTA);
        output.println("默认颜色文本", Colors.DEFAULT);

        output.println("");
        output.println(">>> 更多颜色测试：");
        output.println("橙色文本", Colors.ORANGE);
        output.println("粉色文本", Colors.PINK);
        output.println("棕色文本", Colors.BROWN);
        output.println("金色文本", Colors.GOLD);
        output.println("银色文本", Colors.SILVER);
        output.println("青柠文本", Colors.LIME);
        output.println("蓝绿文本", Colors.TEAL);
        output.println("海军蓝文本", Colors.NAVY);
        output.println("栗色文本", Colors.MAROON);
        output.println("橄榄文本", Colors.OLIVE);
        output.println("珊瑚文本", Colors.CORAL);
        output.println("鲑鱼文本", Colors.SALMON);
        output.println("靛蓝文本", Colors.INDIGO);
        output.println("紫罗兰文本", Colors.VIOLET);

        output.println("");
        output.println(">>> 浅色系测试：");
        output.println("浅红文本", Colors.LIGHT_RED);
        output.println("浅绿文本", Colors.LIGHT_GREEN);
        output.println("浅黄文本", Colors.LIGHT_YELLOW);
        output.println("浅蓝文本", Colors.LIGHT_BLUE);
        output.println("浅青文本", Colors.LIGHT_CYAN);
        output.println("浅紫文本", Colors.LIGHT_MAGENTA);
        output.println("浅灰文本", Colors.LIGHT_GRAY);

        output.println("");
        output.println(">>> 深色系测试：");
        output.println("深灰文本", Colors.DARK_GRAY);

        output.println("");
        output.println(">>> printf 带颜色测试：");
        output.printf(Colors.GREEN, "数值: %d, 字符串: %s\n", 42, "hello");
        output.printf(Colors.YELLOW, "百分比: %.2f%%\n", 99.95);

        output.println("");
        output.println(">>> 倒计时测试：");

        try {
            for (int i = 300; i >= 1; i--) {
                output.printf(Colors.CYAN, "倒计时: %.2fs    \r", i / 100.0f);
                Thread.sleep(10);
            }
            output.println("");
            output.printlnSuccess("倒计时完成！");
        } catch (InterruptedException e) {
            output.printlnError("命令执行被打断...");
        }

        output.println("");
        output.println(">>> 性能测试：");
        
        final int TEST_COUNT = 10000;
        long startTime = System.currentTimeMillis();
        
        for (int i = 0; i < TEST_COUNT; i++) {
            output.print("X", Colors.GREEN);
        }
        
        long endTime = System.currentTimeMillis();
        double duration = (endTime - startTime) / 1000.0;
        
        output.println("");
        output.printf(Colors.YELLOW, "执行 %d 次输出耗时: %.3f 秒\n", TEST_COUNT, duration);
        output.printf(Colors.CYAN, "平均每次输出: %.4f 毫秒\n", (duration * 1000) / TEST_COUNT);

        output.println("");
        output.println(">>> printStackTrace 颜色测试：");
        try {
            throw new RuntimeException("这是一个测试异常");
        } catch (RuntimeException e) {
            output.println("默认颜色:", Colors.DEFAULT);
            output.printStackTrace(e);
            output.println("");
            output.println("红色堆栈:", Colors.RED);
            output.printStackTrace(e, Colors.RED);
            output.println("");
            output.println("灰色堆栈:", Colors.GRAY);
            output.printStackTrace(e, Colors.GRAY);
        }

        output.println("");
        output.println("===== 测试完成 =====");
        if (shouldReturnStructuredData(context)) {
            return createSuccessResult("输出测试命令执行完成");
        }
        return null;
    }
}