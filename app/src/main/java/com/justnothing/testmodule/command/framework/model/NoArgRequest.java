package com.justnothing.testmodule.command.framework.model;

import com.justnothing.testmodule.command.framework.error.IllegalCommandLineArgumentException;

/**
 * "无参请求"占位类型：用于那些不使用请求对象、直接读 {@code ctx.args()} 的纯 CLI 路由（如 help）。
 *
 * <p>路由表要求每条路由都声明一个 {@code request}。此前这类路由写的是抽象基类
 * {@code CommandRequest.class}，逼得 {@code @CmdRoutes.Route.request()} 只能是裸类型，
 * 并在框架与 handler 里各放一处强转。改用这个具体占位类后：</p>
 * <ul>
 *   <li>注解元素可以恢复成 {@code Class<? extends CommandRequest<?>>}，重新拿到编译期类型检查；</li>
 *   <li>它实现 {@link CustomCommandLineParser} 并在 {@code customParse} 里原样返回，
 *       因此剩余命令行参数（如 {@code help class} 的 {@code class}）既不会被解析、
 *       也不会触发"位置参数过多"的报错。</li>
 * </ul>
 */
public class NoArgRequest extends CommandRequest<CommandResult> implements CustomCommandLineParser {

    @Override
    public CommandRequest<?> customParse(ParseContext context) throws IllegalCommandLineArgumentException {
        return this;
    }
}
