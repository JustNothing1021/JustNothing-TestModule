package com.justnothing.testmodule.command.functions.jank.request;

import com.justnothing.testmodule.command.framework.annotation.CmdParam;
import com.justnothing.testmodule.command.framework.model.CommandRequest;
import com.justnothing.testmodule.command.functions.jank.response.JankResult;
import com.justnothing.testmodule.command.functions.jank.JankTexts;

/**
 * {@code jank watch} 的参数：只有一个轮询间隔。
 *
 * <p>没有"时长"：监视是挂着看的，什么时候停由用户决定（Ctrl-C 结束），
 * 定长的那种需求 {@code jank sample} 已经覆盖了。</p>
 */
public class JankWatchRequest extends CommandRequest<JankResult> {

    @CmdParam(
        name = "interval",
        aliases = {"-i", "--interval"},
        required = false,
        defaultValue = "1000",
        description = JankTexts.PARAM_JANK_WATCH_INTERVAL_DESC
    )
    private int interval = 1000;

    public JankWatchRequest() {
        super();
    }

    public int getInterval() { return interval; }
    public void setInterval(int interval) { this.interval = interval; }
}
