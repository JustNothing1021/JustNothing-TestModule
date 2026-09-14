package com.justnothing.testmodule.command.functions.bytecode.impl;

import com.justnothing.testmodule.command.framework.CommandExecutor;
import com.justnothing.testmodule.command.framework.model.CommandRequest;
import com.justnothing.testmodule.command.functions.bytecode.extract.ClassDexExtractor;
import com.justnothing.testmodule.command.functions.bytecode.extract.DexHeader;
import com.justnothing.testmodule.command.functions.bytecode.extract.DexSource;
import com.justnothing.testmodule.command.functions.bytecode.extract.DexSourceLocator;
import com.justnothing.testmodule.command.functions.bytecode.request.BytecodeAnalyzeRequest;
import com.justnothing.testmodule.command.functions.bytecode.request.BytecodeConstantsRequest;
import com.justnothing.testmodule.command.functions.bytecode.request.BytecodeDisasmRequest;
import com.justnothing.testmodule.command.functions.bytecode.request.BytecodeInfoRequest;
import com.justnothing.testmodule.command.functions.bytecode.request.BytecodeMethodRequest;
import com.justnothing.testmodule.command.functions.bytecode.request.BytecodeSourceRequest;
import com.justnothing.testmodule.command.functions.bytecode.request.BytecodeVerifyRequest;
import com.justnothing.testmodule.command.functions.bytecode.response.BytecodeResult;
import com.justnothing.testmodule.command.functions.bytecode.util.DexDisassembler;
import com.justnothing.testmodule.command.functions.bytecode.util.DexToJava;
import com.justnothing.testmodule.command.framework.output.Colors;
import com.justnothing.testmodule.command.framework.utils.CommandExceptionHandler;
import com.justnothing.richconsole.console.Console;
import com.justnothing.richconsole.noneprompt.CancelledException;
import com.justnothing.richconsole.noneprompt.ConfirmPrompt;
import com.justnothing.richconsole.status.Status;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * bytecode 域的"查询"类子命令。
 *
 * <p>分成两类：</p>
 * <ul>
 *   <li><b>反射能答的</b>（{@code info} / {@code method} / {@code constants}）——
 *       这些反映的是运行时真实状态，本来就没问题，保留。</li>
 *   <li><b>要看字节码的</b>（{@code analyze} / {@code verify} / {@code dump}）——
 *       以前按 JVM class 格式解析（{@code 0xCAFEBABE} + 常量池 + ASM/CFR），
 *       在 Android 上必然失败；现在改成解析 dex，语义也跟着改成 dex 口径。</li>
 * </ul>
 */
public class BytecodeQueryCommand extends AbstractBytecodeCommand<CommandRequest<?>> {

    /** dex 超过这个体积就先问一句 —— 手表上这一步是分钟级的，不该点了就跑。 */
    private static final long CONFIRM_DEX_BYTES = 8L * 1024 * 1024;

    /** 跑到这么多秒还没完，就在 spinner 上开始报秒数。 */
    private static final int SLOW_HINT_SECONDS = 10;

    /** 超过这个秒数就提示"超出预期、可以中断"。 */
    private static final int VERY_SLOW_HINT_SECONDS = 45;

    /** 反汇编直接打到屏幕上时最多多少行（手表屏小，多了没法看）。 */
    private static final int MAX_DISASM_LINES = 150;

    @SuppressWarnings("unchecked")
    public BytecodeQueryCommand() {
        super("bytecode query", (Class) CommandRequest.class);
    }

    @Override
    protected BytecodeResult executeInternal(CommandExecutor.CmdExecContext<CommandRequest<?>> context)
            throws Exception {
        CommandRequest request = context.getRequest();
        ClassLoader classLoader = context.classLoader();

        if (request instanceof BytecodeInfoRequest req) {
            return handleInfo(req, classLoader, context);
        } else if (request instanceof BytecodeMethodRequest req) {
            return handleMethod(req, classLoader, context);
        } else if (request instanceof BytecodeAnalyzeRequest req) {
            return handleAnalyze(req, context);
        } else if (request instanceof BytecodeDisasmRequest req) {
            return handleDisasm(req, context);
        } else if (request instanceof BytecodeConstantsRequest req) {
            return handleConstants(req, classLoader, context);
        } else if (request instanceof BytecodeVerifyRequest req) {
            return handleVerify(req, context);
        } else if (request instanceof BytecodeSourceRequest req) {
            return handleSource(req, context);
        }

        return buildErrorResult("不支持的请求类型: " + request.getClass().getSimpleName());
    }

