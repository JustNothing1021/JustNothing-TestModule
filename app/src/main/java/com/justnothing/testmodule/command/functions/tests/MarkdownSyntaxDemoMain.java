package com.justnothing.testmodule.command.functions.tests;

import com.justnothing.richconsole.console.Console;
import com.justnothing.richconsole.markdown.Markdown;
import com.justnothing.richconsole.syntax.Syntax;

import com.justnothing.testmodule.command.framework.base.MainCommand;
import com.justnothing.testmodule.command.framework.CommandExecutor;
import com.justnothing.testmodule.command.framework.base.protocol.CommandResult;
import com.justnothing.testmodule.command.framework.base.protocol.CommandRequest;
import com.justnothing.testmodule.command.framework.base.command.Cmd;

@Cmd(name = "syntaxdemo", description = "RichConsole markdown and syntax highlighting demo", defaultResultType = CommandResult.class)
public class MarkdownSyntaxDemoMain extends MainCommand<CommandResult> {
    public MarkdownSyntaxDemoMain() { super("MarkdownSyntaxDemo", CommandResult.class); }

    @Override
    public CommandResult runMain(CommandExecutor.CmdExecContext<CommandRequest> context) throws Exception {
        Console console = context.console();
        if (console == null) console = Console.of(cfg -> cfg.withForceTerminal(true));

        console.rule("Markdown");
        String mdText = """
                # RichConsole
                
                A **Java** port of Python's [Rich](https://github.com/Textualize/rich) library.
                
                ## Features
                
                - **Tables** with multiple box styles
                - **Progress bars** with spinners and ETA
                - **Syntax highlighting** for Java, Python, JSON
                - **Markdown** rendering
                - *Tree* visualization
                - `Code` blocks with line numbers
                
                ## Quick Start
                
                ```java
                Console console = Console.of(cfg -> {});
                console.println(new Panel("Hello, World!"));
                ```
                
                > "Simplicity is the ultimate sophistication." — Leonardo da Vinci
                
                1. First item
                2. Second item
                3. Third item
                """;
        console.println(new Markdown(mdText));
        console.println();

        console.rule("Java Syntax");
        String javaCode = """
                import io.github.richconsole.console.Console;
                import io.github.richconsole.panel.Panel;
                
                /**
                 * Example application demonstrating RichConsole.
                 */
                public class Example {
                    private static final String GREETING = "Hello, World!";
                    
                    public static void main(String[] args) {
                        Console console = Console.of(cfg -> {});
                        console.println(new Panel(GREETING, "Welcome"));
                        
                        for (int i = 0; i < 10; i++) {
                            System.out.println("Count: " + i);
                        }
                    }
                }
                """;
        console.println(new Syntax(javaCode, "java"));
        console.println();

        console.rule("Python Syntax");
        String pythonCode = """
                from rich.console import Console
                from rich.table import Table
                
                def main():
                    console = Console()
                    table = Table(title="Star Wars Movies")
                    table.add_column("Title", style="cyan")
                    table.add_column("Year", justify="right")
                    console.print(table)
                
                if __name__ == "__main__":
                    main()
                """;
        console.println(new Syntax(pythonCode, "python"));
        console.println();

        console.rule("JSON Syntax");
        String jsonCode = """
                {
                  "name": "RichConsole",
                  "version": "1.0.0",
                  "description": "Java port of Python Rich",
                  "features": [
                    "Tables",
                    "Progress",
                    "Syntax",
                    "Markdown"
                  ],
                  "author": {
                    "name": "Developer",
                    "url": "https://github.com/example"
                  }
                }
                """;
        console.println(new Syntax(jsonCode, "json"));
        console.println();

        return createSuccessResult("完成");
    }
}
