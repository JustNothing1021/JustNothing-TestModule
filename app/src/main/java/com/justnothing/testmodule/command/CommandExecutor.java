package com.justnothing.testmodule.command;

import static com.justnothing.testmodule.constants.CommandServer.MAIN_MODULE_VER;
import com.justnothing.testmodule.command.functions.analyze.AnalyzeMain;
import com.justnothing.testmodule.command.functions.beanshell.BeanShellExecutorMain;
import com.justnothing.testmodule.command.functions.classcmd.ClassMain;
import com.justnothing.testmodule.command.functions.exportcontext.ExportContextMain;
import com.justnothing.testmodule.command.functions.fieldcmd.FieldMain;
import com.justnothing.testmodule.command.functions.interactive.InteractiveExampleMain;
import com.justnothing.testmodule.command.functions.invoke.InvokeMethodMain;
import com.justnothing.testmodule.command.functions.list.ListMethodsMain;
import com.justnothing.testmodule.command.functions.output.OutputExampleMain;
import com.justnothing.testmodule.command.functions.script.ScriptExecutorMain;
import com.justnothing.testmodule.command.functions.watch.WatchMain;
import com.justnothing.testmodule.command.output.StringBuilderCollector;
import com.justnothing.testmodule.command.output.IOutputHandler;
import com.justnothing.testmodule.command.output.SystemOutputRedirector;
import com.justnothing.testmodule.utils.data.ClassLoaderManager;
import com.justnothing.testmodule.utils.functions.Logger;


import java.util.*;

public class CommandExecutor {
    private ClassLoaderManager classLoaderManager;
    private volatile boolean initialized = false;

    private String targetPackage = null;
    private ClassLoader currentClassLoader = null;

    public static class CmdExcLogger extends Logger {
        @Override
        public String getTag() {
            return "CommandExecutor";
        }
    }

    public static final CmdExcLogger logger = new CmdExcLogger();

    public CommandExecutor() {
        this.classLoaderManager = new ClassLoaderManager();
    }

    private void initializeIfNeeded() {
        if (!initialized) {
            synchronized (this) {
                if (!initialized) {
                    this.classLoaderManager = new ClassLoaderManager();
                    initialized = true;
                }
            }
        }
    }

    public void setTargetPackage(String pkgName) {
        this.targetPackage = pkgName;
        this.currentClassLoader = null;
    }

    private ClassLoader getClassLoader() {
        if (currentClassLoader != null) {
            return currentClassLoader;
        }
        if (targetPackage == null || targetPackage.equals("default") || targetPackage.isEmpty()) {
            currentClassLoader = Thread.currentThread().getContextClassLoader();
            if (currentClassLoader == null) {
                currentClassLoader = getClass().getClassLoader();
            }
            return currentClassLoader;
        }
        currentClassLoader = classLoaderManager.getClassLoaderForPackage(targetPackage);
        return currentClassLoader;
    }


    public void execute(String fullCommand, IOutputHandler output) {
        if (fullCommand == null || fullCommand.trim().isEmpty()) {
            logger.warn("命令为空");
            output.println("命令不能为空");
            return;
        }
        if (output == null) {
            logger.error("输出处理器为null");
            throw new IllegalArgumentException("输出处理器不能为null");
        }

        initializeIfNeeded();
        logger.info("开始执行命令: " + fullCommand);
        try {
            SystemOutputRedirector redirector = new SystemOutputRedirector(output);
            redirector.startRedirect();
            try {
                executeCommandInternal(fullCommand, output);
            } finally {
                redirector.stopRedirect();
                output.close();
            }
        } catch (Exception e) {
            logger.error("执行命令异常", e);
            output.println("\n===============================================");
            output.println("执行命令出现严重错误...");
            output.println("错误信息:");
            output.printStackTrace(e);
            output.println("===============================================");
        }
        logger.info("命令执行完成");
    }

    /**
     * 内部命令执行逻辑
     */
    private void executeCommandInternal(String fullCommand, IOutputHandler output) {
        String[] args = parseCommandLine(fullCommand);

        if (args.length == 0) {
            output.println("没有指定命令 (可以用help来获取帮助)");
            return;
        }

        Map<String, String> options = parseOptions(args);
        List<String> commandArgsList = new ArrayList<>();

        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            if (arg.startsWith("-")) {
                if ((arg.equals("-cl") || arg.equals("--classloader")) && i + 1 < args.length) {
                    i++;
                    setTargetPackage(args[i]);
                }
                continue;
            }
            commandArgsList.add(arg);
        }

