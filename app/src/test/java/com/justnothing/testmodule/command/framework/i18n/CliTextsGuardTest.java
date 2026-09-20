package com.justnothing.testmodule.command.framework.i18n;

import com.justnothing.testmodule.command.framework.CommandCatalog;
import com.justnothing.testmodule.command.framework.annotation.Cmd;
import com.justnothing.testmodule.command.framework.annotation.CmdParam;
import com.justnothing.testmodule.command.framework.annotation.CmdRoutes;
import com.justnothing.testmodule.command.framework.annotation.SubCommandInfo;
import com.justnothing.testmodule.command.framework.model.MainCommand;

import org.junit.Test;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;

import static org.junit.Assert.assertTrue;

/**
 * {@link CliTexts} 的守卫测试。
 *
 * <p>注解里的文案是「id 常量 + 运行时查表」，这条链路上有两个环节是编译器管不到的：</p>
 * <ol>
 *   <li><b>id 拼错</b>：{@code description = "route.memory.gcc.desc"} 编译得过，
 *       运行时查不到，输出里就露出这串 id。难看，但至少看得见。</li>
 *   <li><b>族忘了登记</b>：新加了 {@code XxxTexts} 却没在 {@link CliTexts} 的静态块里加
 *       {@code register} 那一行 —— 整族的文案一起失效。</li>
 * </ol>
 * <p>两个都是「编译期合法、运行期静默」，所以放在这里守。</p>
 *
 * <p>反向也守一次：登记了却没有任何注解在用的 id（孤儿）。它通常意味着
 * 「常量改了名、注解还写着旧值」或者「注解和新 id 对不上」，是漂移的早期信号。</p>
 */
public class CliTextsGuardTest {

    /** 一条「注解里写了什么」的记录，失败时用来说明出处。 */
    private record Ref(String where, String value) {
        @Override
        public String toString() {
            return where + " = \"" + value + "\"";
        }
    }

    // 迁移完成后，注解里的值一定是 id。还没迁移的族写的是中文原文，两者靠形态区分。
    private static final Pattern ID_PREFIX = Pattern.compile("^(?:cmd|route|sub|param)\\.");

    private static final Pattern ID_FULL =
            Pattern.compile("^(?:cmd|route|sub|param)\\.[A-Za-z0-9._-]+\\.(?:desc|help|usage|options)$");

    private static final Pattern HAN = Pattern.compile("\\p{IsHan}");

    /**
     * 注解里的值只要长成 id 的样子，就必须形态完整、并且真的登记过。
     *
     * <p>注意「长成 id 的样子」用的是前缀而不是完整形态：{@code route.memory.gc}（漏了尾巴）
     * 从完整形态看只是「一段普通文本」，会被放过去 —— 但它在输出里和正常字符串毫无区别，
     * 比露出完整 id 更难发现。用前缀判定才能拦住它。</p>
     */
    @Test
    public void annotationIdsAreWellFormedAndRegistered() {
        Set<String> registered = CliTexts.registeredIds();
        List<Ref> malformed = new ArrayList<>();
        List<Ref> unregistered = new ArrayList<>();
        List<Ref> unmigrated = new ArrayList<>();
        int idCount = 0;

        for (Ref ref : collectAnnotationTexts()) {
            if (ID_PREFIX.matcher(ref.value()).find()) {
                idCount++;
                if (!ID_FULL.matcher(ref.value()).matches()) {
                    malformed.add(ref);
                } else if (!registered.contains(ref.value())) {
                    unregistered.add(ref);
                }
            } else if (HAN.matcher(ref.value()).find()) {
                // 还没迁移的注解：值是中文原文。空值（没写 usage 之类）和纯命令语法不算。
                unmigrated.add(ref);
            }
        }

        System.out.println("[CliTexts] 已迁移 " + idCount + " 条 / 待迁移(注解里还是中文) " + unmigrated.size()
                + " 条 / 登记表 " + registered.size() + " 条");
        for (Ref ref : unmigrated) {
            System.out.println("    [待迁移] " + ref);
        }

        assertTrue("下面这些注解值像 id 但形态不完整（多半是漏了 .desc/.options 这类后缀）:\n  "
                + join(malformed), malformed.isEmpty());
        assertTrue("下面这些 id 出现在注解里，但没有任何 XxxTexts 登记过（拼错了，"
                + "或者新族忘了在 CliTexts 静态块里加 register）:\n  "
                + join(unregistered), unregistered.isEmpty());
    }

