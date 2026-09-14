package com.justnothing.testmodule.command.functions.bytecode.util;

import com.googlecode.d2j.dex.Dex2Asm;
import com.googlecode.d2j.node.DexFileNode;
import com.googlecode.d2j.reader.DexFileReader;
import com.googlecode.d2j.visitors.DexClassVisitor;
import com.googlecode.d2j.visitors.DexFileVisitor;
import com.justnothing.testmodule.command.functions.bytecode.extract.DexClassIndex;
import com.justnothing.testmodule.utils.logging.Logger;

import org.benf.cfr.reader.api.CfrDriver;
import org.benf.cfr.reader.api.OutputSinkFactory;
import org.objectweb.asm.ClassWriter;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * dex → JVM class → Java 源码（设备上反编译）。
 *
 * <p><b>为什么不用 jadx</b>：jadx 会把整个 dex 里所有类都建成 IR 再去优化，内存与"类数量"
 * 成正比 —— 官方 FAQ 里遇到 OOM 的建议是把 {@code -Xmx} 调到 8G。手表上只可能分出几十 MB，
 * 这条路走不通，{@code --single-class} 也救不了（它同样要先建全局索引）。</p>
 *
 * <p>这里换成"按类流式"的三步：</p>
 * <ol>
 *   <li>{@link DexFileReader} 只解析目标类 —— 它的 {@code visit()} 返回 {@code null} 时
 *       <b>整个类会被跳过</b>，不建节点、不解析成员与代码。所以峰值内存只与这一个类有关，
 *       与 dex 总大小无关（唯一与 dex 大小成正比的开销是 {@code DexFileReader} 会把文件
 *       整份读进内存，几十 MB 的 framework dex 也就是那几十 MB）。</li>
 *   <li>{@link Dex2Asm} 把该类转成 JVM 字节码（纯 ASM，项目本来就依赖 ASM）。</li>
 *   <li>CFR 反编译单个 class，并开启它自带的 {@code lomem} 低内存模式。</li>
 * </ol>
 *
 * <p><b>代价是质量</b>：dex2jar 把 dalvik 转成 JVM 字节码时，{@code synchronized}、
 * try-with-resources、不可约控制流会退化成 goto 和辅助变量，可读性明显不如 jadx
 * （jadx 是专门为 dalvik 写的）。所以这条链路定位是"在手表上看个大概"，
 * 精确还原还是交给电脑。</p>
 *
 * <p><b>另一个必然的代价：变量名和泛型全丢。</b>这里有<b>两层</b>原因，而且第一层才是主因：</p>
 * <ol>
 *   <li><b>源头往往就没有</b>：厂商和混淆过的 dex 通常被 strip 掉调试信息。实测
 *       {@code services.dex}（厂商系统类）里 {@code line=} 和 {@code locals} 出现次数都是
 *       <b>0</b>，而同环境下 AOSP 的 {@code bu.dex} 有 125 处 {@code line=}。
 *       这种 dex 里参数名/局部变量名<b>根本不存在</b>，任何工具都恢复不出来 ——
 *       jadx 在同一份 dex 上同样只能给出 {@code Context context} / {@code String str}
 *       这种"按类型命名"，和 CFR 没区别。</li>
 *   <li><b>就算有也不搬</b>：d2j 的 {@code Dex2Asm} 走 {@code dex → IR → JVM 字节码}，
 *       源码里<b>完全没有</b>写 {@code LocalVariableTable} / {@code LineNumberTable} 的逻辑。
 *       （{@code DexFileReader} 是解析了调试信息的，{@code d2j-dex2jar} 的
 *       {@code -d/--debug-info} 开关也只是控制"要不要解析" —— 解析完依然没人用。）</li>
 * </ol>
 *
 * <p>由此带来三种看起来像 bug 的输出：</p>
 * <ul>
 *   <li>参数只能按类型命名：{@code String string} / {@code boolean bl} / {@code long l}</li>
 *   <li>泛型签名没了 → CFR 保守地补出大量 {@code (Object)} 强转</li>
 *   <li>局部变量槽被复用 → CFR 按槽位合并变量，把两个不相干的变量画成同一个
 *       （表现为"String 变量被赋值成别的类型"这种不可能编译通过的假象）</li>
 * </ul>
 *
 * <p><b>很难修，而且不该在这里修。</b>IR 阶段会重排指令，dex 的局部变量作用域
 * （以 dex 的 PC 为单位）映射不到新生成的字节码上；JVM 生态里也没有更好的 dex→class
 * 转换器（Google 的 enjarify 是 Python 写的，进不了 Android）。所以这是"设备端反编译"的
 * 固有限制，不是哪一个环节写错了。想读干净的代码就导出 dex 到电脑上用 jadx ——
 * 它直接读 dex，不经过 class 这一层。</p>
 */
public final class DexToJava {

    private static final Logger logger = Logger.getLoggerForName("DexToJava");

    private DexToJava() {
    }

