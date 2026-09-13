package com.justnothing.testmodule.command.functions.didyouknow;

import static com.justnothing.testmodule.constants.CommandServer.CMD_DYK_VER;

import com.justnothing.testmodule.command.framework.model.MainCommand;
import com.justnothing.testmodule.command.framework.annotation.Cmd;
import com.justnothing.testmodule.command.framework.annotation.CmdRoutes;
import com.justnothing.testmodule.command.functions.didyouknow.impl.DidYouKnowCommand;
import com.justnothing.testmodule.command.functions.didyouknow.request.DidYouKnowRequest;
import com.justnothing.testmodule.command.functions.didyouknow.response.DidYouKnowResult;

@Cmd(
    name = "did-you-know",
    group = "fun",
    description = "你知道吗？显示有趣的冷知识和彩蛋",
    version = CMD_DYK_VER
)
@CmdRoutes({
    @CmdRoutes.Route(
        path = "",
        request = DidYouKnowRequest.class,
        handler = DidYouKnowCommand.class,
        description = "显示一条随机的'你知道吗'提示"
    )
})
public class DidYouKnowMain extends MainCommand<DidYouKnowResult> {

    public DidYouKnowMain() {
        super("did-you-know", DidYouKnowResult.class);
    }
}
