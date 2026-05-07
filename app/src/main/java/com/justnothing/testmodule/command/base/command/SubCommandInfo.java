package com.justnothing.testmodule.command.base.command;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface SubCommandInfo {
    
    String description() default "这个命令没有描述信息...";
    
    String usage() default "这个命令没有提供用法示例...";
    
    String[] examples() default {};
    
    String optionsDesc() default "这个命令没有帮助信息...";
    
    String[] seeAlso() default {};
}
