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
    description = ClassTexts.CMD_CLASS_DESC,
    version = CMD_CLASS_VER
)
@CmdRoutes({
    @CmdRoutes.Route(path = "info", request = ClassInfoRequest.class,
            handler = ClassInfoCommand.class, description = ClassTexts.ROUTE_CLASS_INFO_DESC),
    @CmdRoutes.Route(path = "graph", request = ClassGraphRequest.class,
            handler = ClassGraphCommand.class, description = ClassTexts.ROUTE_CLASS_GRAPH_DESC),
    @CmdRoutes.Route(path = "analyze", request = AnalyzeClassRequest.class,
            handler = ClassAnalyzeCommand.class, description = ClassTexts.ROUTE_CLASS_ANALYZE_DESC),
    @CmdRoutes.Route(path = "list", request = MethodListRequest.class,
            handler = ClassListCommand.class, description = ClassTexts.ROUTE_CLASS_LIST_DESC),
    @CmdRoutes.Route(path = "invoke", request = InvokeMethodRequest.class,
            handler = ClassInvokeCommand.class, description = ClassTexts.ROUTE_CLASS_INVOKE_DESC),
    @CmdRoutes.Route(path = "field", request = FieldRequest.class,
            handler = ClassFieldCommand.class, description = ClassTexts.ROUTE_CLASS_FIELD_DESC),
    @CmdRoutes.Route(path = "constructor", request = InvokeConstructorRequest.class,
            handler = ClassConstructorCommand.class, description = ClassTexts.ROUTE_CLASS_CONSTRUCTOR_DESC),
    @CmdRoutes.Route(path = "reflect", request = ReflectClassRequest.class,
            handler = ClassReflectCommand.class, description = ClassTexts.ROUTE_CLASS_REFLECT_DESC),
    @CmdRoutes.Route(path = "hierarchy", request = ClassHierarchyRequest.class,
            handler = ClassHierarchyCommand.class, description = ClassTexts.ROUTE_CLASS_HIERARCHY_DESC)
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