        String[] commandArgs = commandArgsList.toArray(new String[0]);

        if (commandArgs.length == 0) {
            output.println("没有指定命令 (可以用help来获取帮助)");
            return;
        }

        String command = commandArgs[0];
        String[] commandParams = new String[Math.max(0, commandArgs.length - 1)];
        if (commandArgs.length > 1) {
            System.arraycopy(commandArgs, 1, commandParams, 0, commandParams.length);
        }

        if (options.containsKey("-cl") || options.containsKey("--classloader")) {
            String pkgName = options.getOrDefault("-cl", options.get("--classloader"));
            setTargetPackage(pkgName);
            output.printf("[INFO] 使用ClassLoader: %s\n", pkgName);
        }

        // 创建执行上下文
        CmdExecContext context = new CmdExecContext(
                commandParams,
                targetPackage,
                getClassLoader(),
                output
        );


        try {
            // 分发命令
            switch (command) {
                case "invoke":
                    invokeMethod(context);
                    break;
                case "list":
                    listMethods(context);
                    break;
                case "analyze":
                    analyzeClass(context);
                    break;
                case "class":
                    showClass(context);
                    break;
                case "field":
                    showField(context);
                    break;
                case "watch":
                    watch(context);
                    break;
                case "export-context":
                    exportContext(context);
                    break;
                case "packages":
                    listPackages(context);
                    break;
                case "help":
                    showHelp(output);
                    break;
                case "bsh":
                    executeBeanShell(context);
                    break;
                case "bvars":
                    listBeanShellVariables(context);
                    break;
                case "bclear":
                    clearBeanShellVariables(context);
                    break;
                case "script":
                    executeScript(context);
                    break;
                case "sclear":
                    clearScriptVariables(context);
                    break;
                case "output_test":
                    testOutput(context);
                    break;
                case "interactive_test":
                    executeInteractiveExample(context);
                    break;
                default:
                    output.println("Unknown command: " + command + ", type help for help");
                    break;
            }
        } catch (Exception e) {
            output.println("\n===============================================");
            output.println("执行命令出现错误...");
            output.println("错误信息:");
            output.printStackTrace(e);
            output.println("===============================================");
        }

    }

    public String executeShellCommand(String fullCommand) {
        StringBuilderCollector collector = new StringBuilderCollector();
        execute(fullCommand, collector);
        return collector.getString();
    }


    public record CmdExecContext(
            String[] args,
            String targetPackage,
            ClassLoader classLoader,
            IOutputHandler output
    ) {

        public void print(Object obj) {
            output.print(obj.toString());
        }
        public void println(Object obj) {
            output.println(obj.toString());
        }
        public void printf(String fmt, Object... args) {
            output.printf(fmt, args);
        }
        public String readLine(String prompt) {
            return output.readLineFromClient(prompt);
        }
        public String readPassword(String prompt) {
            return output.readPasswordFromClient(prompt);
        }

    }

    public void invokeMethod(CmdExecContext context) {
        context.output().println(InvokeMethodMain.runMain(context));
    }

    public void listMethods(CmdExecContext context) {
        context.output().println(ListMethodsMain.runMain(context));
    }

    public void analyzeClass(CmdExecContext context) {
        context.output().println(AnalyzeMain.runMain(context));
    }

    public void showClass(CmdExecContext context) {
        context.output().println(ClassMain.runMain(context));
    }

    public void showField(CmdExecContext context) {
        context.output().println(FieldMain.runMain(context));
    }

    public void watch(CmdExecContext context) {
        context.output().println(WatchMain.runMain(context));
    }

    public void exportContext(CmdExecContext context) {
        context.output().println(ExportContextMain.runMain(context));
    }

    private void executeBeanShell(CmdExecContext context) {
        context.output().println(BeanShellExecutorMain.runMain(context));
    }

    private void executeScript(CmdExecContext context) {
        context.output().println(ScriptExecutorMain.runMain(context));
    }

    private void listBeanShellVariables(CmdExecContext context) {
        context.output().println(BeanShellExecutorMain.listVariables(context));
    }

    private void clearBeanShellVariables(CmdExecContext context) {
        context.output().println(BeanShellExecutorMain.clearVariables(context));
    }

    private void clearScriptVariables(CmdExecContext context) {
        context.output().println(ScriptExecutorMain.clearExecutorVariables(context));
    }

    private void listPackages(CmdExecContext context) {
        IOutputHandler output = context.output();
        List<String> packages = ClassLoaderManager.getAllKnownPackages();
        output.printf("当前进程的ClassLoader (总计%d个):\n", packages.size());
        for (String pkg : packages) {
            output.println("  " + pkg);
        }
        output.println("\n提示: 用 'methods -cl <package> <command>' 可以指定ClassLoader");
    }

    private void testOutput(CmdExecContext context)  {
        context.output().println(OutputExampleMain.runMain(context));
    }

    private void executeInteractiveExample(CmdExecContext context) {
        context.output().println(InteractiveExampleMain.runMain(context));
    }

    private void showHelp(IOutputHandler output) {
        output.print(getHelpText());
    }

    private String[] parseCommandLine(String commandLine) {
        List<String> args = new ArrayList<>();
        StringBuilder currentArg = new StringBuilder();
        boolean inQuotes = false;
        boolean escapeNext = false;
        
        for (int i = 0; i < commandLine.length(); i++) {
            char c = commandLine.charAt(i);
            
            if (escapeNext) {
                currentArg.append(c);
                escapeNext = false;
                continue;
            }
            
            if (c == '\\') {
                escapeNext = true;
                continue;
            }
            
            if (c == '"') {
                inQuotes = !inQuotes;
                continue;
            }
            
            if (c == ' ' && !inQuotes) {
                if (currentArg.length() > 0) {
                    args.add(currentArg.toString());
                    currentArg = new StringBuilder();
                }
            } else {
                currentArg.append(c);
            }
        }

        if (currentArg.length() > 0) {
            args.add(currentArg.toString());
        }
        
        return args.toArray(new String[0]);
    }

    private Map<String, String> parseOptions(String[] args) {
        Map<String, String> options = new HashMap<>();
        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            if (arg.startsWith("-")) {
                if (arg.equals("-cl") || arg.equals("--classloader")) {
                    if (i + 1 < args.length && !args[i + 1].startsWith("-")) {
                        options.put(arg, args[i + 1]);
                        i++;
                    }
                }
            }
        }
        return options;
    }

    private String getHelpText() {
        return String.format("""
            Xposed Method CLI (Command Line Interface) Server端 %s
            作者: JustNothing1021 和 DeepSeek
            
            一个用来调试安卓开发千奇百怪的诡异问题的Xp模块/命令行工具.
            
            命令语法: methods [options] <command> [args...]
            
            可用命令:

              list                              - 列出类的方法
              analyze                           - 分析类的详细信息
              class                             - 查看类的继承关系和构造函数
              field                             - 查看类的字段信息
              watch                             - 监控字段或方法的变化
              export-context                    - 导出设备xtchttp上下文信息
              invoke                            - 调用类的方法
              bsh                               - 通过BeanShell执行代码
              bvars                             - 显示BeanShell执行器的变量
              bclear                            - 清空BeanShell执行器的变量
              script                            - 通过我做的解释器执行代码
              sclear                            - 清空我做的解释器的变量
              packages                          - 列出已知包名
              help                              - 显示所有命令的帮助
              output_test                       - 对命令行输出进行测试
              interactive_test                  - 对命令行交互进行测试
            
            
            可选项:
              -cl, --classloader <package>      - 指定类加载器（软件包名）
            
            示例:
              methods invoke java.lang.System currentTimeMillis
              methods -cl android list com.android.server.am.ActivityManagerService
              methods script for (int i = 0; i < 114; i++) println(i); // 命令行记得加引号
              methods analyze -f java.lang.String
              methods class -i java.util.ArrayList
              methods field -g java.lang.System out
              methods watch add field java.lang.System out 1000

            
            (MainModule: 增加了套接字执行命令的功能, 让输出实时显现, 当然也可以选择不使用)
            """, MAIN_MODULE_VER);
    }
}