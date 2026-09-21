package com.justnothing.testmodule.command.functions.breakpoint.impl;

import com.justnothing.testmodule.command.framework.CommandExecutor;
import com.justnothing.testmodule.command.framework.model.CommandRequest;
import com.justnothing.testmodule.command.framework.annotation.SubCommandInfo;
import com.justnothing.testmodule.command.framework.i18n.CliMessages;
import com.justnothing.testmodule.command.framework.i18n.Text;
import com.justnothing.testmodule.command.functions.breakpoint.BreakpointTexts;
import com.justnothing.testmodule.command.functions.breakpoint.request.BreakpointAddRequest;
import com.justnothing.testmodule.command.functions.breakpoint.request.BreakpointEnableRequest;
import com.justnothing.testmodule.command.functions.breakpoint.request.BreakpointDisableRequest;
import com.justnothing.testmodule.command.functions.breakpoint.request.BreakpointRemoveRequest;
import com.justnothing.testmodule.command.functions.breakpoint.request.BreakpointClearRequest;
import com.justnothing.testmodule.command.functions.breakpoint.response.BreakpointResult;
import com.justnothing.testmodule.command.framework.output.Colors;

@SubCommandInfo(
    description = BreakpointTexts.SUB_BREAKPOINT_MANAGE_DESC,
    usage = "breakpoint <add|enable|disable|remove|clear> [args...]",
    examples = {
        "breakpoint add com.example.MyClass myMethod",
        "breakpoint enable 1",
        "breakpoint disable 1",
        "breakpoint remove 1",
        "breakpoint clear"
    }
)
public class BreakpointManageCommand extends AbstractBreakpointCommand<CommandRequest<?>, BreakpointResult> {

    @SuppressWarnings("unchecked")
    public BreakpointManageCommand() {
        super("breakpoint manage", (Class) CommandRequest.class, BreakpointResult.class);
    }

    @Override
    @SuppressWarnings("unchecked")
    protected BreakpointResult executeInternal(CommandExecutor.CmdExecContext<CommandRequest<?>> context) throws Exception {
        CommandRequest request = context.getRequest();

        if (request instanceof BreakpointAddRequest) {
            return handleAdd((BreakpointAddRequest) request);
        } else if (request instanceof BreakpointEnableRequest) {
            return handleEnable((BreakpointEnableRequest) request);
        } else if (request instanceof BreakpointDisableRequest) {
            return handleDisable((BreakpointDisableRequest) request);
        } else if (request instanceof BreakpointRemoveRequest) {
            return handleRemove((BreakpointRemoveRequest) request);
        } else if (request instanceof BreakpointClearRequest) {
            return handleClear((BreakpointClearRequest) request);
        }

        return createErrorResult(Text.zhEn("未知的断点管理请求类型", "Unknown breakpoint management request type").text());
    }

    private BreakpointResult handleAdd(BreakpointAddRequest request) {
        String className = request.getClassName();
        String methodName = request.getMethodName();
        String signature = request.getSignature();
        ClassLoader classLoader = context.classLoader();

        try {
            int id = manager.addBreakpoint(className, methodName, signature, classLoader);

            out(Text.zhEn("断点已添加", "Breakpoint added").text(), Colors.GREEN);
            out("ID: ", Colors.CYAN);
            out(String.valueOf(id), Colors.YELLOW);
            out(CliMessages.LABEL_CLASS.text(), Colors.CYAN);
            out(className, Colors.GREEN);
            out(CliMessages.LABEL_METHOD.text(), Colors.CYAN);
            out(methodName, Colors.GREEN);
            out(Text.zhEn("签名: ", "Signature: ").text(), Colors.CYAN);
            out(signature != null ? signature : BreakpointTexts.VALUE_ALL_OVERLOADS.text(), Colors.GRAY);
            out(Text.zhEn("状态: ", "Status: ").text(), Colors.CYAN);
            out(BreakpointTexts.VALUE_ENABLED.text(), Colors.GREEN);
            out("", Colors.WHITE);
            out(Text.zhEn("断点已设置并生效！", "Breakpoint is set and active!").text(), Colors.GREEN);

            return createSuccessResult(Text.zhEn("断点已添加，ID=%d", "Breakpoint added, ID=%d").format(id));
        } catch (Exception e) {
            out(CliMessages.ERROR_PREFIX.text() + BreakpointTexts.ERR_ADD_FAILED.text()
                    + " - " + e.getMessage(), Colors.RED);
            return createErrorResult(BreakpointTexts.ERR_ADD_FAILED.text() + ": " + e.getMessage());
        }
    }

    private BreakpointResult handleEnable(BreakpointEnableRequest request) {
        int id = Integer.parseInt(request.getId());

        if (manager.enableTask(id)) {
            out(Text.zhEn("断点已启用", "Breakpoint enabled").text(), Colors.GREEN);
            out("ID: ", Colors.CYAN);
            out(String.valueOf(id), Colors.YELLOW);
            return createSuccessResult(Text.zhEn("断点已启用，ID=%d", "Breakpoint enabled, ID=%d").format(id));
        } else {
            out(CliMessages.ERROR_PREFIX.text() + BreakpointTexts.ERR_NOT_FOUND.text(), Colors.RED);
            return createErrorResult(BreakpointTexts.ERR_NOT_FOUND.text());
        }
    }

    private BreakpointResult handleDisable(BreakpointDisableRequest request) {
        int id = Integer.parseInt(request.getId());

        if (manager.disableTask(id)) {
            out(Text.zhEn("断点已禁用", "Breakpoint disabled").text(), Colors.YELLOW);
            out("ID: ", Colors.CYAN);
            out(String.valueOf(id), Colors.YELLOW);
            return createSuccessResult(Text.zhEn("断点已禁用，ID=%d", "Breakpoint disabled, ID=%d").format(id));
        } else {
            out(CliMessages.ERROR_PREFIX.text() + BreakpointTexts.ERR_NOT_FOUND.text(), Colors.RED);
            return createErrorResult(BreakpointTexts.ERR_NOT_FOUND.text());
        }
    }

    private BreakpointResult handleRemove(BreakpointRemoveRequest request) {
        int id = Integer.parseInt(request.getId());

        if (manager.removeTask(id)) {
            out(Text.zhEn("断点已移除", "Breakpoint removed").text(), Colors.GREEN);
            out("ID: ", Colors.CYAN);
            out(String.valueOf(id), Colors.YELLOW);
            return createSuccessResult(Text.zhEn("断点已移除，ID=%d", "Breakpoint removed, ID=%d").format(id));
        } else {
            out(CliMessages.ERROR_PREFIX.text() + BreakpointTexts.ERR_NOT_FOUND.text(), Colors.RED);
            return createErrorResult(BreakpointTexts.ERR_NOT_FOUND.text());
        }
    }

    private BreakpointResult handleClear(BreakpointClearRequest request) {
        int count = manager.getTaskCount();
        manager.clearAll();
        out(Text.zhEn("已清除所有断点", "All breakpoints cleared").text(), Colors.GREEN);
        out(Text.zhEn("清除数量: ", "Cleared: ").text(), Colors.CYAN);
        out(String.valueOf(count), Colors.YELLOW);
        return createSuccessResult(Text.zhEn("已清除 %d 个断点", "Cleared %d breakpoint(s)").format(count));
    }
}
