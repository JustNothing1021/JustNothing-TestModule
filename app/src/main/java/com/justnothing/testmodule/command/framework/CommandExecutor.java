package com.justnothing.testmodule.command.framework;

import com.justnothing.testmodule.command.framework.model.MainCommand;
import com.justnothing.testmodule.command.framework.annotation.Cmd;
import com.justnothing.testmodule.command.framework.i18n.CliMessages;
import com.justnothing.testmodule.command.framework.i18n.CliTexts;
import com.justnothing.testmodule.command.framework.model.CommandRouter;
import com.justnothing.testmodule.command.framework.model.CommandRequest;
import com.justnothing.testmodule.command.framework.model.CommandResult;

import static com.justnothing.testmodule.constants.CommandServer.MAIN_MODULE_VER;

import com.justnothing.testmodule.command.functions.alias.AliasMain;
import com.justnothing.testmodule.command.framework.output.ClientRequirements;
import com.justnothing.testmodule.command.framework.output.Colors;
import com.justnothing.testmodule.command.framework.output.StringBuilderCollector;
import com.justnothing.testmodule.command.framework.output.ICommandOutputHandler;
import com.justnothing.testmodule.command.framework.output.SystemOutputRedirector;
import com.justnothing.testmodule.command.framework.output.VoidOutputHandler;
import com.justnothing.testmodule.command.framework.utils.CommandArgumentParser;
import com.justnothing.testmodule.utils.logging.Logger;
import com.justnothing.testmodule.utils.reflect.ClassLoaderManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;



public class CommandExecutor {
    private static class CmdExcLogger extends Logger {
        @Override
        public String getTag() {
            return "CommandExecutor";
        }
    }

    private static final CmdExcLogger logger = new CmdExcLogger();

    private static final Map<String, MainCommand<?>> commandRegistry = new ConcurrentHashMap<>();
    private static final ThreadLocal<String> targetPackageThreadLocal = new ThreadLocal<>();
    private static final ThreadLocal<ClassLoader> classLoaderThreadLocal = new ThreadLocal<>();
    private static final ThreadLocal<ClassLoaderManager> classLoaderManagerThreadLocal = new ThreadLocal<>();

    static {
        autoRegister(CommandCatalog.ALL);
    }

    @SafeVarargs
    private static void autoRegister(Class<? extends MainCommand<?>>... commandClasses) {
        for (Class<? extends MainCommand<?>> cmdClass : commandClasses) {
            try {
                Cmd cmdAnnotation = cmdClass.getAnnotation(Cmd.class);
                if (cmdAnnotation == null) {
                    logger.warn(cmdClass.getSimpleName() + " 缺少 @Cmd 注解，跳过");
                    continue;
                }

                String commandName = cmdAnnotation.name();
                CommandRouter.getInstance().registerCommand(cmdClass);

                MainCommand<?> instance = cmdClass.getDeclaredConstructor().newInstance();
                registerCommand(commandName, instance);

            } catch (Exception e) {
                logger.error("自动注册命令失败 (已跳过): " + cmdClass.getSimpleName()
                           + " - " + e.getMessage()
                           + "\n   该命令将不可用，但服务端继续运行");
            }
        }
    }


    private static void registerCommand(String name, MainCommand<?> command) {
        commandRegistry.put(name, command);
    }

    public static MainCommand<? extends CommandResult> getCommand(String name) {
        return commandRegistry.get(name);
    }

    public static Map<String, MainCommand<?>> getAllCommands() {
        return new HashMap<>(commandRegistry);
    }

    public CommandExecutor() {
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
        // 线程来自线程池、会被复用：不清掉的话上一个客户端的语言会泄漏给下一个。
        CliMessages.clearLanguage();
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

        logger.info("开始执行命令: " + fullCommand);

        executeWithRedirect(
                () -> executeCommandInternal(fullCommand, output, requirements, executionType),
                output, executionType);
    }

    public void execute(CommandRequest<?> request, ICommandOutputHandler output,
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

        logger.info("开始执行命令请求: " + request.getCommandType());

        executeWithRedirect(
                () -> executeCommandInternal(request, output, requirements, executionType),
                output, executionType);
    }

    @FunctionalInterface
    private interface ExecutionBody {
        CommandResult get() throws Throwable;
    }

