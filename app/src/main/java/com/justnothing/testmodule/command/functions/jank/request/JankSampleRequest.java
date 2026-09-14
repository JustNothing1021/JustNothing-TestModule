package com.justnothing.testmodule.command.functions.jank.request;

import com.justnothing.testmodule.command.framework.annotation.CmdParam;
import com.justnothing.testmodule.command.framework.model.CommandRequest;
import com.justnothing.testmodule.command.functions.jank.response.JankResult;

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
        description = "采样多少秒"
    )
    private int duration = 20;

    @CmdParam(
        name = "interval",
        aliases = {"-i", "--interval"},
        required = false,
        defaultValue = "1000",
        description = "采样间隔（毫秒）；低于 500ms 时采集开销会明显干扰结果"
    )
    private int interval = 1000;

    @CmdParam(
        name = "top",
        aliases = {"-n", "--top"},
        required = false,
        defaultValue = "5",
        description = "CPU TOP 榜单长度"
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
