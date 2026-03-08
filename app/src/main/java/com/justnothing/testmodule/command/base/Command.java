package com.justnothing.testmodule.command.base;

public interface Command {
    String execute(CommandContext context);
    String getHelpText();
}
