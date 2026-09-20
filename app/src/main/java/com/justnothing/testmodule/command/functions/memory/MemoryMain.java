package com.justnothing.testmodule.command.functions.memory;

import static com.justnothing.testmodule.constants.CommandServer.CMD_MEMORY_VER;

import com.justnothing.testmodule.command.framework.model.MainCommand;
import com.justnothing.testmodule.command.framework.annotation.Cmd;
import com.justnothing.testmodule.command.framework.annotation.CmdRoutes;
import com.justnothing.testmodule.command.framework.model.CommandResult;
import com.justnothing.testmodule.command.functions.memory.impl.InfoCommand;
import com.justnothing.testmodule.command.functions.memory.impl.GcCommand;
import com.justnothing.testmodule.command.functions.memory.impl.DumpCommand;
import com.justnothing.testmodule.command.functions.memory.request.DumpRequest;
import com.justnothing.testmodule.command.functions.memory.request.GcRequest;
import com.justnothing.testmodule.command.functions.memory.request.MemoryInfoRequest;

@Cmd(
    name = "memory",
    group = "system",
    description = MemoryTexts.CMD_MEMORY_DESC,
    version = CMD_MEMORY_VER
)
@CmdRoutes({
    @CmdRoutes.Route(
        path = "info",
        request = MemoryInfoRequest.class,
        handler = InfoCommand.class,
        description = MemoryTexts.ROUTE_MEMORY_INFO_DESC
    ),
    @CmdRoutes.Route(
        path = "gc",
        request = GcRequest.class,
        handler = GcCommand.class,
        description = MemoryTexts.ROUTE_MEMORY_GC_DESC
    ),
    @CmdRoutes.Route(
        path = "dump",
        request = DumpRequest.class,
        handler = DumpCommand.class,
        description = MemoryTexts.ROUTE_MEMORY_DUMP_DESC
    )
})
public class MemoryMain extends MainCommand<CommandResult> {

    public MemoryMain() {
        super("Memory", CommandResult.class);
    }
}
