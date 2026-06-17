package com.justnothing.testmodule.command.base;

import com.justnothing.testmodule.command.CommandExecutor;
import com.justnothing.testmodule.command.base.command.Cmd;
import com.justnothing.testmodule.command.base.protocol.CommandRequest;
import com.justnothing.testmodule.command.base.protocol.CommandResult;
import com.justnothing.testmodule.command.utils.CmdParamProcessor;
import com.justnothing.testmodule.utils.logging.Logger;

@SuppressWarnings("rawtypes")
public abstract class MainCommand<Res extends CommandResult> {

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

    protected final Class<Res> responseType;

    protected CommandLogger logger;

    public MainCommand(String commandName, Class<Res> type) {
        logger = new CommandLogger(commandName);
        this.responseType = type;
    }

    /**
     * 生成帮助文本。
     * <p>
     * 默认实现：自动从 {@link Cmd} / {@link com.justnothing.testmodule.command.base.command.CmdRoutes}
     * 注解生成完整的帮助文档，包括：
     * <ul>
     *   <li>命令描述（来自 @Cmd.description）</li>
     *   <li>子命令列表 + 签名 + 描述</li>
     *   <li>参数说明（来自 @CmdParam）</li>
     * </ul>
     * <p>
     * 子类可以覆盖此方法以提供自定义帮助文本。
     */
    public String getHelpText() {
        Cmd cmdAnnotation = getClass().getAnnotation(Cmd.class);
        if (cmdAnnotation != null) {
            // 有 @Cmd 注解 → 自动从注解体系生成完整帮助
            try {
                return CmdParamProcessor.generateHelpText(getClass());
            } catch (Exception e) {
                // 注解读取失败时降级到简单帮助
            }
        }
        return "用法: " + getCommandName() + " [args...]\n" +
               "输入 " + getCommandName() + " --help 查看详细帮助";
    }

    public abstract Res runMain(CommandExecutor.CmdExecContext<CommandRequest> context) throws Exception;

    public String getCommandName() {
        return logger.getTag();
    }

    protected Res createErrorResult(String message) throws Exception {
        Res result = responseType.newInstance();
        result.setSuccess(false);
        result.setMessage(message);
        return result;
    }

    protected Res createErrorResult(String message, Throwable t) throws Exception {
        Res result = responseType.newInstance();
        result.setSuccess(false);
        result.setError(new CommandResult.ErrorInfo("EXECUTION_FAILED", message, t.toString()));
        return result;
    }

    protected Res createSuccessResult(String message) throws Exception {
        Res result = responseType.newInstance();
        result.setSuccess(true);
        result.setMessage(message);
        return result;
    }

}
