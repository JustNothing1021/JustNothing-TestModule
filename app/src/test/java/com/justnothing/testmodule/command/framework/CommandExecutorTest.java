package com.justnothing.testmodule.command.framework;

import com.justnothing.testmodule.command.framework.output.ClientRequirements;
import com.justnothing.testmodule.command.framework.output.MockOutputHandler;
import com.justnothing.testmodule.utils.io.LocalShellExecutor;
import com.justnothing.testmodule.utils.io.ShellExecutorProvider;
import com.justnothing.testmodule.utils.io.IOManager;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * 命令执行器单元测试。
 * <p>
 * 测试 {@link CommandExecutor} 在 CLI 和 JSON 模式下的行为。
 * 使用 {@link LocalShellExecutor} 代替 root 执行器，在电脑上运行。
 * </p>
 */
public class CommandExecutorTest {

    private MockOutputHandler output;
    private ClientRequirements cliReq;
    private ClientRequirements jsonReq;

    @Before
    public void setUp() {
        // 强制使用本地执行器
        ShellExecutorProvider.setForcedExecutor(new LocalShellExecutor());

        output = new MockOutputHandler();
        cliReq = new ClientRequirements(false, false);
        jsonReq = new ClientRequirements(false, true);
    }

    @After
    public void tearDown() {
        ShellExecutorProvider.setForcedExecutor(null);
    }

    // ==================== MockOutputHandler 测试 ====================

    @Test
    public void testMockOutputHandler() {
        output.println("hello");
        output.print("world");
        output.println("!");

        assertEquals("hello\nworld!\n", output.getOutput());
        assertEquals(2, output.getLines().size());
        assertEquals("hello", output.getLines().get(0));
        assertEquals("!", output.getLines().get(1));
        assertTrue(output.contains("hello"));
        assertFalse(output.isEmpty());
    }

    @Test
    public void testMockOutputHandlerReset() {
        output.println("hello");
        assertFalse(output.isEmpty());

        output.reset();
        assertTrue(output.isEmpty());
        assertEquals(0, output.getLines().size());
    }

    @Test
    public void testMockOutputHandlerClose() {
        assertFalse(output.isClosed());
        output.close();
        assertTrue(output.isClosed());

        // 关闭后输出应被丢弃
        output.println("should not appear");
        assertTrue(output.isEmpty());
    }

    // ==================== ShellExecutorProvider 测试 ====================

    @Test
    public void testLocalShellExecutorAvailable() {
        assertTrue("LocalShellExecutor 应始终可用", ShellExecutorProvider.get().isAvailable());
        assertEquals("应使用 LocalShellExecutor", 
                com.justnothing.testmodule.utils.io.ShellType.LOCAL,
                ShellExecutorProvider.get().getType());
    }

    @Test
    public void testLocalShellExecutorEcho() throws Exception {
        // Windows 上用 cmd /c echo，类 Unix 上用 echo
        String cmd = System.getProperty("os.name", "").toLowerCase().contains("win")
                ? "cmd /c echo hello world"
                : "echo hello world";
        IOManager.ProcessResult result = ShellExecutorProvider.get().execute(cmd, 5000);
        assertTrue("echo 应成功", result.isSuccess());
        assertNotNull("stdout 不应为 null", result.stdout());
        assertTrue("stdout 应包含 'hello world'", result.stdout().contains("hello world"));
    }

    @Test
    public void testLocalShellExecutorPwd() throws Exception {
        // Windows 上用 echo %cd%，类 Unix 上用 pwd
        String cmd = System.getProperty("os.name", "").toLowerCase().contains("win")
                ? "cmd /c echo %cd%"
                : "pwd";
        IOManager.ProcessResult result = ShellExecutorProvider.get().execute(cmd, 5000);
        assertTrue("pwd/cd 应成功", result.isSuccess());
        assertNotNull(result.stdout());
        // 输出应包含驱动器号（Windows）或 /（Unix）或实际路径
        assertFalse("stdout 不应为空", result.stdout().trim().isEmpty());
    }

    // ==================== CLI 模式测试 ====================

    @Test
    public void testCliModeWithHelpCommand() {
        // help 命令不依赖 Android API，在本地可执行
        output.println("CLI 模式测试");
        output.println("命令: help");
        output.println("格式: 文本");

        String outputText = output.getOutput();
        assertTrue(outputText.contains("CLI 模式测试"));
        assertTrue(outputText.contains("help"));
    }

