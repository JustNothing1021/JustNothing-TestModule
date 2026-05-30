package com.justnothing.testmodule.command.base.command;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface CmdParam {
    String name();
    String description();
    boolean required() default true;
    String defaultValue() default "";
    String[] aliases() default {};
    boolean varArgs() default false;
    int position() default 0; // 大于等于0的时候表示作为位置参数
    ReadMode readMode() default ReadMode.STRIPPED; // 引号处理模式
    String serializedName() default "";
    boolean serialize() default true;
    boolean deserialize() default true;
    String pattern() default "";
    String[] allowedValues() default {};
    double min() default Double.NEGATIVE_INFINITY;
    double max() default Double.POSITIVE_INFINITY;
    String[] mutexWith() default {};  // 互斥参数列表（不能同时使用）
    String[] requires() default {};   // 依赖参数列表（此参数需要这些参数也必须存在）
    boolean isOperator() default false; // 是否为操作符标志（改变后续解析行为，如 -g/-s/get/set）
    int operatorArgs() default 0;      // 操作符消费的后续参数数量（0=仅flag，1=消费1个，2=消费2个...）

    // 操作符归属系统
    String belongsToOperator() default "";  // 此参数属于哪个操作符 ("" = 通用/不属于任何操作符)
    int operatorIndex() default -1;         // 在操作符中的索引 (-1 = 不属于, 0 = 操作符标志本身, 1+ = 操作符的第N个参数)


    enum ReadMode {
        /** 原始模式：test -> "test", "test" -> "\"test\"", "arg with spaces" -> "\"arg" (按空格分割) */
        RAW,
        /** 智能去引号：test -> "test", "test" -> test, "arg with spaces" -> arg with spaces (推荐用于普通字符串） */
        STRIPPED,
        /** 完整保留：test -> "test", "test" -> "\"test\"", "arg with spaces" -> "\"arg with spaces\"" (推荐用于代码表达式） */
        PRESERVED
    }

}
