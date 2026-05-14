package com.justnothing.testmodule.command.base.validator;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 正则表达式验证注解 - 限制参数必须匹配指定格式
 *
 * 使用示例:
 * <pre>
 * // 限制 className 必须是合法的 Java 类名
 * &#64;Pattern(regex = "^[a-zA-Z_$][a-zA-Z0-9_$.]*$", description = "Java类名")
 * &#64;PositionalParam(name = "className", order = 2)
 * private String className;
 *
 * // 限制 filePath 必须以 / 或 . 开头
 * &#64;Pattern(regex = "^[./]", description = "文件路径")
 * &#64;PositionalParam(name = "filePath", order = 3)
 * private String filePath;
 * </pre>
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Pattern {
    String regex();
    String description() default "";
    String message() default "";
}
