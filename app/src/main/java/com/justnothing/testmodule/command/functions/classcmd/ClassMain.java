package com.justnothing.testmodule.command.functions.classcmd;

import static com.justnothing.testmodule.constants.CommandServer.CMD_CLASS_VER;

import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.justnothing.testmodule.command.CommandExecutor;
import com.justnothing.testmodule.command.base.IllegalCommandLineArgumentException;
import com.justnothing.testmodule.command.base.MainCommand;
import com.justnothing.testmodule.command.base.command.Cmd;
import com.justnothing.testmodule.command.base.command.CmdParamProcessor;
import com.justnothing.testmodule.command.base.command.CmdRoutes;
import com.justnothing.testmodule.command.base.command.CommandRouter;
import com.justnothing.testmodule.command.base.command.SubCommands;
import com.justnothing.testmodule.command.base.command.SubCommand;
import com.justnothing.testmodule.command.base.command.SupportsRequests;
import com.justnothing.testmodule.command.base.command.RegisterParser;
import com.justnothing.testmodule.command.base.command.RegisterCommand;
import com.justnothing.testmodule.command.base.command.CommandInfo;
import com.justnothing.testmodule.command.base.protocol.CommandRequest;
import com.justnothing.testmodule.command.functions.classcmd.parser.ClassCommandParser;
import com.justnothing.testmodule.command.functions.classcmd.request.AnalyzeClassRequest;
import com.justnothing.testmodule.command.functions.classcmd.request.ClassGraphRequest;
import com.justnothing.testmodule.command.functions.classcmd.request.ClassInfoRequest;
import com.justnothing.testmodule.command.functions.classcmd.request.ClassHierarchyRequest;
import com.justnothing.testmodule.command.functions.classcmd.request.FieldRequest;
import com.justnothing.testmodule.command.functions.classcmd.request.GetFieldValueRequest;
import com.justnothing.testmodule.command.functions.classcmd.request.InvokeConstructorRequest;
import com.justnothing.testmodule.command.functions.classcmd.request.InvokeMethodRequest;
import com.justnothing.testmodule.command.functions.classcmd.request.MethodListRequest;
import com.justnothing.testmodule.command.functions.classcmd.request.ReflectClassRequest;
import com.justnothing.testmodule.command.functions.classcmd.request.SearchClassRequest;
import com.justnothing.testmodule.command.functions.classcmd.request.SetFieldValueRequest;
import com.justnothing.testmodule.command.functions.classcmd.response.ClassInfoResult;          // ← 新增
import com.justnothing.testmodule.command.functions.classcmd.response.ClassGraphResult;        // ← 新增
import com.justnothing.testmodule.command.functions.classcmd.response.AnalyzeReportResult;    // ← 新增
import com.justnothing.testmodule.command.functions.classcmd.response.MethodListResult;       // ← 新增
import com.justnothing.testmodule.command.functions.classcmd.response.InvokeMethodResult;     // ← 新增
import com.justnothing.testmodule.command.functions.classcmd.response.GetFieldValueResult;   // ← 新增
import com.justnothing.testmodule.command.functions.classcmd.response.InvokeConstructorResult;// ← 新增
import com.justnothing.testmodule.command.functions.classcmd.response.ReflectOperationResult; // ← 新增
import com.justnothing.testmodule.command.functions.classcmd.impl.AnalyzeCommand;
import com.justnothing.testmodule.command.functions.classcmd.impl.ConstructorCommand;
import com.justnothing.testmodule.command.functions.classcmd.impl.FieldCommand;
import com.justnothing.testmodule.command.functions.classcmd.impl.GraphCommand;
import com.justnothing.testmodule.command.functions.classcmd.impl.InfoCommand;
import com.justnothing.testmodule.command.functions.classcmd.impl.InvokeCommand;
import com.justnothing.testmodule.command.functions.classcmd.impl.ListCommand;
import com.justnothing.testmodule.command.functions.classcmd.impl.ReflectCommand;
import com.justnothing.testmodule.command.output.Colors;
import com.justnothing.testmodule.command.utils.CommandExceptionHandler;

