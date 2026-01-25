package com.justnothing.testmodule.command;

import static com.justnothing.testmodule.constants.CommandServer.MAIN_MODULE_VER;

import com.justnothing.testmodule.command.functions.analyze.AnalyzeMain;
import com.justnothing.testmodule.command.functions.beanshell.BeanShellExecutorMain;
import com.justnothing.testmodule.command.functions.breakpoint.BreakpointMain;
import com.justnothing.testmodule.command.functions.classcmd.ClassMain;
import com.justnothing.testmodule.command.functions.deadlock.DeadlockMain;
import com.justnothing.testmodule.command.functions.dump.DumpMain;
import com.justnothing.testmodule.command.functions.exportcontext.ExportContextMain;
import com.justnothing.testmodule.command.functions.fieldcmd.FieldMain;
import com.justnothing.testmodule.command.functions.gc.GcMain;
import com.justnothing.testmodule.command.functions.heap.HeapMain;
import com.justnothing.testmodule.command.functions.help.HelpMain;
import com.justnothing.testmodule.command.functions.interactive.InteractiveExampleMain;
import com.justnothing.testmodule.command.functions.invoke.InvokeMethodMain;
import com.justnothing.testmodule.command.functions.list.ListMethodsMain;
import com.justnothing.testmodule.command.functions.memory.MemoryMain;
import com.justnothing.testmodule.command.functions.output.OutputExampleMain;
import com.justnothing.testmodule.command.functions.packages.PackagesMain;
import com.justnothing.testmodule.command.functions.script.ScriptExecutorMain;
import com.justnothing.testmodule.command.functions.scriptinteractive.ScriptInteractiveMain;
import com.justnothing.testmodule.command.functions.system.SystemMain;
import com.justnothing.testmodule.command.functions.threads.ThreadsMain;
import com.justnothing.testmodule.command.functions.trace.TraceMain;
import com.justnothing.testmodule.command.functions.profile.ProfileMain;
import com.justnothing.testmodule.command.functions.graph.GraphMain;
import com.justnothing.testmodule.command.functions.search.SearchMain;
import com.justnothing.testmodule.command.functions.watch.WatchMain;
import com.justnothing.testmodule.command.functions.CommandBase;
import com.justnothing.testmodule.command.output.StringBuilderCollector;
import com.justnothing.testmodule.command.output.IOutputHandler;
import com.justnothing.testmodule.command.output.SystemOutputRedirector;
import com.justnothing.testmodule.utils.data.ClassLoaderManager;
import com.justnothing.testmodule.utils.functions.Logger;

