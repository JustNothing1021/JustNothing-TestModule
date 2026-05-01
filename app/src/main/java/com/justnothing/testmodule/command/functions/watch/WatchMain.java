package com.justnothing.testmodule.command.functions.watch;

import static com.justnothing.testmodule.constants.CommandServer.CMD_WATCH_VER;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;


import com.justnothing.testmodule.command.base.MainCommand;
import com.justnothing.testmodule.command.CommandExecutor;
import com.justnothing.testmodule.command.base.CommandRequest;
import com.justnothing.testmodule.command.output.Colors;
import com.justnothing.testmodule.command.utils.CommandArgumentParser;
import com.justnothing.testmodule.command.utils.CommandExceptionHandler;

import com.justnothing.testmodule.command.base.RegisterCommand;

@RegisterCommand("watch")
public class WatchMain extends MainCommand<WatchRequest, WatchResult> {

    public WatchMain() {
        super("Watch", WatchResult.class);
    }

    @Override
    public String getHelpText() {
        return String.format("""
                语法: watch <subcmd> [args...]
                
                监控字段或方法的变化，非阻塞执行.
                
                子命令:
                    add field <class_name> <member_name> [interval: ms]  - 添加字段watch任务
                    add method <class_name> <method_name> [sig/signature <signature>] [interval: ms]  - 添加方法watch任务
                    list                                                 - 列出所有watch任务
                    stop <id>                                            - 停止指定watch
                    clear                                                - 清除所有watch
                    output <id> [count]                                  - 获取指定watch的输出
                    output all [count]                                   - 获取所有watch的输出
                
                选项:
                    field            - 监控字段值变化
                    method           - 监控方法调用
                    sig, signature   - 指定方法签名（仅method有效）
                    interval         - 检查间隔（毫秒），默认1000ms
                    count            - 输出条数，默认20条

                关于signature参数:
                    可以是用逗号分割的类名列表，如 "String,int" 表示(String, int)参数的方法
                    也可以是JVM内部格式的签名，如 "(Ljava/lang/String;I;)V" 也是一样
                    （不过因为理论上不会有同参数列表不同返回类型的情况，所以返回类型的解析不会有实际作用）
                
                示例:
                    watch add field java.lang.System out 1000
                    watch add method com.example.MyClass myMethod
                    watch add method com.example.MyClass myMethod signature String 500
                    watch add method com.example.MyClass myMethod sig String,int 500
                    watch list
                    watch output 1
                    watch output all 50
                    watch stop 1
                    watch clear
                
                注意:
                    - 方法监控可能会影响性能
                    - 字段监控只支持静态字段
                    - sig的类名之间用逗号隔开即可，不要有空格（懒得改逻辑了，这样也不是不能用的说）
                    - sig可以指定完整类名或者 java.lang.* 和 java.util.* 下的类名，如"java.lang.String,ArrayList"
                    - 不指定sig时，会监控所有同名方法（包括重载），否则只监控匹配签名的特定方法
                    - watch任务在后台运行，不会阻塞其他命令执行
                    - 每个watch最多保留最近100条输出

                
                (Submodule watch %s)
                """, CMD_WATCH_VER);
    }

    @Override
    public WatchResult runMain(CommandExecutor.CmdExecContext<CommandRequest> context) throws Exception {
        String[] args = context.args();
        ClassLoader classLoader = context.classLoader();
        
        logger.debug("执行watch命令，参数: " + Arrays.toString(args));
        
        if (args.length < 1) {
            logger.warn("参数不足");
            context.println(getHelpText(), Colors.WHITE);
            return null;
        }

        String subCommand = args[0];
        WatchManager manager = WatchManager.getInstance();

        try {
            switch (subCommand) {
                case "add" -> handleAdd(args, classLoader, manager, context);
                case "list" -> handleList(manager, context);
                case "stop" -> handleStop(args, manager, context);
                case "clear" -> handleClear(manager, context);
                case "output" -> handleOutput(args, manager, context);
                default -> {
                    context.println("未知子命令: " + subCommand, Colors.RED);
                    context.println(getHelpText(), Colors.WHITE);
                }
            }
        } catch (Exception e) {
            CommandExceptionHandler.handleException("watch", e, context, "执行watch命令失败");

            if (shouldReturnStructuredData(context)) {
                return createErrorResult("执行watch命令失败: " + e.getMessage());
            }
        }

        if (shouldReturnStructuredData(context)) {
            WatchResult result = new WatchResult(java.util.UUID.randomUUID().toString());
            return result;
        }
        return null;
    }

