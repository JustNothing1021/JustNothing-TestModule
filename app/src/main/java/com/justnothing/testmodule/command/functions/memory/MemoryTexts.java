package com.justnothing.testmodule.command.functions.memory;

import com.justnothing.testmodule.command.framework.i18n.Text;

import java.util.Map;

/**
 * memory 命令族的 CLI 文案（id 常量 + 中英对照）。
 *
 * <p>命名与 id 规范见 {@link com.justnothing.testmodule.command.framework.i18n.CliTexts}。
 * 本类由 {@code CliTexts} 的静态块登记，新增条目只需在这里加常量 + 两行 put。</p>
 *
 * <p>英文允许缺失（只 put 中文），缺失时英文环境回落显示中文 —— 所以翻译可以一条一条补。</p>
 */
public final class MemoryTexts {

    // ==================== @Cmd（主命令）====================
    public static final String CMD_MEMORY_DESC = "cmd.memory.desc";

    // ==================== @CmdRoutes.Route（命令列表里那一行）====================
    public static final String ROUTE_MEMORY_INFO_DESC = "route.memory.info.desc";
    public static final String ROUTE_MEMORY_GC_DESC = "route.memory.gc.desc";
    public static final String ROUTE_MEMORY_DUMP_DESC = "route.memory.dump.desc";

    // ==================== @SubCommandInfo（帮助正文）====================
    public static final String SUB_MEMORY_INFO_DESC = "sub.memory.info.desc";
    public static final String SUB_MEMORY_INFO_OPTIONS = "sub.memory.info.options";
    public static final String SUB_MEMORY_GC_DESC = "sub.memory.gc.desc";
    public static final String SUB_MEMORY_GC_OPTIONS = "sub.memory.gc.options";
    public static final String SUB_MEMORY_DUMP_DESC = "sub.memory.dump.desc";
    public static final String SUB_MEMORY_DUMP_OPTIONS = "sub.memory.dump.options";

    // ==================== @CmdParam（参数说明）====================
    public static final String PARAM_MEMORY_INFO_DETAIL_LEVEL_DESC = "param.memory.info.detail-level.desc";
    public static final String PARAM_MEMORY_INFO_HEAP_DESC = "param.memory.info.heap.desc";
    public static final String PARAM_MEMORY_INFO_DETAILED_DESC = "param.memory.info.detailed.desc";
    public static final String PARAM_MEMORY_GC_FULL_DESC = "param.memory.gc.full.desc";
    public static final String PARAM_MEMORY_GC_STATS_DESC = "param.memory.gc.stats.desc";
    public static final String PARAM_MEMORY_DUMP_HEAP_DESC = "param.memory.dump.heap.desc";
    public static final String PARAM_MEMORY_DUMP_THREADS_DESC = "param.memory.dump.threads.desc";
    public static final String PARAM_MEMORY_DUMP_FULL_DESC = "param.memory.dump.full.desc";
    public static final String PARAM_MEMORY_DUMP_FILE_PATH_DESC = "param.memory.dump.filePath.desc";

    // ==================== 输出文案 ====================
    // 判据只看「在本族里出现了两次以上」；只用一次的就地写 Text.zhEn。
    // 同一批内存标签有两种形态：AbstractMemoryCommand / DumpCommand 的是「两空格缩进 + 标签」，
    // InfoCommand 的是「无缩进 + 标签」；中文并不逐字相同，所以两套各留一份（LABEL_INDENTED_* / 其余）。
    public static final Text LABEL_INDENTED_MAX = Text.zhEn("  最大: ", "  Max: ");
    public static final Text LABEL_INDENTED_ALLOCATED = Text.zhEn("  已分配: ", "  Allocated: ");
    public static final Text LABEL_INDENTED_USED = Text.zhEn("  已用: ", "  Used: ");
    public static final Text LABEL_INDENTED_FREE = Text.zhEn("  空闲: ", "  Free: ");
    public static final Text LABEL_INDENTED_AVAILABLE = Text.zhEn("  可用: ", "  Available: ");
    public static final Text LABEL_INDENTED_TOTAL = Text.zhEn("  总计: ", "  Total: ");
    public static final Text LABEL_INDENTED_LOW_MEMORY = Text.zhEn("  低内存: ", "  Low memory: ");

