package com.justnothing.testmodule.command.functions.nativecmd;

import static com.justnothing.testmodule.constants.CommandServer.CMD_NATIVE_VER;

import com.justnothing.testmodule.command.framework.model.MainCommand;
import com.justnothing.testmodule.command.framework.annotation.Cmd;
import com.justnothing.testmodule.command.framework.annotation.CmdRoutes;
import com.justnothing.testmodule.command.framework.model.CommandRouter;
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
import com.justnothing.testmodule.command.functions.nativecmd.response.NativeResult;

@Cmd(
    name = "native",
    description = NativeTexts.CMD_NATIVE_DESC,
    version = CMD_NATIVE_VER
)
@CmdRoutes({
    @CmdRoutes.Route(path = "list", request = NativeListRequest.class, handler = NativeQueryCommand.class, description = NativeTexts.ROUTE_NATIVE_LIST_DESC),
    @CmdRoutes.Route(path = "info", request = NativeInfoRequest.class, handler = NativeQueryCommand.class, description = NativeTexts.ROUTE_NATIVE_INFO_DESC),
    @CmdRoutes.Route(path = "cli", request = NativeCliRequest.class, handler = NativeManageCommand.class, description = NativeTexts.ROUTE_NATIVE_CLI_DESC),
    @CmdRoutes.Route(path = "symbols", request = NativeSymbolsRequest.class, handler = NativeQueryCommand.class, description = NativeTexts.ROUTE_NATIVE_SYMBOLS_DESC),
    @CmdRoutes.Route(path = "memory", request = NativeMemoryRequest.class, handler = NativeQueryCommand.class, description = NativeTexts.ROUTE_NATIVE_MEMORY_DESC),
    @CmdRoutes.Route(path = "heap", request = NativeHeapRequest.class, handler = NativeQueryCommand.class, description = NativeTexts.ROUTE_NATIVE_HEAP_DESC),
    @CmdRoutes.Route(path = "stack", request = NativeStackRequest.class, handler = NativeManageCommand.class, description = NativeTexts.ROUTE_NATIVE_STACK_DESC),
    @CmdRoutes.Route(path = "maps", request = NativeMapsRequest.class, handler = NativeQueryCommand.class, description = NativeTexts.ROUTE_NATIVE_MAPS_DESC),
    @CmdRoutes.Route(path = "search", request = NativeSearchRequest.class, handler = NativeManageCommand.class, description = NativeTexts.ROUTE_NATIVE_SEARCH_DESC)
})
public class NativeMain extends MainCommand<NativeResult> {

    public NativeMain() {
        super("native", NativeResult.class);
    }

    @Override
    public String getHelpText() {
        return CommandRouter.getInstance().generateHelpForCommand("native");
    }
}
