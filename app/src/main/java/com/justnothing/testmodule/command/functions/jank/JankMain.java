package com.justnothing.testmodule.command.functions.jank;

import static com.justnothing.testmodule.constants.CommandServer.CMD_JANK_VER;

import com.justnothing.testmodule.command.framework.annotation.Cmd;
import com.justnothing.testmodule.command.framework.annotation.CmdRoutes;
import com.justnothing.testmodule.command.framework.model.CommandRouter;
import com.justnothing.testmodule.command.framework.model.MainCommand;
import com.justnothing.testmodule.command.functions.jank.impl.JankSampleCommand;
import com.justnothing.testmodule.command.functions.jank.impl.JankWatchCommand;
import com.justnothing.testmodule.command.functions.jank.request.JankSampleRequest;
import com.justnothing.testmodule.command.functions.jank.request.JankWatchRequest;
import com.justnothing.testmodule.command.functions.jank.response.JankResult;

/**
 * jank —— 系统卡顿检测。
 *
 * <p>和 {@code threads} / {@code memory} 的区别：那两个是"看一眼某个瞬间"，这个是在一段时间里
 * <b>连续采样</b>，专门用来抓"偶尔卡一下"这种静态快照抓不到的现象。
 * 它不 hook 任何东西，只读 {@code /proc}，所以量到的就是系统本来的样子。</p>
 */
@Cmd(
    name = "jank",
    description = JankTexts.CMD_JANK_DESC,
    version = CMD_JANK_VER
)
@CmdRoutes({
    @CmdRoutes.Route(
        path = "sample",
        request = JankSampleRequest.class,
        handler = JankSampleCommand.class,
        description = JankTexts.ROUTE_JANK_SAMPLE_DESC
    ),
    @CmdRoutes.Route(
        path = "watch",
        request = JankWatchRequest.class,
        handler = JankWatchCommand.class,
        description = JankTexts.ROUTE_JANK_WATCH_DESC
    )
})
public class JankMain extends MainCommand<JankResult> {

    public JankMain() {
        super("jank", JankResult.class);
    }

    @Override
    public String getHelpText() {
        return CommandRouter.getInstance().generateHelpForCommand("jank");
    }
}
