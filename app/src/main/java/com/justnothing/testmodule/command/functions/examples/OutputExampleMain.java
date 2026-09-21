package com.justnothing.testmodule.command.functions.examples;

import com.justnothing.testmodule.command.framework.model.MainCommand;
import com.justnothing.testmodule.command.framework.CommandExecutor;
import com.justnothing.testmodule.command.framework.i18n.Text;
import com.justnothing.testmodule.command.framework.model.CommandResult;
import com.justnothing.testmodule.command.framework.model.CommandRequest;
import com.justnothing.testmodule.command.framework.output.Colors;
import com.justnothing.testmodule.command.framework.output.ICommandOutputHandler;

import com.justnothing.testmodule.command.framework.annotation.Cmd;

@Cmd(name = "output_test", description = "输出处理器测试")
public class OutputExampleMain extends MainCommand<CommandResult> {

    public OutputExampleMain() {
        super("OutputExample", CommandResult.class);
    }

    @Override
    public String getHelpText() {
        return Text.zhEn(
                "语法: output_test\n\n输出测试命令，演示如何使用输出处理器。\n\n示例:\n    output_test\n\n(Submodule output_test)\n",
                "Syntax: output_test\n\nOutput test command; demonstrates how to use the output handler.\n\nExample:\n    output_test\n\n(Submodule output_test)\n")
                .text();
    }

