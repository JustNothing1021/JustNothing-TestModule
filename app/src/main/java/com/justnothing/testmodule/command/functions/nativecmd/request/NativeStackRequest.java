package com.justnothing.testmodule.command.functions.nativecmd.request;

import com.justnothing.testmodule.command.framework.model.CommandRequest;
import com.justnothing.testmodule.command.functions.nativecmd.response.NativeResult;
import com.justnothing.testmodule.command.framework.annotation.CmdParam;
import com.justnothing.testmodule.command.functions.nativecmd.NativeTexts;

public class NativeStackRequest extends CommandRequest<NativeResult> {

    @CmdParam(name = "threadId", aliases = {"-t", "--thread"}, required = false, description = NativeTexts.PARAM_NATIVE_STACK_THREADID_DESC)
    private String threadId;

    public String getThreadId() { return threadId; }
    public void setThreadId(String threadId) { this.threadId = threadId; }
}
