package com.justnothing.testmodule.command.functions.classcmd;

import static com.justnothing.testmodule.constants.CommandServer.CMD_CLASS_VER;

import com.justnothing.testmodule.command.CommandExecutor;
import com.justnothing.testmodule.command.base.MainCommand;
import com.justnothing.testmodule.command.base.command.Cmd;
import com.justnothing.testmodule.command.base.command.CmdRoutes;
import com.justnothing.testmodule.command.base.command.CommandRouter;
import com.justnothing.testmodule.command.base.protocol.CommandRequest;
import com.justnothing.testmodule.command.functions.classcmd.ClassCommandResult;
import com.justnothing.testmodule.command.functions.classcmd.request.*;
import com.justnothing.testmodule.command.functions.classcmd.impl.*;
import com.justnothing.testmodule.command.output.Colors;

@Cmd(
    name = "class",
    group = "system",
    description = "查看类的详细信息, 包括继承关系, 接口, 构造函数等",
    version = CMD_CLASS_VER,
    defaultResultType = ClassCommandResult.class
)
@CmdRoutes({
    @CmdRoutes.Route(path = "info", request = ClassInfoRequest.class,
            handler = InfoCommand.class, description = "查看类的详细信息"),
    @CmdRoutes.Route(path = "graph", request = ClassGraphRequest.class,
            handler = GraphCommand.class, description = "生成类继承图"),
    @CmdRoutes.Route(path = "analyze", request = AnalyzeClassRequest.class,
            handler = AnalyzeCommand.class, description = "分析类的字段和方法"),
    @CmdRoutes.Route(path = "list", request = MethodListRequest.class,
            handler = ListCommand.class, description = "列出一个类的所有方法"),
    @CmdRoutes.Route(path = "invoke", request = InvokeMethodRequest.class,
            handler = InvokeCommand.class, description = "调用类中的方法"),
    @CmdRoutes.Route(path = "field", request = FieldRequest.class,
            handler = FieldCommand.class, description = "查看或操作字段"),
    @CmdRoutes.Route(path = "constructor", request = InvokeConstructorRequest.class,
            handler = ConstructorCommand.class, description = "创建类的实例"),
    @CmdRoutes.Route(path = "reflect", request = ReflectClassRequest.class,
            handler = ReflectCommand.class, description = "使用反射访问和操作类的私有成员")
})
public class ClassMain extends MainCommand<ClassCommandResult> {

    public ClassMain() {
        super("class", ClassCommandResult.class);
    }

    @Override
    public String getHelpText() {
        return CommandRouter.getInstance().generateHelpForCommand("class");
    }

    @Override
    public ClassCommandResult runMain(CommandExecutor.CmdExecContext<CommandRequest> context) throws Exception {
        if (context.getRequest() != null) {
            Object request = context.getRequest();
            if (request instanceof ClassInfoRequest) {
                return new InfoCommand().execute(context);
            } else if (request instanceof ClassGraphRequest) {
                return new GraphCommand().execute(context);
            } else if (request instanceof AnalyzeClassRequest) {
                return new AnalyzeCommand().execute(context);
            } else if (request instanceof MethodListRequest) {
                return new ListCommand().execute(context);
            } else if (request instanceof InvokeMethodRequest) {
                return new InvokeCommand().execute(context);
            } else if (request instanceof FieldRequest) {
                return new FieldCommand().execute(context);
            } else if (request instanceof InvokeConstructorRequest) {
                return new ConstructorCommand().execute(context);
            } else if (request instanceof ReflectClassRequest) {
                return new ReflectCommand().execute(context);
            }
        }

        context.println(getHelpText(), Colors.WHITE);
        return createErrorResult("请指定子命令: class <subcmd> [args...]");
    }
}
