package com.justnothing.methodsclient.tailtip;

import com.justnothing.methodsclient.completer.CommandCompleter;
import com.justnothing.methodsclient.metadata.CommandMetadataScanner;
import com.justnothing.testmodule.command.output.InputMode;
import com.justnothing.testmodule.utils.logging.Logger;

import org.jline.console.CmdDesc;
import org.jline.reader.LineReader;
import org.jline.utils.AttributedString;
import org.jline.utils.AttributedStringBuilder;
import org.jline.widget.TailTipWidgets;

import java.util.*;

/**
 * JLine TailTip 提示管理器。
 * <p>
 * 从命令系统注解元数据自动生成提示内容，无需手动维护。
 * 数据来源：{@link CommandMetadataScanner} 反射扫描 {@code @Cmd} / {@code @CmdRoutes} / {@code @CmdParam}
 * 注解，与服务端命令注册表实时同步。
 */
public class TailTipManager {

    private static CmdDesc desc(String description) {
        List<AttributedString> mainDesc = Collections.singletonList(
                new AttributedString(description)
        );
        return new CmdDesc(mainDesc, Collections.emptyList(), Collections.emptyMap());
    }

    /**
     * 为 Java 脚本模式设置 TailTips（原始功能，保留兼容）
     */
    public static TailTipWidgets setupJavaTailTips(LineReader reader) {
        Map<String, CmdDesc> tailTips = new HashMap<>();
        var res = new TailTipWidgets(reader, tailTips, 0, TailTipWidgets.TipType.COMBINED);
        res.enable();
        return res;
    }

    /**
     * 为 REPL 命令模式设置 TailTips。
     * 从 {@link CommandMetadataScanner} 扫描的元数据自动生成所有提示。
     *
     * @param reader      LineReader 实例
     * @param completer    已初始化的 CommandCompleter（用于获取路由描述）
     * @return 已启用的 TailTipWidgets
     */
    public static TailTipWidgets setupCommandTailTips(LineReader reader, CommandCompleter completer) {
        Map<String, CmdDesc> tailTips = buildCommandTailTips(completer);

        Logger.getLoggerForName("TailTipMgr").info("生成 TailTip 数据: " + tailTips.size() + " 条, keys=" + tailTips.keySet());

        var res = new TailTipWidgets(reader, tailTips, 0, TailTipWidgets.TipType.COMBINED);
        // 注意：不在此处调用 enable()！
        // TailTipWidgets.enable() 内部调用 callWidget()，必须在 JLine readLine 上下文中调用。
        // 调用方（如 ReplClient）应通过 CommandCompleter.firstUseCallback 延迟启用。
        return res;
    }

    /**
     * 无参版本：内部创建 Completer 并扫描（供简单场景使用）
     */
    public static TailTipWidgets setupCommandTailTips(LineReader reader) {
        CommandCompleter completer = new CommandCompleter();
        // 触发扫描以填充数据
        completer.getTopLevelCommands();
        return setupCommandTailTips(reader, completer);
    }

    // ==================== 自动生成 TailTip 映射 ====================

    private static Map<String, CmdDesc> buildCommandTailTips(CommandCompleter completer) {
        Map<String, CmdDesc> tips = new LinkedHashMap<>();

        try {
            // 1. 从扫描器获取完整元数据
            List<CommandMetadataScanner.CommandMeta> commands =
                    CommandMetadataScanner.scanAllCommands();

            for (CommandMetadataScanner.CommandMeta cmd : commands) {
                // 顶层命令提示（包含子路由列表）
                String cmdTip = buildCommandTipWithSubRoutes(cmd);
                tips.put(cmd.name(), desc(cmdTip));

                // 子路由 + 参数提示
                // 注意：RouteMeta.path() 来自 RouteConfig，已经是完整路径（如 "class:info"），
                // 不需要再拼接 cmdName 前缀。
                for (CommandMetadataScanner.RouteMeta route : cmd.routes()) {
                    String routeKey = route.path();

                    // 子路由描述
                    if (!route.description().isEmpty()) {
                        tips.put(routeKey, desc(route.description()));
                    } else {
                        tips.put(routeKey, desc(buildRouteParamSummary(route)));
                    }
                }
            }

            // 2. 参数级提示（从所有 Request 类中提取）
            buildParamTips(commands, tips);

            // 3. 内置通用提示
            buildCommonParamTips(tips);

        } catch (Exception e) {
            // 扫描失败时降级为最小可用集
            buildFallbackTips(tips);
        }

        return tips;
    }

    /**
     * 为每个子路由构建参数摘要提示
     */
    private static String buildCommandTipWithSubRoutes(CommandMetadataScanner.CommandMeta cmd) {
        StringBuilder sb = new StringBuilder();

        // 命令描述
        if (!cmd.description().isEmpty()) {
            sb.append(cmd.description());
        } else {
            sb.append(cmd.name()).append(" 命令");
        }

        // 如果有子路由，列出可用选项
        if (!cmd.routes().isEmpty()) {
            sb.append("\n");
            for (CommandMetadataScanner.RouteMeta route : cmd.routes()) {
                // 取路径最后一段作为显示名（如 "class:info" → "info"）
                String displayName = route.path();
                if (displayName.contains(":")) {
                    displayName = displayName.substring(displayName.lastIndexOf(':') + 1);
                }
                sb.append("  ").append(displayName);

                // 子路由描述
                if (!route.description().isEmpty()) {
                    sb.append("  ").append(route.description());
                }
                sb.append("\n");
            }
        }

        return sb.toString().trim();
    }

