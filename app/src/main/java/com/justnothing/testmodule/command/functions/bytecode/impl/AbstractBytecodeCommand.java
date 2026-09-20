package com.justnothing.testmodule.command.functions.bytecode.impl;

import com.justnothing.testmodule.command.framework.CommandExecutor;
import com.justnothing.testmodule.command.framework.model.AbstractCommand;
import com.justnothing.testmodule.command.framework.model.CommandRequest;
import com.justnothing.testmodule.command.functions.bytecode.extract.ClassDexExtractor;
import com.justnothing.testmodule.command.functions.bytecode.extract.DexClassIndex;
import com.justnothing.testmodule.command.functions.bytecode.extract.DexSourceLocator;
import com.justnothing.testmodule.command.functions.bytecode.response.BytecodeResult;
import com.justnothing.testmodule.command.framework.output.Colors;
import com.justnothing.richconsole.console.Console;
import com.justnothing.richconsole.progress.Progress;
import com.justnothing.richconsole.syntax.Syntax;
import com.justnothing.testmodule.constants.FileDirectory;
import com.justnothing.testmodule.utils.reflect.ClassResolver;
import com.justnothing.testmodule.utils.logging.Logger;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * bytecode 域各子命令的公共基类。
 *
 * <p>这里<b>不再提供 "获取 .class 字节码"</b> 这类方法：Android 上不存在 JVM 的
 * {@code .class}，ART 只保留 dex。以前那几个方法（ASM 反汇编、CFR 反编译）拿 APK 里的
 * dex 去喂 JVM 工具，必然失败 —— 现在统一改为通过 {@link ClassDexExtractor}
 * 拿到 dex（附带可信度），需要使用字节码的功能自己决定怎么用。</p>
 */
public abstract class AbstractBytecodeCommand<Req extends CommandRequest<?>> extends AbstractCommand<Req, BytecodeResult> {

    protected static final Logger logger = Logger.getLoggerForName("BytecodeCmd");

    protected AbstractBytecodeCommand(String commandName, Class<Req> requestType) {
        super(commandName, requestType, BytecodeResult.class);
    }

    protected abstract BytecodeResult executeInternal(CommandExecutor.CmdExecContext<Req> context) throws Exception;

    /**
     * 高亮的规模上限（行数 + 字符数）。
     *
     * <p><b>这个上限是拿真机换来的，别随手调大。</b>手表堆只有 256MB，实测跑这条链路时
     * 只剩 33MB 可用、单次 GC 暂停 500ms —— 一次 412 行 × 156 列的高亮会把 GC 拖成风暴，
     * 表现是整机卡死、只能长按重启。</p>
     *
     * <p>配合 {@link #CHUNK_LINES} 的分片渲染后，峰值内存已经与代码总长无关了，
     * 所以这里主要是在限制<b>总耗时</b>：渲染成本 ≈ 行数 × 终端列数，宽终端下要按比例收紧。</p>
     */
    private static final int MAX_HIGHLIGHT_LINES = 300;

    private static final int MAX_HIGHLIGHT_CHARS = 20_000;

    /**
     * 分片渲染时每片的行数。
     *
     * <p><b>为什么要分片</b>：RichConsole 的 {@code Syntax} 会把<b>每一行都填充到终端完整宽度</b>
     * （见它 {@code richConsole()} 里的 {@code adjustLineLength}），所以 412 行 × 156 列
     * 就是六万多个 Segment 对象 —— 在只剩 33MB 可用堆的手表上直接 GC 风暴。
     * 分片后峰值内存只与单片有关、与代码总长无关，这才是"整机卡死"的正解。</p>
     *
     * <p>顺带的好处：输出一片片出现，用户看得到进展，而不是对着黑屏猜是不是死了。</p>
     */
    private static final int CHUNK_LINES = 40;

    protected void out(CommandExecutor.CmdExecContext<?> context, String message) {
        out(context, message, Colors.DEFAULT);
    }

    protected void out(CommandExecutor.CmdExecContext<?> context, String message, byte color) {
        context.println(message, color);
    }

