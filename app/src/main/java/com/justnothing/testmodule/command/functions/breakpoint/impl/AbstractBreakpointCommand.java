package com.justnothing.testmodule.command.functions.breakpoint.impl;

import com.justnothing.testmodule.command.framework.CommandExecutor;
import com.justnothing.testmodule.command.framework.model.CommandRequest;
import com.justnothing.testmodule.command.framework.model.CommandResult;
import com.justnothing.testmodule.command.framework.model.AbstractCommand;
import com.justnothing.testmodule.command.functions.breakpoint.util.BreakpointManager;
import com.justnothing.testmodule.command.framework.output.Colors;
import com.justnothing.testmodule.utils.logging.Logger;

public abstract class AbstractBreakpointCommand<REQUEST extends CommandRequest<?>, RESULT extends CommandResult>
        extends AbstractCommand<REQUEST, RESULT> {

    protected static final Logger logger = Logger.getLoggerForName("BreakpointCmd");

    protected final BreakpointManager manager = BreakpointManager.getInstance();
    protected CommandExecutor.CmdExecContext<?> context;

    protected AbstractBreakpointCommand(String commandName, Class<REQUEST> requestType, Class<RESULT> resultType) {
        super(commandName, requestType, resultType);
    }

    @Override
    public RESULT execute(CommandExecutor.CmdExecContext<? extends CommandRequest<?>> context) {
        // 保留额外行为：out(...) 依赖此字段输出；其余（请求类型护栏、异常兜底、
        // 失败结果的 requestId/ErrorInfo）交由 AbstractCommand.executeWithResult 统一处理。
        this.context = context;
        return super.execute(context);
    }

    protected abstract RESULT executeInternal(CommandExecutor.CmdExecContext<REQUEST> request) throws Exception;

    protected void out(String text) { context.println(text, Colors.WHITE); }
    protected void out(String text, byte color) { context.println(text, color); }

    // createSuccessResult / createErrorResult 由 AbstractCommand 统一提供。
    // 原先这里返回的是具体类型 BreakpointResult（而不是类型变量 RESULT），
    // 在父类补上同名方法后会变成"继承冲突"，因此删掉改为继承。
}
