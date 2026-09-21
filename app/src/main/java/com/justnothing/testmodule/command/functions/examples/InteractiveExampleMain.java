package com.justnothing.testmodule.command.functions.examples;

import com.justnothing.testmodule.command.framework.model.MainCommand;
import com.justnothing.testmodule.command.framework.CommandExecutor;
import com.justnothing.testmodule.command.framework.i18n.Text;
import com.justnothing.testmodule.command.framework.model.CommandResult;
import com.justnothing.testmodule.command.framework.model.CommandRequest;
import com.justnothing.testmodule.command.framework.output.ICommandOutputHandler;

import com.justnothing.testmodule.command.framework.annotation.Cmd;

@Cmd(name = "interactive_test", description = "交互式输入测试")
public class InteractiveExampleMain extends MainCommand<CommandResult> {

    public InteractiveExampleMain() {
        super("InteractiveExample", CommandResult.class);
    }

    @Override
    public String getHelpText() {
        return Text.zhEn(
                "语法: interactive_test\n\n交互式测试命令，演示如何使用交互式输入。\n\n示例:\n    interactive_test\n\n(Submodule interactive_test)\n",
                "Syntax: interactive_test\n\nInteractive test command; demonstrates how to use interactive input.\n\nExample:\n    interactive_test\n\n(Submodule interactive_test)\n")
                .text();
    }

    @Override
    protected CommandResult executeInternal(CommandExecutor.CmdExecContext<CommandRequest<?>> context) throws Exception {
        ICommandOutputHandler output = context.output();
        output.println(Text.zhEn("=== 交互式示例 ===", "=== Interactive example ===").text());
        String name = context.readLine(Text.zhEn("请输入你的名字: ", "Enter your name: ").text());
        output.println(Text.zhEn("你好, %s!", "Hello, %s!").format(name));
        String ageStr = context.readLine(Text.zhEn("请输入你的年龄: ", "Enter your age: ").text());
        try {
            int age = Integer.parseInt(ageStr);
            output.println(Text.zhEn("你的年龄是: %d 岁", "Your age is: %d years old").format(age));
        } catch (NumberFormatException e) {
            output.println(Text.zhEn("无效的年龄输入", "Invalid age input").text());
        }
        String password = context.readPassword(Text.zhEn("请输入密码: ", "Enter your password: ").text());
        output.println(Text.zhEn("密码长度: %d 个字符", "Password length: %d characters").format(password.length()));
        output.println(Text.zhEn("=== 交互完成 ===", "=== Interaction complete ===").text());
        return createSuccessResult(Text.zhEn("交互式测试命令执行完成", "Interactive test command finished").text());
    }
}