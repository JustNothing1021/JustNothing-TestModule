package com.justnothing.testmodule.command.framework.i18n;

import java.util.Locale;

/**
 * CLI 文案的语言切换。
 *
 * <h3>为什么不用 Android 的 strings.xml</h3>
 * 这套命令框架被设计成<b>能脱离 Android 运行</b>：{@code AppEnvironment.isAndroidEnv()} 会探测
 * 自己是不是跑在安卓上，工具链里还有 {@code isHookEnv()}。对应的代价是 {@code command/} 目录下
 * 一个 {@code R.string} 都没有，15 个测试文件全部跑在纯 JVM 上。
 * 一旦为了让命令输出用上 {@code R.string} 而把 Context 引进来，这些测试就得集体改造成 Robolectric ——
 * 而它们恰好是保护这套框架的唯一屏障。
 *
 * <p>所以这里走项目里已有的先例（{@code utils/tips/TipSystem} + {@code ChineseTips}/{@code EnglishTips}）：
 * 纯 Java、按 {@code Locale} 切。区别只在于本类把两种语言<b>并排写在同一个枚举里</b> ——
 * Tips 那边是两个类各持一个 Map，加一条提示要改两个文件、而且某一边漏了不会报错，
 * 这里是编译期就凑齐的。</p>
 *
 * <h3>两类文案，两种处理</h3>
 * <ol>
 *   <li><b>框架自己的骨架文字</b>（「子命令:」「必需」「整数」「用法:」…）：本枚举里有名字，直接引用。
 *       这类是代码，改起来必须能被 IDE 重命名和搜索，所以用枚举成员而不是 id 字符串。</li>
 *   <li><b>各命令写在注解里的说明文字</b>（{@code @SubCommandInfo(description = MemoryTexts.SUB_MEMORY_GC_DESC)} 等）：
 *       注解值是编译期常量，运行时换不了语言，所以走 {@code <id 常量> + 查表} 的路子 ——
 *       见 {@link CliTexts} 与本枚举里 {@code chinese()} 的关系。</li>
 * </ol>
 */
public enum CliMessages {

    // ==================== 帮助文档骨架 ====================
    HELP_SUBCOMMANDS("子命令:\n", "Subcommands:\n"),
    HELP_PARAM_DETAILS("参数详情:\n", "Parameter details:\n"),
    HELP_NO_PARAMS("  (所有子命令暂无参数)\n\n", "  (no subcommand takes parameters yet)\n\n"),
    HELP_USAGE("用法:\n", "Usage:\n"),
    // 与 HELP_USAGE 的区别只在排版：这里是「标签 + 同行内容」（用法: memory gc [options]），
    // 上面那个是「标签独占一行 + 内容缩进」。两条帮助渲染路径各有各的版式，不强行统一，
    // 否则会顺带改掉其中一个界面的观感。
    HELP_USAGE_INLINE("用法: ", "Usage: "),
    HELP_EXAMPLES("示例:\n", "Examples:\n"),
    HELP_OPTIONS("选项:\n", "Options:\n"),
    HELP_OPTION_DETAILS("选项详情:\n", "Option details:\n"),
    HELP_SEE_ALSO("相关命令:\n", "See also:\n"),
    HELP_INDENTED_EXAMPLES("\n\n示例:", "\n\nExamples:"),
    HELP_INDENTED_SEE_ALSO("\n\n相关命令:", "\n\nSee also:"),
    HELP_NO_ANNOTATION("错误: 无法生成帮助文档; 该命令没有打上 @Cmd 注解",
            "Error: cannot generate help; this command has no @Cmd annotation"),
    HELP_FALLBACK_USAGE("用法: %s <args>\n输入 %s --help 查看详细帮助",
            "Usage: %s <args>\nRun %s --help for detailed help"),
    HELP_CLASS_PARAMS("%s 参数:\n\n", "%s parameters:\n\n"),
    HELP_OPTIONS_SUFFIX(" [选项]", " [options]"),

