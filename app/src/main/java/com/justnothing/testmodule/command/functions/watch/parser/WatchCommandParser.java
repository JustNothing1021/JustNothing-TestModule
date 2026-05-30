package com.justnothing.testmodule.command.functions.watch.parser;

import com.justnothing.testmodule.command.base.command.CommandLineParser;
import com.justnothing.testmodule.command.base.protocol.CommandRequest;
import com.justnothing.testmodule.command.CommandExecutor;
import com.justnothing.testmodule.command.functions.watch.request.WatchAddRequest;
import com.justnothing.testmodule.command.functions.watch.request.WatchClearRequest;
import com.justnothing.testmodule.command.functions.watch.request.WatchListRequest;
import com.justnothing.testmodule.command.functions.watch.request.WatchOutputRequest;
import com.justnothing.testmodule.command.functions.watch.request.WatchStopRequest;

import java.util.Arrays;

public class WatchCommandParser implements CommandLineParser<CommandRequest> {

    @Override
    public CommandRequest parse(CommandExecutor.CmdExecContext<? extends CommandRequest> context) {
        String[] args = context.args();

        if (args.length < 1) {
            return new WatchListRequest();
        }

        String subCommand = args[0];
        String[] remainingArgs = Arrays.copyOfRange(args, 1, args.length);

        return switch (subCommand) {
            case "add" -> new WatchAddRequest().fromCommandLine(remainingArgs);
            case "list" -> new WatchListRequest().fromCommandLine(remainingArgs);
            case "stop" -> new WatchStopRequest().fromCommandLine(remainingArgs);
            case "clear" -> new WatchClearRequest().fromCommandLine(remainingArgs);
            case "output" -> new WatchOutputRequest().fromCommandLine(remainingArgs);
            default -> null;
        };
    }
}
