package com.justnothing.testmodule.constants;

import com.justnothing.testmodule.BuildConfig;

public final class CommandServer {
    // 从 build.gradle 的 versionName 派生，别再写死：原来这里硬编码 "v0.4.9"，
    // 和 defaultConfig.versionName 是两处独立维护，改了版本号只会改一处，
    // 结果 help 横幅一直报旧版本。BuildConfig 是编译期常量，不是 Android 资源，
    // command/ 层在纯 JVM 下也照常可用。
    public static final String MAIN_MODULE_VER = "v" + BuildConfig.VERSION_NAME;
    public static final String CMD_BEAN_SHELL_VER = "v0.1.0";
    public static final String CMD_CLASS_VER = "v0.1.3";
    public static final String CMD_WATCH_VER = "v0.1.1";
    public static final String CMD_TRACE_VER = "v0.1.1";
    public static final String CMD_SCRIPT_VER = "v0.4.1";
    public static final String CMD_EXPORT_CONTEXT_VER = "v0.1.0";
    public static final String CMD_MEMORY_VER = "v0.1.1";
    public static final String CMD_THREADS_VER = "v0.1.1";
    public static final String CMD_SYSTEM_VER = "v0.1.0";
    public static final String CMD_BREAKPOINT_VER = "v0.1.0";
    public static final String CMD_PACKAGES_VER = "v0.1.0";
    public static final String CMD_HELP_VER = "v0.1.1";
    public static final String CMD_HOOK_VER = "v0.1.2";
    public static final String CMD_BYTECODE_VER = "v0.2.0";
    public static final String CMD_NATIVE_VER = "v0.1.0";
    public static final String CMD_PERFORMANCE_VER = "v0.1.1";
    public static final String CMD_ALIAS_VER = "v0.1.0";
    public static final String CMD_NETWORK_VER = "v0.1.0";
    public static final String CMD_DYK_VER = "v0.1.0";
    public static final String CMD_JANK_VER = "v0.1.1";
    public static final int DEFAULT_SOCKET_PORT = 11451;
}
