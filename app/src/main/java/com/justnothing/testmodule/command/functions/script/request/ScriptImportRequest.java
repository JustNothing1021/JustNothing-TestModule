package com.justnothing.testmodule.command.functions.script.request;

import com.justnothing.testmodule.command.functions.script.ScriptTexts;
import com.justnothing.testmodule.command.functions.script.response.ScriptResult;
import com.justnothing.testmodule.command.framework.annotation.CmdParam;

public class ScriptImportRequest extends ScriptBaseRequest<ScriptResult> {

    @CmdParam(name = "filePath", position = 1, required = true, description = ScriptTexts.PARAM_SCRIPT_IMPORT_FILEPATH_DESC)
    private String filePath;

    public ScriptImportRequest() {
        super();
    }

    public String getFilePath() { return filePath; }
    public void setFilePath(String filePath) { this.filePath = filePath; }
}
