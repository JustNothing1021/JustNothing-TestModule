package com.justnothing.testmodule.command.functions.threads.request;

import com.justnothing.testmodule.command.framework.annotation.CmdParam;
import com.justnothing.testmodule.command.framework.model.CommandRequest;
import com.justnothing.testmodule.command.functions.threads.ThreadsTexts;
import com.justnothing.testmodule.command.functions.threads.response.ThreadProfileExportResult;

public class ThreadProfileExportRequest extends CommandRequest<ThreadProfileExportResult> {

    @CmdParam(
        name = "--file",
        description = ThreadsTexts.PARAM_THREADS_PROFILE_EXPORT_FILE_DESC,
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
