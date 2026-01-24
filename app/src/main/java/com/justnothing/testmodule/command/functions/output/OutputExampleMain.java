package com.justnothing.testmodule.command.functions.output;

import com.justnothing.testmodule.command.CommandExecutor;
import com.justnothing.testmodule.command.functions.CommandBase;
import com.justnothing.testmodule.command.output.IOutputHandler;

public class OutputExampleMain extends CommandBase {

    public OutputExampleMain() {
        super("OutputExample");
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
    public String runMain(CommandExecutor.CmdExecContext context) {
        IOutputHandler output = context.output();

        try {
            for (int i = 1000; i >= 1; i--) {
                output.printf("倒计时: %.2fs    \r", i / 100.0f);
                Thread.sleep(10);
            }
            output.println("MISSION FAILED (bushi)\n");
        } catch (InterruptedException e) {
            output.println("命令执行被打断...");
        }

        return "输出测试执行完成";
    }
}