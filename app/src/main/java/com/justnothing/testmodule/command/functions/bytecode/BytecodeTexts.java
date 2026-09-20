package com.justnothing.testmodule.command.functions.bytecode;

import java.util.Map;

/**
 * bytecode 命令族的 CLI 文案（id 常量 + 中英对照）。
 *
 * <p>命名与 id 规范见 {@link com.justnothing.testmodule.command.framework.i18n.CliTexts}。
 * 本类由 {@code CliTexts} 的静态块登记，新增条目只需在这里加常量 + 两行 put。</p>
 *
 * <p>英文允许缺失（只 put 中文），缺失时英文环境回落显示中文 —— 所以翻译可以一条一条补。</p>
 */
public final class BytecodeTexts {

    // ==================== @Cmd（主命令）====================
    public static final String CMD_BYTECODE_DESC = "cmd.bytecode.desc";

    // ==================== @CmdRoutes.Route（命令列表里那一行）====================
    public static final String ROUTE_BYTECODE_INFO_DESC = "route.bytecode.info.desc";
    public static final String ROUTE_BYTECODE_METHOD_DESC = "route.bytecode.method.desc";
    public static final String ROUTE_BYTECODE_DUMP_DESC = "route.bytecode.dump.desc";
    public static final String ROUTE_BYTECODE_LOCATE_DESC = "route.bytecode.locate.desc";
    public static final String ROUTE_BYTECODE_ANALYZE_DESC = "route.bytecode.analyze.desc";
    public static final String ROUTE_BYTECODE_DISASM_DESC = "route.bytecode.disasm.desc";
    public static final String ROUTE_BYTECODE_CONSTANTS_DESC = "route.bytecode.constants.desc";
    public static final String ROUTE_BYTECODE_VERIFY_DESC = "route.bytecode.verify.desc";
    public static final String ROUTE_BYTECODE_SOURCE_DESC = "route.bytecode.source.desc";
    public static final String ROUTE_BYTECODE_BATCH_EXPORT_DESC = "route.bytecode.batch_export.desc";
    public static final String ROUTE_BYTECODE_LIST_CLASSES_DESC = "route.bytecode.list_classes.desc";
    public static final String ROUTE_BYTECODE_FIND_DESC = "route.bytecode.find.desc";

    // ==================== @CmdParam（参数说明）====================
    // --- bytecode info ---
    public static final String PARAM_BYTECODE_INFO_CLASSNAME_DESC = "param.bytecode.info.className.desc";
    public static final String PARAM_BYTECODE_INFO_VERBOSE_DESC = "param.bytecode.info.verbose.desc";

    // --- bytecode method ---
    public static final String PARAM_BYTECODE_METHOD_CLASSNAME_DESC = "param.bytecode.method.className.desc";
    public static final String PARAM_BYTECODE_METHOD_METHODNAME_DESC = "param.bytecode.method.methodName.desc";

    // --- bytecode dump ---
    public static final String PARAM_BYTECODE_DUMP_CLASSNAME_DESC = "param.bytecode.dump.className.desc";
    public static final String PARAM_BYTECODE_DUMP_OUTPUTPATH_DESC = "param.bytecode.dump.outputPath.desc";
    public static final String PARAM_BYTECODE_DUMP_DISASM_DESC = "param.bytecode.dump.disasm.desc";

    // --- bytecode locate ---
    public static final String PARAM_BYTECODE_LOCATE_CLASSNAME_DESC = "param.bytecode.locate.className.desc";

    // --- bytecode analyze ---
    public static final String PARAM_BYTECODE_ANALYZE_CLASSNAME_DESC = "param.bytecode.analyze.className.desc";
    public static final String PARAM_BYTECODE_ANALYZE_VERBOSE_DESC = "param.bytecode.analyze.verbose.desc";

    // --- bytecode disasm ---
    public static final String PARAM_BYTECODE_DISASM_CLASSNAME_DESC = "param.bytecode.disasm.className.desc";
    public static final String PARAM_BYTECODE_DISASM_METHODNAME_DESC = "param.bytecode.disasm.methodName.desc";
    public static final String PARAM_BYTECODE_DISASM_OUTPUTPATH_DESC = "param.bytecode.disasm.outputPath.desc";

