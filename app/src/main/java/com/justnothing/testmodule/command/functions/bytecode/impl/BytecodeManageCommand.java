package com.justnothing.testmodule.command.functions.bytecode.impl;

import com.justnothing.richconsole.console.Console;
import com.justnothing.richconsole.progress.Progress;
import com.justnothing.richconsole.status.Status;

import com.justnothing.testmodule.command.framework.CommandExecutor;
import com.justnothing.testmodule.command.framework.model.CommandRequest;
import com.justnothing.testmodule.command.functions.bytecode.extract.DexClassIndex;
import com.justnothing.testmodule.command.functions.bytecode.extract.DexExtractionManager;
import com.justnothing.testmodule.command.functions.bytecode.extract.DexExtractor;
import com.justnothing.testmodule.command.functions.bytecode.extract.DexSource;
import com.justnothing.testmodule.command.functions.bytecode.extract.DexSourceLocator;
import com.justnothing.testmodule.command.functions.bytecode.extract.DexTrust;
import com.justnothing.testmodule.command.functions.bytecode.request.BytecodeBatchExportRequest;
import com.justnothing.testmodule.command.functions.bytecode.request.BytecodeDumpRequest;
import com.justnothing.testmodule.command.functions.bytecode.request.BytecodeFindRequest;
import com.justnothing.testmodule.command.functions.bytecode.request.BytecodeListClassesRequest;
import com.justnothing.testmodule.command.functions.bytecode.request.BytecodeLocateRequest;
import com.justnothing.testmodule.command.functions.bytecode.response.BytecodeResult;
import com.justnothing.testmodule.command.functions.bytecode.util.DexDisassembler;
import com.justnothing.testmodule.command.framework.output.Colors;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * 与"把 dex 弄到手上"相关的子命令：{@code dump} / {@code batch_export} / {@code list_classes} /
 * {@code locate} / {@code find}。
 *
 * <p>这一批不依赖 {@code .class} 字节码 —— Android 上根本不存在那个东西（ART 只保留 dex）。
 * 原来的实现是拿 APK 里的 dex 去喂 ASM/CFR 这类 JVM 工具，必然失败；现在统一改成
 * "定位来源 → 提取 dex → 标注可信度"。</p>
 */
public class BytecodeManageCommand extends AbstractBytecodeCommand<CommandRequest<?>> {

    private static final int DEFAULT_LIST_LIMIT = 200;

    private static final int DEFAULT_FIND_LIMIT = 50;

    /**
     * 扫描类名时，单个来源最多读出多少个类名。
     *
     * <p>正常 dex 顶多几千个类，这个上限是防呆 —— 万一碰到畸形文件，
     * 不至于让 {@code class_defs_size} 里的垃圾值把内存吃光。</p>
     */
    private static final int MAX_CLASSES_PER_SOURCE = 200_000;

    /** {@code --disasm} 一次最多输出多少行（手表屏幕小，多了也没法看）。 */
    private static final int MAX_DISASM_LINES = 150;

    @SuppressWarnings("unchecked")
    public BytecodeManageCommand() {
        super("bytecode manage", (Class) CommandRequest.class);
    }

    @Override
    protected BytecodeResult executeInternal(CommandExecutor.CmdExecContext<CommandRequest<?>> context)
            throws Exception {
        CommandRequest request = context.getRequest();

        if (request instanceof BytecodeDumpRequest req) {
            return handleDump(req, context);
        } else if (request instanceof BytecodeBatchExportRequest req) {
            return handleBatchExport(req, context);
        } else if (request instanceof BytecodeListClassesRequest req) {
            return handleListClasses(req, context);
        } else if (request instanceof BytecodeLocateRequest req) {
            return handleLocate(req, context);
        } else if (request instanceof BytecodeFindRequest req) {
            return handleFind(req, context);
        }

        return buildErrorResult("不支持的请求类型: " + request.getClass().getSimpleName());
    }

