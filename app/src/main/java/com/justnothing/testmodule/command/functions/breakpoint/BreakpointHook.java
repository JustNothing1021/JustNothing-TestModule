package com.justnothing.testmodule.command.functions.breakpoint;

import com.justnothing.testmodule.utils.functions.Logger;

import java.lang.reflect.Member;
import java.lang.reflect.Method;
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

    private static XC_MethodHook.Unhook hookMethod(ClassLoader classLoader, BreakpointMain.BreakpointInfo info) {
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

    private static XC_MethodHook.Unhook hookMethodWithSignature(Class<?> targetClass, BreakpointMain.BreakpointInfo info) {
        Class<?>[] paramTypes = parseSignature(info.signature);
        
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
                                argStr = arrayToString(arg);
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
                        
                        String signature = getMethodSignature(param.method);
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
                                    argStr = arrayToString(arg);
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
        
        if (hookedCount == 0) {
            logger.warn("未找到任何方法: " + info.className + "." + info.methodName);
            return null;
        }
        
        logger.info("已Hook " + hookedCount + " 个方法: " + info.className + "." + info.methodName);
        return lastUnhook;
    }

    private static Class<?>[] parseSignature(String signature) {
        if (signature == null || signature.isEmpty()) {
            return new Class<?>[0];
        }
        
        String[] parts = signature.split(",");
        Class<?>[] paramTypes = new Class<?>[parts.length];
        
        for (int i = 0; i < parts.length; i++) {
            String part = parts[i].trim();
            paramTypes[i] = parseType(part);
        }
        
        return paramTypes;
    }

    private static Class<?> parseType(String typeName) {
        switch (typeName) {
            case "int":
                return int.class;
            case "long":
                return long.class;
            case "float":
                return float.class;
            case "double":
                return double.class;
            case "boolean":
                return boolean.class;
            case "char":
                return char.class;
            case "byte":
                return byte.class;
            case "short":
                return short.class;
            case "void":
                return void.class;
            case "String":
                return String.class;
            case "Object":
                return Object.class;
            default:
                if (typeName.startsWith("java.lang.")) {
                    try {
                        return Class.forName(typeName);
                    } catch (ClassNotFoundException e) {
                        logger.warn("类型未找到: " + typeName + ", 使用 Object.class");
                        return Object.class;
                    }
                } else if (typeName.startsWith("java.util.")) {
                    try {
                        return Class.forName(typeName);
                    } catch (ClassNotFoundException e) {
                        logger.warn("类型未找到: " + typeName + ", 使用 Object.class");
                        return Object.class;
                    }
                } else {
                    try {
                        return Class.forName("java.lang." + typeName);
                    } catch (ClassNotFoundException e) {
                        try {
                            return Class.forName("java.util." + typeName);
                        } catch (ClassNotFoundException e2) {
                            logger.warn("类型未找到: " + typeName + ", 使用 Object.class");
                            return Object.class;
                        }
                    }
                }
        }
    }

    private static String getMethodSignature(Member member) {
        if (!(member instanceof Method)) {
            return "()";
        }
        Method method = (Method) member;
        StringBuilder sb = new StringBuilder();
        sb.append("(");
        Class<?>[] paramTypes = method.getParameterTypes();
        for (int i = 0; i < paramTypes.length; i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(paramTypes[i].getSimpleName());
        }
        sb.append(")");
        return sb.toString();
    }

    private static String arrayToString(Object array) {
        if (array == null) {
            return "null";
        }
        
        if (!array.getClass().isArray()) {
            return array.toString();
        }
        
        if (array instanceof Object[]) {
            Object[] objArray = (Object[]) array;
            StringBuilder sb = new StringBuilder("[");
            for (int i = 0; i < objArray.length; i++) {
                if (i > 0) {
                    sb.append(", ");
                }
                sb.append(objArray[i] != null ? objArray[i].toString() : "null");
            }
            sb.append("]");
            return sb.toString();
        } else if (array instanceof int[]) {
            int[] intArray = (int[]) array;
            StringBuilder sb = new StringBuilder("[");
            for (int i = 0; i < intArray.length; i++) {
                if (i > 0) {
                    sb.append(", ");
                }
                sb.append(intArray[i]);
            }
            sb.append("]");
            return sb.toString();
        } else if (array instanceof long[]) {
            long[] longArray = (long[]) array;
            StringBuilder sb = new StringBuilder("[");
            for (int i = 0; i < longArray.length; i++) {
                if (i > 0) {
                    sb.append(", ");
                }
                sb.append(longArray[i]);
            }
            sb.append("]");
            return sb.toString();
        } else if (array instanceof float[]) {
            float[] floatArray = (float[]) array;
            StringBuilder sb = new StringBuilder("[");
            for (int i = 0; i < floatArray.length; i++) {
                if (i > 0) {
                    sb.append(", ");
                }
                sb.append(floatArray[i]);
            }
            sb.append("]");
            return sb.toString();
        } else if (array instanceof double[]) {
            double[] doubleArray = (double[]) array;
            StringBuilder sb = new StringBuilder("[");
            for (int i = 0; i < doubleArray.length; i++) {
                if (i > 0) {
                    sb.append(", ");
                }
                sb.append(doubleArray[i]);
            }
            sb.append("]");
            return sb.toString();
        } else if (array instanceof boolean[]) {
            boolean[] boolArray = (boolean[]) array;
            StringBuilder sb = new StringBuilder("[");
            for (int i = 0; i < boolArray.length; i++) {
                if (i > 0) {
                    sb.append(", ");
                }
                sb.append(boolArray[i]);
            }
            sb.append("]");
            return sb.toString();
        } else if (array instanceof char[]) {
            char[] charArray = (char[]) array;
            StringBuilder sb = new StringBuilder("[");
            for (int i = 0; i < charArray.length; i++) {
                if (i > 0) {
                    sb.append(", ");
                }
                sb.append(charArray[i]);
            }
            sb.append("]");
            return sb.toString();
        } else if (array instanceof byte[]) {
            byte[] byteArray = (byte[]) array;
            StringBuilder sb = new StringBuilder("[");
            for (int i = 0; i < byteArray.length; i++) {
                if (i > 0) {
                    sb.append(", ");
                }
                sb.append(byteArray[i]);
            }
            sb.append("]");
            return sb.toString();
        } else if (array instanceof short[]) {
            short[] shortArray = (short[]) array;
            StringBuilder sb = new StringBuilder("[");
            for (int i = 0; i < shortArray.length; i++) {
                if (i > 0) {
                    sb.append(", ");
                }
                sb.append(shortArray[i]);
            }
            sb.append("]");
            return sb.toString();
        }
        
        return array.toString();
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
