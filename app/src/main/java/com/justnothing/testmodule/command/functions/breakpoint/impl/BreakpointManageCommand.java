package com.justnothing.testmodule.command.functions.breakpoint.impl;

import com.justnothing.testmodule.command.CommandExecutor;
import com.justnothing.testmodule.command.base.protocol.CommandRequest;
import com.justnothing.testmodule.command.base.command.SubCommandInfo;
import com.justnothing.testmodule.command.functions.breakpoint.request.BreakpointAddRequest;
import com.justnothing.testmodule.command.functions.breakpoint.request.BreakpointEnableRequest;
import com.justnothing.testmodule.command.functions.breakpoint.request.BreakpointDisableRequest;
import com.justnothing.testmodule.command.functions.breakpoint.request.BreakpointRemoveRequest;
import com.justnothing.testmodule.command.functions.breakpoint.request.BreakpointClearRequest;
import com.justnothing.testmodule.command.functions.breakpoint.response.BreakpointResult;
import com.justnothing.testmodule.command.output.Colors;

@SubCommandInfo(
    description = "断点管理命令（添加、启用、禁用、移除、清除）",
    usage = "breakpoint <add|enable|disable|remove|clear> [args...]",
    examples = {
        "breakpoint add com.example.MyClass myMethod",
        "breakpoint enable 1",
        "breakpoint disable 1",
        "breakpoint remove 1",
        "breakpoint clear"
    }
)
public class BreakpointManageCommand extends AbstractBreakpointCommand<CommandRequest, BreakpointResult> {

    public BreakpointManageCommand() {
        super("breakpoint manage", CommandRequest.class, BreakpointResult.class);
    }

    @Override
    @SuppressWarnings("unchecked")
    protected BreakpointResult executeInternal(CommandExecutor.CmdExecContext<CommandRequest> context) throws Exception {
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

        return createErrorResult("未知的断点管理请求类型");
    }

    private BreakpointResult handleAdd(BreakpointAddRequest request) {
        String className = request.getClassName();
        String methodName = request.getMethodName();
        String signature = request.getSignature();
        ClassLoader classLoader = context.classLoader();

        try {
            int id = manager.addBreakpoint(className, methodName, signature, classLoader);

            out("断点已添加", Colors.GREEN);
            out("ID: ", Colors.CYAN);
            out(String.valueOf(id), Colors.YELLOW);
            out("类: ", Colors.CYAN);
            out(className, Colors.GREEN);
            out("方法: ", Colors.CYAN);
            out(methodName, Colors.GREEN);
            out("签名: ", Colors.CYAN);
            out(signature != null ? signature : "所有重载", Colors.GRAY);
            out("状态: ", Colors.CYAN);
            out("启用", Colors.GREEN);
            out("", Colors.WHITE);
            out("断点已设置并生效！", Colors.GREEN);

            return createSuccessResult("断点已添加，ID=" + id);
        } catch (Exception e) {
            out("错误: 添加断点失败 - " + e.getMessage(), Colors.RED);
            return createErrorResult("添加断点失败: " + e.getMessage());
        }
    }

    private BreakpointResult handleEnable(BreakpointEnableRequest request) {
        int id = Integer.parseInt(request.getId());

        if (manager.enableTask(id)) {
            out("断点已启用", Colors.GREEN);
            out("ID: ", Colors.CYAN);
            out(String.valueOf(id), Colors.YELLOW);
            return createSuccessResult("断点已启用，ID=" + id);
        } else {
            out("错误: 断点不存在", Colors.RED);
            return createErrorResult("断点不存在");
        }
    }

    private BreakpointResult handleDisable(BreakpointDisableRequest request) {
        int id = Integer.parseInt(request.getId());

        if (manager.disableTask(id)) {
            out("断点已禁用", Colors.YELLOW);
            out("ID: ", Colors.CYAN);
            out(String.valueOf(id), Colors.YELLOW);
            return createSuccessResult("断点已禁用，ID=" + id);
        } else {
            out("错误: 断点不存在", Colors.RED);
            return createErrorResult("断点不存在");
        }
    }

    private BreakpointResult handleRemove(BreakpointRemoveRequest request) {
        int id = Integer.parseInt(request.getId());

        if (manager.removeTask(id)) {
            out("断点已移除", Colors.GREEN);
            out("ID: ", Colors.CYAN);
            out(String.valueOf(id), Colors.YELLOW);
            return createSuccessResult("断点已移除，ID=" + id);
        } else {
            out("错误: 断点不存在", Colors.RED);
            return createErrorResult("断点不存在");
        }
    }

    private BreakpointResult handleClear(BreakpointClearRequest request) {
        int count = manager.getTaskCount();
        manager.clearAll();
        out("已清除所有断点", Colors.GREEN);
        out("清除数量: ", Colors.CYAN);
        out(String.valueOf(count), Colors.YELLOW);
        return createSuccessResult("已清除 " + count + " 个断点");
    }
}
