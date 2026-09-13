package com.justnothing.testmodule.command.functions.nativecmd.request;

import com.justnothing.testmodule.command.framework.model.CommandRequest;
import com.justnothing.testmodule.command.functions.nativecmd.NativeResult;
import com.justnothing.testmodule.command.framework.annotation.CmdParam;

public class NativeStackRequest extends CommandRequest<NativeResult> {

    @CmdParam(name = "threadId", aliases = {"-t", "--thread"}, required = false, description = "线程ID")
    private String threadId;

    public String getThreadId() { return threadId; }
    public void setThreadId(String threadId) { this.threadId = threadId; }
}
