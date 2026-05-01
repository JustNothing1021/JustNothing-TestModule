package com.justnothing.testmodule.command.functions.classcmd.parser;

import com.justnothing.testmodule.command.CommandExecutor;
import com.justnothing.testmodule.command.base.CommandLineParser;
import com.justnothing.testmodule.command.base.CommandRequest;
import com.justnothing.testmodule.command.functions.classcmd.ClassCommandRequest;
import com.justnothing.testmodule.command.functions.classcmd.request.*;
import com.justnothing.testmodule.utils.logging.Logger;

import java.util.Arrays;

public class ClassCommandParser implements CommandLineParser<ClassCommandRequest> {
    private static final Logger logger = Logger.getLoggerForName("ClassCommandParser");

    @Override
    public ClassCommandRequest parse(CommandExecutor.CmdExecContext<? extends CommandRequest> context) {
        String[] args = context.args();

        if (args.length < 1) {
            logger.warn("参数不足，需要至少1个参数");
            return null;
        }

        String subCommand = args[0];
        String[] remainingArgs = Arrays.copyOfRange(args, 1, args.length);

        return switch (subCommand) {
            case "info" -> new ClassInfoRequest().fromCommandLine(remainingArgs);
            case "list" -> new MethodListRequest().fromCommandLine(remainingArgs);
            case "field" -> new GetFieldValueRequest().fromCommandLine(remainingArgs);
            case "constructor" -> new InvokeConstructorRequest().fromCommandLine(remainingArgs);
            case "invoke" -> new InvokeMethodRequest().fromCommandLine(remainingArgs);
            case "search" -> new SearchClassRequest().fromCommandLine(remainingArgs);
            case "analyze" -> new AnalyzeClassRequest().fromCommandLine(remainingArgs);
            case "reflect" -> new ReflectClassRequest().fromCommandLine(remainingArgs);
            case "graph" -> new ClassGraphRequest().fromCommandLine(remainingArgs);
            default -> null;
        };
    }
}
