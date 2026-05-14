package com.justnothing.testmodule.command.functions.classcmd;

import com.justnothing.testmodule.command.base.*;
import static com.justnothing.testmodule.constants.CommandServer.CMD_CLASS_VER;

import com.justnothing.testmodule.command.CommandExecutor;
import com.justnothing.testmodule.command.base.command.CommandInfo;
import com.justnothing.testmodule.command.base.command.RegisterCommand;
import com.justnothing.testmodule.command.base.command.RegisterParser;
import com.justnothing.testmodule.command.base.command.SubCommand;
import com.justnothing.testmodule.command.base.command.SubCommands;
import com.justnothing.testmodule.command.base.command.SupportsRequests;
import com.justnothing.testmodule.command.base.protocol.CommandRequest;
import com.justnothing.testmodule.command.functions.classcmd.parser.ClassCommandParser;
import com.justnothing.testmodule.command.functions.classcmd.request.AnalyzeClassRequest;
import com.justnothing.testmodule.command.functions.classcmd.request.ClassGraphRequest;
import com.justnothing.testmodule.command.functions.classcmd.request.ClassHierarchyRequest;
import com.justnothing.testmodule.command.functions.classcmd.request.ClassInfoRequest;
import com.justnothing.testmodule.command.functions.classcmd.request.GetFieldValueRequest;
import com.justnothing.testmodule.command.functions.classcmd.request.InvokeConstructorRequest;
import com.justnothing.testmodule.command.functions.classcmd.request.InvokeMethodRequest;
import com.justnothing.testmodule.command.functions.classcmd.request.MethodListRequest;
import com.justnothing.testmodule.command.functions.classcmd.request.ReflectClassRequest;
import com.justnothing.testmodule.command.functions.classcmd.request.SearchClassRequest;
import com.justnothing.testmodule.command.functions.classcmd.request.SetFieldValueRequest;
import com.justnothing.testmodule.command.functions.classcmd.response.AnalyzeReportResult;
import com.justnothing.testmodule.command.functions.classcmd.response.ClassGraphResult;
import com.justnothing.testmodule.command.functions.classcmd.response.ClassInfoResult;
import com.justnothing.testmodule.command.functions.classcmd.response.GetFieldValueResult;
import com.justnothing.testmodule.command.functions.classcmd.response.InvokeConstructorResult;
import com.justnothing.testmodule.command.functions.classcmd.response.InvokeMethodResult;
import com.justnothing.testmodule.command.functions.classcmd.response.MethodListResult;
import com.justnothing.testmodule.command.functions.classcmd.response.ReflectOperationResult;
import com.justnothing.testmodule.command.functions.classcmd.response.SearchResult;
import com.justnothing.testmodule.command.functions.classcmd.impl.InfoCommand;
import com.justnothing.testmodule.command.functions.classcmd.impl.GraphCommand;
import com.justnothing.testmodule.command.functions.classcmd.impl.AnalyzeCommand;
import com.justnothing.testmodule.command.functions.classcmd.impl.ListCommand;
import com.justnothing.testmodule.command.functions.classcmd.impl.InvokeCommand;
import com.justnothing.testmodule.command.functions.classcmd.impl.FieldCommand;
import com.justnothing.testmodule.command.functions.classcmd.impl.ConstructorCommand;
import com.justnothing.testmodule.command.functions.classcmd.impl.SearchCommand;
import com.justnothing.testmodule.command.functions.classcmd.impl.ReflectCommand;
import com.justnothing.testmodule.command.output.Colors;
import com.justnothing.testmodule.command.utils.CommandExceptionHandler;

import java.util.Arrays;

