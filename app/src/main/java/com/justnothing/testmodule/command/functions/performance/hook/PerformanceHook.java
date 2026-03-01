package com.justnothing.testmodule.command.functions.performance.hook;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class PerformanceHook {
    private static final Map<Integer, MethodStats> methodStats = new ConcurrentHashMap<>();
    private static final Map<Integer, XC_MethodHook.Unhook> hooks = new ConcurrentHashMap<>();
    private static final AtomicInteger nextId = new AtomicInteger(1);

    public static int hookMethod(Class<?> clazz, String methodName, Class<?>[] paramTypes) {
        try {
            Method method = clazz.getDeclaredMethod(methodName, paramTypes);
            int id = nextId.getAndIncrement();
            
            XC_MethodHook hook = new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    param.setObjectExtra("startTime", System.nanoTime());
                }
                
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    Long startTime = (Long) param.getObjectExtra("startTime");
                    if (startTime != null) {
                        long duration = System.nanoTime() - startTime;
                        
                        MethodStats stats = methodStats.computeIfAbsent(id, 
                            k -> new MethodStats(clazz.getName(), methodName, paramTypes));
                        stats.recordCall(duration);
                    }
                }
            };
            
            XC_MethodHook.Unhook unhook = XposedBridge.hookMethod(method, hook);
            hooks.put(id, unhook);
            
            return id;
        } catch (NoSuchMethodException e) {
            throw new RuntimeException("方法未找到: " + clazz.getName() + "." + methodName, e);
        }
    }

    public static int hookAllMethods(Class<?> clazz, String methodName) {
        int id = nextId.getAndIncrement();
        
        XC_MethodHook hook = new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                param.setObjectExtra("startTime", System.nanoTime());
            }
            
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                Long startTime = (Long) param.getObjectExtra("startTime");
                if (startTime != null) {
                    long duration = System.nanoTime() - startTime;
                    
                    Method method = (Method) param.method;
                    Class<?>[] paramTypes = method.getParameterTypes();
                    
                    MethodStats stats = methodStats.computeIfAbsent(id, 
                        k -> new MethodStats(clazz.getName(), methodName, paramTypes));
                    stats.recordCall(duration);
                }
            }
        };
        
        XC_MethodHook.Unhook unhook = XposedHelpers.findAndHookMethod(clazz, methodName, hook);
        hooks.put(id, unhook);
        
        return id;
    }

    public static void unhookMethod(int id) {
        XC_MethodHook.Unhook unhook = hooks.remove(id);
        if (unhook != null) {
            unhook.unhook();
        }
        methodStats.remove(id);
    }

    public static void unhookAll() {
        for (XC_MethodHook.Unhook unhook : hooks.values()) {
            unhook.unhook();
        }
        hooks.clear();
        methodStats.clear();
        nextId.set(1);
    }

    public static Map<Integer, MethodStats> getStats() {
        return new ConcurrentHashMap<>(methodStats);
    }

    public static MethodStats getStats(int id) {
        return methodStats.get(id);
    }

    public static boolean hasHook(int id) {
        return hooks.containsKey(id);
    }

    public static int getHookCount() {
        return hooks.size();
    }
}