import java.util.HashMap;
import java.util.Map;



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

    private static final Map<String, CommandBase> commandRegistry = new HashMap<>();

    static {
        registerCommand("help", new HelpMain());
        registerCommand("list", new ListMethodsMain());
        registerCommand("analyze", new AnalyzeMain());
        registerCommand("class", new ClassMain());
        registerCommand("field", new FieldMain());
        registerCommand("watch", new WatchMain());
        registerCommand("trace", new TraceMain());
        registerCommand("profile", new ProfileMain());
        registerCommand("graph", new GraphMain());
        registerCommand("search", new SearchMain());
        registerCommand("export-context", new ExportContextMain());
        registerCommand("heap", new HeapMain());
        registerCommand("gc", new GcMain());
        registerCommand("memory", new MemoryMain());
        registerCommand("dump", new DumpMain());
        registerCommand("deadlock", new DeadlockMain());
        registerCommand("threads", new ThreadsMain());
        registerCommand("system", new SystemMain());
        registerCommand("breakpoint", new BreakpointMain());
        registerCommand("invoke", new InvokeMethodMain());
        registerCommand("packages", new PackagesMain());
        
        BeanShellExecutorMain beanShellExecutor = new BeanShellExecutorMain("bsh");
        registerCommand("bsh", beanShellExecutor);
        registerCommand("bvars", beanShellExecutor);
        registerCommand("bclear", beanShellExecutor);
        
        ScriptExecutorMain scriptExecutor = new ScriptExecutorMain("script");
        registerCommand("script", scriptExecutor);
        registerCommand("sclear", scriptExecutor);
        registerCommand("svars", scriptExecutor);
        
        registerCommand("script_interactive", new ScriptInteractiveMain());
        registerCommand("output_test", new OutputExampleMain());
        registerCommand("interactive_test", new InteractiveExampleMain());
    }

    private static void registerCommand(String name, CommandBase command) {
        commandRegistry.put(name, command);
    }

    public static CommandBase getCommand(String name) {
        return commandRegistry.get(name);
    }

    public static Map<String, CommandBase> getAllCommands() {
        return new HashMap<>(commandRegistry);
    }

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


    private String parseOptions(String cmdline) {
        while (true) {
            cmdline = cmdline.trim();
            if (cmdline.startsWith("-")) {
                int index = cmdline.indexOf(' ');
                if (index == -1) {
                    return cmdline;
                } else {
                    String option = cmdline.substring(0, index);
                    if (option.equals("-cl") || option.equals("-classloader")) {
                        cmdline = cmdline.substring(index).trim();
                        int nextIndex = cmdline.indexOf(' ');
                        if (nextIndex == -1) {
                            logger.warn("指定了类加载器参数，但没有指定类加载器名称");
                        }
                        String classloader = cmdline.substring(0, nextIndex);
                        setTargetPackage(classloader);
                        cmdline = cmdline.substring(nextIndex).trim();
                    } else {
                        logger.warn("无效的参数: " + option);
                        cmdline = cmdline.substring(index).trim();
                    }
                }
            } else {
                break;
            }
        }
        return cmdline;
    }

    private String[] splitArguments(String cmdline) {
        return cmdline.split(" ");
    }


    private void executeCommandInternal(String fullCommand, IOutputHandler output) {
        fullCommand = fullCommand.trim();
        if (fullCommand.isEmpty()) {
            output.println("没有指定命令 (可以用help来获取帮助)");
            return;
        }

        fullCommand = parseOptions(fullCommand);
        String[] commandParams = splitArguments(fullCommand);
        if (commandParams.length == 0) {
            output.println("没有指定命令 (可以用help来获取帮助)");
            return;
        }
        
        String command = commandParams[0];
        int spaceIndex = fullCommand.indexOf(' ');
        String commandString = (spaceIndex != -1) ? fullCommand.substring(spaceIndex) : "";

        // 去掉命令本身，只保留参数
        String[] args = new String[commandParams.length - 1];
        System.arraycopy(commandParams, 1, args, 0, args.length);

        // 创建执行上下文
        CmdExecContext context = new CmdExecContext(
            command,
            args,
            commandString,
            targetPackage,
            getClassLoader(),
            output
        );

        try {
            // 使用命令注册表分发命令
            CommandBase commandObj = getCommand(command);
            if (commandObj != null) {
                context.output().println(commandObj.runMain(context));
            } else {
                output.println("未知的命令: " + command + ", 输入help获取帮助");
            }
        } catch (Exception e) {
            output.println("\n===============================================");
            output.println("执行命令时出现错误!");
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
            String cmdName,
            String[] args,
            String origCommand,
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


    public static String getHelpText() {
        return String.format("""
            Xposed Method CLI (Command Line Interface) Server端 %s
            作者: JustNothing1021, DeepSeek 和 GLM-4.7
            
            一个用来调试安卓开发千奇百怪的诡异问题的Xp模块/命令行工具.
            
            命令语法: methods [options] <command> [args...]
            
            可用命令:
              help                              - 显示所有命令的帮助或特定命令的帮助
              list                              - 列出类的方法
              analyze                           - 分析类的详细信息
              class                             - 查看类的继承关系和构造函数
              field                             - 查看类的字段信息
              watch                             - 监控字段或方法的变化
              export-context                    - 导出设备xtchttp上下文信息
              heap                              - 查看Java堆内存信息
              gc                                - 手动触发垃圾回收
              memory                            - 显示详细内存使用情况
              graph                             - 生成类图, 调用图和依赖图
              search                            - 搜索类, 方法或字段
              dump                              - 导出堆信息到文件
              deadlock                          - 检测死锁
              threads                           - 列出所有线程及其状态
              system                            - 显示系统信息
              breakpoint                        - 设置和管理断点
              invoke                            - 调用类的方法
              packages                          - 列出已知包名
              bsh                               - 通过BeanShell执行代码
              bvars                             - 显示BeanShell执行器的变量
              bclear                            - 清空BeanShell执行器的变量
              script                            - 通过我做的解释器执行代码
              script_interactive                - 进入交互式脚本执行模式
              sclear                            - 清空我做的解释器的变量
              svars                             - 显示我做的解释器的变量

            测试类命令:
              output_test                       - 对命令行输出进行测试
              interactive_test                  - 对命令行交互进行测试
            
            获取一个子命令的帮助:
              help <cmd_name>
            
            可选项:
              -cl, --classloader <package>      - 指定类加载器（软件包名，没找到会是默认的类加载器）
            
            示例:
              methods invoke java.lang.System currentTimeMillis
              methods -cl android list com.android.server.am.ActivityManagerService
              methods script for (int i = 0; i < 114; i++) println(i); // 命令行记得加引号
              methods analyze -f java.lang.String
              methods class -i java.util.ArrayList
              methods field -g java.lang.System out
              methods watch add field java.lang.System out 1000
            
            

            
            (MainModule: 让AI给我写了一堆新功能, 再也不用担心自己研究不透系统了)
            """, MAIN_MODULE_VER);
    }
}