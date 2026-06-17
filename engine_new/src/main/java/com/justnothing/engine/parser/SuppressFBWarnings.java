package com.justnothing.engine.parser;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 抑制 SpotBugs/FindBugs 警告。
 * 等效于 com.github.spotbugs.annotations.SuppressFBWarnings，
 * 但不引入外部依赖。
 */
@Target({ElementType.TYPE, ElementType.METHOD, ElementType.FIELD})
@Retention(RetentionPolicy.CLASS)
public @interface SuppressFBWarnings {
    String[] value();
}
