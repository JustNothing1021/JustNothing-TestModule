package com.justnothing.testmodule.command.framework.utils;

import com.justnothing.testmodule.command.framework.annotation.CmdParam;
import com.justnothing.testmodule.command.framework.model.CommandRequest;
import com.justnothing.testmodule.command.framework.model.CommandResult;

import org.junit.Test;

import java.util.List;

import static org.junit.Assert.*;

/**
 * {@link CmdParamProcessor} 命令行解析的行为测试。
 *
 * <p>本测试同时承担两个职责：</p>
 * <ul>
 *   <li>作为拆分 {@code CmdParamProcessor} 巨石类之前的重构安全网；</li>
 *   <li>锁定若干已修复的逻辑缺陷，防止回归（required 校验跳过、requires 数字依赖失效、
 *       可选原始类型默认值失效、操作符 --get/--set 形式不可达、负数不能作为参数值）。</li>
 * </ul>
 */
public class CmdParamProcessorTest {

    private static <T extends CommandRequest<?>> T parse(T request, String... args) {
        CmdParamProcessor.parseCommandLineArgs(request, args);
        return request;
    }

    // ==================== 位置参数 ====================

    @Test
    public void testPositionalParamAssigned() {
        BasicRequest req = parse(new BasicRequest(), "com.example.Foo");
        assertEquals("com.example.Foo", req.className);
    }

    // ==================== 关键字参数 / 别名 / 内联值 ====================

    @Test
    public void testFlagSetsTrue() {
        BasicRequest req = parse(new BasicRequest(), "com.example.Foo", "--verbose");
        assertTrue("--verbose 应置为 true", req.verbose);
    }

    @Test
    public void testFlagAliasSetsTrue() {
        BasicRequest req = parse(new BasicRequest(), "com.example.Foo", "-v");
        assertTrue("-v 别名应置为 true", req.verbose);
    }

    @Test
    public void testInlineKeyValue() {
        BasicRequest req = parse(new BasicRequest(), "com.example.Foo", "--depth=7");
        assertEquals(7, req.depth);
    }

    @Test
    public void testSeparatedKeyValue() {
        BasicRequest req = parse(new BasicRequest(), "com.example.Foo", "--depth", "7");
        assertEquals(7, req.depth);
    }

    @Test
    public void testStringDefaultValueAppliedWhenAbsent() {
        BasicRequest req = parse(new BasicRequest(), "com.example.Foo");
        assertEquals("unknown", req.name);
    }

    /** 修复回归：可选原始类型（int）也须套用 defaultValue（此前因 0 非 null 而被跳过）。 */
    @Test
    public void testOptionalPrimitiveDefaultValueAppliedWhenAbsent() {
        BasicRequest req = parse(new BasicRequest(), "com.example.Foo");
        assertEquals("未提供 --depth 时应套用默认值 3", 3, req.depth);
    }

    /** 修复回归：负数应能作为参数值（此前 --depth -3 会被判为"缺少值"）。 */
    @Test
    public void testNegativeNumberAsValue() {
        BasicRequest req = parse(new BasicRequest(), "com.example.Foo", "--depth", "-3");
        assertEquals(-3, req.depth);
    }

    // ==================== ReadMode 引号处理 ====================

    @Test
    public void testStrippedReadModeRemovesSurroundingQuotes() {
        BasicRequest req = parse(new BasicRequest(), "com.example.Foo", "--name", "\"hello world\"");
        assertEquals("hello world", req.name);
    }

    @Test
    public void testPreservedReadModeKeepsQuotes() {
        BasicRequest req = parse(new BasicRequest(), "com.example.Foo", "--expr", "abc");
        assertEquals("\"abc\"", req.expr);
    }

    // ==================== 反引号合并 ====================

    @Test
    public void testBacktickArgsAreMerged() {
        BasicRequest req = parse(new BasicRequest(), "com.example.Foo", "--name", "`new", "ArrayList()`");
        assertEquals("new ArrayList()", req.name);
    }