    /**
     * 两条 executeWithResult 入口共用的执行流程：建立 System 输出重定向，执行 body，
     * 并统一做异常转换与收尾。日志内容与顺序与原先逐条入口保持一致。
     */
    private void executeWithRedirect(ExecutionBody body, ICommandOutputHandler output, CommandType executionType) {
        // JSON 模式下用 VoidOutputHandler 拦截 System.out，
        // 防止 context.console().println() 通过 SystemOutputRedirector 泄漏到客户端
        ICommandOutputHandler redirectTarget = (executionType == CommandType.USER_INTERFACE)
                ? new VoidOutputHandler()
                : output;
        SystemOutputRedirector redirector = new SystemOutputRedirector(redirectTarget);
        CommandResult result = null;
        try {
            redirector.startRedirect();
            result = body.get();
        } catch (Throwable t) {
            result = handleExecutionError(t, output, executionType);
        } finally {
            redirector.stopRedirect();
            logger.info("命令执行完毕");
            if (result != null) {
                output.finish(result);
            }
            output.close();
            cleanup();
        }
        logger.info("命令执行完成");
    }

    private CommandResult handleExecutionError(Throwable t, ICommandOutputHandler output, CommandType executionType) {
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
            return null;
        } else {
            CommandResult result = new CommandResult();
            CommandResult.ErrorInfo error = new CommandResult.ErrorInfo("INTERNAL_ERROR", t.getMessage());
            error.setStacktrace(t);
            result.setSuccess(false);
            result.setMessage(t.getMessage());
            result.setError(error);
            return result;
        }
    }


    private CommandResult executeCommandInternal(String fullCommand, ICommandOutputHandler output,
            ClientRequirements requirements, CommandType executionType) throws Throwable {
        fullCommand = fullCommand.trim();
        if (fullCommand.isEmpty()) {
            output.println("没有指定命令 (可以用help来获取帮助)", Colors.RED);
            return null;
        }

        fullCommand = AliasMain.resolveAlias(fullCommand);

        CommandArgumentParser.ParseResult parseResult = CommandArgumentParser.parseOptions(fullCommand, logger);

        if (parseResult.classLoader() != null) {
            setTargetPackage(parseResult.classLoader());
        }

        String[] commandParams = CommandArgumentParser.splitArguments(parseResult.commandLine());
        if (commandParams.length == 0) {
            output.println("没有指定命令 (可以用help来获取帮助)", Colors.ORANGE);
            return null;
        }

        String command = commandParams[0];

        // 去掉命令本身，只保留参数
        String[] args = new String[commandParams.length - 1];
        System.arraycopy(commandParams, 1, args, 0, args.length);

        return buildContextAndDispatch(command, args, null, output, requirements, executionType);
    }

    private CommandResult executeCommandInternal(CommandRequest<?> request, ICommandOutputHandler output,
                                       ClientRequirements requirements, CommandType executionType) throws Throwable {
        return buildContextAndDispatch(request.getCommandType(), new String[0], request,
                output, requirements, executionType);
    }

    /**
     * 两个 executeCommandInternal 重载共用的尾段：建执行上下文 → 预置 request → 分发执行。
     */
    private CommandResult buildContextAndDispatch(String cmdName, String[] args,
            CommandRequest<?> request, ICommandOutputHandler output,
            ClientRequirements requirements, CommandType executionType) throws Throwable {
        // 文案语言在这里定，而不是让各渲染点自己读 Locale：服务端可能要服务多个客户端
        // （GUI + 终端 + agent），它们的界面语言可以不一样，所以语言是按请求、按线程的。
        // 语言由客户端在 sys.hello 里声明；没声明（老客户端 / agent 内部调用）才回落到本进程 Locale。
        CliMessages.useLanguage(requirements != null ? requirements.getLanguage() : null);

        // 创建执行上下文
        CmdExecContext<CommandRequest<?>> context = new CmdExecContext<>(
            cmdName,
            args,
            getTargetPackage(),
            getClassLoader(),
            executionType == CommandType.COMMAND_LINE ? output : new VoidOutputHandler(),
            requirements
        );
        if (request != null) {
            context.setRequest(request);
        }
        context.setExecutionType(executionType);

        // 使用命令注册表分发命令
        return dispatchAndExecute(context, executionType, output);
    }

    private CommandResult dispatchAndExecute(CmdExecContext<CommandRequest<?>> context, CommandType executionType,
                            ICommandOutputHandler origOutput)
            throws Throwable {
        // 非 CLI 模式下，context.output 是 VoidOutputHandler（丢弃直接输出），
        // 但 Console 应来自 origOutput（InteractiveOutputHandler），让 RichConsole 可用。
        // 注意用 Supplier 懒取而不是立刻取：立刻取会构造 JLine ExternalTerminal（实测 ~530ms），
        // 而绝大多数命令根本不渲染 RichConsole，这个代价纯属白付。
        if (executionType != CommandType.COMMAND_LINE && origOutput != null) {
            context.setConsoleSupplier(origOutput::getConsole);
        }

        String command = context.cmdName();
        ICommandOutputHandler output = context.output();
        MainCommand<? extends CommandResult> commandObj = getCommand(command);

        // 命令名可能自带子路径（路由 key 形式，如 "class/info"、"threads/profile/start"）。
        // 命令注册表里只有主命令名，子路径需要拆出来交给 CommandRouter 的路由表。
        if (commandObj == null) {
            int separatorIndex = command.indexOf('/');
            if (separatorIndex > 0) {
                String baseCmd = command.substring(0, separatorIndex);
                String subPath = command.substring(separatorIndex + 1);
                commandObj = getCommand(baseCmd);
                if (commandObj != null) {
                    logger.info("命令名包含子路径: " + command + " → 主命令: " + baseCmd + ", 子路径: " + subPath);
                    context.cmdName = baseCmd;
                    // 多级子路径要拆成多个 arg，matchRoute 才能逐段匹配（如 profile/start）
                    context.args = subPath.split("/");
                    command = baseCmd;
                }
            }
        }

        if (commandObj == null || !commandObj.getClass().isAnnotationPresent(Cmd.class)) {
            // 命令未注册
            if (executionType != CommandType.COMMAND_LINE) {
                logger.error("在非命令行模式下执行未知命令: " + command);
                throw new RuntimeException("未知的命令: " + command + ", 输入help获取帮助");
            }
            output.println("未知的命令: " + command + ", 输入help获取帮助", Colors.ORANGE);
            return null;
        }

        try {
            // 唯一执行入口：命中路由 → 路由处理器；无路由定义的命令 → CommandRouter 内部回退 execute()
            CommandResult result = CommandRouter.getInstance().dispatch(context);
            logger.info("命令执行成功: " + command);
            return result;

        } catch (IllegalArgumentException e) {
            return handleParameterError(e, command, context, commandObj, output, executionType);

        } catch (InvocationTargetException e) {
            Throwable cause = e.getTargetException() != null ? e.getTargetException() : e;
            if (cause instanceof IllegalArgumentException iae) {
                return handleParameterError(iae, command, context, commandObj, output, executionType);
            }
            logger.error("命令执行出错: " + cause.getClass().getSimpleName() +
                       " - " + cause.getMessage(), cause);
            throw cause;
        }
    }

    /**
     * 参数错误统一处理：CLI 模式下展示（子）命令帮助并返回 null；
     * 其余模式抛出，由 {@link #handleExecutionError} 转为结构化错误结果。
     */
    private CommandResult handleParameterError(IllegalArgumentException e, String command,
                                               CmdExecContext<CommandRequest<?>> context,
                                               MainCommand<? extends CommandResult> commandObj,
                                               ICommandOutputHandler output, CommandType executionType) {
        logger.warn("命令参数错误: " + e.getMessage());
        if (executionType != CommandType.COMMAND_LINE) {
            throw e;
        }

        output.println("参数错误: " + e.getMessage(), Colors.RED);
        output.println("", Colors.DEFAULT);

        // 子命令级帮助优先（匹配到子命令时只显示该子命令的帮助）
        String helpText = CommandRouter.getInstance().generateHelpForRoute(command, context.args());
        if (helpText != null && !helpText.isEmpty()) {
            output.println(helpText, Colors.WHITE);
        } else {
            output.println(commandObj.getHelpText(), Colors.WHITE);
        }
        return null;
    }

    public String executeShellCommand(String fullCommand) {
        StringBuilderCollector collector = new StringBuilderCollector();
        execute(fullCommand, collector);
        return collector.getString();
    }


    public static class CmdExecContext<T extends CommandRequest<?>> {
        public String cmdName;
        public String[] args;
        public String targetPackage;
        public ClassLoader classLoader;
        public ICommandOutputHandler output;
        private com.justnothing.richconsole.console.Console console;

        /**
         * Console 的懒加载来源。非 CLI 模式下 {@code output} 是 VoidOutputHandler（无 Console），
         * 真正的 Console 来自原始的 InteractiveOutputHandler；用 Supplier 延迟到首次
         * 真正渲染时才构造，避免"命令不渲染也付终端代价"。
         */
        private Supplier<com.justnothing.richconsole.console.Console> consoleSupplier;

        public void setConsoleSupplier(Supplier<com.justnothing.richconsole.console.Console> supplier) {
            this.consoleSupplier = supplier;
        }

        private T request;
        private CommandType executionType = CommandType.COMMAND_LINE;

        /**
         * 客户端能力（终端宽高 / ANSI / 输入支持）。
         *
         * <p>以前构造器收到它就扔了，导致需要它的命令（比如 agent 代理执行时要把能力
         * 透传给目标进程）只能自己瞎猜，猜出来的宽高是 0x0、ANSI 是不支持 ——
         * 渲染结果就是一片空白。</p>
         */
        private ClientRequirements requirements;

        public String cmdName() { return cmdName; }
        public String[] args() { return args; }
        public String targetPackage() { return targetPackage; }
        public ClassLoader classLoader() { return classLoader; }
        public ICommandOutputHandler output() { return output; }

        /** 客户端能力；可能为 null（非交互式调用时没有握手信息）。 */
        public ClientRequirements requirements() { return requirements; }

        public T getRequest() { return request; }
        public void setRequest(T r) { this.request = r; }

        public CommandType getExecutionType() { return executionType; }
        public void setExecutionType(CommandType t) { this.executionType = t; }

        public boolean isCli() { return executionType == CommandType.COMMAND_LINE; }

        public T getCommandRequest() {
            return request;
        }

        public CmdExecContext(String cmdName, String[] args,
                              String targetPackage, ClassLoader classLoader,
                              ICommandOutputHandler output,
                              ClientRequirements requirements) {
            this.cmdName = cmdName;
            this.args = args;
            this.targetPackage = targetPackage;
            this.classLoader = classLoader;
            this.output = output;
            this.requirements = requirements;
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

        /**
         * 获取 RichConsole Console 实例（用于高级渲染：表格、面板、进度条等）。
         * 仅在 InteractiveOutputHandler 下可用，其他输出目标返回 null。
         */
        public com.justnothing.richconsole.console.Console console() {
            if (console != null) return console;
            if (consoleSupplier != null) {
                com.justnothing.richconsole.console.Console supplied = consoleSupplier.get();
                if (supplied != null) {
                    console = supplied;
                    return console;
                }
            }
            return output.getConsole();
        }

        /**
         * RichConsole 的渲染结果能不能真的到达用户眼前。
         */
        public boolean supportsRichOutput() {
            return output != null && output.supportsRichRendering();
        }
    }

    /**
     * @Cmd.group() → help 小节标题；未列出的 group 原样作为小节标题。
     *
     * <p>写成方法而不是静态 Map：语言是运行时可变的（{@code CliMessages.useLanguage}），
     * 静态 Map 会在类加载那一刻就把当时的语言固化下来。</p>
     */
    private static String groupTitle(String group) {
        switch (group) {
            case "general": return CliMessages.HELP_GROUP_GENERAL.text();
            case "system": return CliMessages.HELP_GROUP_SYSTEM.text();
            case "fun": return CliMessages.HELP_GROUP_FUN.text();
            default: return group;
        }
    }

    private static final String DEFAULT_COMMAND_GROUP = "general";

    /**
     * 从面向用户的命令清单 {@link CommandCatalog#USER_FACING} 派生「小节标题 + 命令名 - 描述」清单。
     * demo/test 命令（{@link CommandCatalog#DEBUG_ONLY}）仍然可执行，但不在这里列出。
     *
     * <p>命令按 {@code @Cmd.group()} 分小节：已知 group 映射为中文小节名，未知 group 原样作标题，
     * 未设置 group 的命令归入默认小节。单个命令读取注解失败只跳过它，不影响整体 help 渲染。</p>
     */
    private static String buildCommandList() {
        Map<String, List<String>> sections = new LinkedHashMap<>();
        for (Class<? extends MainCommand<?>> cmdClass : CommandCatalog.USER_FACING) {
            try {
                Cmd cmd = cmdClass.getAnnotation(Cmd.class);
                if (cmd == null) {
                    continue;
                }
                String group = (cmd.group() == null || cmd.group().isEmpty())
                        ? DEFAULT_COMMAND_GROUP : cmd.group();
                sections.computeIfAbsent(group, k -> new ArrayList<>())
                        .add(String.format("  %-34s - %s", cmd.name(), CliTexts.resolve(cmd.description())));
            } catch (Throwable t) {
                logger.warn("渲染 help 命令清单时跳过 " + cmdClass.getSimpleName() + ": " + t.getMessage());
            }
        }

        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, List<String>> entry : sections.entrySet()) {
            sb.append('\n')
              .append(groupTitle(entry.getKey()))
              .append(":\n");
            for (String line : entry.getValue()) {
                sb.append(line).append('\n');
            }
        }
        return sb.toString();
    }

    public static String getHelpText() {
        return CliMessages.HELP_BANNER.format(MAIN_MODULE_VER, buildCommandList());
    }
}
