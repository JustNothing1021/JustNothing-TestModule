package com.justnothing.testmodule.command.base.command;

import com.justnothing.testmodule.command.base.protocol.CommandResult;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface Cmd {
    String name();
    String group() default "general";
    String description();
    String version() default "";
    Class<? extends CommandResult> defaultResultType();
    boolean needsClassContext() default false;
    String permission() default "USER";
    String helpText() default "";
}