    /** 类的基本元数据 —— 纯反射，反映运行时真实状态。 */
    private BytecodeResult handleInfo(BytecodeInfoRequest request, ClassLoader classLoader,
            CommandExecutor.CmdExecContext<CommandRequest<?>> context) {
        String className = request.getClassName();
        boolean verbose = request.isVerbose();

        try {
            Class<?> targetClass = loadClass(className, classLoader);

            StringBuilder sb = new StringBuilder();
            sb.append("类名: ").append(targetClass.getName()).append("\n");
            sb.append("类加载器: ").append(targetClass.getClassLoader()).append("\n");
            sb.append("包名: ").append(targetClass.getPackage() != null ? targetClass.getPackage().getName() : "无")
                    .append("\n");
            sb.append("修饰符: ").append(Modifier.toString(targetClass.getModifiers())).append("\n");
            sb.append("父类: ").append(targetClass.getSuperclass() != null ? targetClass.getSuperclass().getName() : "无")
                    .append("\n");

            Class<?>[] interfaces = targetClass.getInterfaces();
            sb.append("接口: ").append(interfaces.length).append(" 个\n");
            if (verbose) {
                for (Class<?> iface : interfaces) {
                    sb.append("  - ").append(iface.getName()).append("\n");
                }
            }

            Method[] methods = targetClass.getDeclaredMethods();
            sb.append("方法: ").append(methods.length).append(" 个\n");
            if (verbose) {
                for (Method method : methods) {
                    sb.append("  - ").append(method.getName())
                            .append("(").append(Arrays.toString(method.getParameterTypes())).append(")\n");
                }
            }

            Field[] fields = targetClass.getDeclaredFields();
            sb.append("字段: ").append(fields.length).append(" 个\n");
            if (verbose) {
                for (Field field : fields) {
                    sb.append("  - ").append(field.getType().getSimpleName()).append(" ").append(field.getName())
                            .append("\n");
                }
            }

            out(context, sb.toString(), Colors.DEFAULT);
            return buildSuccessResult("info", className, sb.toString());

        } catch (Exception e) {
            logger.error("获取类信息失败", e);
            String errorMsg = CommandExceptionHandler.handleException("bytecode", e, context, "获取类信息失败");
            return buildErrorResult(errorMsg);
        }
    }

    /**
     * 方法元数据。
     *
     * <p>方法体拿不到 —— 反射不暴露它，而 ART 又不保留 {@code .class}。
     * 想要指令只能导出 dex 到电脑上用 baksmali / jadx 看。</p>
     */
    private BytecodeResult handleMethod(BytecodeMethodRequest request, ClassLoader classLoader,
            CommandExecutor.CmdExecContext<CommandRequest<?>> context) {
        String className = request.getClassName();
        String methodName = request.getMethodName();

        try {
            Class<?> targetClass = loadClass(className, classLoader);

            Method targetMethod = null;
            for (Method method : targetClass.getDeclaredMethods()) {
                if (method.getName().equals(methodName)) {
                    targetMethod = method;
                    break;
                }
            }
            if (targetMethod == null) {
                return buildErrorResult("找不到方法: " + methodName);
            }

            StringBuilder sb = new StringBuilder();
            sb.append("方法: ").append(targetMethod.getName()).append("\n");
            sb.append("修饰符: ").append(Modifier.toString(targetMethod.getModifiers())).append("\n");
            sb.append("返回类型: ").append(targetMethod.getReturnType().getName()).append("\n");
            sb.append("参数类型: ").append(Arrays.toString(targetMethod.getParameterTypes())).append("\n");
            sb.append("异常: ").append(Arrays.toString(targetMethod.getExceptionTypes())).append("\n");

            sb.append("\n方法体拿不到：反射不暴露方法体，而 Android 上 ART 也不保留 JVM 的 .class。\n");
            // 这里刻意只"定位"不"提取"：定位只读类名表，永远有效；提取要复制整个 dex
            // （可能几十 MB），甚至 fork 一个 vdexExtractor —— 只是想告诉用户往哪走，不值这个代价。
            List<DexSource> sources = DexSourceLocator.locate(className);
            if (sources.isEmpty()) {
                sb.append("也定位不到它落在哪个文件里（可能是运行期动态生成的类）。\n");
            } else {
                sb.append("它所在的来源:\n");
                for (DexSource source : sources) {
                    sb.append("  - ").append(source.getKind()).append("  ")
                            .append(source.getLabel()).append("\n");
                }
                sb.append("要看方法体:\n");
                sb.append("  bytecode dump ").append(className).append("    导出 dex\n");
                sb.append("  bytecode source ").append(className).append("  直接在设备上反编译成 Java\n");
            }

            out(context, sb.toString(), Colors.DEFAULT);
            return buildSuccessResult("method", className, sb.toString());

        } catch (Exception e) {
            String errorMsg = CommandExceptionHandler.handleException("bytecode", e, context, "获取方法信息失败");
            return buildErrorResult(errorMsg);
        }
    }

