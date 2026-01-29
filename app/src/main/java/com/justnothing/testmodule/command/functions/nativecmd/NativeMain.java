package com.justnothing.testmodule.command.functions.nativecmd;

import static com.justnothing.testmodule.constants.CommandServer.CMD_NATIVE_VER;

import com.justnothing.testmodule.command.CommandExecutor;
import com.justnothing.testmodule.command.functions.CommandBase;

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
                    disasm <class_name> <method_name>       - 反汇编native方法
                    symbols <lib_name>                      - 查看库的符号表
                    memory                                  - 查看native内存使用情况
                    heap                                    - 查看native堆内存
                    stack <tid>                             - 查看线程的native栈
                    maps                                    - 查看进程内存映射
                    search <pattern>                        - 搜索native库或函数
                
                选项:
                    -v, --verbose                           - 详细输出
                    -h, --hex                               - 以十六进制格式显示
                    -o, --output <path>                     - 指定输出文件路径
                    -t, --thread <tid>                      - 指定线程ID
                
                示例:
                    native list
                    native list libart.so
                    native info libc.so
                    native functions java.lang.System
                    native disasm java.lang.System arraycopy
                    native symbols libc.so
                    native memory
                    native heap
                    native stack 1234
                    native maps
                    native search malloc
                
                注意:
                    - 需要root权限才能访问某些信息
                    - 反汇编需要objdump或其他工具
                    - native调试需要理解ARM/x86汇编
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
        boolean hexFormat = false;
        String outputPath = null;
        String threadId = null;
        
        for (int i = 1; i < args.length; i++) {
            String arg = args[i];
            if (arg.equals("-v") || arg.equals("--verbose")) {
                verbose = true;
            } else if (arg.equals("-h") || arg.equals("--hex")) {
                hexFormat = true;
            } else if (arg.equals("-o") || arg.equals("--output")) {
                if (i + 1 < args.length) {
                    outputPath = args[++i];
                }
            } else if (arg.equals("-t") || arg.equals("--thread")) {
                if (i + 1 < args.length) {
                    threadId = args[++i];
                }
            }
        }
        
        return switch (subcmd) {
            case "list" -> handleList(args, verbose);
            case "info" -> handleInfo(args, verbose);
            case "functions" -> handleFunctions(args, verbose);
            case "disasm" -> handleDisasm(args, hexFormat);
            case "symbols" -> handleSymbols(args, verbose);
            case "memory" -> handleMemory(verbose);
            case "heap" -> handleHeap(verbose);
            case "stack" -> handleStack(threadId);
            case "maps" -> handleMaps(verbose);
            case "search" -> handleSearch(args, verbose);
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
            logger.error("获取native库列表失败", e);
            return "错误: " + e.getMessage() + "\n堆栈: " + getStackTrace(e);
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
            logger.error("获取库信息失败", e);
            return "错误: " + e.getMessage() + "\n堆栈: " + getStackTrace(e);
        }
    }
    
    private String handleFunctions(String[] args, boolean verbose) {
        if (args.length < 2) {
            return "参数不足，需要指定类名";
        }
        
        String className = args[1];
        
        try {
            Class<?> targetClass = Class.forName(className);
            
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
            logger.error("获取native方法失败", e);
            return "错误: " + e.getMessage() + "\n堆栈: " + getStackTrace(e);
        }
    }
    
    private String handleDisasm(String[] args, boolean hexFormat) {
        if (args.length < 3) {
            return "参数不足，需要指定类名和方法名";
        }
        
        String className = args[1];
        String methodName = args[2];
        
        try {
            Class<?> targetClass = Class.forName(className);
            
            Method[] methods = targetClass.getDeclaredMethods();
            Method targetMethod = null;
            
            for (Method method : methods) {
                if (method.getName().equals(methodName) && Modifier.isNative(method.getModifiers())) {
                    targetMethod = method;
                    break;
                }
            }
            
            if (targetMethod == null) {
                return "找不到native方法: " + methodName;
            }

            return "反汇编: " + className + "." + methodName + "\n\n" +
                    "你不会以为我真的做了吧 (雾)\n\n" +
                    "方法信息:\n" +
                    "  修饰符: " + Modifier.toString(targetMethod.getModifiers()) + "\n" +
                    "  返回类型: " + targetMethod.getReturnType().getName() + "\n" +
                    "  参数类型: " + Arrays.toString(targetMethod.getParameterTypes()) + "\n" +
                    "  JNI签名: " + getNativeSignature(targetMethod) + "\n";
            
        } catch (Exception e) {
            logger.error("反汇编命令执行失败", e);
            return "错误: " + e.getMessage() + "\n堆栈: " + getStackTrace(e);
        }
    }
    
    private String handleSymbols(String[] args, boolean verbose) {
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
            logger.error("获取符号表失败", e);
            return "错误: " + e.getMessage() + "\n堆栈: " + getStackTrace(e);
        }
    }
    
    private String handleMemory(boolean verbose) {
        try {
            Map<String, String> memoryInfo = getNativeMemoryInfo();
            
            StringBuilder sb = new StringBuilder();
            sb.append("Native内存使用情况:\n\n");
            
            for (Map.Entry<String, String> entry : memoryInfo.entrySet()) {
                sb.append("  ").append(entry.getKey()).append(": ").append(entry.getValue()).append("\n");
            }
            
            if (verbose) {
                sb.append("\n详细信息:\n");
                sb.append("  注意: 完整的native内存分析需要:\n");
                sb.append("    - /proc/self/maps 解析\n");
                sb.append("    - /proc/self/smaps 解析\n");
                sb.append("    - malloc_info 或类似工具\n");
            }
            
            return sb.toString();
            
        } catch (Exception e) {
            logger.error("获取native内存信息失败", e);
            return "错误: " + e.getMessage() + "\n堆栈: " + getStackTrace(e);
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
            logger.error("获取native堆信息失败", e);
            return "错误: " + e.getMessage() + "\n堆栈: " + getStackTrace(e);
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
            logger.error("获取native栈失败", e);
            return "错误: " + e.getMessage() + "\n堆栈: " + getStackTrace(e);
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
            logger.error("获取内存映射失败", e);
            return "错误: " + e.getMessage() + "\n堆栈: " + getStackTrace(e);
        }
    }
    
    private String handleSearch(String[] args, boolean verbose) {
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
            logger.error("搜索失败", e);
            return "错误: " + e.getMessage() + "\n堆栈: " + getStackTrace(e);
        }
    }
    
    private List<String> getLoadedLibraries() {
        List<String> libraries = new ArrayList<>();
        
        try {
            Runtime runtime = Runtime.getRuntime();
            java.lang.Process process = runtime.exec("cat /proc/self/maps");
            java.io.BufferedReader reader = new java.io.BufferedReader(
                new java.io.InputStreamReader(process.getInputStream())
            );
            
            String line;
            while ((line = reader.readLine()) != null) {
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
            
            reader.close();
            process.waitFor();
            
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
            
            Runtime runtime = Runtime.getRuntime();
            java.lang.Process process = runtime.exec("readelf -s " + libPath);
            java.io.BufferedReader reader = new java.io.BufferedReader(
                new java.io.InputStreamReader(process.getInputStream())
            );
            
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.contains("FUNC") || line.contains("OBJECT")) {
                    String[] parts = line.trim().split("\\s+");
                    if (parts.length >= 8) {
                        symbols.add(parts[7]);
                    }
                }
            }
            
            reader.close();
            process.waitFor();
            
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
            Runtime runtime = Runtime.getRuntime();
            java.lang.Process process = runtime.exec("cat /proc/self/status");
            java.io.BufferedReader reader = new java.io.BufferedReader(
                new java.io.InputStreamReader(process.getInputStream())
            );
            
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("Vm") || line.contains("Mem")) {
                    String[] parts = line.split(":");
                    if (parts.length == 2) {
                        info.put(parts[0].trim(), parts[1].trim());
                    }
                }
            }
            
            reader.close();
            process.waitFor();
            
        } catch (Exception e) {
            logger.error("获取native内存信息失败", e);
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
            Runtime runtime = Runtime.getRuntime();
            String command = threadId != null ? 
                "cat /proc/" + threadId + "/stack" : 
                "cat /proc/self/task/*/stack";
            
            java.lang.Process process = runtime.exec(command);
            java.io.BufferedReader reader = new java.io.BufferedReader(
                new java.io.InputStreamReader(process.getInputStream())
            );
            
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append("  ").append(line).append("\n");
            }
            
            reader.close();
            process.waitFor();
            
        } catch (Exception e) {
            logger.error("获取native栈失败", e);
            sb.append("注意: 获取native栈需要root权限\n");
        }
        
        return sb.toString();
    }
    
    private List<String> getMemoryMaps() {
        List<String> maps = new ArrayList<>();
        
        try {
            Runtime runtime = Runtime.getRuntime();
            java.lang.Process process = runtime.exec("cat /proc/self/maps");
            java.io.BufferedReader reader = new java.io.BufferedReader(
                new java.io.InputStreamReader(process.getInputStream())
            );
            
            String line;
            while ((line = reader.readLine()) != null) {
                maps.add(line);
            }
            
            reader.close();
            process.waitFor();
            
        } catch (Exception e) {
            logger.error("获取内存映射失败", e);
        }
        
        return maps;
    }
    
    private String getStackTrace(Exception e) {
        StringBuilder sb = new StringBuilder();
        for (StackTraceElement element : e.getStackTrace()) {
            sb.append("\n    at ").append(element.toString());
        }
        return sb.toString();
    }
}
