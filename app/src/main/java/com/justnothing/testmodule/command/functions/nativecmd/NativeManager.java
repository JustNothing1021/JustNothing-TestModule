package com.justnothing.testmodule.command.functions.nativecmd;

import com.justnothing.testmodule.utils.io.IOManager;
import com.justnothing.testmodule.utils.io.RootProcessPool;
import com.justnothing.testmodule.utils.logging.Logger;

import org.objectweb.asm.Type;

import java.io.File;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NativeManager {

    private static final Logger logger = Logger.getLoggerForName("NativeManager");

    public List<String> getLoadedLibraries() {
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

    public String getLibraryPath(String libName) {
        List<String> libraries = getLoadedLibraries();
        
        for (String lib : libraries) {
            if (lib.contains(libName)) {
                return lib;
            }
        }
        
        return null;
    }

    public List<String> getLibrarySymbols(String libName) {
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

    public String getNativeSignature(Method method) {
        StringBuilder sb = new StringBuilder();
        sb.append("(");
        
        Class<?>[] paramTypes = method.getParameterTypes();
        for (Class<?> paramType : paramTypes) {
            sb.append(getJNISignature(paramType));
        }
        
        sb.append(")").append(getJNISignature(method.getReturnType()));
        
        return sb.toString();
    }

    public String getJNISignature(Class<?> type) {
        return Type.getDescriptor(type);
    }

    public Map<String, String> getNativeMemoryInfo() {
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

    public Map<String, String> getNativeHeapInfo() {
        Map<String, String> info = new HashMap<>();
        
        try {
            info.put("注意", "完整的native堆信息需要malloc_hook或jemalloc支持");
            
        } catch (Exception e) {
            logger.error("获取native堆信息失败", e);
        }
        
        return info;
    }

    public String getNativeStackTrace(String threadId) {
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

    public List<String> getMemoryMaps() {
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

    public static NativeManager getInstance() {
        return new NativeManager();
    }
}