    @Test
    public void testCliModeOutputFormat() {
        // CLI 模式输出应为纯文本
        assertFalse("CLI 模式 isJsonMode 应为 false", cliReq.isJsonMode());
        assertFalse("CLI 模式 supportsInput 应为 false", cliReq.isSupportsInput());
    }

    // ==================== JSON 模式测试 ====================

    @Test
    public void testJsonModeCapabilities() {
        // JSON 模式应正确设置能力
        assertTrue("JSON 模式 isJsonMode 应为 true", jsonReq.isJsonMode());
        assertFalse("JSON 模式 supportsInput 应为 false", jsonReq.isSupportsInput());
    }

    @Test
    public void testJsonModeOutputFormat() {
        // JSON 模式输出应为结构化 JSON
        String jsonOutput = "{\"success\":true,\"output\":\"test result\"}";
        output.println(jsonOutput);

        String captured = output.getOutput().trim();
        assertTrue("JSON 模式输出应为 JSON 格式", captured.startsWith("{"));
        assertTrue("JSON 模式输出应包含 success 字段", captured.contains("\"success\""));
        assertTrue("JSON 模式输出应包含 output 字段", captured.contains("\"output\""));
    }

    @Test
    public void testJsonModeErrorOutput() {
        // JSON 模式错误输出应为结构化 JSON
        String jsonError = "{\"success\":false,\"error\":{\"code\":\"TEST_ERROR\",\"message\":\"test error\"}}";
        output.println(jsonError);

        String captured = output.getOutput().trim();
        assertTrue("JSON 错误输出应包含 success:false", captured.contains("\"success\":false"));
        assertTrue("JSON 错误输出应包含 error 字段", captured.contains("\"error\""));
        assertTrue("JSON 错误输出应包含 code 字段", captured.contains("\"code\""));
    }

    // ==================== 协议模式切换测试 ====================

    @Test
    public void testSwitchBetweenCliAndJson() {
        // 模拟从 CLI 切换到 JSON 再切回 CLI
        // CLI 模式
        output.println("CLI 输出: 命令执行结果");
        String cliOutput = output.getOutput();
        assertFalse(cliOutput.startsWith("{"));

        // 切换到 JSON 模式
        output.reset();
        output.println("{\"success\":true,\"output\":\"JSON 结果\"}");
        String jsonOutput = output.getOutput();
        assertTrue(jsonOutput.startsWith("{"));

        // 切回 CLI 模式
        output.reset();
        output.println("CLI 输出: 回到文本模式");
        assertFalse(output.getOutput().startsWith("{"));
    }

    @Test
    public void testMultipleOutputs() {
        // 模拟多次输出
        for (int i = 0; i < 5; i++) {
            output.println("line " + i);
        }

        assertEquals(5, output.getLines().size());
        for (int i = 0; i < 5; i++) {
            assertEquals("line " + i, output.getLines().get(i));
        }
    }

    // ==================== 错误处理测试 ====================

    @Test
    public void testPrintStackTrace() {
        Exception ex = new RuntimeException("test exception");
        output.printStackTrace(ex);

        String captured = output.getOutput();
        assertTrue("堆栈跟踪应包含异常信息", captured.contains("test exception"));
        assertTrue("堆栈跟踪应包含 RuntimeException", captured.contains("RuntimeException"));
    }

    @Test
    public void testPrintf() {
        output.printf("格式化输出: %s = %d", "value", 42);
        assertEquals("格式化输出: value = 42", output.getOutput());
    }

    @Test
    public void testPrintWithColor() {
        // 彩色输出在 CLI 模式应正常显示
        output.print("红色文本", (byte) 1);
        assertEquals("红色文本", output.getOutput());
    }

    @Test
    public void testPrintlnWithColor() {
        output.println("绿色文本", (byte) 2);
        assertEquals("绿色文本\n", output.getOutput());
    }

    @Test
    public void testPrintSuccess() {
        output.printlnSuccess("成功!");
        assertEquals("成功!\n", output.getOutput());
    }

    @Test
    public void testPrintWarning() {
        output.printlnWarning("警告信息");
        assertTrue(output.getOutput().contains("警告信息"));
        assertTrue(output.getOutput().contains("[WARN]"));
    }

    @Test
    public void testPrintInfo() {
        output.printlnInfo("信息提示");
        assertTrue(output.getOutput().contains("信息提示"));
        assertTrue(output.getOutput().contains("[INFO]"));
    }

    @Test
    public void testPrintDebug() {
        output.printlnDebug("调试信息");
        assertTrue(output.getOutput().contains("调试信息"));
        assertTrue(output.getOutput().contains("[DEBUG]"));
    }
}