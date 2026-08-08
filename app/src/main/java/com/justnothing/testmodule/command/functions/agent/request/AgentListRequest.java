package com.justnothing.testmodule.command.functions.agent.request;

import com.justnothing.testmodule.command.framework.model.CommandRequest;
import com.justnothing.testmodule.command.framework.annotation.SerializeKeyName;

@SerializeKeyName("AgentList")
public class AgentListRequest extends CommandRequest {
    // 无参数命令 — 列出所有在线 Agent
}
