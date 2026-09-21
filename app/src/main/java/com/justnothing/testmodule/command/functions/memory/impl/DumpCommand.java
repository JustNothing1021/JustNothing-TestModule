package com.justnothing.testmodule.command.functions.memory.impl;

import com.justnothing.testmodule.command.framework.annotation.SubCommandInfo;
import com.justnothing.testmodule.command.framework.i18n.CliMessages;
import com.justnothing.testmodule.command.framework.i18n.Text;
import com.justnothing.testmodule.command.functions.memory.MemoryTexts;
import com.justnothing.testmodule.command.functions.memory.request.DumpRequest;
import com.justnothing.testmodule.command.functions.memory.response.DumpResult;
import com.justnothing.testmodule.command.functions.memory.util.MemoryUtils;
import com.justnothing.testmodule.command.framework.output.Colors;
import com.justnothing.testmodule.utils.io.IOManager;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.Map;

/**
 * 导出内存转储。
 *
 * <p><b>本文件的中文曾经整体损坏</b>：原始提交里所有汉字都变成了字面量 {@code '?'}
 * （整个文件 0 个非 ASCII 字节），而仓库只有那一个 commit、编译产物里的字符串常量也同样没有中文，
 * 所以没有可回滚的原稿。下面的文案是按同族 {@link AbstractMemoryCommand} 的彩色输出版逐条镜像回来的
 * —— 那一份是完好的，`=== 堆内存信息 ===`、`Java运行时内存:`、`最大:`/`已分配:`/`空闲:`/`已用:`、
 * `=== 线程信息 ===`、`=== 系统信息 ===`、`操作系统:`/`系统版本:`/`架构:` 等都是<b>逐字相同</b>的；
 * 少数几处没有镜像可依（顶层标题、导出文件路径那几条、以及 console 里「已保存」的措辞）是按语义补写的。</p>
 *
 * <p>如果哪天找到了原稿，改这里的字面量即可，不必动结构。</p>
 */
@SubCommandInfo(
    description = MemoryTexts.SUB_MEMORY_DUMP_DESC,
    usage = "memory dump [options] [file]",
    examples = {
        "memory dump",
        "memory dump /sdcard/heap_dump.txt",
        "memory dump --heap /sdcard/heap_only.txt",
        "memory dump --full /sdcard/full_dump.txt"
    },
    optionsDesc = MemoryTexts.SUB_MEMORY_DUMP_OPTIONS
)
public class DumpCommand extends AbstractMemoryCommand<DumpRequest, DumpResult> {

    public DumpCommand() {
        super("memory dump", DumpRequest.class, DumpResult.class);
    }

    @Override
    protected DumpResult executeMemoryCommand(DumpRequest request) throws Exception {
        DumpResult result = new DumpResult(request.getRequestId());
        result.setTimestamp(System.currentTimeMillis());

        StringBuilder output = new StringBuilder();

        output.append(Text.zhEn("=== 内存转储 ===\n", "=== Memory dump ===\n").text());
        output.append(MemoryTexts.LABEL_TIME.text()).append(new Date()).append("\n");

        if (request.getFilePath() != null) {
            output.append(Text.zhEn("文件: ", "File: ").text()).append(request.getFilePath()).append("\n\n");
        } else {
            output.append("\n");
        }

        if (request.isHeapOnly() || request.isFullDump()) {
            appendHeapInfo(output);
        }

        if (request.isThreadsOnly() || request.isFullDump()) {
            appendThreadInfo(output);
        }

        if (request.isFullDump()) {
            appendSystemInfo(output);
        }

        result.setDumpContent(output.toString());

        if (request.getFilePath() != null) {
            File outputFile = new File(request.getFilePath());
            File parentDir = outputFile.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                IOManager.createDirectory(parentDir.getAbsolutePath());
            }

            try {
                logger.info("开始写入转储文件: " + request.getFilePath());
                IOManager.writeFile(outputFile.getAbsolutePath(), output.toString());
                logger.info("内存转储已完成");

                context.print(Text.zhEn("内存转储已保存: ", "Memory dump saved: ").text(), Colors.LIGHT_GREEN);
                context.println(request.getFilePath(), Colors.CYAN);

                result.setFilePath(request.getFilePath());
            } catch (IOException e) {
                logger.error("写入转储文件失败", e);
                context.print(CliMessages.ERROR_PREFIX.text(), Colors.RED);
                context.println(e.getMessage() != null ? e.getMessage() : MemoryTexts.UNKNOWN_ERROR.text(), Colors.YELLOW);
                result.setSuccess(false);
                return result;
            }
        } else {
            context.println(Text.zhEn("=== 内存转储 ===", "=== Memory dump ===").text(), Colors.CYAN);
            context.print(MemoryTexts.LABEL_TIME.text(), Colors.GRAY);
            context.println(new Date().toString(), Colors.YELLOW);
            context.println("");

            if (request.isHeapOnly() || request.isFullDump()) {
                dumpHeapInfoColored();
            }

            if (request.isThreadsOnly() || request.isFullDump()) {
                dumpThreadInfoColored();
            }

            if (request.isFullDump()) {
                dumpSystemInfoColored();
            }
        }