    /**
     * 分析类所在 dex 的结构。
     *
     * <p>以前这里按 JVM class 头部读"魔数 / 版本 / 常量池大小"，但拿到的其实是 dex，
     * 于是显示的字段全是错的（比如把 dex 的 checksum 当成版本号）。现在直接读 dex 头部。</p>
     */
    private BytecodeResult handleAnalyze(BytecodeAnalyzeRequest request,
            CommandExecutor.CmdExecContext<CommandRequest<?>> context) {
        String className = request.getClassName();

        ClassDexExtractor.Outcome outcome = resolveDex(className, context);
        if (outcome == null) {
            return buildErrorResult("没能取出 " + className + " 所在的 dex，无法分析。\n"
                    + "先用 `bytecode locate " + className + "` 确认它落在哪个文件里。");
        }

        byte[] dex = outcome.result().dex();
        DexHeader header = DexHeader.parse(dex);
        if (header == null) {
            return buildErrorResult("取出来的字节不是合法 dex（魔数不对），无法分析。");
        }

        StringBuilder sb = new StringBuilder();
        sb.append("类: ").append(className).append("\n");
        sb.append("来源: ").append(outcome.source().getLabel()).append("\n");
        sb.append("可信度: ").append(outcome.result().trust().describe()).append("\n\n");
        sb.append(header.describe());
        sb.append("\nchecksum: ")
                .append(header.checksumMatches(dex) ? "自洽（内容未被改写）" : "对不上（内容被改写过）")
                .append("\n");

        if (request.isVerbose()) {
            sb.append("\ndex 实际大小: ").append(dex.length).append(" 字节\n");
            sb.append("提取说明: ").append(outcome.result().note()).append("\n");
        }

        out(context, sb.toString(), Colors.DEFAULT);
        BytecodeResult result = buildSuccessResult("analyze", className, sb.toString());
        result.setSource(outcome.source().getLabel());
        result.setDexTrust(outcome.result().trust().name());
        return result;
    }

