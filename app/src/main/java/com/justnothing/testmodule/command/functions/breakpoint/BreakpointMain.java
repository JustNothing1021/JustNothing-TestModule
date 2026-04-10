package com.justnothing.testmodule.command.functions.breakpoint;

import static com.justnothing.testmodule.constants.CommandServer.CMD_BREAKPOINT_VER;

import com.justnothing.testmodule.command.CommandExecutor;
import com.justnothing.testmodule.command.functions.CommandBase;
import com.justnothing.testmodule.command.functions.intercept.BreakpointInterceptTask;
import com.justnothing.testmodule.command.output.Colors;
import com.justnothing.testmodule.command.utils.CommandArgumentParser;
import com.justnothing.testmodule.command.utils.CommandExceptionHandler;

import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class BreakpointMain extends CommandBase {

    public BreakpointMain() {
        super("Breakpoint");
    }

    @Override
    public String getHelpText() {
        return String.format(
                Locale.getDefault(),
                """
                语法: breakpoint <subcmd> [args...]
                
                设置和管理断点。
                
                子命令:
                    add <class_name> <method_name> [sig/signature <signature>]  - 添加断点
                    list                                                        - 列出所有断点
                    enable <id>                                                 - 启用断点
                    disable <id>                                                - 禁用断点
                    remove <id>                                                 - 移除断点
                    clear                                                       - 清除所有断点
                    hits                                                        - 显示断点命中统计
                
                选项:
                    sig, signature   - 指定方法签名
                        可以是用逗号分割的类名列表，如 "String,int" 表示(String, int)参数的方法
                        也可以是JVM内部格式的签名，如 "(Ljava/lang/String;I;)V" 也是一样
                        （不过因为理论上不会有同参数列表不同返回类型的情况，所以返回类型的解析不会有实际作用）
                
                示例:
                    breakpoint add com.example.MyClass myMethod
                    breakpoint add com.example.MyClass myMethod signature String
                    breakpoint add com.example.MyClass myMethod sig String,int
                    breakpoint list
                    breakpoint enable 1
                    breakpoint disable 1
                    breakpoint remove 1
                    breakpoint clear
                    breakpoint hits
                
                注意:
                    - 断点会在方法被调用时输出调用栈信息
                    - 不指定sig时，会在所有同名方法上设置断点
                    - sig的类名之间用逗号隔开，不要有空格
                    - sig可以指定完整类名或者 java.lang.* 和 java.util.* 下的类名
                    - 断点会记录命中次数
                    - 断点不会暂停程序执行，只会输出信息
                
                (Submodule breakpoint %s)
                """, CMD_BREAKPOINT_VER);
    }

    @Override
    public void runMain(CommandExecutor.CmdExecContext context) {
        String[] args = context.args();
        ClassLoader classLoader = context.classLoader();

        logger.debug("执行breakpoint命令，参数: " + Arrays.toString(args));

        if (args.length < 1) {
            logger.warn("参数不足");
            context.println(getHelpText(), Colors.WHITE);
            return;
        }

        String subCommand = args[0];
        BreakpointManager manager = BreakpointManager.getInstance();

        try {
            switch (subCommand) {
                case "add" -> handleAdd(args, classLoader, manager, context);
                case "list" -> handleList(manager, context);
                case "enable" -> handleEnable(args, manager, context);
                case "disable" -> handleDisable(args, manager, context);
                case "remove" -> handleRemove(args, manager, context);
                case "clear" -> handleClear(manager, context);
                case "hits" -> handleHits(manager, context);
                default -> {
                    context.println("未知子命令: " + subCommand, Colors.RED);
                    context.println(getHelpText(), Colors.WHITE);
                }
            }
        } catch (Exception e) {
            CommandExceptionHandler.handleException("breakpoint " + subCommand, e, context, "执行breakpoint的某个子命令时出错");
        }
    }

    private void handleAdd(String[] args, ClassLoader classLoader, BreakpointManager manager, CommandExecutor.CmdExecContext context) {
        if (args.length < 3) {
            context.println("错误: 参数不足", Colors.RED);
            context.println("用法: breakpoint add <class_name> <method_name> [sig/signature <signature>]", Colors.GRAY);
            return;
        }

        String className = args[1];
        String methodName = args[2];
        String signature = CommandArgumentParser.getOptionValue(args, "sig", "signature");

        try {
            int id = manager.addBreakpoint(className, methodName, signature, classLoader);

            context.println("断点已添加", Colors.GREEN);
            context.print("ID: ", Colors.CYAN);
            context.println(String.valueOf(id), Colors.YELLOW);
            context.print("类: ", Colors.CYAN);
            context.println(className, Colors.GREEN);
            context.print("方法: ", Colors.CYAN);
            context.println(methodName, Colors.GREEN);
            context.print("签名: ", Colors.CYAN);
            context.println(signature != null ? signature : "所有重载", Colors.GRAY);
            context.print("状态: ", Colors.CYAN);
            context.println("启用", Colors.GREEN);
            context.println("", Colors.WHITE);
            context.println("断点已设置并生效！", Colors.GREEN);

        } catch (Exception e) {
            Map<String, Object> errorContext = new HashMap<>();
            errorContext.put("类名", className);
            errorContext.put("方法名", methodName);
            errorContext.put("签名", signature != null ? signature : "无");
            CommandExceptionHandler.handleException("breakpoint add", e, context, errorContext, "添加断点失败");
        }
    }

    private void handleList(BreakpointManager manager, CommandExecutor.CmdExecContext context) {
        List<BreakpointInterceptTask> breakpoints = manager.listBreakpoints();
        if (breakpoints.isEmpty()) {
            context.println("没有设置任何断点", Colors.GRAY);
            return;
        }

        context.println("=== 断点列表 ===", Colors.CYAN);
        context.println("", Colors.WHITE);

        for (BreakpointInterceptTask task : breakpoints) {
            context.print("ID: ", Colors.CYAN);
            context.println(String.valueOf(task.getId()), Colors.YELLOW);
            context.print("  类: ", Colors.CYAN);
            context.println(task.getClassName(), Colors.GREEN);
            context.print("  方法: ", Colors.CYAN);
            context.println(task.getMethodName(), Colors.GREEN);
            context.print("  签名: ", Colors.CYAN);
            context.println(task.getSignature() != null ? task.getSignature() : "所有重载", Colors.GRAY);
            context.print("  状态: ", Colors.CYAN);
            context.println(task.isEnabled() ? "启用" : "禁用", task.isEnabled() ? Colors.GREEN : Colors.RED);
            context.print("  命中次数: ", Colors.CYAN);
            context.println(String.valueOf(task.getHitCount()), Colors.YELLOW);
            if (task.getLastHitAt() > 0) {
                context.print("  最后命中: ", Colors.CYAN);
                context.println(String.valueOf(new Date(task.getLastHitAt())), Colors.GRAY);
            }
            context.println("", Colors.WHITE);
        }
    }

    private void handleEnable(String[] args, BreakpointManager manager, CommandExecutor.CmdExecContext context) {
        try {
            CommandArgumentParser.requireArgsLength(args, 2);
        } catch (IllegalArgumentException e) {
            context.println("错误: " + e.getMessage(), Colors.RED);
            context.println("用法: breakpoint enable <id>", Colors.GRAY);
            return;
        }

        Integer id = CommandArgumentParser.parseId(args, 1);
        if (id == null) {
            context.println("错误: 无效的断点ID", Colors.RED);
            return;
        }

        if (manager.enableBreakpoint(id)) {
            context.println("断点已启用", Colors.GREEN);
            context.print("ID: ", Colors.CYAN);
            context.println(String.valueOf(id), Colors.YELLOW);
        } else {
            context.println("错误: 断点不存在", Colors.RED);
        }
    }

    private void handleDisable(String[] args, BreakpointManager manager, CommandExecutor.CmdExecContext context) {
        try {
            CommandArgumentParser.requireArgsLength(args, 2);
        } catch (IllegalArgumentException e) {
            context.println("错误: " + e.getMessage(), Colors.RED);
            context.println("用法: breakpoint disable <id>", Colors.GRAY);
            return;
        }

        Integer id = CommandArgumentParser.parseId(args, 1);
        if (id == null) {
            context.println("错误: 无效的断点ID", Colors.RED);
            return;
        }

        if (manager.disableBreakpoint(id)) {
            context.println("断点已禁用", Colors.YELLOW);
            context.print("ID: ", Colors.CYAN);
            context.println(String.valueOf(id), Colors.YELLOW);
        } else {
            context.println("错误: 断点不存在", Colors.RED);
        }
    }

    private void handleRemove(String[] args, BreakpointManager manager, CommandExecutor.CmdExecContext context) {
        try {
            CommandArgumentParser.requireArgsLength(args, 2);
        } catch (IllegalArgumentException e) {
            context.println("错误: " + e.getMessage(), Colors.RED);
            context.println("用法: breakpoint remove <id>", Colors.GRAY);
            return;
        }

        Integer id = CommandArgumentParser.parseId(args, 1);
        if (id == null) {
            context.println("错误: 无效的断点ID", Colors.RED);
            return;
        }

        if (manager.removeBreakpoint(id)) {
            context.println("断点已移除", Colors.GREEN);
            context.print("ID: ", Colors.CYAN);
            context.println(String.valueOf(id), Colors.YELLOW);
        } else {
            context.println("错误: 断点不存在", Colors.RED);
        }
    }

    private void handleClear(BreakpointManager manager, CommandExecutor.CmdExecContext context) {
        int count = manager.getBreakpointCount();
        manager.clearAll();
        context.println("已清除所有断点", Colors.GREEN);
        context.print("清除数量: ", Colors.CYAN);
        context.println(String.valueOf(count), Colors.YELLOW);
    }

    private void handleHits(BreakpointManager manager, CommandExecutor.CmdExecContext context) {
        List<BreakpointInterceptTask> breakpoints = manager.listBreakpoints();
        if (breakpoints.isEmpty()) {
            context.println("没有设置任何断点", Colors.GRAY);
            return;
        }

        context.println("=== 断点命中统计 ===", Colors.CYAN);
        context.println("", Colors.WHITE);

        int totalHits = 0;
        for (BreakpointInterceptTask task : breakpoints) {
            context.print("ID ", Colors.CYAN);
            context.print(String.valueOf(task.getId()), Colors.YELLOW);
            context.print(": ", Colors.WHITE);
            context.print(task.getClassName() + "." + task.getMethodName(), Colors.GREEN);
            context.print(" - 命中 ", Colors.WHITE);
            context.print(String.valueOf(task.getHitCount()), Colors.YELLOW);
            context.println(" 次", Colors.WHITE);
            totalHits += task.getHitCount();
        }

        context.println("", Colors.WHITE);
        context.print("总计: ", Colors.CYAN);
        context.print(String.valueOf(totalHits), Colors.YELLOW);
        context.println(" 次命中", Colors.WHITE);
    }
}
