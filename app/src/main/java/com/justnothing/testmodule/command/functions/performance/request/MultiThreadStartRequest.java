package com.justnothing.testmodule.command.functions.performance.request;

import com.justnothing.testmodule.command.framework.annotation.CmdParam;
import com.justnothing.testmodule.command.functions.performance.PerformanceTexts;
import com.justnothing.testmodule.command.functions.performance.response.MultiThreadResult;

public class MultiThreadStartRequest extends PerformanceRequest<MultiThreadResult> {

    @CmdParam(
        name = "rate",
        position = 1,
        required = false,
        defaultValue = "100",
        description = PerformanceTexts.PARAM_PERFORMANCE_MULTITHREAD_START_RATE_DESC
    )
    private int rate = 100;

    @CmdParam(
        name = "--exclude",
        aliases = {"-e"},
        required = false,
        description = PerformanceTexts.PARAM_PERFORMANCE_MULTITHREAD_START_EXCLUDE_DESC
    )
    private String exclude;

    public MultiThreadStartRequest() {
        super();
    }

    public int getRate() { return rate; }
    public void setRate(int rate) { this.rate = rate; }

    public String getExclude() { return exclude; }
    public void setExclude(String exclude) { this.exclude = exclude; }
}