    // ==================== 顶层帮助（CommandExecutor.getHelpText）====================
    // 两个 %s 分别是版本号和由 buildCommandList() 拼出来的命令清单。
    HELP_BANNER("""
            Xposed Method CLI (Command Line Interface) Server端 %s
            作者: JustNothing1021, DeepSeek 和 GLM-4.7

            一个用来调试安卓开发千奇百怪的诡异问题的Xp模块/命令行工具.

            命令语法: methods [options] <command> [args...]
            %s
            获取一个子命令的帮助:
              help <cmd_name>

            可选项:
              -cl, --classloader <package>      - 指定类加载器（软件包名，没找到会是默认的类加载器）
            """,
            """
            Xposed Method CLI (Command Line Interface) Server %s
            Author: JustNothing1021, DeepSeek and GLM-4.7

            An Xposed module / command line tool for debugging the bizarre problems
            that turn up in Android development.

            Syntax: methods [options] <command> [args...]
            %s
            Get help for a single subcommand:
              help <cmd_name>

            Options:
              -cl, --classloader <package>      - which class loader to use (package name; falls back to the default one)
            """),

    // 命令清单的小节标题，对应 @Cmd.group()。未列出的 group 原样显示。
    HELP_GROUP_GENERAL("可用命令", "Available commands"),
    HELP_GROUP_SYSTEM("系统命令", "System commands"),
    HELP_GROUP_FUN("娱乐性命令", "Fun commands"),

    // ==================== 参数属性（formatParamDetail / formatParamHelp）====================
    PARAM_REQUIRED("必需", "required"),
    PARAM_OPTIONAL("可选", "optional"),
    PARAM_DEFAULT_PREFIX("默认=", "default="),
    PARAM_DEFAULT_INLINE(" (默认: ", " (default: "),
    PARAM_VARARGS("可变参数", "varargs"),
    PARAM_RANGE_PREFIX("范围: ", "range: "),
    PARAM_ENUM_PREFIX("枚举: {", "enum: {"),
    PARAM_OPERATOR("操作符", "operator"),
    PARAM_OPERATOR_INLINE(" [操作符", " [operator"),
    PARAM_MUTEX_PREFIX(" [互斥: ", " [mutually exclusive with: "),
    PARAM_OPERATOR_CONSUMES_PREFIX(", 消费", ", consumes"),
    PARAM_OPERATOR_CONSUMES_SUFFIX("个参数", " args"),

    // ==================== 类型名（inferTypeName）====================
    TYPE_INT("整数", "int"),
    TYPE_LONG("长整数", "long"),
    TYPE_DOUBLE("浮点数", "double"),
    TYPE_BOOLEAN("布尔值", "boolean"),
    TYPE_STRING("字符串", "string"),
    TYPE_LIST("列表", "list"),
    ;

    private final String chinese;
    private final String english;

    CliMessages(String chinese, String english) {
        this.chinese = chinese;
        this.english = english;
    }

    /** 当前语言下的文案。 */
    public String text() {
        return chineseLocale ? chinese : english;
    }

    /** 当前语言下的文案，带 {@link String#format} 参数。 */
    public String format(Object... args) {
        return String.format(text(), args);
    }

    // ==================== 语言判定 ====================

    // 名字不能叫 chinese —— 那是每个枚举常量的实例字段名，同名字段会直接编译不过。
    private static boolean chineseLocale = isChineseLocale(Locale.getDefault());

    /**
     * 覆盖语言。不调的话就是跟随系统（{@link Locale#getDefault()}）。
     * 给「用户想强制某个语言」和测试用。
     */
    public static void useLanguage(Locale locale) {
        chineseLocale = isChineseLocale(locale == null ? Locale.getDefault() : locale);
    }

    public static boolean chinese() {
        return chineseLocale;
    }

    private static boolean isChineseLocale(Locale locale) {
        // 只判语言码就够了：zh-CN / zh-TW / zh-Hans 的 getLanguage() 都是 "zh"。
        return "zh".equals(locale.getLanguage());
    }
}