    /**
     * 导出"某个类所在的 dex"。
     *
     * <p>输出目录而不是单个文件：一个来源（apk / vdex）里可能装着多个 dex，
     * 而反编译时必须把它们一起给到工具，否则跨 dex 的引用会解析不了。</p>
     */
    private BytecodeResult handleDump(BytecodeDumpRequest request,
            CommandExecutor.CmdExecContext<CommandRequest<?>> context) {
        String className = request.getClassName();
        Console console = context.supportsRichOutput() ? context.console() : null;

        // 定位阶段：候选文件数是确定的，可以给真实百分比
        List<DexSource> sources = console == null
                ? DexSourceLocator.locate(className)
                : locateWithProgress(console, className);

        if (sources.isEmpty()) {
            return buildErrorResult("没能在本进程的代码来源里找到类 " + className + "。\n"
                    + "用 `bytecode locate " + className + "` 可以看到到底扫了哪些文件。\n"
                    + "（类不在任何代码文件里 → 通常是运行时动态生成的。）");
        }
        DexSource source = sources.get(0);

        // 提取阶段：vdexExtractor 要跑几秒且耗时不确定，用 spinner 而不是百分比
        List<DexExtractor.Result> results;
        if (console == null) {
            results = DexExtractionManager.extract(source);
        } else {
            try (Status status = console.status("从 " + source.getKind() + " 提取 dex…")) {
                results = DexExtractionManager.extract(source);
            }
        }
        if (results.isEmpty()) {
            return buildErrorResult("类 " + className + " 确实在 " + source.getLabel()
                    + " 里，但没能把 dex 取出来。\n"
                    + "跑 `bytecode locate " + className + "` 可以看到这个来源当前有没有可用的提取策略。");
        }

        File outputDir = resolveOutputDir(request.getOutputPath(), "dump");
        if (outputDir == null) {
            return buildErrorResult("找不到可写的输出目录；请用 -o 指定一个（例如 /data/local/tmp/dex）");
        }
        try {
            ensureDirectory(outputDir);
        } catch (IOException e) {
            return buildErrorResult("无法创建输出目录: " + e.getMessage());
        }

        StringBuilder sb = new StringBuilder();
        List<String> written = new ArrayList<>();
        sb.append("类: ").append(className).append("\n");
        sb.append("来源: ").append(source.getLabel()).append("\n");
        sb.append("输出目录: ").append(outputDir.getAbsolutePath()).append("\n\n");

        DexTrust trust = null;
        // 一个来源（apk）里可能有好几个 dex，每个都要写几十 MB —— 给条进度，别让人以为卡死
        Progress writeProgress = console != null && results.size() > 1
                ? console.progress(c -> c.transientMode = true)
                : null;
        int writeTask = -1;
        if (writeProgress != null) {
            writeTask = writeProgress.addTask("写出 dex", results.size());
            writeProgress.start();
        }
        try {
            for (int i = 0; i < results.size(); i++) {
                // 推进放在开头：下面有 continue，放结尾会被跳过
                if (writeProgress != null) {
                    writeProgress.advance(writeTask, 1);
                }
                DexExtractor.Result result = results.get(i);
                trust = result.trust();

                String name = fileNameFor(className, i, results.size());
                File target = new File(outputDir, name);
                try {
                    writeOutput(target, result.dex());
                } catch (IOException e) {
                    logger.error("写出 " + target + " 失败", e);
                    sb.append("  ✗ ").append(name).append(" —— 写入失败: ").append(e.getMessage()).append("\n");
                    continue;
                }

                written.add(target.getAbsolutePath());
                sb.append("  ✓ ").append(name)
                        .append("  ").append(result.dex().length).append(" 字节")
                        .append("  [").append(result.trust().describe()).append("]\n")
                        .append("      ").append(result.note()).append("\n");
            }
        } finally {
            if (writeProgress != null) {
                writeProgress.close();
            }
        }

        sb.append("\n可信度: ").append(trust != null ? trust.describe() : "未知").append("\n");
        if (trust != null && !trust.isCodeTrustworthy()) {
            sb.append("注意: 这份 dex 已被 ART 改写（quicken），反编译出来的逻辑不可信，\n");
            sb.append("      但类/方法/字段结构仍然是准的。要拿到可信代码，先把它 de-quicken：\n");
            sb.append("      把上面「来源」那个文件拉到电脑上，用 vdexExtractor 处理后即可反编译。\n");
        } else {
            sb.append("直接用 jadx / baksmali 打开这个 dex 就能反编译。\n");
        }

        // 指令单独走一遍：终端上要高亮，纯文本那份再拼进 sb 供 output 字段用
        DexDisassembler.Result disasm = request.isDisasm()
                ? runDisassembly(className, written, sb)
                : null;

        out(context, sb.toString(), trust != null && trust.isCodeTrustworthy() ? Colors.LIGHT_GREEN : Colors.YELLOW);

        if (disasm != null) {
            sb.append("\n指令:\n\n").append(disasm.text());
            out(context, "\n指令:", Colors.DEFAULT);
            // dalvik 没有专用 lexer，借 java 的让注释/字符串/数字有个颜色区分
            printCode(context, disasm.text(), "java");
            if (disasm.truncated()) {
                String hint = "\n... 已截断（上限 " + MAX_DISASM_LINES + " 行），完整内容看上面导出的 dex。\n";
                sb.append(hint);
                out(context, hint, Colors.GRAY);
            }
        }

        BytecodeResult result = buildSuccessResult("dump", className, sb.toString());
        result.setSource(source.getLabel());
        result.setDexTrust(trust != null ? trust.name() : null);
        result.setFiles(written);
        return result;
    }

