package com.justnothing.testmodule.command;

import static com.justnothing.testmodule.constants.CommandServer.MAIN_MODULE_VER;

import com.justnothing.testmodule.command.functions.beanshell.BeanShellExecutorMain;
import com.justnothing.testmodule.command.functions.breakpoint.BreakpointMain;
import com.justnothing.testmodule.command.functions.classcmd.ClassMain;
import com.justnothing.testmodule.command.functions.examples.InteractiveExampleMain;
import com.justnothing.testmodule.command.functions.examples.OutputExampleMain;
import com.justnothing.testmodule.command.functions.exportcontext.ExportContextMain;
import com.justnothing.testmodule.command.functions.help.HelpMain;
import com.justnothing.testmodule.command.functions.hook.HookMain;
import com.justnothing.testmodule.command.functions.memory.MemoryMain;
import com.justnothing.testmodule.command.functions.packages.PackagesMain;
import com.justnothing.testmodule.command.functions.script.ScriptExecutorMain;
import com.justnothing.testmodule.command.functions.system.SystemMain;
import com.justnothing.testmodule.command.functions.threads.ThreadsMain;
import com.justnothing.testmodule.command.functions.trace.TraceMain;
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
        registerCommand("class", new ClassMain());
        registerCommand("watch", new WatchMain());
        registerCommand("trace", new TraceMain());
        registerCommand("export-context", new ExportContextMain());
        registerCommand("memory", new MemoryMain());
        registerCommand("threads", new ThreadsMain());
        registerCommand("system", new SystemMain());
        registerCommand("breakpoint", new BreakpointMain());
        registerCommand("packages", new PackagesMain());
        registerCommand("hook", new HookMain());
        
        ClassMain classExecutor = new ClassMain("class");
        registerCommand("cinfo", classExecutor);
        registerCommand("cgraph", classExecutor);
        registerCommand("canalyze", classExecutor);
        registerCommand("clist", classExecutor);
        registerCommand("cinvoke", classExecutor);
        registerCommand("cfield", classExecutor);
        registerCommand("csearch", classExecutor);
        
        BeanShellExecutorMain beanShellExecutor = new BeanShellExecutorMain("bsh");
        registerCommand("bsh", beanShellExecutor);
        registerCommand("bvars", beanShellExecutor);
        registerCommand("bclear", beanShellExecutor);
        registerCommand("bscript", beanShellExecutor);
        
        ScriptExecutorMain scriptExecutor = new ScriptExecutorMain("script");
        registerCommand("script", scriptExecutor);
        registerCommand("srun", scriptExecutor);
        registerCommand("sclear", scriptExecutor);
        registerCommand("svars", scriptExecutor);
        registerCommand("sinteractive", scriptExecutor);
        
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
                logger.info("命令执行完毕");
                output.close();
            }
        } catch (Exception e) {
            logger.error("执行命令异常", e);
            output.println("\n===============================================");
            output.println("执行命令出现严重错误...");
            output.println("错误信息:");
            output.printStackTrace(e);
            output.println("===============================================");
            // 出错时也要关闭output
            output.close();
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
              watch                             - 监控字段或方法的变化
              trace                             - 跟踪方法调用链
              profile                           - 性能分析
              graph                             - 生成类图, 调用图和依赖图
              export-context                    - 导出设备xtchttp上下文信息
              memory                            - 显示详细内存使用情况
              threads                           - 列出所有线程及其状态
              system                            - 显示系统信息
              breakpoint                        - 设置和管理断点
              packages                          - 列出已知包名
              hook                              - 动态Hook注入器

              bsh                               - 通过BeanShell执行代码
                bvars                           - 显示BeanShell执行器的变量
                bclear                          - 清空BeanShell执行器的变量

              class                            - 查看类信息
                cinfo                          - 查看类的详细信息 (快捷方式)
                cgraph                         - 生成类继承图 (快捷方式)
                canalyze                       - 分析类的字段和方法 (快捷方式)
                clist                          - 列出一个类的所有方法 (快捷方式)
                cinvoke                        - 调用类中的方法 (快捷方式)
                cfield                         - 查看或操作字段 (快捷方式)
                csearch                        - 搜索类、方法、字段或注解 (快捷方式)
                
              script                            - 脚本管理系统
                srun                            - 快捷执行脚本代码
                sinteractive                    - 进入交互式脚本执行模式
                sclear                          - 清空脚本解释器的变量
                svars                           - 显示脚本解释器的变量

            测试类命令:
              output_test                       - 对命令行输出进行测试
              interactive_test                  - 对命令行交互进行测试
            
            获取一个子命令的帮助:
              help <cmd_name>
            
            可选项:
              -cl, --classloader <package>      - 指定类加载器（软件包名，没找到会是默认的类加载器）
            
            示例:
              methods class info java.lang.String
              methods class graph java.util.ArrayList
              methods class analyze -f java.lang.String
              methods class list -vb java.lang.String
              methods class list com.android.server.am.ActivityManagerService
              methods class invoke java.lang.System currentTimeMillis
              methods class field -g java.lang.System out
              methods class search class *Activity
              methods class search method onCreate
              methods class search field m*
              methods class search annotation Override
              methods -cl android list com.android.server.am.ActivityManagerService
              methods script for (int i = 0; i < 114; i++) println(i); // 命令行记得加引号
              methods watch add field java.lang.System out 1000
              methods hook add com.example.MainActivity onCreate before 'println("onCreate called")'
            
            

            
            (MainModule: 让AI给我写了一堆新功能, 再也不用担心自己研究不透系统了)
            """, MAIN_MODULE_VER);
    }
}