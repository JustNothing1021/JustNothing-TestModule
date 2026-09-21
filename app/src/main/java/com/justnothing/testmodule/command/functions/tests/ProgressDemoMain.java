package com.justnothing.testmodule.command.functions.tests;

import com.justnothing.richconsole.console.Console;
import com.justnothing.richconsole.progress.Progress;
import com.justnothing.richconsole.status.Status;

import com.justnothing.testmodule.command.framework.model.MainCommand;
import com.justnothing.testmodule.command.framework.CommandExecutor;
import com.justnothing.testmodule.command.framework.i18n.Text;
import com.justnothing.testmodule.command.framework.model.CommandResult;
import com.justnothing.testmodule.command.framework.model.CommandRequest;
import com.justnothing.testmodule.command.framework.annotation.Cmd;

@Cmd(name = "progressdemo", description = "RichConsole progress demo")
public class ProgressDemoMain extends MainCommand<CommandResult> {
    public ProgressDemoMain() { super("ProgressDemo", CommandResult.class); }

    @Override
    protected CommandResult executeInternal(CommandExecutor.CmdExecContext<CommandRequest<?>> context) throws Exception {
        Console console = context.console();
        if (console == null) console = Console.of(cfg -> cfg.withForceTerminal(true));

        console.rule("Single Progress Bar");
        try (Progress progress = console.progress()) {
            int task = progress.addTask("Processing", 100);
            progress.start();
            while (!progress.getTask(task).isFinished()) {
                Thread.sleep(60);
                progress.advance(task, 1);
            }
        }
        console.println();

        console.rule("Multiple Progress Bars");
        try (Progress progress = console.progress()) {
            int task1 = progress.addTask("Downloading", 200);
            int task2 = progress.addTask("Extracting", 150);
            int task3 = progress.addTask("Installing", 100);
            progress.start();
            while (!progress.isFinished()) {
                Thread.sleep(40);
                if (!progress.getTask(task1).isFinished()) progress.advance(task1, 2);
                if (!progress.getTask(task2).isFinished()) progress.advance(task2, 1.5);
                if (!progress.getTask(task3).isFinished()) progress.advance(task3, 1);
            }
        }
        console.println();

        console.rule("Indeterminate Progress");
        try (Progress progress = console.progress()) {
            int task = progress.addTask("Waiting for response...", 0);
            progress.start();
            Thread.sleep(3000);
            progress.update(task, 100.0, 100.0, null, null, null, false, null);
        }
        console.println();

        console.rule("Status Spinner");
        try (Status status = console.status("Loading database...")) {
            Thread.sleep(1500);
            status.update("Compiling shaders...");
            Thread.sleep(1500);
            status.update("Ready!");
            Thread.sleep(500);
        }
        console.println();

        console.rule("Progress with Logging");
        try (Progress progress = console.progress()) {
            int task = progress.addTask("Deploying", 10);
            progress.start();
            String[] steps = {"Build", "Test", "Package", "Upload", "Verify", "Configure", "Migrate", "Seed", "Restart", "Done!"};
            for (int i = 0; i < steps.length; i++) {
                progress.advance(task, 1);
                console.log("[green]" + steps[i] + "[/green] completed");
                Thread.sleep(300);
            }
        }
        console.println();

        return createSuccessResult(Text.zhEn("完成", "Done").text());
    }
}
