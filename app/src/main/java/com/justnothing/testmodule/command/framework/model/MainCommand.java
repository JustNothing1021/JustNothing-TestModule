package com.justnothing.testmodule.command.framework.model;

import com.justnothing.testmodule.command.framework.CommandExecutor;
import com.justnothing.testmodule.command.framework.annotation.Cmd;
import com.justnothing.testmodule.command.framework.annotation.CmdRoutes;
import com.justnothing.testmodule.command.framework.utils.CmdParamProcessor;
import com.justnothing.testmodule.utils.logging.Logger;

public abstract class MainCommand<Res extends CommandResult>
        extends AbstractCommand<CommandRequest<?> /* 接受任意类型 */, Res> {

    public static class CommandLogger extends Logger {
        private final String tag;

        public CommandLogger(String tag) {
            this.tag = tag;
        }

        @Override
        public String getTag() {
            return tag;
        }
    }

    protected CommandLogger logger;

    @SuppressWarnings("unchecked")
    protected MainCommand(String commandName, Class<Res> type) {
        // 命令根不绑定具体请求类型：类型护栏退化为"是 CommandRequest 即可"
        super(commandName, (Class<CommandRequest<?>>) (Class<?>) CommandRequest.class, type);
        logger = new CommandLogger(commandName);
    }

    /**
     * 生成帮助文本。
     * <p>
     * 默认实现：自动从 {@link Cmd} / {@link CmdRoutes}
     * 注解生成完整的帮助文档，包括：
     * <ul>
     *   <li>命令描述（来自 @Cmd.description）</li>
     *   <li>子命令列表 + 签名 + 描述</li>
     *   <li>参数说明（来自 @CmdParam）</li>
     * </ul>
     * <p>
     * 子类可以覆盖此方法以提供自定义帮助文本。
     */
    @Override
    public String getHelpText() {
        Cmd cmdAnnotation = getClass().getAnnotation(Cmd.class);
        if (cmdAnnotation != null) {
            try {
                return CmdParamProcessor.generateHelpText(getClass());
            } catch (Exception ignored) {
            }
        }
        return "用法: " + getCommandName() + " [args...]\n" +
               "输入 " + getCommandName() + " --help 查看详细帮助";
    }

    /**
     * 命令根的默认执行体：带 @CmdRoutes 的命令根只作为**路由与帮助的载体**，
     * 真正的执行由各路由的 handler（AbstractCommand 子类）承担；只有没有路由的命令
     * （各 demo/test）才需要覆写本方法。
     */
    @Override
    protected Res executeInternal(CommandExecutor.CmdExecContext<CommandRequest<?>> context) throws Exception {
        throw new UnsupportedOperationException(
                "命令 " + commandName + " 没有可执行的实现（它只提供路由与帮助）");
    }

    public String getCommandName() {
        return logger.getTag();
    }
}
