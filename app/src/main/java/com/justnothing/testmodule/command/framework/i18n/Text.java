package com.justnothing.testmodule.command.framework.i18n;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * 一条可本地化的文案：中英并排写在一起，<b>取值时才看当前语言</b>。
 *
 * <h3>为什么不走 id</h3>
 * 注解里的文案只能走 {@code id 常量 + CliTexts 查表}：注解值必须是编译期常量，运行时换不了语言
 * （见 {@link CliMessages} 的 javadoc）。输出文案没有这个约束，就地成对写要省得多 ——
 * 不需要 id 常量、不需要登记表、不会有孤儿 id，也不会多出一张要养的守卫测试。
 * 代价是文案散在各个调用点，想集中看一遍的话按 {@code zhEn(} 搜即可。
 *
 * <h3>为什么是「惰性」的</h3>
 * 构造时<b>不读语言</b>，只在 {@link #text()} / {@link #format} 里读。于是：
 * <ul>
 *   <li>可以写成 {@code private static final Text X = Text.zhEn(...)} 当常量用，
 *       类初始化不会把语言冻住，每次取都跟随当次请求的语言；</li>
 *   <li>同一条文案能被不同客户端复用 —— 服务端一个进程可能同时在用不同语言服务不同客户端。</li>
 * </ul>
 *
 * <h3>语言比两种多的时候</h3>
 * {@link #with} 追加任意语言码。取值顺序是：当前语言 → 英文。英文在 {@link #zhEn} 里是必填的，
 * 所以永远兜得住，缺翻译只会回落、不会空白或抛异常。
 *
 * <p>占位符一律用 {@link String#format} 语义。真出现第三种语言时模板可能要改用带序号的写法
 * （{@code %1$s}），因为语序会变 —— 所以这里从一开始就别用字符串拼接。</p>
 */
public final class Text {

    private static final String CHINESE = "zh";
    private static final String ENGLISH = "en";

    private final String chinese;
    private final String english;

    /** 第三种语言起才用得上的东西；只有中英两种时是 null，不额外分配。 */
    private final Map<String, String> extras;

    private Text(String chinese, String english, Map<String, String> extras) {
        this.chinese = chinese;
        this.english = english;
        this.extras = extras;
    }

    /** 中英并排的快捷写法：绝大多数文案只要这个。 */
    public static Text zhEn(String chinese, String english) {
        return new Text(chinese, english, null);
    }

    /**
     * 追加一种语言，返回<b>新的</b> {@link Text}，不改自己。
     *
     * <p>注意这条链是"不可变的"，所以 {@code Text.zhEn(..).with("ja", ..);} 这种<b>不接返回值</b>
     * 的写法是静默空操作。要么把它接住，要么（推荐）直接提取成常量：</p>
     * <pre>
     * private static final Text NOT_FOUND =
     *         Text.zhEn("未找到类 %s", "Class not found: %s")
     *             .with("ja", "クラスが見つかりません: %s");
     * </pre>
     *
     * @param language 语言码，如 {@code "ja"}、{@code "ko"}
     * @param template 该语言下的模板
     */
    public Text with(String language, String template) {
        Map<String, String> merged = extras == null ? new HashMap<>() : new HashMap<>(extras);
        merged.put(language.toLowerCase(Locale.ROOT), template);
        return new Text(chinese, english, merged);
    }

    /** 当前语言下的文案。 */
    public String text() {
        String language = CliMessages.language();
        if (CHINESE.equals(language)) {
            return chinese;
        }
        if (extras != null) {
            String extra = extras.get(language);
            if (extra != null) {
                return extra;
            }
        }
        // 没写这个语言就回落英文。英文是 zhEn() 的必填项，所以这里一定兜得住。
        return english;
    }

    /** 当前语言下的文案，带 {@link String#format} 参数。 */
    public String format(Object... args) {
        return String.format(text(), args);
    }
}
