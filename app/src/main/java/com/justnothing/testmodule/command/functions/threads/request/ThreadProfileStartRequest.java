package com.justnothing.testmodule.command.functions.threads.request;

import com.justnothing.testmodule.command.base.command.CmdParam;
import com.justnothing.testmodule.command.base.protocol.CommandRequest;
import com.justnothing.testmodule.command.base.protocol.SerializeKeyName;
import java.util.List;

@SerializeKeyName("threads:profile:start")
public class ThreadProfileStartRequest extends CommandRequest {

    @CmdParam(
        name = "--duration",
        description = "分析时长(秒)",
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
        description = "目标线程ID列表",
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
