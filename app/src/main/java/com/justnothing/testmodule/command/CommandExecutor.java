package com.justnothing.testmodule.command;

import com.justnothing.testmodule.command.base.CommandLineParser;
import com.justnothing.testmodule.command.base.CommandRequest;
import com.justnothing.testmodule.command.base.CommandResult;
import com.justnothing.testmodule.command.base.MainCommand;
import com.justnothing.testmodule.command.base.RegisterCommand;
import com.justnothing.testmodule.command.base.SupportsRequests;
import com.justnothing.testmodule.command.base.RegisterParser;
import static com.justnothing.testmodule.constants.CommandServer.MAIN_MODULE_VER;

import com.justnothing.testmodule.command.base.IllegalCommandLineArgumentException;
import com.justnothing.testmodule.command.functions.bsh.impl.BeanShellExecutorMain;
import com.justnothing.testmodule.command.functions.breakpoint.impl.BreakpointMain;
import com.justnothing.testmodule.command.functions.bytecode.impl.BytecodeMain;
import com.justnothing.testmodule.command.functions.classcmd.ClassMain;
import com.justnothing.testmodule.command.functions.examples.InteractiveExampleMain;
import com.justnothing.testmodule.command.functions.examples.OutputExampleMain;
import com.justnothing.testmodule.command.functions.exportcontext.ExportContextMain;
import com.justnothing.testmodule.command.functions.help.HelpMain;
import com.justnothing.testmodule.command.functions.alias.impl.AliasMain;
import com.justnothing.testmodule.command.functions.hook.HookMain;
import com.justnothing.testmodule.command.functions.memory.MemoryMain;
import com.justnothing.testmodule.command.functions.nativecmd.NativeMain;
import com.justnothing.testmodule.command.functions.network.NetworkMain;
import com.justnothing.testmodule.command.functions.packages.PackagesMain;
import com.justnothing.testmodule.command.functions.performance.PerformanceMain;
import com.justnothing.testmodule.command.functions.script.ScriptExecutorMain;
import com.justnothing.testmodule.command.functions.system.SystemMain;
import com.justnothing.testmodule.command.functions.threads.ThreadsMain;
import com.justnothing.testmodule.command.functions.trace.TraceMain;
import com.justnothing.testmodule.command.functions.watch.WatchMain;
import com.justnothing.testmodule.command.functions.tests.SandboxTestMain;
import com.justnothing.testmodule.command.functions.tests.AnonClassTestMain;
import com.justnothing.testmodule.command.output.ClientRequirements;
import com.justnothing.testmodule.command.output.Colors;
import com.justnothing.testmodule.command.output.StringBuilderCollector;
import com.justnothing.testmodule.command.output.ICommandOutputHandler;
import com.justnothing.testmodule.command.output.SystemOutputRedirector;
import com.justnothing.testmodule.command.output.VoidOutputHandler;
import com.justnothing.testmodule.command.utils.ArgumentGroup;
import com.justnothing.testmodule.command.utils.CommandArgumentParser;
import com.justnothing.testmodule.utils.reflect.ClassLoaderManager;
import com.justnothing.testmodule.utils.logging.Logger;

import org.json.JSONException;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;



public class CommandExecutor {
    private static class CmdExcLogger extends Logger {
        @Override
        public String getTag() {
            return "CommandExecutor";
        }
    }

    private static final CmdExcLogger logger = new CmdExcLogger();

    private static final Map<String, MainCommand<?, ?>> commandRegistry = new ConcurrentHashMap<>();
    private static final Map<String, CommandLineParser<? extends CommandRequest>>
                commandParsers = new ConcurrentHashMap<>();

    private static final ThreadLocal<String> targetPackageThreadLocal = new ThreadLocal<>();
    private static final ThreadLocal<ClassLoader> classLoaderThreadLocal = new ThreadLocal<>();
    private static final ThreadLocal<ClassLoaderManager> classLoaderManagerThreadLocal = new ThreadLocal<>();

    private volatile boolean initialized = false;

