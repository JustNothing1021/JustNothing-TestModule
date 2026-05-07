package com.justnothing.testmodule.command.base.command;

import com.justnothing.testmodule.command.base.protocol.CommandRequest;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface SupportsRequests {
    Class<? extends CommandRequest>[] value();
}
