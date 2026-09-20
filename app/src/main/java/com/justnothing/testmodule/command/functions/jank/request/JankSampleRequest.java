package com.justnothing.testmodule.command.functions.jank.request;

import com.justnothing.testmodule.command.framework.annotation.CmdParam;
import com.justnothing.testmodule.command.framework.model.CommandRequest;
import com.justnothing.testmodule.command.functions.jank.response.JankResult;
import com.justnothing.testmodule.command.functions.jank.JankTexts;

/**
 * {@code jank sample} 的参数。
 *
 * <p>默认值都往"轻"的方向取：采样本身要占 CPU，间隔太密就会把被测对象挤成它自己。</p>
 */
public class JankSampleRequest extends CommandRequest<JankResult> {

    @CmdParam(
        name = "duration",
        position = 1,
        required = false,
        defaultValue = "20",
        description = JankTexts.PARAM_JANK_SAMPLE_DURATION_DESC
    )
    private int duration = 20;

    @CmdParam(
        name = "interval",
        aliases = {"-i", "--interval"},
        required = false,
        defaultValue = "1000",
        description = JankTexts.PARAM_JANK_SAMPLE_INTERVAL_DESC
    )
    private int interval = 1000;

    @CmdParam(
        name = "top",
        aliases = {"-n", "--top"},
        required = false,
        defaultValue = "5",
        description = JankTexts.PARAM_JANK_SAMPLE_TOP_DESC
    )
    private int top = 5;

    public JankSampleRequest() {
        super();
    }

    public int getDuration() { return duration; }
    public void setDuration(int duration) { this.duration = duration; }

    public int getInterval() { return interval; }
    public void setInterval(int interval) { this.interval = interval; }

    public int getTop() { return top; }
    public void setTop(int top) { this.top = top; }
}
