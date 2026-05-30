package com.justnothing.testmodule.command.functions.threads.request;

import com.justnothing.testmodule.command.base.command.CmdParam;
import com.justnothing.testmodule.command.base.protocol.CommandRequest;
import com.justnothing.testmodule.command.base.protocol.SerializeKeyName;

@SerializeKeyName("threads:profile:export")
public class ThreadProfileExportRequest extends CommandRequest {

    @CmdParam(
        name = "--file",
        description = "导出文件路径",
        required = true,
        position = 1,
        serializedName = "filePath"
    )
    private String filePath;

    public ThreadProfileExportRequest() {
        super();
    }

    public String getFilePath() { return filePath; }
    public void setFilePath(String filePath) { this.filePath = filePath; }
}