    /**
     * 反汇编刚导出的 dex（{@code --disasm}）。
     *
     * <p>走设备自带的 {@code /system/bin/dexdump}，不额外打包任何反编译库。
     * 失败原因写进 {@code sb}（用户要看得到），成功时把结果返回给调用方去高亮输出。</p>
     */
    private static DexDisassembler.Result runDisassembly(String className, List<String> dexFiles,
            StringBuilder sb) {
        if (dexFiles.isEmpty()) {
            sb.append("\n指令: 没有可反汇编的 dex（上面的写入都失败了）。\n");
            return null;
        }
        if (!DexDisassembler.available()) {
            sb.append("\n指令: 这台设备没有 /system/bin/dexdump，跳过反汇编。\n");
            return null;
        }

        DexDisassembler.Result result = DexDisassembler.disassembleClass(
                new File(dexFiles.get(0)), className, null, MAX_DISASM_LINES);
        if (result == null) {
            sb.append("\n指令: 没读到这个类的指令（dexdump 跑了但没匹配到）。\n");
            return null;
        }
        return result;
    }

    /**
     * 只做定位，不提取。
     *
     * <p>因为定位永远有效（只读类名表），这条命令在提取失败时是唯一的排查入口。</p>
     */
    private BytecodeResult handleLocate(BytecodeLocateRequest request,
            CommandExecutor.CmdExecContext<CommandRequest<?>> context) {
        String className = request.getClassName();
        Console console = context.supportsRichOutput() ? context.console() : null;

        List<DexSource> sources = console == null
                ? DexSourceLocator.locate(className)
                : locateWithProgress(console, className);

        StringBuilder sb = new StringBuilder();
        sb.append("类: ").append(className).append("\n");
        sb.append("命中来源: ").append(sources.size()).append(" 个\n\n");

        if (sources.isEmpty()) {
            sb.append("没有找到。这个类不是从本进程用到的任何代码文件里加载的\n");
            sb.append("（运行时动态生成 / 从网络或内存加载的类都会是这种情况）。\n");
            out(context, sb.toString(), Colors.YELLOW);
            return buildSuccessResult("locate", className, sb.toString());
        }

        for (DexSource source : sources) {
            DexExtractor extractor = DexExtractionManager.findExtractor(source);
            sb.append("  ").append(source.getKind()).append("  ").append(source.getLabel()).append("\n");
            sb.append("      可用的提取策略: ")
                    .append(extractor != null ? extractor.name() : "无（当前工具链处理不了这个格式）")
                    .append("\n");
        }
        sb.append("\n用 `bytecode dump ").append(className).append("` 把 dex 导出来。\n");

        out(context, sb.toString(), Colors.DEFAULT);
        return buildSuccessResult("locate", className, sb.toString());
    }

