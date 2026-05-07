package com.justnothing.testmodule.command;

import com.justnothing.testmodule.command.base.*;
import com.justnothing.testmodule.command.base.command.CommandInfo;
import com.justnothing.testmodule.command.base.command.CommandLineParser;
import com.justnothing.testmodule.command.base.command.RegisterCommand;
import com.justnothing.testmodule.command.base.command.RegisterParser;
import com.justnothing.testmodule.command.base.command.SubCommand;
import com.justnothing.testmodule.command.base.command.SubCommandInfo;
import com.justnothing.testmodule.command.base.command.SubCommands;
import com.justnothing.testmodule.command.base.command.SupportsRequests;
import com.justnothing.testmodule.command.base.protocol.CommandRequest;
import com.justnothing.testmodule.command.base.protocol.CommandResult;
import com.justnothing.testmodule.command.utils.AutoSerializer;
import com.justnothing.testmodule.command.utils.ParamParser;

import java.util.Arrays;
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
import java.util.concurrent.ConcurrentHashMap;



public class CommandExecutor {
    private static class CmdExcLogger extends Logger {
        @Override
        public String getTag() {
            return "CommandExecutor";
        }
    }

    private static final CmdExcLogger logger = new CmdExcLogger();

    private static final Map<String, MainCommand<?>> commandRegistry = new ConcurrentHashMap<>();
    private static final Map<String, CommandLineParser<? extends CommandRequest>>
                commandParsers = new ConcurrentHashMap<>();

    private static final ThreadLocal<String> targetPackageThreadLocal = new ThreadLocal<>();
    private static final ThreadLocal<ClassLoader> classLoaderThreadLocal = new ThreadLocal<>();
    private static final ThreadLocal<ClassLoaderManager> classLoaderManagerThreadLocal = new ThreadLocal<>();

    private volatile boolean initialized = false;

    static {
        autoRegister(
            HelpMain.class,
            WatchMain.class,
            TraceMain.class,
            ExportContextMain.class,
            MemoryMain.class,
            ThreadsMain.class,
            SystemMain.class,
            BreakpointMain.class,
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

        autoRegisterWithCommandInfo(
            ClassMain.class,
            PackagesMain.class
        );
    }

    @SafeVarargs
    private static void autoRegister(Class<? extends MainCommand<?>>... commandClasses) {
        for (Class<? extends MainCommand<?>> cmdClass : commandClasses) {
            try {
                RegisterCommand rc = cmdClass.getAnnotation(RegisterCommand.class);
                if (rc == null) {
                    logger.warn(cmdClass.getSimpleName() + " 缺少 @RegisterCommand 注解，跳过");
                    continue;
                }

                String commandName = rc.value();
                MainCommand<?> instance = cmdClass.getDeclaredConstructor().newInstance();

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
                } else if (!commandParsers.containsKey(commandName)
                        && cmdClass.isAnnotationPresent(SubCommands.class)) {
                    try {
                        CommandLineParser<?> defaultParser = new AnnotationBasedParser(cmdClass);
                        registerParser(commandName, defaultParser);
                    } catch (Exception ignored) {}
                }
            } catch (Exception e) {
                logger.error("自动注册命令失败 (已跳过): " + cmdClass.getSimpleName()
                           + " - " + e.getMessage()
                           + "\n   该命令将不可用，但服务端继续运行");
            }
        }
    }

