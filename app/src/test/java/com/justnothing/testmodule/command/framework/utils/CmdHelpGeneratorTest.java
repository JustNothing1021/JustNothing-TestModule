package com.justnothing.testmodule.command.framework.utils;

import com.justnothing.testmodule.command.framework.CommandExecutor;
import com.justnothing.testmodule.command.framework.annotation.Cmd;
import com.justnothing.testmodule.command.framework.annotation.CmdParam;
import com.justnothing.testmodule.command.framework.annotation.CmdRoutes;
import com.justnothing.testmodule.command.framework.i18n.CliMessages;
import com.justnothing.testmodule.command.framework.model.CommandRequest;
import com.justnothing.testmodule.command.framework.model.CommandResult;
import com.justnothing.testmodule.command.framework.model.MainCommand;

import org.junit.Test;

import java.util.function.Supplier;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * 帮助文本「参数详情」的排版守卫。
 *
 * <p>那一节是"名称 / 描述 / 属性"三列拼出来的，靠补空格对齐；而补空格在内容本身就超宽时
 * 一格都不给，两个词就粘成一个（{@code Number of rows in the CPU TOP listoptional}）。
 * 英文描述普遍比中文长，所以这个坑是 i18n 之后才露出来的。</p>
 *
 * <p>断言固定用英文：中文描述短，粘不上，测不出这个 bug。</p>
 */
public class CmdHelpGeneratorTest {

    /** 描述超过描述列宽（30 显示列）：正是会粘住的那种。 */
    @Test
    public void longDescriptionDoesNotGlueToAttributes() {
        String help = inEnglish(() -> CmdHelpGenerator.generateHelpText(LongDescriptionCommand.class));
        assertTrue("参数详情里应当有属性列，否则这条测试什么也没守到:\n" + help,
                help.contains("optional"));

        for (String line : help.split("\n")) {
            assertFalse("描述和属性粘在一起了: " + line,
                    line.matches(".*\\S(optional|required).*"));
        }
    }

    /** 短描述按列对齐（补空格到列宽），同样不允许出现"零空格"。 */
    @Test
    public void shortDescriptionKeepsColumnAlignment() {
        String help = inEnglish(() -> CmdHelpGenerator.generateHelpText(ShortDescriptionCommand.class));

        String detailLine = null;
        for (String line : help.split("\n")) {
            if (line.contains("Sampling interval")) {
                detailLine = line;
            }
        }
        assertTrue("找不到参数详情行:\n" + help, detailLine != null);
        assertTrue("描述和属性之间应当有对齐用的空格: " + detailLine,
                detailLine.matches(".*Sampling interval in ms\\s+optional.*"));
    }

    /** 语言是 ThreadLocal，用完要还回去，免得影响同一线程上的其它测试。 */
    private static String inEnglish(Supplier<String> action) {
        CliMessages.useLanguage("en");
        try {
            return action.get();
        } finally {
            CliMessages.clearLanguage();
        }
    }

    // ==================== 测试用命令 ====================

    public static class LongDescriptionRequest extends CommandRequest<CommandResult> {
        @CmdParam(name = "top", aliases = {"-n", "--top"}, defaultValue = "5",
                description = "Number of rows in the CPU TOP list")
        public int top;
    }

    public static class ShortDescriptionRequest extends CommandRequest<CommandResult> {
        @CmdParam(name = "interval", aliases = {"-i", "--interval"}, defaultValue = "1000",
                description = "Sampling interval in ms")
        public int interval;
    }

    @Cmd(name = "help-format-long", description = "排版测试命令")
    @CmdRoutes(@CmdRoutes.Route(path = "help-format-long/run", request = LongDescriptionRequest.class,
            handler = LongDescriptionCommand.class, description = "长描述子命令"))
    public static class LongDescriptionCommand extends MainCommand<CommandResult> {
        public LongDescriptionCommand() {
            super("help-format-long", CommandResult.class);
        }

        @Override
        protected CommandResult executeInternal(CommandExecutor.CmdExecContext<CommandRequest<?>> ctx) {
            return new CommandResult();
        }
    }

    @Cmd(name = "help-format-short", description = "排版测试命令")
    @CmdRoutes(@CmdRoutes.Route(path = "help-format-short/run", request = ShortDescriptionRequest.class,
            handler = ShortDescriptionCommand.class, description = "短描述子命令"))
    public static class ShortDescriptionCommand extends MainCommand<CommandResult> {
        public ShortDescriptionCommand() {
            super("help-format-short", CommandResult.class);
        }

        @Override
        protected CommandResult executeInternal(CommandExecutor.CmdExecContext<CommandRequest<?>> ctx) {
            return new CommandResult();
        }
    }
}
