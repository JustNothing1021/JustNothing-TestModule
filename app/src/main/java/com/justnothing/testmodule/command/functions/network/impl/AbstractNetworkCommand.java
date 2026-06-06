package com.justnothing.testmodule.command.functions.network.impl;

import com.justnothing.testmodule.command.CommandExecutor;
import com.justnothing.testmodule.command.base.protocol.CommandRequest;
import com.justnothing.testmodule.command.base.protocol.CommandResult;
import com.justnothing.testmodule.command.functions.network.NetworkMain;
import com.justnothing.testmodule.command.functions.network.NetworkManager;
import com.justnothing.testmodule.command.functions.network.NetworkResult;
import com.justnothing.testmodule.command.output.Colors;
import com.justnothing.testmodule.utils.logging.Logger;

public abstract class AbstractNetworkCommand<Req extends CommandRequest, Res extends CommandResult> {

    public static final Logger logger = Logger.getLoggerForName("NetworkCommand");
    public static final NetworkManager manager = NetworkManager.getInstance();
    protected CommandExecutor.CmdExecContext<?> context;

    protected void out(String text) { context.print(text, Colors.WHITE); }
    protected void out(String text, byte color) { context.print(text, color); }
    protected void outln(String text) { context.println(text, Colors.WHITE); }
    protected void outln(String text, byte color) { context.println(text, color); }

    @SuppressWarnings("unchecked")
    public Res execute(CommandExecutor.CmdExecContext<?> ctx) {
        this.context = ctx;
        try {
            Req request = (Req) ctx.getCommandRequest();
            return executeInternal(request);
        } catch (Exception e) {
            logger.error("执行失败: " + e.getMessage(), e);
            ctx.println("错误: " + (e.getMessage() != null ? e.getMessage() : "未知错误"), Colors.RED);
            return createErrorResult(e.getMessage());
        }
    }

    protected abstract Res executeInternal(Req request) throws Exception;

    @SuppressWarnings("unchecked")
    protected Res okResult(String subCmd) {
        NetworkResult r = new NetworkResult();
        r.setSubCommand(subCmd);
        r.setSuccess(true);
        return (Res) r;
    }

    @SuppressWarnings("unchecked")
    protected Res createErrorResult(String msg) {
        NetworkResult r = new NetworkResult();
        r.setSubCommand("error");
        r.setSuccess(false);
        r.setMessage(msg);
        return (Res) r;
    }
}
