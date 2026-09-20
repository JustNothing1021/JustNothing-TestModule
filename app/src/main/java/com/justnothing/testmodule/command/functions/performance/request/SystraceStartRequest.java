package com.justnothing.testmodule.command.functions.performance.request;

import com.justnothing.testmodule.command.framework.annotation.CmdParam;
import com.justnothing.testmodule.command.functions.performance.PerformanceTexts;
import com.justnothing.testmodule.command.functions.performance.response.SystraceResult;

public class SystraceStartRequest extends PerformanceRequest<SystraceResult> {

    @CmdParam(
        name = "duration",
        position = 1,
        required = false,
        description = PerformanceTexts.PARAM_PERFORMANCE_SYSTRACE_START_DURATION_DESC
    )
    private Integer duration;

    @CmdParam(
        name = "categories",
        position = 2,
        required = false,
        varArgs = true,
        description = PerformanceTexts.PARAM_PERFORMANCE_SYSTRACE_START_CATEGORIES_DESC
    )
    private String categories;

    public SystraceStartRequest() {
        super();
    }

    public Integer getDuration() { return duration; }
    public void setDuration(Integer duration) { this.duration = duration; }

    public String getCategories() { return categories; }
    public void setCategories(String categories) { this.categories = categories; }
}