    /**
     * 列出类名。
     *
     * <p>不填 {@code -s} 时列出本进程所有代码来源；填了就只列那一个文件。
     * 和以前的区别：这里读的是 dex 的类名表（{@code class_defs}），
     * 而不是"反射出已加载的类再逐个尝试提取字节码" —— 后者在 ART 上必然全军覆没。</p>
     */
    private BytecodeResult handleListClasses(BytecodeListClassesRequest request,
            CommandExecutor.CmdExecContext<CommandRequest<?>> context) {
        int limit = request.getLimit() > 0 ? request.getLimit() : DEFAULT_LIST_LIMIT;

        List<DexSource> sources = new ArrayList<>();
        if (request.getSource() != null && !request.getSource().isEmpty()) {
            File file = new File(request.getSource());
            if (!file.isFile()) {
                return buildErrorResult("来源文件不存在: " + request.getSource());
            }
            sources.add(DexSource.of(DexSourceLocator.kindOfPath(file.getAbsolutePath()), file));
        } else {
            sources.addAll(DexSourceLocator.candidates());
        }

        StringBuilder sb = new StringBuilder();
        sb.append("来源: ").append(sources.size()).append(" 个，总共最多列出 ").append(limit).append(" 个类\n\n");

        Console console = context.supportsRichOutput() ? context.console() : null;
        // 同上：要 transient，否则进度条会一直在屏幕上留着
        Progress progress = console != null && !sources.isEmpty()
                ? console.progress(c -> c.transientMode = true)
                : null;
        int task = -1;
        if (progress != null) {
            task = progress.addTask("读取类名", sources.size());
            progress.start();
        }

        int total = 0;
        try {
            for (DexSource source : sources) {
                if (total >= limit) {
                    sb.append("... 已达到总量上限 ").append(limit).append("，停止\n");
                    break;
                }
                List<String> classes;
                try {
                    classes = DexClassIndex.listClasses(source.getFile(), limit - total);
                } catch (IOException e) {
                    sb.append(source.getLabel()).append("  —— 读取失败: ").append(e.getMessage()).append("\n\n");
                    if (progress != null) {
                        progress.advance(task, 1);
                    }
                    continue;
                }
                if (!classes.isEmpty()) {
                    sb.append(source.getLabel()).append("  (").append(classes.size()).append(" 个)\n");
                    for (String name : classes) {
                        sb.append("  ").append(name).append("\n");
                    }
                    sb.append("\n");
                    total += classes.size();
                }
                if (progress != null) {
                    progress.advance(task, 1);
                }
            }
        } finally {
            if (progress != null) {
                progress.close();
            }
        }

        if (total == 0) {
            sb.append("没列出任何类。可能这些来源里没有 dex（framework jar 在多数设备上是空壳）。\n");
        } else {
            sb.append("合计 ").append(total).append(" 个类。\n");
        }

        out(context, sb.toString(), Colors.DEFAULT);
        return buildSuccessResult("list_classes", request.getSource(), sb.toString());
    }

