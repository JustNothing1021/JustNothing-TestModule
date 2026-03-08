package com.justnothing.testmodule.command.functions.breakpoint;

import com.justnothing.testmodule.utils.functions.Logger;
import com.justnothing.testmodule.utils.reflect.SignatureUtils;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Map;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

public class BreakpointHook {
    private static final Logger logger = Logger.getLoggerForName("BreakpointHook");
    private static final Map<Integer, XC_MethodHook.Unhook> activeHooks = new java.util.concurrent.ConcurrentHashMap<>();

    public static void setupBreakpoints(ClassLoader classLoader) {
        Map<Integer, BreakpointMain.BreakpointInfo> breakpoints = BreakpointMain.getBreakpoints();
        
        for (BreakpointMain.BreakpointInfo info : breakpoints.values()) {
            if (activeHooks.containsKey(info.id)) {
                logger.debug("断点已存在，跳过: " + info.id);
                continue;
            }
            
            try {
                XC_MethodHook.Unhook unhook = hookMethod(classLoader, info);
                if (unhook != null) {
                    activeHooks.put(info.id, unhook);
                    logger.info("断点Hook已设置: " + info.className + "." + info.methodName + " (ID: " + info.id + ")");
                }
            } catch (Exception e) {
                logger.error("设置断点Hook失败: " + info.className + "." + info.methodName + " (ID: " + info.id + ")", e);
            }
        }
    }

    private static XC_MethodHook.Unhook hookMethod(ClassLoader classLoader, BreakpointMain.BreakpointInfo info)
            throws ClassNotFoundException {
        Class<?> targetClass = XposedHelpers.findClass(info.className, classLoader);
        
        if (targetClass == null) {
            logger.error("类未找到: " + info.className);
            return null;
        }
        
        if (info.signature != null) {
            return hookMethodWithSignature(targetClass, info);
        } else {
            return hookAllMethods(targetClass, info);
        }
    }

    private static XC_MethodHook.Unhook hookMethodWithSignature(
            Class<?> targetClass, BreakpointMain.BreakpointInfo info)
            throws ClassNotFoundException {
        Class<?>[] paramTypes = SignatureUtils.parseSignature(info.signature);
        
        try {
            Method method = targetClass.getDeclaredMethod(info.methodName, paramTypes);
            return XposedBridge.hookMethod(method, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    if (!info.enabled) {
                        return;
                    }
                    
                    BreakpointMain.onBreakpointHit(info.id);
                    
                    logger.info("=== 断点命中 ===");
                    logger.info("类: " + info.className);
                    logger.info("方法: " + info.methodName);
                    logger.info("签名: " + info.signature);
                    
                    if (param.args != null && param.args.length > 0) {
                        logger.info("参数:");
                        for (int i = 0; i < param.args.length; i++) {
                            Object arg = param.args[i];
                            String argStr = arg != null ? arg.toString() : "null";
                            if (arg != null && arg.getClass().isArray()) {
                                argStr = Arrays.toString((Object[]) arg);
                            }
                            logger.info("  [" + i + "] " + (arg != null ? arg.getClass().getName() : "null") + " = " + argStr);
                        }
                    }
                    
                    logger.info("调用栈:");
                    StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
                    for (int i = 0; i < stackTrace.length && i < 20; i++) {
                        logger.info("  " + stackTrace[i]);
                    }
                    logger.info("================");
                }
            });
        } catch (NoSuchMethodException e) {
            logger.error("方法未找到: " + info.className + "." + info.methodName + info.signature, e);
            return null;
        }
    }

    private static XC_MethodHook.Unhook hookAllMethods(Class<?> targetClass, BreakpointMain.BreakpointInfo info) {
        Method[] methods = targetClass.getDeclaredMethods();
        int hookedCount = 0;
        XC_MethodHook.Unhook lastUnhook = null;
        
        for (Method method : methods) {
            if (method.getName().equals(info.methodName)) {
                XC_MethodHook.Unhook unhook = XposedBridge.hookMethod(method, new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        if (!info.enabled) {
                            return;
                        }
                        
                        BreakpointMain.onBreakpointHit(info.id);
                        
                        String signature = SignatureUtils.formatSignature(param.method);
                        logger.info("=== 断点命中 ===");
                        logger.info("类: " + info.className);
                        logger.info("方法: " + info.methodName);
                        logger.info("签名: " + signature);
                        
                        if (param.args != null && param.args.length > 0) {
                            logger.info("参数:");
                            for (int i = 0; i < param.args.length; i++) {
                                Object arg = param.args[i];
                                String argStr = arg != null ? arg.toString() : "null";
                                if (arg != null && arg.getClass().isArray()) {
                                    argStr = Arrays.toString((Object[]) arg);
                                }
                                logger.info("  [" + i + "] " + (arg != null ? arg.getClass().getName() : "null") + " = " + argStr);
                            }
                        }
                        
                        logger.info("调用栈:");
                        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
                        for (int i = 0; i < stackTrace.length && i < 20; i++) {
                            logger.info("  " + stackTrace[i]);
                        }
                        logger.info("================");
                    }
                });
                
                hookedCount++;
                lastUnhook = unhook;
            }
        }

        logger.warn("未找到任何方法: " + info.className + "." + info.methodName);
        return null;

    }




    public static void removeBreakpoint(int id) {
        XC_MethodHook.Unhook unhook = activeHooks.remove(id);
        if (unhook != null) {
            unhook.unhook();
            logger.info("断点Hook已移除: " + id);
        }
    }

    public static void clearAllBreakpoints() {
        for (XC_MethodHook.Unhook unhook : activeHooks.values()) {
            unhook.unhook();
        }
        activeHooks.clear();
        logger.info("所有断点Hook已清除");
    }
}
