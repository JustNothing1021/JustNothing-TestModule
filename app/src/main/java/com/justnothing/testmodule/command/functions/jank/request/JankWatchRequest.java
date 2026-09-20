package com.justnothing.testmodule.command.functions.jank.request;

import com.justnothing.testmodule.command.framework.annotation.CmdParam;
import com.justnothing.testmodule.command.framework.model.CommandRequest;
import com.justnothing.testmodule.command.functions.jank.response.JankResult;

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
        description = "轮询间隔（毫秒）；每轮只做一次 readdir，压到 100ms 也不会明显干扰系统"
    )
    private int interval = 1000;

    public JankWatchRequest() {
        super();
    }

    public int getInterval() { return interval; }
    public void setInterval(int interval) { this.interval = interval; }
}
