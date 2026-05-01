package com.justnothing.testmodule.command.agent;

import com.justnothing.testmodule.utils.logging.Logger;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class AgentCommandRouter {

    private static final Logger logger = Logger.getLoggerForName("AgentCommandRouter");

    private static final Map<String, AgentCommandHandler<?>> handlers = new ConcurrentHashMap<>();

    public static void register(AgentCommandHandler<?> handler) {
        if (handler == null || handler.getCommandType() == null) return;
        handlers.put(handler.getCommandType(), handler);
        logger.debug("注册 Agent 命令: " + handler.getCommandType());
    }

    public static void unregister(String commandType) {
        if (commandType != null) handlers.remove(commandType);
    }

    public static AgentCommandHandler<?> getHandler(String commandType) {
        return commandType != null ? handlers.get(commandType) : null;
    }

    public static Set<String> getAvailableCommands() {
        return Collections.unmodifiableSet(handlers.keySet());
    }

    public static int getRegisteredCount() {
        return handlers.size();
    }

    public static void clear() {
        handlers.clear();
    }
}
