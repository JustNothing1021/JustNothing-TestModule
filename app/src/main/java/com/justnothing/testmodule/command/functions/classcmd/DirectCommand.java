package com.justnothing.testmodule.command.functions.classcmd;

import com.justnothing.testmodule.utils.logging.Logger;

import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * 轻量级命令实现 — 允许通过函数式接口直接定义命令逻辑，
 * 无需为每个简单操作创建完整的 Command 子类。
 *
 * <p>适用场景:
 * <ul>
 *   <li>GUI/JSON 请求的快速集成（无需遵循 CLI 设计模式）</li>
 *   <li>逻辑简单的操作（<50行代码）</li>
 *   <li>原型开发或过渡期代码</li>
 * </ul>
 *
 * <p>使用示例:
 * <pre>{@code
 * // 方式1: 使用 BiFunction (推荐 — 可访问 context)
 * ClassCommand<ClassHierarchyRequest, ClassHierarchyResult> hierarchyCmd =
 *     new DirectCommand<>("hierarchy",
 *         ClassHierarchyRequest.class,
 *         ClassHierarchyResult.class,
 *         (ctx, req) -> {
 *             // 访问 ctx.execContext() 获取 classLoader/logger 等
 *             // 处理 req 并返回结果
 *             return result;
 *         }
 *     );
 *
 * // 方式2: 使用 Function (简化版 — 只需 Request)
 * ClassCommand<SetFieldValueRequest, SetFieldValueResult> setFieldCmd =
 *     new DirectCommand<>("setfield",
 *         SetFieldValueRequest.class,
 *         SetFieldValueResult.class,
 *         req -> {
 *             // 简单处理，直接返回结果
 *             return result;
 *         }
 *     );
 * }</pre>
 */
public class DirectCommand<Req extends ClassCommandRequest, Res extends ClassCommandResult>
        extends AbstractClassCommand<Req, Res> {

    private static final Logger logger = Logger.getLoggerForName("DirectCommand");

    private final String commandName;
    private final Class<Res> responseType;
    private final BiFunction<ClassCommandContext<Req>, Req, Res> executor;

    /**
     * 创建一个轻量级命令（完整版 — 可访问 Context）。
     *
     * @param commandName 命令名称（用于注册和日志）
     * @param requestType 请求类型
     * @param responseType 结果类型
     * @param executor 执行器函数，接收 (context, request) 返回 result
     */
    public DirectCommand(String commandName,
                        Class<Req> requestType,
                        Class<Res> responseType,
                        BiFunction<ClassCommandContext<Req>, Req, Res> executor) {
        super(commandName, requestType, responseType);
        this.commandName = commandName;
        this.responseType = responseType;
        this.executor = executor;
    }

    /**
     * 创建一个轻量级命令（简化版 — 只需 Request）。
     *
     * @param commandName 命令名称
     * @param requestType 请求类型
     * @param responseType 结果类型
     * @param executor 执行器函数，只接收 request 返回 result
     */
    public DirectCommand(String commandName,
                        Class<Req> requestType,
                        Class<Res> responseType,
                        Function<Req, Res> executor) {
        this(commandName, requestType, responseType,
            (ctx, req) -> executor.apply(req));
    }

    /**
     * 获取命令名称。
     */
    public String getCommandName() {
        return commandName;
    }

    @Override
    protected Res executeClassCommand(ClassCommandContext<Req> context) throws Exception {
        try {
            Req request = context.execContext().getCommandRequest();
            logger.debug("执行DirectCommand: " + commandName +
                ", request=" + request.getClass().getSimpleName());

            Res result = executor.apply(context, request);

            if (result == null) {
                logger.warn("DirectCommand " + commandName + " 返回了null结果");
                result = createErrorResult("命令执行返回null", responseType);
            }

            return result;
        } catch (Exception e) {
            logger.error("DirectCommand " + commandName + " 执行失败", e);
            throw e;
        }
    }
}
