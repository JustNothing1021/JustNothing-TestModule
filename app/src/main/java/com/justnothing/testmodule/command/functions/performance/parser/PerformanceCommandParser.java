package com.justnothing.testmodule.command.functions.performance.parser;

import com.justnothing.testmodule.command.CommandExecutor;
import com.justnothing.testmodule.command.base.command.CommandLineParser;
import com.justnothing.testmodule.command.base.protocol.CommandRequest;
import com.justnothing.testmodule.command.functions.performance.request.*;

import java.util.Arrays;

public class PerformanceCommandParser implements CommandLineParser<CommandRequest> {

    @Override
    public CommandRequest parse(CommandExecutor.CmdExecContext<? extends CommandRequest> context) {
        String[] args = context.args();

        if (args.length < 1) {
            return null;
        }

        String subCommand = args[0];
        String[] remainingArgs = Arrays.copyOfRange(args, 1, args.length);

        return switch (subCommand) {
            case "sample" -> new SampleRequest().fromCommandLine(remainingArgs);
            case "multithread" -> new MultiThreadRequest().fromCommandLine(remainingArgs);
            case "hierarchical" -> new HierarchicalRequest().fromCommandLine(remainingArgs);
            case "trace" -> new TraceRequest().fromCommandLine(remainingArgs);
            case "systrace" -> new SystraceRequest().fromCommandLine(remainingArgs);
            case "hook" -> new PerfHookRequest().fromCommandLine(remainingArgs);
            default -> null;
        };
    }
}