    /**
     * 按关键词模糊搜索类名。
     *
     * <p>和 {@link #handleListClasses} 互补：那个回答"这个来源里有哪些类名"，需要你已知来源；
     * 这个回答"哪儿有名字长这样的类"。逆向时通常后者才是第一步 —— 手头只有一个模糊印象
     * （"某个 xxxManager"、"com.xtc 下面的东西"），拿到确切类名之后才轮到
     * {@code source} / {@code dump}。</p>
     *
     * <p>用<b>子串匹配、不区分大小写</b>：dex 里的类名是全限定形式（{@code com.a.b.C}），
     * 用户可能输入包名片段、也可能输入类名片段，子串匹配两种都覆盖，不必让用户去区分
     * "前缀 / 后缀 / 通配符"。</p>
     *
     * <p>逐来源扫描、命中够数就停，所以最坏情况（什么也没匹配上）需要把全部来源扫一遍 ——
     * boot classpath 上有十几个 vdex，这就是进度条存在的原因。</p>
     */
    private BytecodeResult handleFind(BytecodeFindRequest request,
            CommandExecutor.CmdExecContext<CommandRequest<?>> context) {
        String rawKeyword = request.getKeyword();
        if (rawKeyword == null || rawKeyword.trim().isEmpty()) {
            return buildErrorResult("请给出要搜索的关键词，例如: bytecode find ActivityManager");
        }
        String keyword = rawKeyword.trim();
        String needle = keyword.toLowerCase(Locale.ROOT);
        int limit = request.getLimit() > 0 ? request.getLimit() : DEFAULT_FIND_LIMIT;

        List<DexSource> sources = new ArrayList<>();
        if (request.getSource() != null && !request.getSource().isEmpty()) {
            File file = new File(request.getSource());
            if (!file.isFile()) {
                return buildErrorResult("来源文件不存在: " + request.getSource());
            }
            sources.add(DexSource.of(DexSourceLocator.kindOfPath(file.getAbsolutePath()), file));
        } else {
            sources.addAll(DexSourceLocator.candidates());
        }

        Console console = context.supportsRichOutput() ? context.console() : null;
        // 同 list_classes：要 transient，否则进度条会留在屏幕上
        Progress progress = console != null && !sources.isEmpty()
                ? console.progress(c -> c.transientMode = true)
                : null;
        int task = -1;
        if (progress != null) {
            task = progress.addTask("搜索类名", sources.size());
            progress.start();
        }

        StringBuilder sb = new StringBuilder();
        sb.append("关键词: ").append(keyword)
                .append("    来源: ").append(sources.size()).append(" 个\n\n");

        int total = 0;
        boolean limited = false;
        try {
            for (DexSource source : sources) {
                if (total >= limit) {
                    limited = true;
                    sb.append("... 已达到上限 ").append(limit).append(" 个命中，停止扫描\n");
                    break;
                }
                List<String> classes;
                try {
                    classes = DexClassIndex.listClasses(source.getFile(), MAX_CLASSES_PER_SOURCE);
                } catch (IOException e) {
                    sb.append(source.getLabel()).append("  —— 读取失败: ").append(e.getMessage()).append("\n\n");
                    if (progress != null) {
                        progress.advance(task, 1);
                    }
                    continue;
                }

                List<String> hits = new ArrayList<>();
                for (String name : classes) {
                    if (name.toLowerCase(Locale.ROOT).contains(needle)) {
                        hits.add(name);
                        if (total + hits.size() >= limit) {
                            limited = true;
                            break;
                        }
                    }
                }
                if (!hits.isEmpty()) {
                    sb.append(source.getLabel()).append("  (").append(hits.size()).append(" 个)\n");
                    for (String name : hits) {
                        sb.append("  ").append(name).append("\n");
                    }
                    sb.append("\n");
                    total += hits.size();
                }
                if (progress != null) {
                    progress.advance(task, 1);
                }
            }
        } finally {
            if (progress != null) {
                progress.close();
            }
        }

        if (total == 0) {
            sb.append("没有匹配的类名。可以换个更短的子串（大小写不敏感），\n");
            sb.append("或者用 `bytecode list_classes -s <文件>` 看看某个来源里到底有哪些类。\n");
        } else {
            sb.append("合计命中 ").append(total).append(" 个");
            if (limited) {
                sb.append("（已到上限，用 -l 可以调大）");
            }
            sb.append("。拿到类名后可以 `bytecode source <类名>` 看源码。\n");
        }

        out(context, sb.toString(), Colors.DEFAULT);
        return buildSuccessResult("find", request.getSource(), sb.toString());
    }

