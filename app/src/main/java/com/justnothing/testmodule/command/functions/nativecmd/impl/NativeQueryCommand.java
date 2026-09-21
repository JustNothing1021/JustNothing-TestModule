package com.justnothing.testmodule.command.functions.nativecmd.impl;

import com.justnothing.testmodule.command.framework.model.CommandRequest;
import com.justnothing.testmodule.command.framework.model.CommandResult;
import com.justnothing.testmodule.command.framework.utils.CommandExceptionHandler;
import com.justnothing.testmodule.command.functions.nativecmd.response.NativeResult;
import com.justnothing.testmodule.command.functions.nativecmd.request.*;
import com.justnothing.testmodule.command.framework.output.Colors;
import com.justnothing.testmodule.command.framework.i18n.Text;
import com.justnothing.testmodule.command.functions.nativecmd.NativeTexts;

import java.io.File;
import java.util.List;
import java.util.Map;

public class NativeQueryCommand extends AbstractNativeCommand<CommandRequest<?>, CommandResult> {

    @SuppressWarnings("unchecked")
    public NativeQueryCommand() {
        super("native query", (Class) CommandRequest.class, CommandResult.class);
    }

    public NativeResult handleList(NativeListRequest request) {
        String pattern = request.getPattern();
        boolean verbose = request.getVerbose() != null && request.getVerbose();

        try {
            List<String> libraries = manager.getLoadedLibraries();

            out(Text.zhEn("已加载的Native库: ", "Loaded native libraries: ").text(), Colors.CYAN);
            outln(NativeTexts.COUNT_UNIT.format(libraries.size()), Colors.YELLOW);
            outln("", Colors.WHITE);

            for (String lib : libraries) {
                if (pattern == null || lib.contains(pattern)) {
                    out("  - ", Colors.GRAY);
                    outln(lib, Colors.GREEN);
                    if (verbose) {
                        String libPath = manager.getLibraryPath(lib);
                        if (libPath != null) {
                            out(Text.zhEn("    路径: ", "    Path: ").text(), Colors.CYAN);
                            outln(libPath, Colors.GRAY);
                        }
                    }
                }
            }

        } catch (Exception e) {
            CommandExceptionHandler.handleException(
                "native list", e, context, Text.zhEn("获取native库列表失败", "Failed to list native libraries").text());
        }

        NativeResult r = new NativeResult(java.util.UUID.randomUUID().toString());
        r.setSubCommand("list");
        r.setSuccess(true);
        return r;
    }

    public NativeResult handleInfo(NativeInfoRequest request) {
        String libName = request.getLibName();
        boolean verbose = request.getVerbose() != null && request.getVerbose();

        try {
            String libPath = manager.getLibraryPath(libName);

            out(Text.zhEn("库信息: ", "Library info: ").text(), Colors.CYAN);
            outln(libName, Colors.GREEN);
            out(NativeTexts.LABEL_PATH.text(), Colors.CYAN);
            outln(libPath != null ? libPath : NativeTexts.VALUE_UNKNOWN.text(), Colors.GRAY);

            if (libPath != null) {
                File libFile = new File(libPath);
                if (libFile.exists()) {
                    out(Text.zhEn("大小: ", "Size: ").text(), Colors.CYAN);
                    outln(libFile.length() + NativeTexts.UNIT_BYTES.text(), Colors.YELLOW);
                    out(Text.zhEn("可读: ", "Readable: ").text(), Colors.CYAN);
                    outln(String.valueOf(libFile.canRead()), libFile.canRead() ? Colors.GREEN : Colors.RED);
                    out(Text.zhEn("可执行: ", "Executable: ").text(), Colors.CYAN);
                    outln(String.valueOf(libFile.canExecute()), libFile.canExecute() ? Colors.GREEN : Colors.RED);
                }
            }

            if (verbose) {
                outln("", Colors.WHITE);
                outln(Text.zhEn("导出符号:", "Exported symbols:").text(), Colors.CYAN);
                List<String> symbols = manager.getLibrarySymbols(libName);
                for (String symbol : symbols) {
                    out("  - ", Colors.GRAY);
                    outln(symbol, Colors.GRAY);
                }
            }

        } catch (Exception e) {
            CommandExceptionHandler.handleException(
                "native info", e, context, Text.zhEn("获取库信息失败", "Failed to get library info").text());
        }

        NativeResult r = new NativeResult(java.util.UUID.randomUUID().toString());
        r.setSubCommand("info");
        r.setSuccess(true);
        return r;
    }

    public NativeResult handleSymbols(NativeSymbolsRequest request) {
        String libName = request.getLibName();

        try {
            List<String> symbols = manager.getLibrarySymbols(libName);

            out(Text.zhEn("符号表: ", "Symbol table: ").text(), Colors.CYAN);
            outln(libName, Colors.GREEN);
            out(NativeTexts.LABEL_COUNT.text(), Colors.CYAN);
            outln(NativeTexts.COUNT_UNIT.format(symbols.size()), Colors.YELLOW);
            outln("", Colors.WHITE);

            for (String symbol : symbols) {
                out("  - ", Colors.GRAY);
                outln(symbol, Colors.GRAY);
            }

        } catch (Exception e) {
            CommandExceptionHandler.handleException(
                "native symbols", e, context, Text.zhEn("获取符号表失败", "Failed to get the symbol table").text());
        }

        NativeResult r = new NativeResult(java.util.UUID.randomUUID().toString());
        r.setSubCommand("symbols");
        r.setSuccess(true);
        return r;
    }

