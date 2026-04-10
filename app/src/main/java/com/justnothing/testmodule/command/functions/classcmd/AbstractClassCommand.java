package com.justnothing.testmodule.command.functions.classcmd;

import com.justnothing.testmodule.command.base.AbstractCommand;
import com.justnothing.testmodule.command.base.CommandContext;

public abstract class AbstractClassCommand extends AbstractCommand implements ClassCommand {

    protected AbstractClassCommand(String commandName) {
        super(commandName);
    }

    @Override
    protected void executeInternal(CommandContext context) throws Exception {
        if (!(context instanceof ClassCommandContext)) {
            throw new IllegalArgumentException("需要 ClassCommandContext");
        }
        executeInternal((ClassCommandContext) context);
    }

    protected abstract void executeInternal(ClassCommandContext context) throws Exception;


}
