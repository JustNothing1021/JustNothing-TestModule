package com.justnothing.testmodule.command.functions.script.request;

import com.justnothing.testmodule.command.functions.script.ScriptTexts;
import com.justnothing.testmodule.command.functions.script.response.ScriptResult;
import com.justnothing.testmodule.command.framework.annotation.CmdParam;

public class ScriptExportRequest extends ScriptBaseRequest<ScriptResult> {

    @CmdParam(name = "name", position = 1, required = true, description = ScriptTexts.PARAM_SCRIPT_EXPORT_NAME_DESC)
    private String name;

    @CmdParam(name = "filePath", position = 2, required = true, description = ScriptTexts.PARAM_SCRIPT_EXPORT_FILEPATH_DESC)
    private String filePath;

    public ScriptExportRequest() {
        super();
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getFilePath() { return filePath; }
    public void setFilePath(String filePath) { this.filePath = filePath; }
}
