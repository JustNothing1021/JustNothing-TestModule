package com.justnothing.testmodule.command.functions.watch;

import static com.justnothing.testmodule.constants.CommandServer.CMD_WATCH_VER;

import android.util.Log;

import com.justnothing.testmodule.command.CommandExecutor;
import com.justnothing.testmodule.command.functions.CommandBase;

public class WatchMain extends CommandBase {

    public WatchMain() {
        super("WatchMain");
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
                    sig, signature   - 指定方法签名（仅method有效），如 "String,int" 表示(String, int)参数的方法
                    interval         - 检查间隔（毫秒），默认1000ms
                    count            - 输出条数，默认20条

                
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
    public String runMain(CommandExecutor.CmdExecContext context) {
        String[] args = context.args();
        ClassLoader classLoader = context.classLoader();
        
        logger.debug("执行watch命令，参数: " + java.util.Arrays.toString(args));
        
        if (args.length < 1) {
            logger.warn("参数不足");
            return getHelpText();
        }

        String subCommand = args[0];
        WatchManager manager = WatchManager.getInstance();

        try {
            switch (subCommand) {
                case "add":
                    return handleAdd(args, classLoader, manager);
                case "list":
                    return handleList(manager);
                case "stop":
                    return handleStop(args, manager);
                case "clear":
                    return handleClear(manager);
                case "output":
                    return handleOutput(args, manager);
                default:
                    return "未知子命令: " + subCommand + "\n" + getHelpText();
            }
        } catch (Exception e) {
            logger.error("执行watch命令失败", e);
            return "错误: " + e.getMessage() +
                    "\n堆栈追踪: \n" + Log.getStackTraceString(e);
        }
    }

    private String handleAdd(String[] args, ClassLoader classLoader, WatchManager manager) {
        if (args.length < 4) {
            return "参数不足\n用法: watch add <field|method> <class_name> <member_name> [sig/signature <signature>] [interval]\n" + getHelpText();
        }

        String type = args[1];
        String className = args[2];
        String memberName = args[3];
        String signature = null;
        long interval = 1000;

        int i = 4;
        while (i < args.length) {
            if (args[i].equals("sig") || args[i].equals("signature")) {
                if (i + 1 < args.length) {
                    signature = args[i + 1];
                    i += 2;
                } else {
                    return "sig参数需要指定签名\n用法: watch add <field|method> <class_name> <member_name> [sig/signature <signature>] [interval]";
                }
            } else {
                try {
                    interval = Long.parseLong(args[i]);
                    i++;
                } catch (NumberFormatException e) {
                    return "无效参数: " + args[i];
                }
            }
        }

        if (interval < 10) {
            return "间隔不能小于10ms (指定的是" + interval + "ms, 频率过高容易炸掉系统)";
        }

        try {
            int id;
            if (type.equals("field")) {
                id = manager.addFieldWatch(classLoader, className, memberName, interval);
                return "字段watch任务已添加\n" +
                       "ID: " + id + "\n" +
                       "类: " + className + "\n" +
                       "字段: " + memberName + "\n" +
                       "间隔: " + interval + "ms\n" +
                       "提示: 使用 'watch output " + id + "' 查看输出";
            } else if (type.equals("method")) {
                id = manager.addMethodWatch(classLoader, className, memberName, signature, interval);
                return "方法watch任务已添加\n" +
                       "ID: " + id + "\n" +
                       "类: " + className + "\n" +
                       "方法: " + memberName + "\n" +
                       (signature != null ? "签名: " + signature + "\n" : "") +
                       "间隔: " + interval + "ms\n" +
                       "提示: 使用 'watch output " + id + "' 查看输出";
            } else {
                return "未知类型: " + type + "，必须是 'field' 或 'method'";
            }
        } catch (Exception e) {
            logger.error("添加watch任务失败", e);
            StringBuilder sb = new StringBuilder();
            sb.append("添加watch任务失败: ").append(e.getClass().getSimpleName()).append(": ").append(e.getMessage()).append("\n");
            
            if (e.getCause() != null) {
                sb.append("原因: ").append(e.getCause().getClass().getSimpleName()).append(": ").append(e.getCause().getMessage()).append("\n");
            }
            
            sb.append("\n堆栈追踪:\n").append(Log.getStackTraceString(e));
            return sb.toString();
        }
    }

    private String handleList(WatchManager manager) {
        return manager.listWatches();
    }

    private String handleStop(String[] args, WatchManager manager) {
        if (args.length < 2) {
            return "参数不足\n用法: watch stop <id>\n" + getHelpText();
        }

        try {
            int id = Integer.parseInt(args[1]);
            if (manager.stopWatch(id)) {
                return "已停止watch任务: " + id;
            } else {
                return "未找到watch任务: " + id;
            }
        } catch (NumberFormatException e) {
            return "ID必须是数字: " + args[1];
        }
    }

    private String handleClear(WatchManager manager) {
        int count = manager.getWatchCount();
        manager.clearAll();
        return "已清除所有watch任务，共 " + count + " 个";
    }

    private String handleOutput(String[] args, WatchManager manager) {
        if (args.length < 2) {
            return  """
                    参数不足
                    
                    用法: watch output <id|all> [limit]

                    选项:
                        - limit: 输出条数限制，默认20
                    """ + getHelpText();
        }

        int limit = 20;
        if (args.length >= 3) {
            try {
                limit = Integer.parseInt(args[2]);
            } catch (NumberFormatException e) {
                return "数量必须是数字: " + args[2];
            }
        }

        if (args[1].equals("all")) {
            return manager.getAllWatchOutput(limit);
        } else {
            try {
                int id = Integer.parseInt(args[1]);
                return manager.getWatchOutput(id, limit);
            } catch (NumberFormatException e) {
                return "ID必须是数字: " + args[1];
            }
        }
    }
}
