package com.justnothing.testmodule.command.functions.threads.request;

import com.justnothing.testmodule.command.framework.annotation.CmdParam;
import com.justnothing.testmodule.command.framework.model.CommandRequest;
import com.justnothing.testmodule.command.functions.threads.response.ThreadListResult;

public class ThreadListRequest extends CommandRequest<ThreadListResult> {

    /** 明细级别：只带线程状态信息（GUI 高频刷新用，不传堆栈） */
    public static final String LEVEL_BASIC = "basic";
    /** 明细级别：携带完整堆栈（GUI 手动刷新用） */
    public static final String LEVEL_FULL = "full";

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

    @CmdParam(
        name = "--detail-level",
        description = "明细级别: basic=不含堆栈, full=含堆栈",
        required = false,
        defaultValue = LEVEL_FULL,
        allowedValues = {LEVEL_BASIC, LEVEL_FULL},
        serializedName = "detailLevel"
    )
    private String detailLevel = LEVEL_FULL;

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

    public String getDetailLevel() { return detailLevel; }
    public void setDetailLevel(String detailLevel) { this.detailLevel = detailLevel; }
}
