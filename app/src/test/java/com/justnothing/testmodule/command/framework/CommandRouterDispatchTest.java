package com.justnothing.testmodule.command.framework;

import com.justnothing.testmodule.command.framework.annotation.Cmd;
import com.justnothing.testmodule.command.framework.annotation.CmdRoutes;
import com.justnothing.testmodule.command.framework.model.CommandRequest;
import com.justnothing.testmodule.command.framework.model.CommandResult;
import com.justnothing.testmodule.command.framework.model.CommandRouter;
import com.justnothing.testmodule.command.framework.model.MainCommand;
import com.justnothing.testmodule.command.framework.output.ClientRequirements;
import com.justnothing.testmodule.command.framework.output.MockOutputHandler;

import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * 验证统一的命令执行入口 {@link CommandRouter#dispatch}：
 * 命中路由走路由处理器，无路由定义的命令在 dispatch 内部回退 executeWithResult()，
 * 命令注册了路由但子命令不匹配时抛 IllegalArgumentException（供上层展示帮助）。
 */
public class CommandRouterDispatchTest {

    @BeforeClass
    public static void registerTestCommands() {
        CommandRouter.getInstance().registerCommand(RouteLessCommand.class);
        CommandRouter.getInstance().registerCommand(RoutedCommand.class);
    }

    private static CommandExecutor.CmdExecContext<CommandRequest<?>> context(String cmdName, String... args) {
        return new CommandExecutor.CmdExecContext<>(
                cmdName, args,
                null, CommandRouterDispatchTest.class.getClassLoader(),
                new MockOutputHandler(), new ClientRequirements(false, false));
    }

    /** 无 @CmdRoutes 的命令：应由 dispatch 内部回退 executeWithResult()，而不是抛「未找到匹配的路由」 */
    @Test
    public void testRouteLessCommandFallsBackToExecute() throws Throwable {
        CommandResult result = CommandRouter.getInstance().dispatch(context("test-route-less"));

        assertNotNull("无路由命令应执行成功", result);
        assertEquals("route-less-ran", result.getMessage());
    }

    /** 命中路由 → 交给路由处理器执行 */
    @Test
    public void testRoutedCommandMatchesRoute() throws Throwable {
        CommandResult result = CommandRouter.getInstance().dispatch(context("test-routed", "ping"));

        assertNotNull("命中路由应执行成功", result);
        assertEquals("routed-ran", result.getMessage());
    }

    /** 已注册路由但子命令不匹配（如拼写错误）→ IllegalArgumentException，由上层决定展示帮助 */
    @Test
    public void testRoutedCommandWithUnknownSubCommandThrows() {
        try {
            CommandRouter.getInstance().dispatch(context("test-routed", "nope"));
            fail("子命令不匹配应抛 IllegalArgumentException");
        } catch (IllegalArgumentException expected) {
            assertTrue("错误信息应说明未匹配到路由, 实际: " + expected.getMessage(),
                    expected.getMessage().contains("未找到匹配的路由"));
        } catch (Throwable t) {
            fail("应抛 IllegalArgumentException, 实际: " + t.getClass().getName());
        }
    }

    /** 未注册的命令 → IllegalArgumentException */
    @Test
    public void testUnknownCommandThrows() {
        try {
            CommandRouter.getInstance().dispatch(context("test-not-registered"));
            fail("未注册命令应抛 IllegalArgumentException");
        } catch (IllegalArgumentException expected) {
            assertTrue("错误信息应提示未知命令, 实际: " + expected.getMessage(),
                    expected.getMessage().contains("未知的命令"));
        } catch (Throwable t) {
            fail("应抛 IllegalArgumentException, 实际: " + t.getClass().getName());
        }
    }

    // ==================== 测试用命令 ====================

    /** 只有 @Cmd、没有 @CmdRoutes：走 dispatch 内部的 executeWithResult() 回退 */
    @Cmd(name = "test-route-less", description = "无路由测试命令")
    public static class RouteLessCommand extends MainCommand<CommandResult> {
        public RouteLessCommand() {
            super("test-route-less", CommandResult.class);
        }

        @Override
        protected CommandResult executeInternal(CommandExecutor.CmdExecContext<CommandRequest<?>> ctx) {
            CommandResult result = new CommandResult();
            result.setMessage("route-less-ran");
            return result;
        }
    }

    public static class RoutedRequest extends CommandRequest<CommandResult> {
    }

    /** 带一条路由的测试命令 */
    @Cmd(name = "test-routed", description = "有路由测试命令")
    @CmdRoutes(@CmdRoutes.Route(path = "ping", request = RoutedRequest.class,
            handler = RoutedCommand.class, description = "ping 子命令"))
    public static class RoutedCommand extends MainCommand<CommandResult> {
        public RoutedCommand() {
            super("test-routed", CommandResult.class);
        }

        @Override
        protected CommandResult executeInternal(CommandExecutor.CmdExecContext<CommandRequest<?>> ctx) {
            CommandResult result = new CommandResult();
            result.setMessage("routed-ran");
            return result;
        }
    }
}