    /**
     * 为每个子路由构建参数摘要提示
     */
    private static String buildRouteParamSummary(CommandMetadataScanner.RouteMeta route) {
        StringBuilder sb = new StringBuilder();
        sb.append(route.path());

        if (!route.params().isEmpty()) {
            sb.append(" [");
            boolean first = true;
            for (CommandMetadataScanner.ParamMeta param : route.params()) {
                if (!first) sb.append(" ");
                first = false;

                if (param.required()) {
                    sb.append(param.name());
                } else {
                    sb.append("[").append(param.name()).append("]");
                }
            }
            sb.append("]");
        }

        return sb.toString();
    }

    /**
     * 构建参数级 TailTip（--param → 描述）
     */
    private static void buildParamTips(List<CommandMetadataScanner.CommandMeta> commands,
                                        Map<String, CmdDesc> tips) {
        for (CommandMetadataScanner.CommandMeta cmd : commands) {
            for (CommandMetadataScanner.RouteMeta route : cmd.routes()) {
                for (CommandMetadataScanner.ParamMeta param : route.params()) {
                    String paramDesc = buildParamDescription(param);
                    if (!paramDesc.isEmpty()) {
                        tips.put(param.name(), desc(paramDesc));
                        // 别名也注册
                        for (String alias : param.aliases()) {
                            tips.put(alias, desc(paramDesc));
                        }
                    }
                }
            }
        }
    }

    /**
     * 构建单个参数的描述文本
     */
    private static String buildParamDescription(CommandMetadataScanner.ParamMeta param) {
        StringBuilder sb = new StringBuilder();

        // 名称 + 必填标记
        sb.append(param.name());
        if (param.required()) {
            sb.append(" (必填)");
        }
        if (param.isPositional()) {
            sb.append(" #").append(param.position());
        }
        if (param.varArgs()) {
            sb.append(" ...");
        }

        // 描述
        if (!param.description().isEmpty() && !param.description().equals("...")) {
            sb.append(" — ").append(param.description());
        }

        // 默认值
        if (!param.defaultValue().isEmpty()) {
            sb.append(" [默认: ").append(param.defaultValue()).append("]");
        }

        // 枚举值
        if (!param.allowedValues().isEmpty()) {
            sb.append(" (");
            sb.append(String.join("|", param.allowedValues()));
            sb.append(")");
        }

        return sb.toString();
    }

    /**
     * 通用参数提示（不依赖具体命令的公共选项）
     */
    private static void buildCommonParamTips(Map<String, CmdDesc> tips) {
        tips.put("--help", desc("显示帮助信息"));
        tips.put("-h", desc("显示帮助信息"));
        tips.put("--verbose", desc("详细输出模式"));
        tips.put("-v", desc("详细输出模式"));
        tips.put("--json", desc("以 JSON 格式输出结果"));
        tips.put("--format", desc("指定输出格式"));
        tips.put("--quiet", desc("静默模式"));
        tips.put("--color", desc("彩色输出开关"));
        tips.put("--timeout", desc("超时时间 (毫秒)"));
        tips.put("--depth", desc("递归深度限制"));
        tips.put("--limit", desc("结果数量限制"));
        tips.put("--output", desc("输出文件路径"));
        tips.put("--file", desc("输入/输出文件路径"));
        tips.put("-f", desc("指定输出格式 / 文件路径"));
        tips.put("-t", desc("超时时间"));
        tips.put("-d", desc("深度限制"));
        tips.put("-l", desc("数量限制"));
        tips.put("-o", desc("输出路径"));
        tips.put("-i", desc("交互式 / 指定实例表达式"));
        tips.put("-e", desc("直接求值 / 表达式执行"));
        tips.put("-n", desc("名称 / 编号"));
        tips.put("-s", desc("静态模式 / 静默模式"));
        tips.put("--repl", desc(InputMode.getDescription(InputMode.SCRIPT)));
        tips.put("--eval", desc("直接求值模式"));
        tips.put("--sandbox", desc("沙箱安全模式"));
        tips.put("--name", desc("目标名称"));
        tips.put("--type", desc("类型指定"));
        tips.put("--class", desc("目标类名 (全限定名)"));
        tips.put("-c", desc("目标类名 (全限定名)"));
        tips.put("--method", desc("目标方法名"));
        tips.put("-m", desc("目标方法名"));
        tips.put("--field", desc("目标字段名"));
    }

    /**
     * 降级方案：扫描失败时的最小提示集
     */
    private static void buildFallbackTips(Map<String, CmdDesc> tips) {
        tips.put("class", desc("类信息查询：查看类/方法/字段/继承关系"));
        tips.put("script", desc("脚本引擎：运行自定义脚本代码"));
        tips.put("memory", desc("内存工具：堆分析/GC/对象dump"));
        tips.put("agent", desc("Agent 管理：启动/停止/查询 Agent"));
        tips.put("help", desc("显示帮助文档"));
        tips.put("--help", desc("显示帮助信息"));
        tips.put("-h", desc("显示帮助信息"));
    }
}
