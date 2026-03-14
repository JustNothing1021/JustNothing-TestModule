package com.justnothing.testmodule.constants;

import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;

public class AppEnvironment {
    private static boolean isHookEnvironment = false;
    private static boolean isAndroidEnvironment = true;

    static {
        try {
            Class<?> logClass = Class.forName("android.util.Log");
            Method m = logClass.getMethod("d", String.class, String.class);
            m.invoke(null, "JustNothing", "当前为安卓环境");
        }
        catch (ClassNotFoundException | NoSuchMethodException 
            | IllegalAccessException | InvocationTargetException e) {
            isAndroidEnvironment = false;
        }
    }


    public static boolean isHookEnv() {
        return isHookEnvironment;
    }

    public static boolean isAndroidEnv() {
        return isAndroidEnvironment;
    }

    public static void setHookEnv() {
        isHookEnvironment = true;
    }
}
