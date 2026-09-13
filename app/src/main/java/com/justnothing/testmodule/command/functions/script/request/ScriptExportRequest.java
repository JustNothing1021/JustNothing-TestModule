package com.justnothing.testmodule.command.functions.script.request;

import com.justnothing.testmodule.command.functions.script.ScriptResult;
import com.justnothing.testmodule.command.framework.annotation.CmdParam;

public class ScriptExportRequest extends ScriptBaseRequest<ScriptResult> {

    @CmdParam(name = "name", position = 1, required = true, description = "脚本名称")
    private String name;

    @CmdParam(name = "filePath", position = 2, required = true, description = "导出路径")
    private String filePath;

    public ScriptExportRequest() {
        super();
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getFilePath() { return filePath; }
    public void setFilePath(String filePath) { this.filePath = filePath; }
}
