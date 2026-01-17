package com.justnothing.testmodule.command.functions.output;

import com.justnothing.testmodule.command.CommandExecutor;
import com.justnothing.testmodule.command.output.IOutputHandler;

public class OutputExampleMain {
    public static String runMain(CommandExecutor.CmdExecContext context) {
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