    static {
        autoRegister(
            HelpMain.class,
            ClassMain.class,
            WatchMain.class,
            TraceMain.class,
            ExportContextMain.class,
            MemoryMain.class,
            ThreadsMain.class,
            SystemMain.class,
            BreakpointMain.class,
            PackagesMain.class,
            HookMain.class,
            BytecodeMain.class,
            NativeMain.class,
            PerformanceMain.class,
            AliasMain.class,
            NetworkMain.class,
            BeanShellExecutorMain.class,
            ScriptExecutorMain.class,
            OutputExampleMain.class,
            InteractiveExampleMain.class,
            SandboxTestMain.class,
            AnonClassTestMain.class
        );
    }

    @SafeVarargs
    private static void autoRegister(Class<? extends MainCommand<?, ?>>... commandClasses) {
        for (Class<? extends MainCommand<?, ?>> cmdClass : commandClasses) {
            try {
                RegisterCommand rc = cmdClass.getAnnotation(RegisterCommand.class);
                if (rc == null) {
                    logger.warn(cmdClass.getSimpleName() + " 缺少 @RegisterCommand 注解，跳过");
                    continue;
                }

                String commandName = rc.value();
                MainCommand<?, ?> instance = cmdClass.getDeclaredConstructor().newInstance();

                registerCommand(commandName, instance);

                SupportsRequests sr = cmdClass.getAnnotation(SupportsRequests.class);
                if (sr != null) {
                    for (Class<? extends CommandRequest> reqType : sr.value()) {
                        registerCommand(reqType, instance);
                    }
                }

                RegisterParser rp = cmdClass.getAnnotation(RegisterParser.class);
                if (rp != null) {
                    CommandLineParser<?> parser = rp.value().getDeclaredConstructor().newInstance();
                    registerParser(commandName, parser);
                }
            } catch (Exception e) {
                logger.error("自动注册命令失败: " + cmdClass.getSimpleName(), e);
                throw new RuntimeException("自动注册命令失败: " + cmdClass.getSimpleName(), e);
            }
        }
    }

    private static void registerCommand(String name, MainCommand<?, ?> command) {
        commandRegistry.put(name, command);
    }

    private static void registerCommand(Class<? extends CommandRequest> requestType, MainCommand<?, ?> command)
                throws RuntimeException {
        try {
            registerCommand(requestType.newInstance().getCommandType(), command);
        } catch (InstantiationException | IllegalAccessException e) {
            logger.error("注册命令时出错: " + e.getMessage(), e);
            throw new RuntimeException("注册命令时出错", e);
        }
    }

    public static void registerParser(String name, CommandLineParser<? extends CommandRequest> parser) {
        commandParsers.put(name, parser);
    }

    public static MainCommand<? extends CommandRequest, ? extends CommandResult> getCommand(String name) {
        return commandRegistry.get(name);
    }

    public static Map<String, MainCommand<?, ?>> getAllCommands() {
        return new HashMap<>(commandRegistry);
    }

    public CommandExecutor() {
    }

    private void initializeIfNeeded() {
        if (!initialized) {
            synchronized (this) {
                if (!initialized) {
                    initialized = true;
                }
            }
        }
    }

    public void setTargetPackage(String pkgName) {
        targetPackageThreadLocal.set(pkgName);
        classLoaderThreadLocal.remove();
        logger.debug("设置目标包名: " + pkgName);
    }

    private String getTargetPackage() {
        return targetPackageThreadLocal.get();
    }

    private ClassLoader getClassLoader() {
        ClassLoader classLoader = classLoaderThreadLocal.get();
        if (classLoader != null) {
            return classLoader;
        }
        
        String targetPackage = getTargetPackage();
        if (targetPackage == null || targetPackage.equals("default") || targetPackage.isEmpty()) {
            classLoader = Thread.currentThread().getContextClassLoader();
            if (classLoader == null) {
                classLoader = getClass().getClassLoader();
            }
        } else {
            ClassLoaderManager manager = getClassLoaderManager();
            classLoader = manager.getClassLoaderForPackage(targetPackage);
        }
        
        classLoaderThreadLocal.set(classLoader);
        return classLoader;
    }

    private ClassLoaderManager getClassLoaderManager() {
        ClassLoaderManager manager = classLoaderManagerThreadLocal.get();
        if (manager == null) {
            manager = new ClassLoaderManager();
            classLoaderManagerThreadLocal.set(manager);
        }
        return manager;
    }