    @Test
    public void testSelfClosingBacktickArg() {
        BasicRequest req = parse(new BasicRequest(), "com.example.Foo", "--name", "`java.lang.String`");
        assertEquals("java.lang.String", req.name);
    }

    // ==================== 必填校验 ====================

    @Test
    public void testMissingRequiredParamThrows() {
        try {
            parse(new BasicRequest(), "--verbose");
            fail("缺少必填参数应抛异常");
        } catch (IllegalArgumentException e) {
            assertTrue("错误信息应说明缺少必填参数, 实际: " + e.getMessage(),
                    e.getMessage().contains("缺少必填参数"));
        }
    }

    /**
     * 修复回归：参数为空时也必须执行必填校验。
     * 此前 {@code parseRequest} 在 args 为空时直接 return，导致"必填但留空"的参数被静默放行。
     */
    @Test
    public void testParseRequestWithEmptyArgsStillValidatesRequired() {
        try {
            CmdParamProcessor.parseRequest(new BasicRequest(), new String[0]);
            fail("空参数下缺少必填参数也应抛异常");
        } catch (IllegalArgumentException e) {
            assertTrue("错误信息应说明缺少必填参数, 实际: " + e.getMessage(),
                    e.getMessage().contains("缺少必填参数"));
        }
    }

    @Test
    public void testParseRequestWithArgsParsesDeclaratively() {
        BasicRequest req = new BasicRequest();
        CommandRequest parsed = CmdParamProcessor.parseRequest(req, new String[]{"com.example.Foo"});
        assertSame("非自定义解析器应返回同一实例", req, parsed);
        assertEquals("com.example.Foo", ((BasicRequest) parsed).className);
    }

    // ==================== 取值校验 ====================

    @Test
    public void testAllowedValuesValidation() {
        try {
            parse(new BasicRequest(), "com.example.Foo", "--mode", "turbo");
            fail("不在允许列表的值应抛异常");
        } catch (IllegalArgumentException e) {
            assertTrue("实际: " + e.getMessage(), e.getMessage().contains("不在允许列表"));
        }
    }

    @Test
    public void testMinValidation() {
        try {
            parse(new BasicRequest(), "com.example.Foo", "--count", "0");
            fail("小于最小值应抛异常");
        } catch (IllegalArgumentException e) {
            assertTrue("实际: " + e.getMessage(), e.getMessage().contains("小于最小值"));
        }
    }

    @Test
    public void testMaxValidation() {
        try {
            parse(new BasicRequest(), "com.example.Foo", "--count", "99");
            fail("大于最大值应抛异常");
        } catch (IllegalArgumentException e) {
            assertTrue("实际: " + e.getMessage(), e.getMessage().contains("大于最大值"));
        }
    }

    /**
     * min/max 只能约束数值型参数。若声明在字符串字段上则无法比较大小，
     * 应快速失败而不是静默跳过（否则约束形同虚设）。
     */
    @Test
    public void testMinMaxOnNonNumericFieldThrows() {
        try {
            parse(new BasicRequest(), "com.example.Foo", "--bad-range", "abc");
            fail("字符串字段声明 min/max 应抛异常");
        } catch (IllegalArgumentException e) {
            assertTrue("实际: " + e.getMessage(), e.getMessage().contains("不是数值类型"));
        }
    }

    // ==================== negated flag ====================

    @Test
    public void testNegatedFlagSetsFalse() {
        BasicRequest req = parse(new BasicRequest(), "com.example.Foo", "--no-color");
        assertFalse("--no-color 应把默认为 true 的字段置为 false", req.color);
    }

    // ==================== 互斥 / 依赖约束 ====================

    @Test
    public void testOneWayMutexThrows() {
        try {
            parse(new BasicRequest(), "com.example.Foo", "--json", "--raw");
            fail("单向互斥参数同时使用应抛异常");
        } catch (IllegalArgumentException e) {
            assertTrue("实际: " + e.getMessage(), e.getMessage().contains("互斥"));
        }
    }

