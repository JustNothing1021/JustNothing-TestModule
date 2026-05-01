package com.justnothing.testmodule.command.functions.trace;

import static com.justnothing.testmodule.constants.CommandServer.CMD_TRACE_VER;


import com.justnothing.testmodule.command.CommandExecutor;
import com.justnothing.testmodule.command.base.MainCommand;
import com.justnothing.testmodule.command.base.CommandRequest;
import com.justnothing.testmodule.command.functions.intercept.TraceInterceptTask;
import com.justnothing.testmodule.command.output.Colors;
import com.justnothing.testmodule.command.utils.CommandArgumentParser;
import com.justnothing.testmodule.command.utils.CommandExceptionHandler;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.justnothing.testmodule.command.base.RegisterCommand;

@RegisterCommand("trace")
public class TraceMain extends MainCommand<TraceRequest, TraceResult> {

    public TraceMain() {
        super("Trace", TraceResult.class);
    }

    @Override
    public String getHelpText() {
        return String.format("""
                语法: trace <subcmd> [args...]
                
                跟踪方法调用链，生成调用树，非阻塞执行.
                
                子命令:
                    add <class_name> <method_name> [sig/signature <signature>]  - 添加跟踪任务
                    list                                                 - 列出所有跟踪任务
                    show <id>                                            - 显示调用树
                    export <id> <file>                                   - 导出调用链到文件
                    stop <id>                                            - 停止指定跟踪
                    clear                                                - 清除所有跟踪
                
                选项:
                    sig, signature   - 指定方法签名
                    可以是用逗号分割的类名列表，如 "String,int" 表示(String, int)参数的方法
                    也可以是JVM内部格式的签名，如 "(Ljava/lang/String;I;)V" 也是一样
                    （不过因为理论上不会有同参数列表不同返回类型的情况，所以返回类型的解析不会有实际作用）
                
                示例:
                    trace add com.example.MyClass myMethod
                    trace add com.example.MyClass myMethod signature String,int
                    trace list
                    trace show 1
                    trace export 1 /sdcard/trace_1.txt
                    trace stop 1
                    trace clear
                
                注意:
                    - 跟踪会记录完整调用链，包括调用深度和调用次数
                    - 方法跟踪可能会影响性能
                    - sig的类名之间用逗号隔开即可，不要有空格
                    - sig可以指定完整类名或者 java.lang.* 和 java.util.* 下的类名
                    - 不指定sig时，会跟踪所有同名方法（包括重载），否则只跟踪匹配签名的特定方法
                    - 跟踪任务在后台运行，不会阻塞其他命令执行
                    - 每个跟踪最多保留最近1000条调用记录

                
                (Submodule trace %s)
                """, CMD_TRACE_VER);
    }

    @Override
    public TraceResult runMain(CommandExecutor.CmdExecContext<CommandRequest> context) throws Exception {
        String[] args = context.args();
        ClassLoader classLoader = context.classLoader();
        
        logger.debug("执行trace命令，参数: " + Arrays.toString(args));
        
        if (args.length < 1) {
            logger.warn("参数不足");
            context.println(getHelpText(), Colors.WHITE);
            return null;
        }

        String subCommand = args[0];
        TraceManager manager = TraceManager.getInstance();

        try {
            switch (subCommand) {
                case "add" -> handleAdd(args, classLoader, manager, context);
                case "list" -> handleList(manager, context);
                case "show" -> handleShow(args, manager, context);
                case "export" -> handleExport(args, manager, context);
                case "stop" -> handleStop(args, manager, context);
                case "clear" -> handleClear(manager, context);
                default -> {
                    context.println("未知子命令: " + subCommand, Colors.RED);
                    context.println(getHelpText(), Colors.WHITE);
                }
            }
        } catch (Exception e) {
            CommandExceptionHandler.handleException("trace", e, context, "执行trace命令失败");

            if (shouldReturnStructuredData(context)) {
                return createErrorResult("执行trace命令失败: " + e.getMessage());
            }
        }

        if (shouldReturnStructuredData(context)) {
            return createSuccessResult("跟踪命令执行完成");
        }
        return null;
    }

    private void handleAdd(String[] args, ClassLoader classLoader, TraceManager manager, CommandExecutor.CmdExecContext context) {
        try {
            CommandArgumentParser.requireArgsLength(args, 3);
        } catch (IllegalArgumentException e) {
            context.println("错误: " + e.getMessage(), Colors.RED);
            context.println("用法: trace add <class_name> <method_name> [sig/signature <signature>]", Colors.GRAY);
            return;
        }

        String className = args[1];
        String methodName = args[2];
        String signature = CommandArgumentParser.getOptionValue(args, "sig", "signature");

        try {
            int id = manager.addTraceTask(className, methodName, signature, classLoader);
            context.println("添加trace任务成功", Colors.GREEN);
            context.print("ID: ", Colors.CYAN);
            context.println(String.valueOf(id), Colors.YELLOW);
        } catch (Exception e) {
            Map<String, Object> errorContext = new HashMap<>();
            errorContext.put("类名", className);
            errorContext.put("方法名", methodName);
            errorContext.put("签名", signature != null ? signature : "无");
            CommandExceptionHandler.handleException("trace add", e, context, errorContext, "添加trace任务失败");
        }
    }

