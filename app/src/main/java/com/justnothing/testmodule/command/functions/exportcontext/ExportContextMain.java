package com.justnothing.testmodule.command.functions.exportcontext;

import com.justnothing.testmodule.command.base.*;
import com.justnothing.testmodule.command.CommandExecutor;
import com.justnothing.testmodule.command.base.command.CommandInfo;
import com.justnothing.testmodule.command.base.command.RegisterCommand;
import com.justnothing.testmodule.command.base.command.SubCommand;
import com.justnothing.testmodule.command.base.command.SubCommands;
import com.justnothing.testmodule.command.base.protocol.CommandRequest;

@RegisterCommand("export-context")
@CommandInfo(
    name = "export-context",
    group = "system",
    description = "导出设备上下文信息, 包括HTTP配置, 设备标识等",
    helpText = "语法: export-context [options]\n\n" +
              "导出当前设备上下文信息, 包括设备信息、应用信息、系统状态等.\n\n" +
              "选项:\n" +
              "  -p, --pretty-printing   以表格格式输出 (默认为JSON原始数据)\n\n" +
              "输出格式:\n" +
              "  默认: JSON格式的结构化数据 (兼容 xtc-httplib)\n" +
              "  -p:   带边框的表格格式 (人类可读)\n\n" +
              "示例:\n" +
              "  export-context                    输出JSON数据\n" +
              "  export-context -p                 输出表格格式\n" +
              "  export-context --pretty-printing  同上\n\n" +
              "注意: \n" +
              "  - 默认输出JSON是为了兼容外部工具 (如 xtc-httplib) 直接解析\n" +
              "  使用 -p 参数可获得更易读的表格输出\n\n" +
              "(Submodule export-context %s)",
    version = "1.0.0",
    resultType = ExportContextResult.class
)
@SubCommands({
    @SubCommand(request = ExportContextRequest.class, result = ExportContextResult.class, command = ExportContextCommand.class, description = "导出设备上下文信息")
})
public class ExportContextMain extends MainCommand<ExportContextResult> {

    public ExportContextMain() {
        super("export-context", ExportContextResult.class);
    }

    @Override
    public ExportContextResult runMain(CommandExecutor.CmdExecContext<CommandRequest> context) throws Exception {
        if (context.getRequest() == null) {
            context.setRequest(new ExportContextRequest());
        }

        AbstractCommand<?, ?> command = resolveSubCommandFromRequest(context.getRequest());
        if (command != null) {
            @SuppressWarnings("unchecked")
            AbstractCommand<ExportContextRequest, ExportContextResult> typedCommand =
                (AbstractCommand<ExportContextRequest, ExportContextResult>) command;
            return typedCommand.execute(context);
        }

        return createErrorResult("无法解析子命令");
    }
}
