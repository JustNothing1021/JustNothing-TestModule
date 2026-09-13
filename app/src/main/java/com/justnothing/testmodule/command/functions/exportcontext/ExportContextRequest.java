package com.justnothing.testmodule.command.functions.exportcontext;

import com.justnothing.testmodule.command.framework.annotation.CmdParam;
import com.justnothing.testmodule.command.framework.model.CommandRequest;

// key 必须等于服务端路由的完整路径。export-context 路由的 path 为空，
// fullPath 即父路径本身（如 "export-context"，参见 CommandRouter.registerRoute）。
public class ExportContextRequest extends CommandRequest<ExportContextResult> {

    @CmdParam(
        name = "prettyPrinting",
        aliases = {"-p", "--pretty-printing"},
        required = false,
        description = "以表格格式输出 (默认为JSON原始数据)"
    )
    private Boolean prettyPrinting = false;

    public ExportContextRequest() {
        super();
    }

    public boolean isPrettyPrinting() {
        return prettyPrinting != null && prettyPrinting;
    }

    public void setPrettyPrinting(boolean prettyPrinting) {
        this.prettyPrinting = prettyPrinting;
    }
}
