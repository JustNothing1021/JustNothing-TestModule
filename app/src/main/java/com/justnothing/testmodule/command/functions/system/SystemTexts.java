package com.justnothing.testmodule.command.functions.system;

import java.util.Map;

/**
 * system 命令族的 CLI 文案（id 常量 + 中英对照）。
 *
 * <p>命名与 id 规范见 {@link com.justnothing.testmodule.command.framework.i18n.CliTexts}。
 * 本类由 {@code CliTexts} 的静态块登记，新增条目只需在这里加常量 + 两行 put。</p>
 *
 * <p>英文允许缺失（只 put 中文），缺失时英文环境回落显示中文 —— 所以翻译可以一条一条补。</p>
 *
 * <p>该族的路由 {@code path} 为空（子命令即主命令本身），故 id 里省掉空的那一段，
 * 写作 {@code cmd.system.desc} / {@code route.system.desc} / {@code sub.system.*} /
 * {@code param.system.*.desc}。注意 {@code SystemInfoRequest} 这个 request 类虽然放在
 * {@code script.request} 包里，但它只服务于 system 路由，故参数文案也登记在本类。</p>
 */
public final class SystemTexts {

    // ==================== @Cmd（主命令）====================
    public static final String CMD_SYSTEM_DESC = "cmd.system.desc";

    // ==================== @CmdRoutes.Route（命令列表里那一行）====================
    public static final String ROUTE_SYSTEM_DESC = "route.system.desc";

    // ==================== @SubCommandInfo（帮助正文）====================
    public static final String SUB_SYSTEM_DESC = "sub.system.desc";
    public static final String SUB_SYSTEM_OPTIONS = "sub.system.options";

    // ==================== @CmdParam（参数说明）====================
    public static final String PARAM_SYSTEM_CPU_DESC = "param.system.cpu.desc";
    public static final String PARAM_SYSTEM_MEMORY_DESC = "param.system.memory.desc";
    public static final String PARAM_SYSTEM_OS_DESC = "param.system.os.desc";
    public static final String PARAM_SYSTEM_PROPS_DESC = "param.system.props.desc";
    public static final String PARAM_SYSTEM_ALL_DESC = "param.system.all.desc";

    private SystemTexts() {
    }

    /**
     * 由 {@code CliTexts} 的静态块调用。必须是 public —— 它在另一个包里。
     */
    public static void register(Map<String, String> zh, Map<String, String> en) {
        zh.put(CMD_SYSTEM_DESC, "显示系统信息 (CPU, 内存, OS, 属性)");
        en.put(CMD_SYSTEM_DESC, "Show system information (CPU, memory, OS, properties)");

        zh.put(ROUTE_SYSTEM_DESC, "显示系统信息");
        en.put(ROUTE_SYSTEM_DESC, "Show system information");

        zh.put(SUB_SYSTEM_DESC, "显示系统信息, 包括操作系统、Android、CPU、内存等");
        en.put(SUB_SYSTEM_DESC, "Show system information, including OS, Android, CPU and memory");

        // optionsDesc 是多行文本块，整块一个 id（含缩进，逐字节照抄原注解里的内容）
        zh.put(SUB_SYSTEM_OPTIONS, """
                选项:
                  --cpu     - 只显示CPU信息
                  --memory  - 只显示内存信息
                  --os      - 只显示操作系统信息
                  --props   - 只显示系统属性
                """);
        en.put(SUB_SYSTEM_OPTIONS, """
                Options:
                  --cpu     - Show CPU information only
                  --memory  - Show memory information only
                  --os      - Show OS information only
                  --props   - Show system properties only
                """);

        zh.put(PARAM_SYSTEM_CPU_DESC, "显示CPU信息");
        en.put(PARAM_SYSTEM_CPU_DESC, "Show CPU information");

        zh.put(PARAM_SYSTEM_MEMORY_DESC, "显示内存信息");
        en.put(PARAM_SYSTEM_MEMORY_DESC, "Show memory information");

        zh.put(PARAM_SYSTEM_OS_DESC, "显示操作系统信息");
        en.put(PARAM_SYSTEM_OS_DESC, "Show OS information");

        zh.put(PARAM_SYSTEM_PROPS_DESC, "显示系统属性");
        en.put(PARAM_SYSTEM_PROPS_DESC, "Show system properties");

        zh.put(PARAM_SYSTEM_ALL_DESC, "显示所有信息（默认）");
        en.put(PARAM_SYSTEM_ALL_DESC, "Show all information (default)");
    }
}
