package com.justnothing.testmodule.command.base;

public interface Command {
    void execute(CommandContext context);
    String getHelpText();
}