    public NativeResult handleMemory(NativeMemoryRequest request) {
        try {
            Map<String, String> memoryInfo = manager.getNativeMemoryInfo();

            outln(Text.zhEn("Native内存使用情况:", "Native memory usage:").text(), Colors.CYAN);
            outln("", Colors.WHITE);

            for (Map.Entry<String, String> entry : memoryInfo.entrySet()) {
                out("  " + entry.getKey() + ": ", Colors.CYAN);
                outln(entry.getValue(), Colors.YELLOW);
            }

        } catch (Exception e) {
            CommandExceptionHandler.handleException(
                "native memory", e, context, Text.zhEn("获取native内存信息失败", "Failed to get native memory info").text());
        }

        NativeResult r = new NativeResult(java.util.UUID.randomUUID().toString());
        r.setSubCommand("memory");
        r.setSuccess(true);
        return r;
    }

    public NativeResult handleHeap(NativeHeapRequest request) {
        boolean verbose = request.getVerbose() != null && request.getVerbose();

        try {
            Map<String, String> heapInfo = manager.getNativeHeapInfo();

            outln(Text.zhEn("Native堆内存:", "Native heap memory:").text(), Colors.CYAN);
            outln("", Colors.WHITE);

            for (Map.Entry<String, String> entry : heapInfo.entrySet()) {
                out("  " + entry.getKey() + ": ", Colors.CYAN);
                outln(entry.getValue(), Colors.YELLOW);
            }

            if (verbose) {
                outln("", Colors.WHITE);
                outln(Text.zhEn("详细信息:", "Details:").text(), Colors.CYAN);
                outln(Text.zhEn("  注意: 完整的native堆分析需要:", "  Note: a complete native heap analysis requires:").text(), Colors.GRAY);
                outln(Text.zhEn("    - malloc_hook 或类似工具", "    - malloc_hook or a similar tool").text(), Colors.GRAY);
                outln(Text.zhEn("    - jemalloc 或 tcmalloc 支持", "    - jemalloc or tcmalloc support").text(), Colors.GRAY);
                outln(Text.zhEn("    - ASAN 或 Valgrind 等工具", "    - ASAN, Valgrind and similar tools").text(), Colors.GRAY);
            }

        } catch (Exception e) {
            CommandExceptionHandler.handleException(
                "native heap", e, context, Text.zhEn("获取native堆信息失败", "Failed to get native heap info").text());
        }

        NativeResult r = new NativeResult(java.util.UUID.randomUUID().toString());
        r.setSubCommand("heap");
        r.setSuccess(true);
        return r;
    }

    public NativeResult handleMaps(NativeMapsRequest request) {
        boolean verbose = request.getVerbose() != null && request.getVerbose();

        try {
            List<String> maps = manager.getMemoryMaps();

            out(Text.zhEn("内存映射: ", "Memory map: ").text(), Colors.CYAN);
            outln(Text.zhEn("%s 个区域", "%s regions").format(maps.size()), Colors.YELLOW);
            outln("", Colors.WHITE);

            for (String map : maps) {
                outln("  " + map, Colors.GRAY);
            }

            if (verbose) {
                outln("", Colors.WHITE);
                outln(Text.zhEn("说明:", "Legend:").text(), Colors.CYAN);
                outln(Text.zhEn("  r - 读权限", "  r - read permission").text(), Colors.GRAY);
                outln(Text.zhEn("  w - 写权限", "  w - write permission").text(), Colors.GRAY);
                outln(Text.zhEn("  x - 执行权限", "  x - execute permission").text(), Colors.GRAY);
                outln(Text.zhEn("  p - 私有映射", "  p - private mapping").text(), Colors.GRAY);
                outln(Text.zhEn("  s - 共享映射", "  s - shared mapping").text(), Colors.GRAY);
            }

        } catch (Exception e) {
            CommandExceptionHandler.handleException(
                "native maps", e, context, Text.zhEn("获取内存映射失败", "Failed to get the memory map").text());
        }

        NativeResult r = new NativeResult(java.util.UUID.randomUUID().toString());
        r.setSubCommand("maps");
        r.setSuccess(true);
        return r;
    }

    @Override
    protected CommandResult executeRequest(CommandRequest<?> request) throws Exception {
        if (request instanceof NativeListRequest r) return handleList(r);
        if (request instanceof NativeInfoRequest r) return handleInfo(r);
        if (request instanceof NativeSymbolsRequest r) return handleSymbols(r);
        if (request instanceof NativeMemoryRequest r) return handleMemory(r);
        if (request instanceof NativeHeapRequest r) return handleHeap(r);
        if (request instanceof NativeMapsRequest r) return handleMaps(r);

        throw new IllegalArgumentException(
                NativeTexts.ERR_UNSUPPORTED_REQUEST_TYPE.format(request.getClass().getSimpleName()));
    }
}
