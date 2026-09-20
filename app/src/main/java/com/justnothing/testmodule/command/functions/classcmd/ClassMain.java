package com.justnothing.testmodule.command.functions.classcmd;

import static com.justnothing.testmodule.constants.CommandServer.CMD_CLASS_VER;

import com.justnothing.testmodule.command.framework.model.MainCommand;
import com.justnothing.testmodule.command.framework.annotation.Cmd;
import com.justnothing.testmodule.command.framework.annotation.CmdRoutes;
import com.justnothing.testmodule.command.framework.model.CommandRouter;
import com.justnothing.testmodule.command.functions.classcmd.request.*;
import com.justnothing.testmodule.command.functions.classcmd.impl.*;
import com.justnothing.testmodule.command.functions.classcmd.response.ClassCommandResult;

@Cmd(
    name = "class",
    group = "system",
    description = "查看类的详细信息, 包括继承关系, 接口, 构造函数等",
    version = CMD_CLASS_VER
)
@CmdRoutes({
    @CmdRoutes.Route(path = "info", request = ClassInfoRequest.class,
            handler = ClassInfoCommand.class, description = "查看类的详细信息"),
    @CmdRoutes.Route(path = "graph", request = ClassGraphRequest.class,
            handler = ClassGraphCommand.class, description = "生成类继承图"),
    @CmdRoutes.Route(path = "analyze", request = AnalyzeClassRequest.class,
            handler = ClassAnalyzeCommand.class, description = "分析类的字段和方法"),
    @CmdRoutes.Route(path = "list", request = MethodListRequest.class,
            handler = ClassListCommand.class, description = "列出一个类的所有方法"),
    @CmdRoutes.Route(path = "invoke", request = InvokeMethodRequest.class,
            handler = ClassInvokeCommand.class, description = "调用类中的方法"),
    @CmdRoutes.Route(path = "field", request = FieldRequest.class,
            handler = ClassFieldCommand.class, description = "查看或操作字段"),
    @CmdRoutes.Route(path = "constructor", request = InvokeConstructorRequest.class,
            handler = ClassConstructorCommand.class, description = "创建类的实例"),
    @CmdRoutes.Route(path = "reflect", request = ReflectClassRequest.class,
            handler = ClassReflectCommand.class, description = "使用反射访问和操作类的私有成员"),
    @CmdRoutes.Route(path = "hierarchy", request = ClassHierarchyRequest.class,
            handler = ClassHierarchyCommand.class, description = "查看类的继承层次结构")
})
public class ClassMain extends MainCommand<ClassCommandResult> {

    public ClassMain() {
        super("class", ClassCommandResult.class);
    }

    @Override
    public String getHelpText() {
        return CommandRouter.getInstance().generateHelpForCommand("class");
    }
}
