package com.justnothing.testmodule.command.framework.model;

import com.justnothing.testmodule.command.framework.CommandExecutor;
import com.justnothing.testmodule.command.framework.annotation.SubCommandInfo;
import com.justnothing.testmodule.command.framework.error.IllegalCommandLineArgumentException;
import com.justnothing.testmodule.command.framework.i18n.CliMessages;
import com.justnothing.testmodule.command.framework.i18n.CliTexts;
import com.justnothing.testmodule.command.framework.utils.CommandExceptionHandler;

public abstract class AbstractCommand<Req extends CommandRequest<?>, Res extends CommandResult> implements Command<Res> {
    protected final String commandName;
    protected final Class<Req> requestType;
    protected final Class<Res> returnType;


    protected AbstractCommand(String commandName, Class<Req> requestType, Class<Res> returnType) {
        this.commandName = commandName;
        this.requestType = requestType;
        this.returnType = returnType;
    }

    protected Class<Req> getAcceptableRequestType() {
        return requestType;
    }

    protected boolean acceptable(CommandRequest<?> req) {
        if (req == null) {
            return true;
        }
        return getAcceptableRequestType().isAssignableFrom(req.getClass());
    }

    @SuppressWarnings("unchecked")
    public Res execute(CommandExecutor.CmdExecContext<? extends CommandRequest<?>> context) {
        if (!acceptable(context.getRequest())) {
            throw new IllegalArgumentException("命令请求类型错误; 期待"
                    + getAcceptableRequestType().getSimpleName()
                    + ", 却接收到了" + context.getRequest().getClass().getSimpleName());
        }
        try {

            return executeInternal((CommandExecutor.CmdExecContext<Req>) context);
        } catch (IllegalCommandLineArgumentException e) {
            throw e;
        } catch (Exception e) {
            CommandExceptionHandler.handleException(
                commandName,  e,  context,
                "执行" + commandName + "命令失败"
            );
            Res result = newResultInstance();
            result.setSuccess(false);
            if (context.getRequest() != null) {
                result.setRequestId(context.getRequest().getRequestId());
            }
            // 同时写 message：结构化 error 供客户端取 code，message 供纯文本展示，
            // 两者都会上线（stacktrace 仍排除在外）。
            String message = "执行" + commandName + "命令失败"
                    + (e.getMessage() != null ? ": " + e.getMessage() : "");
            result.setMessage(message);
            result.setError(
                new CommandResult.ErrorInfo("UNEXPECTED_ERROR", message, e)
            );

            return result;
        }
    }

    protected abstract Res executeInternal(CommandExecutor.CmdExecContext<Req> context) throws Exception;

    /**
     * 反射构造一个结果实例。
     *
     * <p>取代已弃用的 {@code Class.newInstance()}：用 {@code getDeclaredConstructor()} 保证能抛出
     * 真实原因，并且失败时抛出异常而不是返回 null——返回 null 会让调用方在解引用时二次崩溃，
     * 掩盖真正的失败原因。</p>
     */
    protected Res newResultInstance() {
        try {
            return returnType.getDeclaredConstructor().newInstance();
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(
                    "无法创建结果类型实例 " + returnType.getName() + "（需要可见的无参构造器）", e);
        }
    }

    /** 构造一个失败结果。 */
    protected Res createErrorResult(String message) {
        Res result = newResultInstance();
        result.setSuccess(false);
        result.setMessage(message);
        return result;
    }

    /** 构造一个失败结果，并把完整堆栈放进结构化 error（而非 {@code t.toString()}）。 */
    protected Res createErrorResult(String message, Throwable t) {
        Res result = newResultInstance();
        result.setSuccess(false);
        result.setMessage(message);
        result.setError(new CommandResult.ErrorInfo("EXECUTION_FAILED", message, t));
        return result;
    }

    /** 构造一个成功结果。 */
    protected Res createSuccessResult(String message) {
        Res result = newResultInstance();
        result.setSuccess(true);
        result.setMessage(message);
        return result;
    }

    @Override
    public String getHelpText() {
        SubCommandInfo info = this.getClass().getAnnotation(SubCommandInfo.class);
        if (info != null) {
            return generateHelpFromAnnotation(info);
        }
        return CliMessages.HELP_FALLBACK_USAGE.format(commandName, commandName);
    }


    public static String generateHelpFromAnnotation(SubCommandInfo info) {
        StringBuilder help = new StringBuilder();

        // usage 多数是纯命令语法，resolve 通常原样返回；含中文时才需要拆成 id
        String usage = CliTexts.resolve(info.usage());
        if (!usage.isEmpty()) {
            help.append(CliMessages.HELP_USAGE_INLINE.text()).append(usage).append("\n");
        }

        String description = CliTexts.resolve(info.description());
        if (!description.isEmpty()) {
            help.append("\n\n").append(description);
        }


        String optionsDesc = CliTexts.resolve(info.optionsDesc());
        if (!optionsDesc.isEmpty()) {
            if (help.length() > 0) {
                help.append("\n\n");
            }
            help.append(optionsDesc);
        }


        if (info.examples().length > 0) {
            help.append(CliMessages.HELP_INDENTED_EXAMPLES.text());
            for (String ex : info.examples()) {
                help.append("\n  ").append(ex);
            }
        }

        if (info.seeAlso().length > 0) {
            help.append(CliMessages.HELP_INDENTED_SEE_ALSO.text());
            for (String see : info.seeAlso()) {
                help.append("\n  ").append(see);
            }
        }

        return help.toString();
    }
}
