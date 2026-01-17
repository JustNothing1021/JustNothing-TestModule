package com.justnothing.testmodule.command.functions.interactive;

import com.justnothing.testmodule.command.CommandExecutor;
import com.justnothing.testmodule.command.output.IOutputHandler;

public class InteractiveExampleMain {
    public static String runMain(CommandExecutor.CmdExecContext context) {
        IOutputHandler output = context.output();

        output.println("=== 交互式示例 ===");

        // 读取用户输入
        String name = context.readLine("请输入你的名字: ");
        output.println("你好, " + name + "!");

        // 读取年龄
        String ageStr = context.readLine("请输入你的年龄: ");
        try {
            int age = Integer.parseInt(ageStr);
            output.println("你的年龄是: " + age + " 岁");
        } catch (NumberFormatException e) {
            output.println("无效的年龄输入");
        }

        // 读取密码（不回显）
        String password = context.readPassword("请输入密码: ");
        output.println("密码长度: " + password.length() + " 个字符");

        output.println("=== 交互完成 ===");

        return "交互命令执行完成";
    }
}