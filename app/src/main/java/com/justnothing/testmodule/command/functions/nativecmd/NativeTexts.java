package com.justnothing.testmodule.command.functions.nativecmd;

import com.justnothing.testmodule.command.framework.i18n.CliTexts;

import java.util.Map;

/**
 * nativecmd 命令族的 CLI 文案（id 常量 + 中英对照）。
 *
 * <p>命名与 id 规范见 {@link CliTexts}。
 * 本类由 {@code CliTexts} 的静态块登记，新增条目只需在这里加常量 + 两行 put。</p>
 *
 * <p>英文允许缺失（只 put 中文），缺失时英文环境回落显示中文 —— 所以翻译可以一条一条补。</p>
 */
public final class NativeTexts {

    // ==================== @Cmd（主命令）====================
    public static final String CMD_NATIVE_DESC = "cmd.native.desc";

    // ==================== @CmdRoutes.Route（命令列表里那一行）====================
    public static final String ROUTE_NATIVE_LIST_DESC = "route.native.list.desc";
    public static final String ROUTE_NATIVE_INFO_DESC = "route.native.info.desc";
    public static final String ROUTE_NATIVE_CLI_DESC = "route.native.cli.desc";
    public static final String ROUTE_NATIVE_SYMBOLS_DESC = "route.native.symbols.desc";
    public static final String ROUTE_NATIVE_MEMORY_DESC = "route.native.memory.desc";
    public static final String ROUTE_NATIVE_HEAP_DESC = "route.native.heap.desc";
    public static final String ROUTE_NATIVE_STACK_DESC = "route.native.stack.desc";
    public static final String ROUTE_NATIVE_MAPS_DESC = "route.native.maps.desc";
    public static final String ROUTE_NATIVE_SEARCH_DESC = "route.native.search.desc";

    // ==================== @CmdParam（参数说明）====================
    public static final String PARAM_NATIVE_LIST_PATTERN_DESC = "param.native.list.pattern.desc";
    public static final String PARAM_NATIVE_LIST_VERBOSE_DESC = "param.native.list.verbose.desc";
    public static final String PARAM_NATIVE_INFO_LIBNAME_DESC = "param.native.info.libName.desc";
    public static final String PARAM_NATIVE_INFO_VERBOSE_DESC = "param.native.info.verbose.desc";
    public static final String PARAM_NATIVE_CLI_CLASSNAME_DESC = "param.native.cli.className.desc";
    public static final String PARAM_NATIVE_CLI_VERBOSE_DESC = "param.native.cli.verbose.desc";
    public static final String PARAM_NATIVE_SYMBOLS_LIBNAME_DESC = "param.native.symbols.libName.desc";
    public static final String PARAM_NATIVE_HEAP_VERBOSE_DESC = "param.native.heap.verbose.desc";
    public static final String PARAM_NATIVE_STACK_THREADID_DESC = "param.native.stack.threadId.desc";
    public static final String PARAM_NATIVE_MAPS_VERBOSE_DESC = "param.native.maps.verbose.desc";
    public static final String PARAM_NATIVE_SEARCH_PATTERN_DESC = "param.native.search.pattern.desc";

    private NativeTexts() {
    }

    /**
     * 由 {@code CliTexts} 的静态块调用。必须是 public —— 它在另一个包里。
     * 命名上刻意带 register 而不是「构造时自己注册」：登记动作集中在 CliTexts 一处，
     * 「哪些族登记了」才看得全，漏登记也能被守卫测试发现。
     */
    public static void register(Map<String, String> zh, Map<String, String> en) {
        zh.put(CMD_NATIVE_DESC, "查看和调试Native代码，分析JNI函数和库");
        en.put(CMD_NATIVE_DESC, "Inspect and debug native code, and analyze JNI functions and libraries");

        zh.put(ROUTE_NATIVE_LIST_DESC, "列出已加载的native库");
        en.put(ROUTE_NATIVE_LIST_DESC, "List loaded native libraries");

        zh.put(ROUTE_NATIVE_INFO_DESC, "查看native库的详细信息");
        en.put(ROUTE_NATIVE_INFO_DESC, "Show details of a native library");

        zh.put(ROUTE_NATIVE_CLI_DESC, "列出类的native方法");
        en.put(ROUTE_NATIVE_CLI_DESC, "List the native methods of a class");

        zh.put(ROUTE_NATIVE_SYMBOLS_DESC, "查看库的符号表");
        en.put(ROUTE_NATIVE_SYMBOLS_DESC, "Show a library's symbol table");

        zh.put(ROUTE_NATIVE_MEMORY_DESC, "查看native内存使用情况");
        en.put(ROUTE_NATIVE_MEMORY_DESC, "Show native memory usage");

        zh.put(ROUTE_NATIVE_HEAP_DESC, "查看native堆内存");
        en.put(ROUTE_NATIVE_HEAP_DESC, "Show native heap memory");

        zh.put(ROUTE_NATIVE_STACK_DESC, "查看线程的native栈");
        en.put(ROUTE_NATIVE_STACK_DESC, "Show a thread's native stack trace");

        zh.put(ROUTE_NATIVE_MAPS_DESC, "查看进程内存映射");
        en.put(ROUTE_NATIVE_MAPS_DESC, "Show the process memory map");

        zh.put(ROUTE_NATIVE_SEARCH_DESC, "搜索native库或函数");
        en.put(ROUTE_NATIVE_SEARCH_DESC, "Search native libraries or functions");

        zh.put(PARAM_NATIVE_LIST_PATTERN_DESC, "过滤模式");
        en.put(PARAM_NATIVE_LIST_PATTERN_DESC, "Filter pattern");

        zh.put(PARAM_NATIVE_LIST_VERBOSE_DESC, "详细输出");
        en.put(PARAM_NATIVE_LIST_VERBOSE_DESC, "Verbose output");

        zh.put(PARAM_NATIVE_INFO_LIBNAME_DESC, "库名");
        en.put(PARAM_NATIVE_INFO_LIBNAME_DESC, "Library name");

        zh.put(PARAM_NATIVE_INFO_VERBOSE_DESC, "详细输出");
        en.put(PARAM_NATIVE_INFO_VERBOSE_DESC, "Verbose output");

        zh.put(PARAM_NATIVE_CLI_CLASSNAME_DESC, "类名");
        en.put(PARAM_NATIVE_CLI_CLASSNAME_DESC, "Class name");

        zh.put(PARAM_NATIVE_CLI_VERBOSE_DESC, "详细输出");
        en.put(PARAM_NATIVE_CLI_VERBOSE_DESC, "Verbose output");

        zh.put(PARAM_NATIVE_SYMBOLS_LIBNAME_DESC, "库名");
        en.put(PARAM_NATIVE_SYMBOLS_LIBNAME_DESC, "Library name");

        zh.put(PARAM_NATIVE_HEAP_VERBOSE_DESC, "详细输出");
        en.put(PARAM_NATIVE_HEAP_VERBOSE_DESC, "Verbose output");

        zh.put(PARAM_NATIVE_STACK_THREADID_DESC, "线程ID");
        en.put(PARAM_NATIVE_STACK_THREADID_DESC, "Thread ID");

        zh.put(PARAM_NATIVE_MAPS_VERBOSE_DESC, "详细输出");
        en.put(PARAM_NATIVE_MAPS_VERBOSE_DESC, "Verbose output");

        zh.put(PARAM_NATIVE_SEARCH_PATTERN_DESC, "搜索模式");
        en.put(PARAM_NATIVE_SEARCH_PATTERN_DESC, "Search pattern");
    }
}
