package com.justnothing.testmodule.command.functions.bytecode.util;

import com.justnothing.testmodule.command.functions.bytecode.extract.DexClassIndex;
import com.justnothing.testmodule.utils.logging.Logger;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/**
 * 用设备自带的 {@code /system/bin/dexdump} 反汇编某个类的指令。
 *
 * <p>这是"在设备上看代码"最划算的一条路：dexdump 是 AOSP 自带的（{@code frameworks/base/cmds/dexdump}），
 * <b>不用往 APK 里塞任何东西</b>，输出就是带类型/方法/字符串注释的 dalvik 指令
 * （{@code invoke-virtual {v0, v1}, Ljava/lang/String;.startsWith:...} 这种）。</p>
 *
 * <p>为什么用 dexdump 而不是反编译：dexdump 什么都不用带 —— 它已经在系统里，
 * 输出就是带类型/方法/字符串注释的 dalvik 指令。要看 Java 源码有 {@code bytecode source}
 * （dex2jar + CFR，按类处理）；而 jadx 那种"先把整个 dex 建成 IR"的反编译器
 * 内存与类数量成正比，手表上分不出来。</p>
 */
public final class DexDisassembler {

    private static final Logger logger = Logger.getLoggerForName("DexDisassembler");

    private static final String TOOL = "/system/bin/dexdump";

    /** 一个 dex 的全量 dump 可能几万行，给个上限免得卡死。 */
    private static final long TIMEOUT_MS = 60_000L;

    /**
     * 一次反汇编的产物。
     *
     * @param text      指令文本
     * @param truncated 是否因为触及行数/时间上限被截断
     */
    public record Result(String text, boolean truncated) {
    }

    private DexDisassembler() {
    }

    /** 这台设备有没有可用的 dexdump。 */
    public static boolean available() {
        File tool = new File(TOOL);
        return tool.isFile() && tool.canExecute();
    }

    /**
     * 反汇编 {@code dex} 里 {@code className} 的方法。
     *
     * @param methodName 只看这一个方法；为 {@code null} / 空时输出该类的全部方法
     * @param maxLines   最多返回多少行
     * @return 结果；工具跑不起来、或这个类不在这个 dex 里时返回 {@code null}
     */
    public static Result disassembleClass(File dex, String className, String methodName, int maxLines) {
        String descriptor = DexClassIndex.descriptorOf(className);
        if (descriptor == null || dex == null || !dex.isFile() || maxLines <= 0) {
            return null;
        }
        // dexdump 给每个方法都打一行 "#N : (in Lxxx;)"，用它把目标类的段落切出来
        String classMarker = "(in " + descriptor + ")";
        boolean filterMethod = methodName != null && !methodName.isEmpty();

        Process process = null;
        try {
            process = new ProcessBuilder(TOOL, "-d", dex.getAbsolutePath())
                    .redirectErrorStream(true)
                    .start();

            StringBuilder text = new StringBuilder();
            boolean inClass = false;
            boolean collecting = false;
            boolean truncated = false;
            int lines = 0;
            long deadline = System.currentTimeMillis() + TIMEOUT_MS;

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (System.currentTimeMillis() > deadline) {
                        logger.warn("dexdump 超时，返回已读到的部分");
                        truncated = true;
                        break;
                    }

                    if (line.contains("(in L")) {
                        // 换到下一个方法的元数据段了
                        inClass = line.contains(classMarker);
                        collecting = false;
                    } else if (inClass && line.contains("name") && line.contains(": '")) {
                        // 形如 "      name          : 'doBackup'"
                        String name = quotedValue(line);
                        collecting = !filterMethod || methodName.equals(name);
                    }

                    if (!inClass || !collecting) {
                        continue;
                    }
                    if (lines >= maxLines) {
                        truncated = true;
                        break;
                    }
                    text.append(line).append('\n');
                    lines++;
                }
            }

            if (text.length() == 0) {
                return null;
            }
            return new Result(text.toString(), truncated);
        } catch (IOException e) {
            logger.warn("调用 dexdump 失败: " + e.getMessage());
            return null;
        } finally {
            if (process != null) {
                process.destroy();
            }
        }
    }

    /** 从 {@code "      name          : 'doBackup'"} 里取出 {@code doBackup}。 */
    private static String quotedValue(String line) {
        int start = line.indexOf('\'');
        if (start < 0) {
            return null;
        }
        int end = line.indexOf('\'', start + 1);
        return end < 0 ? null : line.substring(start + 1, end);
    }
}
