package com.justnothing.testmodule.command.functions.bytecode;

import static com.justnothing.testmodule.constants.CommandServer.CMD_BYTECODE_VER;

import com.justnothing.testmodule.command.framework.model.MainCommand;
import com.justnothing.testmodule.command.framework.annotation.Cmd;
import com.justnothing.testmodule.command.framework.annotation.CmdRoutes;
import com.justnothing.testmodule.command.framework.model.CommandRouter;
import com.justnothing.testmodule.command.functions.bytecode.impl.BytecodeManageCommand;
import com.justnothing.testmodule.command.functions.bytecode.impl.BytecodeQueryCommand;
import com.justnothing.testmodule.command.functions.bytecode.request.BytecodeInfoRequest;
import com.justnothing.testmodule.command.functions.bytecode.request.BytecodeMethodRequest;
import com.justnothing.testmodule.command.functions.bytecode.request.BytecodeDumpRequest;
import com.justnothing.testmodule.command.functions.bytecode.request.BytecodeFindRequest;
import com.justnothing.testmodule.command.functions.bytecode.request.BytecodeLocateRequest;
import com.justnothing.testmodule.command.functions.bytecode.request.BytecodeAnalyzeRequest;
import com.justnothing.testmodule.command.functions.bytecode.request.BytecodeDisasmRequest;
import com.justnothing.testmodule.command.functions.bytecode.request.BytecodeConstantsRequest;
import com.justnothing.testmodule.command.functions.bytecode.request.BytecodeVerifyRequest;
import com.justnothing.testmodule.command.functions.bytecode.request.BytecodeBatchExportRequest;
import com.justnothing.testmodule.command.functions.bytecode.request.BytecodeListClassesRequest;
import com.justnothing.testmodule.command.functions.bytecode.request.BytecodeSourceRequest;
import com.justnothing.testmodule.command.functions.bytecode.response.BytecodeResult;

@Cmd(
    name = "bytecode",
    description = BytecodeTexts.CMD_BYTECODE_DESC,
    version = CMD_BYTECODE_VER
)
@CmdRoutes({
    @CmdRoutes.Route(
        path = "info",
        request = BytecodeInfoRequest.class,
        handler = BytecodeQueryCommand.class,
        description = BytecodeTexts.ROUTE_BYTECODE_INFO_DESC
    ),
    @CmdRoutes.Route(
        path = "method",
        request = BytecodeMethodRequest.class,
        handler = BytecodeQueryCommand.class,
        description = BytecodeTexts.ROUTE_BYTECODE_METHOD_DESC
    ),
    @CmdRoutes.Route(
        path = "dump",
        request = BytecodeDumpRequest.class,
        handler = BytecodeManageCommand.class,
        description = BytecodeTexts.ROUTE_BYTECODE_DUMP_DESC
    ),
    @CmdRoutes.Route(
        path = "locate",
        request = BytecodeLocateRequest.class,
        handler = BytecodeManageCommand.class,
        description = BytecodeTexts.ROUTE_BYTECODE_LOCATE_DESC
    ),
    @CmdRoutes.Route(
        path = "analyze",
        request = BytecodeAnalyzeRequest.class,
        handler = BytecodeQueryCommand.class,
        description = BytecodeTexts.ROUTE_BYTECODE_ANALYZE_DESC
    ),
    @CmdRoutes.Route(
        path = "disasm",
        request = BytecodeDisasmRequest.class,
        handler = BytecodeQueryCommand.class,
        description = BytecodeTexts.ROUTE_BYTECODE_DISASM_DESC
    ),
    @CmdRoutes.Route(
        path = "constants",
        request = BytecodeConstantsRequest.class,
        handler = BytecodeQueryCommand.class,
        description = BytecodeTexts.ROUTE_BYTECODE_CONSTANTS_DESC
    ),
    @CmdRoutes.Route(
        path = "verify",
        request = BytecodeVerifyRequest.class,
        handler = BytecodeQueryCommand.class,
        description = BytecodeTexts.ROUTE_BYTECODE_VERIFY_DESC
    ),
    @CmdRoutes.Route(
        path = "source",
        request = BytecodeSourceRequest.class,
        handler = BytecodeQueryCommand.class,
        description = BytecodeTexts.ROUTE_BYTECODE_SOURCE_DESC
    ),
    @CmdRoutes.Route(
        path = "batch_export",
        request = BytecodeBatchExportRequest.class,
        handler = BytecodeManageCommand.class,
        description = BytecodeTexts.ROUTE_BYTECODE_BATCH_EXPORT_DESC
    ),
    @CmdRoutes.Route(
        path = "list_classes",
        request = BytecodeListClassesRequest.class,
        handler = BytecodeManageCommand.class,
        description = BytecodeTexts.ROUTE_BYTECODE_LIST_CLASSES_DESC
    ),
    @CmdRoutes.Route(
        path = "find",
        request = BytecodeFindRequest.class,
        handler = BytecodeManageCommand.class,
        description = BytecodeTexts.ROUTE_BYTECODE_FIND_DESC
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