    private void handleList(TraceManager manager, CommandExecutor.CmdExecContext context) {
        List<TraceInterceptTask> tasks = manager.listTasks();
        if (tasks.isEmpty()) {
            context.println("没有活跃的trace任务", Colors.GRAY);
            return;
        }

        context.println("活跃的trace任务:", Colors.CYAN);
        context.println("ID\t类名\t方法名\t签名\t状态\t调用次数", Colors.GRAY);
        context.println("--------------------------------------------------", Colors.GRAY);
        for (TraceInterceptTask task : tasks) {
            context.print(String.valueOf(task.getId()), Colors.YELLOW);
            context.print("\t", Colors.WHITE);
            context.print(task.getClassName(), Colors.GREEN);
            context.print("\t", Colors.WHITE);
            context.print(task.getMethodName(), Colors.GREEN);
            context.print("\t", Colors.WHITE);
            context.print(task.getSignature() != null ? task.getSignature() : "所有", Colors.GRAY);
            context.print("\t", Colors.WHITE);
            context.print(task.isRunning() ? "运行中" : "已停止", task.isRunning() ? Colors.GREEN : Colors.RED);
            context.print("\t", Colors.WHITE);
            context.println(String.valueOf(task.getCallCount()), Colors.YELLOW);
        }
    }

    private void handleShow(String[] args, TraceManager manager, CommandExecutor.CmdExecContext context) {
        if (args.length < 2) {
            context.println("错误: 参数不足", Colors.RED);
            context.println("用法: trace show <id>", Colors.GRAY);
            return;
        }

        try {
            int id = Integer.parseInt(args[1]);
            TraceInterceptTask task = manager.getTask(id);
            if (task == null) {
                context.println("错误: 未找到trace任务", Colors.RED);
                context.print("ID: ", Colors.CYAN);
                context.println(String.valueOf(id), Colors.YELLOW);
                return;
            }

            String result = task.getCallTree();
            context.println(result, Colors.WHITE);
        } catch (NumberFormatException e) {
            context.println("错误: 无效的ID: " + args[1], Colors.RED);
        }
    }

    private void handleExport(String[] args, TraceManager manager, CommandExecutor.CmdExecContext context) {
        if (args.length < 3) {
            context.println("错误: 参数不足", Colors.RED);
            context.println("用法: trace export <id> <file>", Colors.GRAY);
            return;
        }

        try {
            int id = Integer.parseInt(args[1]);
            String filePath = args[2];
            TraceInterceptTask task = manager.getTask(id);
            if (task == null) {
                context.println("错误: 未找到trace任务", Colors.RED);
                context.print("ID: ", Colors.CYAN);
                context.println(String.valueOf(id), Colors.YELLOW);
                return;
            }

            boolean success = task.exportToFile(filePath);
            if (success) {
                context.println("导出trace任务成功", Colors.GREEN);
                context.print("文件路径: ", Colors.CYAN);
                context.println(filePath, Colors.GRAY);
            } else {
                context.println("导出trace任务失败", Colors.RED);
            }
        } catch (NumberFormatException e) {
            context.println("错误: 无效的ID: " + args[1], Colors.RED);
        }
    }

    private void handleStop(String[] args, TraceManager manager, CommandExecutor.CmdExecContext context) {
        if (args.length < 2) {
            context.println("错误: 参数不足", Colors.RED);
            context.println("用法: trace stop <id>", Colors.GRAY);
            return;
        }

        try {
            int id = Integer.parseInt(args[1]);
            boolean success = manager.removeTask(id);
            if (success) {
                context.println("停止trace任务成功", Colors.GREEN);
            } else {
                context.println("错误: 未找到trace任务", Colors.RED);
            }
            context.print("ID: ", Colors.CYAN);
            context.println(String.valueOf(id), Colors.YELLOW);
        } catch (NumberFormatException e) {
            context.println("错误: 无效的ID: " + args[1], Colors.RED);
        }
    }

    private void handleClear(TraceManager manager, CommandExecutor.CmdExecContext context) {
        manager.clearAll();
        context.println("清除所有trace任务成功", Colors.GREEN);
    }
}
