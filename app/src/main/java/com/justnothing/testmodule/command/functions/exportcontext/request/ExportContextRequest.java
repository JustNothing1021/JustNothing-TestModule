package com.justnothing.testmodule.command.functions.exportcontext.request;

import com.justnothing.testmodule.command.framework.annotation.CmdParam;
import com.justnothing.testmodule.command.framework.model.CommandRequest;
import com.justnothing.testmodule.command.functions.exportcontext.ExportContextTexts;
import com.justnothing.testmodule.command.functions.exportcontext.response.ExportContextResult;

// key 必须等于服务端路由的完整路径。export-context 路由的 path 为空，
// fullPath 即父路径本身（如 "export-context"，参见 CommandRouter.registerRoute）。
public class ExportContextRequest extends CommandRequest<ExportContextResult> {

    @CmdParam(
        name = "prettyPrinting",
        aliases = {"-p", "--pretty-printing"},
        required = false,
        description = ExportContextTexts.PARAM_EXPORT_CONTEXT_PRETTY_PRINTING_DESC
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
