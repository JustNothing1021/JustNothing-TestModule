package com.justnothing.testmodule.command.functions.threads.request;

import com.justnothing.testmodule.command.base.command.CmdParam;
import com.justnothing.testmodule.command.base.protocol.CommandRequest;
import com.justnothing.testmodule.command.base.protocol.SerializeKeyName;

@SerializeKeyName("threads:list")
public class ThreadListRequest extends CommandRequest {

    @CmdParam(
        name = "--id",
        description = "只显示指定ID的线程",
        required = false,
        position = 1,
        serializedName = "threadId"
    )
    private String threadId;

    @CmdParam(
        name = "--name",
        description = "只显示指定名称的线程",
        required = false,
        position = 2,
        serializedName = "threadName"
    )
    private String threadName;

    @CmdParam(
        name = "--state",
        description = "只显示指定状态的线程",
        required = false,
        allowedValues = {"NEW", "RUNNABLE", "BLOCKED", "WAITING", "TIMED_WAITING", "TERMINATED"},
        serializedName = "threadState"
    )
    private String state;

    @CmdParam(
        name = "--filter-id",
        description = "按线程ID过滤",
        required = false,
        serializedName = "filterId"
    )
    private Long filterId;

    @CmdParam(
        name = "--filter-name",
        description = "按线程名称过滤",
        required = false,
        serializedName = "filterName"
    )
    private String filterName;

    @CmdParam(
        name = "--filter-state",
        description = "按线程状态过滤",
        required = false,
        serializedName = "filterState"
    )
    private String filterState;

    public ThreadListRequest() {
        super();
    }

    public String getThreadId() { return threadId; }
    public void setThreadId(String threadId) { this.threadId = threadId; }

    public String getThreadName() { return threadName; }
    public void setThreadName(String threadName) { this.threadName = threadName; }

    public String getState() { return state; }
    public void setState(String state) { this.state = state; }

    public Long getFilterId() { return filterId; }
    public void setFilterId(Long filterId) { this.filterId = filterId; }

    public String getFilterName() { return filterName; }
    public void setFilterName(String filterName) { this.filterName = filterName; }

    public String getFilterState() { return filterState; }
    public void setFilterState(String filterState) { this.filterState = filterState; }
}
