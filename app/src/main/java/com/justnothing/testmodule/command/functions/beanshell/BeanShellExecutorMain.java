package com.justnothing.testmodule.command.functions.beanshell;

import static com.justnothing.testmodule.constants.CommandServer.CMD_BEAN_SHELL_VER;

import android.util.Log;

import com.justnothing.testmodule.command.CommandExecutor;
import com.justnothing.testmodule.command.functions.CommandBase;
import com.justnothing.testmodule.utils.data.DataBridge;
import com.justnothing.testmodule.utils.io.IOManager;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class BeanShellExecutorMain extends CommandBase {

    private static final ConcurrentHashMap<ClassLoader, BeanShellExecutor>
            beanShellExecutors = new ConcurrentHashMap<>();
    private static final BeanShellExecutor systemBeanShellExecutor = new BeanShellExecutor(null);


    private static final Map<String, Object> beanShellExecutionContext = new HashMap<>();

    private final String commandName;

    public BeanShellExecutorMain() {
        this("bsh");
    }

    public BeanShellExecutorMain(String commandName) {
        super("BeanShellExecutor");
        this.commandName = commandName;
    }

    @Override
    public String getHelpText() {
        if (commandName.equals("bvars")) {
            return String.format("""
                    语法: bvars
                    
                    显示BeanShell执行器的变量列表.
                    
                    示例:
                        bvars
                    
                    (Submodule bsh %s)
                    """, CMD_BEAN_SHELL_VER);
        } else if (commandName.equals("bclear")) {
            return String.format("""
                    语法: bclear
                    
                    清空BeanShell执行器的所有变量.
                    
                    示例:
                        bclear
                    
                    (Submodule bsh %s)
                    """, CMD_BEAN_SHELL_VER);
        } else if (commandName.equals("bscript")) {
            return String.format("""
                    语法: bscript <subcmd> [args...]
                    
                    BeanShell脚本管理系统.
                    
                    子命令:
                        create <name>              - 创建新脚本
                        edit <name>               - 编辑脚本
                        list                       - 列出所有脚本
                        show <name>               - 显示脚本内容
                        delete <name>             - 删除脚本
                        run <name>                - 执行脚本
                        import <file>            - 导入脚本文件
                        export <name> <file>     - 导出脚本文件
                    
                    示例:
                        bscript create my_script
                        bscript edit my_script
                        bscript list
                        bscript show my_script
                        bscript run my_script
                        bscript import /sdcard/script.bsh
                        bscript export my_script /sdcard/backup.bsh
                        bscript delete my_script
                    
                    (Submodule bsh %s)
                    """, CMD_BEAN_SHELL_VER);
        } else {
            return String.format("""
                    语法: bsh <beanShell code>
                    
                    用BeanShell解释器执行代码.
                    (说实话这个我也不会用, AI推荐给我的, 其实能算是废稿, 但留着好玩)
                    
                    示例:
                      bsh 'a = "114514";'
                      bsh 'println("Hello, World!");'
                      bsh 'for (i = 0; i < 5; i++) { println("i = " + i); }'
                    
                    提示: BeanShell不需要也不能指定类型
                        a = "114514";
                        String a = "114514"; // 爆炸
                    
                    (Submodule bsh %s)
                    """, CMD_BEAN_SHELL_VER);
        }
    }
    @Override
    public String runMain(CommandExecutor.CmdExecContext context) {
        String cmdName = context.cmdName();
        String[] args = context.args();
        ClassLoader classLoader = context.classLoader();
        
        if (cmdName.equals("bvars")) {
            return listVariables(context);
        } else if (cmdName.equals("bclear")) {
            return clearVariables(context);
        } else if (cmdName.equals("bscript")) {
            try {
                return handleScriptCommand(args, context);
            } catch (IOException e) {
                context.output().println("执行脚本时发生错误");
                context.output().printStackTrace(e);
                return "";
            }
        } else {
            if (args.length < 1) {
                return getHelpText();
            }

            try {
                StringBuilder code = new StringBuilder();
                for (int i = 0; i < args.length; i++) {
                    code.append(args[i]);
                    if (i < args.length - 1) {
                        code.append(" ");
                    }
                }
                logger.info("执行BeanShell代码: " + code);
                String result = getBeanShellExecutor(classLoader).execute(code.toString(), beanShellExecutionContext);
                return "BeanShell执行器结果:\n\n" + result;

            } catch (Exception e) {
                return "BeanShell执行出错: " + e.getMessage() + "\n堆栈追踪: \n" + Log.getStackTraceString(e);
            }
        }
    }

    public String listVariables(CommandExecutor.CmdExecContext context) {
        ClassLoader classLoader = context.classLoader();
        String targetPackage = context.targetPackage();

        StringBuilder sb = new StringBuilder("(当前ClassLoader: " + (targetPackage == null ? "默认加载器" : targetPackage) + ")");

        sb.append("\n\nBeanShell执行器的变量列表:\n");
        Map<String, Object> bshVars = getBeanShellExecutor(classLoader).getVariables();
        if (bshVars.isEmpty()) {
            sb.append("  (空)\n");
        } else {
            for (Map.Entry<String, Object> entry : bshVars.entrySet()) {
                Object value = entry.getValue();
                sb.append("  ").append(entry.getKey())
                        .append(" = ").append(value)
                        .append(" (").append(value != null ? value.getClass().getSimpleName() : "null")
                        .append(")\n");
            }
        }

        return sb.toString();
    }

    public String clearVariables(CommandExecutor.CmdExecContext context) {
        ClassLoader classLoader = context.classLoader();
        String targetPackage = context.targetPackage();
        getBeanShellExecutor(classLoader).clearVariables();
        return "已清空BeanShell执行器的所有变量\n提示: 只清空了" +
                (targetPackage == null ? "默认" : targetPackage) + "的ClassLoader的执行器的变量，其他ClassLoader的并没有被清空";
    }

    private static BeanShellExecutor getBeanShellExecutor(ClassLoader cl) {
        if (cl == null) return systemBeanShellExecutor;
        return beanShellExecutors.computeIfAbsent(cl, BeanShellExecutor::new);
    }

    private String handleScriptCommand(String[] args, CommandExecutor.CmdExecContext context) throws IOException {
        if (args.length < 1) {
            return getHelpText();
        }

        String subCommand = args[0];

        switch (subCommand) {
            case "create":
                return handleScriptCreate(args);
            case "edit":
                return handleScriptEdit(args);
            case "list":
                return handleScriptList();
            case "show":
                return handleScriptShow(args);
            case "delete":
                return handleScriptDelete(args);
            case "run":
                return handleScriptRun(args, context);
            case "import":
                return handleScriptImport(args);
            case "export":
                return handleScriptExport(args);
            default:
                return "未知子命令: " + subCommand + "\n" + getHelpText();
        }
    }

    private File getBeanShellScriptFile(String scriptName) {
        File scriptsDir = DataBridge.getScriptsDirectory();
        return new File(scriptsDir, scriptName + ".bsh");
    }

    private String handleScriptCreate(String[] args) throws IOException {
        if (args.length < 2) {
            return "错误: 需要指定脚本名称\n用法: bscript create <name>";
        }

        String scriptName = args[1];
        if (!isValidScriptName(scriptName)) {
            return "错误: 脚本名称只能包含字母、数字和下划线";
        }

        File scriptFile = getBeanShellScriptFile(scriptName);
        if (scriptFile.exists()) {
            return "错误: 脚本 '" + scriptName + "' 已存在";
        }

        scriptFile.getParentFile().mkdirs();
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(scriptFile))) {
            writer.write("# BeanShell Script: " + scriptName);
            writer.newLine();
            writer.write("# Created by: " + System.currentTimeMillis());
            writer.newLine();
            writer.newLine();
            writer.write("# 在这里编写你的BeanShell脚本代码...");
            writer.newLine();
        }

        return "BeanShell脚本 '" + scriptName + "' 创建成功\n" +
               "路径: " + scriptFile.getAbsolutePath() + "\n" +
               "提示: 使用 'bscript edit " + scriptName + "' 编辑脚本";
    }

    private String handleScriptEdit(String[] args) throws IOException {
        if (args.length < 2) {
            return "错误: 需要指定脚本名称\n用法: bscript edit <name>";
        }

        String scriptName = args[1];
        File scriptFile = getBeanShellScriptFile(scriptName);

        if (!scriptFile.exists()) {
            return "错误: 脚本 '" + scriptName + "' 不存在";
        }

        return "BeanShell脚本 '" + scriptName + "' 已准备好编辑\n" +
               "路径: " + scriptFile.getAbsolutePath() + "\n" +
               "提示: 使用外部编辑器编辑脚本文件";
    }

    private String handleScriptList() throws IOException {
        File scriptsDir = DataBridge.getScriptsDirectory();
        if (!scriptsDir.exists()) {
            return "脚本目录不存在: " + scriptsDir.getAbsolutePath();
        }

        File[] scriptFiles = scriptsDir.listFiles((dir, name) -> name.endsWith(".bsh"));

        if (scriptFiles == null || scriptFiles.length == 0) {
            return "没有找到BeanShell脚本";
        }

        StringBuilder result = new StringBuilder();
        result.append("===== BeanShell脚本列表 =====\n\n");

        for (File scriptFile : scriptFiles) {
            String name = scriptFile.getName();
            long size = scriptFile.length();
            long lastModified = scriptFile.lastModified();
            
            result.append("名称: ").append(name).append("\n");
            result.append("  大小: ").append(formatSize(size)).append("\n");
            result.append("  修改时间: ").append(formatTime(lastModified)).append("\n");
            result.append("  路径: ").append(scriptFile.getAbsolutePath()).append("\n\n");
        }

        result.append("总计: ").append(scriptFiles.length).append(" 个脚本");
        return result.toString();
    }

    private String handleScriptShow(String[] args) throws IOException {
        if (args.length < 2) {
            return "错误: 需要指定脚本名称\n用法: bscript show <name>";
        }

        String scriptName = args[1];
        File scriptFile = getBeanShellScriptFile(scriptName);

        if (!scriptFile.exists()) {
            return "错误: 脚本 '" + scriptName + "' 不存在";
        }

        String content = IOManager.readFile(scriptFile.getAbsolutePath());
        return "===== BeanShell脚本内容: " + scriptName + " =====\n\n" + content;
    }

    private String handleScriptDelete(String[] args) throws IOException {
        if (args.length < 2) {
            return "错误: 需要指定脚本名称\n用法: bscript delete <name>";
        }

        String scriptName = args[1];
        File scriptFile = getBeanShellScriptFile(scriptName);

        if (!scriptFile.exists()) {
            return "错误: 脚本 '" + scriptName + "' 不存在";
        }

        if (scriptFile.delete()) {
            return "BeanShell脚本 '" + scriptName + "' 已删除";
        } else {
            return "错误: 无法删除脚本 '" + scriptName + "'";
        }
    }

    private String handleScriptRun(String[] args, CommandExecutor.CmdExecContext context) throws IOException {
        if (args.length < 2) {
            return "错误: 需要指定脚本名称\n用法: bscript run <name>";
        }

        String scriptName = args[1];
        File scriptFile = getBeanShellScriptFile(scriptName);

        if (!scriptFile.exists()) {
            return "错误: 脚本 '" + scriptName + "' 不存在";
        }

        String content = IOManager.readFile(scriptFile.getAbsolutePath());
        
        StringBuilder result = new StringBuilder();
        result.append("===== 执行BeanShell脚本: ").append(scriptName).append(" =====\n\n");
        
        try {
            String execResult = getBeanShellExecutor(context.classLoader()).execute(content, beanShellExecutionContext);
            result.append("执行结果:\n").append(execResult);
        } catch (Exception e) {
            result.append("脚本执行失败: ").append(e.getMessage());
            result.append("\n堆栈追踪:\n").append(Log.getStackTraceString(e));
        }
        
        return result.toString();
    }

    private String handleScriptImport(String[] args) throws IOException {
        if (args.length < 2) {
            return "错误: 需要指定文件路径\n用法: bscript import <file>";
        }

        String filePath = args[1];
        File sourceFile = new File(filePath);

        if (!sourceFile.exists()) {
            return "错误: 文件 '" + filePath + "' 不存在";
        }

        String content = IOManager.readFile(sourceFile.getAbsolutePath());
        String scriptName = extractScriptName(sourceFile.getName());
        File destFile = getBeanShellScriptFile(scriptName);

        if (destFile.exists()) {
            return "错误: 脚本 '" + scriptName + "' 已存在\n" +
                   "提示: 使用 'bscript delete " + scriptName + "' 删除旧脚本";
        }

        destFile.getParentFile().mkdirs();
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(destFile))) {
            writer.write(content);
        }

        return "BeanShell脚本导入成功\n" +
               "源文件: " + sourceFile.getAbsolutePath() + "\n" +
               "脚本名称: " + scriptName + "\n" +
               "目标路径: " + destFile.getAbsolutePath();
    }

    private String handleScriptExport(String[] args) throws IOException {
        if (args.length < 3) {
            return "错误: 需要指定脚本名称和导出路径\n用法: bscript export <name> <file>";
        }

        String scriptName = args[1];
        String exportPath = args[2];
        File scriptFile = getBeanShellScriptFile(scriptName);

        if (!scriptFile.exists()) {
            return "错误: 脚本 '" + scriptName + "' 不存在";
        }

        File exportFile = new File(exportPath);
        String content = IOManager.readFile(scriptFile.getAbsolutePath());

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(exportFile))) {
            writer.write(content);
        }

        return "BeanShell脚本导出成功\n" +
               "脚本: " + scriptName + "\n" +
               "导出路径: " + exportFile.getAbsolutePath();
    }

    private boolean isValidScriptName(String name) {
        return name != null && name.matches("^[a-zA-Z0-9_]+$");
    }

    private String extractScriptName(String fileName) {
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex > 0) {
            return fileName.substring(0, dotIndex);
        }
        return fileName;
    }

    private String formatSize(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        } else if (bytes < 1024 * 1024) {
            return String.format("%.2f KB", bytes / 1024.0);
        } else if (bytes < 1024 * 1024 * 1024) {
            return String.format("%.2f MB", bytes / (1024.0 * 1024));
        } else {
            return String.format("%.2f GB", bytes / (1024.0 * 1024 * 1024));
        }
    }

    private String formatTime(long timestamp) {
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return sdf.format(new java.util.Date(timestamp));
    }

}