    /** 双向声明构成互斥组：无论从哪一侧声明，同时出现一律报错（此前被静默放行）。 */
    @Test
    public void testMutualMutexGroupThrows() {
        try {
            parse(new BasicRequest(), "com.example.Foo", "--wide", "--narrow");
            fail("互斥组内参数同时使用应抛异常");
        } catch (IllegalArgumentException e) {
            assertTrue("实际: " + e.getMessage(), e.getMessage().contains("互斥"));
        }
    }

    @Test
    public void testRequiresThrowsWhenStringDependencyMissing() {
        try {
            parse(new BasicRequest(), "com.example.Foo", "--with-index");
            fail("依赖参数缺失应抛异常");
        } catch (IllegalArgumentException e) {
            assertTrue("实际: " + e.getMessage(), e.getMessage().contains("需要同时指定"));
        }
    }

    @Test
    public void testRequiresSatisfiedDoesNotThrow() {
        BasicRequest req = parse(new BasicRequest(), "com.example.Foo",
                "--with-index", "--index-name", "idx");
        assertTrue(req.withIndex);
        assertEquals("idx", req.indexName);
    }

    /**
     * 修复回归：数字型依赖也必须生效。
     * 此前依赖判定用"字段值启发式"，int 默认 0 非 null 被判为"已使用"，导致依赖恒被视为满足。
     */
    @Test
    public void testRequiresThrowsWhenNumericDependencyMissing() {
        try {
            parse(new BasicRequest(), "com.example.Foo", "--use-index");
            fail("数字型依赖缺失也应抛异常");
        } catch (IllegalArgumentException e) {
            assertTrue("实际: " + e.getMessage(), e.getMessage().contains("需要同时指定"));
        }
    }

    @Test
    public void testRequiresSatisfiedWithNumericDependency() {
        BasicRequest req = parse(new BasicRequest(), "com.example.Foo", "--use-index", "--index", "2");
        assertTrue(req.useIndex);
        assertEquals(2, req.index);
    }

    // ==================== 操作符 ====================

    /** 修复回归：文档主用写法 --set 必须能消费后续参数（此前会被当普通 flag）。 */
    @Test
    public void testOperatorConsumesArgsWithDoubleDashForm() {
        OperatorRequest req = parse(new OperatorRequest(), "--set", "com.example.Foo", "bar");

        assertTrue("操作符标志应被置为 true", req.setOp);
        assertEquals("第一个参数应分配给 operatorIndex=1 的子字段", "com.example.Foo", req.className);
        assertEquals("第二个参数应分配给 operatorIndex=2 的子字段", "bar", req.fieldName);
        assertTrue("操作符应被记录到 receivedOperators", req.hasOperator("set"));
    }

    @Test
    public void testOperatorConsumesArgsWithSingleDashForm() {
        OperatorRequest req = parse(new OperatorRequest(), "-s", "com.example.Foo", "bar");

        assertTrue(req.setOp);
        assertEquals("com.example.Foo", req.className);
        assertEquals("bar", req.fieldName);
        assertTrue(req.hasOperator("set"));
    }

    @Test
    public void testOperatorConsumesArgsWithBareWordForm() {
        OperatorRequest req = parse(new OperatorRequest(), "set", "com.example.Foo", "bar");

        assertTrue(req.setOp);
        assertEquals("com.example.Foo", req.className);
        assertEquals("bar", req.fieldName);
        assertTrue(req.hasOperator("set"));
    }

    // ==================== varArgs ====================

    /** 修复回归：关键字形式的 varArgs 必须能收集数字（此前正数被误判为数值而提前终止收集）。 */
    @Test
    public void testKeywordVarArgsCollectsNumericValues() {
        BasicRequest req = parse(new BasicRequest(), "com.example.Foo", "--ids", "1", "2", "3");
        assertEquals("应收集全部 3 个数字", 3, req.ids.size());
        assertEquals("1", req.ids.get(0));
        assertEquals("3", req.ids.get(2));
    }

