package com.justnothing.testmodule.command.framework;

import com.justnothing.testmodule.command.framework.model.CommandResult;
import com.justnothing.testmodule.command.framework.utils.CommandArgumentParser;
import com.justnothing.testmodule.utils.io.IOManager;
import com.justnothing.testmodule.utils.io.ShellExecutionException;
import com.justnothing.testmodule.utils.io.ShellType;

import java.io.IOException;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * 命令框架核心组件的纯 Java 单元测试。
 * <p>
 * 不依赖任何 Android API，可在电脑上直接运行。
 * 覆盖 {@link CommandResult}、{@link CommandArgumentParser}、
 * {@link ShellType}、{@link ShellExecutionException}、{@link IOManager.ProcessResult}。
 * </p>
 */
public class FrameworkCoreTest {

    // ==================== CommandResult 测试 ====================

    @Test
    public void testCommandResultDefaultValues() {
        CommandResult result = new CommandResult();
        assertTrue("默认 success 应为 true", result.isSuccess());
        assertNull("默认 requestId 应为 null", result.getRequestId());
        assertNull("默认 message 应为 null", result.getMessage());
        assertNull("默认 error 应为 null", result.getError());
        assertNull("默认 data 应为 null", result.getData());
    }

    @Test
    public void testCommandResultWithRequestId() {
        CommandResult result = new CommandResult("req-001");
        assertEquals("requestId 应匹配", "req-001", result.getRequestId());
        assertTrue(result.isSuccess());
    }

    @Test
    public void testCommandResultSetError() {
        CommandResult result = new CommandResult();
        result.setError(new CommandResult.ErrorInfo("ERR_001", "something went wrong"));
        assertFalse("设置 error 后 success 应为 false", result.isSuccess());
        assertNotNull("error 不应为 null", result.getError());
        assertEquals("error code 应匹配", "ERR_001", result.getError().getCode());
        assertEquals("error message 应匹配", "something went wrong", result.getError().getMessage());
    }

    @Test
    public void testCommandResultToJsonString() {
        CommandResult result = new CommandResult("req-002");
        result.setMessage("操作成功");
        result.setData("some data");

        String json = result.toJsonString();
        assertNotNull("JSON 不应为 null", json);
        assertTrue("JSON 应包含 requestId", json.contains("req-002"));
        assertTrue("JSON 应包含 success:true", json.contains("\"success\":true"));
    }

    @Test
    public void testCommandResultFromJsonString() {
        String json = "{\"requestId\":\"req-003\",\"success\":true,\"message\":\"test\"}";
        CommandResult result = new CommandResult().fromJsonString(json);
        assertEquals("requestId 应反序列化", "req-003", result.getRequestId());
        assertTrue("success 应反序列化", result.isSuccess());
        assertEquals("message 应反序列化", "test", result.getMessage());
    }

    @Test
    public void testCommandResultRoundTrip() {
        CommandResult original = new CommandResult("req-004");
        original.setMessage("round trip test");
        original.setData(42);

        String json = original.toJsonString();
        CommandResult restored = new CommandResult().fromJsonString(json);

        assertEquals("requestId 应一致", original.getRequestId(), restored.getRequestId());
        assertEquals("success 应一致", original.isSuccess(), restored.isSuccess());
        assertEquals("message 应一致", original.getMessage(), restored.getMessage());
    }

    @Test
    public void testCommandResultErrorInfo() {
        CommandResult.ErrorInfo error = new CommandResult.ErrorInfo("ERR_002", "错误信息", "stacktrace here");
        assertEquals("code", "ERR_002", error.getCode());
        assertEquals("message", "错误信息", error.getMessage());
        assertEquals("stacktrace", "stacktrace here", error.getStacktrace());
    }

    @Test
    public void testCommandResultErrorInfoFromThrowable() {
        Exception ex = new RuntimeException("test exception");
        CommandResult.ErrorInfo error = new CommandResult.ErrorInfo("ERR_003", "出错了", ex);
        assertTrue("stacktrace 应包含异常信息", error.getStacktrace().contains("test exception"));
        assertTrue("stacktrace 应包含 RuntimeException", error.getStacktrace().contains("RuntimeException"));
    }

    // ==================== CommandArgumentParser 测试 ====================

    @Test
    public void testSplitArgumentsEmpty() {
        assertArrayEquals("空字符串应返回空数组",
                new String[0], CommandArgumentParser.splitArguments(""));
        assertArrayEquals("null 应返回空数组",
                new String[0], CommandArgumentParser.splitArguments(null));
        assertArrayEquals("纯空格应返回空数组",
                new String[0], CommandArgumentParser.splitArguments("   "));
    }

    @Test
    public void testSplitArgumentsSimple() {
        String[] result = CommandArgumentParser.splitArguments("cmd arg1 arg2");
        assertArrayEquals("简单参数分割", new String[]{"cmd", "arg1", "arg2"}, result);
    }

