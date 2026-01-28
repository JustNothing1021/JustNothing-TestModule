package com.justnothing.testmodule.command.functions.trace;

import static com.justnothing.testmodule.constants.CommandServer.CMD_TRACE_VER;

import android.util.Log;

import com.justnothing.testmodule.command.CommandExecutor;
import com.justnothing.testmodule.command.functions.CommandBase;

public class TraceMain extends CommandBase {

    public TraceMain() {
        super("TraceMain");
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
                    sig, signature   - 指定方法签名，如 "String,int" 表示(String, int)参数的方法
                
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
    public String runMain(CommandExecutor.CmdExecContext context) {
        String[] args = context.args();
        ClassLoader classLoader = context.classLoader();
        
        logger.debug("执行trace命令，参数: " + java.util.Arrays.toString(args));
        
        if (args.length < 1) {
            logger.warn("参数不足");
            return getHelpText();
        }

        String subCommand = args[0];
        TraceManager manager = TraceManager.getInstance();

        try {
            return switch (subCommand) {
                case "add" -> handleAdd(args, classLoader, manager);
                case "list" -> handleList(manager);
                case "show" -> handleShow(args, manager);
                case "export" -> handleExport(args, manager);
                case "stop" -> handleStop(args, manager);
                case "clear" -> handleClear(manager);
                default -> "未知子命令: " + subCommand + "\n" + getHelpText();
            };
        } catch (Exception e) {
            logger.error("执行trace命令失败", e);
            return "错误: " + e.getMessage() +
                    "\n堆栈追踪: \n" + Log.getStackTraceString(e);
        }
    }

    private String handleAdd(String[] args, ClassLoader classLoader, TraceManager manager) {
        if (args.length < 3) {
            return "错误: 参数不足\n用法: trace add <class_name> <method_name> [sig/signature <signature>]";
        }

        String className = args[1];
        String methodName = args[2];
        String signature = null;

        for (int i = 3; i < args.length; i++) {
            if (args[i].equals("sig") || args[i].equals("signature")) {
                if (i + 1 < args.length) {
                    signature = args[i + 1];
                    i++;
                } else {
                    return "错误: signature需要指定参数类型";
                }
            }
        }

        try {
            int id = manager.addTraceTask(className, methodName, signature, classLoader);
            return "添加trace任务成功，ID: " + id;
        } catch (Exception e) {
            logger.error("添加trace任务失败", e);
            return "添加trace任务失败: " + e.getMessage();
        }
    }

    private String handleList(TraceManager manager) {
        java.util.List<TraceTask> tasks = manager.listTasks();
        if (tasks.isEmpty()) {
            return "没有活跃的trace任务";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("活跃的trace任务:\n");
        sb.append("ID\t类名\t方法名\t签名\t状态\t调用次数\n");
        sb.append("--------------------------------------------------\n");
        for (TraceTask task : tasks) {
            sb.append(task.getId()).append("\t")
              .append(task.getClassName()).append("\t")
              .append(task.getMethodName()).append("\t")
              .append(task.getSignature() != null ? task.getSignature() : "所有").append("\t")
              .append(task.isRunning() ? "运行中" : "已停止").append("\t")
              .append(task.getCallCount()).append("\n");
        }
        return sb.toString();
    }

    private String handleShow(String[] args, TraceManager manager) {
        if (args.length < 2) {
            return "错误: 参数不足\n用法: trace show <id>";
        }

        try {
            int id = Integer.parseInt(args[1]);
            TraceTask task = manager.getTask(id);
            if (task == null) {
                return "错误: 未找到ID为 " + id + " 的trace任务";
            }

            return task.getCallTree();
        } catch (NumberFormatException e) {
            return "错误: 无效的ID: " + args[1];
        }
    }

    private String handleExport(String[] args, TraceManager manager) {
        if (args.length < 3) {
            return "错误: 参数不足\n用法: trace export <id> <file>";
        }

        try {
            int id = Integer.parseInt(args[1]);
            String filePath = args[2];
            TraceTask task = manager.getTask(id);
            if (task == null) {
                return "错误: 未找到ID为 " + id + " 的trace任务";
            }

            boolean success = task.exportToFile(filePath);
            if (success) {
                return "导出trace任务成功: " + filePath;
            } else {
                return "导出trace任务失败";
            }
        } catch (NumberFormatException e) {
            return "错误: 无效的ID: " + args[1];
        }
    }

    private String handleStop(String[] args, TraceManager manager) {
        if (args.length < 2) {
            return "错误: 参数不足\n用法: trace stop <id>";
        }

        try {
            int id = Integer.parseInt(args[1]);
            boolean success = manager.stopTask(id);
            if (success) {
                return "停止trace任务成功，ID: " + id;
            } else {
                return "错误: 未找到ID为 " + id + " 的trace任务";
            }
        } catch (NumberFormatException e) {
            return "错误: 无效的ID: " + args[1];
        }
    }

    private String handleClear(TraceManager manager) {
        manager.clearAllTasks();
        return "清除所有trace任务成功";
    }
}
