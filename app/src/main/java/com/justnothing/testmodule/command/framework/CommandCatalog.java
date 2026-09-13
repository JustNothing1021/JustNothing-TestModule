package com.justnothing.testmodule.command.framework;

import com.justnothing.testmodule.command.framework.model.MainCommand;
import com.justnothing.testmodule.command.functions.agent.AgentCliMain;
import com.justnothing.testmodule.command.functions.alias.AliasMain;
import com.justnothing.testmodule.command.functions.breakpoint.impl.BreakpointMain;
import com.justnothing.testmodule.command.functions.bsh.BeanShellExecutorMain;
import com.justnothing.testmodule.command.functions.bytecode.impl.BytecodeMain;
import com.justnothing.testmodule.command.functions.classcmd.ClassMain;
import com.justnothing.testmodule.command.functions.didyouknow.DidYouKnowMain;
import com.justnothing.testmodule.command.functions.examples.InteractiveExampleMain;
import com.justnothing.testmodule.command.functions.examples.OutputExampleMain;
import com.justnothing.testmodule.command.functions.exportcontext.ExportContextMain;
import com.justnothing.testmodule.command.functions.help.HelpMain;
import com.justnothing.testmodule.command.functions.hook.HookMain;
import com.justnothing.testmodule.command.functions.memory.MemoryMain;
import com.justnothing.testmodule.command.functions.nativecmd.NativeMain;
import com.justnothing.testmodule.command.functions.network.NetworkMain;
import com.justnothing.testmodule.command.functions.packages.PackagesMain;
import com.justnothing.testmodule.command.functions.performance.PerformanceMain;
import com.justnothing.testmodule.command.functions.script.ScriptExecutorMain;
import com.justnothing.testmodule.command.functions.system.SystemMain;
import com.justnothing.testmodule.command.functions.tests.AnonClassTestMain;
import com.justnothing.testmodule.command.functions.tests.FeatureDemoMain;
import com.justnothing.testmodule.command.functions.tests.LayoutPanelDemoMain;
import com.justnothing.testmodule.command.functions.tests.LiveDemoMain;
import com.justnothing.testmodule.command.functions.tests.MarkdownSyntaxDemoMain;
import com.justnothing.testmodule.command.functions.tests.NonePromptDemoMain;
import com.justnothing.testmodule.command.functions.tests.ProgressDemoMain;
import com.justnothing.testmodule.command.functions.tests.RichDemoMain;
import com.justnothing.testmodule.command.functions.tests.SandboxTestMain;
import com.justnothing.testmodule.command.functions.tests.TableDemoMain;
import com.justnothing.testmodule.command.functions.tests.TestCardDemoMain;
import com.justnothing.testmodule.command.functions.tests.TreeDemoMain;
import com.justnothing.testmodule.command.functions.threads.ThreadsMain;
import com.justnothing.testmodule.command.functions.trace.TraceMain;
import com.justnothing.testmodule.command.functions.watch.WatchMain;

/**
 * 命令清单的唯一真相源。
 *
 * <p>此前服务端（{@code CommandExecutor}）与客户端元数据（{@code CommandMetadataScanner}）
 * 各自维护一份命令类列表，已经漂移过一次（{@code did-you-know} 只在服务端注册）。
 * 现在两份合并为这里的唯一一份，新增命令只改这里。</p>
 *
 * <p>清单分两类：{@link #USER_FACING} 是面向用户的正式命令，help 列表与客户端补全只来自它；
 * {@link #DEBUG_ONLY} 是仅供本地调试的 demo/test 命令，仍会被注册（可以用命令名手动执行），
 * 但不会出现在 help 与补全里。{@link #ALL} 是二者的并集，用于注册与路由扫描。</p>
 */
public final class CommandCatalog {

    private CommandCatalog() {
    }

    /** 面向用户的正式命令：help 清单与客户端补全只来自这里。 */
    @SuppressWarnings("unchecked")
    public static final Class<? extends MainCommand<?>>[] USER_FACING = new Class[]{
            HelpMain.class,
            WatchMain.class,
            TraceMain.class,
            ExportContextMain.class,
            MemoryMain.class,
            ThreadsMain.class,
            SystemMain.class,
            BreakpointMain.class,
            HookMain.class,
            BytecodeMain.class,
            NativeMain.class,
            PerformanceMain.class,
            AliasMain.class,
            NetworkMain.class,
            BeanShellExecutorMain.class,
            ScriptExecutorMain.class,
            ClassMain.class,
            PackagesMain.class,
            AgentCliMain.class,
            DidYouKnowMain.class,
    };

    /** 仅供本地调试的 demo/test 命令：会注册、可手动执行，但不对用户展示。 */
    @SuppressWarnings("unchecked")
    public static final Class<? extends MainCommand<?>>[] DEBUG_ONLY = new Class[]{
            OutputExampleMain.class,
            InteractiveExampleMain.class,
            SandboxTestMain.class,
            AnonClassTestMain.class,
            RichDemoMain.class,
            TableDemoMain.class,
            ProgressDemoMain.class,
            LayoutPanelDemoMain.class,
            TreeDemoMain.class,
            NonePromptDemoMain.class,
            TestCardDemoMain.class,
            LiveDemoMain.class,
            MarkdownSyntaxDemoMain.class,
            FeatureDemoMain.class,
    };

    /** 全部命令类（{@link #USER_FACING} + {@link #DEBUG_ONLY}），用于注册与路由扫描。 */
    public static final Class<? extends MainCommand<?>>[] ALL = buildAll();

    @SuppressWarnings("unchecked")
    private static Class<? extends MainCommand<?>>[] buildAll() {
        Class<? extends MainCommand<?>>[] combined =
                new Class[USER_FACING.length + DEBUG_ONLY.length];
        System.arraycopy(USER_FACING, 0, combined, 0, USER_FACING.length);
        System.arraycopy(DEBUG_ONLY, 0, combined, USER_FACING.length, DEBUG_ONLY.length);
        return combined;
    }
}