@Cmd(
    name = "class",
    group = "system",
    description = "查看类的详细信息, 包括继承关系, 接口, 构造函数等",
    version = CMD_CLASS_VER,
    defaultResultType = ClassCommandResult.class
)
@CommandInfo(
    name = "class",
    group = "classcmd",
    description = "查看类的详细信息, 包括继承关系, 接口, 构造函数等",
    version = CMD_CLASS_VER,
    resultType = ClassCommandResult.class
)
@CmdRoutes({
    @CmdRoutes.Route(
        path = "info",
        request = ClassInfoRequest.class,
        handler = InfoCommand.class,
        description = "查看类的详细信息"
    ),
    @CmdRoutes.Route(
        path = "graph",
        request = ClassGraphRequest.class,
        handler = GraphCommand.class,
        description = "生成类继承图"
    ),
    @CmdRoutes.Route(
        path = "analyze",
        request = AnalyzeClassRequest.class,
        handler = AnalyzeCommand.class,
        description = "分析类的字段和方法"
    ),
    @CmdRoutes.Route(
        path = "list",
        request = MethodListRequest.class,
        handler = ListCommand.class,
        description = "列出一个类的所有方法"
    ),
    @CmdRoutes.Route(
        path = "invoke",
        request = InvokeMethodRequest.class,
        handler = InvokeCommand.class,
        description = "调用类中的方法"
    ),
    @CmdRoutes.Route(
        path = "field",
        request = FieldRequest.class,
        handler = FieldCommand.class,
        description = "查看或操作字段"
    ),
    @CmdRoutes.Route(
        path = "constructor",
        request = InvokeConstructorRequest.class,
        handler = ConstructorCommand.class,
        description = "创建类的实例"
    ),
    @CmdRoutes.Route(
        path = "reflect",
        request = ReflectClassRequest.class,
        handler = ReflectCommand.class,
        description = "使用反射访问和操作类的私有成员"
    )
})
@SubCommands({
    @SubCommand(value = "info", request = ClassInfoRequest.class, result = ClassInfoResult.class, command = InfoCommand.class, description = "查看类的详细信息"),
    @SubCommand(value = "graph", request = ClassGraphRequest.class, result = ClassGraphResult.class, command = GraphCommand.class, description = "生成类继承图"),
    @SubCommand(value = "analyze", request = AnalyzeClassRequest.class, result = AnalyzeReportResult.class, command = AnalyzeCommand.class, description = "分析类的字段和方法"),
    @SubCommand(value = "list", request = MethodListRequest.class, result = MethodListResult.class, command = ListCommand.class, description = "列出一个类的所有方法"),
    @SubCommand(value = "invoke", request = InvokeMethodRequest.class, result = InvokeMethodResult.class, command = InvokeCommand.class, description = "调用类中的方法"),
    @SubCommand(value = "field", request = GetFieldValueRequest.class, result = GetFieldValueResult.class, command = FieldCommand.class, description = "查看或操作字段"),
    @SubCommand(value = "constructor", request = InvokeConstructorRequest.class, result = InvokeConstructorResult.class, command = ConstructorCommand.class, description = "创建类的实例"),
    @SubCommand(value = "reflect", request = ReflectClassRequest.class, result = ReflectOperationResult.class, command = ReflectCommand.class, description = "使用反射访问和操作类的私有成员")
})
@RegisterCommand("class")
@SupportsRequests({
    ClassInfoRequest.class,
    ClassGraphRequest.class,
    ClassHierarchyRequest.class,
    AnalyzeClassRequest.class,
    GetFieldValueRequest.class,
    InvokeConstructorRequest.class,
    InvokeMethodRequest.class,
    MethodListRequest.class,
    ReflectClassRequest.class,
    SearchClassRequest.class,
    SetFieldValueRequest.class
})
@RegisterParser(ClassCommandParser.class)
public class ClassMain extends MainCommand<ClassCommandResult> {

    private final Map<String, Class<? extends ClassCommand<?>>> subCommandMap = new ConcurrentHashMap<>();

    public ClassMain() {
        super("class", ClassCommandResult.class);
        
        registerSubCommand("info", InfoCommand.class);
        registerSubCommand("graph", GraphCommand.class);
        registerSubCommand("analyze", AnalyzeCommand.class);
        registerSubCommand("list", ListCommand.class);
        registerSubCommand("invoke", InvokeCommand.class);
        registerSubCommand("field", FieldCommand.class);
        registerSubCommand("constructor", ConstructorCommand.class);
        registerSubCommand("reflect", ReflectCommand.class);
        
        CommandRouter.getInstance().registerCommand(ClassMain.class);
        
        logger.info("✅ ClassMain 初始化完成 (新架构), 已注册 9 个子命令");
    }

    public ClassMain(String commandName) {
        super(commandName, ClassCommandResult.class);
    }

    private void registerSubCommand(String name, Class<? extends ClassCommand<?>> commandClass) {
        subCommandMap.put(name.toLowerCase(), commandClass);
        logger.debug("注册子命令: " + name + " -> " + commandClass.getSimpleName());
    }

    @Override
    public String getHelpText() {
        return CommandRouter.getInstance().generateHelpForCommand("class");
    }

