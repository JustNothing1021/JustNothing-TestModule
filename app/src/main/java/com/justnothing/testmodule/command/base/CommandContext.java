package com.justnothing.testmodule.command.base;

import com.justnothing.testmodule.command.CommandExecutor;
import com.justnothing.testmodule.utils.functions.Logger;

public class CommandContext {
    private final String[] args;
    private final ClassLoader classLoader;
    private final String targetPackage;
    private final CommandExecutor.CmdExecContext execContext;
    private final Logger logger;

    public CommandContext(String[] args, ClassLoader classLoader, String targetPackage, 
                          CommandExecutor.CmdExecContext execContext, Logger logger) {
        this.args = args;
        this.classLoader = classLoader;
        this.targetPackage = targetPackage;
        this.execContext = execContext;
        this.logger = logger;
    }

    public String[] getArgs() {
        return args;
    }

    public ClassLoader getClassLoader() {
        return classLoader;
    }

    public String getTargetPackage() {
        return targetPackage;
    }

    public CommandExecutor.CmdExecContext getExecContext() {
        return execContext;
    }

    public Logger getLogger() {
        return logger;
    }
}