    @Test
    public void testSplitArgumentsWithQuotes() {
        String[] result = CommandArgumentParser.splitArguments("cmd \"arg1 with space\" arg2");
        assertEquals("带引号应保留 3 个参数", 3, result.length);
        assertEquals("引号内空格应保留", "\"arg1 with space\"", result[1]);
        assertEquals("第三个参数", "arg2", result[2]);
    }

    @Test
    public void testSplitArgumentsWithSingleQuotes() {
        String[] result = CommandArgumentParser.splitArguments("cmd 'single quoted' arg2");
        assertEquals(3, result.length);
        assertEquals("单引号内容", "'single quoted'", result[1]);
    }

    @Test
    public void testSplitArgumentsWithBackticks() {
        String[] result = CommandArgumentParser.splitArguments("cmd `expr 1 + 2` arg2");
        assertEquals("反引号内容应作为一个参数", 3, result.length);
        assertEquals("反引号表达式", "expr 1 + 2", result[1]);
    }

    @Test
    public void testSplitArgumentsWithTabs() {
        String[] result = CommandArgumentParser.splitArguments("cmd\targ1\targ2");
        assertArrayEquals("Tab 分隔", new String[]{"cmd", "arg1", "arg2"}, result);
    }

    @Test
    public void testHasOption() {
        String[] args = {"cmd", "-v", "--verbose", "arg1"};
        assertTrue("应找到 -v", CommandArgumentParser.hasOption(args, "-v"));
        assertTrue("应找到 --verbose", CommandArgumentParser.hasOption(args, "--verbose"));
        assertFalse("不应找到 -x", CommandArgumentParser.hasOption(args, "-x"));
    }

    @Test
    public void testGetOptionValue() {
        String[] args = {"cmd", "-o", "value", "arg1"};
        assertEquals("应获取选项值", "value", CommandArgumentParser.getOptionValue(args, "-o"));
        assertNull("不存在的选项应返回 null", CommandArgumentParser.getOptionValue(args, "-x"));
    }

    @Test
    public void testParseId() {
        String[] args = {"cmd", "42", "99"};
        assertEquals(Integer.valueOf(42), CommandArgumentParser.parseId(args, 1));
        assertEquals(Integer.valueOf(99), CommandArgumentParser.parseId(args, 2));
        assertNull("越界应返回 null", CommandArgumentParser.parseId(args, 5));
        assertNull("非数字应返回 null", CommandArgumentParser.parseId(new String[]{"cmd", "abc"}, 1));
    }

