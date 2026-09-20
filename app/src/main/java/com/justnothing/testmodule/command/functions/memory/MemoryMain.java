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
    description = "内存调试和管理工具, 包括内存信息查询、GC、堆转储等功能",
    version = CMD_MEMORY_VER
)
@CmdRoutes({
    @CmdRoutes.Route(
        path = "info",
        request = MemoryInfoRequest.class,
        handler = InfoCommand.class,
        description = "显示详细的内存使用情况"
    ),
    @CmdRoutes.Route(
        path = "gc",
        request = GcRequest.class,
        handler = GcCommand.class,
        description = "手动触发垃圾回收"
    ),
    @CmdRoutes.Route(
        path = "dump",
        request = DumpRequest.class,
        handler = DumpCommand.class,
        description = "导出堆信息和系统状态"
    )
})
public class MemoryMain extends MainCommand<CommandResult> {

    public MemoryMain() {
        super("Memory", CommandResult.class);
    }
}
