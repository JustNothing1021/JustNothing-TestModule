package com.justnothing.testmodule.command.functions.jank;

import static com.justnothing.testmodule.constants.CommandServer.CMD_JANK_VER;

import com.justnothing.testmodule.command.framework.annotation.Cmd;
import com.justnothing.testmodule.command.framework.annotation.CmdRoutes;
import com.justnothing.testmodule.command.framework.model.CommandRouter;
import com.justnothing.testmodule.command.framework.model.MainCommand;
import com.justnothing.testmodule.command.functions.jank.impl.JankSampleCommand;
import com.justnothing.testmodule.command.functions.jank.request.JankSampleRequest;
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
    description = "检测系统卡顿（连续采样 CPU/内存/队列/进程变动）",
    version = CMD_JANK_VER
)
@CmdRoutes({
    @CmdRoutes.Route(
        path = "sample",
        request = JankSampleRequest.class,
        handler = JankSampleCommand.class,
        description = "采样一段时间，实时显示卡顿指标"
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
