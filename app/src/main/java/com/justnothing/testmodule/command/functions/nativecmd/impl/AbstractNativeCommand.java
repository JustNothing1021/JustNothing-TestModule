package com.justnothing.testmodule.command.functions.nativecmd.impl;

import com.justnothing.testmodule.command.framework.CommandExecutor;
import com.justnothing.testmodule.command.framework.model.AbstractCommand;
import com.justnothing.testmodule.command.framework.model.CommandRequest;
import com.justnothing.testmodule.command.framework.model.CommandResult;
import com.justnothing.testmodule.command.functions.nativecmd.util.NativeManager;
import com.justnothing.testmodule.command.functions.nativecmd.response.NativeResult;
import com.justnothing.testmodule.command.framework.output.Colors;
import com.justnothing.testmodule.utils.logging.Logger;

public abstract class AbstractNativeCommand<Req extends CommandRequest<?>, Res extends CommandResult>
        extends AbstractCommand<Req, Res> {

    public static final Logger logger = Logger.getLoggerForName("NativeCommand");

    public static final NativeManager manager = new NativeManager();

    protected CommandExecutor.CmdExecContext<Req> context;

    protected AbstractNativeCommand(String commandName, Class<Req> requestType, Class<Res> returnType) {
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
        NativeResult r = new NativeResult(java.util.UUID.randomUUID().toString());
        r.setSubCommand(subCmd);
        r.setSuccess(true);
        return (Res) r;
    }

    @SuppressWarnings("unchecked")
    protected Res createErrorResult(String msg) {
        NativeResult r = new NativeResult(java.util.UUID.randomUUID().toString());
        r.setSubCommand("error");
        r.setSuccess(false);
        r.setMessage(msg);
        return (Res) r;
    }
}