    @SafeVarargs
    private static void autoRegisterWithCommandInfo(Class<? extends MainCommand<?>>... commandClasses) {
        for (Class<? extends MainCommand<?>> cmdClass : commandClasses) {
            try {
                CommandInfo cmdInfo = cmdClass.getAnnotation(CommandInfo.class);
                if (cmdInfo == null) {
                    logger.warn(cmdClass.getSimpleName() + " 缺少 @CommandInfo 注解，跳过");
                    continue;
                }

                String commandName = cmdInfo.name();
                MainCommand<?> instance = cmdClass.getDeclaredConstructor().newInstance();

                registerCommand(commandName, instance);

                SubCommands subCommandsAnnotation = cmdClass.getAnnotation(SubCommands.class);
                if (subCommandsAnnotation != null) {
                    for (SubCommand subCmd : subCommandsAnnotation.value()) {
                        if (subCmd.request() != CommandRequest.class) {
                            AutoSerializer.registerRequest(subCmd.request());
                            registerCommand(subCmd.request(), instance);
                        }

                        if (subCmd.result() != CommandResult.class) {
                            AutoSerializer.registerResult(subCmd.result());
                        }
                    }
                }

                SupportsRequests supportsReqs = cmdClass.getAnnotation(SupportsRequests.class);
                if (supportsReqs != null) {
                    for (Class<? extends CommandRequest> reqType : supportsReqs.value()) {
                        AutoSerializer.registerRequest(reqType);
                        registerCommand(reqType, instance);
                    }
                }

                CommandLineParser<?> parser = new AnnotationBasedParser(cmdClass);
                if (!commandParsers.containsKey(commandName)) {
                    registerParser(commandName, parser);
                }

                logger.info("通过 @CommandInfo 注册命令: " + commandName + 
                           " (" + cmdClass.getSimpleName() + ")");
                
            } catch (Exception e) {
                logger.error("⚠️ @CommandInfo 自动注册命令失败 (已跳过): " + cmdClass.getSimpleName()
                           + " - " + e.getMessage()
                           + "\n   该命令将不可用，但服务端继续运行");
            }
        }
    }

    private static void registerCommand(String name, MainCommand<?> command) {
        commandRegistry.put(name, command);
    }

    private static void registerCommand(Class<? extends CommandRequest> requestType, MainCommand<?> command)
                throws RuntimeException {
        try {
            registerCommand(requestType.newInstance().getCommandType(), command);
        } catch (InstantiationException | IllegalAccessException e) {
            logger.error("注册命令时出错 (已跳过): " + requestType.getSimpleName()
                       + " - " + e.getMessage()
                       + "\n   该请求类型将无法路由，但服务端继续运行");
        } catch (IllegalStateException e) {
            if (e.getMessage() != null && e.getMessage().contains("@SerializeKeyName")) {
                logger.error(requestType.getSimpleName() + " 缺少@SerializeKeyName注解 (已跳过)");
            } else {
                logger.error("注册命令时状态异常 (已跳过): " + e.getMessage());
            }
        }
    }

    public static void registerParser(String name, CommandLineParser<? extends CommandRequest> parser) {
        commandParsers.put(name, parser);
    }

    public static MainCommand<? extends CommandResult> getCommand(String name) {
        return commandRegistry.get(name);
    }

    public static Map<String, MainCommand<?>> getAllCommands() {
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
        dispatchAndExecute(context, executionType, output);
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
            executionType == CommandType.COMMAND_LINE ? output : new VoidOutputHandler(),
            ArgumentGroup.parse(""),
            requirements
        );
        context.setRequest(request);
        context.setExecutionType(executionType);

