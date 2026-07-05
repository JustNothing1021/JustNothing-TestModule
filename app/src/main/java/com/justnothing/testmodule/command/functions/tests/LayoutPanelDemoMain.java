package com.justnothing.testmodule.command.functions.tests;

import java.util.Arrays;

import com.justnothing.richconsole.console.Group;
import com.justnothing.richconsole.console.Console;
import com.justnothing.richconsole.layout.Layout;
import com.justnothing.richconsole.panel.Panel;
import com.justnothing.richconsole.table.Table;

import com.justnothing.testmodule.command.base.MainCommand;
import com.justnothing.testmodule.command.CommandExecutor;
import com.justnothing.testmodule.command.base.protocol.CommandResult;
import com.justnothing.testmodule.command.base.protocol.CommandRequest;
import com.justnothing.testmodule.command.output.Colors;
import com.justnothing.testmodule.command.base.command.Cmd;

@Cmd(name = "layoutdemo", description = "RichConsole layout and panel demo", defaultResultType = CommandResult.class)
public class LayoutPanelDemoMain extends MainCommand<CommandResult> {
    public LayoutPanelDemoMain() { super("LayoutPanelDemo", CommandResult.class); }

    @Override
    public CommandResult runMain(CommandExecutor.CmdExecContext<CommandRequest> context) throws Exception {
        Console console = context.console();
        if (console == null) console = Console.of(cfg -> cfg.withForceTerminal(true));

        console.rule("Panel Basics");
        console.println(new Panel("A simple panel"));
        console.println(new Panel("Panel with title", "My Title"));
        console.println(Panel.of("Styled panel", cfg -> cfg
                .title("Styled").borderStyle("red").expand(false)));
        console.println();

        console.rule("Panel.fit()");
        console.println(Panel.fit("This panel fits its content width", cfg -> cfg.title("Fit Panel")));
        console.println(Panel.fit("Short text", "Compact"));
        console.println();

        console.rule("Panel Border Styles");
        String[] borderStyles = {"red", "green", "blue", "yellow", "cyan", "magenta"};
        for (String style : borderStyles) {
            console.println(Panel.fit("Panel with " + style + " border",
                    cfg -> cfg.borderStyle(style)));
        }
        console.println();

        console.rule("Panel Title & Subtitle");
        console.println(Panel.of("Content here", cfg -> cfg
                .title("Title").subtitle("Subtitle").borderStyle("cyan")));
        console.println(Panel.of("Left title", cfg -> cfg
                .title("Left Title").titleAlign("left").borderStyle("green")));
        console.println(Panel.of("Right title", cfg -> cfg
                .title("Right Title").titleAlign("right").borderStyle("yellow")));
        console.println();

        console.rule("Nested Panels");
        Panel inner = Panel.of("Inner content", cfg -> cfg
                .title("Inner").borderStyle("green").expand(false));
        console.println(Panel.of(inner, cfg -> cfg
                .title("Outer").borderStyle("blue")));
        console.println();

        console.rule("Layout");
        Layout left = new Layout("Left panel\nwith some\ncontent", "Left");
        Layout right = new Layout("Right panel\nwith other\ncontent", "Right");
        Layout layout = new Layout(new Group(Arrays.asList(left, right)));
        console.println(layout);
        console.println();

        console.rule("Panel with Table");
        Table tableInPanel = Table.of(cfg -> cfg.expand(false));
        tableInPanel.addColumn("Key");
        tableInPanel.addColumn("Value");
        tableInPanel.addRow("CPU", "85%");
        tableInPanel.addRow("Memory", "4.2 GB");
        tableInPanel.addRow("Disk", "120 GB");
        console.println(Panel.fit(tableInPanel, "System Info"));
        console.println();

        return createSuccessResult("完成");
    }
}
