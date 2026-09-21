package com.justnothing.testmodule.command.functions.tests;

import com.justnothing.richconsole.console.Console;
import com.justnothing.richconsole.tree.Tree;
import com.justnothing.richconsole.tree.Tree.TreeNode;

import com.justnothing.testmodule.command.framework.model.MainCommand;
import com.justnothing.testmodule.command.framework.CommandExecutor;
import com.justnothing.testmodule.command.framework.i18n.Text;
import com.justnothing.testmodule.command.framework.model.CommandResult;
import com.justnothing.testmodule.command.framework.model.CommandRequest;
import com.justnothing.testmodule.command.framework.annotation.Cmd;

@Cmd(name = "treedemo", description = "RichConsole tree demo")
public class TreeDemoMain extends MainCommand<CommandResult> {
    public TreeDemoMain() { super("TreeDemo", CommandResult.class); }

    @Override
    protected CommandResult executeInternal(CommandExecutor.CmdExecContext<CommandRequest<?>> context) throws Exception {
        Console console = context.console();
        if (console == null) console = Console.of(cfg -> cfg.withForceTerminal(true));

        console.rule("Basic Tree");
        Tree root = new Tree("RichConsole");
        root.add("Console");
        root.add("Panel");
        root.add("Table");
        TreeNode progress = root.add("Progress");
        progress.add("ProgressBar");
        progress.add("Spinner");
        TreeNode text = root.add("Text");
        text.add("Span");
        text.add("Markup");
        console.println(root);
        console.println();

        console.rule("File System Tree");
        Tree fs = new Tree("src/");
        TreeNode mainDir = fs.add("main/");
        TreeNode java = mainDir.add("java/");
        java.add("Console.java");
        java.add("Panel.java");
        java.add("Table.java");
        TreeNode resources = mainDir.add("resources/");
        resources.add("styles.css");
        TreeNode test = fs.add("test/");
        TreeNode testJava = test.add("java/");
        testJava.add("ConsoleTest.java");
        testJava.add("PanelTest.java");
        console.println(fs);
        console.println();

        console.rule("Styled Tree");
        Tree styled = new Tree("[bold magenta]Project[/]");
        styled.add("[cyan]README.md[/]");
        styled.add("[green].gitignore[/]");
        TreeNode src = styled.add("[yellow]src/[/]");
        src.add("[blue]main/[/]");
        src.add("[red]test/[/]");
        styled.add("[dim]build.gradle[/]");
        styled.add("[dim]settings.gradle[/]");
        console.println(styled);
        console.println();

        return createSuccessResult(Text.zhEn("完成", "Done").text());
    }
}
