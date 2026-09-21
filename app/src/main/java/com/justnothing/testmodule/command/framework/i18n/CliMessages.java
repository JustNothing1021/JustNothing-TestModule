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

    // ==================== 命令输出里的通用标签 ====================
    // 「类名: 」「方法: 」这类标签在命令层到处都在用（实测 command/ 下各自出现 30+ 次、跨多个命令族），
    // 放进各族的 XxxTexts 会各存一份、各译一遍，以后改措辞还要改多处，所以统一放这里。
    // 判据是「跨族复用」；只在某一个族里出现的（比如 class 族的树形前缀文案）走那个族的 XxxTexts。
    LABEL_CLASS_NAME("类名: ", "Class: "),
    // 复数那两条是「计数」用的（「字段: 5」），单数这两条是「标签 + 单个名字」用的（「字段: mFoo」）。
    // 中文没有单复数，但英文不加区分会印出 "Fields: mFoo"，所以两种都得留。
    LABEL_METHODS("方法: ", "Methods: "),
    LABEL_FIELDS("字段: ", "Fields: "),
    LABEL_CONSTRUCTORS("构造函数: ", "Constructors: "),
    LABEL_METHOD("方法: ", "Method: "),
    LABEL_FIELD("字段: ", "Field: "),
    LABEL_INTERFACES("接口: ", "Interfaces: "),
    LABEL_MODIFIERS("修饰符: ", "Modifiers: "),
    LABEL_CLASS_LOADER("类加载器: ", "Class loader: "),
    LABEL_SUPER_CLASS("父类: ", "Super class: "),
    LABEL_PACKAGE_NAME("包名: ", "Package: "),
    // 与 LABEL_CLASS_NAME 的区别只在中文措辞（「类:」vs「类名:」），英文撞成一条是正常的。
    LABEL_CLASS("类: ", "Class: "),

    // 命令自己拼错误行时的前缀（「错误: 别名已存在」）。跟 ExceptionHandler 那套转储的措辞不同，
    // 那边是整段格式，这里是单行提示，所以不共用。
    ERROR_PREFIX("错误: ", "Error: "),

    ERR_NOT_ENOUGH_ARGS("参数不足", "not enough arguments"),
    ERR_UNKNOWN_TYPE("未知类型: %s", "Unknown type: %s"),

    // 「无」在各种「取不到东西」的位置当值用（父类、包名、类加载器…），中文里它是完整答案，
    // 英文里当值就要小写。
    VALUE_NONE("无", "none"),

    // ==================== 异常上下文里的键 ====================
    // CommandExceptionHandler 会把 context 逐行打成「  键: 值」，所以这些键也是用户看得见的文案。
    // 它们跨 command/ 多个族（hook、watch、class…），且是 map 的键，不能带冒号。
    CONTEXT_CLASS_NAME("类名", "Class"),
    CONTEXT_FIELD_NAME("字段名", "Field"),
    CONTEXT_METHOD_NAME("方法名", "Method"),
    CONTEXT_PARAM_INDEX("参数索引", "Parameter index"),
    CONTEXT_PARAM_EXPRESSION("参数表达式", "Parameter expression"),
    CONTEXT_ERROR_MESSAGE("错误信息", "Error"),
    CONTEXT_SIGNATURE("签名", "Signature"),

    // ==================== 异常转储（CommandExceptionHandler）====================
    // 每一条都同时喂给「彩色输出」和「拼给调用方的纯文本」，所以调用点先把 .text() 存进局部变量，
    // 别查两遍。
    ERR_HEAD_PREFIX("错误: 执行", "Error: exception while executing "),
    // 英语的语序里没有对应「命令时发生异常」的后半截，留空（中英两侧都只是收尾，不留额外空格）。
    ERR_HEAD_SUFFIX("命令时发生异常", ""),
    ERR_EXCEPTION_TYPE("异常类型: ", "Exception type: "),
    ERR_MESSAGE("错误信息: ", "Error message: "),
    ERR_DETAILS("错误详情: ", "Details: "),
    ERR_CONTEXT("上下文信息:", "Context:"),
    ERR_STACK_TRACE("堆栈追踪:", "Stack trace:"),
    ERR_NO_MESSAGE("无详细信息", "no details"),

    // ==================== 命令执行基类（CommandExecutor）====================
    EXEC_EMPTY_COMMAND("命令不能为空", "Command cannot be empty"),
    EXEC_EMPTY_REQUEST("请求不能为空", "Request cannot be empty"),
    EXEC_NO_COMMAND("没有指定命令 (可以用help来获取帮助)", "No command given (use help for help)"),
    EXEC_UNKNOWN_COMMAND("未知的命令: %s, 输入help获取帮助", "Unknown command: %s; run help for help"),
    EXEC_ARG_ERROR("参数错误: %s", "Argument error: %s"),

    // 这段严重错误报告是分三段打出来的：中间那段要单独染成黄色，所以没法在模板里一次成形。
    // 中英语序不同，三段各自成句，拼起来仍是一句完整的话。
    EXEC_FATAL_HEAD("执行命令出现严重错误...", "A fatal error occurred while executing the command..."),
    EXEC_FATAL_DETAIL_PREFIX("（你现在看到的是命令执行基类的错误报告, 大概率是命令执行爆掉了或者命令内部",
            "(This is the error report from the command execution base class — the command most likely blew up, or it threw an"),
    EXEC_FATAL_DETAIL_HIGHLIGHT("出现了Error而不是Exception", "Error instead of an Exception"),
    EXEC_FATAL_DETAIL_SUFFIX("!）", "!)"),

    // ==================== 参数解析与校验 ====================
    // 这些抛出的 IllegalArgumentException 会被外层接住、打到用户界面上。
    // 模板里的 fieldName 是请求对象里的字段名（代码里的英文标识符），翻不了，所以只翻模板本身。
    ERR_NOT_A_NUMBER("%s必须是数字", "%s must be a number"),
    ERR_PARAM_TOO_SMALL("%s不能小于%d", "%s must not be less than %d"),
    ERR_PARAM_TOO_LARGE("%s不能大于%d", "%s must not be greater than %d"),
    ERR_PARAM_OUT_OF_RANGE("%s必须在%d到%d之间", "%s must be between %d and %d"),
    ERR_NOT_ENOUGH_VALUES("参数不足，至少需要%d个参数", "not enough arguments: at least %d required"),
    ERR_MISSING_PARAM("缺少必填参数: %s", "missing required parameter: %s"),
    ERR_PARAM_NEEDS_VALUE("参数 %s 需要值", "parameter %s needs a value"),
    ERR_PATTERN_MISMATCH("参数 %s 值 '%s' 不匹配模式: %s",
            "parameter %s value '%s' does not match pattern: %s"),
    ERR_WRONG_REQUEST_TYPE("命令请求类型错误; 期待%s, 却接收到了%s",
            "Wrong request type; expected %s but received %s"),
    ERR_INPUT_TIMEOUT("输入请求超时 (%d秒)", "input request timed out (%d s)"),
    ERR_INPUT_FAILED("输入请求失败", "input request failed"),

    // 字段级取值约束（CmdParamValidator）。这一层拿到的 fieldName / 参数名同样是代码里的英文标识符。
    ERR_INVALID_PATTERN("无效的正则表达式: %s", "invalid regular expression: %s"),
    ERR_NOT_IN_ALLOWED_VALUES("参数 %s 值 '%s' 不在允许列表中: %s",
            "parameter %s value '%s' is not in the allowed list: %s"),
    ERR_RANGE_ON_NON_NUMBER("参数 %s 声明了 min/max，但其值 '%s' (%s) 不是数值类型，无法比较大小",
            "parameter %s declares min/max, but its value '%s' (%s) is not numeric and cannot be compared"),
    ERR_BELOW_MIN("参数 %s 值 %s 小于最小值 %s", "parameter %s value %s is below the minimum %s"),
    ERR_ABOVE_MAX("参数 %s 值 %s 大于最大值 %s", "parameter %s value %s is above the maximum %s"),
    ERR_FIELD_ACCESS_FAILED("无法访问字段: %s", "cannot access field: %s"),
    ERR_MUTEX_CONFLICT("参数 '%s' 与 '%s' 互斥，不能同时使用。\n提示: 请选择其中之一",
            "parameters '%s' and '%s' are mutually exclusive.\nHint: pick one of them"),
    ERR_REQUIRES_MISSING("参数 '%s' 需要同时指定 '%s'。\n提示: 请添加 --%s 参数",
            "parameter '%s' also requires '%s'.\nHint: add the --%s option"),

    // ==================== 路由（CommandRouter）====================
    ERR_UNKNOWN_COMMAND("未知的命令: %s", "Unknown command: %s"),
    ERR_NO_MATCHING_ROUTE("未找到匹配的路由: %s %s", "No matching route found: %s %s"),

    // ==================== 框架内部断言 / 协议错误 ====================
    // 这些大多是不可达的调用方误用断言，但它们跟上面那些一样会经由异常转储打到屏幕上，
    // 所以一并翻译 —— 半中半英的报错比全英文更难读。
    ERR_OUTPUT_HANDLER_NULL("输出处理器不能为null", "output handler must not be null"),
    ERR_REQUEST_NULL("请求对象不能为 null", "request object must not be null"),
    ERR_SET_FIELD_FAILED("设置字段 %s 失败: %s", "failed to set field %s: %s"),
    ERR_INPUT_STREAM_NULL("InputStream不能为null", "InputStream must not be null"),
    ERR_OUTPUT_STREAM_NULL("OutputStream不能为null", "OutputStream must not be null"),
    ERR_WRITER_CLOSED("输出器已关闭", "the output writer is already closed"),
    ERR_NO_TERMINAL_INPUT("InvalidConsole 不支持终端输入", "InvalidConsole does not support terminal input"),
    ERR_UTILITY_NOT_INSTANTIABLE("工具类不能实例化", "utility class cannot be instantiated"),

    // IPC 分帧错误：连接被写坏时才会抛，但会原样进错误转储。
    ERR_FRAME_TRUNCATED_HEADER("帧被截断：流已结束，但只收到 %s 字节包头（需要 %s）",
            "truncated frame: stream ended after only %s bytes of header (%s required)"),
    ERR_FRAME_TRUNCATED_BODY("帧被截断：期望 %s 字节，流结束时只有 %s",
            "truncated frame: expected %s bytes but the stream ended after %s"),
    ERR_FRAME_BAD_START_MARKER("无效的起始标记: %s", "invalid start marker: %s"),
    ERR_FRAME_BAD_END_MARKER("无效的结束标记: %s", "invalid end marker: %s"),
    ERR_FRAME_BAD_DATA_LENGTH("无效的数据长度: %s", "invalid data length: %s"),
    ERR_FRAME_TOO_LARGE("缓冲区已满(%s 字节)仍凑不出完整帧，帧长度超过上限",
            "buffer is full (%s bytes) and still holds no complete frame; the frame exceeds the size limit"),
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
    // 用 ThreadLocal 而不是 static 字段：这里是**服务端**，同一个进程可能同时在服务多个客户端
    // （GUI 一个、设备终端一个、别的 app 通过 agent 再来一个），而它们的界面语言可以各不相同。
    // 语言是「按请求」决定的，不是「按进程」决定的，所以必须按线程隔离，否则两个客户端会互相踩。
    // 未设置时回落到本进程的 Locale.getDefault() —— 也就是引入这条通道之前的老行为。
    //
    // 存的是语言码，不是「是不是中文」这个布尔值：本枚举只有中英两套骨架文案，判个 chinese()
    // 就够了；但 Text（输出文案）是可能带第三语言的，布尔值表达不了。今天改成语言码是几行的事，
    // 等输出文案都铺开之后再改就是横切手术。
    private static final ThreadLocal<String> languageOverride = new ThreadLocal<>();

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
            languageOverride.set(normalize(language));
        }
    }

    /**
     * 清除当前线程的覆盖，恢复跟随本进程的 Locale。
     *
     * <p>必须在每次请求结束时调用：服务端线程来自线程池、会被复用，
     * 残留的覆盖会把上一个客户端的语言泄漏给下一个。</p>
     */
    public static void clearLanguage() {
        languageOverride.remove();
    }

    /**
     * 在当前线程临时切到指定语言执行 {@code body}，结束后恢复本线程原来的语言。
     *
     * <p>给「文案不是命令线程产出的」用。有些任务（比如 {@code watch} 的监控任务、被 hook 的
     * app 线程回调）在发起它的那条命令返回之后还在跑，而线程池的线程和被 hook 的线程上都没有
     * 请求上下文；这时它渲染出来的文案会回落到本进程 Locale —— 也就是目标 app 的语言，
     * 而不是客户的界面语言。做法是：命令线程上用 {@link #language()} 把语言记进任务对象，
     * 任务在别的线程上产出文案时用本方法包一层。</p>
     *
     * <p>必须配对恢复（本方法用 finally 保证）：池线程是复用的，残留的语言会泄漏给
     * 下一个跑在这条线程上的任务。</p>
     *
     * @param language 要切到的语言码；null 或空串表示「这个任务没有声明语言」，跟随本进程 Locale
     */
    public static void withLanguage(String language, Runnable body) {
        String previous = languageOverride.get();
        try {
            useLanguage(language);
            body.run();
        } finally {
            // 不能直接 useLanguage(previous)：它把 null 和空串都当「清除」，而这里要区分
            // 「原本就没有覆盖（要 remove）」和「原本有覆盖（要写回去）」。
            if (previous == null) {
                clearLanguage();
            } else {
                languageOverride.set(previous);
            }
        }
    }

    /** 当前线程实际生效的语言码（如 {@code "zh"} / {@code "en"}）。 */
    public static String language() {
        String override = languageOverride.get();
        return override != null ? override : normalize(Locale.getDefault().getLanguage());
    }

    /**
     * 当前线程生效的是不是中文。
     *
     * <p>本枚举只有中英两套骨架文案，所以非中文（包括以后可能出现的第三语言）一律按英文处理。
     * 要区分第三语言的是 {@link Text}，它自己按语言码查。</p>
     */
    public static boolean chinese() {
        return "zh".equals(language());
    }

    private static String normalize(String language) {
        // 只取语言码就够了：zh-CN / zh-TW / zh-Hans 的 getLanguage() 都是 "zh"。
        return language == null ? "" : language.toLowerCase(Locale.ROOT);
    }

    // ==================== 给守卫测试用 ====================

    // 包级可见，不进公开 API。名字不能叫 chinese()：那会和上面的静态 chinese() 撞签名。
    // 暴露出来是为了让 TextGuardTest 用反射遍历全部成员做占位符比对 —— 枚举成员在这之前是守卫的盲区。

    /** 中文原文那一侧。 */
    String chineseTemplate() {
        return chinese;
    }

    /** 英文译文那一侧。 */
    String englishTemplate() {
        return english;
    }
}