    public void cleanup() {
        targetPackageThreadLocal.remove();
        classLoaderThreadLocal.remove();
        classLoaderManagerThreadLocal.remove();
    }


    public void execute(String fullCommand, ICommandOutputHandler output) {
        execute(fullCommand, output, null, CommandType.COMMAND_LINE);
    }

    public void execute(String fullCommand, ICommandOutputHandler output, ClientRequirements requirements) {
        execute(fullCommand, output, requirements, CommandType.COMMAND_LINE);
    }

    public void execute(String fullCommand, ICommandOutputHandler output, ClientRequirements requirements, CommandType executionType) {
        if (fullCommand == null || fullCommand.trim().isEmpty()) {
            logger.warn("命令为空");
            output.println("命令不能为空", Colors.RED);
            return;
        }

        if (output == null) {
            logger.error("输出处理器为null");
            throw new IllegalArgumentException("输出处理器不能为null");
        }

        initializeIfNeeded();
        logger.info("开始执行命令: " + fullCommand);

        SystemOutputRedirector redirector = new SystemOutputRedirector(output);
        try {
            redirector.startRedirect();
            executeCommandInternal(fullCommand, output, requirements, executionType);
        } catch (Throwable t) {
            handleExecutionError(t, output, executionType);
        } finally {
            redirector.stopRedirect();
            logger.info("命令执行完毕");
            output.close();
            cleanup();
        }
        logger.info("命令执行完成");
    }

    public void execute(CommandRequest request, ICommandOutputHandler output,
                        ClientRequirements requirements, CommandType executionType) {
        if (request == null) {
            logger.warn("请求为空");
            output.println("请求不能为空", Colors.RED);
            return;
        }

        if (output == null) {
            logger.error("输出处理器为null");
            throw new IllegalArgumentException("输出处理器不能为null");
        }

        initializeIfNeeded();
        logger.info("开始执行命令请求: " + request.getCommandType());

        SystemOutputRedirector redirector = new SystemOutputRedirector(output);
        try {
            redirector.startRedirect();
            executeCommandInternal(request, output, requirements, executionType);
        } catch (Throwable t) {
            handleExecutionError(t, output, executionType);
        } finally {
            redirector.stopRedirect();
            logger.info("命令执行完毕");
            output.close();
            cleanup();
        }
        logger.info("命令执行完成");
    }

    private void handleExecutionError(Throwable t, ICommandOutputHandler output, CommandType executionType) {
        if (executionType == CommandType.COMMAND_LINE) {
            logger.error("执行命令异常", t);
            output.println("\n===============================================", Colors.RED);
            output.println("执行命令出现严重错误...", Colors.RED);
            output.print("（你现在看到的是命令执行基类的错误报告, 大概率是命令执行爆掉了或者命令内部", Colors.GRAY);
            output.print("出现了Error而不是Exception", Colors.YELLOW);
            output.println("!）", Colors.GRAY);
            output.print("错误信息: ", Colors.ORANGE);
            output.printf(Colors.YELLOW, "[%s] ", t.getClass().getSimpleName());
            output.println(t.getMessage(), Colors.GRAY);
            output.println("堆栈追踪:", Colors.GRAY);
            output.printStackTrace(t, Colors.GRAY);
            output.println("===============================================", Colors.RED);
            output.close();
        } else {
            try {
                CommandResult result = new CommandResult();
                CommandResult.ErrorInfo error = new CommandResult.ErrorInfo("INTERNAL_ERROR", t.getMessage());
                error.setStacktrace(t);
                result.setSuccess(false);
                result.setMessage(t.getMessage());
                result.setError(error);
                output.println(result.toJson().toString());
            } catch (JSONException e) {
                logger.error("序列化命令结果时出错", e);
                output.println("{\"success\": false, \"message\": \"序列化命令结果时出错\"}");
            }
        }
    }


