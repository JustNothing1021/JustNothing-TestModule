package com.justnothing.testmodule.command.base.command;

import com.justnothing.testmodule.command.base.protocol.CommandRequest;
import com.justnothing.testmodule.command.base.protocol.CommandResult;

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
