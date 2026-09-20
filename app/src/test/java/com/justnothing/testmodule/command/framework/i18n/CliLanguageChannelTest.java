package com.justnothing.testmodule.command.framework.i18n;

import com.google.gson.JsonObject;
import com.justnothing.testmodule.command.framework.CommandExecutor;
import com.justnothing.testmodule.command.framework.output.ClientRequirements;
import com.justnothing.testmodule.command.framework.output.MockOutputHandler;

import org.junit.After;
import org.junit.Test;

import java.util.Locale;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * 「命令输出的语言从哪来」这条通道的测试。
 *
 * <p>链路：客户端 {@code buildClientRequirements} 填语言 → sys.hello 的 params 带过去 →
 * 服务端 {@link ClientRequirements#fromRpcParams} 读回 → {@link CommandExecutor} 执行前应用到
 * 当前线程 → 各渲染点读 {@link CliMessages}。</p>
 *
 * <p>这里守三件事：协议两端的字段名对齐、语言按线程隔离、请求结束后不残留。
 * 都是「改错了照样编译过、运行时静默出错」的地方。</p>
 */
public class CliLanguageChannelTest {

    @After
    public void tearDown() {
        // 这些测试会在当前线程上设语言；不清掉会泄漏给同线程的其它测试
        CliMessages.clearLanguage();
    }

    // ==================== 协议：sys.hello 的 params ====================

    @Test
    public void languageSurvivesRpcRoundTrip() {
        ClientRequirements req = new ClientRequirements(false, true);
        req.setLanguage("en");

        ClientRequirements restored =
                ClientRequirements.fromRpcParamsJson(ClientRequirements.toRpcParamsJson(req));

        assertNotNull(restored);
        assertEquals("language 应能经 sys.hello 的 params 往返", "en", restored.getLanguage());
    }

    @Test
    public void rpcFieldNameIsLanguage() {
        ClientRequirements req = new ClientRequirements();
        req.setLanguage("zh");
        assertEquals("字段名是协议契约，一端改名就会静默失效",
                "zh", ClientRequirements.toRpcParams(req).get("language").getAsString());
    }

    /**
     * 老客户端不会发 language。这时必须保持「没声明」（null），不能变成空串、
     * 更不能因为 Gson 写出 JsonNull 而在读回时抛异常 —— 那会让升级服务端打挂旧客户端。
     */
    @Test
    public void absentLanguageStaysUndeclared() {
        ClientRequirements req = new ClientRequirements(false, false);

        JsonObject params = ClientRequirements.toRpcParams(req);
        assertFalse("没声明语言时不应出现 language 键", params.has("language"));

        assertNull("读回时应仍是 null（表示没声明，服务端回落自己的 Locale）",
                ClientRequirements.fromRpcParams(params).getLanguage());
    }

    // ==================== 线程隔离 ====================

    @Test
    public void languageOverrideIsPerThread() throws Exception {
        CliMessages.useLanguage("zh");
        String chinese = CliMessages.HELP_USAGE.text();

        String[] otherThreadText = new String[1];
        Thread other = new Thread(() -> {
            CliMessages.useLanguage("en");
            otherThreadText[0] = CliMessages.HELP_USAGE.text();
        });
        other.start();
        other.join();

        assertEquals("另一个线程改成英文，不应影响本线程", chinese, CliMessages.HELP_USAGE.text());
        assertNotEquals("另一个线程自己应是英文", chinese, otherThreadText[0]);
    }

    @Test
    public void clearingOverrideFallsBackToProcessLocale() {
        Locale original = Locale.getDefault();
        try {
            Locale.setDefault(Locale.CHINA);
            CliMessages.clearLanguage();
            assertTrue("本进程 Locale 是中文时应回落到中文", CliMessages.chinese());

            Locale.setDefault(Locale.US);
            CliMessages.clearLanguage();
            assertFalse("本进程 Locale 是英文时应回落到英文", CliMessages.chinese());
        } finally {
            Locale.setDefault(original);
            CliMessages.clearLanguage();
        }
    }

    // ==================== 端到端：客户端声明的语言真的影响输出 ====================

    /**
     * 这是本通道存在的唯一理由：客户端说英文，服务端渲染出来的就是英文。
     * 用 {@code help} 是因为它不碰 Android API，且输出全由 {@link CliMessages} 与 id 查表拼成。
     */
    @Test
    public void executorRendersInClientLanguage() {
        MockOutputHandler output = new MockOutputHandler();
        ClientRequirements req = new ClientRequirements(false, false);
        req.setLanguage("en");

        new CommandExecutor().execute("help", output, req);

        String rendered = output.getOutput();
        assertTrue("英文客户端应拿到英文帮助, 实际输出:\n" + rendered,
                rendered.contains("Available commands"));
        assertFalse("不应混入中文小节标题, 实际输出:\n" + rendered, rendered.contains("可用命令"));
    }

    /** 反过来：没声明语言时应回落到本进程 Locale，而不是恒定英文。 */
    @Test
    public void executorWithoutDeclaredLanguageFallsBackToProcessLocale() {
        Locale original = Locale.getDefault();
        try {
            Locale.setDefault(Locale.CHINA);
            CliMessages.clearLanguage();

            MockOutputHandler output = new MockOutputHandler();
            new CommandExecutor().execute("help", output, new ClientRequirements(false, false));

            assertTrue("没声明语言、进程是中文时应输出中文, 实际输出:\n" + output.getOutput(),
                    output.getOutput().contains("可用命令"));
        } finally {
            Locale.setDefault(original);
            CliMessages.clearLanguage();
        }
    }
}