    /**
     * 输出一段代码：能高亮就高亮，不能就老老实实打纯文本。
     *
     * <p>三条底线：</p>
     * <ul>
     *   <li>{@code console()} 为 null 时（GUI / JSON / 被 agent 调用）绝不走 RichConsole ——
     *       那些客户端收到一坨 ANSI 转义只会更难读；</li>
     *   <li>规模超限不上色，见 {@link #MAX_HIGHLIGHT_LINES}；</li>
     *   <li>高亮抛异常不影响命令。</li>
     * </ul>
     *
     * <p>写文件时不要用这个方法 —— 文件里应该是纯文本，ANSI 转义在别的编辑器里就是乱码。</p>
     *
     * @param lexerName RichConsole 注册的 lexer 名（{@code java} / {@code python} / {@code json}）
     */
    protected void printCode(CommandExecutor.CmdExecContext<?> context, String code, String lexerName) {
        printCode(context, code, lexerName, false);
    }

    /** @param force 忽略规模上限强行高亮（用户显式要求，风险自负） */
    protected void printCode(CommandExecutor.CmdExecContext<?> context, String code, String lexerName,
                             boolean force) {
        if (code == null || code.isEmpty()) {
            return;
        }
        // 必须问 supportsRichOutput()：无终端场景下 console() 非 null，但写进去的内容会被丢掉
        boolean richAvailable = context.supportsRichOutput();
        if (!richAvailable || (!force && !withinHighlightBudget(code))) {
            context.println(code);
            if (richAvailable && !force) {
                context.println("（这段有 " + countLines(code) + " 行 / " + code.length()
                        + " 字符，超过高亮上限 " + MAX_HIGHLIGHT_LINES + " 行 / "
                        + MAX_HIGHLIGHT_CHARS + " 字符，已按纯文本输出。\n"
                        + "  加 --highlight 可以强制高亮，但大段代码在手表上会明显卡顿，慎用。）", Colors.GRAY);
            }
            return;
        }
        try {
            Console console = context.console();
            // 分片渲染，别把整份代码一次性丢给 Syntax 去建对象树（见 CHUNK_LINES）
            for (String chunk : splitIntoChunks(code, CHUNK_LINES)) {
                console.println(Syntax.of(chunk, cfg -> cfg.lexerName(lexerName)));
            }
        } catch (Throwable t) {
            // 高亮只是装饰，它挂了不该把命令一起带走
            logger.warn("代码高亮失败，退回纯文本: " + t);
            context.println(code);
        }
    }

    private static boolean withinHighlightBudget(String code) {
        return countLines(code) <= MAX_HIGHLIGHT_LINES && code.length() <= MAX_HIGHLIGHT_CHARS;
    }

    /**
     * 把代码切成若干片（每片最多 {@code maxLines} 行），供分片渲染用。
     *
     * <p>切点必须"安全"：不能落在块注释或字符串中间。因为 RichConsole 的
     * {@code RegexLexer} 里处理 {@code /* *}{@code /}、字符串这些跨行结构的状态栈是
     * {@code tokenize()} 的<b>局部变量</b>（每片都从 ROOT 重新开始）——
     * 在块注释中间切开，后一片会把注释正文当成代码来上色。</p>
     *
     * <p>误判的代价只是颜色错乱，不影响内容，所以这里的状态机刻意写得简单：
     * 只认块注释、字符串、字符字面量、行注释这四种会互相干扰的情况。</p>
     */
    static List<String> splitIntoChunks(String code, int maxLines) {
        List<String> chunks = new ArrayList<>();
        if (code == null || code.isEmpty() || maxLines <= 0) {
            return chunks;
        }

        int chunkStart = 0;
        int linesInChunk = 0;
        int i = 0;
        boolean inBlockComment = false;
        boolean inString = false;
        boolean inChar = false;
        boolean escaped = false;

        while (i < code.length()) {
            char c = code.charAt(i);
            char next = i + 1 < code.length() ? code.charAt(i + 1) : '\0';

            if (inString || inChar) {
                if (escaped) {
                    escaped = false;
                } else if (c == '\\') {
                    escaped = true;
                } else if (inString && c == '"') {
                    inString = false;
                } else if (inChar && c == '\'') {
                    inChar = false;
                }
                if (c == '\n') {
                    // 只计数、不切：在这里切开会让后一片的状态机对不上
                    linesInChunk++;
                }
                i++;
                continue;
            }
            if (inBlockComment) {
                if (c == '*' && next == '/') {
                    inBlockComment = false;
                    i += 2;
                    continue;
                }
                if (c == '\n') {
                    linesInChunk++; // 同上：块注释内部不切
                }
                i++;
                continue;
            }
            if (c == '/' && next == '*') {
                inBlockComment = true;
                i += 2;
                continue;
            }
            if (c == '/' && next == '/') {
                int eol = code.indexOf('\n', i);
                if (eol < 0) {
                    break;
                }
                // 跳到行尾，让下面的 '\n' 分支处理 —— 行注释结束处是安全切点
                i = eol;
                continue;
            }
            if (c == '"') {
                inString = true;
                i++;
                continue;
            }
            if (c == '\'') {
                inChar = true;
                i++;
                continue;
            }

            if (c == '\n') {
                linesInChunk++;
                if (linesInChunk >= maxLines) {
                    chunks.add(code.substring(chunkStart, i + 1));
                    chunkStart = i + 1;
                    linesInChunk = 0;
                }
            }
            i++;
        }

        if (chunkStart < code.length()) {
            chunks.add(code.substring(chunkStart));
        }
        return chunks;
    }

