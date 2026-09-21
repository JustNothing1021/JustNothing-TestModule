package com.justnothing.testmodule.command.functions.intercept.base;

import com.justnothing.testmodule.command.framework.i18n.Text;

public enum TaskType {
    WATCH("watch", Text.zhEn("监控字段或方法的变化", "Monitor field or method changes")),
    BREAKPOINT("breakpoint", Text.zhEn("设置和管理断点", "Set up and manage breakpoints")),
    TRACE("trace", Text.zhEn("跟踪方法调用链", "Trace method call chains")),
    HOOK("hook", Text.zhEn("动态Hook注入器", "Dynamic hook injector")),
    PERFORMANCE("performance", Text.zhEn("性能分析", "Performance analysis"));

    private final String commandName;
    private final Text description;

    TaskType(String commandName, Text description) {
        this.commandName = commandName;
        this.description = description;
    }

    public String getCommandName() {
        return commandName;
    }

    public String getDescription() {
        return description.text();
    }
}
