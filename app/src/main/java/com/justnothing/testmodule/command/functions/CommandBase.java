package com.justnothing.testmodule.command.functions;

import com.justnothing.testmodule.command.CommandExecutor;
import com.justnothing.testmodule.utils.functions.Logger;

public abstract class CommandBase {

    public static class CommandLogger extends Logger {
        private final String tag;

        public CommandLogger(String tag) {
            this.tag = tag;
        }

        @Override
        public String getTag() {
            return tag;
        }
    }

    protected static CommandLogger logger;

    public CommandBase(String commandName) {
        logger = new CommandLogger(commandName);
    }

    public abstract String getHelpText();

    public abstract String runMain(CommandExecutor.CmdExecContext context);

    public String getCommandName() {
        return logger.getTag();
    }
}
