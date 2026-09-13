package com.justnothing.testmodule.command.framework.model;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import com.justnothing.testmodule.command.framework.utils.GsonFactory;
import com.justnothing.testmodule.utils.logging.Logger;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
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

    /**
     * 从 Gson 反序列化的结果复制字段到 this
     */
    private void copyFieldsFrom(CommandRequest<?> source) {
        try {
            for (Field field : source.getClass().getDeclaredFields()) {
                if (Modifier.isStatic(field.getModifiers())) {
                    continue;
                }
                field.setAccessible(true);
                copyField(field.getName(), field.get(source));
            }

            Class<?> superClass = source.getClass().getSuperclass();
            while (superClass != null && superClass != Object.class) {
                for (Field field : superClass.getDeclaredFields()) {
                    if (Modifier.isStatic(field.getModifiers())) {
                        continue;
                    }
                    field.setAccessible(true);
                    copyField(field.getName(), field.get(source));
                }
                superClass = superClass.getSuperclass();
            }
        } catch (Exception e) {
            logger.error("Failed to copy fields from Gson result", e);
        }
    }

    private void copyField(String name, Object value) throws IllegalAccessException {
        Field targetField = findField(name);
        if (targetField != null) {
            targetField.setAccessible(true);
            targetField.set(this, value);
        }
    }

    /**
     * 查找字段（包括父类）
     */
    private Field findField(String fieldName) {
        Class<?> currentClass = this.getClass();

        while (currentClass != null && currentClass != Object.class) {
            try {
                return currentClass.getDeclaredField(fieldName);
            } catch (NoSuchFieldException e) {
                currentClass = currentClass.getSuperclass();
            }
        }

        return null;
    }

    /**
     * 纯 Gson 序列化（推荐使用）
     * 返回 JSON 字符串，完全绕过 org.json
     * 适用于所有环境（包括 Android 测试）
     */
    public String toJsonString() {
        return GsonFactory.getInstance().toJson(this);
    }

    /**
     * 纯 Gson 反序列化（推荐使用）
     * 从 JSON 字符串反序列化，完全绕过 org.json
     * 适用于所有环境（包括 Android 测试）
     */
    @SuppressWarnings("unchecked")
    public <T extends CommandRequest<?>> T fromJsonString(String jsonStr) {
        if (jsonStr == null || jsonStr.trim().isEmpty()) {
            logger.warn("Cannot deserialize from null/empty string");
            return (T) this;
        }

        try {
            T result = (T) GsonFactory.getInstance().fromJson(jsonStr, this.getClass());

            if (result != null) {
                copyFieldsFrom(result);
                return (T) this;
            } else {
                logger.warn("Gson returned null for " + this.getClass().getSimpleName());
                return (T) this;
            }
        } catch (Exception e) {
            logger.error("Failed to deserialize from string", e);
            return (T) this;
        }
    }
}