    /** 已登记但没有任何注解引用的 id：常量与注解之间漂移的信号。 */
    @Test
    public void registeredIdsAreAllUsedByAnnotations() {
        Set<String> used = new LinkedHashSet<>();
        for (Ref ref : collectAnnotationTexts()) {
            if (ID_FULL.matcher(ref.value()).matches()) {
                used.add(ref.value());
            }
        }

        Set<String> orphans = new TreeSet<>(CliTexts.registeredIds());
        orphans.removeAll(used);

        assertTrue("下面这些 id 已登记，但没有任何注解在用（注解里换了新 id，"
                + "或者常量名和注解写的内容对不上）:\n  "
                + String.join("\n  ", orphans), orphans.isEmpty());
    }

    /** 中文是主语言：登记了就得有中文，否则中文环境会直接露出 id 本身。 */
    @Test
    public void everyRegisteredIdHasChinese() {
        Set<String> missing = new TreeSet<>();
        for (String id : CliTexts.registeredIds()) {
            String zh = CliTexts.chineseOf(id);
            if (zh == null || zh.isEmpty()) {
                missing.add(id);
            }
        }
        assertTrue("下面这些 id 只有英文、没有中文:\n  " + String.join("\n  ", missing), missing.isEmpty());
    }

    // ==================== 收集 ====================

    /**
     * 扫出全部会进入输出的注解文案。
     *
     * <p>入口只有 {@link CommandCatalog#ALL} 这一份清单 —— 它已经是「命令清单的唯一真相源」，
     * 从每个主命令出发顺着路由走到 handler 与 request，就能覆盖挂在它们身上的
     * {@link SubCommandInfo} 与 {@link CmdParam}。</p>
     */
    private static List<Ref> collectAnnotationTexts() {
        List<Ref> refs = new ArrayList<>();
        Set<Class<?>> visited = new HashSet<>();

        for (Class<? extends MainCommand<?>> cmdClass : CommandCatalog.ALL) {
            Cmd cmd = cmdClass.getAnnotation(Cmd.class);
            String prefix = cmd != null ? cmd.name() : cmdClass.getSimpleName();
            if (cmd != null) {
                refs.add(new Ref(cmdClass.getSimpleName() + "@Cmd.description", cmd.description()));
                refs.add(new Ref(cmdClass.getSimpleName() + "@Cmd.helpText", cmd.helpText()));
            }

            CmdRoutes routes = cmdClass.getAnnotation(CmdRoutes.class);
            if (routes == null) {
                continue;
            }
            for (CmdRoutes.Route route : routes.value()) {
                String path = route.path().isEmpty() ? prefix : prefix + "/" + route.path();
                refs.add(new Ref(path + "@CmdRoutes.Route.description", route.description()));
                collectHandler(refs, route.handler(), visited);
                collectRequest(refs, route.request(), visited);
            }
        }
        return refs;
    }

    private static void collectHandler(List<Ref> refs, Class<?> handler, Set<Class<?>> visited) {
        if (handler == null || handler == Void.class || !visited.add(handler)) {
            return;
        }
        SubCommandInfo info = handler.getAnnotation(SubCommandInfo.class);
        if (info == null) {
            return;
        }
        String where = handler.getSimpleName() + "@SubCommandInfo";
        refs.add(new Ref(where + ".description", info.description()));
        refs.add(new Ref(where + ".usage", info.usage()));
        refs.add(new Ref(where + ".optionsDesc", info.optionsDesc()));
        // examples / seeAlso 是命令名或命令语法，不属于文案，不参与本测试。
    }

    private static void collectRequest(List<Ref> refs, Class<?> request, Set<Class<?>> visited) {
        if (request == null || !visited.add(request)) {
            return;
        }
        // @CmdParam 可能声明在父类上（继承来的参数同样会渲染），所以要沿继承链走。
        for (Class<?> current = request; current != null && current != Object.class;
             current = current.getSuperclass()) {
            for (Field field : current.getDeclaredFields()) {
                CmdParam param = field.getAnnotation(CmdParam.class);
                if (param != null) {
                    refs.add(new Ref(current.getSimpleName() + "#" + field.getName()
                            + "@CmdParam.description", param.description()));
                }
            }
        }
    }

    private static String join(List<Ref> refs) {
        StringBuilder sb = new StringBuilder();
        for (Ref ref : refs) {
            sb.append("  ").append(ref).append('\n');
        }
        return sb.toString();
    }
}