    private static int countLines(String text) {
        int lines = 1;
        for (int i = 0; i < text.length(); i++) {
            if (text.charAt(i) == '\n') {
                lines++;
            }
        }
        return lines;
    }

    protected BytecodeResult buildSuccessResult(String subCommand, String className, String output) {
        BytecodeResult result = new BytecodeResult(java.util.UUID.randomUUID().toString());
        result.setSuccess(true);
        result.setSubCommand(subCommand);
        result.setClassName(className);
        result.setOutput(output);
        return result;
    }

    protected BytecodeResult buildErrorResult(String message) {
        BytecodeResult result = new BytecodeResult();
        result.setSuccess(false);
        result.setMessage(message);
        // 错误文本同时放进 output：客户端渲染的是 output 字段，只设 message 的话
        // 用户看到的是"命令跑完了但一片空白"，比报错还难排查。
        result.setOutput(message);
        return result;
    }

    protected Class<?> loadClass(String className, ClassLoader classLoader) throws ClassNotFoundException {
        return ClassResolver.findClassOrFail(className, classLoader);
    }

    /**
     * 定位类所在的来源并取出 dex。
     *
     * <p>候选已按可信度排序，但<b>不能直接取第一个</b>：一个来源（apk）里可能装着多个
     * {@code classes*.dex}，而第一个不一定含这个类 —— 直接取 {@code get(0)} 会把错误的
     * dex 交给反编译器，表现成"类和来源都找到了，却反编译不出任何东西"。</p>
     *
     * <p><b>而且必须按"定义"挑，不能按"引用"挑。</b>{@link DexClassIndex#containsClass} 查的是
     * string_ids：一个类只要在别的 dex 里被引用过就会命中，于是挑到"只引用、不定义"的那份，
     * 反编译器在里面找不到类定义，直接返回空。多 dex 的 APK 里这非常常见 ——
     * 实测 i3launcher 的 5 个 dex，{@code ayj} 在 classes2 被引用、定义却在 classes4。
     * 所以这里用 {@link DexClassIndex#definesClass}。</p>
     *
     * @return 提取结果；定位不到、取不出来、或没有任何一份候选 dex <b>定义</b>了这个类时返回 {@code null}
     */
    protected ClassDexExtractor.Outcome resolveDex(String className) {
        return pickDefiningOutcome(className, ClassDexExtractor.extract(className));
    }

