package com.justnothing.testmodule.command.functions.script.request;

import com.justnothing.testmodule.command.functions.script.response.ScriptResult;
import com.justnothing.testmodule.command.framework.annotation.CmdParam;

public class ScriptImportRequest extends ScriptBaseRequest<ScriptResult> {

    @CmdParam(name = "filePath", position = 1, required = true, description = "导入文件路径")
    private String filePath;

    public ScriptImportRequest() {
        super();
    }

    public String getFilePath() { return filePath; }
    public void setFilePath(String filePath) { this.filePath = filePath; }
}