    public static final Text LABEL_NATIVE_ALLOCATED = Text.zhEn("已分配: ", "Allocated: ");
    public static final Text LABEL_NATIVE_USED = Text.zhEn("已用: ", "Used: ");
    public static final Text LABEL_NATIVE_FREE = Text.zhEn("空闲: ", "Free: ");
    public static final Text LABEL_JAVA_MAX = Text.zhEn("最大内存: ", "Max memory: ");
    public static final Text LABEL_JAVA_ALLOCATED = Text.zhEn("已分配内存: ", "Allocated memory: ");
    public static final Text LABEL_JAVA_USED = Text.zhEn("已用内存: ", "Used memory: ");
    public static final Text LABEL_JAVA_FREE = Text.zhEn("空闲内存: ", "Free memory: ");
    public static final Text LABEL_SYSTEM_MEMORY = Text.zhEn("系统内存:", "System memory:");
    public static final Text LABEL_OS = Text.zhEn("操作系统: ", "OS: ");
    public static final Text LABEL_OS_VERSION = Text.zhEn("系统版本: ", "OS version: ");
    public static final Text LABEL_ARCH = Text.zhEn("架构: ", "Architecture: ");
    public static final Text LABEL_TIME = Text.zhEn("时间: ", "Time: ");
    public static final Text LABEL_THREAD_COUNT = Text.zhEn("线程总数: ", "Total threads: ");
    public static final Text LABEL_THREAD = Text.zhEn("线程: ", "Thread: ");
    public static final Text LABEL_THREAD_STATE = Text.zhEn("  状态: ", "  State: ");

    public static final Text SECTION_NATIVE_HEAP = Text.zhEn("===== 原生堆内存 =====", "===== Native heap memory =====");
    public static final Text SECTION_JAVA_RUNTIME = Text.zhEn("===== Java运行时内存 =====", "===== Java runtime memory =====");

    public static final Text VALUE_YES = Text.zhEn("是", "yes");
    public static final Text VALUE_NO = Text.zhEn("否", "no");
    /** 「无法读取 /proc/meminfo: 」+ 异常消息，AbstractMemoryCommand 里彩色输出和拼字符串两处都在用。 */
    public static final Text READ_MEMINFO_FAILED = Text.zhEn("无法读取 /proc/meminfo: ", "Failed to read /proc/meminfo: ");
    public static final Text UNKNOWN_ERROR = Text.zhEn("未知错误", "Unknown error");

    private MemoryTexts() {
    }

