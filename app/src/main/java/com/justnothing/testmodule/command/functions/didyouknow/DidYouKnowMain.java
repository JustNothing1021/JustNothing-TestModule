package com.justnothing.testmodule.command.functions.didyouknow;

import static com.justnothing.testmodule.constants.CommandServer.CMD_DYK_VER;

import com.justnothing.testmodule.command.CommandExecutor;
import com.justnothing.testmodule.command.base.MainCommand;
import com.justnothing.testmodule.command.base.protocol.CommandRequest;
import com.justnothing.testmodule.command.base.protocol.CommandResult;
import com.justnothing.testmodule.command.base.command.Cmd;
import com.justnothing.testmodule.command.base.command.CmdRoutes;
import com.justnothing.testmodule.command.base.command.CommandRouter;
import com.justnothing.testmodule.command.functions.didyouknow.impl.DidYouKnowCommand;
import com.justnothing.testmodule.command.functions.didyouknow.request.DidYouKnowRequest;
import com.justnothing.testmodule.command.functions.didyouknow.response.DidYouKnowResult;
import com.justnothing.testmodule.command.output.Colors;

@Cmd(
    name = "did-you-know",
    group = "fun",
    description = "你知道吗？显示有趣的冷知识和彩蛋",
    version = CMD_DYK_VER,
    defaultResultType = DidYouKnowResult.class
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


    @Override
    public DidYouKnowResult runMain(CommandExecutor.CmdExecContext<CommandRequest> context) throws Exception {
        if (context.getRequest() != null) {
            Object request = context.getRequest();
            if (request instanceof DidYouKnowRequest) {
                return new DidYouKnowCommand().execute(context);
            }
        }
        context.println(getHelpText(), Colors.WHITE);
        DidYouKnowResult result = new DidYouKnowResult();
        result.setSuccess(false);
        result.setMessage("请使用 did-you-know [options]");
        return result;
    }
}
