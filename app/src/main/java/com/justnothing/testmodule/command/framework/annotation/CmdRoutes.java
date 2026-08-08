package com.justnothing.testmodule.command.framework.annotation;

import com.justnothing.testmodule.command.framework.model.CommandRequest;
import com.justnothing.testmodule.command.framework.model.CommandResult;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface CmdRoutes {
    Route[] value();

    @Retention(RetentionPolicy.RUNTIME)
    @interface Route {
        String path();
        Class<? extends CommandRequest> request();
        Class<? extends CommandResult> result() default CommandResult.class;
        Class<?> handler();
        String description() default "";
    }
}
