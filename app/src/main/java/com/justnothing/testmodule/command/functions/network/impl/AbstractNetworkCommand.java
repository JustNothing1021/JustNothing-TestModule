package com.justnothing.testmodule.command.functions.network.impl;

import com.justnothing.testmodule.command.framework.CommandExecutor;
import com.justnothing.testmodule.command.framework.model.AbstractCommand;
import com.justnothing.testmodule.command.framework.model.CommandRequest;
import com.justnothing.testmodule.command.framework.model.CommandResult;
import com.justnothing.testmodule.command.functions.network.util.NetworkManager;
import com.justnothing.testmodule.command.functions.network.response.NetworkResult;
import com.justnothing.testmodule.command.framework.output.Colors;
import com.justnothing.testmodule.utils.logging.Logger;

public abstract class AbstractNetworkCommand<Req extends CommandRequest<?>, Res extends CommandResult>
        extends AbstractCommand<Req, Res> {

    public static final Logger logger = Logger.getLoggerForName("NetworkCommand");

    public static final NetworkManager manager = NetworkManager.getInstance();

    protected CommandExecutor.CmdExecContext<Req> context;

    protected AbstractNetworkCommand(String commandName, Class<Req> requestType, Class<Res> returnType) {
        super(commandName, requestType, returnType);
    }

    protected void out(String text) { context.print(text, Colors.WHITE); }
    protected void out(String text, byte color) { context.print(text, color); }
    protected void outln(String text) { context.println(text, Colors.WHITE); }
    protected void outln(String text, byte color) { context.println(text, color); }

    /**
     * 基类负责把"执行上下文"适配成"请求对象"，子类只关心自己的请求类型。
     * 异常兜底、请求类型护栏由 {@link AbstractCommand} 统一提供。
     */
    @Override
    protected final Res executeInternal(CommandExecutor.CmdExecContext<Req> context) throws Exception {
        this.context = context;
        return executeRequest(context.getRequest());
    }

    protected abstract Res executeRequest(Req request) throws Exception;

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