    /**
     * 一次反编译的结果。
     *
     * @param javaSource    Java 源码
     * @param classSize     中间产物（JVM class 文件）的字节数
     * @param elapsedMs     总耗时
     * @param peakHeapBytes 全过程观测到的峰值已用堆，用于判断手表吃不吃得下
     */
    public record Result(String javaSource, int classSize, long elapsedMs, long peakHeapBytes) {
    }

    /**
     * 把 {@code dexFile} 里的 {@code className} 反编译成 Java 源码。
     *
     * @param workDir 中间产物目录（class 文件写这里；调用方需保证可写）
     * @return 结果；类不在这个 dex 里、或没能产出源码时返回 {@code null}
     */
    public static Result decompile(File dexFile, String className, File workDir) throws IOException {
        String descriptor = DexClassIndex.descriptorOf(className);
        if (descriptor == null) {
            return null;
        }
        String internalName = descriptor.substring(1, descriptor.length() - 1);

        long start = System.currentTimeMillis();
        Runtime runtime = Runtime.getRuntime();
        long peak = usedHeap(runtime);

        // 1) 只解析目标类
        DexFileReader reader = new DexFileReader(dexFile);
        DexFileNode target = new DexFileNode();
        reader.accept(new SingleClassVisitor(descriptor, target), 0);
        if (target.clzs == null || target.clzs.isEmpty()) {
            logger.warn("这个 dex 里没有类 " + className);
            return null;
        }
        peak = Math.max(peak, usedHeap(runtime));

        // 2) dex → JVM 字节码（只转这一个类）
        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        new Dex2Asm().convertDex(target, name -> internalName.equals(name) ? writer : null);
        byte[] classBytes = writer.toByteArray();
        peak = Math.max(peak, usedHeap(runtime));

        // class 落盘再交给 CFR：CFR 自己按文件读，比让它在内存里拿字节更接近它的常规用法
        File classFile = new File(workDir, internalName.replace('/', '_') + ".class");
        writeBytes(classFile, classBytes);

        // 3) class → Java
        String source = cfrDecompile(classFile, className);
        peak = Math.max(peak, usedHeap(runtime));

        if (source == null || source.isBlank()) {
            return null;
        }
        return new Result(source, classBytes.length, System.currentTimeMillis() - start, peak);
    }

    /**
     * 只放行目标类：{@code visit()} 返回 {@code null} 时 {@link DexFileReader} 会跳过该类的
     * 全部解析（不建节点、不读成员与代码），这是"内存与 dex 大小解耦"的关键。
     */
    private static final class SingleClassVisitor extends DexFileVisitor {

        private final String descriptor;
        private final DexFileNode target;

        SingleClassVisitor(String descriptor, DexFileNode target) {
            this.descriptor = descriptor;
            this.target = target;
        }

        @Override
        public void visitDexFileVersion(int version) {
            // Dex2Asm 靠它决定生成哪个版本的 class 文件，不能漏
            target.visitDexFileVersion(version);
        }

        @Override
        public DexClassVisitor visit(int accessFlags, String name, String superName, String[] interfaces) {
            return descriptor.equals(name)
                    ? target.visit(accessFlags, name, superName, interfaces)
                    : null;
        }
    }

    /**
     * CFR 反编译单个 class。
     *
     * <p>输出直接收集到内存（不用 {@code outputdir}），省掉一次写盘 + 回读。
     * {@code lomem} 是 CFR 自带的低内存模式，正是为这类受限环境准备的。</p>
     */
    private static String cfrDecompile(File classFile, String className) {
        StringBuilder out = new StringBuilder();

        Map<String, String> options = new HashMap<>();
        options.put("lomem", "true");
        options.put("silent", "true");
        options.put("comments", "false");

        OutputSinkFactory sink = new OutputSinkFactory() {
            @Override
            public List<SinkClass> getSupportedSinks(SinkType sinkType, Collection<SinkClass> available) {
                return Collections.singletonList(SinkClass.STRING);
            }

            @Override
            public <T> Sink<T> getSink(SinkType sinkType, SinkClass sinkClass) {
                if (sinkType == SinkType.JAVA || sinkType == SinkType.EXCEPTION) {
                    return value -> {
                        if (value instanceof String text) {
                            out.append(text);
                        }
                    };
                }
                return value -> { };
            }
        };

        try {
            new CfrDriver.Builder()
                    .withOptions(options)
                    .withOutputSink(sink)
                    .build()
                    .analyse(Collections.singletonList(classFile.getAbsolutePath()));
        } catch (Throwable t) {
            logger.warn("CFR 反编译 " + className + " 失败: " + t);
            return null;
        }
        return out.toString();
    }

    private static long usedHeap(Runtime runtime) {
        return runtime.totalMemory() - runtime.freeMemory();
    }

    private static void writeBytes(File target, byte[] data) throws IOException {
        File parent = target.getParentFile();
        if (parent != null && !parent.isDirectory() && !parent.mkdirs() && !parent.isDirectory()) {
            throw new IOException("无法创建目录: " + parent.getAbsolutePath());
        }
        try (FileOutputStream out = new FileOutputStream(target)) {
            out.write(data);
        }
    }
}
