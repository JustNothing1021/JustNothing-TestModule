package com.justnothing.testmodule.command.base;

import com.justnothing.testmodule.command.CommandExecutor;
import com.justnothing.testmodule.command.base.command.Command;
import com.justnothing.testmodule.command.base.command.SubCommandInfo;
import com.justnothing.testmodule.command.base.protocol.CommandRequest;
import com.justnothing.testmodule.command.base.protocol.CommandResult;
import com.justnothing.testmodule.command.utils.CommandExceptionHandler;

public abstract class AbstractCommand<Req extends CommandRequest, Res extends CommandResult> implements Command<Res> {
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
        if (req == null) {
            return true;
        }
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
        } catch (IllegalCommandLineArgumentException e) {
            throw e;
        } catch (Exception e) {
            CommandExceptionHandler.handleException(
                commandName,  e,  context,
                "执行" + commandName + "命令失败"
            );
            try {
                Res result = returnType.newInstance();
                result.setSuccess(false);
                if (context.getRequest() != null) {
                    result.setRequestId(context.getRequest().getRequestId());
                }
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
        SubCommandInfo info = this.getClass().getAnnotation(SubCommandInfo.class);
        if (info != null) {
            return generateHelpFromAnnotation(info);
        }
        return "用法: " + commandName + " <args>\n" +
               "输入 " + commandName + " --help 查看详细帮助";
    }


    public static String generateHelpFromAnnotation(SubCommandInfo info) {
        StringBuilder help = new StringBuilder();
        
        if (!info.usage().isEmpty()) {
            help.append("用法: ").append(info.usage()).append("\n");
        }
        
        if (!info.description().isEmpty()) {
            help.append("\n\n").append(info.description());
        }

        
        if (!info.optionsDesc().isEmpty()) {
            if (help.length() > 0) {
                help.append("\n\n");
            }
            help.append(info.optionsDesc());
        }


        if (info.examples().length > 0) {
            help.append("\n\n示例:");
            for (String ex : info.examples()) {
                help.append("\n  ").append(ex);
            }
        }
        
        if (info.seeAlso().length > 0) {
            help.append("\n\n相关命令:");
            for (String see : info.seeAlso()) {
                help.append("\n  ").append(see);
            }
        }
        
        return help.toString();
    }
}
