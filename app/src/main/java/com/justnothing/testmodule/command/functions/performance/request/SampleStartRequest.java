package com.justnothing.testmodule.command.functions.performance.request;

import com.justnothing.testmodule.command.framework.annotation.CmdParam;
import com.justnothing.testmodule.command.functions.performance.PerformanceTexts;
import com.justnothing.testmodule.command.functions.performance.response.SampleResult;

public class SampleStartRequest extends PerformanceRequest<SampleResult> {

    @CmdParam(
        name = "rate",
        position = 1,
        required = false,
        defaultValue = "100",
        description = PerformanceTexts.PARAM_PERFORMANCE_SAMPLE_START_RATE_DESC
    )
    private int rate = 100;

    @CmdParam(
        name = "--exclude",
        aliases = {"-e"},
        required = false,
        description = PerformanceTexts.PARAM_PERFORMANCE_SAMPLE_START_EXCLUDE_DESC
    )
    private String exclude;

    public SampleStartRequest() {
        super();
    }

    public int getRate() { return rate; }
    public void setRate(int rate) { this.rate = rate; }

    public String getExclude() { return exclude; }
    public void setExclude(String exclude) { this.exclude = exclude; }
}