    /**
     * 反汇编：把类（或某个方法）的 dalvik 指令打出来。
     *
     * <p>走设备自带的 {@code /system/bin/dexdump} —— 不打包任何反编译库，也几乎不吃内存。
     * 它和 {@link #handleSource} 是互补的两条路：</p>
     * <ul>
     *   <li><b>想知道代码在做什么</b> → 用 {@code source}（Java 源码，好读）</li>
     *   <li><b>想知道编译器实际生成了什么</b>（内联、泛型擦除、异常表、synchronized 的实现），
     *       或者 {@code source} 因为 dex 被 quicken 而失败时 → 用这里。
     *       dexdump 读的是指令本身，被 ART 改写过的 dex 照样 dump 得出来。</li>
     * </ul>
     */
    private BytecodeResult handleDisasm(BytecodeDisasmRequest request,
            CommandExecutor.CmdExecContext<CommandRequest<?>> context) {
        String className = request.getClassName();
        String methodName = request.getMethodName();

        if (!DexDisassembler.available()) {
            return buildErrorResult("这台设备没有 /system/bin/dexdump，没法在这里反汇编。\n"
                    + "可以改用 `bytecode dump " + className + "` 导出 dex，在电脑上用 baksmali 看。");
        }

        ClassDexExtractor.Outcome outcome = resolveDex(className, context);
        if (outcome == null) {
            return buildErrorResult("没能取出 " + className + " 所在的 dex，无法反汇编。\n"
                    + "先用 `bytecode locate " + className + "` 确认它落在哪个文件里。");
        }

        String outputPath = request.getOutputPath();
        boolean toFile = outputPath != null && !outputPath.isEmpty();
        // 写文件时不截断：用户既然要存下来，就该拿到完整内容
        int maxLines = toFile ? Integer.MAX_VALUE : MAX_DISASM_LINES;

        File workDir = resolveOutputDir(null, "disasm");
        if (workDir == null) {
            return buildErrorResult("找不到可写的中间目录，无法落盘 dex");
        }

        DexDisassembler.Result disasm;
        try {
            // dexdump 只接受文件路径，没法从内存喂给它
            File dexFile = new File(workDir, "input.dex");
            writeOutput(dexFile, outcome.result().dex());
            disasm = DexDisassembler.disassembleClass(dexFile, className, methodName, maxLines);
        } catch (IOException e) {
            logger.error("反汇编 " + className + " 失败", e);
            return buildErrorResult("反汇编失败: " + e.getMessage());
        } finally {
            // 中间目录里那份 dex 可能有几十 MB，用完必须清掉
            deleteQuietly(workDir);
        }

        if (disasm == null) {
            return buildErrorResult(methodName != null && !methodName.isEmpty()
                    ? "没读到指令：这个类里没有名为 " + methodName + " 的方法（也可能它被内联掉了）"
                    : "没读到指令：dexdump 跑完了，但输出里没有这个类");
        }

        boolean trustworthy = outcome.result().trust().isCodeTrustworthy();
        StringBuilder sb = new StringBuilder();
        sb.append("类: ").append(className).append("\n");
        if (methodName != null && !methodName.isEmpty()) {
            sb.append("方法: ").append(methodName).append("\n");
        }
        sb.append("来源: ").append(outcome.source().getLabel()).append("\n");
        sb.append("指令: dalvik（dexdump -d）\n\n");

        if (toFile) {
            File target = new File(outputPath);
            if (target.isDirectory()) {
                target = new File(target, className.replace('.', '_').replace('$', '_') + ".smali");
            }
            try {
                writeOutput(target, disasm.text().getBytes(StandardCharsets.UTF_8));
            } catch (IOException e) {
                logger.error("写出 " + target + " 失败", e);
                return buildErrorResult("写出指令失败: " + e.getMessage());
            }
            sb.append("已写入: ").append(target.getAbsolutePath())
                    .append("  (").append(disasm.text().length()).append(" 字符)\n");
            String summary = sb.toString();
            out(context, summary, Colors.LIGHT_GREEN);

            BytecodeResult result = buildSuccessResult("disasm", className, summary);
            result.setSource(outcome.source().getLabel());
            result.setDexTrust(outcome.result().trust().name());
            result.setFiles(Collections.singletonList(target.getAbsolutePath()));
            return result;
        }

        String header = sb.toString();
        String instructions = disasm.text();

        StringBuilder tail = new StringBuilder();
        if (disasm.truncated()) {
            tail.append("\n... 已截断（上限 ").append(MAX_DISASM_LINES)
                    .append(" 行）。看全部用 -o <文件>，或加上方法名缩小范围。\n");
        }
        if (!trustworthy) {
            tail.append("\n注意: 这份 dex 被 ART 改写（").append(outcome.result().trust().describe())
                    .append("），指令里的字段/方法索引指向优化后的槽位，\n");
            tail.append("      和人写的代码对不上。结构能看，语义别信。\n");
        }

        String text = header + instructions + tail;
        out(context, header, trustworthy ? Colors.DEFAULT : Colors.YELLOW);
        // dalvik 指令没有专用 lexer，借 java 的只是为了让注释、字符串、数字有个颜色区分；
        // 指令名会被当成普通标识符，不影响可读性。
        printCode(context, instructions, "java");
        if (tail.length() > 0) {
            out(context, tail.toString(), Colors.YELLOW);
        }

        BytecodeResult result = buildSuccessResult("disasm", className, text);
        result.setSource(outcome.source().getLabel());
        result.setDexTrust(outcome.result().trust().name());
        return result;
    }

