package com.justnothing.testmodule.command.framework.i18n;

import org.junit.Test;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * {@link Text} 的守卫测试。
 *
 * <p>{@code Text.zhEn(中文, 英文)} 的两个模板必须消费<b>同样的参数</b>。对不上时编译器不会报错，
 * 要等那条命令真被执行才抛 {@code MissingFormatArgumentException} 或
 * {@code IllegalFormatConversionException} —— 而命令输出这块几乎没有测试覆盖，
 * 等于把崩溃留到设备上。这个错又偏偏最容易在"加翻译"的时候引入，所以放在这里守。</p>
 *
 * <p>与 {@link CliTextsGuardTest} 的分工：那边守注解文案（id + 查表，靠反射枚举得到），
 * 这边守输出文案（就地写在调用点，反射枚举不到，只能扫源码）。</p>
 *
 * <p>这个守卫的重点是<b>它自己不能静默失效</b>：源码根找不到、或者有 {@code zhEn(} 调用
 * 解析不出两个字面量，都会直接失败，而不是"跳过所以通过"。</p>
 */
public class TextGuardTest {

    /** {@code Text.zhEn} 的声明自己长这样，不是调用点，扫的时候要跳过。 */
    private static final String DECLARATION_FILE = "Text.java";

    /** 源码树最少得有这么多文件，否则就是源码根没定位对 —— 守卫必须在那里就炸，不能静默放过。 */
    private static final int MIN_SOURCE_FILES = 300;

    private static final Pattern ZH_EN_CALL = Pattern.compile("\\bzhEn\\s*\\(");

    private static final Pattern ZH_EN_LITERALS = Pattern.compile(
            "\\bzhEn\\s*\\(\\s*(\"(?:\\\\.|[^\"\\\\])*\")\\s*,\\s*(\"(?:\\\\.|[^\"\\\\])*\")\\s*\\)");

    /** {@code %[参数序号$][标志][宽度][.精度]转换符}。 */
    private static final Pattern SPECIFIER =
            Pattern.compile("%(\\d+\\$)?([-#+ 0,(<]*)(\\d+)?(\\.\\d+)?([a-zA-Z%])");

    /** 一条 {@code zhEn(中文, 英文)} 记录。 */
    private record Pair(String where, String chineseTemplate, String englishTemplate) {
        @Override
        public String toString() {
            return where + "\n      中文: " + chineseTemplate + "\n      英文: " + englishTemplate;
        }
    }

    @Test
    public void bothTemplatesConsumeTheSameArguments() {
        Path root = sourceRoot();
        List<Pair> pairs = new ArrayList<>();
        List<String> unresolved = new ArrayList<>();
        int fileCount = 0;

        try (Stream<Path> files = Files.walk(root)) {
            for (Path file : files.filter(p -> p.toString().endsWith(".java")).toList()) {
                fileCount++;
                if (DECLARATION_FILE.equals(file.getFileName().toString())) {
                    continue;
                }
                String text = read(file);
                for (int start : callStarts(text)) {
                    Matcher m = ZH_EN_LITERALS.matcher(text);
                    m.region(start, text.length());
                    if (!m.lookingAt()) {
                        unresolved.add(file + ":" + lineOf(text, start));
                        continue;
                    }
                    pairs.add(new Pair(file.getFileName() + ":" + lineOf(text, start),
                            unquote(m.group(1)), unquote(m.group(2))));
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        System.out.println("[Text] 扫描 " + fileCount + " 个文件 / 发现 " + pairs.size()
                + " 处 Text.zhEn()");

        assertTrue("源码树只扫到 " + fileCount + " 个文件（少于 " + MIN_SOURCE_FILES
                + "），src/main/java 大概没定位对。守卫在这种情况下会静默放过一切，所以直接失败。",
                fileCount >= MIN_SOURCE_FILES);

        assertTrue("下面这些 Text.zhEn() 调用解析不出两个字面量（参数写成变量了，或者写法超出了本测试的"
                + "识别范围）。要么改成字面量，要么放宽这里的识别规则 —— 但不能让它被静默跳过:\n  "
                + String.join("\n  ", unresolved), unresolved.isEmpty());

        List<String> mismatched = new ArrayList<>();
        for (Pair pair : pairs) {
            Map<Integer, String> zh = specifiersOf(pair.chineseTemplate());
            Map<Integer, String> en = specifiersOf(pair.englishTemplate());
            if (!zh.equals(en)) {
                mismatched.add(pair + "\n      中文占位符: " + zh + "  英文占位符: " + en);
            }
        }

        assertTrue("下面这些文案的中英模板消费的参数对不上，运行到那条命令就会抛异常:\n  "
                + String.join("\n  ", mismatched), mismatched.isEmpty());
    }

    // ==================== 枚举那条链路 ====================

    /**
     * {@link CliMessages} 是「中英并排写在同一个枚举里」，所以它和 {@link Text} 有同一个失败模式：
     * 两种语言的占位符对不上，运行到那条文案就抛异常。
     *
     * <p>它一度是本守卫的盲区 —— 上面那个测试只扫源码里的 {@code Text.zhEn}，看不见枚举成员。
     * 好处是这里不用扫源码：{@code values()} 直接可枚举，比正则稳。</p>
     */
    @Test
    public void cliMessagesEnumTemplatesAlsoHaveMatchingPlaceholders() {
        CliMessages[] all = CliMessages.values();
        assertTrue("CliMessages 一个成员都没有，枚举大概没读对", all.length > 0);

        List<String> mismatched = new ArrayList<>();
        for (CliMessages message : all) {
            Map<Integer, String> zh = specifiersOf(message.chineseTemplate());
            Map<Integer, String> en = specifiersOf(message.englishTemplate());
            if (!zh.equals(en)) {
                mismatched.add(message.name() + "\n      中文占位符: " + zh + "  英文占位符: " + en);
            }
        }

        assertTrue("下面这些 CliMessages 成员的中英模板消费的参数对不上，运行到就会抛异常:\n  "
                + String.join("\n  ", mismatched), mismatched.isEmpty());
    }

    // ==================== 占位符解析 ====================

    /**
     * 把模板归一成「第几个参数 → 转换符」。用参数序号而不是出现顺序当键，
     * 这样中英互换参数顺序（{@code %s %d} 对 {@code %2$d %1$s}）不会被误报。
     *
     * <p>{@code %%} 和 {@code %n} 不消费参数，直接跳过。</p>
     */
    static Map<Integer, String> specifiersOf(String template) {
        Map<Integer, String> byArgument = new LinkedHashMap<>();
        Matcher m = SPECIFIER.matcher(template);
        int position = 0;
        while (m.find()) {
            char conversion = m.group(5).charAt(0);
            if (conversion == '%' || conversion == 'n') {
                continue;
            }
            position++;
            String index = m.group(1);
            int argument = index == null
                    ? position
                    : Integer.parseInt(index.substring(0, index.length() - 1));
            byArgument.put(argument, String.valueOf(conversion));
        }
        return byArgument;
    }

    // ==================== 守卫自己的逻辑也要被测 ====================

    @Test
    public void extractsSpecifiers() {
        assertEquals(Map.of(), specifiersOf(""));
        assertEquals(Map.of(), specifiersOf("无记录"));
        assertEquals(Map.of(1, "s"), specifiersOf("类未找到: %s"));
        assertEquals(Map.of(1, "s", 2, "d"), specifiersOf("共 %s 个，用了 %d 次"));
        assertEquals(Map.of(1, "d"), specifiersOf("已清除 %d 条"));
    }

    /** 字面百分号和新行符不消费参数，否则满篇都是假报警。 */
    @Test
    public void literalPercentAndNewlineConsumeNothing() {
        assertEquals(Map.of(), specifiersOf("100%% 完成"));
        assertEquals(Map.of(), specifiersOf("第一行%n第二行"));
        assertEquals(Map.of(1, "s"), specifiersOf("%s 已完成 100%%"));
    }

    /** 带序号时可以换序，这类必须判成等价，不然会拦住正确的翻译。 */
    @Test
    public void positionalSpecifiersAllowReordering() {
        assertEquals(specifiersOf("%s 用了 %d 次"), specifiersOf("%2$d 次，%1$s"));
    }

    /** 宽度、精度、标志都不能干扰转换符的识别。 */
    @Test
    public void flagsWidthAndPrecisionAreIgnored() {
        assertEquals(Map.of(1, "s"), specifiersOf("%-10s"));
        assertEquals(Map.of(1, "f"), specifiersOf("%5.2f"));
        assertEquals(Map.of(1, "s", 2, "d"), specifiersOf("%-10s|%08d"));
    }

    /** 反例：中英占位符不一致时应该被判出来。这条是守卫本身的"红测"。 */
    @Test
    public void mismatchIsDetected() {
        assertTrue(!specifiersOf("共 %d 条记录").equals(specifiersOf("%d records were found in %s")));
        assertTrue(!specifiersOf("已清除 %d 条").equals(specifiersOf("Cleared %s records")));
    }

    // ==================== 注释识别（守卫自己的边界）====================

    /** javadoc 里解释 API 时写的 {@code Text.zhEn(...)} 是文档，不是调用点。 */
    @Test
    public void javadocMentionIsNotACallSite() {
        String text = "/**\n"
                + " * <p>只用一次的就地写 {@code Text.zhEn(...)}。\n"
                + " * 也可以用 Text.zhEn(zh, en) 这种省略写法。\n"
                + " */\n"
                + "class A { String s = Text.zhEn(\"a\", \"b\").text(); }";
        assertEquals(1, callStarts(text).size());
    }

    /** 行注释同理。 */
    @Test
    public void lineCommentMentionIsNotACallSite() {
        String text = "// 改成 Text.zhEn(\"a\", \"b\") 就行\n"
                + "class A { String s = Text.zhEn(\"c\", \"d\").text(); }";
        assertEquals(1, callStarts(text).size());
    }

    /**
     * 存字符串里的 {@code "//"} 不是注释开始符。这条是防漏报的关键：
     * 要是不认字符串，URL 后面那半行会被当成注释，真正的调用点就被静默跳过了。
     */
    @Test
    public void slashesInsideAStringAreNotAComment() {
        String text = "String u = \"http://example\"; String s = Text.zhEn(\"a\", \"b\").text();";
        assertEquals(1, callStarts(text).size());
    }

    /** 转义引号不能让扫描器提前以为字符串结束了。 */
    @Test
    public void escapedQuoteDoesNotEndTheString() {
        String text = "String s = \"a\\\"//b\"; String t = Text.zhEn(\"a\", \"b\").text();";
        assertEquals(1, callStarts(text).size());
    }

    /** text block 里的 {@code //} 同样不是注释；块结束之后的代码要照常扫。 */
    @Test
    public void textBlockDoesNotSwallowTheCodeAfterIt() {
        String text = "@X(v = \"\"\"\n// 不是注释\n\"\"\")\n"
                + "class A { String s = Text.zhEn(\"a\", \"b\").text(); }";
        assertEquals(1, callStarts(text).size());
    }

    /** 字符字面量里的引号不能让扫描器以为进了字符串（{@code '"'} 是合法字面量）。 */
    @Test
    public void charLiteralWithADoubleQuoteIsHandled() {
        String text = "char q = '\"'; String s = Text.zhEn(\"a\", \"b\").text();";
        assertEquals(1, callStarts(text).size());
    }

    // ==================== 工具 ====================

    private static Path sourceRoot() {
        Path start = Paths.get("").toAbsolutePath();
        for (Path p = start; p != null; p = p.getParent()) {
            for (String relative : new String[] {"src/main/java", "app/src/main/java"}) {
                Path candidate = p.resolve(relative);
                if (Files.isDirectory(candidate)) {
                    return candidate;
                }
            }
        }
        fail("从 " + start + " 往上找不到 src/main/java，无法扫描源码");
        return null;
    }

    private static String read(Path file) throws IOException {
        // 显式 UTF-8：源码是 UTF-8，跟随平台默认字符集会把中文读坏（HookPack 那次就是这个坑）。
        return new String(Files.readAllBytes(file), StandardCharsets.UTF_8);
    }

    /** 每个 {@code zhEn(} 出现的位置，注释里的提及不算。 */
    private static List<Integer> callStarts(String text) {
        boolean[] comment = commentMask(text);
        List<Integer> starts = new ArrayList<>();
        Matcher m = ZH_EN_CALL.matcher(text);
        while (m.find()) {
            if (!comment[m.start()]) {
                starts.add(m.start());
            }
        }
        return starts;
    }

    /** 扫描时的位置状态。 */
    private enum Scan {
        CODE, LINE_COMMENT, BLOCK_COMMENT, STRING, CHAR, TEXT_BLOCK
    }

    /**
     * 标出哪些字符落在注释里。
     *
     * <p>不这么做的话，javadoc 里一句「就地写 {@code Text.zhEn(...)}」就会被当成调用点，
     * 逼着大家写文档时绕着 API 说话 —— 那是守卫在惩罚正确的做法。</p>
     *
     * <p>必须认得字符串、字符字面量和 text block：否则字面量里的一个 {@code "//"}
     * 就会把后面半行当成注释，<b>把真正的调用点漏掉</b>。守卫漏报比误报危险得多，
     * 所以这几条边界都有对应的自测。</p>
     */
    static boolean[] commentMask(String text) {
        boolean[] masked = new boolean[text.length()];
        Scan state = Scan.CODE;

        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            char next = i + 1 < text.length() ? text.charAt(i + 1) : '\0';

            switch (state) {
                case CODE -> {
                    if (c == '/' && next == '/') {
                        masked[i] = masked[i + 1] = true;
                        state = Scan.LINE_COMMENT;
                        i++;
                    } else if (c == '/' && next == '*') {
                        masked[i] = masked[i + 1] = true;
                        state = Scan.BLOCK_COMMENT;
                        i++;
                    } else if (c == '"' && next == '"' && i + 2 < text.length() && text.charAt(i + 2) == '"') {
                        state = Scan.TEXT_BLOCK;
                        i += 2;
                    } else if (c == '"') {
                        state = Scan.STRING;
                    } else if (c == '\'') {
                        state = Scan.CHAR;
                    }
                }
                case LINE_COMMENT -> {
                    if (c == '\n') {
                        state = Scan.CODE;
                    } else {
                        masked[i] = true;
                    }
                }
                case BLOCK_COMMENT -> {
                    masked[i] = true;
                    if (c == '*' && next == '/') {
                        masked[i + 1] = true;
                        state = Scan.CODE;
                        i++;
                    }
                }
                case STRING -> {
                    if (c == '\\') {
                        i++;
                    } else if (c == '"') {
                        state = Scan.CODE;
                    }
                }
                case CHAR -> {
                    if (c == '\\') {
                        i++;
                    } else if (c == '\'') {
                        state = Scan.CODE;
                    }
                }
                case TEXT_BLOCK -> {
                    if (c == '"' && next == '"' && i + 2 < text.length() && text.charAt(i + 2) == '"') {
                        state = Scan.CODE;
                        i += 2;
                    }
                }
            }
        }
        return masked;
    }

    private static int lineOf(String text, int index) {
        int line = 1;
        for (int i = 0; i < index && i < text.length(); i++) {
            if (text.charAt(i) == '\n') {
                line++;
            }
        }
        return line;
    }

    private static String unquote(String literal) {
        return literal.substring(1, literal.length() - 1);
    }
}
