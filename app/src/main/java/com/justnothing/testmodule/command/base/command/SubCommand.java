package com.justnothing.testmodule.command.base.command;

import com.justnothing.testmodule.command.base.AbstractCommand;
import com.justnothing.testmodule.command.base.parser.ParamBase;
import com.justnothing.testmodule.command.base.protocol.CommandRequest;
import com.justnothing.testmodule.command.base.protocol.CommandResult;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.TYPE_USE})
public @interface SubCommand {
    String value() default "";
    Class<? extends CommandRequest> request() default CommandRequest.class;
    Class<? extends CommandResult> result() default CommandResult.class;
    Class<? extends AbstractCommand> command() default AbstractCommand.class;
    String description() default "";
    ParamSpec[] params() default {};
    
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD, ElementType.CONSTRUCTOR, ElementType.ANNOTATION_TYPE})
    @ParamBase
    @interface ParamSpec {
        String name();
        String description();
        boolean required() default true;
        boolean varArgs() default false;
        Class<?> type() default Object.class;
        String defaultValue() default "";
    }
}
