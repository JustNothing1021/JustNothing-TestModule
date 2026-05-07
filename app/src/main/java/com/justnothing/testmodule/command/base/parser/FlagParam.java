package com.justnothing.testmodule.command.base.parser;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@ParamBase
public @interface FlagParam {
    String[] names();
    String description() default "";
    String mutexId() default "";
    boolean defaultValue() default false;

    @SuppressWarnings("BooleanMethodIsAlwaysInverted") boolean negated() default false;
}