    /**
     * 带进度显示的 {@link #resolveDex(String)}。
     *
     * <p>定位要挨个扫候选来源（boot classpath 的十几个 vdex、若干 apk），几秒内什么都看不到，
     * 所以给需要它的命令一个带进度条的版本。无富渲染能力时自动退回不带进度的实现。</p>
     */
    protected ClassDexExtractor.Outcome resolveDex(String className,
            CommandExecutor.CmdExecContext<?> context) {
        Console console = context.supportsRichOutput() ? context.console() : null;
        if (console == null) {
            return resolveDex(className);
        }

        Progress progress = console.progress(c -> c.transientMode = true);
        List<ClassDexExtractor.Outcome> outcomes;
        try {
            outcomes = ClassDexExtractor.extract(className, new DexSourceLocator.ScanListener() {

                private int task = -1;

                @Override
                public void onStart(int total) {
                    if (total <= 0) {
                        return;
                    }
                    task = progress.addTask("扫描代码来源", total);
                    progress.start();
                }

                @Override
                public void onScanned(int done, String label) {
                    if (task >= 0) {
                        progress.advance(task, 1);
                    }
                }
            });
        } finally {
            progress.close();
        }
        return pickDefiningOutcome(className, outcomes);
    }

    /** 从候选里挑出"真正定义了这个类"的那一份，见 {@link #resolveDex(String)} 的说明。 */
    private ClassDexExtractor.Outcome pickDefiningOutcome(String className,
            List<ClassDexExtractor.Outcome> outcomes) {
        for (ClassDexExtractor.Outcome outcome : outcomes) {
            if (DexClassIndex.definesClass(outcome.result().dex(), className)) {
                return outcome;
            }
        }
        if (!outcomes.isEmpty()) {
            logger.warn("取出的 " + outcomes.size() + " 个候选 dex 里都只是引用、没有 " + className
                    + " 的类定义");
        }
        return null;
    }

    protected static void ensureDirectory(File dir) throws IOException {
        if (!dir.isDirectory() && !dir.mkdirs() && !dir.isDirectory()) {
            throw new IOException("无法创建目录: " + dir.getAbsolutePath());
        }
    }

    /**
     * 输出目录候选，按"用户最好拿"到"一定有权限"排序。
     *
     * <p>{@code /sdcard} 在最前面是因为它最方便（直接就能 pull），但它<b>只对应用进程可写</b>：
     * system_server 的 uid 是 AID_SYSTEM，既不在 {@code sdcard_rw} 组，
     * sdcardfs 的 mask 也不给它写权限。</p>
     */
    private static final String[] OUTPUT_BASES = {
            FileDirectory.SDCARD_PATH + "/dex",
            FileDirectory.METHODS_DATA_DIR + "/dex",
    };

    /**
     * 挑一个真的能写的输出目录。
     *
     * <p>不能直接照搬请求里的路径、也不能只试一个默认值：{@code mkdirs()} 失败只返回 false、
     * 不告诉你原因，结果就是"命令执行成功但没有任何输出" —— 这个坑已经在真机上踩过一次了。</p>
     *
     * @param requested  调用方指定的目录；为空时按 {@link #OUTPUT_BASES} 依次尝试
     * @param namePrefix 目录名前缀（如 {@code dump} / {@code batch}）
     * @return 可写的目录；候选全部失败时返回 {@code null}
     */
    protected static File resolveOutputDir(String requested, String namePrefix) {
        if (requested != null && !requested.isEmpty()) {
            return new File(requested);
        }
        String name = namePrefix + "-" + System.currentTimeMillis();
        for (String base : OUTPUT_BASES) {
            File dir = new File(base, name);
            if (dir.isDirectory() || dir.mkdirs()) {
                return dir;
            }
        }
        return null;
    }

    /**
     * 写文件，并确认字节数确实对得上。
     *
     * <p>刻意不走 {@code IOManager.writeFile}：那条路在 zygote 阶段会<b>静默 return</b>
     * （不写、也不报错），而这里写的都是用户要拿去用（反编译 / 看指令）的东西 ——
     * 静默失败比直接报错危险得多。</p>
     */
    protected static void writeOutput(File target, byte[] content) throws IOException {
        File parent = target.getParentFile();
        if (parent != null) {
            ensureDirectory(parent);
        }
        try (FileOutputStream out = new FileOutputStream(target)) {
            out.write(content);
            out.flush();
        }
        if (target.length() != content.length) {
            throw new IOException("写入不完整: 期望 " + content.length
                    + " 字节，实际 " + target.length() + " 字节");
        }
    }
}