    // --- bytecode constants ---
    public static final String PARAM_BYTECODE_CONSTANTS_CLASSNAME_DESC = "param.bytecode.constants.className.desc";

    // --- bytecode verify ---
    public static final String PARAM_BYTECODE_VERIFY_CLASSNAME_DESC = "param.bytecode.verify.className.desc";

    // --- bytecode source ---
    public static final String PARAM_BYTECODE_SOURCE_CLASSNAME_DESC = "param.bytecode.source.className.desc";
    public static final String PARAM_BYTECODE_SOURCE_OUTPUTPATH_DESC = "param.bytecode.source.outputPath.desc";
    public static final String PARAM_BYTECODE_SOURCE_HIGHLIGHT_DESC = "param.bytecode.source.highlight.desc";

    // --- bytecode batch_export ---
    public static final String PARAM_BYTECODE_BATCH_EXPORT_SOURCE_DESC = "param.bytecode.batch_export.source.desc";
    public static final String PARAM_BYTECODE_BATCH_EXPORT_OUTPUTPATH_DESC = "param.bytecode.batch_export.outputPath.desc";
    public static final String PARAM_BYTECODE_BATCH_EXPORT_LIMIT_DESC = "param.bytecode.batch_export.limit.desc";

    // --- bytecode list_classes ---
    public static final String PARAM_BYTECODE_LIST_CLASSES_SOURCE_DESC = "param.bytecode.list_classes.source.desc";
    public static final String PARAM_BYTECODE_LIST_CLASSES_LIMIT_DESC = "param.bytecode.list_classes.limit.desc";

    // --- bytecode find ---
    public static final String PARAM_BYTECODE_FIND_KEYWORD_DESC = "param.bytecode.find.keyword.desc";
    public static final String PARAM_BYTECODE_FIND_SOURCE_DESC = "param.bytecode.find.source.desc";
    public static final String PARAM_BYTECODE_FIND_LIMIT_DESC = "param.bytecode.find.limit.desc";

    private BytecodeTexts() {
    }

