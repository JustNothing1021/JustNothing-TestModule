package com.justnothing.testmodule.command.functions.threads.request;

import com.justnothing.testmodule.command.framework.annotation.CmdParam;
import com.justnothing.testmodule.command.framework.model.CommandRequest;
import com.justnothing.testmodule.command.functions.threads.ThreadsTexts;
import com.justnothing.testmodule.command.functions.threads.response.ThreadProfileStartResult;
import java.util.List;

public class ThreadProfileStartRequest extends CommandRequest<ThreadProfileStartResult> {

    @CmdParam(
        name = "--duration",
        description = ThreadsTexts.PARAM_THREADS_PROFILE_START_DURATION_DESC,
        required = false,
        defaultValue = "60",
        min = 1,
        max = 3600,
        position = 1,
        serializedName = "duration"
    )
    private Integer duration;

    @CmdParam(
        name = "--target-threads",
        description = ThreadsTexts.PARAM_THREADS_PROFILE_START_TARGET_THREADS_DESC,
        required = false,
        varArgs = true,
        serializedName = "targetThreads"
    )
    private List<String> targetThreads;

    public ThreadProfileStartRequest() {
        super();
        this.duration = 60;
    }

    public Integer getDuration() { return duration; }
    public void setDuration(Integer duration) { this.duration = duration; }

    public List<String> getTargetThreads() { return targetThreads; }
    public void setTargetThreads(List<String> targetThreads) { this.targetThreads = targetThreads; }
}
