package com.justnothing.testmodule.command.base;

import com.justnothing.testmodule.command.CommandExecutor;
import com.justnothing.testmodule.command.utils.CommandExceptionHandler;

public abstract class AbstractCommand<Req extends CommandRequest, Res extends CommandResult> implements Command<Req, Res> {
    protected final String commandName;
    protected final Class<Req> requestType;
    protected final Class<Res> returnType;


    protected AbstractCommand(String commandName, Class<Req> requestType, Class<Res> returnType) {
        this.commandName = commandName;
        this.requestType = requestType;
        this.returnType = returnType;
    }

    protected Class<Req> getAcceptableRequestType() {
        return requestType;
    }

    protected boolean acceptable(CommandRequest req) {
        return getAcceptableRequestType().isAssignableFrom(req.getClass());
    }

    @SuppressWarnings("unchecked")
    public Res execute(CommandExecutor.CmdExecContext<? extends CommandRequest> context) {
        if (!acceptable(context.getRequest())) {
            throw new IllegalArgumentException("命令请求类型错误; 期待"
                    + getAcceptableRequestType().getSimpleName()
                    + ", 却接收到了" + context.getRequest().getClass().getSimpleName());
        }
        try {

            return executeInternal((CommandExecutor.CmdExecContext<Req>) context);
        } catch (Exception e) {
            CommandExceptionHandler.handleException(
                commandName,  e,  context,
                "执行" + commandName + "命令失败"
            );
            try {
                Res result = returnType.newInstance();
                result.setSuccess(false);
                result.setRequestId(context.getRequest().getRequestId());
                result.setError(
                    new CommandResult.ErrorInfo(
                        "UNEXPECTED_ERROR", "执行" + commandName + "命令失败", e
                    )
                );

                return result;
            } catch (InstantiationException | IllegalAccessException e1) {
                throw new RuntimeException("无法创建结果类型实例", e1);
            }
        }
    }

    protected abstract Res executeInternal(CommandExecutor.CmdExecContext<Req> context) throws Exception;

    @Override
    public String getHelpText() {
        return "用法: " + commandName + " <args>\n" +
               "输入 " + commandName + " --help 查看详细帮助";
    }
}
