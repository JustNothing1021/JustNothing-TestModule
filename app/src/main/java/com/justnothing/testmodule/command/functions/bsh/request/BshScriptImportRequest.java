package com.justnothing.testmodule.command.functions.bsh.request;

import com.justnothing.testmodule.command.framework.model.CommandRequest;
import com.justnothing.testmodule.command.functions.bsh.response.BeanShellResult;
import com.justnothing.testmodule.command.framework.annotation.CmdParam;

public class BshScriptImportRequest extends CommandRequest<BeanShellResult> {

    @CmdParam(
        name = "filePath",
        position = 1,
        required = true,
        description = "导入文件路径"
    )
    private String filePath;

    public BshScriptImportRequest() {
        super();
    }

    public String getFilePath() { return filePath; }
    public void setFilePath(String filePath) { this.filePath = filePath; }
}
