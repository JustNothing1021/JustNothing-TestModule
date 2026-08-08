package com.justnothing.testmodule.command.functions.threads.request;

import com.justnothing.testmodule.command.framework.annotation.CmdParam;
import com.justnothing.testmodule.command.framework.model.CommandRequest;
import com.justnothing.testmodule.command.framework.annotation.SerializeKeyName;

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
