package com.justnothing.testmodule.command.base;

import com.justnothing.testmodule.command.CommandExecutor;

public interface CommandLineParser<T extends CommandRequest> {
    T parse(CommandExecutor.CmdExecContext<? extends CommandRequest> context)
            // 放宽点算了, 真的懒得写这种猎奇的类型检查了
            // (已经被python的covariant和invariant整红温了)
            throws IllegalCommandLineArgumentException;
}
