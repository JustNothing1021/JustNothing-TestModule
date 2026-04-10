package com.justnothing.testmodule.command.functions.classcmd;

import java.util.HashMap;
import java.util.Map;

public class ClassCommandRegistry {
    private static final Map<String, ClassCommand> commands = new HashMap<>();

    static {
        registerCommand("info", new InfoCommand());
        registerCommand("graph", new GraphCommand());
        registerCommand("analyze", new AnalyzeCommand());
        registerCommand("list", new ListCommand());
        registerCommand("invoke", new InvokeCommand());
        registerCommand("field", new FieldCommand());
        registerCommand("search", new SearchCommand());
        registerCommand("constructor", new ConstructorCommand());
        registerCommand("reflect", new ReflectCommand());
    }

    private static void registerCommand(String name, ClassCommand command) {
        commands.put(name, command);
    }

    public static ClassCommand getCommand(String name) {
        return commands.get(name);
    }

}
