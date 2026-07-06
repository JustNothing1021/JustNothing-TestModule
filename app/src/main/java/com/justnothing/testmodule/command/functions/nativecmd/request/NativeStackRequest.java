package com.justnothing.testmodule.command.functions.nativecmd.request;

import com.justnothing.testmodule.command.framework.base.protocol.CommandRequest;
import com.justnothing.testmodule.command.framework.base.command.CmdParam;
import com.justnothing.testmodule.command.framework.base.protocol.SerializeKeyName;

@SerializeKeyName("native:stack")
public class NativeStackRequest extends CommandRequest {

    @CmdParam(name = "threadId", aliases = {"-t", "--thread"}, required = false, description = "线程ID")
    private String threadId;

    public String getThreadId() { return threadId; }
    public void setThreadId(String threadId) { this.threadId = threadId; }
}
