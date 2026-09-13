package com.justnothing.testmodule.command.functions.performance.request;

import com.justnothing.testmodule.command.framework.annotation.CmdParam;
import com.justnothing.testmodule.command.functions.performance.PerformanceRequest;
import com.justnothing.testmodule.command.functions.performance.response.SystraceResult;

public class SystraceStartRequest extends PerformanceRequest<SystraceResult> {

    @CmdParam(
        name = "duration",
        position = 1,
        required = false,
        description = "持续时间(ms)"
    )
    private Integer duration;

    @CmdParam(
        name = "categories",
        position = 2,
        required = false,
        varArgs = true,
        description = "跟踪类别"
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