    /**
     * 由 {@code CliTexts} 的静态块调用。必须是 public —— 它在另一个包里。
     * 命名上刻意带 register 而不是「构造时自己注册」：登记动作集中在 CliTexts 一处，
     * 「哪些族登记了」才看得全，漏登记也能被守卫测试发现。
     */
    public static void register(Map<String, String> zh, Map<String, String> en) {
        zh.put(CMD_MEMORY_DESC, "内存调试和管理工具, 包括内存信息查询、GC、堆转储等功能");
        en.put(CMD_MEMORY_DESC, "Memory debugging and management: info, GC and heap dumps");

        zh.put(ROUTE_MEMORY_INFO_DESC, "显示详细的内存使用情况");
        en.put(ROUTE_MEMORY_INFO_DESC, "Show detailed memory usage");

        zh.put(ROUTE_MEMORY_GC_DESC, "手动触发垃圾回收");
        en.put(ROUTE_MEMORY_GC_DESC, "Trigger garbage collection manually");

        zh.put(ROUTE_MEMORY_DUMP_DESC, "导出堆信息和系统状态");
        en.put(ROUTE_MEMORY_DUMP_DESC, "Dump heap information and system state");

        zh.put(SUB_MEMORY_GC_DESC, "手动触发垃圾回收, 可选完整GC或显示统计信息");
        en.put(SUB_MEMORY_GC_DESC, "Trigger garbage collection manually, optionally with a full GC or statistics");

        // optionsDesc 是多行文本块，整块一个 id（含缩进，逐字节照抄原注解里的内容）
        zh.put(SUB_MEMORY_GC_OPTIONS, """
                选项:
                  --full    - 执行完整的GC (建议开启)
                  --stats   - 显示GC统计信息
                """);
        en.put(SUB_MEMORY_GC_OPTIONS, """
                Options:
                  --full    - Run a full GC (recommended)
                  --stats   - Show GC statistics
                """);

        zh.put(PARAM_MEMORY_GC_FULL_DESC, "执行完整的GC");
        en.put(PARAM_MEMORY_GC_FULL_DESC, "Run a full GC");

        zh.put(PARAM_MEMORY_GC_STATS_DESC, "显示GC统计信息");
        en.put(PARAM_MEMORY_GC_STATS_DESC, "Show GC statistics");

        zh.put(SUB_MEMORY_INFO_DESC, "显示详细的内存使用情况, 包括Java堆、原生堆、进程内存等");
        en.put(SUB_MEMORY_INFO_DESC, "Show detailed memory usage, including the Java heap, native heap and process memory");

        // optionsDesc 与 GcCommand 一样：整块一个 id（含缩进，逐字节照抄原注解）
        zh.put(SUB_MEMORY_INFO_OPTIONS, """
                选项:
                  -h, --heap       只显示堆内存信息
                  -d, --detailed   显示详细内存信息 (默认)
                """);
        en.put(SUB_MEMORY_INFO_OPTIONS, """
                Options:
                  -h, --heap       Show heap memory information only
                  -d, --detailed   Show detailed memory information (default)
                """);

        zh.put(PARAM_MEMORY_INFO_DETAIL_LEVEL_DESC, "信息详细程度");
        en.put(PARAM_MEMORY_INFO_DETAIL_LEVEL_DESC, "Level of detail");

        zh.put(PARAM_MEMORY_INFO_HEAP_DESC, "只显示堆内存信息");
        en.put(PARAM_MEMORY_INFO_HEAP_DESC, "Show heap memory information only");

        zh.put(PARAM_MEMORY_INFO_DETAILED_DESC, "显示详细内存信息 (默认)");
        en.put(PARAM_MEMORY_INFO_DETAILED_DESC, "Show detailed memory information (default)");

        zh.put(PARAM_MEMORY_DUMP_HEAP_DESC, "只导出堆信息");
        en.put(PARAM_MEMORY_DUMP_HEAP_DESC, "Export heap information only");

        zh.put(PARAM_MEMORY_DUMP_THREADS_DESC, "只导出线程信息");
        en.put(PARAM_MEMORY_DUMP_THREADS_DESC, "Export thread information only");

        zh.put(PARAM_MEMORY_DUMP_FULL_DESC, "导出完整信息 (默认)");
        en.put(PARAM_MEMORY_DUMP_FULL_DESC, "Export everything (default)");

        zh.put(PARAM_MEMORY_DUMP_FILE_PATH_DESC, "输出文件路径");
        en.put(PARAM_MEMORY_DUMP_FILE_PATH_DESC, "Output file path");

        // 注意：dump 这里的文案是「重建」的，不是原稿 —— DumpCommand.java 的中文在仓库初始提交里
        // 就已经整体损坏成字面量 '?'（整个文件 0 个非 ASCII 字节），且仓库只有那一个 commit，
        // 编译产物里的 .class 也一样没有中文，无处可恢复。以下按同族 AbstractMemoryCommand 的
        // 彩色输出版逐条镜像（`=== 堆内存信息 ===` / `Java运行时内存:` / `最大:` `已分配:` `空闲:` `已用:`
        // 等是逐字相同的），少数没有镜像可依的（本族描述、选项块里的分区标题、导出路径相关的几条）
        // 是按语义补写的。若日后拿到原稿，直接改这里的值即可，id 不用动。
        zh.put(SUB_MEMORY_DUMP_DESC, "导出堆内存、线程和系统信息, 可写入文件或直接显示在控制台");
        en.put(SUB_MEMORY_DUMP_DESC, "Export heap, thread and system information, to a file or straight to the console");

        zh.put(SUB_MEMORY_DUMP_OPTIONS, """
                选项:
                  --heap            - 只导出堆信息
                  --threads         - 只导出线程信息
                  --full            - 导出完整信息 (默认)

                参数:
                  file              - 输出文件路径 (不指定则直接打印到控制台)
                """);
        en.put(SUB_MEMORY_DUMP_OPTIONS, """
                Options:
                  --heap            - Export heap information only
                  --threads         - Export thread information only
                  --full            - Export everything (default)

                Arguments:
                  file              - Output file path (prints to the console if omitted)
                """);
    }
}