    private void executeCommandInternal(String fullCommand, ICommandOutputHandler output,
            ClientRequirements requirements, CommandType executionType) throws Throwable {
        fullCommand = fullCommand.trim();
        if (fullCommand.isEmpty()) {
            output.println("没有指定命令 (可以用help来获取帮助)", Colors.RED);
            return;
        }

        fullCommand = AliasMain.resolveAlias(fullCommand);

        CommandArgumentParser.ParseResult parseResult = CommandArgumentParser.parseOptions(fullCommand, logger);

        if (parseResult.classLoader() != null) {
            setTargetPackage(parseResult.classLoader());
        }

        String[] commandParams = CommandArgumentParser.splitArguments(parseResult.commandLine());
        if (commandParams.length == 0) {
            output.println("没有指定命令 (可以用help来获取帮助)", Colors.ORANGE);
            return;
        }

        String command = commandParams[0];
        int spaceIndex = fullCommand.indexOf(' ');
        String commandString = (spaceIndex != -1) ? fullCommand.substring(spaceIndex) : "";

        // 去掉命令本身，只保留参数
        String[] args = new String[commandParams.length - 1];
        System.arraycopy(commandParams, 1, args, 0, args.length);

        // 解析 ArgumentGroup（提供多种参数格式）
        ArgumentGroup argGroup = ArgumentGroup.parse(commandString.trim());

        // 创建执行上下文
        CmdExecContext<CommandRequest> context = new CmdExecContext<>(
            command,
            args,
            commandString,
            getTargetPackage(),
            getClassLoader(),
            executionType == CommandType.COMMAND_LINE ? output : new VoidOutputHandler(),
            argGroup,
            requirements
        );

        // 设置执行类型
        context.setExecutionType(executionType);

        // 使用命令注册表分发命令
        dispatchAndExecute(context, executionType);
    }

    private void executeCommandInternal(CommandRequest request, ICommandOutputHandler output,
                                       ClientRequirements requirements, CommandType executionType) throws Throwable {
        String cmdName = request.getCommandType();

        CmdExecContext<CommandRequest> context = new CmdExecContext<>(
            cmdName,
            new String[0],
            "",
            getTargetPackage(),
            getClassLoader(),
            output,
            ArgumentGroup.parse(""),
            requirements
        );
        context.setRequest(request);
        context.setExecutionType(executionType);

        dispatchAndExecute(context, executionType);
    }

    private void dispatchAndExecute(CmdExecContext<CommandRequest> context, CommandType executionType)
            throws Throwable {
        String command = context.cmdName();
        ICommandOutputHandler output = context.output();
        MainCommand<? extends CommandRequest, ? extends CommandResult> commandObj = getCommand(command);
        if (commandObj != null) {
            try {
                context.parseRequest();
            } catch (Exception e) {
                logger.debug("命令 " + command + " 无需参数解析或解析失败（非致命）: " + e.getMessage());
            }

            CommandResult result = commandObj.runMain(context);
            if (executionType != CommandType.COMMAND_LINE) {
                if (result != null) {
                    output.println(result.toJson().toString());
                } else {
                    throw new RuntimeException("命令执行失败或暂时不支持该命令的非CLI模式");
                }
            }
        } else {
            output.println("未知的命令: " + command + ", 输入help获取帮助", Colors.ORANGE);
        }
    }

    public String executeShellCommand(String fullCommand) {
        StringBuilderCollector collector = new StringBuilderCollector();
        execute(fullCommand, collector);
        return collector.getString();
    }


    public static class CmdExecContext<T extends CommandRequest> {
        public String cmdName;
        public String[] args;
        public String origCommand;
        public String targetPackage;
        public ClassLoader classLoader;
        public ICommandOutputHandler output;
        public ArgumentGroup argGroup;
        public ClientRequirements requirements;

        private T request;
        private CommandType executionType = CommandType.COMMAND_LINE;

        public String cmdName() { return cmdName; }
        public String[] args() { return args; }
        public String origCommand() { return origCommand; }
        public String targetPackage() { return targetPackage; }
        public ClassLoader classLoader() { return classLoader; }
        public ICommandOutputHandler output() { return output; }
        public ArgumentGroup argGroup() { return argGroup; }
        public ClientRequirements requirements() { return requirements; }

