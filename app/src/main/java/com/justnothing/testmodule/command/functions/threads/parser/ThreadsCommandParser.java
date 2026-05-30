package com.justnothing.testmodule.command.functions.threads.parser;

import com.justnothing.testmodule.command.base.command.CommandLineParser;
import com.justnothing.testmodule.command.base.protocol.CommandRequest;
import com.justnothing.testmodule.command.CommandExecutor;
import com.justnothing.testmodule.command.functions.threads.request.ThreadListRequest;
import com.justnothing.testmodule.command.functions.threads.request.ThreadDeadlockRequest;
import com.justnothing.testmodule.command.functions.threads.request.ThreadProfileStartRequest;
import com.justnothing.testmodule.command.functions.threads.request.ThreadProfileStopRequest;
import com.justnothing.testmodule.command.functions.threads.request.ThreadProfileShowRequest;
import com.justnothing.testmodule.command.functions.threads.request.ThreadProfileExportRequest;

import java.util.Arrays;

public class ThreadsCommandParser implements CommandLineParser<CommandRequest> {

    @Override
    public CommandRequest parse(CommandExecutor.CmdExecContext<? extends CommandRequest> context) {
        String[] args = context.args();

        if (args.length < 1) {
            return new ThreadListRequest();
        }

        String subCommand = args[0];

        if ("profile".equals(subCommand)) {
            if (args.length < 2) {
                return new ThreadProfileStartRequest();
            }
            
            String profileSubCmd = args[1];
            String[] remainingArgs = Arrays.copyOfRange(args, 2, args.length);
            
            return switch (profileSubCmd) {
                case "start" -> new ThreadProfileStartRequest().fromCommandLine(remainingArgs);
                case "stop" -> new ThreadProfileStopRequest().fromCommandLine(remainingArgs);
                case "show" -> new ThreadProfileShowRequest().fromCommandLine(remainingArgs);
                case "export" -> new ThreadProfileExportRequest().fromCommandLine(remainingArgs);
                default -> null;
            };
        }

        String[] remainingArgs = Arrays.copyOfRange(args, 1, args.length);

        return switch (subCommand) {
            case "list" -> new ThreadListRequest().fromCommandLine(remainingArgs);
            case "deadlock" -> new ThreadDeadlockRequest().fromCommandLine(remainingArgs);
            default -> null;
        };
    }
}
