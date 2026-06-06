package com.justnothing.testmodule.command.functions.nativecmd.impl;

import com.justnothing.testmodule.command.base.protocol.CommandRequest;
import com.justnothing.testmodule.command.base.protocol.CommandResult;
import com.justnothing.testmodule.command.functions.nativecmd.NativeManager;
import com.justnothing.testmodule.command.functions.nativecmd.NativeResult;
import com.justnothing.testmodule.command.functions.nativecmd.request.*;
import com.justnothing.testmodule.command.output.Colors;

import java.io.File;
import java.util.List;
import java.util.Map;

public class NativeQueryCommand extends AbstractNativeCommand<CommandRequest, CommandResult> {

    public NativeResult handleList(NativeListRequest request) {
        String pattern = request.getPattern();
        boolean verbose = request.getVerbose() != null && request.getVerbose();

        try {
            List<String> libraries = manager.getLoadedLibraries();

            out("已加载的Native库: ", Colors.CYAN);
            outln(libraries.size() + " 个", Colors.YELLOW);
            outln("", Colors.WHITE);

            for (String lib : libraries) {
                if (pattern == null || lib.contains(pattern)) {
                    out("  - ", Colors.GRAY);
                    outln(lib, Colors.GREEN);
                    if (verbose) {
                        String libPath = manager.getLibraryPath(lib);
                        if (libPath != null) {
                            out("    路径: ", Colors.CYAN);
                            outln(libPath, Colors.GRAY);
                        }
                    }
                }
            }

        } catch (Exception e) {
            com.justnothing.testmodule.command.utils.CommandExceptionHandler.handleException(
                "native list", e, context, "获取native库列表失败");
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

            out("库信息: ", Colors.CYAN);
            outln(libName, Colors.GREEN);
            out("路径: ", Colors.CYAN);
            outln(libPath != null ? libPath : "未知", Colors.GRAY);

            if (libPath != null) {
                File libFile = new File(libPath);
                if (libFile.exists()) {
                    out("大小: ", Colors.CYAN);
                    outln(libFile.length() + " 字节", Colors.YELLOW);
                    out("可读: ", Colors.CYAN);
                    outln(String.valueOf(libFile.canRead()), libFile.canRead() ? Colors.GREEN : Colors.RED);
                    out("可执行: ", Colors.CYAN);
                    outln(String.valueOf(libFile.canExecute()), libFile.canExecute() ? Colors.GREEN : Colors.RED);
                }
            }

            if (verbose) {
                outln("", Colors.WHITE);
                outln("导出符号:", Colors.CYAN);
                List<String> symbols = manager.getLibrarySymbols(libName);
                for (String symbol : symbols) {
                    out("  - ", Colors.GRAY);
                    outln(symbol, Colors.GRAY);
                }
            }

        } catch (Exception e) {
            com.justnothing.testmodule.command.utils.CommandExceptionHandler.handleException(
                "native info", e, context, "获取库信息失败");
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

            out("符号表: ", Colors.CYAN);
            outln(libName, Colors.GREEN);
            out("数量: ", Colors.CYAN);
            outln(symbols.size() + " 个", Colors.YELLOW);
            outln("", Colors.WHITE);

            for (String symbol : symbols) {
                out("  - ", Colors.GRAY);
                outln(symbol, Colors.GRAY);
            }

        } catch (Exception e) {
            com.justnothing.testmodule.command.utils.CommandExceptionHandler.handleException(
                "native symbols", e, context, "获取符号表失败");
        }

        NativeResult r = new NativeResult(java.util.UUID.randomUUID().toString());
        r.setSubCommand("symbols");
        r.setSuccess(true);
        return r;
    }

    public NativeResult handleMemory(NativeMemoryRequest request) {
        try {
            Map<String, String> memoryInfo = manager.getNativeMemoryInfo();

            outln("Native内存使用情况:", Colors.CYAN);
            outln("", Colors.WHITE);

            for (Map.Entry<String, String> entry : memoryInfo.entrySet()) {
                out("  " + entry.getKey() + ": ", Colors.CYAN);
                outln(entry.getValue(), Colors.YELLOW);
            }

        } catch (Exception e) {
            com.justnothing.testmodule.command.utils.CommandExceptionHandler.handleException(
                "native memory", e, context, "获取native内存信息失败");
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

            outln("Native堆内存:", Colors.CYAN);
            outln("", Colors.WHITE);

            for (Map.Entry<String, String> entry : heapInfo.entrySet()) {
                out("  " + entry.getKey() + ": ", Colors.CYAN);
                outln(entry.getValue(), Colors.YELLOW);
            }

            if (verbose) {
                outln("", Colors.WHITE);
                outln("详细信息:", Colors.CYAN);
                outln("  注意: 完整的native堆分析需要:", Colors.GRAY);
                outln("    - malloc_hook 或类似工具", Colors.GRAY);
                outln("    - jemalloc 或 tcmalloc 支持", Colors.GRAY);
                outln("    - ASAN 或 Valgrind 等工具", Colors.GRAY);
            }

        } catch (Exception e) {
            com.justnothing.testmodule.command.utils.CommandExceptionHandler.handleException(
                "native heap", e, context, "获取native堆信息失败");
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

            out("内存映射: ", Colors.CYAN);
            outln(maps.size() + " 个区域", Colors.YELLOW);
            outln("", Colors.WHITE);

            for (String map : maps) {
                outln("  " + map, Colors.GRAY);
            }

            if (verbose) {
                outln("", Colors.WHITE);
                outln("说明:", Colors.CYAN);
                outln("  r - 读权限", Colors.GRAY);
                outln("  w - 写权限", Colors.GRAY);
                outln("  x - 执行权限", Colors.GRAY);
                outln("  p - 私有映射", Colors.GRAY);
                outln("  s - 共享映射", Colors.GRAY);
            }

        } catch (Exception e) {
            com.justnothing.testmodule.command.utils.CommandExceptionHandler.handleException(
                "native maps", e, context, "获取内存映射失败");
        }

        NativeResult r = new NativeResult(java.util.UUID.randomUUID().toString());
        r.setSubCommand("maps");
        r.setSuccess(true);
        return r;
    }

    @Override
    protected CommandResult executeInternal(CommandRequest request) throws Exception {
        if (request instanceof NativeListRequest r) return handleList(r);
        if (request instanceof NativeInfoRequest r) return handleInfo(r);
        if (request instanceof NativeSymbolsRequest r) return handleSymbols(r);
        if (request instanceof NativeMemoryRequest r) return handleMemory(r);
        if (request instanceof NativeHeapRequest r) return handleHeap(r);
        if (request instanceof NativeMapsRequest r) return handleMaps(r);

        throw new IllegalArgumentException("不支持的请求类型: " + request.getClass().getSimpleName());
    }
}