    /**
     * 批量导出 dex。
     *
     * <p>不填 {@code -s} 时只处理 apk / jar —— 它们不需要任何外部工具（纯解压），
     * 快且不会失败；vdex / odex 要跑 de-quicken，代价大得多，必须由调用方显式指定。</p>
     */
    private BytecodeResult handleBatchExport(BytecodeBatchExportRequest request,
            CommandExecutor.CmdExecContext<CommandRequest<?>> context) {
        int limit = request.getLimit() > 0 ? request.getLimit() : 32;

        List<File> inputs = collectInputs(request.getSource(), limit);
        if (inputs.isEmpty()) {
            return buildErrorResult(request.getSource() != null && !request.getSource().isEmpty()
                    ? "没有在 " + request.getSource() + " 里找到可处理的文件（apk/jar/dex/vdex/odex）"
                    : "没找到当前应用的 APK；可以用 -s 指定来源文件或目录");
        }

        File outputDir = resolveOutputDir(request.getOutputPath(), "batch");
        if (outputDir == null) {
            return buildErrorResult("找不到可写的输出目录；请用 -o 指定一个（例如 /data/local/tmp/dex）");
        }
        try {
            ensureDirectory(outputDir);
        } catch (IOException e) {
            return buildErrorResult("无法创建输出目录: " + e.getMessage());
        }

        StringBuilder sb = new StringBuilder();
        List<String> written = new ArrayList<>();
        int failed = 0;
        sb.append("输出目录: ").append(outputDir.getAbsolutePath()).append("\n");
        sb.append("待处理: ").append(inputs.size()).append(" 个来源\n\n");

        // 每个来源都要提取 + 写盘，vdex 的还原还可能跑几十秒 —— 必须有进度反馈。
        // 只有真的会有多条时才去取 console（取它会构造 JLine 终端，实测 ~530ms）。
        Console console = inputs.size() > 1 && context.supportsRichOutput() ? context.console() : null;
        Progress progress = console != null
                ? console.progress(c -> c.transientMode = true)
                : null;
        int task = -1;
        if (progress != null) {
            task = progress.addTask("导出 dex", inputs.size());
            progress.start();
        }

        try {
            for (File input : inputs) {
                // 推进放在开头：下面有几处 continue，放结尾会被跳过
                if (progress != null) {
                    progress.advance(task, 1);
                }

                // 每个来源一个子目录：不同来源都可能有 classes.dex，放一起会互相覆盖
                File subDir = new File(outputDir, sanitize(input.getName())
                        + "-" + Integer.toHexString(input.getAbsolutePath().hashCode()));
                try {
                    ensureDirectory(subDir);
                } catch (IOException e) {
                    failed++;
                    sb.append("  ✗ ").append(input.getAbsolutePath())
                            .append("  —— 无法创建输出子目录: ").append(e.getMessage()).append("\n");
                    continue;
                }

                DexSource source = DexSource.of(DexSourceLocator.kindOfPath(input.getAbsolutePath()), input);
                List<DexExtractor.Result> results = DexExtractionManager.extract(source);
                if (results.isEmpty()) {
                    failed++;
                    sb.append("  ✗ ").append(input.getAbsolutePath()).append("  —— 取不出 dex\n");
                    continue;
                }

                sb.append("  ").append(input.getAbsolutePath()).append("\n");
                for (int i = 0; i < results.size(); i++) {
                    DexExtractor.Result result = results.get(i);
                    File target = new File(subDir, dexFileName(input.getName(), i, results.size()));
                    try {
                        writeOutput(target, result.dex());
                    } catch (IOException e) {
                        logger.error("写出 " + target + " 失败", e);
                        failed++;
                        sb.append("      ✗ ").append(target.getName()).append(" 写入失败\n");
                        continue;
                    }
                    written.add(target.getAbsolutePath());
                    sb.append("      ✓ ").append(target.getName())
                            .append("  ").append(result.dex().length).append(" 字节")
                            .append("  [").append(result.trust().describe()).append("]\n");
                }
            }
        } finally {
            if (progress != null) {
                progress.close();
            }
        }

        sb.append("\n合计导出 ").append(written.size()).append(" 个 dex")
                .append(failed > 0 ? "，" + failed + " 个来源失败" : "").append("\n");

        out(context, sb.toString(), failed > 0 ? Colors.YELLOW : Colors.LIGHT_GREEN);

        BytecodeResult result = buildSuccessResult("batch_export", request.getSource(), sb.toString());
        result.setFiles(written);
        return result;
    }

