package com.justnothing.testmodule.command.base.protocol;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ResultField {
    String name();
    String description() default "";
    boolean required() default false;
    Class<? extends ValueSupplier> defaultValue() default NoDefaultSupplier.class;
}
