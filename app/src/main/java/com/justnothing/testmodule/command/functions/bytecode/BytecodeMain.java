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
    description = "查看和分析Java字节码",
    version = CMD_BYTECODE_VER
)
@CmdRoutes({
    @CmdRoutes.Route(
        path = "info",
        request = BytecodeInfoRequest.class,
        handler = BytecodeQueryCommand.class,
        description = "查看类的元数据（反射）"
    ),
    @CmdRoutes.Route(
        path = "method",
        request = BytecodeMethodRequest.class,
        handler = BytecodeQueryCommand.class,
        description = "查看指定方法的元数据（方法体需导出 dex 后反编译）"
    ),
    @CmdRoutes.Route(
        path = "dump",
        request = BytecodeDumpRequest.class,
        handler = BytecodeManageCommand.class,
        description = "导出某个类所在的 dex（-d 顺带用 dexdump 反汇编）"
    ),
    @CmdRoutes.Route(
        path = "locate",
        request = BytecodeLocateRequest.class,
        handler = BytecodeManageCommand.class,
        description = "查找类在哪个文件里（只定位，不提取）"
    ),
    @CmdRoutes.Route(
        path = "analyze",
        request = BytecodeAnalyzeRequest.class,
        handler = BytecodeQueryCommand.class,
        description = "分析类所在 dex 的结构"
    ),
    @CmdRoutes.Route(
        path = "disasm",
        request = BytecodeDisasmRequest.class,
        handler = BytecodeQueryCommand.class,
        description = "反汇编成 dalvik 指令（设备自带 dexdump，可选方法名 / -o 存文件）"
    ),
    @CmdRoutes.Route(
        path = "constants",
        request = BytecodeConstantsRequest.class,
        handler = BytecodeQueryCommand.class,
        description = "查看静态常量字段"
    ),
    @CmdRoutes.Route(
        path = "verify",
        request = BytecodeVerifyRequest.class,
        handler = BytecodeQueryCommand.class,
        description = "校验类所在 dex 的完整性"
    ),
    @CmdRoutes.Route(
        path = "source",
        request = BytecodeSourceRequest.class,
        handler = BytecodeQueryCommand.class,
        description = "在设备上把类反编译成 Java 源码（dex2jar + CFR，按类处理，省内存）"
    ),
    @CmdRoutes.Route(
        path = "batch_export",
        request = BytecodeBatchExportRequest.class,
        handler = BytecodeManageCommand.class,
        description = "批量导出 dex"
    ),
    @CmdRoutes.Route(
        path = "list_classes",
        request = BytecodeListClassesRequest.class,
        handler = BytecodeManageCommand.class,
        description = "列出代码来源里的类名"
    ),
    @CmdRoutes.Route(
        path = "find",
        request = BytecodeFindRequest.class,
        handler = BytecodeManageCommand.class,
        description = "按关键词模糊搜索类名（不区分大小写，跨所有代码来源）"
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