@CommandInfo(
    name = "class",
    group = "classcmd",
    description = "查看类的详细信息, 包括继承关系, 接口, 构造函数等",
    helpText = """
            语法: class <subcmd> [args...]
            
            查看类的详细信息, 包括继承关系, 接口, 构造函数等.
            (输入 class <subcmd> 查看子命令帮助)
            
            注意:
                - 所有操作默认支持访问私有成员 (使用 setAccessible(true))
                - 使用 --super 选项访问父类成员
                - 使用 --interfaces 选项访问接口成员
                - 方法参数必须使用 Type:value 格式
            
            子命令:
                info [options] <class_name>                    - 查看类的详细信息
                graph <class_name>                             - 生成类继承图
                analyze [options] <class_name>                 - 分析类的字段和方法
                list [options] <class_name>                    - 列出一个类的所有方法
                invoke [options] <class_name> <method_name> [params...]  - 调用类中的方法
                field [options] <class_name> [field_name]      - 查看或操作字段
                constructor <class_name> [params...]           - 创建类的实例
                search <subcmd> <pattern>                      - 搜索类, 方法, 字段或注解
                reflect <class> <type> <name> [options]       - 使用反射访问和操作类的私有成员
            
            info 选项:
                -v, --verbose       显示详细信息
                -i, --interfaces    显示实现的接口
                -c, --constructors  显示构造函数
                -s, --super         显示父类信息
                -m, --modifiers     显示修饰符信息
                -a, --all           显示所有信息 (默认)
            
            (Submodule class %s)
            """,
    version = CMD_CLASS_VER,
    resultType = ClassCommandResult.class
)
@SubCommands({
    @SubCommand(value = "info", request = ClassInfoRequest.class, result = ClassInfoResult.class, command = InfoCommand.class, description = "查看类的详细信息"),
    @SubCommand(value = "graph", request = ClassGraphRequest.class, result = ClassGraphResult.class, command = GraphCommand.class, description = "生成类继承图"),
    @SubCommand(value = "analyze", request = AnalyzeClassRequest.class, result = AnalyzeReportResult.class, command = AnalyzeCommand.class, description = "分析类的字段和方法"),
    @SubCommand(value = "list", request = MethodListRequest.class, result = MethodListResult.class, command = ListCommand.class, description = "列出一个类的所有方法"),
    @SubCommand(value = "invoke", request = InvokeMethodRequest.class, result = InvokeMethodResult.class, command = InvokeCommand.class, description = "调用类中的方法"),
    @SubCommand(value = "field", request = GetFieldValueRequest.class, result = GetFieldValueResult.class, command = FieldCommand.class, description = "查看或操作字段"),
    @SubCommand(value = "constructor", request = InvokeConstructorRequest.class, result = InvokeConstructorResult.class, command = ConstructorCommand.class, description = "创建类的实例"),
    @SubCommand(value = "search", request = SearchClassRequest.class, result = SearchResult.class, command = SearchCommand.class, description = "搜索类, 方法, 字段或注解"),
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

    public ClassMain() {
        super("class", ClassCommandResult.class);
    }
    
    public ClassMain(String commandName) {
        super(commandName, ClassCommandResult.class);
    }

    @Override
    @SuppressWarnings("unchecked")
    public ClassCommandResult runMain(CommandExecutor.CmdExecContext<CommandRequest> context) throws Exception {
        String cmdName = context.cmdName();
        String[] args = context.args();
        ClassLoader classLoader = context.classLoader();
        String targetPackage = context.targetPackage();

        logger.debug("执行class命令（使用新架构），参数: " + Arrays.toString(args));
        logger.debug("目标包: " + targetPackage + ", 类加载器: " + classLoader);

        ClassCommand<? extends ClassCommandResult> command;

        if (args.length >= 1) {
            String subCmdName = args[0];
            logger.debug("CLI模式: 从args获取子命令: " + subCmdName);
            command = findSubCommandByName(subCmdName);
            if (command == null) {
                handleLegacyCommand(cmdName, context);
                return createErrorResult("未知子命令: " + subCmdName);
            }
            
            try {
                CommandRequest parsedRequest = context.parseRequest();
                if (parsedRequest != null) {
                    logger.debug("CLI模式: parseRequest成功, 类型=" + parsedRequest.getClass().getSimpleName());
                } else {
                    logger.warn("CLI模式: parseRequest返回null, 使用默认Request");
                }
            } catch (Exception e) {
                logger.warn("CLI模式: parseRequest失败: " + e.getMessage());
            }
        } else {
            CommandRequest request = context.getRequest();
            if (request != null) {
                command = (ClassCommand<? extends ClassCommandResult>)
                        resolveSubCommandFromRequest(request);
                logger.debug("GUI模式: 从Request注解解析子命令: " + request.getClass().getSimpleName());
                if (command == null) {
                    command = resolveDirectCommand(request);
                    if (command == null) {
                        handleLegacyCommand(cmdName, context);
                        return createErrorResult("无法找到对应的子命令处理");
                    }
                }
            } else {
                context.println(getHelpText(), Colors.WHITE);
                return createErrorResult("请指定子命令和参数");
            }
        }

        try {
            return command.execute(context);
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

    @SuppressWarnings("unchecked")
    private ClassCommand<? extends ClassCommandResult> findSubCommandByName(String name) {
        SubCommands subCommandsAnnotation = this.getClass().getAnnotation(SubCommands.class);
        if (subCommandsAnnotation != null) {
            for (SubCommand subCmd : subCommandsAnnotation.value()) {
                if (subCmd.value().equals(name) && !subCmd.command().getName().equals(AbstractCommand.class.getName())) {
                    try {
                        return (ClassCommand<? extends ClassCommandResult>)
                                subCmd.command().getDeclaredConstructor().newInstance();
                    } catch (Exception e) {
                        logger.error("创建子命令实例失败: " + name, e);
                        return null;
                    }
                }
            }
        }
        return null;
    }

    private ClassCommand<? extends ClassCommandResult> resolveDirectCommand(CommandRequest request) {
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

    private void handleLegacyCommand(String cmdName, CommandExecutor.CmdExecContext<?> context) {
        context.println("命令 '" + cmdName + "' 还未迁移到新架构或者根本没有这个命令", Colors.RED);
        context.println(getHelpText(), Colors.WHITE);
    }
}
