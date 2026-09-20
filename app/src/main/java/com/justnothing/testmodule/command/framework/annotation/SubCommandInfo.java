package com.justnothing.testmodule.command.framework.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 子命令的帮助信息。
 *
 * <p><b>注意：本注解的所有文字元素都只接受「文案 id」，不接受中文原文。</b>
 * 例如 {@code description = MemoryTexts.SUB_MEMORY_GC_DESC}，而不是
 * {@code description = "手动触发垃圾回收"}。id 的命名规范见
 * {@link com.justnothing.testmodule.command.framework.i18n.CliTexts}，
 * 文案本体（中英两份）写在对应命令族的 {@code XxxTexts} 类里。</p>
 *
 * <p>为什么不像别处那样直接写中文：注解值是<b>编译期常量</b>，运行时换不了语言。
 * 若把中文放在这里，就等于把中文变成了主键 —— 哪天改了中文，英文翻译会静默失效。
 * 所以这里只放 id，让文案与「它代表什么」解耦。代价是打开命令类看不回原文，
 * 这是明确接受了的取舍。</p>
 *
 * <p>以前这里的默认值是「这个命令没有描述信息...」这类中文占位串，还被
 * {@code CmdHelpGenerator} 用 {@code startsWith} 反查过 —— 那是拿内容当标记用。
 * 现在默认值统一为空串，渲染方在取到空值时回落到路由注册时的描述。</p>
 *
 * <p>{@link #usage()} 是例外：它多数是纯命令语法（{@code memory gc [options]}），
 * 不含中文，因此保留直接写原文的写法；只有当 usage 里确实有中文时，才需要拆出 id。</p>
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface SubCommandInfo {

    /** 帮助正文里的描述，值是文案 id（如 {@code sub.memory.gc.desc}）。空则回落到路由描述。 */
    String description() default "";

    /** 用法。纯语法直接写原文；含中文时才用 id。 */
    String usage() default "";

    String[] examples() default {};

    /** 选项详细说明，值是文案 id。多行文本块整体作为一个 id。 */
    String optionsDesc() default "";

    String[] seeAlso() default {};
}
