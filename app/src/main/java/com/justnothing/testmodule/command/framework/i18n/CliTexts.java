package com.justnothing.testmodule.command.framework.i18n;

import com.justnothing.testmodule.command.functions.agent.AgentTexts;
import com.justnothing.testmodule.command.functions.alias.AliasTexts;
import com.justnothing.testmodule.command.functions.breakpoint.BreakpointTexts;
import com.justnothing.testmodule.command.functions.bsh.BshTexts;
import com.justnothing.testmodule.command.functions.bytecode.BytecodeTexts;
import com.justnothing.testmodule.command.functions.classcmd.ClassTexts;
import com.justnothing.testmodule.command.functions.didyouknow.DidYouKnowTexts;
import com.justnothing.testmodule.command.functions.exportcontext.ExportContextTexts;
import com.justnothing.testmodule.command.functions.help.HelpTexts;
import com.justnothing.testmodule.command.functions.hook.HookTexts;
import com.justnothing.testmodule.command.functions.jank.JankTexts;
import com.justnothing.testmodule.command.functions.memory.MemoryTexts;
import com.justnothing.testmodule.command.functions.nativecmd.NativeTexts;
import com.justnothing.testmodule.command.functions.network.NetworkTexts;
import com.justnothing.testmodule.command.functions.packages.PackagesTexts;
import com.justnothing.testmodule.command.functions.performance.PerformanceTexts;
import com.justnothing.testmodule.command.functions.script.ScriptTexts;
import com.justnothing.testmodule.command.functions.system.SystemTexts;
import com.justnothing.testmodule.command.functions.threads.ThreadsTexts;
import com.justnothing.testmodule.command.functions.trace.TraceTexts;
import com.justnothing.testmodule.command.functions.watch.WatchTexts;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * 全部 CLI 文案的登记处与查表入口。
 *
 * <h3>为什么用「id 常量」而不是「中文原文当 key」</h3>
 * 最初想在渲染出口拿中文原文当 key 去查翻译表，省掉改注解的功夫。但这个方案有个致命处：
 * <b>改动中文就等于换掉了 key</b> —— 哪天你把「清空所有已定义的命令别名」改成「清空全部别名」，
 * 英文翻译就静默失效、悄悄回落成中文，没有任何提示。
 * 而 id 是稳定的：{@code ROUTE_ALIAS_CLEAR_DESC = "route.alias.clear.desc"} 一旦定下，
 * 中文英文随便改，两者互不影响。代价是注解里看不回原文（要跳到这里来看），
 * 这个代价是明确接受了的。
 *
 * <h3>id 命名规范</h3>
 * <pre>
 *   cmd.&lt;命令名&gt;.desc               @Cmd.description
 *   cmd.&lt;命令名&gt;.help               @Cmd.helpText
 *   route.&lt;主命令&gt;.&lt;子命令&gt;.desc     @CmdRoutes.Route.description   （命令列表里那一行）
 *   sub.&lt;主命令&gt;.&lt;子命令&gt;.desc       @SubCommandInfo.description    （帮助正文）
 *   sub.&lt;主命令&gt;.&lt;子命令&gt;.usage      @SubCommandInfo.usage
 *   sub.&lt;主命令&gt;.&lt;子命令&gt;.options    @SubCommandInfo.optionsDesc
 *   param.&lt;主命令&gt;.&lt;子命令&gt;.&lt;参数名&gt;.desc   @CmdParam.description
 * </pre>
 * 前缀按<b>注解来源</b>区分，而不是按语义 —— 因为同一个命令的 {@code Route.description}
 * 和 {@code SubCommandInfo.description} 是<b>两段不同的文字</b>（前者一行、后者一段），
 * 按语义命名会撞在一起。按来源命名，看到一个 id 就知道该去哪个注解里找它，也方便守卫测试反查。
 *
 * <h3>文案存在哪</h3>
 * 每个命令族一个 {@code XxxTexts} 类，和它的命令放在同一个包下（如 memory 族在
 * {@code command/functions/memory/MemoryTexts.java}），里面同时放 id 常量、中文、英文。
 * 不集中在一个大文件里，是因为这样翻一个命令族就只碰一个文件，改动面清晰。
 * 新增一个族时，在下面静态块里加一行 {@code XxxTexts.register(ZH, EN);} 即可 ——
 * 这一行是刻意的，因为它让「哪些族登记了」这件事可以被一眼看全，也能被守卫测试检查。
 */
public final class CliTexts {

    private static final Map<String, String> ZH = new HashMap<>();
    private static final Map<String, String> EN = new HashMap<>();

    static {
        // 每个命令族一行。漏登记的话该族的 id 在运行时会查不到、直接显示出 id 字符串，
        // 所以守卫测试会检查「注解里用到的 id 是否都已登记」。
        AgentTexts.register(ZH, EN);
        AliasTexts.register(ZH, EN);
        BreakpointTexts.register(ZH, EN);
        BshTexts.register(ZH, EN);
        BytecodeTexts.register(ZH, EN);
        ClassTexts.register(ZH, EN);
        DidYouKnowTexts.register(ZH, EN);
        ExportContextTexts.register(ZH, EN);
        HelpTexts.register(ZH, EN);
        HookTexts.register(ZH, EN);
        JankTexts.register(ZH, EN);
        MemoryTexts.register(ZH, EN);
        NativeTexts.register(ZH, EN);
        NetworkTexts.register(ZH, EN);
        PackagesTexts.register(ZH, EN);
        PerformanceTexts.register(ZH, EN);
        ScriptTexts.register(ZH, EN);
        SystemTexts.register(ZH, EN);
        ThreadsTexts.register(ZH, EN);
        TraceTexts.register(ZH, EN);
        WatchTexts.register(ZH, EN);
    }

    private CliTexts() {
    }

    /**
     * 把注解里的值解析成当前语言下的文案。
     *
     * <p><b>查不到就原样返回</b>，这在两个方向上都有用：</p>
     * <ul>
     *   <li>还没迁移的注解（{@code description = "内存调试和管理工具"}）会被原样返回，
     *       所以迁移过程可以一个族一个族地做，中间状态永远是可运行的；</li>
     *   <li>已经迁移但 id 写错、或表里漏登记时，输出里会直接露出
     *       {@code route.memory.gc.desc} 这样的字符串 —— 难看，但<b>一眼就能发现</b>，
     *       这正是从「中文当 key」那套方案里学到的东西：宁可难看，不要静默。</li>
     * </ul>
     *
     * <p>英文缺失时回落中文（而不是回落 id）：一个还没翻译的条目在英文环境下显示中文，
     * 比显示一个 id 有用得多。所以翻译可以逐条补，不会有「没翻完就不能用」的状态。</p>
     */
    public static String resolve(String id) {
        if (id == null || id.isEmpty()) {
            return "";
        }
        String text = CliMessages.chinese() ? ZH.get(id) : EN.get(id);
        if (text == null) {
            text = ZH.get(id);
        }
        return text != null ? text : id;
    }

    // ==================== 下面是给守卫测试用的 ====================

    /** 已登记的全部 id。 */
    public static Set<String> registeredIds() {
        return Set.copyOf(ZH.keySet());
    }

    public static String chineseOf(String id) {
        return ZH.get(id);
    }

    public static String englishOf(String id) {
        return EN.get(id);
    }
}