    @Override
    protected CommandResult executeInternal(CommandExecutor.CmdExecContext<CommandRequest<?>> context) throws Exception {
        ICommandOutputHandler output = context.output();

        output.println(Text.zhEn("===== 输出处理器测试 =====", "===== Output handler test =====").text());
        output.println("");

        output.println(Text.zhEn(">>> 预定义颜色方法测试：", ">>> Predefined color methods:").text());
        output.printSuccess(Text.zhEn("这是成功消息（绿色）", "This is a success message (green)").text());
        output.printlnSuccess(Text.zhEn("这是成功消息带换行（绿色）", "This is a success message with a newline (green)").text());

        output.printError(Text.zhEn("这是错误消息（红色）", "This is an error message (red)").text());
        output.printlnError(Text.zhEn("这是错误消息带换行（红色）", "This is an error message with a newline (red)").text());

        output.printWarning(Text.zhEn("这是警告消息（黄色）", "This is a warning message (yellow)").text());
        output.printlnWarning(Text.zhEn("这是警告消息带换行（黄色）", "This is a warning message with a newline (yellow)").text());

        output.printInfo(Text.zhEn("这是信息消息（蓝色）", "This is an info message (blue)").text());
        output.printlnInfo(Text.zhEn("这是信息消息带换行（蓝色）", "This is an info message with a newline (blue)").text());

        output.printDebug(Text.zhEn("这是调试消息（青色）", "This is a debug message (cyan)").text());
        output.printlnDebug(Text.zhEn("这是调试消息带换行（青色）", "This is a debug message with a newline (cyan)").text());

        output.println("");
        output.println(Text.zhEn(">>> 自定义颜色参数测试：", ">>> Custom color argument test:").text());

        output.print(Text.zhEn("自定义红色文本\n", "Custom red text\n").text(), Colors.RED);
        output.println(Text.zhEn("自定义绿色文本", "Custom green text").text(), Colors.GREEN);
        output.println(Text.zhEn("自定义黄色文本", "Custom yellow text").text(), Colors.YELLOW);
        output.println(Text.zhEn("自定义蓝色文本", "Custom blue text").text(), Colors.BLUE);
        output.println(Text.zhEn("自定义青色文本", "Custom cyan text").text(), Colors.CYAN);
        output.println(Text.zhEn("自定义紫色文本", "Custom magenta text").text(), Colors.MAGENTA);
        output.println(Text.zhEn("默认颜色文本", "Default color text").text(), Colors.DEFAULT);

        output.println("");
        output.println(Text.zhEn(">>> 更多颜色测试：", ">>> More colors:").text());
        output.println(Text.zhEn("橙色文本", "Orange text").text(), Colors.ORANGE);
        output.println(Text.zhEn("粉色文本", "Pink text").text(), Colors.PINK);
        output.println(Text.zhEn("棕色文本", "Brown text").text(), Colors.BROWN);
        output.println(Text.zhEn("金色文本", "Gold text").text(), Colors.GOLD);
        output.println(Text.zhEn("银色文本", "Silver text").text(), Colors.SILVER);
        output.println(Text.zhEn("青柠文本", "Lime text").text(), Colors.LIME);
        output.println(Text.zhEn("蓝绿文本", "Teal text").text(), Colors.TEAL);
        output.println(Text.zhEn("海军蓝文本", "Navy text").text(), Colors.NAVY);
        output.println(Text.zhEn("栗色文本", "Maroon text").text(), Colors.MAROON);
        output.println(Text.zhEn("橄榄文本", "Olive text").text(), Colors.OLIVE);
        output.println(Text.zhEn("珊瑚文本", "Coral text").text(), Colors.CORAL);
        output.println(Text.zhEn("鲑鱼文本", "Salmon text").text(), Colors.SALMON);
        output.println(Text.zhEn("靛蓝文本", "Indigo text").text(), Colors.INDIGO);
        output.println(Text.zhEn("紫罗兰文本", "Violet text").text(), Colors.VIOLET);

        output.println("");
        output.println(Text.zhEn(">>> 浅色系测试：", ">>> Light colors:").text());
        output.println(Text.zhEn("浅红文本", "Light red text").text(), Colors.LIGHT_RED);
        output.println(Text.zhEn("浅绿文本", "Light green text").text(), Colors.LIGHT_GREEN);
        output.println(Text.zhEn("浅黄文本", "Light yellow text").text(), Colors.LIGHT_YELLOW);
        output.println(Text.zhEn("浅蓝文本", "Light blue text").text(), Colors.LIGHT_BLUE);
        output.println(Text.zhEn("浅青文本", "Light cyan text").text(), Colors.LIGHT_CYAN);
        output.println(Text.zhEn("浅紫文本", "Light magenta text").text(), Colors.LIGHT_MAGENTA);
        output.println(Text.zhEn("浅灰文本", "Light gray text").text(), Colors.LIGHT_GRAY);

        output.println("");
        output.println(Text.zhEn(">>> 深色系测试：", ">>> Dark colors:").text());
        output.println(Text.zhEn("深灰文本", "Dark gray text").text(), Colors.DARK_GRAY);

        output.println("");
        output.println(Text.zhEn(">>> printf 带颜色测试：", ">>> printf with colors:").text());
        output.printf(Colors.GREEN, Text.zhEn("数值: %d, 字符串: %s\n", "Number: %d, string: %s\n").text(), 42, "hello");
        output.printf(Colors.YELLOW, Text.zhEn("百分比: %.2f%%\n", "Percentage: %.2f%%\n").text(), 99.95);

        output.println("");
        output.println(Text.zhEn(">>> 倒计时测试：", ">>> Countdown:").text());

        try {
            for (int i = 300; i >= 1; i--) {
                output.printf(Colors.CYAN, Text.zhEn("倒计时: %.2fs    \r", "Countdown: %.2fs    \r").text(), i / 100.0f);
                Thread.sleep(10);
            }
            output.println("");
            output.printlnSuccess(Text.zhEn("倒计时完成！", "Countdown finished!").text());
        } catch (InterruptedException e) {
            output.printlnError(Text.zhEn("命令执行被打断...", "Command interrupted...").text());
        }

        output.println("");
        output.println(Text.zhEn(">>> 性能测试：", ">>> Performance test:").text());
        
        final int TEST_COUNT = 10000;
        long startTime = System.currentTimeMillis();
        
        for (int i = 0; i < TEST_COUNT; i++) {
            output.print("X", Colors.GREEN);
        }
        
        long endTime = System.currentTimeMillis();
        double duration = (endTime - startTime) / 1000.0;
        
        output.println("");
        output.printf(Colors.YELLOW, Text.zhEn("执行 %d 次输出耗时: %.3f 秒\n", "%d output calls took %.3f seconds\n").text(), TEST_COUNT, duration);
        output.printf(Colors.CYAN, Text.zhEn("平均每次输出: %.4f 毫秒\n", "Average per output call: %.4f ms\n").text(), (duration * 1000) / TEST_COUNT);

        output.println("");
        output.println(Text.zhEn(">>> printStackTrace 颜色测试：", ">>> printStackTrace color test:").text());
        try {
            throw new RuntimeException(Text.zhEn("这是一个测试异常", "This is a test exception").text());
        } catch (RuntimeException e) {
            output.println(Text.zhEn("默认颜色:", "Default color:").text(), Colors.DEFAULT);
            output.printStackTrace(e);
            output.println("");
            output.println(Text.zhEn("红色堆栈:", "Red stack trace:").text(), Colors.RED);
            output.printStackTrace(e, Colors.RED);
            output.println("");
            output.println(Text.zhEn("灰色堆栈:", "Gray stack trace:").text(), Colors.GRAY);
            output.printStackTrace(e, Colors.GRAY);
        }

        output.println("");
        output.println(Text.zhEn("===== 测试完成 =====", "===== Test complete =====").text());
        return createSuccessResult(Text.zhEn("输出测试命令执行完成", "Output test command finished").text());
    }
}