    /**
     * 由 {@code CliTexts} 的静态块调用。必须是 public —— 它在另一个包里。
     * 命名上刻意带 register 而不是「构造时自己注册」：登记动作集中在 CliTexts 一处，
     * 「哪些族登记了」才看得全，漏登记也能被守卫测试发现。
     */
    public static void register(Map<String, String> zh, Map<String, String> en) {
        zh.put(CMD_BYTECODE_DESC, "查看和分析Java字节码");
        en.put(CMD_BYTECODE_DESC, "Inspect and analyze Java bytecode");

        zh.put(ROUTE_BYTECODE_INFO_DESC, "查看类的元数据（反射）");
        en.put(ROUTE_BYTECODE_INFO_DESC, "Show class metadata (via reflection)");

        zh.put(ROUTE_BYTECODE_METHOD_DESC, "查看指定方法的元数据（方法体需导出 dex 后反编译）");
        en.put(ROUTE_BYTECODE_METHOD_DESC, "Show metadata of a method (its body requires exporting the dex and decompiling)");

        zh.put(ROUTE_BYTECODE_DUMP_DESC, "导出某个类所在的 dex（-d 顺带用 dexdump 反汇编）");
        en.put(ROUTE_BYTECODE_DUMP_DESC, "Export the dex containing a class (-d also disassembles it with dexdump)");

        zh.put(ROUTE_BYTECODE_LOCATE_DESC, "查找类在哪个文件里（只定位，不提取）");
        en.put(ROUTE_BYTECODE_LOCATE_DESC, "Find which file a class lives in (locate only, no extraction)");

        zh.put(ROUTE_BYTECODE_ANALYZE_DESC, "分析类所在 dex 的结构");
        en.put(ROUTE_BYTECODE_ANALYZE_DESC, "Analyze the structure of the dex containing a class");

        zh.put(ROUTE_BYTECODE_DISASM_DESC, "反汇编成 dalvik 指令（设备自带 dexdump，可选方法名 / -o 存文件）");
        en.put(ROUTE_BYTECODE_DISASM_DESC, "Disassemble into dalvik instructions (uses the built-in dexdump; optional method name / -o to save to a file)");

        zh.put(ROUTE_BYTECODE_CONSTANTS_DESC, "查看静态常量字段");
        en.put(ROUTE_BYTECODE_CONSTANTS_DESC, "Show static constant fields");

        zh.put(ROUTE_BYTECODE_VERIFY_DESC, "校验类所在 dex 的完整性");
        en.put(ROUTE_BYTECODE_VERIFY_DESC, "Verify the integrity of the dex containing a class");

        zh.put(ROUTE_BYTECODE_SOURCE_DESC, "在设备上把类反编译成 Java 源码（dex2jar + CFR，按类处理，省内存）");
        en.put(ROUTE_BYTECODE_SOURCE_DESC, "Decompile a class to Java source on device (dex2jar + CFR, one class at a time to save memory)");

        zh.put(ROUTE_BYTECODE_BATCH_EXPORT_DESC, "批量导出 dex");
        en.put(ROUTE_BYTECODE_BATCH_EXPORT_DESC, "Batch export dex files");

        zh.put(ROUTE_BYTECODE_LIST_CLASSES_DESC, "列出代码来源里的类名");
        en.put(ROUTE_BYTECODE_LIST_CLASSES_DESC, "List the class names in a code source");

        zh.put(ROUTE_BYTECODE_FIND_DESC, "按关键词模糊搜索类名（不区分大小写，跨所有代码来源）");
        en.put(ROUTE_BYTECODE_FIND_DESC, "Fuzzy search class names by keyword (case-insensitive, across all code sources)");

        zh.put(PARAM_BYTECODE_INFO_CLASSNAME_DESC, "类名");
        en.put(PARAM_BYTECODE_INFO_CLASSNAME_DESC, "Class name");

        zh.put(PARAM_BYTECODE_INFO_VERBOSE_DESC, "详细输出");
        en.put(PARAM_BYTECODE_INFO_VERBOSE_DESC, "Verbose output");

        zh.put(PARAM_BYTECODE_METHOD_CLASSNAME_DESC, "类名");
        en.put(PARAM_BYTECODE_METHOD_CLASSNAME_DESC, "Class name");

        zh.put(PARAM_BYTECODE_METHOD_METHODNAME_DESC, "方法名");
        en.put(PARAM_BYTECODE_METHOD_METHODNAME_DESC, "Method name");

        zh.put(PARAM_BYTECODE_DUMP_CLASSNAME_DESC, "类名");
        en.put(PARAM_BYTECODE_DUMP_CLASSNAME_DESC, "Class name");

        zh.put(PARAM_BYTECODE_DUMP_OUTPUTPATH_DESC, "输出目录（不填则自动挑一个可写目录）");
        en.put(PARAM_BYTECODE_DUMP_OUTPUTPATH_DESC, "Output directory (a writable one is picked automatically when omitted)");

        zh.put(PARAM_BYTECODE_DUMP_DISASM_DESC, "导出后用设备自带 dexdump 反汇编该类的方法");
        en.put(PARAM_BYTECODE_DUMP_DISASM_DESC, "After exporting, disassemble the class methods with the built-in dexdump");

        zh.put(PARAM_BYTECODE_LOCATE_CLASSNAME_DESC, "类名");
        en.put(PARAM_BYTECODE_LOCATE_CLASSNAME_DESC, "Class name");

        zh.put(PARAM_BYTECODE_ANALYZE_CLASSNAME_DESC, "类名");
        en.put(PARAM_BYTECODE_ANALYZE_CLASSNAME_DESC, "Class name");

        zh.put(PARAM_BYTECODE_ANALYZE_VERBOSE_DESC, "详细输出");
        en.put(PARAM_BYTECODE_ANALYZE_VERBOSE_DESC, "Verbose output");

        zh.put(PARAM_BYTECODE_DISASM_CLASSNAME_DESC, "类名");
        en.put(PARAM_BYTECODE_DISASM_CLASSNAME_DESC, "Class name");

        zh.put(PARAM_BYTECODE_DISASM_METHODNAME_DESC, "只看这一个方法（不填则输出该类的全部方法）");
        en.put(PARAM_BYTECODE_DISASM_METHODNAME_DESC, "Only this method (all methods of the class are dumped when omitted)");

        zh.put(PARAM_BYTECODE_DISASM_OUTPUTPATH_DESC, "把指令写入文件（不填则直接显示，最多 150 行）");
        en.put(PARAM_BYTECODE_DISASM_OUTPUTPATH_DESC, "Write the instructions to a file (printed directly when omitted, up to 150 lines)");

        zh.put(PARAM_BYTECODE_CONSTANTS_CLASSNAME_DESC, "类名");
        en.put(PARAM_BYTECODE_CONSTANTS_CLASSNAME_DESC, "Class name");

        zh.put(PARAM_BYTECODE_VERIFY_CLASSNAME_DESC, "类名");
        en.put(PARAM_BYTECODE_VERIFY_CLASSNAME_DESC, "Class name");

        zh.put(PARAM_BYTECODE_SOURCE_CLASSNAME_DESC, "类名");
        en.put(PARAM_BYTECODE_SOURCE_CLASSNAME_DESC, "Class name");

        zh.put(PARAM_BYTECODE_SOURCE_OUTPUTPATH_DESC, "把反编译结果另存到该文件或目录（不填则只打印）");
        en.put(PARAM_BYTECODE_SOURCE_OUTPUTPATH_DESC, "Also save the decompiled result to this file or directory (printed only when omitted)");

        zh.put(PARAM_BYTECODE_SOURCE_HIGHLIGHT_DESC, "强制语法高亮（忽略规模上限；大段代码在手表上会卡顿）");
        en.put(PARAM_BYTECODE_SOURCE_HIGHLIGHT_DESC, "Force syntax highlighting (ignores the size limit; large listings can lag on a watch)");

        zh.put(PARAM_BYTECODE_BATCH_EXPORT_SOURCE_DESC, "来源文件或目录（目录会递归找 apk/jar/dex/vdex/odex）；不填则导出当前应用的 APK");
        en.put(PARAM_BYTECODE_BATCH_EXPORT_SOURCE_DESC, "Source file or directory (directories are searched recursively for apk/jar/dex/vdex/odex); exports the current app's APK when omitted");

        zh.put(PARAM_BYTECODE_BATCH_EXPORT_OUTPUTPATH_DESC, "输出目录");
        en.put(PARAM_BYTECODE_BATCH_EXPORT_OUTPUTPATH_DESC, "Output directory");

        zh.put(PARAM_BYTECODE_BATCH_EXPORT_LIMIT_DESC, "最多处理多少个来源文件");
        en.put(PARAM_BYTECODE_BATCH_EXPORT_LIMIT_DESC, "Maximum number of source files to process");

        zh.put(PARAM_BYTECODE_LIST_CLASSES_SOURCE_DESC, "指定来源文件（apk/jar/dex/vdex/odex）；不填则列出本进程所有代码来源");
        en.put(PARAM_BYTECODE_LIST_CLASSES_SOURCE_DESC, "Specific source file (apk/jar/dex/vdex/odex); lists every code source in this process when omitted");

        zh.put(PARAM_BYTECODE_LIST_CLASSES_LIMIT_DESC, "最多列出多少个类名");
        en.put(PARAM_BYTECODE_LIST_CLASSES_LIMIT_DESC, "Maximum number of class names to list");

        zh.put(PARAM_BYTECODE_FIND_KEYWORD_DESC, "类名关键词，不区分大小写，匹配类名的任意片段（如 ActivityManager、com.xtc）");
        en.put(PARAM_BYTECODE_FIND_KEYWORD_DESC, "Class name keyword, case-insensitive, matches any fragment of a class name (e.g. ActivityManager, com.xtc)");

        zh.put(PARAM_BYTECODE_FIND_SOURCE_DESC, "只在这个来源里搜（apk/jar/dex/vdex/odex）；不填则搜本进程所有代码来源");
        en.put(PARAM_BYTECODE_FIND_SOURCE_DESC, "Search only this source (apk/jar/dex/vdex/odex); searches every code source in this process when omitted");

        zh.put(PARAM_BYTECODE_FIND_LIMIT_DESC, "最多列出多少个命中");
        en.put(PARAM_BYTECODE_FIND_LIMIT_DESC, "Maximum number of matches to list");
    }
}