    @Override
    public ClassCommandResult runMain(CommandExecutor.CmdExecContext<CommandRequest> context) throws Exception {
        String[] args = context.args();

        logger.debug("执行class命令（使用新架构），参数: " + Arrays.toString(args));

        try {
            if (args.length < 1) {
                if (context.getRequest() != null) {
                    return handleGuiMode(context);
                }
                context.println(getHelpText(), Colors.WHITE);
                return createErrorResult("请指定子命令: class <subcmd> [args...]");
            }

            String subCmdName = args[0];
            String[] remainingArgs = (args.length > 1) 
                ? Arrays.copyOfRange(args, 1, args.length) 
                : new String[0];

            ClassCommand<?> command = resolveAndCreateCommand(subCmdName);
            
            if (command == null) {
                context.print("未知子命令: ", Colors.RED);
                context.println(subCmdName, Colors.YELLOW);
                context.println("\n可用的子命令:", Colors.WHITE);
                context.println(getHelpText(), Colors.WHITE);
                return createErrorResult("未知子命令: " + subCmdName);
            }

            CommandRequest parsedRequest = parseRequestForCommand(subCmdName, remainingArgs, context);
            context.setRequest(parsedRequest);

            logger.debug("CLI模式: 子命令=" + subCmdName + 
                       ", Request类型=" + parsedRequest.getClass().getSimpleName());

            return command.execute(context);

        } catch (IllegalCommandLineArgumentException e) {
            // 丢给上层，上层看到了会展示命令详细帮助
            throw e;
        } catch (Exception e) {
            CommandExceptionHandler.handleException(
                "class " + (args.length >= 1 ? args[0] : "unknown"),
                e,
                context,
                "执行class命令失败"
            );
            return createErrorResult("执行class命令失败: " + e.getMessage());
        }
    }

    private ClassCommand<?> resolveAndCreateCommand(String subCmdName) {
        Class<? extends ClassCommand<?>> commandClass = subCommandMap.get(subCmdName.toLowerCase());
        if (commandClass == null) return null;

        try {
            return commandClass.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            logger.error("创建子命令实例失败: " + subCmdName, e);
            return null;
        }
    }

    private CommandRequest parseRequestForCommand(String subCmdName, String[] args,
                                                  CommandExecutor.CmdExecContext<CommandRequest> context)
            throws Exception {
        CommandRouter.RouteMatch match = CommandRouter.getInstance()
            .matchRoute("class", new String[]{subCmdName});

        if (match != null && match.routeConfig() != null) {
            Class<? extends CommandRequest> requestType = match.routeConfig().requestType();
            CommandRequest request = requestType.getDeclaredConstructor().newInstance();

            // 使用统一的智能解析入口
            return CmdParamProcessor.parseRequest(request, args);
        }

        logger.warn("⚠️ 未找到路由配置，返回默认 ClassInfoRequest");
        return new ClassInfoRequest();
    }

    private ClassCommandResult handleGuiMode(CommandExecutor.CmdExecContext<CommandRequest> context) throws Exception {
        CommandRequest request = context.getRequest();
        
        logger.debug("GUI模式: 从Request注解解析子命令: " + request.getClass().getSimpleName());
        
        ClassCommand<?> command = resolveDirectCommand(request);
        if (command == null) {
            command = resolveCommandFromRequest(request);
        }
        
        if (command == null) {
            context.println("无法找到对应的子命令处理", Colors.RED);
            context.println(getHelpText(), Colors.WHITE);
            return createErrorResult("无法找到对应的子命令处理");
        }
        
        return command.execute(context);
    }

    private ClassCommand<?> resolveDirectCommand(CommandRequest request) {
        if (request instanceof ClassHierarchyRequest) {
            logger.debug("解析DirectCommand: ClassHierarchyCommand");
            return new com.justnothing.testmodule.command.functions.classcmd.impl.ClassHierarchyCommand();
        }
        if (request instanceof SetFieldValueRequest) {
            logger.debug("解析DirectCommand: FieldCommand (setfield)");
            return new com.justnothing.testmodule.command.functions.classcmd.impl.FieldCommand();
        }
        return null;
    }

    private ClassCommand<?> resolveCommandFromRequest(CommandRequest request) {
        CommandRouter.RouteMatch match = CommandRouter.getInstance()
            .matchRouteByRequest(request.getClass());
            
        if (match != null && match.routeConfig() != null) {
            try {
                return (ClassCommand<?>) match.routeConfig().handlerType().getDeclaredConstructor().newInstance();
            } catch (Exception e) {
                logger.error("从Request类型创建Handler失败: " + e.getMessage());
            }
        }
        
        return null;
    }
}
