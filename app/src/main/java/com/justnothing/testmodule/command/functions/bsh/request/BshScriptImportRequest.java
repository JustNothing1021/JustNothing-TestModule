package com.justnothing.testmodule.command.functions.bsh.request;

import com.justnothing.testmodule.command.framework.base.protocol.CommandRequest;
import com.justnothing.testmodule.command.framework.base.protocol.SerializeKeyName;
import com.justnothing.testmodule.command.framework.base.command.CmdParam;

@SerializeKeyName("bsh:script:import")
public class BshScriptImportRequest extends CommandRequest {

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
