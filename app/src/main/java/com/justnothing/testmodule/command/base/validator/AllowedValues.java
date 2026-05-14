package com.justnothing.testmodule.command.base.validator;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 枚举值验证注解 - 限制参数只能是预定义的值之一
 *
 * 使用示例:
 * <pre>
 * // 限制 action 只能是 start/stop/report/export
 * &#64;AllowedValues({"start", "stop", "report", "export"})
 * &#64;PositionalParam(name = "action", order = 1)
 * private String action;
 *
 * // 限制 mode 只能是 get/set/list (不区分大小写)
 * &#64;AllowedValues(value = {"get", "set", "list"}, caseSensitive = false)
 * &#64;KeywordParam(name = "mode", names = {"-m"})
 * private String mode;
 * </pre>
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface AllowedValues {
    String[] value();
    boolean caseSensitive() default true;
    String message() default "";
}