    // ---------- 辅助 ----------

    /** 带进度条地定位类：候选文件数是确定的，所以能给出真实百分比。 */
    private static List<DexSource> locateWithProgress(Console console, String className) {
        // 进度条默认是 non-transient：stop() 之后那一帧会留在屏幕上，后续输出会紧贴着它。
        // 这里要它自清（rich 的 transient 语义），否则每跑一次就留一条。
        Progress progress = console.progress(c -> c.transientMode = true);
        try {
            return DexSourceLocator.locate(className, new DexSourceLocator.ScanListener() {

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
    }

    /**
     * 决定要处理哪些来源文件。
     *
     * <p>不指定 {@code -s} 时走"当前进程的 apk/jar"，并<b>刻意排除 vdex/odex</b>：
     * 前者是纯解压，后者每个都要跑一次 de-quicken（可能几十秒），
     * 无差别地对几十个 framework vdex 跑一遍会把设备拖死。</p>
     */
    private static List<File> collectInputs(String sourcePath, int limit) {
        List<File> inputs = new ArrayList<>();

        if (sourcePath != null && !sourcePath.isEmpty()) {
            File file = new File(sourcePath);
            if (file.isFile()) {
                inputs.add(file);
            } else if (file.isDirectory()) {
                collectRecursively(file, inputs, limit);
            }
            return inputs;
        }

        for (DexSource source : DexSourceLocator.candidates()) {
            DexSource.Kind kind = source.getKind();
            if (kind == DexSource.Kind.APK || kind == DexSource.Kind.JAR) {
                inputs.add(source.getFile());
                if (inputs.size() >= limit) {
                    break;
                }
            }
        }
        return inputs;
    }

    private static void collectRecursively(File dir, List<File> out, int limit) {
        if (out.size() >= limit) {
            return;
        }
        File[] children = dir.listFiles();
        if (children == null) {
            return;
        }
        Arrays.sort(children, Comparator.comparing(File::getName));
        for (File child : children) {
            if (out.size() >= limit) {
                return;
            }
            if (child.isDirectory()) {
                collectRecursively(child, out, limit);
            } else if (isCodeContainer(child.getName())) {
                out.add(child);
            }
        }
    }

    private static boolean isCodeContainer(String fileName) {
        String lower = fileName.toLowerCase(Locale.ROOT);
        return lower.endsWith(".apk") || lower.endsWith(".jar") || lower.endsWith(".zip")
                || lower.endsWith(".dex") || lower.endsWith(".vdex") || lower.endsWith(".odex");
    }

    private static String fileNameFor(String className, int index, int total) {
        String base = sanitize(className.replace('.', '_'));
        return total <= 1 ? base + ".dex" : base + "-" + (index + 1) + ".dex";
    }

    /** {@code base.apk} → {@code base-1.dex} / {@code base-2.dex}；单 dex 时就是 {@code base.dex}。 */
    private static String dexFileName(String sourceName, int index, int total) {
        int dot = sourceName.lastIndexOf('.');
        String base = sanitize(dot > 0 ? sourceName.substring(0, dot) : sourceName);
        return total <= 1 ? base + ".dex" : base + "-" + (index + 1) + ".dex";
    }

    private static String sanitize(String name) {
        return name.replaceAll("[^A-Za-z0-9._-]", "_");
    }
}