    private void handleAdd(String[] args, ClassLoader classLoader, WatchManager manager, CommandExecutor.CmdExecContext context) {
        try {
            CommandArgumentParser.requireArgsLength(args, 4);
        } catch (IllegalArgumentException e) {
            context.println("错误: " + e.getMessage(), Colors.RED);
            context.println("用法: watch add <field|method> <class_name> <member_name> [sig/signature <signature>] [interval]", Colors.GRAY);
            return;
        }

        String type = args[1];
        String className = args[2];
        String memberName = args[3];
        String signature = CommandArgumentParser.getOptionValue(args, "sig", "signature");
        long interval = 1000;

        String intervalStr = null;
        for (int i = 4; i < args.length; i++) {
            if (!args[i].equals("sig") && !args[i].equals("signature")) {
                intervalStr = args[i];
                break;
            }
        }

        if (intervalStr != null) {
            try {
                interval = Long.parseLong(intervalStr);
            } catch (NumberFormatException e) {
                context.println("错误: 无效的interval参数", Colors.RED);
                return;
            }
        }

        try {
            CommandArgumentParser.requireMin(interval, 10, "间隔");
        } catch (IllegalArgumentException e) {
            context.println("错误: " + e.getMessage(), Colors.RED);
            context.println("(指定的是" + interval + "ms, 频率过高容易炸掉系统)", Colors.YELLOW);
            return;
        }

        try {
            int id;
            if (type.equals("field")) {
                id = manager.addFieldWatch(classLoader, className, memberName, interval);
                context.println("字段watch任务已添加", Colors.GREEN);
                context.print("ID: ", Colors.CYAN);
                context.println(String.valueOf(id), Colors.YELLOW);
                context.print("类: ", Colors.CYAN);
                context.println(className, Colors.GREEN);
                context.print("字段: ", Colors.CYAN);
                context.println(memberName, Colors.GREEN);
                context.print("间隔: ", Colors.CYAN);
                context.println(interval + "ms", Colors.YELLOW);
                context.println("提示: 使用 'watch output " + id + "' 查看输出", Colors.GRAY);
            } else if (type.equals("method")) {
                id = manager.addMethodWatch(classLoader, className, memberName, signature, interval);
                context.println("方法watch任务已添加", Colors.GREEN);
                context.print("ID: ", Colors.CYAN);
                context.println(String.valueOf(id), Colors.YELLOW);
                context.print("类: ", Colors.CYAN);
                context.println(className, Colors.GREEN);
                context.print("方法: ", Colors.CYAN);
                context.println(memberName, Colors.GREEN);
                if (signature != null) {
                    context.print("签名: ", Colors.CYAN);
                    context.println(signature, Colors.GRAY);
                }
                context.print("间隔: ", Colors.CYAN);
                context.println(interval + "ms", Colors.YELLOW);
                context.println("提示: 使用 'watch output " + id + "' 查看输出", Colors.GRAY);
            } else {
                context.println("错误: 未知类型: " + type + "，必须是 'field' 或 'method'", Colors.RED);
            }
        } catch (Exception e) {
            Map<String, Object> errorContext = new HashMap<>();
            errorContext.put("类型", type);
            errorContext.put("类名", className);
            errorContext.put("成员名", memberName);
            errorContext.put("签名", signature != null ? signature : "无");
            errorContext.put("间隔", interval + "ms");
            CommandExceptionHandler.handleException("watch add", e, context, errorContext, "添加watch任务失败");
        }
    }

    private void handleList(WatchManager manager, CommandExecutor.CmdExecContext context) {
        String result = manager.getTaskListString();
        context.println(result, Colors.WHITE);
    }

    private void handleStop(String[] args, WatchManager manager, CommandExecutor.CmdExecContext context) {
        if (args.length < 2) {
            context.println("错误: 参数不足", Colors.RED);
            context.println("用法: watch stop <id>", Colors.GRAY);
            return;
        }

        try {
            int id = Integer.parseInt(args[1]);
            if (manager.removeTask(id)) {
                context.println("已停止watch任务", Colors.GREEN);
                context.print("ID: ", Colors.CYAN);
                context.println(String.valueOf(id), Colors.YELLOW);
            } else {
                context.println("错误: 未找到watch任务", Colors.RED);
                context.print("ID: ", Colors.CYAN);
                context.println(String.valueOf(id), Colors.YELLOW);
            }
        } catch (NumberFormatException e) {
            context.println("错误: ID必须是数字: " + args[1], Colors.RED);
        }
    }

    private void handleClear(WatchManager manager, CommandExecutor.CmdExecContext context) {
        int count = manager.getTaskCount();
        manager.clearAll();
        context.println("已清除所有watch任务", Colors.GREEN);
        context.print("清除数量: ", Colors.CYAN);
        context.println(String.valueOf(count), Colors.YELLOW);
    }

    private void handleOutput(String[] args, WatchManager manager, CommandExecutor.CmdExecContext context) {
        if (args.length < 2) {
            context.println("错误: 参数不足", Colors.RED);
            context.println("用法: watch output <id|all> [limit]", Colors.GRAY);
            context.println("选项:", Colors.CYAN);
            context.println("  - limit: 输出条数限制，默认20", Colors.GRAY);
            return;
        }

        int limit = 20;
        if (args.length >= 3) {
            try {
                limit = Integer.parseInt(args[2]);
            } catch (NumberFormatException e) {
                context.println("错误: 数量必须是数字: " + args[2], Colors.RED);
                return;
            }
        }

        if (args[1].equals("all")) {
            String result = manager.getAllWatchOutput(limit);
            context.println(result, Colors.WHITE);
        } else {
            try {
                int id = Integer.parseInt(args[1]);
                String result = manager.getTaskOutput(id, limit);
                context.println(result, Colors.WHITE);
            } catch (NumberFormatException e) {
                context.println("错误: ID必须是数字: " + args[1], Colors.RED);
            }
        }
    }
}
