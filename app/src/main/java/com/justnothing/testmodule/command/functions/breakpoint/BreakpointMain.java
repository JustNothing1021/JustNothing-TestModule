package com.justnothing.testmodule.command.functions.breakpoint;

import static com.justnothing.testmodule.constants.CommandServer.CMD_BREAKPOINT_VER;

import com.justnothing.testmodule.command.CommandExecutor;
import com.justnothing.testmodule.command.functions.CommandBase;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class BreakpointMain extends CommandBase {

    private static final AtomicInteger nextId = new AtomicInteger(1);
    private static final ConcurrentHashMap<Integer, BreakpointInfo> breakpoints = new ConcurrentHashMap<>();

    public BreakpointMain() {
        super("Breakpoint");
    }

    public static class BreakpointInfo {
        public final int id;
        public final String className;
        public final String methodName;
        public final String signature;
        public volatile boolean enabled;
        public volatile int hitCount;
        public final long createdAt;
        public volatile long lastHitAt;

        public BreakpointInfo(int id, String className, String methodName, String signature) {
            this.id = id;
            this.className = className;
            this.methodName = methodName;
            this.signature = signature;
            this.enabled = true;
            this.hitCount = 0;
            this.createdAt = System.currentTimeMillis();
            this.lastHitAt = 0;
        }
    }

    @Override
    public String getHelpText() {
        return String.format("""
                语法: breakpoint <subcmd> [args...]
                
                设置和管理断点。
                
                子命令:
                    add <class_name> <method_name> [sig/signature <signature>]  - 添加断点
                    list                                                      - 列出所有断点
                    enable <id>                                               - 启用断点
                    disable <id>                                              - 禁用断点
                    remove <id>                                               - 移除断点
                    clear                                                     - 清除所有断点
                    hits                                                      - 显示断点命中统计
                
                选项:
                    sig, signature   - 指定方法签名，如 "String,int" 表示(String, int)参数的方法
                
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
    public String runMain(CommandExecutor.CmdExecContext context) {
        String[] args = context.args();
        ClassLoader classLoader = context.classLoader();
        
        logger.debug("执行breakpoint命令，参数: " + java.util.Arrays.toString(args));
        
        if (args.length < 1) {
            logger.warn("参数不足");
            return getHelpText();
        }

        String subCommand = args[0];

        try {
            switch (subCommand) {
                case "add":
                    return handleAdd(args, classLoader);
                case "list":
                    return handleList();
                case "enable":
                    return handleEnable(args);
                case "disable":
                    return handleDisable(args);
                case "remove":
                    return handleRemove(args);
                case "clear":
                    return handleClear();
                case "hits":
                    return handleHits();
                default:
                    return "未知子命令: " + subCommand + "\n" + getHelpText();
            }
        } catch (Exception e) {
            logger.error("执行breakpoint命令失败", e);
            return "错误: " + e.getMessage();
        }
    }

    private String handleAdd(String[] args, ClassLoader classLoader) {
        if (args.length < 3) {
            return "错误: 参数不足\n用法: breakpoint add <class_name> <method_name> [sig/signature <signature>]";
        }

        String className = args[1];
        String methodName = args[2];
        String signature = null;

        for (int i = 3; i < args.length; i++) {
            if ((args[i].equals("sig") || args[i].equals("signature")) && i + 1 < args.length) {
                signature = args[++i];
                break;
            }
        }

        try {
            Class<?> targetClass = Class.forName(className, false, classLoader);
            int id = nextId.getAndIncrement();
            
            BreakpointInfo info = new BreakpointInfo(id, className, methodName, signature);
            breakpoints.put(id, info);
            
            String sigText = signature != null ? " (签名: " + signature + ")" : " (所有重载)";
            logger.info("添加断点: " + className + "." + methodName + sigText);
            
            return "断点已添加 (ID: " + id + ")\n" +
                    "  类: " + className + "\n" +
                    "  方法: " + methodName + "\n" +
                    "  签名: " + (signature != null ? signature : "所有重载") + "\n" +
                    "  状态: 启用\n" +
                    "\n" +
                    "注意: 断点已设置，但需要Xposed框架支持才能生效。";
            
        } catch (ClassNotFoundException e) {
            logger.error("类未找到: " + className, e);
            return "错误: 类未找到: " + className;
        }
    }

    private String handleList() {
        if (breakpoints.isEmpty()) {
            return "没有设置任何断点";
        }

        StringBuilder result = new StringBuilder();
        result.append("=== 断点列表 ===\n\n");
        
        for (BreakpointInfo info : breakpoints.values()) {
            result.append("ID: ").append(info.id).append("\n");
            result.append("  类: ").append(info.className).append("\n");
            result.append("  方法: ").append(info.methodName).append("\n");
            result.append("  签名: ").append(info.signature != null ? info.signature : "所有重载").append("\n");
            result.append("  状态: ").append(info.enabled ? "启用" : "禁用").append("\n");
            result.append("  命中次数: ").append(info.hitCount).append("\n");
            result.append("  创建时间: ").append(new java.util.Date(info.createdAt)).append("\n");
            if (info.lastHitAt > 0) {
                result.append("  最后命中: ").append(new java.util.Date(info.lastHitAt)).append("\n");
            }
            result.append("\n");
        }
        
        return result.toString();
    }

    private String handleEnable(String[] args) {
        if (args.length < 2) {
            return "错误: 参数不足\n用法: breakpoint enable <id>";
        }

        try {
            int id = Integer.parseInt(args[1]);
            BreakpointInfo info = breakpoints.get(id);
            
            if (info == null) {
                return "错误: 断点不存在 (ID: " + id + ")";
            }
            
            info.enabled = true;
            logger.info("启用断点: " + id);
            return "断点已启用 (ID: " + id + ")";
            
        } catch (NumberFormatException e) {
            return "错误: 无效的断点ID";
        }
    }

    private String handleDisable(String[] args) {
        if (args.length < 2) {
            return "错误: 参数不足\n用法: breakpoint disable <id>";
        }

        try {
            int id = Integer.parseInt(args[1]);
            BreakpointInfo info = breakpoints.get(id);
            
            if (info == null) {
                return "错误: 断点不存在 (ID: " + id + ")";
            }
            
            info.enabled = false;
            logger.info("禁用断点: " + id);
            return "断点已禁用 (ID: " + id + ")";
            
        } catch (NumberFormatException e) {
            return "错误: 无效的断点ID";
        }
    }

    private String handleRemove(String[] args) {
        if (args.length < 2) {
            return "错误: 参数不足\n用法: breakpoint remove <id>";
        }

        try {
            int id = Integer.parseInt(args[1]);
            BreakpointInfo info = breakpoints.remove(id);
            
            if (info == null) {
                return "错误: 断点不存在 (ID: " + id + ")";
            }
            
            logger.info("移除断点: " + id);
            return "断点已移除 (ID: " + id + ")";
            
        } catch (NumberFormatException e) {
            return "错误: 无效的断点ID";
        }
    }

    private String handleClear() {
        int count = breakpoints.size();
        breakpoints.clear();
        nextId.set(1);
        logger.info("清除所有断点");
        return "已清除所有断点 (共 " + count + " 个)";
    }

    private String handleHits() {
        if (breakpoints.isEmpty()) {
            return "没有设置任何断点";
        }

        StringBuilder result = new StringBuilder();
        result.append("=== 断点命中统计 ===\n\n");
        
        int totalHits = 0;
        for (BreakpointInfo info : breakpoints.values()) {
            result.append("ID ").append(info.id).append(": ")
                  .append(info.className).append(".").append(info.methodName)
                  .append(" - 命中 ").append(info.hitCount).append(" 次\n");
            totalHits += info.hitCount;
        }
        
        result.append("\n总计: ").append(totalHits).append(" 次命中\n");
        
        return result.toString();
    }

    public static Map<Integer, BreakpointInfo> getBreakpoints() {
        return new HashMap<>(breakpoints);
    }

    public static void onBreakpointHit(int id) {
        BreakpointInfo info = breakpoints.get(id);
        if (info != null && info.enabled) {
            info.hitCount++;
            info.lastHitAt = System.currentTimeMillis();
            logger.info("断点命中: " + info.className + "." + info.methodName + " (ID: " + id + ")");
            StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
            logger.info("调用栈:");
            for (StackTraceElement element : stackTrace) {
                logger.info("  " + element.toString());
            }
        }
    }

}
