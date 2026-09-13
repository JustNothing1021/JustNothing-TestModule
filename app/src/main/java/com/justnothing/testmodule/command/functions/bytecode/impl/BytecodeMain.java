package com.justnothing.testmodule.command.functions.bytecode.impl;

import static com.justnothing.testmodule.constants.CommandServer.CMD_BYTECODE_VER;

import com.justnothing.testmodule.command.framework.model.MainCommand;
import com.justnothing.testmodule.command.framework.annotation.Cmd;
import com.justnothing.testmodule.command.framework.annotation.CmdRoutes;
import com.justnothing.testmodule.command.framework.model.CommandRouter;
import com.justnothing.testmodule.command.functions.bytecode.request.BytecodeInfoRequest;
import com.justnothing.testmodule.command.functions.bytecode.request.BytecodeMethodRequest;
import com.justnothing.testmodule.command.functions.bytecode.request.BytecodeDumpRequest;
import com.justnothing.testmodule.command.functions.bytecode.request.BytecodeAnalyzeRequest;
import com.justnothing.testmodule.command.functions.bytecode.request.BytecodeDisasmRequest;
import com.justnothing.testmodule.command.functions.bytecode.request.BytecodeConstantsRequest;
import com.justnothing.testmodule.command.functions.bytecode.request.BytecodeVerifyRequest;
import com.justnothing.testmodule.command.functions.bytecode.request.BytecodeDecompileRequest;
import com.justnothing.testmodule.command.functions.bytecode.request.BytecodeBatchExportRequest;
import com.justnothing.testmodule.command.functions.bytecode.request.BytecodeListClassesRequest;
import com.justnothing.testmodule.command.functions.bytecode.response.BytecodeResult;

@Cmd(
    name = "bytecode",
    description = "查看和分析Java字节码",
    version = CMD_BYTECODE_VER
)
@CmdRoutes({
    @CmdRoutes.Route(
        path = "info",
        request = BytecodeInfoRequest.class,
        handler = BytecodeQueryCommand.class,
        description = "查看类的字节码信息"
    ),
    @CmdRoutes.Route(
        path = "method",
        request = BytecodeMethodRequest.class,
        handler = BytecodeQueryCommand.class,
        description = "查看指定方法的字节码"
    ),
    @CmdRoutes.Route(
        path = "dump",
        request = BytecodeDumpRequest.class,
        handler = BytecodeManageCommand.class,
        description = "导出类的字节码到文件"
    ),
    @CmdRoutes.Route(
        path = "analyze",
        request = BytecodeAnalyzeRequest.class,
        handler = BytecodeQueryCommand.class,
        description = "分析类的字节码结构"
    ),
    @CmdRoutes.Route(
        path = "disasm",
        request = BytecodeDisasmRequest.class,
        handler = BytecodeQueryCommand.class,
        description = "反汇编字节码"
    ),
    @CmdRoutes.Route(
        path = "constants",
        request = BytecodeConstantsRequest.class,
        handler = BytecodeQueryCommand.class,
        description = "查看常量池"
    ),
    @CmdRoutes.Route(
        path = "verify",
        request = BytecodeVerifyRequest.class,
        handler = BytecodeQueryCommand.class,
        description = "验证字节码有效性"
    ),
    @CmdRoutes.Route(
        path = "decompile",
        request = BytecodeDecompileRequest.class,
        handler = BytecodeQueryCommand.class,
        description = "反编译为Java代码"
    ),
    @CmdRoutes.Route(
        path = "batch_export",
        request = BytecodeBatchExportRequest.class,
        handler = BytecodeManageCommand.class,
        description = "批量导出类型信息"
    ),
    @CmdRoutes.Route(
        path = "list_classes",
        request = BytecodeListClassesRequest.class,
        handler = BytecodeManageCommand.class,
        description = "列出所有类"
    )
})
public class BytecodeMain extends MainCommand<BytecodeResult> {

    public BytecodeMain() {
        super("bytecode", BytecodeResult.class);
    }

    @Override
    public String getHelpText() {
        return CommandRouter.getInstance().generateHelpForCommand("bytecode");
    }
}
