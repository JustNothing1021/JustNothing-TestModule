package com.justnothing.testmodule.command.functions.nativecmd;

import static com.justnothing.testmodule.constants.CommandServer.CMD_NATIVE_VER;

import com.justnothing.testmodule.command.CommandExecutor;
import com.justnothing.testmodule.command.base.MainCommand;
import com.justnothing.testmodule.command.base.protocol.CommandRequest;
import com.justnothing.testmodule.command.base.protocol.CommandResult;
import com.justnothing.testmodule.command.base.command.Cmd;
import com.justnothing.testmodule.command.base.command.CmdRoutes;
import com.justnothing.testmodule.command.base.command.CommandRouter;
import com.justnothing.testmodule.command.output.Colors;
import com.justnothing.testmodule.command.functions.nativecmd.impl.NativeQueryCommand;
import com.justnothing.testmodule.command.functions.nativecmd.impl.NativeManageCommand;
import com.justnothing.testmodule.command.functions.nativecmd.request.NativeListRequest;
import com.justnothing.testmodule.command.functions.nativecmd.request.NativeInfoRequest;
import com.justnothing.testmodule.command.functions.nativecmd.request.NativeCliRequest;
import com.justnothing.testmodule.command.functions.nativecmd.request.NativeSymbolsRequest;
import com.justnothing.testmodule.command.functions.nativecmd.request.NativeMemoryRequest;
import com.justnothing.testmodule.command.functions.nativecmd.request.NativeHeapRequest;
import com.justnothing.testmodule.command.functions.nativecmd.request.NativeStackRequest;
import com.justnothing.testmodule.command.functions.nativecmd.request.NativeMapsRequest;
import com.justnothing.testmodule.command.functions.nativecmd.request.NativeSearchRequest;

@Cmd(
    name = "native",
    description = "查看和调试Native代码，分析JNI函数和库",
    version = CMD_NATIVE_VER,
    defaultResultType = NativeResult.class
)
@CmdRoutes({
    @CmdRoutes.Route(path = "list", request = NativeListRequest.class, handler = NativeQueryCommand.class, description = "列出已加载的native库"),
    @CmdRoutes.Route(path = "info", request = NativeInfoRequest.class, handler = NativeQueryCommand.class, description = "查看native库的详细信息"),
    @CmdRoutes.Route(path = "cli", request = NativeCliRequest.class, handler = NativeManageCommand.class, description = "列出类的native方法"),
    @CmdRoutes.Route(path = "symbols", request = NativeSymbolsRequest.class, handler = NativeQueryCommand.class, description = "查看库的符号表"),
    @CmdRoutes.Route(path = "memory", request = NativeMemoryRequest.class, handler = NativeQueryCommand.class, description = "查看native内存使用情况"),
    @CmdRoutes.Route(path = "heap", request = NativeHeapRequest.class, handler = NativeQueryCommand.class, description = "查看native堆内存"),
    @CmdRoutes.Route(path = "stack", request = NativeStackRequest.class, handler = NativeManageCommand.class, description = "查看线程的native栈"),
    @CmdRoutes.Route(path = "maps", request = NativeMapsRequest.class, handler = NativeQueryCommand.class, description = "查看进程内存映射"),
    @CmdRoutes.Route(path = "search", request = NativeSearchRequest.class, handler = NativeManageCommand.class, description = "搜索native库或函数")
})
public class NativeMain extends MainCommand<NativeResult> {

    public NativeMain() {
        super("native", NativeResult.class);
    }

    @Override
    public String getHelpText() {
        return CommandRouter.getInstance().generateHelpForCommand("native");
    }

    @Override
    public NativeResult runMain(CommandExecutor.CmdExecContext<CommandRequest> context) throws Exception {
        String[] args = context.args();

        if (args.length < 1) {
            context.println(getHelpText(), Colors.WHITE);
            return null;
        }

        context.println("提示: 使用 'native help' 查看路由帮助", Colors.CYAN);
        return createErrorResult("旧路径已迁移到 @CmdRoutes，请使用新路径");
    }
}
