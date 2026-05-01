package com.justnothing.testmodule.command.functions.classcmd;

import com.justnothing.testmodule.command.functions.classcmd.impl.AnalyzeCommand;
import com.justnothing.testmodule.command.functions.classcmd.impl.ConstructorCommand;
import com.justnothing.testmodule.command.functions.classcmd.impl.FieldCommand;
import com.justnothing.testmodule.command.functions.classcmd.impl.GraphCommand;
import com.justnothing.testmodule.command.functions.classcmd.impl.InfoCommand;
import com.justnothing.testmodule.command.functions.classcmd.impl.InvokeCommand;
import com.justnothing.testmodule.command.functions.classcmd.impl.ListCommand;
import com.justnothing.testmodule.command.functions.classcmd.impl.ReflectCommand;
import com.justnothing.testmodule.command.functions.classcmd.impl.SearchCommand;

import java.util.HashMap;
import java.util.Map;

public class ClassCommandRegistry {
    private static final Map<String, ClassCommand<?, ?>> commands = new HashMap<>();

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

    private static void registerCommand(String name, ClassCommand<?, ?> command) {
        commands.put(name, command);
    }

    public static ClassCommand<?, ?> getCommand(String name) {
        return commands.get(name);
    }

}
