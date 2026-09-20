package com.justnothing.testmodule.command.framework.model;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import com.justnothing.testmodule.command.framework.utils.GsonFactory;
import com.justnothing.testmodule.utils.logging.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public abstract class CommandRequest<Res extends CommandResult> {

    private static final Logger logger = Logger.getLoggerForName("CommandRequest");

    @Expose @SerializedName("requestId")
    private String requestId;

    @Expose @SerializedName("commandType")
    private String commandType;

    // 操作符追踪列表：记录实际使用了哪些操作符（如 ["get", "set"]）
    private final List<String> receivedOperators = new ArrayList<>();

    public CommandRequest() {
        this.requestId = UUID.randomUUID().toString();
        this.commandType = extractTypeKey();
    }

    private String extractTypeKey() {
        // key 完全由路由派生：CommandRouter 注册路由时按 "父路径/子路径" 自动生成，
        // 因此它与路由永远一致，不存在手写注解写错或漏改的问题。
        @SuppressWarnings("unchecked")
        Class<? extends CommandRequest<?>> type = (Class<? extends CommandRequest<?>>) getClass();
        String derived = CommandRouter.getInstance().getCommandTypeFor(type);
        if (derived != null) return derived;

        // 没有任何路由引用这个 Request：正常命令不可能走到这里，
        // "每个 Request 都必须挂在一条路由上"由 CmdParamDeclarationTest 在构建期兜底。
        logger.warn("Request 类 " + getClass().getSimpleName() + " 没有注册路由，commandType 将为空");
        return null;
    }

    public String getRequestId() { return requestId; }
    public void setRequestId(String requestId) { this.requestId = requestId; }
    public String getCommandType() { return commandType; }
    public void setCommandType(String commandType) { this.commandType = commandType; }

    /**
     * 添加已接收的操作符（由 CmdArgParser.handleOperator() 调用）
     */
    public void addReceivedOperator(String operatorName) {
        if (operatorName != null && !operatorName.isEmpty() && !receivedOperators.contains(operatorName)) {
            receivedOperators.add(operatorName);
        }
    }

    /**
     * 检查是否使用了指定操作符
     */
    public boolean hasOperator(String operatorName) {
        return receivedOperators.contains(operatorName);
    }


    public String toJsonString() {
        return GsonFactory.getInstance().toJson(this);
    }


    @SuppressWarnings("unchecked")
    public <T extends CommandRequest<?>> T fromJsonString(String jsonStr) {
        if (jsonStr == null || jsonStr.trim().isEmpty()) {
            logger.warn("Cannot deserialize from null/empty string");
            return (T) this;
        }

        try {
            T result = (T) GsonFactory.getInstance().fromJson(jsonStr, this.getClass());

            if (result != null) {
                CommandFieldCopier.copy(result, this);
            } else {
                logger.warn("Gson returned null for " + this.getClass().getSimpleName());
            }
            return (T) this;
        } catch (Exception e) {
            logger.error("Failed to deserialize from string", e);
            return (T) this;
        }
    }
}