    @Test
    public void testKeywordVarArgsStopsAtNextOption() {
        BasicRequest req = parse(new BasicRequest(), "com.example.Foo", "--ids", "a", "b", "--verbose");
        assertEquals("应在遇到下一个选项时停止收集", 2, req.ids.size());
        assertTrue(req.verbose);
    }

    /** 位置 varArgs：消费普通位置参数分配后剩余的候选 token。 */
    @Test
    public void testPositionalVarArgsCollectsRemaining() {
        BasicRequest req = parse(new BasicRequest(), "com.example.Foo", "left", "right");
        assertEquals("com.example.Foo", req.className);
        assertEquals("left right", req.tail);
    }

    // ==================== 帮助文档生成 ====================

    @Test
    public void testGenerateHelpTextContainsParamNames() {
        String help = CmdParamProcessor.generateHelpText(BasicRequest.class);
        assertTrue("帮助应包含参数名, 实际: " + help, help.contains("className"));
        assertTrue("帮助应包含参数名, 实际: " + help, help.contains("--verbose"));
        assertTrue("帮助应包含参数描述, 实际: " + help, help.contains("类名"));
    }

    // ==================== 测试用请求类 ====================

    public static class BasicRequest extends CommandRequest<CommandResult> {

        @CmdParam(name = "className", description = "类名", required = true, position = 1)
        String className;

        @CmdParam(name = "--verbose", description = "详细输出", aliases = {"-v"})
        boolean verbose;

        @CmdParam(name = "--depth", description = "深度", defaultValue = "3")
        int depth;

        @CmdParam(name = "--name", description = "名称", defaultValue = "unknown")
        String name;

        @CmdParam(name = "--expr", description = "表达式", readMode = CmdParam.ReadMode.PRESERVED)
        String expr;

        @CmdParam(name = "--mode", description = "模式", allowedValues = {"fast", "slow"})
        String mode;

        @CmdParam(name = "--count", description = "数量", min = 1, max = 10)
        int count;

        @CmdParam(name = "--no-color", description = "关闭颜色", isNegated = true)
        boolean color = true;

        @CmdParam(name = "--json", description = "JSON 输出", mutexWith = {"--raw"})
        boolean json;

        @CmdParam(name = "--raw", description = "原始输出")
        boolean raw;

        @CmdParam(name = "--wide", description = "宽", mutexWith = {"--narrow"})
        boolean wide;

        @CmdParam(name = "--narrow", description = "窄", mutexWith = {"--wide"})
        boolean narrow;

        @CmdParam(name = "--with-index", description = "带索引", requires = {"--index-name"})
        boolean withIndex;

        @CmdParam(name = "--index-name", description = "索引名")
        String indexName;

        @CmdParam(name = "--use-index", description = "使用索引", requires = {"--index"})
        boolean useIndex;

        @CmdParam(name = "--index", description = "索引")
        int index;

        @CmdParam(name = "--ids", description = "ID 列表", varArgs = true)
        List<String> ids;

        @CmdParam(name = "--bad-range", description = "错误声明：字符串无法比较大小", min = 1, max = 10)
        String badRange;

        @CmdParam(name = "tail", description = "尾部（位置 varArgs）", position = 2, varArgs = true)
        String tail;
    }

    public static class OperatorRequest extends CommandRequest<CommandResult> {

        @CmdParam(name = "--set", description = "设置", aliases = {"-s", "set"},
                isOperator = true, operatorArgs = 2, belongsToOperator = "set")
        boolean setOp;

        @CmdParam(name = "className", description = "类名",
                belongsToOperator = "set", operatorIndex = 1)
        String className;

        @CmdParam(name = "fieldName", description = "字段名",
                belongsToOperator = "set", operatorIndex = 2)
        String fieldName;
    }
}
