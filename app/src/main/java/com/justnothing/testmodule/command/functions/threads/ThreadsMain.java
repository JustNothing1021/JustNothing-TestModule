package com.justnothing.testmodule.command.functions.threads;

import static com.justnothing.testmodule.constants.CommandServer.CMD_THREADS_VER;

import com.justnothing.testmodule.command.framework.model.MainCommand;
import com.justnothing.testmodule.command.framework.annotation.Cmd;
import com.justnothing.testmodule.command.framework.annotation.CmdRoutes;
import com.justnothing.testmodule.command.framework.model.CommandRouter;
import com.justnothing.testmodule.command.functions.threads.impl.DeadlockCommand;
import com.justnothing.testmodule.command.functions.threads.impl.ListCommand;
import com.justnothing.testmodule.command.functions.threads.impl.ProfileExportCommand;
import com.justnothing.testmodule.command.functions.threads.impl.ProfileShowCommand;
import com.justnothing.testmodule.command.functions.threads.impl.ProfileStartCommand;
import com.justnothing.testmodule.command.functions.threads.impl.ProfileStopCommand;
import com.justnothing.testmodule.command.functions.threads.request.ThreadDeadlockRequest;
import com.justnothing.testmodule.command.functions.threads.request.ThreadListRequest;
import com.justnothing.testmodule.command.functions.threads.request.ThreadProfileExportRequest;
import com.justnothing.testmodule.command.functions.threads.request.ThreadProfileShowRequest;
import com.justnothing.testmodule.command.functions.threads.request.ThreadProfileStartRequest;
import com.justnothing.testmodule.command.functions.threads.request.ThreadProfileStopRequest;
import com.justnothing.testmodule.command.functions.threads.response.ThreadCommandResult;

@Cmd(
    name = "threads",
    group = "system",
    description = "线程管理和分析工具",
    version = CMD_THREADS_VER
)
@CmdRoutes({
    @CmdRoutes.Route(
        path = "list",
        request = ThreadListRequest.class,
        handler = ListCommand.class,
        description = "列出所有线程及其状态"
    ),
    @CmdRoutes.Route(
        path = "deadlock",
        request = ThreadDeadlockRequest.class,
        handler = DeadlockCommand.class,
        description = "检测Java应用程序中的死锁"
    ),
    @CmdRoutes.Route(
        path = "profile/start",
        request = ThreadProfileStartRequest.class,
        handler = ProfileStartCommand.class,
        description = "开始性能分析"
    ),
    @CmdRoutes.Route(
        path = "profile/stop",
        request = ThreadProfileStopRequest.class,
        handler = ProfileStopCommand.class,
        description = "停止当前分析"
    ),
    @CmdRoutes.Route(
        path = "profile/show",
        request = ThreadProfileShowRequest.class,
        handler = ProfileShowCommand.class,
        description = "显示分析结果"
    ),
    @CmdRoutes.Route(
        path = "profile/export",
        request = ThreadProfileExportRequest.class,
        handler = ProfileExportCommand.class,
        description = "导出分析结果到文件"
    )
})
public class ThreadsMain extends MainCommand<ThreadCommandResult> {

    public ThreadsMain() {
        super("Threads", ThreadCommandResult.class);
    }

    @Override
    public String getHelpText() {
        return CommandRouter.getInstance().generateHelpForCommand("threads");
    }
}
