package com.justnothing.testmodule.command.functions.nativecmd;

import static com.justnothing.testmodule.constants.CommandServer.CMD_NATIVE_VER;

import com.justnothing.testmodule.command.CommandExecutor;
import com.justnothing.testmodule.command.functions.CommandBase;
import com.justnothing.testmodule.command.output.Colors;
import com.justnothing.testmodule.command.utils.CommandExceptionHandler;
import com.justnothing.testmodule.utils.reflect.ClassResolver;
import com.justnothing.testmodule.utils.io.IOManager;
import com.justnothing.testmodule.utils.io.RootProcessPool;

import org.objectweb.asm.Type;

import java.io.File;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NativeMain extends CommandBase {

    public NativeMain() {
        super("Native");
    }

    @Override
    public String getHelpText() {
        return String.format("""
                语法: native <subcmd> [args...]
                
                查看和调试Native代码，分析JNI函数和库.
                
                子命令:
                    list [pattern]                          - 列出已加载的native库
                    info <lib_name>                         - 查看native库的详细信息
                    functions <class_name>                  - 列出类的native方法
                    symbols <lib_name>                      - 查看库的符号表
                    memory                                  - 查看native内存使用情况
                    heap                                    - 查看native堆内存
                    stack <tid>                             - 查看线程的native栈
                    maps                                    - 查看进程内存映射
                    search <pattern>                        - 搜索native库或函数
                
                选项:
                    -v, --verbose                           - 详细输出
                    -t, --thread <tid>                      - 指定线程ID
                
                示例:
                    native list
                    native list libart.so
                    native info libc.so
                    native functions java.lang.System
                    native symbols libc.so
                    native memory
                    native heap
                    native stack 1234
                    native maps
                    native search malloc
                
                注意:
                    - 内存映射信息来自/proc/self/maps
                
                (Submodule native %s)
                """, CMD_NATIVE_VER);
    }

    @Override
    public void runMain(CommandExecutor.CmdExecContext context) {
        String[] args = context.args();
        
        if (args.length < 1) {
            context.println(getHelpText(), Colors.WHITE);
            return;
        }

        String subcmd = args[0];
        boolean verbose = false;
        String threadId = null;
        
        for (int i = 1; i < args.length; i++) {
            String arg = args[i];
            switch (arg) {
                case "-v", "--verbose" -> verbose = true;
                case "-t", "--thread" -> {
                    if (i + 1 < args.length) {
                        threadId = args[++i];
                    }
                }
            }
        }
        
        switch (subcmd) {
            case "list" -> handleList(args, verbose, context);
            case "info" -> handleInfo(args, verbose, context);
            case "functions" -> handleFunctions(args, verbose, context);
            case "symbols" -> handleSymbols(args, context);
            case "memory" -> handleMemory(context);
            case "heap" -> handleHeap(verbose, context);
            case "stack" -> handleStack(threadId, context);
            case "maps" -> handleMaps(verbose, context);
            case "search" -> handleSearch(args, context);
            default -> {
                context.println("未知子命令: " + subcmd, Colors.RED);
                context.println(getHelpText(), Colors.WHITE);
            }
        }
    }
    
    private void handleList(String[] args, boolean verbose, CommandExecutor.CmdExecContext context) {
        String pattern = args.length > 1 ? args[1] : null;
        
        try {
            List<String> libraries = getLoadedLibraries();
            
            context.print("已加载的Native库: ", Colors.CYAN);
            context.println(String.valueOf(libraries.size()) + " 个", Colors.YELLOW);
            context.println("", Colors.WHITE);
            
            for (String lib : libraries) {
                if (pattern == null || lib.contains(pattern)) {
                    context.print("  - ", Colors.GRAY);
                    context.println(lib, Colors.GREEN);
                    if (verbose) {
                        String libPath = getLibraryPath(lib);
                        if (libPath != null) {
                            context.print("    路径: ", Colors.CYAN);
                            context.println(libPath, Colors.GRAY);
                        }
                    }
                }
            }
            
        } catch (Exception e) {
            CommandExceptionHandler.handleException("native list", e, context, "获取native库列表失败");
        }
    }
    
    private void handleInfo(String[] args, boolean verbose, CommandExecutor.CmdExecContext context) {
        if (args.length < 2) {
            context.println("参数不足，需要指定库名", Colors.RED);
            return;
        }
        
        String libName = args[1];
        
        try {
            String libPath = getLibraryPath(libName);
            
            context.print("库信息: ", Colors.CYAN);
            context.println(libName, Colors.GREEN);
            context.print("路径: ", Colors.CYAN);
            context.println(libPath != null ? libPath : "未知", Colors.GRAY);
            
            if (libPath != null) {
                File libFile = new File(libPath);
                if (libFile.exists()) {
                    context.print("大小: ", Colors.CYAN);
                    context.println(libFile.length() + " 字节", Colors.YELLOW);
                    context.print("可读: ", Colors.CYAN);
                    context.println(String.valueOf(libFile.canRead()), libFile.canRead() ? Colors.GREEN : Colors.RED);
                    context.print("可执行: ", Colors.CYAN);
                    context.println(String.valueOf(libFile.canExecute()), libFile.canExecute() ? Colors.GREEN : Colors.RED);
                }
            }
            
            if (verbose) {
                context.println("", Colors.WHITE);
                context.println("导出符号:", Colors.CYAN);
                List<String> symbols = getLibrarySymbols(libName);
                for (String symbol : symbols) {
                    context.print("  - ", Colors.GRAY);
                    context.println(symbol, Colors.GRAY);
                }
            }
            
        } catch (Exception e) {
            CommandExceptionHandler.handleException("native info", e, context, "获取库信息失败");
        }
    }
    
    private void handleFunctions(String[] args, boolean verbose, CommandExecutor.CmdExecContext context) {
        if (args.length < 2) {
            context.println("参数不足，需要指定类名", Colors.RED);
            return;
        }
        
        String className = args[1];
        
        try {
            Class<?> targetClass = ClassResolver.findClassOrFail(className, context.classLoader());
            
            Method[] methods = targetClass.getDeclaredMethods();
            List<Method> nativeMethods = new ArrayList<>();
            
            for (Method method : methods) {
                if (Modifier.isNative(method.getModifiers())) {
                    nativeMethods.add(method);
                }
            }
            
            context.print("Native方法: ", Colors.CYAN);
            context.println(className, Colors.GREEN);
            context.print("数量: ", Colors.CYAN);
            context.println(nativeMethods.size() + " 个", Colors.YELLOW);
            context.println("", Colors.WHITE);
            
            for (Method method : nativeMethods) {
                context.print("  " + Modifier.toString(method.getModifiers()), Colors.GRAY);
                context.print(" " + method.getReturnType().getSimpleName(), Colors.CYAN);
                context.print(" " + method.getName(), Colors.GREEN);
                context.println("(" + Arrays.toString(method.getParameterTypes()) + ")", Colors.GRAY);
                
                if (verbose) {
                    String signature = getNativeSignature(method);
                    context.print("    JNI签名: ", Colors.CYAN);
                    context.println(signature, Colors.GRAY);
                }
            }
            
        } catch (Exception e) {
            CommandExceptionHandler.handleException("native functions", e, context, "获取native方法失败");
        }
    }

    private void handleSymbols(String[] args, CommandExecutor.CmdExecContext context) {
        if (args.length < 2) {
            context.println("参数不足，需要指定库名", Colors.RED);
            return;
        }
        
        String libName = args[1];
        
        try {
            List<String> symbols = getLibrarySymbols(libName);
            
            context.print("符号表: ", Colors.CYAN);
            context.println(libName, Colors.GREEN);
            context.print("数量: ", Colors.CYAN);
            context.println(symbols.size() + " 个", Colors.YELLOW);
            context.println("", Colors.WHITE);
            
            for (String symbol : symbols) {
                context.print("  - ", Colors.GRAY);
                context.println(symbol, Colors.GRAY);
            }
            
        } catch (Exception e) {
            CommandExceptionHandler.handleException("native symbols", e, context, "获取符号表失败");
        }
    }
    
    private void handleMemory(CommandExecutor.CmdExecContext context) {
        try {
            Map<String, String> memoryInfo = getNativeMemoryInfo();
            
            context.println("Native内存使用情况:", Colors.CYAN);
            context.println("", Colors.WHITE);
            
            for (Map.Entry<String, String> entry : memoryInfo.entrySet()) {
                context.print("  " + entry.getKey() + ": ", Colors.CYAN);
                context.println(entry.getValue(), Colors.YELLOW);
            }
            
        } catch (Exception e) {
            CommandExceptionHandler.handleException("native memory", e, context, "获取native内存信息失败");
        }
    }
    
    private void handleHeap(boolean verbose, CommandExecutor.CmdExecContext context) {
        try {
            Map<String, String> heapInfo = getNativeHeapInfo();
            
            context.println("Native堆内存:", Colors.CYAN);
            context.println("", Colors.WHITE);
            
            for (Map.Entry<String, String> entry : heapInfo.entrySet()) {
                context.print("  " + entry.getKey() + ": ", Colors.CYAN);
                context.println(entry.getValue(), Colors.YELLOW);
            }
            
            if (verbose) {
                context.println("", Colors.WHITE);
                context.println("详细信息:", Colors.CYAN);
                context.println("  注意: 完整的native堆分析需要:", Colors.GRAY);
                context.println("    - malloc_hook 或类似工具", Colors.GRAY);
                context.println("    - jemalloc 或 tcmalloc 支持", Colors.GRAY);
                context.println("    - ASAN 或 Valgrind 等工具", Colors.GRAY);
            }
            
        } catch (Exception e) {
            CommandExceptionHandler.handleException("native heap", e, context, "获取native堆信息失败");
        }
    }
    
    private void handleStack(String threadId, CommandExecutor.CmdExecContext context) {
        try {
            String stackTrace = getNativeStackTrace(threadId);
            
            context.print("Native栈跟踪", Colors.CYAN);
            if (threadId != null) {
                context.print(" (TID: ", Colors.GRAY);
                context.print(threadId, Colors.YELLOW);
                context.print(")", Colors.GRAY);
            }
            context.println(":", Colors.CYAN);
            context.println("", Colors.WHITE);
            context.println(stackTrace, Colors.GRAY);
            
        } catch (Exception e) {
            CommandExceptionHandler.handleException("native stack", e, context, "获取native栈失败");
        }
    }
    
    private void handleMaps(boolean verbose, CommandExecutor.CmdExecContext context) {
        try {
            List<String> maps = getMemoryMaps();
            
            context.print("内存映射: ", Colors.CYAN);
            context.println(maps.size() + " 个区域", Colors.YELLOW);
            context.println("", Colors.WHITE);
            
            for (String map : maps) {
                context.println("  " + map, Colors.GRAY);
            }
            
            if (verbose) {
                context.println("", Colors.WHITE);
                context.println("说明:", Colors.CYAN);
                context.println("  r - 读权限", Colors.GRAY);
                context.println("  w - 写权限", Colors.GRAY);
                context.println("  x - 执行权限", Colors.GRAY);
                context.println("  p - 私有映射", Colors.GRAY);
                context.println("  s - 共享映射", Colors.GRAY);
            }
            
        } catch (Exception e) {
            CommandExceptionHandler.handleException("native maps", e, context, "获取内存映射失败");
        }
    }
    
    private void handleSearch(String[] args, CommandExecutor.CmdExecContext context) {
        if (args.length < 2) {
            context.println("参数不足，需要指定搜索模式", Colors.RED);
            return;
        }
        
        String pattern = args[1];
        
        try {
            List<String> libraries = getLoadedLibraries();
            List<String> results = new ArrayList<>();
            
            for (String lib : libraries) {
                if (lib.contains(pattern)) {
                    results.add(lib);
                }
                
                List<String> symbols = getLibrarySymbols(lib);
                for (String symbol : symbols) {
                    if (symbol.contains(pattern)) {
                        results.add(lib + "::" + symbol);
                    }
                }
            }
            
            context.print("搜索结果: \"", Colors.CYAN);
            context.print(pattern, Colors.YELLOW);
            context.println("\"", Colors.CYAN);
            context.print("数量: ", Colors.CYAN);
            context.println(results.size() + " 个", Colors.YELLOW);
            context.println("", Colors.WHITE);
            
            for (String result : results) {
                context.print("  - ", Colors.GRAY);
                context.println(result, Colors.GREEN);
            }
            
        } catch (Exception e) {
            CommandExceptionHandler.handleException("native search", e, context, "搜索失败");
        }
    }
    
    private List<String> getLoadedLibraries() {
        List<String> libraries = new ArrayList<>();
        
        try {
            IOManager.ProcessResult result = RootProcessPool.executeCommand("cat /proc/self/maps", 30000, false);
            
            if (result.isSuccess()) {
                String[] lines = result.stdout().split("\n");
                for (String line : lines) {
                    if (line.contains(".so")) {
                        String[] parts = line.split("\\s+");
                        if (parts.length >= 6) {
                            String libPath = parts[5];
                            if (libPath.endsWith(".so") && !libraries.contains(libPath)) {
                                libraries.add(libPath);
                            }
                        }
                    }
                }
            }
            
        } catch (Exception e) {
            logger.error("获取已加载库失败", e);
        }
        
        return libraries;
    }
    
    private String getLibraryPath(String libName) {
        List<String> libraries = getLoadedLibraries();
        
        for (String lib : libraries) {
            if (lib.contains(libName)) {
                return lib;
            }
        }
        
        return null;
    }
    
    private List<String> getLibrarySymbols(String libName) {
        List<String> symbols = new ArrayList<>();
        
        try {
            String libPath = getLibraryPath(libName);
            if (libPath == null) {
                return symbols;
            }
            
            IOManager.ProcessResult result = RootProcessPool.executeCommand("readelf -s " + libPath, 30000, false);
            
            if (result.isSuccess()) {
                String[] lines = result.stdout().split("\n");
                for (String line : lines) {
                    if (line.contains("FUNC") || line.contains("OBJECT")) {
                        String[] parts = line.trim().split("\\s+");
                        if (parts.length >= 8) {
                            symbols.add(parts[7]);
                        }
                    }
                }
            }
            
        } catch (Exception e) {
            logger.error("获取库符号失败", e);
        }
        
        return symbols;
    }
    
    private String getNativeSignature(Method method) {
        StringBuilder sb = new StringBuilder();
        sb.append("(");
        
        Class<?>[] paramTypes = method.getParameterTypes();
        for (Class<?> paramType : paramTypes) {
            sb.append(getJNISignature(paramType));
        }
        
        sb.append(")").append(getJNISignature(method.getReturnType()));
        
        return sb.toString();
    }
    
    private String getJNISignature(Class<?> type) {
        return Type.getDescriptor(type);
    }
    
    private Map<String, String> getNativeMemoryInfo() {
        Map<String, String> info = new HashMap<>();
        
        try {
            IOManager.ProcessResult result = RootProcessPool.executeCommand("cat /proc/self/status", 30000, false);
            
            if (result.isSuccess()) {
                String[] lines = result.stdout().split("\n");
                for (String line : lines) {
                    if (line.startsWith("Vm") || line.contains("Mem")) {
                        String[] parts = line.split(":");
                        if (parts.length == 2) {
                            info.put(parts[0].trim(), parts[1].trim());
                        }
                    }
                }
            }
            
        } catch (Exception e) {
            logger.error("获取Native内存信息失败", e);
        }
        
        return info;
    }
    
    private Map<String, String> getNativeHeapInfo() {
        Map<String, String> info = new HashMap<>();
        
        try {
            info.put("注意", "完整的native堆信息需要malloc_hook或jemalloc支持");
            
        } catch (Exception e) {
            logger.error("获取native堆信息失败", e);
        }
        
        return info;
    }
    
    private String getNativeStackTrace(String threadId) {
        StringBuilder sb = new StringBuilder();
        
        try {
            String command = threadId != null ? 
                "cat /proc/" + threadId + "/stack" : 
                "cat /proc/self/task/*/stack";
            
            IOManager.ProcessResult result = RootProcessPool.executeCommand(command, 30000, false);
            
            if (result.isSuccess()) {
                String[] lines = result.stdout().split("\n");
                for (String line : lines) {
                    sb.append("  ").append(line).append("\n");
                }
            }
            
        } catch (Exception e) {
            logger.error("获取native栈失败", e);
            sb.append("注意: 获取native栈需要root权限\n");
        }
        
        return sb.toString();
    }
    
    private List<String> getMemoryMaps() {
        List<String> maps = new ArrayList<>();
        
        try {
            IOManager.ProcessResult result = RootProcessPool.executeCommand("cat /proc/self/maps", 30000, false);
            
            if (result.isSuccess()) {
                String[] lines = result.stdout().split("\n");
                Collections.addAll(maps, lines);
            }
            
        } catch (Exception e) {
            logger.error("获取内存映射失败", e);
        }
        
        return maps;
    }

}
