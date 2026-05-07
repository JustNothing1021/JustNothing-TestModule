package com.justnothing.testmodule.command.functions.alias.impl;

import java.util.HashMap;
import java.util.Map;

public class AliasCommandRegistry {

    private static final Map<String, Object> commands = new HashMap<>();

    static {
        register("list", new AliasListCommand());
        register("add", new AliasAddCommand());
        register("remove", new AliasRemoveCommand());
        register("clear", new AliasClearCommand());
    }

    private static void register(String name, Object command) {
        commands.put(name, command);
    }

    public static Object getCommand(String name) {
        return commands.get(name);
    }
}