        dispatchAndExecute(context, executionType, output);
    }

    private void dispatchAndExecute(CmdExecContext<CommandRequest> context, CommandType executionType,
                            ICommandOutputHandler origOutput)
            throws Throwable {
        String command = context.cmdName();
        ICommandOutputHandler output = context.output();
        MainCommand<? extends CommandResult> commandObj = getCommand(command);
        if (commandObj != null) {
            try {
                context.parseRequest();
            } catch (IllegalCommandLineArgumentException e) {
                if (executionType == CommandType.COMMAND_LINE) {
                    output.println("参数错误: " + e.getMessage(), Colors.RED);
                    
                    String subCommandHelp = getSubCommandHelp(commandObj, context);
                    if (subCommandHelp != null && !subCommandHelp.isEmpty()) {
                        output.println("", Colors.DEFAULT);
                        output.println(subCommandHelp, Colors.WHITE);
                    } else {
                        output.println(commandObj.getHelpText(), Colors.WHITE);
                    }
                    return;
                }
                throw e;
            } catch (Exception e) {
                logger.debug("命令 " + command + " 无需参数解析或解析失败: " + e.getMessage());
            }

            CommandResult result = commandObj.runMain(context);
            if (executionType == CommandType.USER_INTERFACE)
                origOutput.println(result.toJson().toString());

        } else {
            if (executionType != CommandType.COMMAND_LINE) {
                logger.error("在非命令行模式下执行未知命令: " + command + ", 将会抛出错误");
                throw new RuntimeException("未知的命令: " + command + ", 输入help获取帮助");
            }
            output.println("未知的命令: " + command + ", 输入help获取帮助", Colors.ORANGE);
        }
    }

    private String getSubCommandHelp(MainCommand<?> commandObj, CmdExecContext<?> context) {
        try {
            String[] args = context.args();
            if (args == null || args.length == 0) return null;

            String subCmdName = args[0];
            SubCommands subCommandsAnnotation = commandObj.getClass().getAnnotation(SubCommands.class);
            if (subCommandsAnnotation == null) return null;

            for (SubCommand subCmd : subCommandsAnnotation.value()) {
                if (subCmd.value().equals(subCmdName) && subCmd.command() != AbstractCommand.class) {
                    SubCommandInfo info = subCmd.command().getAnnotation(SubCommandInfo.class);
                    if (info != null) return AbstractCommand.generateHelpFromAnnotation(info);
                    break;
                }
            }
        } catch (Exception e) {
            logger.debug("获取子命令帮助信息失败: " + e.getMessage());
        }
        return null;
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
                CommandLineParser<?> parser = commandParsers.get(cmdName);
                if (parser == null) {
                    return null;
                }
                return this.request = (T) parser.parse(this);
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


    private static class AnnotationBasedParser implements CommandLineParser<CommandRequest> {
        private final CommandInfo commandInfo;
        private final SubCommands subCommands;

        AnnotationBasedParser(Class<? extends MainCommand<?>> commandClass) {
            this.commandInfo = commandClass.getAnnotation(CommandInfo.class);
            this.subCommands = commandClass.getAnnotation(SubCommands.class);
        }

        @Override
        public CommandRequest parse(CmdExecContext<? extends CommandRequest> context) 
                throws IllegalCommandLineArgumentException {
            String[] args = context.args();
            
            if (args.length < 1) {
                if (commandInfo != null && !commandInfo.defaultSubcommand().isEmpty()) {
                    return createDefaultRequest(commandInfo.defaultSubcommand());
                }
                return null;
            }

            String subCommandName = args[0];
            String[] remainingArgs = Arrays.copyOfRange(args, 1, args.length);

            SubCommand matchingSubCmd = findSubCommand(subCommandName);
            
            if (matchingSubCmd == null) {
                throw new IllegalCommandLineArgumentException(
                    "未知的命令: " + subCommandName +
                    ". 可用的有: " + getAvailableSubcommands());
            }

            try {
                Class<? extends CommandRequest> requestType = matchingSubCmd.request();
                CommandRequest requestInstance = ParamParser.parse(requestType, remainingArgs);

                return requestInstance.fromCommandLine(remainingArgs);

            } catch (Exception e) {
                throw new IllegalCommandLineArgumentException(
                    "无法给子命令 " + subCommandName + " 创建请求: " + e.getMessage());
            }
        }

        private SubCommand findSubCommand(String name) {
            if (subCommands == null) return null;
            
            for (SubCommand subCmd : subCommands.value()) {
                if (subCmd.value().equals(name)) {
                    return subCmd;
                }
            }
            return null;
        }

        private CommandRequest createDefaultRequest(String subCommandName) 
                throws IllegalCommandLineArgumentException {
            SubCommand subCmd = findSubCommand(subCommandName);
            if (subCmd == null) {
                throw new IllegalCommandLineArgumentException(
                    "Default subcommand not found: " + subCommandName);
            }
            
            try {
                return subCmd.request().getDeclaredConstructor().newInstance();
            } catch (Exception e) {
                throw new IllegalCommandLineArgumentException(
                    "Failed to create default request: " + e.getMessage());
            }
        }

        private String getAvailableSubcommands() {
            if (subCommands == null) return "(none)";
            
            StringBuilder sb = new StringBuilder();
            for (SubCommand subCmd : subCommands.value()) {
                if (sb.length() > 0) sb.append(", ");
                sb.append(subCmd.value());
            }
            return sb.toString();
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
              export-context                    - 导出设备context上下文信息
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