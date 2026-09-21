package com.justnothing.testmodule.command.framework.i18n;

import org.junit.After;
import org.junit.Test;

import java.util.Locale;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * {@link Text}（就地成对写法的输出文案）的行为测试。
 *
 * <p>守的都是「改错了照样编译过、运行时静默出错」的地方：惰性求值、第三语言的回落、
 * 未知语言不炸、以及提取成常量之后仍然跟随当次请求的语言。</p>
 */
public class TextTest {

    /** 故意做成 static final：用来验证"构造时不读语言"这条性质。 */
    private static final Text NO_RECORDS = Text.zhEn("无记录", "No records");

    private static final Text CLASS_NOT_FOUND =
            Text.zhEn("未找到类 %s", "Class not found: %s")
                    .with("ja", "クラスが見つかりません: %s");

    @After
    public void tearDown() {
        CliMessages.clearLanguage();
    }

    @Test
    public void resolvesChineseAndEnglish() {
        CliMessages.useLanguage("zh");
        assertEquals("无记录", NO_RECORDS.text());
        CliMessages.useLanguage("en");
        assertEquals("No records", NO_RECORDS.text());
    }

    @Test
    public void formatSubstitutesArguments() {
        CliMessages.useLanguage("zh");
        assertEquals("未找到类 Foo", CLASS_NOT_FOUND.format("Foo"));
        CliMessages.useLanguage("en");
        assertEquals("Class not found: Foo", CLASS_NOT_FOUND.format("Foo"));
    }

    @Test
    public void thirdLanguageWinsWhenPresent() {
        CliMessages.useLanguage("ja");
        assertEquals("クラスが見つかりません: Foo", CLASS_NOT_FOUND.format("Foo"));
    }

    /** 没写这种语言的文案不允许空白、不允许抛异常，要静默回落英文。 */
    @Test
    public void missingThirdLanguageFallsBackToEnglish() {
        CliMessages.useLanguage("ja");
        assertEquals("No records", NO_RECORDS.text());
    }

    /** 以后出现第四种语言时，没有翻译的语言同样按英文处理。 */
    @Test
    public void unknownLanguageUsesEnglish() {
        CliMessages.useLanguage("fr");
        assertEquals("No records", NO_RECORDS.text());
    }

    /**
     * 关键性质：{@code static final} 常量不会被类初始化时的语言冻住。
     * 如果哪天有人把 text() 改成构造时就求值，这里会立刻红。
     */
    @Test
    public void constantFollowsTheCurrentLanguage() {
        CliMessages.useLanguage("zh");
        assertEquals("无记录", NO_RECORDS.text());
        CliMessages.useLanguage("en");
        assertEquals("No records", NO_RECORDS.text());
        CliMessages.useLanguage("zh");
        assertEquals("无记录", NO_RECORDS.text());
    }

    /** 客户端没声明语言时，跟随本进程 Locale —— 与 CliMessages.chinese() 同一套判定。 */
    @Test
    public void undeclaredLanguageFallsBackToProcessLocale() {
        Locale previous = Locale.getDefault();
        try {
            CliMessages.clearLanguage();

            Locale.setDefault(Locale.US);
            assertEquals("No records", NO_RECORDS.text());

            Locale.setDefault(Locale.CHINA);
            assertEquals("无记录", NO_RECORDS.text());
        } finally {
            Locale.setDefault(previous);
        }
    }

    /** 语言码大小写不敏感：客户端送来 "JA" 也要能和 with("ja", ..) 对上。 */
    @Test
    public void languageCodeIsCaseInsensitive() {
        CliMessages.useLanguage("JA");
        assertEquals("クラスが見つかりません: Foo", CLASS_NOT_FOUND.format("Foo"));
        CliMessages.useLanguage("ZH");
        assertEquals("未找到类 Foo", CLASS_NOT_FOUND.format("Foo"));
    }

    // ==================== 跨线程带语言（withLanguage）====================

    /** 后台任务按发起它的那条命令的语言渲染，而不是本进程 Locale。 */
    @Test
    public void withLanguageRendersInTheGivenLanguage() {
        Locale previous = Locale.getDefault();
        try {
            Locale.setDefault(Locale.CHINA);   // 本进程是中文

            CliMessages.useLanguage("en");     // 但这条命令的客户要英文
            String captured = CliMessages.language();

            String[] rendered = new String[1];
            CliMessages.withLanguage(captured, () -> rendered[0] = NO_RECORDS.text());
            assertEquals("No records", rendered[0]);
        } finally {
            Locale.setDefault(previous);
        }
    }

    /** 池线程是复用的，跑完必须把原来的覆盖写回去，不能把语言泄漏给下一个任务。 */
    @Test
    public void withLanguageRestoresThePreviousOverride() {
        CliMessages.useLanguage("ja");
        CliMessages.withLanguage("en", () -> assertEquals("No records", NO_RECORDS.text()));
        assertEquals("クラスが見つかりません: Foo", CLASS_NOT_FOUND.format("Foo"));
    }

    /**
     * 原本没有覆盖时，跑完要回到「跟随本进程 Locale」，而不是留一个覆盖在那里 ——
     * 后者会让池线程上的下一个任务误以为客户端声明了语言。
     */
    @Test
    public void withLanguageRestoresToProcessLocaleWhenThereWasNoOverride() {
        Locale previous = Locale.getDefault();
        try {
            Locale.setDefault(Locale.US);
            CliMessages.clearLanguage();

            CliMessages.withLanguage("zh", () -> assertEquals("无记录", NO_RECORDS.text()));

            // 覆盖已清掉：此时改 Locale 应该能立刻反应出来
            Locale.setDefault(Locale.CHINA);
            assertEquals("无记录", NO_RECORDS.text());
            Locale.setDefault(Locale.US);
            assertEquals("No records", NO_RECORDS.text());
        } finally {
            Locale.setDefault(previous);
        }
    }

    /** 语言传 null 是「这个任务没声明语言」，等价于不设覆盖，不能炸。 */
    @Test
    public void withLanguageAcceptsNull() {
        Locale previous = Locale.getDefault();
        try {
            Locale.setDefault(Locale.US);
            CliMessages.useLanguage("zh");

            CliMessages.withLanguage(null, () -> assertEquals("No records", NO_RECORDS.text()));

            assertEquals("无记录", NO_RECORDS.text());
        } finally {
            Locale.setDefault(previous);
        }
    }

    /** body 抛异常也要恢复 —— 否则后台任务一旦出错，池线程的语言就永远歪了。 */
    @Test
    public void withLanguageRestoresEvenWhenBodyThrows() {
        CliMessages.useLanguage("zh");
        try {
            CliMessages.withLanguage("en", () -> {
                throw new IllegalStateException("boom");
            });
            fail("body 的异常应该原样抛出来");
        } catch (IllegalStateException expected) {
            assertEquals("boom", expected.getMessage());
        }
        assertEquals("无记录", NO_RECORDS.text());
    }
}
