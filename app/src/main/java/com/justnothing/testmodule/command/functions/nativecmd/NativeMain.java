package com.justnothing.testmodule.command.functions.nativecmd;

import static com.justnothing.testmodule.constants.CommandServer.CMD_NATIVE_VER;

import com.justnothing.testmodule.command.CommandExecutor;
import com.justnothing.testmodule.command.functions.CommandBase;
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
    public String runMain(CommandExecutor.CmdExecContext context) {
        String[] args = context.args();
        
        if (args.length < 1) {
            return getHelpText();
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
        
        return switch (subcmd) {
            case "list" -> handleList(args, verbose);
            case "info" -> handleInfo(args, verbose);
            case "functions" -> handleFunctions(args, verbose, context.classLoader());
            case "symbols" -> handleSymbols(args);
            case "memory" -> handleMemory();
            case "heap" -> handleHeap(verbose);
            case "stack" -> handleStack(threadId);
            case "maps" -> handleMaps(verbose);
            case "search" -> handleSearch(args);
            default -> "未知子命令: " + subcmd + "\n" + getHelpText();
        };
    }
    
    private String handleList(String[] args, boolean verbose) {
        String pattern = args.length > 1 ? args[1] : null;
        
        try {
            List<String> libraries = getLoadedLibraries();
            
            StringBuilder sb = new StringBuilder();
            sb.append("已加载的Native库: ").append(libraries.size()).append(" 个\n\n");
            
            for (String lib : libraries) {
                if (pattern == null || lib.contains(pattern)) {
                    sb.append("  - ").append(lib).append("\n");
                    if (verbose) {
                        String libPath = getLibraryPath(lib);
                        if (libPath != null) {
                            sb.append("    路径: ").append(libPath).append("\n");
                        }
                    }
                }
            }
            
            return sb.toString();
            
        } catch (Exception e) {
            return CommandExceptionHandler.handleException("native list", e, logger, "获取native库列表失败");
        }
    }
    
    private String handleInfo(String[] args, boolean verbose) {
        if (args.length < 2) {
            return "参数不足，需要指定库名";
        }
        
        String libName = args[1];
        
        try {
            String libPath = getLibraryPath(libName);
            
            StringBuilder sb = new StringBuilder();
            sb.append("库信息: ").append(libName).append("\n");
            sb.append("路径: ").append(libPath != null ? libPath : "未知").append("\n");
            
            if (libPath != null) {
                File libFile = new File(libPath);
                if (libFile.exists()) {
                    sb.append("大小: ").append(libFile.length()).append(" 字节\n");
                    sb.append("可读: ").append(libFile.canRead()).append("\n");
                    sb.append("可执行: ").append(libFile.canExecute()).append("\n");
                }
            }
            
            if (verbose) {
                sb.append("\n导出符号:\n");
                List<String> symbols = getLibrarySymbols(libName);
                for (String symbol : symbols) {
                    sb.append("  - ").append(symbol).append("\n");
                }
            }
            
            return sb.toString();
            
        } catch (Exception e) {
            return CommandExceptionHandler.handleException("native info", e, logger, "获取库信息失败");
        }
    }
    
    private String handleFunctions(String[] args, boolean verbose, ClassLoader classLoader) {
        if (args.length < 2) {
            return "参数不足，需要指定类名";
        }
        
        String className = args[1];
        
        try {
            Class<?> targetClass = ClassResolver.findClassOrFail(className, classLoader);
            
            Method[] methods = targetClass.getDeclaredMethods();
            List<Method> nativeMethods = new ArrayList<>();
            
            for (Method method : methods) {
                if (Modifier.isNative(method.getModifiers())) {
                    nativeMethods.add(method);
                }
            }
            
            StringBuilder sb = new StringBuilder();
            sb.append("Native方法: ").append(className).append("\n");
            sb.append("数量: ").append(nativeMethods.size()).append(" 个\n\n");
            
            for (Method method : nativeMethods) {
                sb.append("  ").append(Modifier.toString(method.getModifiers()))
                  .append(" ").append(method.getReturnType().getSimpleName())
                  .append(" ").append(method.getName())
                  .append("(").append(Arrays.toString(method.getParameterTypes())).append(")\n");
                
                if (verbose) {
                    String signature = getNativeSignature(method);
                    sb.append("    JNI签名: ").append(signature).append("\n");
                }
            }
            
            return sb.toString();
            
        } catch (Exception e) {
            return CommandExceptionHandler.handleException("native functions", e, logger, "获取native方法失败");
        }
    }

    private String handleSymbols(String[] args) {
        if (args.length < 2) {
            return "参数不足，需要指定库名";
        }
        
        String libName = args[1];
        
        try {
            List<String> symbols = getLibrarySymbols(libName);
            
            StringBuilder sb = new StringBuilder();
            sb.append("符号表: ").append(libName).append("\n");
            sb.append("数量: ").append(symbols.size()).append(" 个\n\n");
            
            for (String symbol : symbols) {
                sb.append("  - ").append(symbol).append("\n");
            }
            
            return sb.toString();
            
        } catch (Exception e) {
            return CommandExceptionHandler.handleException("native symbols", e, logger, "获取符号表失败");
        }
    }
    
    private String handleMemory() {
        try {
            Map<String, String> memoryInfo = getNativeMemoryInfo();
            
            StringBuilder sb = new StringBuilder();
            sb.append("Native内存使用情况:\n\n");
            
            for (Map.Entry<String, String> entry : memoryInfo.entrySet()) {
                sb.append("  ").append(entry.getKey()).append(": ").append(entry.getValue()).append("\n");
            }
            

            return sb.toString();
            
        } catch (Exception e) {
            return CommandExceptionHandler.handleException("native memory", e, logger, "获取native内存信息失败");
        }
    }
    
    private String handleHeap(boolean verbose) {
        try {
            Map<String, String> heapInfo = getNativeHeapInfo();
            
            StringBuilder sb = new StringBuilder();
            sb.append("Native堆内存:\n\n");
            
            for (Map.Entry<String, String> entry : heapInfo.entrySet()) {
                sb.append("  ").append(entry.getKey()).append(": ").append(entry.getValue()).append("\n");
            }
            
            if (verbose) {
                sb.append("\n详细信息:\n");
                sb.append("  注意: 完整的native堆分析需要:\n");
                sb.append("    - malloc_hook 或类似工具\n");
                sb.append("    - jemalloc 或 tcmalloc 支持\n");
                sb.append("    - ASAN 或 Valgrind 等工具\n");
            }
            
            return sb.toString();
            
        } catch (Exception e) {
            return CommandExceptionHandler.handleException("native heap", e, logger, "获取native堆信息失败");
        }
    }
    
    private String handleStack(String threadId) {
        try {
            String stackTrace = getNativeStackTrace(threadId);
            
            StringBuilder sb = new StringBuilder();
            sb.append("Native栈跟踪");
            if (threadId != null) {
                sb.append(" (TID: ").append(threadId).append(")");
            }
            sb.append(":\n\n");
            sb.append(stackTrace);
            
            return sb.toString();
            
        } catch (Exception e) {
            return CommandExceptionHandler.handleException("native stack", e, logger, "获取native栈失败");
        }
    }
    
    private String handleMaps(boolean verbose) {
        try {
            List<String> maps = getMemoryMaps();
            
            StringBuilder sb = new StringBuilder();
            sb.append("内存映射: ").append(maps.size()).append(" 个区域\n\n");
            
            for (String map : maps) {
                sb.append("  ").append(map).append("\n");
            }
            
            if (verbose) {
                sb.append("\n说明:\n");
                sb.append("  r - 读权限\n");
                sb.append("  w - 写权限\n");
                sb.append("  x - 执行权限\n");
                sb.append("  p - 私有映射\n");
                sb.append("  s - 共享映射\n");
            }
            
            return sb.toString();
            
        } catch (Exception e) {
            return CommandExceptionHandler.handleException("native maps", e, logger, "获取内存映射失败");
        }
    }
    
    private String handleSearch(String[] args) {
        if (args.length < 2) {
            return "参数不足，需要指定搜索模式";
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
            
            StringBuilder sb = new StringBuilder();
            sb.append("搜索结果: \"").append(pattern).append("\"\n");
            sb.append("数量: ").append(results.size()).append(" 个\n\n");
            
            for (String result : results) {
                sb.append("  - ").append(result).append("\n");
            }
            
            return sb.toString();
            
        } catch (Exception e) {
            return CommandExceptionHandler.handleException("native search", e, logger, "搜索失败");
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
                for (String line : lines) {
                    maps.add(line);
                }
            }
            
        } catch (Exception e) {
            logger.error("获取内存映射失败", e);
        }
        
        return maps;
    }

}
