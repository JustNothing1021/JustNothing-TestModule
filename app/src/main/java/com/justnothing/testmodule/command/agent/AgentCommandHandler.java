package com.justnothing.testmodule.command.agent;

import android.content.Context;
import com.justnothing.testmodule.command.base.protocol.CommandResult;
import org.json.JSONObject;

/**
 * Agent 命令处理器基类（统一接口）
 * <p>
 * 所有 Agent Handler 现在返回 {@link CommandResult}，与 CLI 命令体系保持一致。
 * <p>
 * IPC 层（InspectionAgent）负责将 CommandResult 序列化为 JSON 通过 socket 发送。
 */
public abstract class AgentCommandHandler {

    public abstract String getCommandType();

    /**
     * 处理 Agent 请求，返回结构化的 CommandResult
     *
     * @param params  客户端传来的参数（JSON-RPC params 字段）
     * @param context 目标进程的 Android Context
     * @return 统一的 CommandResult，包含 success/data/error 信息
     */
    public abstract CommandResult handle(JSONObject params, Context context) throws Exception;
}
