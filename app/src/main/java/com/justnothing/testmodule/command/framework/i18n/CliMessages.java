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
 * 纯 Java、按语言码切。区别只在于本类把两种语言<b>并排写在同一个枚举里</b> ——
 * Tips 那边是两个类各持一个 Map，加一条提示要改两个文件、而且某一边漏了不会报错，
 * 这里是编译期就凑齐的。</p>
 *
 * <h3>语言从哪来</h3>
 * <b>由客户端声明，不由服务端猜。</b>命令是客户端发起的，但文案是服务端渲染的，而两边常常不在一个进程里：
 * GUI 用 {@code R.string}（跟随应用语言），服务端可能跑在目标 app 的进程里，它自己的
 * {@link Locale#getDefault()} 是那个进程的语言。所以语言随请求传过来：
 * {@code ClientRequirements#getLanguage()} → {@code sys.hello} 握手 → 服务端每次执行前
 * {@link #useLanguage(String)}。客户端没声明时（老客户端、agent 内部调用）才回落到本进程 Locale。
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
        return chinese() ? chinese : english;
    }

    /** 当前语言下的文案，带 {@link String#format} 参数。 */
    public String format(Object... args) {
        return String.format(text(), args);
    }

    // ==================== 语言判定 ====================

    // 名字不能叫 chinese —— 那是每个枚举常量的实例字段名，同名字段会直接编译不过。
    //
    // 用 ThreadLocal 而不是 static 布尔值：这里是**服务端**，同一个进程可能同时在服务多个客户端
    // （GUI 一个、设备终端一个、别的 app 通过 agent 再来一个），而它们的界面语言可以各不相同。
    // 语言是「按请求」决定的，不是「按进程」决定的，所以必须按线程隔离，否则两个客户端会互相踩。
    // 未设置时回落到本进程的 Locale.getDefault() —— 也就是引入这条通道之前的老行为。
    private static final ThreadLocal<Boolean> chineseLocale = new ThreadLocal<>();

    /**
     * 覆盖<b>当前线程</b>的语言。
     *
     * <p>语言是随请求从客户端带过来的（{@code ClientRequirements#getLanguage()}，经 sys.hello 握手），
     * 因为服务端进程的 {@link Locale#getDefault()} 未必等于客户端用户的界面语言 ——
     * 服务端可能跑在另一个 app 进程里。服务端在每次命令执行前调用本方法，
     * 执行结束在 {@code CommandExecutor#cleanup()} 里 {@link #clearLanguage()}。</p>
     *
     * @param language 语言码（如 {@code "zh"} / {@code "en"}）；null 或空串表示清除覆盖、跟随本进程 Locale
     */
    public static void useLanguage(String language) {
        if (language == null || language.isEmpty()) {
            clearLanguage();
        } else {
            chineseLocale.set(isChinese(language));
        }
    }

    /**
     * 清除当前线程的覆盖，恢复跟随本进程的 Locale。
     *
     * <p>必须在每次请求结束时调用：服务端线程来自线程池、会被复用，
     * 残留的覆盖会把上一个客户端的语言泄漏给下一个。</p>
     */
    public static void clearLanguage() {
        chineseLocale.remove();
    }

    /** 当前线程实际生效的语言：有覆盖就用覆盖，否则看本进程的 Locale。 */
    public static boolean chinese() {
        Boolean override = chineseLocale.get();
        return override != null ? override : isChinese(Locale.getDefault().getLanguage());
    }

    private static boolean isChinese(String language) {
        // 只判语言码就够了：zh-CN / zh-TW / zh-Hans 的 getLanguage() 都是 "zh"。
        return "zh".equalsIgnoreCase(language);
    }
}
