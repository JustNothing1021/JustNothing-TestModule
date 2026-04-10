package com.justnothing.testmodule.command.functions.intercept;

public enum TaskType {
    WATCH("watch", "监控字段或方法的变化"),
    BREAKPOINT("breakpoint", "设置和管理断点"),
    TRACE("trace", "跟踪方法调用链"),
    HOOK("hook", "动态Hook注入器"),
    PERFORMANCE("performance", "性能分析");

    private final String commandName;
    private final String description;

    TaskType(String commandName, String description) {
        this.commandName = commandName;
        this.description = description;
    }

    public String getCommandName() {
        return commandName;
    }

    public String getDescription() {
        return description;
    }
}
