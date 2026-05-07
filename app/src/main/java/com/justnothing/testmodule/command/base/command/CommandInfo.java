package com.justnothing.testmodule.command.base.command;

import com.justnothing.testmodule.command.base.protocol.CommandResult;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface CommandInfo {
    String name();
    String group() default "general";
    String description();
    String helpText() default "";
    String version() default "";
    String[] examples() default {};
    Class<? extends CommandResult> resultType();
    String defaultSubcommand() default "";
    boolean needsClassContext() default false;
    String permission() default "USER";
}