    /** 静态常量字段 —— 反射能拿到的那部分本来就是对的，这里只去掉"常量池"这个 JVM 说法。 */
    private BytecodeResult handleConstants(BytecodeConstantsRequest request, ClassLoader classLoader,
            CommandExecutor.CmdExecContext<CommandRequest<?>> context) {
        String className = request.getClassName();

        try {
            Class<?> targetClass = loadClass(className, classLoader);

            StringBuilder sb = new StringBuilder();
            sb.append("静态常量字段: ").append(className).append("\n\n");
            sb.append("dex 里没有 JVM 那种\"常量池\"，所以这里列的是反射能读到的\n");
            sb.append("static final 字段值 —— 真正有信息量的那部分。\n\n");

            int count = 0;
            for (Field field : targetClass.getDeclaredFields()) {
                int modifiers = field.getModifiers();
                if (!Modifier.isStatic(modifiers) || !Modifier.isFinal(modifiers)) {
                    continue;
                }
                count++;
                String value;
                try {
                    field.setAccessible(true);
                    Object raw = field.get(null);
                    value = raw == null ? "null" : raw.toString();
                } catch (Throwable t) {
                    value = "[读不到: " + t.getClass().getSimpleName() + "]";
                }
                sb.append("  ").append(field.getType().getSimpleName())
                        .append(" ").append(field.getName())
                        .append(" = ").append(value).append("\n");
            }
            if (count == 0) {
                sb.append("  （没有 static final 字段）\n");
            }

            out(context, sb.toString(), Colors.DEFAULT);
            return buildSuccessResult("constants", className, sb.toString());

        } catch (Exception e) {
            String errorMsg = CommandExceptionHandler.handleException("bytecode", e, context, "获取常量字段失败");
            return buildErrorResult(errorMsg);
        }
    }

    /** 校验类所在 dex 的完整性：魔数、头部自洽、checksum。 */
    private BytecodeResult handleVerify(BytecodeVerifyRequest request,
            CommandExecutor.CmdExecContext<CommandRequest<?>> context) {
        String className = request.getClassName();

        ClassDexExtractor.Outcome outcome = resolveDex(className, context);
        if (outcome == null) {
            return buildErrorResult("没能取出 " + className + " 所在的 dex，无法校验。\n"
                    + "先用 `bytecode locate " + className + "` 确认它落在哪个文件里。");
        }

        byte[] dex = outcome.result().dex();
        StringBuilder sb = new StringBuilder();
        sb.append("dex 校验: ").append(className).append("\n");
        sb.append("来源: ").append(outcome.source().getLabel()).append("\n\n");

        boolean valid = true;
        DexHeader header = DexHeader.parse(dex);
        if (header == null) {
            sb.append("✗ 魔数不对，这不是一个 dex\n");
            valid = false;
        } else {
            sb.append("✓ 魔数 dex ").append(header.version()).append("\n");

            if (header.plausible(dex.length)) {
                sb.append("✓ 头部声明的大小与文件长度自洽\n");
            } else {
                sb.append("✗ 头部声明 file_size=").append(header.fileSize())
                        .append("，实际 ").append(dex.length).append(" 字节，对不上\n");
                valid = false;
            }

            if (header.checksumMatches(dex)) {
                sb.append("✓ checksum 自洽 —— 内容未被改写\n");
            } else {
                sb.append("✗ checksum 对不上 —— 内容被改写过（很可能被 ART quicken 过）\n");
                valid = false;
            }
        }

        sb.append("\n可信度: ").append(outcome.result().trust().describe()).append("\n");
        sb.append("结论: ").append(valid
                ? "✓ 文件结构与头部一致，可以直接拿去反编译"
                : "✗ 头部与内容不一致；类/方法/字段结构仍然可读，但代码不可信").append("\n");

        out(context, sb.toString(), valid ? Colors.LIGHT_GREEN : Colors.YELLOW);
        BytecodeResult result = buildSuccessResult("verify", className, sb.toString());
        result.setSource(outcome.source().getLabel());
        result.setDexTrust(outcome.result().trust().name());
        return result;
    }