        result.setSuccess(true);
        return result;
    }

    private void appendHeapInfo(StringBuilder output) {
        output.append(Text.zhEn("=== 堆内存信息 ===\n\n", "=== Heap memory information ===\n\n").text());

        Runtime runtime = Runtime.getRuntime();
        output.append(Text.zhEn("Java运行时内存:\n", "Java runtime memory:\n").text());
        output.append(MemoryTexts.LABEL_INDENTED_MAX.text()).append(MemoryUtils.formatBytes(runtime.maxMemory())).append("\n");
        output.append(MemoryTexts.LABEL_INDENTED_ALLOCATED.text()).append(MemoryUtils.formatBytes(runtime.totalMemory())).append("\n");
        output.append(MemoryTexts.LABEL_INDENTED_FREE.text()).append(MemoryUtils.formatBytes(runtime.freeMemory())).append("\n");
        output.append(MemoryTexts.LABEL_INDENTED_USED.text()).append(MemoryUtils.formatBytes(runtime.totalMemory() - runtime.freeMemory())).append("\n\n");

        output.append(Text.zhEn("原生堆内存:\n", "Native heap memory:\n").text());
        output.append(MemoryTexts.LABEL_INDENTED_ALLOCATED.text()).append(MemoryUtils.formatBytes(android.os.Debug.getNativeHeapAllocatedSize())).append("\n");
        output.append(MemoryTexts.LABEL_INDENTED_USED.text()).append(MemoryUtils.formatBytes(android.os.Debug.getNativeHeapSize())).append("\n");
        output.append(MemoryTexts.LABEL_INDENTED_FREE.text()).append(MemoryUtils.formatBytes(android.os.Debug.getNativeHeapFreeSize())).append("\n\n");

        output.append(Text.zhEn("=== 内存详细信息 ===\n\n", "=== Detailed memory information ===\n\n").text());
        output.append(readMeminfo());
    }

    private void appendThreadInfo(StringBuilder output) {
        output.append(Text.zhEn("=== 线程信息 ===\n\n", "=== Thread information ===\n\n").text());

        Map<Thread, StackTraceElement[]> allStackTraces = Thread.getAllStackTraces();
        output.append(MemoryTexts.LABEL_THREAD_COUNT.text()).append(allStackTraces.size()).append("\n\n");

        for (Map.Entry<Thread, StackTraceElement[]> entry : allStackTraces.entrySet()) {
            Thread thread = entry.getKey();

            output.append(MemoryTexts.LABEL_THREAD.text()).append(thread.getName()).append("\n");
            output.append("  ID: ").append(thread.getId()).append("\n");
            output.append(MemoryTexts.LABEL_THREAD_STATE.text()).append(thread.getState()).append("\n\n");
        }
    }

    private void appendSystemInfo(StringBuilder output) {
        output.append(Text.zhEn("=== 系统信息 ===\n\n", "=== System information ===\n\n").text());

        output.append(MemoryTexts.LABEL_OS.text()).append(System.getProperty("os.name")).append("\n");
        output.append(MemoryTexts.LABEL_OS_VERSION.text()).append(System.getProperty("os.version")).append("\n");
        output.append(MemoryTexts.LABEL_ARCH.text()).append(System.getProperty("os.arch")).append("\n\n");
    }
}