        @SuppressWarnings("unchecked")
        public T parseRequest() throws IllegalCommandLineArgumentException {
            if (request != null) return request;
            try {
                return this.request = (T) Objects.requireNonNull(commandParsers.get(cmdName),
                        "找不到命令" + cmdName + "对应的命令行参数解析器")
                            .parse(this);
            } catch (Exception e) {
                throw new IllegalCommandLineArgumentException("命令解析异常: " + e.getMessage());
            }
        }

        public T getRequest() { return request; }
        public void setRequest(T r) { this.request = r; }

        public CommandType getExecutionType() { return executionType; }
        public void setExecutionType(CommandType t) { this.executionType = t; }

        public boolean isCli() { return executionType == CommandType.COMMAND_LINE; }
        public boolean isGui() { return executionType == CommandType.USER_INTERFACE; }
        public boolean isAgent() { return executionType == CommandType.AGENT; }

        public T getCommandRequest() {
            return request;
        }

        public CmdExecContext(String cmdName, String[] args, String origCommand,
                              String targetPackage, ClassLoader classLoader,
                              ICommandOutputHandler output, ArgumentGroup argGroup,
                              ClientRequirements requirements) {
            this.cmdName = cmdName;
            this.args = args;
            this.origCommand = origCommand;
            this.targetPackage = targetPackage;
            this.classLoader = classLoader;
            this.output = output;
            this.argGroup = argGroup;
            this.requirements = requirements;
        }

        public static <T extends CommandRequest> CmdExecContext<T> copyOf(CmdExecContext<? extends T> other) {
            return new CmdExecContext<>(
                other.cmdName, other.args, other.origCommand,
                other.targetPackage, other.classLoader,
                other.output, other.argGroup, other.requirements
            );
        }


        public void print(Object obj) {
            output.print(obj.toString());
        }
        public void print(Object obj, byte color) {
            output.print(obj.toString(), color);
        }
        public void println(Object obj) {
            output.println(obj.toString());
        }
        public void println(Object obj, byte color) {
            output.println(obj.toString(), color);
        }
        public void printStackTrace(Throwable t) {
            output.printStackTrace(t);
        }
        public void printStackTrace(Throwable t, byte color) {
            output.printStackTrace(t, color);
        }
        public void printf(String fmt, Object... args) {
            output.printf(fmt, args);
        }
        public void printf(byte color, String fmt, Object... args) {
            output.printf(color, fmt, args);
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
              alias                             - 管理命令别名
              packages                          - 列出已知包名
              export-context                    - 导出设备xtchttp上下文信息
              bsh                               - 通过BeanShell执行代码
              script                            - 脚本管理系统
              hook                              - 动态Hook注入器
              class                             - 查看类信息
              reflect                           - 使用反射访问和操作类的私有成员
              breakpoint                        - 设置和管理断点
              watch                             - 监控字段或方法的变化
              trace                             - 跟踪方法调用链
              native                            - 查看和调试Native代码
              system                            - 显示系统信息
              profile                           - 性能分析
              threads                           - 列出所有线程及其状态
              bytecode                          - 查看和分析Java字节码 (未正式使用, 很可能实现不了)
              memory                            - 显示详细内存使用情况
              network                           - 进行网络调试

            测试类命令:
              output_test                       - 对命令行输出进行测试
              interactive_test                  - 对命令行交互进行测试
            
            获取一个子命令的帮助:
              help <cmd_name>
            
            可选项:
              -cl, --classloader <package>      - 指定类加载器（软件包名，没找到会是默认的类加载器）
            
            示例:
              methods class graph java.util.ArrayList
              methods -cl android clist com.android.server.am.ActivityManagerService
              methods minfo -h
              methods mgc --full
              methods mdump --heap /sdcard/heap_only.txt
              methods script for (int i = 0; i < 114; i++) println(i); // 命令行记得加引号
              methods watch add field java.lang.System out 1000
              methods hook add com.example.MainActivity onCreate before 'println("onCreate called")'
              methods reflect java.lang.String constructor
              methods reflect -s java.lang.String field value
              methods binfo -v java.util.ArrayList
              methods native list libart.so
            
            (MainModule: 让AI给我写了一堆新功能, 再也不用担心自己研究不透系统了)
            """, MAIN_MODULE_VER);
    }
}