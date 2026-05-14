package com.justnothing.testmodule.command.base.validator;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 数值范围验证注解 - 限制参数必须在指定范围内
 *
 * 使用示例:
 * <pre>
 * // 限制 sampleRate 在 1-10000 之间
 * &#64;Range(min = 1, max = 10000)
 * &#64;PositionalParam(name = "rate", order = 2)
 * private Integer sampleRate;
 *
 * // 限制 duration 至少为 100ms
 * &#64;Range(min = 100)
 * &#64;PositionalParam(name = "duration", order = 2)
 * private Integer duration;
 * </pre>
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Range {
    double min() default Double.NEGATIVE_INFINITY;
    double max() default Double.POSITIVE_INFINITY;
    boolean inclusive() default true;
    String message() default "";
}