    /**
     * 在设备上把类反编译成 Java 源码。
     *
     * <p>链路见 {@link DexToJava}：dex2jar 只转目标类、CFR 只反编译这一个 class，
     * 所以峰值内存与 dex 里有多少类无关 —— 这正是它替代 jadx 的原因
     * （jadx 要先把整个 dex 建成 IR，手表上分不出那个内存）。</p>
     *
     * <p>不保证质量：dex2jar 把 dalvik 转成 JVM 字节码时会丢掉部分结构信息，
     * 可读性明显不如电脑上的 jadx。这个子命令的定位是"在手表上看个大概"。</p>
     */
    private BytecodeResult handleSource(BytecodeSourceRequest request,
            CommandExecutor.CmdExecContext<CommandRequest<?>> context) {
        String className = request.getClassName();

        ClassDexExtractor.Outcome outcome = resolveDex(className, context);
        if (outcome == null) {
            return buildErrorResult("没能取出 " + className + " 所在的 dex，无法反编译。\n"
                    + "先用 `bytecode locate " + className + "` 确认它落在哪个文件里。");
        }

        StringBuilder sb = new StringBuilder();
        boolean trustworthy = outcome.result().trust().isCodeTrustworthy();
        if (!trustworthy) {
            sb.append("注意: 这份 dex 已被 ART 改写（").append(outcome.result().trust().describe())
                    .append("），下面反编译出的逻辑不可信，只能当结构参考。\n\n");
        }

        File workDir = resolveOutputDir(null, "source");
        if (workDir == null) {
            return buildErrorResult("找不到可写的中间目录，无法落盘 dex");
        }

        // 注意用 supportsRichOutput() 而不是 console() != null：agent 在目标进程里执行时
        // console() 返回的是"写进去就丢"的占位对象，用它弹确认框只会等不到回答。
        Console console = context.supportsRichOutput() ? context.console() : null;
        long dexBytes = outcome.result().dex().length;

        // 大 dex 先问一句。只在真能交互时问：不能交互的场景下弹确认没人回答，会把命令挂住。
        if (console != null && dexBytes >= CONFIRM_DEX_BYTES
                && !confirmBigDex(console, className, dexBytes)) {
            deleteQuietly(workDir);
            return buildErrorResult("已取消（这份 dex 有 " + (dexBytes / 1024 / 1024) + " MB）");
        }

        DexToJava.Result decompiled;
        try {
            // dex 落盘再喂进去：dex2jar 与 CFR 都是按文件读，堆里只留目标类的那部分
            File dexFile = new File(workDir, "input.dex");
            writeOutput(dexFile, outcome.result().dex());
            decompiled = decompileWithStatus(console, className, dexFile, workDir);
        } catch (Throwable t) {
            logger.error("反编译 " + className + " 失败", t);
            deleteQuietly(workDir);
            return buildErrorResult("反编译失败: " + t);
        }

        if (decompiled == null) {
            sb.append("没有产出源码。常见原因:\n");
            sb.append("  - 这份 dex 被 ART 改写过（quicken），dex2jar 读不懂里面的指令\n");
            sb.append("  - 这个类只有 native / 抽象方法，没有可还原的方法体\n");
            sb.append("日志里搜 DexToJava 有更具体的原因。");
            out(context, sb.toString(), Colors.YELLOW);
            deleteQuietly(workDir);
            return buildErrorResult(sb.toString());
        }

        sb.append("类: ").append(className).append("\n");
        sb.append("来源: ").append(outcome.source().getLabel()).append("\n");
        sb.append("链路: dex2jar + CFR（按类处理，峰值堆 ")
                .append(decompiled.peakHeapBytes() / 1024 / 1024).append(" MB，耗时 ")
                .append(decompiled.elapsedMs()).append(" ms）\n");
        sb.append("说明: 代码里可能会经常冒出多余的 (Object) 强转，\n");
        sb.append("      甚至出现「String 变量被赋值成别的类型」这种假象。\n");
        sb.append("      原因见 DexToJava 注释：多半是这份 dex 的调试信息被厂商 strip 了。\n\n");
        String header = sb.toString();

        String savedPath = null;
        String outputPath = request.getOutputPath();
        if (outputPath != null && !outputPath.isEmpty()) {
            File target = new File(outputPath);
            if (target.isDirectory()) {
                target = new File(target, className.replace('.', '_').replace('$', '_') + ".java");
            }
            try {
                // 存文件存纯文本：ANSI 转义写进去，在别的编辑器里就是乱码
                writeOutput(target, (header + decompiled.javaSource()).getBytes(StandardCharsets.UTF_8));
                savedPath = target.getAbsolutePath();
            } catch (IOException e) {
                logger.warn("写 " + target + " 失败: " + e.getMessage());
            }
        }

        String text = header + decompiled.javaSource()
                + (savedPath != null ? "\n\n已写入: " + savedPath : "");

        // 头部信息按普通文本打，源码走语法高亮 —— 两者分开，高亮失败也不会连累前面的信息
        out(context, header, trustworthy ? Colors.DEFAULT : Colors.YELLOW);
        printCode(context, decompiled.javaSource(), "java", request.isHighlight());
        if (savedPath != null) {
            out(context, "\n已写入: " + savedPath, Colors.LIGHT_GREEN);
        }

        BytecodeResult result = buildSuccessResult("source", className, text);
        result.setSource(outcome.source().getLabel());
        result.setDexTrust(outcome.result().trust().name());
        if (savedPath != null) {
            result.setFiles(Collections.singletonList(savedPath));
        }
        // 中间产物里那份 input.dex 有几十 MB，class 文件也没别的用处 —— 用完就清
        deleteQuietly(workDir);
        return result;
    }

