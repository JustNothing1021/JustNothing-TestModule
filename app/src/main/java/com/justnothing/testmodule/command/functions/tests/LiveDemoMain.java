package com.justnothing.testmodule.command.functions.tests;

import com.justnothing.richconsole.console.Console;
import com.justnothing.richconsole.live.Live;
import com.justnothing.richconsole.panel.Panel;
import com.justnothing.richconsole.status.Status;
import com.justnothing.richconsole.table.Table;

import com.justnothing.testmodule.command.base.MainCommand;
import com.justnothing.testmodule.command.CommandExecutor;
import com.justnothing.testmodule.command.base.protocol.CommandResult;
import com.justnothing.testmodule.command.base.protocol.CommandRequest;
import com.justnothing.testmodule.command.output.Colors;
import com.justnothing.testmodule.command.base.command.Cmd;

@Cmd(name = "livedemo", description = "RichConsole live demo", defaultResultType = CommandResult.class)
public class LiveDemoMain extends MainCommand<CommandResult> {
    public LiveDemoMain() { super("LiveDemo", CommandResult.class); }

    @Override
    public CommandResult runMain(CommandExecutor.CmdExecContext<CommandRequest> context) throws Exception {
        Console console = context.console();
        if (console == null) console = Console.of(cfg -> cfg.withForceTerminal(true));

        console.rule("Logging");
        console.log("Starting application...");
        console.log("Loading [bold]configuration[/bold]");
        console.log("Connecting to database");
        console.log("[green]All systems operational[/green]");
        console.println();

        console.rule("Status");
        try (Status status = console.status("Initializing...")) {
            Thread.sleep(1000);
            status.update("[green]Loading modules...[/green]");
            Thread.sleep(1000);
            status.update("[yellow]Compiling sources...[/yellow]");
            Thread.sleep(1000);
            status.update("[cyan]Linking binaries...[/cyan]");
            Thread.sleep(1000);
            status.update("[bold green]Build complete![/bold green]");
            Thread.sleep(500);
        }
        console.println();

        console.rule("Log + Print");
        console.log("Before panel");
        console.println(Panel.of("This panel appears between log entries", cfg -> cfg
                .title("Info").borderStyle("green")));
        console.log("After panel");
        console.println();

        console.rule("Live Refresh");
        Table initialStats = Table.of(cfg -> cfg.title("System Stats").expand(false));
        initialStats.addColumn("Metric", "cyan", null);
        initialStats.addColumn("Value", "green", "right");
        initialStats.addRow("CPU", "20%");
        initialStats.addRow("Memory", "30 MB");
        initialStats.addRow("Requests", "0");
        initialStats.addRow("Uptime", "0s");

        try (Live live = new Live(initialStats, console)) {
            live.start();
            for (int i = 1; i <= 10; i++) {
                Table stats = Table.of(cfg -> cfg.title("System Stats").expand(false));
                stats.addColumn("Metric", "cyan", null);
                stats.addColumn("Value", "green", "right");
                stats.addRow("CPU", (20 + i * 5) + "%");
                stats.addRow("Memory", (30 + i * 3) + " MB");
                stats.addRow("Requests", String.valueOf(i * 100));
                stats.addRow("Uptime", i + "s");
                live.update(stats);
                Thread.sleep(300);
            }
        }
        console.println();

        console.rule("Convenience APIs");
        console.rule("Styled rule", cfg -> cfg.style("red").align("center"));
        console.out("Output via console.out()");
        console.println();
        console.log("Timestamped log entry");
        console.println();

        console.rule("Exception Display");
        try {
            int result = 10 / 0;
        } catch (ArithmeticException e) {
            console.printException(e);
        }
        console.println();

        return createSuccessResult("完成");
    }
}
