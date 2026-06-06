package com.justnothing.testmodule.command.functions.bsh.request;

import com.justnothing.testmodule.command.base.protocol.CommandRequest;
import com.justnothing.testmodule.command.base.protocol.SerializeKeyName;
import com.justnothing.testmodule.command.base.command.CmdParam;

@SerializeKeyName("bsh:script:export")
public class BshScriptExportRequest extends CommandRequest {

    @CmdParam(
        name = "name",
        position = 1,
        required = true,
        description = "脚本名称"
    )
    private String name;

    @CmdParam(
        name = "exportPath",
        position = 2,
        required = true,
        description = "导出路径"
    )
    private String exportPath;

    public BshScriptExportRequest() {
        super();
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getExportPath() { return exportPath; }
    public void setExportPath(String exportPath) { this.exportPath = exportPath; }
}