    @Test
    public void testParseInt() {
        String[] args = {"cmd", "42"};
        assertEquals(Integer.valueOf(42), CommandArgumentParser.parseInt(args, 1, "测试字段"));
        assertNull("越界应返回 null", CommandArgumentParser.parseInt(args, 5, "测试字段"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testParseIntInvalid() {
        CommandArgumentParser.parseInt(new String[]{"cmd", "abc"}, 1, "测试字段");
    }

    @Test
    public void testParseLong() {
        String[] args = {"cmd", "12345678901"};
        assertEquals(Long.valueOf(12345678901L), CommandArgumentParser.parseLong(args, 1, "测试字段"));
        assertNull("越界应返回 null", CommandArgumentParser.parseLong(args, 5, "测试字段"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testParseLongInvalid() {
        CommandArgumentParser.parseLong(new String[]{"cmd", "abc"}, 1, "测试字段");
    }

    @Test
    public void testRequireMin() {
        CommandArgumentParser.requireMin(5, 1, "测试");
        // 正常通过，无异常
    }

    @Test(expected = IllegalArgumentException.class)
    public void testRequireMinFail() {
        CommandArgumentParser.requireMin(0, 1, "测试");
    }

    @Test
    public void testRequireMax() {
        CommandArgumentParser.requireMax(5, 10, "测试");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testRequireMaxFail() {
        CommandArgumentParser.requireMax(15, 10, "测试");
    }

    @Test
    public void testRequireRange() {
        CommandArgumentParser.requireRange(5, 1, 10, "测试");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testRequireRangeFail() {
        CommandArgumentParser.requireRange(15, 1, 10, "测试");
    }

    @Test
    public void testRequireArgsLength() {
        CommandArgumentParser.requireArgsLength(new String[]{"a", "b", "c"}, 2);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testRequireArgsLengthFail() {
        CommandArgumentParser.requireArgsLength(new String[]{"a"}, 2);
    }

    @Test
    public void testParseOptionsWithClassLoader() {
        String cmdline = "-cl myClassLoader exec cmd";
        CommandArgumentParser.ParseResult result = CommandArgumentParser.parseOptions(cmdline, null);
        assertEquals("exec cmd", result.commandLine());
        assertEquals("myClassLoader", result.classLoader());
    }

    @Test
    public void testParseOptionsWithoutClassLoader() {
        String cmdline = "exec cmd";
        CommandArgumentParser.ParseResult result = CommandArgumentParser.parseOptions(cmdline, null);
        assertEquals("exec cmd", result.commandLine());
        assertNull(result.classLoader());
    }

    // ==================== ShellType 测试 ====================

    @Test
    public void testShellTypeValues() {
        assertEquals(4, ShellType.values().length);
        assertEquals(ShellType.ROOT, ShellType.fromCode("root"));
        assertEquals(ShellType.LOCAL, ShellType.fromCode("local"));
        assertEquals(ShellType.MOCK, ShellType.fromCode("mock"));
        assertEquals(ShellType.ADB, ShellType.fromCode("adb"));
    }

    @Test
    public void testShellTypeFromCodeCaseInsensitive() {
        assertEquals(ShellType.ROOT, ShellType.fromCode("ROOT"));
        assertEquals(ShellType.LOCAL, ShellType.fromCode("LOCAL"));
    }

    @Test
    public void testShellTypeFromCodeNull() {
        assertNull(ShellType.fromCode(null));
        assertNull(ShellType.fromCode("unknown"));
    }

    @Test
    public void testShellTypeGetCode() {
        assertEquals("root", ShellType.ROOT.getCode());
        assertEquals("local", ShellType.LOCAL.getCode());
    }

    @Test
    public void testShellTypeGetDescription() {
        assertNotNull(ShellType.ROOT.getDescription());
        assertNotNull(ShellType.LOCAL.getDescription());
    }

    // ==================== ShellExecutionException 测试 ====================

    @Test
    public void testShellExecutionExceptionSimple() {
        ShellExecutionException ex = new ShellExecutionException("test error");
        assertEquals("test error", ex.getMessage());
        assertNull(ex.getCommand());
        assertNull(ex.getExecutorType());
    }

    @Test
    public void testShellExecutionExceptionWithCause() {
        Throwable cause = new RuntimeException("root cause");
        ShellExecutionException ex = new ShellExecutionException("test error", cause);
        assertEquals("test error", ex.getMessage());
        assertSame(cause, ex.getCause());
    }

    @Test
    public void testShellExecutionExceptionWithCommand() {
        ShellExecutionException ex = new ShellExecutionException("ls -la", ShellType.LOCAL, "permission denied");
        assertEquals("ls -la", ex.getCommand());
        assertEquals(ShellType.LOCAL, ex.getExecutorType());
        assertTrue("消息应包含命令信息", ex.getMessage().contains("ls -la"));
        assertTrue("消息应包含 LOCAL", ex.getMessage().contains("LOCAL"));
    }

    @Test
    public void testShellExecutionExceptionWithCommandAndCause() {
        Throwable cause = new IOException("IO error");
        ShellExecutionException ex = new ShellExecutionException("cat /proc/version", ShellType.ROOT, "read failed", cause);
        assertEquals("cat /proc/version", ex.getCommand());
        assertEquals(ShellType.ROOT, ex.getExecutorType());
        assertSame(cause, ex.getCause());
    }

    // ==================== IOManager.ProcessResult 测试 ====================

    @Test
    public void testProcessResultSuccess() {
        IOManager.ProcessResult result = new IOManager.ProcessResult(0, "ok", "", 100);
        assertTrue(result.isSuccess());
        assertEquals("ok", result.stdout());
        assertEquals("", result.stderr());
        assertEquals(100, result.executionTime());
    }

    @Test
    public void testProcessResultFailure() {
        IOManager.ProcessResult result = new IOManager.ProcessResult(1, "", "error", 50);
        assertFalse(result.isSuccess());
        assertEquals("error", result.stderr());
    }

    @Test
    public void testProcessResultCompactConstructor() {
        IOManager.ProcessResult result = new IOManager.ProcessResult(0, "output", "error");
        assertEquals(0, result.executionTime());
        assertTrue(result.isSuccess());
    }

    @Test
    public void testProcessResultGetOutput() {
        IOManager.ProcessResult result = new IOManager.ProcessResult(0, "stdout content", "stderr content", 0);
        assertEquals("stdout content", result.getOutput());
    }

    @Test
    public void testProcessResultGetOutputReturnsStdout() {
        IOManager.ProcessResult result = new IOManager.ProcessResult(1, "", "stderr content", 0);
        assertEquals("getOutput 应返回 stdout（空字符串）", "", result.getOutput());
    }

    @Test
    public void testProcessResultGetError() {
        IOManager.ProcessResult result = new IOManager.ProcessResult(1, "", "stderr content", 0);
        assertEquals("stderr content", result.getError());
    }

    @Test
    public void testProcessResultGetOutputBothEmpty() {
        IOManager.ProcessResult result = new IOManager.ProcessResult(0, "", "", 0);
        assertEquals("", result.getOutput());
    }
}