    /** 大 dex 的确认框。用户取消（Esc / Ctrl-C）也算"不继续"。 */
    private static boolean confirmBigDex(Console console, String className, long dexBytes) {
        String question = String.format(
                "这份 dex 有 %.1f MB（类 %s）。在手表上反编译可能要几十秒甚至更久，继续？",
                dexBytes / 1024.0 / 1024.0, className);
        try {
            return ConfirmPrompt.ask(question, console, true);
        } catch (CancelledException e) {
            return false;
        }
    }

    /**
     * 在 spinner 下反编译，并随时间升级提示。
     *
     * <p><b>刻意不设硬超时</b>：dex2jar 与 CFR 都不响应中断，强行"超时返回"只会留下一个还在
     * 后台烧 CPU 的线程，还会让人以为"已经停了"。所以这里只做越来越明显的提示
     * （先报秒数，再提示可以中断），让用户自己决定。</p>
     */
    private static DexToJava.Result decompileWithStatus(Console console, String className,
            File dexFile, File workDir) throws IOException {
        if (console == null) {
            return DexToJava.decompile(dexFile, className, workDir);
        }
        try (Status status = console.status("反编译 " + className + "…")) {
            Thread ticker = new Thread(() -> {
                long begin = System.currentTimeMillis();
                try {
                    while (!Thread.currentThread().isInterrupted()) {
                        Thread.sleep(2000);
                        long seconds = (System.currentTimeMillis() - begin) / 1000;
                        if (seconds >= VERY_SLOW_HINT_SECONDS) {
                            status.update("反编译 " + className + "… 已 " + seconds
                                    + " 秒，超出预期（可以 Ctrl-C 中断）");
                        } else if (seconds >= SLOW_HINT_SECONDS) {
                            status.update("反编译 " + className + "… 已 " + seconds
                                    + " 秒（这个类偏大）");
                        }
                    }
                } catch (InterruptedException ignored) {
                    Thread.currentThread().interrupt();
                }
            }, "bytecode-source-hint");
            ticker.setDaemon(true);
            ticker.start();
            try {
                return DexToJava.decompile(dexFile, className, workDir);
            } finally {
                ticker.interrupt();
            }
        }
    }

    /** 删掉中间目录（里面那份 dex 可能有几十 MB）。删不掉只记日志，不影响命令结果。 */
    private static void deleteQuietly(File dir) {
        if (dir == null) {
            return;
        }
        File[] children = dir.listFiles();
        if (children != null) {
            for (File child : children) {
                if (!child.delete()) {
                    logger.warn("中间文件删除失败: " + child.getAbsolutePath());
                }
            }
        }
        if (!dir.delete()) {
            logger.warn("中间目录删除失败: " + dir.getAbsolutePath());
        }
    }
}
