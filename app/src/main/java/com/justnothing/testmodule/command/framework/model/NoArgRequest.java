package com.justnothing.testmodule.command.framework.model;

import com.justnothing.testmodule.command.framework.error.IllegalCommandLineArgumentException;

/**
 * "无参请求"占位类型。
 */
public class NoArgRequest extends CommandRequest<CommandResult> implements CustomCommandLineParser {

    @Override
    public CommandRequest<?> customParse(ParseContext context) throws IllegalCommandLineArgumentException {
        return this;
    }
}
