package com.justnothing.testmodule.command.base.protocol;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface AutoSerializable {
    boolean includeSuperFields() default true;
    String[] excludeFields() default {};
}
