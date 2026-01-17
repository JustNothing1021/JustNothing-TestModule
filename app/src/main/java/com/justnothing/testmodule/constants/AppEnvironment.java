package com.justnothing.testmodule.constants;

public class AppEnvironment {
    private static boolean isHookEnvironment = false;

    public static boolean isHookEnv() {
        return isHookEnvironment;
    }

    public static void setHookEnv() {
        isHookEnvironment = true;
    }
}
