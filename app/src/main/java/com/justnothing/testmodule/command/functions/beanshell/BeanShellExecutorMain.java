package com.justnothing.testmodule.command.functions.beanshell;

import static com.justnothing.testmodule.constants.CommandServer.CMD_BEAN_SHELL_VER;


import com.justnothing.testmodule.command.CommandExecutor;
import com.justnothing.testmodule.command.functions.CommandBase;
import com.justnothing.testmodule.command.output.Colors;
import com.justnothing.testmodule.command.utils.CommandExceptionHandler;
import com.justnothing.testmodule.utils.data.DataBridge;
import com.justnothing.testmodule.utils.io.IOManager;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

public class BeanShellExecutorMain extends CommandBase {

    private static final ConcurrentHashMap<ClassLoader, BeanShellExecutor>
            beanShellExecutors = new ConcurrentHashMap<>();
    private static final BeanShellExecutor systemBeanShellExecutor = new BeanShellExecutor(null);


    private static final Map<String, Object> beanShellExecutionContext = new HashMap<>();

    private final String commandName;


    public BeanShellExecutorMain(String commandName) {
        super("BeanShellExecutor");
        this.commandName = commandName;
    }

    @Override
    public String getHelpText() {
        return switch (commandName) {
            case "bvars" -> String.format("""
                    语法: bvars
                    
                    显示BeanShell执行器的变量列表.
                    
                    示例:
                        bvars
                    
                    (Submodule bsh %s)
                    """, CMD_BEAN_SHELL_VER);
            case "bclear" -> String.format("""
                    语法: bclear
                    
                    清空BeanShell执行器的所有变量.
                    
                    示例:
                        bclear
                    
                    (Submodule bsh %s)
                    """, CMD_BEAN_SHELL_VER);
            case "bscript" -> String.format("""
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
            default -> String.format("""
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
        };
    }
    @Override
    public void runMain(CommandExecutor.CmdExecContext context) {
        String cmdName = context.cmdName();
        String[] args = context.args();
        ClassLoader classLoader = context.classLoader();

        switch (cmdName) {
            case "bvars" -> listVariables(context);
            case "bclear" -> clearVariables(context);
            case "bscript" -> {
                try {
                    handleScriptCommand(args, context);
                } catch (IOException e) {
                    context.output().println("执行脚本时发生错误");
                    context.output().printStackTrace(e);
                }
            }
            default -> {
                if (args.length < 1) {
                    context.println(getHelpText(), Colors.WHITE);
                    return;
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
                    context.println("BeanShell执行器结果:", Colors.CYAN);
                    context.println("", Colors.WHITE);
                    context.println(result, Colors.GRAY);

                } catch (Exception e) {
                    CommandExceptionHandler.handleException(cmdName, e, context, "BeanShell执行出错");
                }
            }
        }
    }

    public void listVariables(CommandExecutor.CmdExecContext context) {
        ClassLoader classLoader = context.classLoader();
        String targetPackage = context.targetPackage();

        context.print("(当前ClassLoader: ", Colors.GRAY);
        context.println((targetPackage == null ? "默认加载器" : targetPackage) + ")", Colors.YELLOW);
        context.println("", Colors.WHITE);
        context.println("BeanShell执行器的变量列表:", Colors.CYAN);
        
        Map<String, Object> bshVars = getBeanShellExecutor(classLoader).getVariables();
        if (bshVars.isEmpty()) {
            context.println("  (空)", Colors.GRAY);
        } else {
            for (Map.Entry<String, Object> entry : bshVars.entrySet()) {
                Object value = entry.getValue();
                context.print("  " + entry.getKey() + " = ", Colors.CYAN);
                context.print(String.valueOf(value), Colors.GREEN);
                context.println(" (" + (value != null ? value.getClass().getSimpleName() : "null") + ")", Colors.GRAY);
            }
        }

    }

    public void clearVariables(CommandExecutor.CmdExecContext context) {
        ClassLoader classLoader = context.classLoader();
        String targetPackage = context.targetPackage();
        getBeanShellExecutor(classLoader).clearVariables();
        context.println("已清空BeanShell执行器的所有变量", Colors.GREEN);
        context.print("提示: 只清空了", Colors.GRAY);
        context.print(targetPackage == null ? "默认" : targetPackage, Colors.YELLOW);
        context.println("的ClassLoader的执行器的变量，其他ClassLoader的并没有被清空", Colors.GRAY);
    }

    private static BeanShellExecutor getBeanShellExecutor(ClassLoader cl) {
        if (cl == null) return systemBeanShellExecutor;
        return beanShellExecutors.computeIfAbsent(cl, BeanShellExecutor::new);
    }

    private void handleScriptCommand(String[] args, CommandExecutor.CmdExecContext context) throws IOException {
        if (args.length < 1) {
            getHelpText();
            return;
        }

        String subCommand = args[0];

        switch (subCommand) {
            case "create" -> handleScriptCreate(args, context);
            case "edit" -> handleScriptEdit(args, context);
            case "list" -> handleScriptList(context);
            case "show" -> handleScriptShow(args, context);
            case "delete" -> handleScriptDelete(args, context);
            case "run" -> handleScriptRun(args, context);
            case "import" -> handleScriptImport(args, context);
            case "export" -> handleScriptExport(args, context);
            default -> {
                context.println("未知子命令: " + subCommand, Colors.RED);
                getHelpText();
            }
        }
    }

    private File getBeanShellScriptFile(String scriptName) {
        File scriptsDir = DataBridge.getScriptsDirectory();
        return new File(scriptsDir, scriptName + ".bsh");
    }

    private void handleScriptCreate(String[] args, CommandExecutor.CmdExecContext context) throws IOException {
        if (args.length < 2) {
            context.println("错误: 需要指定脚本名称", Colors.RED);
            context.println("用法: bscript create <name>", Colors.GRAY);
            return;
        }

        String scriptName = args[1];
        if (!isValidScriptName(scriptName)) {
            context.println("错误: 脚本名称只能包含字母、数字和下划线", Colors.RED);
            return;
        }

        File scriptFile = getBeanShellScriptFile(scriptName);
        if (scriptFile.exists()) {
            context.println("错误: 脚本 '" + scriptName + "' 已存在", Colors.RED);
            return;
        }

        IOManager.createDirectory(Objects.requireNonNull(scriptFile.getParentFile()).getAbsolutePath());

        String content = "# BeanShell Script: " + scriptName + "\n" +
                "# Created by: " + System.currentTimeMillis() + "\n" +
                "\n" +
                "# 在这里编写你的BeanShell脚本代码...\n";
        
        IOManager.writeFile(scriptFile.getAbsolutePath(), content);

        context.println("BeanShell脚本创建成功", Colors.GREEN);
        context.print("名称: ", Colors.CYAN);
        context.println(scriptName, Colors.YELLOW);
        context.print("路径: ", Colors.CYAN);
        context.println(scriptFile.getAbsolutePath(), Colors.GRAY);
        context.println("提示: 使用 'bscript edit " + scriptName + "' 编辑脚本", Colors.GRAY);
    }

    private void handleScriptEdit(String[] args, CommandExecutor.CmdExecContext context) {
        if (args.length < 2) {
            context.println("错误: 需要指定脚本名称", Colors.RED);
            context.println("用法: bscript edit <name>", Colors.GRAY);
            return;
        }

        String scriptName = args[1];
        File scriptFile = getBeanShellScriptFile(scriptName);

        if (!scriptFile.exists()) {
            context.println("错误: 脚本 '" + scriptName + "' 不存在", Colors.RED);
            return;
        }

        context.println("BeanShell脚本已准备好编辑", Colors.GREEN);
        context.print("名称: ", Colors.CYAN);
        context.println(scriptName, Colors.YELLOW);
        context.print("路径: ", Colors.CYAN);
        context.println(scriptFile.getAbsolutePath(), Colors.GRAY);
        context.println("提示: 使用外部编辑器编辑脚本文件", Colors.GRAY);
    }

    private void handleScriptList(CommandExecutor.CmdExecContext context) {
        File scriptsDir = DataBridge.getScriptsDirectory();
        if (!scriptsDir.exists()) {
            context.println("脚本目录不存在: " + scriptsDir.getAbsolutePath(), Colors.RED);
            return;
        }

        File[] scriptFiles = scriptsDir.listFiles((dir, name) -> name.endsWith(".bsh"));

        if (scriptFiles == null || scriptFiles.length == 0) {
            context.println("没有找到BeanShell脚本", Colors.GRAY);
            return;
        }

        context.println("===== BeanShell脚本列表 =====", Colors.CYAN);
        context.println("", Colors.WHITE);

        for (File scriptFile : scriptFiles) {
            String name = scriptFile.getName();
            long size = scriptFile.length();
            long lastModified = scriptFile.lastModified();
            
            context.print("名称: ", Colors.CYAN);
            context.println(name, Colors.GREEN);
            context.print("  大小: ", Colors.CYAN);
            context.println(formatSize(size), Colors.YELLOW);
            context.print("  修改时间: ", Colors.CYAN);
            context.println(formatTime(lastModified), Colors.GRAY);
            context.print("  路径: ", Colors.CYAN);
            context.println(scriptFile.getAbsolutePath(), Colors.GRAY);
            context.println("", Colors.WHITE);
        }

        context.print("总计: ", Colors.CYAN);
        context.println(scriptFiles.length + " 个脚本", Colors.YELLOW);
    }

    private void handleScriptShow(String[] args, CommandExecutor.CmdExecContext context) throws IOException {
        if (args.length < 2) {
            context.println("错误: 需要指定脚本名称", Colors.RED);
            context.println("用法: bscript show <name>", Colors.GRAY);
            return;
        }

        String scriptName = args[1];
        File scriptFile = getBeanShellScriptFile(scriptName);

        if (!scriptFile.exists()) {
            context.println("错误: 脚本 '" + scriptName + "' 不存在", Colors.RED);
            return;
        }

        String content = IOManager.readFile(scriptFile.getAbsolutePath());
        context.println("===== BeanShell脚本内容: " + scriptName + " =====", Colors.CYAN);
        context.println("", Colors.WHITE);
        context.println(content, Colors.GRAY);
    }

    private void handleScriptDelete(String[] args, CommandExecutor.CmdExecContext context) {
        if (args.length < 2) {
            context.println("错误: 需要指定脚本名称", Colors.RED);
            context.println("用法: bscript delete <name>", Colors.GRAY);
            return;
        }

        String scriptName = args[1];
        File scriptFile = getBeanShellScriptFile(scriptName);

        if (!scriptFile.exists()) {
            context.println("错误: 脚本 '" + scriptName + "' 不存在", Colors.RED);
            return;
        }

        if (IOManager.deleteFile(scriptFile.getAbsolutePath())) {
            context.println("BeanShell脚本已删除", Colors.GREEN);
            context.print("名称: ", Colors.CYAN);
            context.println(scriptName, Colors.YELLOW);
        } else {
            context.println("错误: 无法删除脚本 '" + scriptName + "'", Colors.RED);
        }
    }

    private void handleScriptRun(String[] args, CommandExecutor.CmdExecContext context) throws IOException {
        if (args.length < 2) {
            context.println("错误: 需要指定脚本名称", Colors.RED);
            context.println("用法: bscript run <name>", Colors.GRAY);
            return;
        }

        String scriptName = args[1];
        File scriptFile = getBeanShellScriptFile(scriptName);

        if (!scriptFile.exists()) {
            context.println("错误: 脚本 '" + scriptName + "' 不存在", Colors.RED);
            return;
        }

        String content = IOManager.readFile(scriptFile.getAbsolutePath());
        
        context.println("===== 执行BeanShell脚本: " + scriptName + " =====", Colors.CYAN);
        context.println("", Colors.WHITE);
        
        try {
            String execResult = getBeanShellExecutor(context.classLoader()).execute(content, beanShellExecutionContext);
            context.println("执行结果:", Colors.GREEN);
            context.println(execResult, Colors.GRAY);
        } catch (Exception e) {
            CommandExceptionHandler.handleException("bsh", e, context, "BeanShell执行出错");
        }

    }

    private void handleScriptImport(String[] args, CommandExecutor.CmdExecContext context) throws IOException {
        if (args.length < 2) {
            context.println("错误: 需要指定文件路径", Colors.RED);
            context.println("用法: bscript import <file>", Colors.GRAY);
            return;
        }

        String filePath = args[1];
        File sourceFile = new File(filePath);

        if (!sourceFile.exists()) {
            context.println("错误: 文件 '" + filePath + "' 不存在", Colors.RED);
            return;
        }

        String content = IOManager.readFile(sourceFile.getAbsolutePath());
        String scriptName = extractScriptName(sourceFile.getName());
        File destFile = getBeanShellScriptFile(scriptName);

        if (destFile.exists()) {
            context.println("错误: 脚本 '" + scriptName + "' 已存在", Colors.RED);
            context.println("提示: 使用 'bscript delete " + scriptName + "' 删除旧脚本", Colors.GRAY);
            return;
        }

        IOManager.createDirectory(Objects.requireNonNull(destFile.getParentFile()).getAbsolutePath());
        IOManager.writeFile(destFile.getAbsolutePath(), content);

        context.println("BeanShell脚本导入成功", Colors.GREEN);
        context.print("源文件: ", Colors.CYAN);
        context.println(sourceFile.getAbsolutePath(), Colors.GRAY);
        context.print("脚本名称: ", Colors.CYAN);
        context.println(scriptName, Colors.YELLOW);
        context.print("目标路径: ", Colors.CYAN);
        context.println(destFile.getAbsolutePath(), Colors.GRAY);
    }

    private void handleScriptExport(String[] args, CommandExecutor.CmdExecContext context) throws IOException {
        if (args.length < 3) {
            context.println("错误: 需要指定脚本名称和导出路径", Colors.RED);
            context.println("用法: bscript export <name> <file>", Colors.GRAY);
            return;
        }

        String scriptName = args[1];
        String exportPath = args[2];
        File scriptFile = getBeanShellScriptFile(scriptName);

        if (!scriptFile.exists()) {
            context.println("错误: 脚本 '" + scriptName + "' 不存在", Colors.RED);
            return;
        }

        File exportFile = new File(exportPath);
        String content = IOManager.readFile(scriptFile.getAbsolutePath());

        IOManager.writeFile(exportFile.getAbsolutePath(), content);

        context.println("BeanShell脚本导出成功", Colors.GREEN);
        context.print("脚本: ", Colors.CYAN);
        context.println(scriptName, Colors.YELLOW);
        context.print("导出路径: ", Colors.CYAN);
        context.println(exportFile.getAbsolutePath(), Colors.GRAY);
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
            return String.format(Locale.getDefault(), "%.2f KB", bytes / 1024.0);
        } else if (bytes < 1024 * 1024 * 1024) {
            return String.format(Locale.getDefault(), "%.2f MB", bytes / (1024.0 * 1024));
        } else {
            return String.format(Locale.getDefault(), "%.2f GB", bytes / (1024.0 * 1024 * 1024));
        }
    }

    private String formatTime(long timestamp) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        return sdf.format(new Date(timestamp));
    }

}
