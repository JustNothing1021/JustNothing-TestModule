package com.justnothing.testmodule.command.framework.utils;

import com.justnothing.testmodule.command.framework.annotation.Cmd;
import com.justnothing.testmodule.command.framework.annotation.CmdParam;
import com.justnothing.testmodule.command.framework.annotation.CmdRoutes;
import com.justnothing.testmodule.command.framework.annotation.SubCommandInfo;
import com.justnothing.testmodule.command.framework.i18n.CliMessages;
import com.justnothing.testmodule.command.framework.i18n.CliTexts;
import com.justnothing.testmodule.command.framework.model.CommandRouter;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 帮助文档生成。
 *
 * <p>注解里的文字元素（{@code description} / {@code helpText} / {@code optionsDesc} / 参数说明）
 * 存的是<b>文案 id</b>而不是原文，所以这里每取一处都要过 {@link CliTexts#resolve(String)}。
 * {@code resolve} 对「已经是原文的旧写法」是透明的（查不到就原样返回），
 * 所以没迁移完的命令不会因此显示异常 —— 迁移可以一个族一个族地做。</p>
 *
 * <p>{@code usage} / {@code examples} / {@code seeAlso} 不走 resolve：
 * 它们是命令语法和命令名，不随语言变。</p>
 */
public class CmdHelpGenerator {

    public static String generateHelpText(Class<?> cmdClass) {
        StringBuilder sb = new StringBuilder();

        Cmd cmdAnnotation = cmdClass.getAnnotation(Cmd.class);
        if (cmdAnnotation != null) {
            sb.append(cmdAnnotation.name()).append(" - ")
              .append(CliTexts.resolve(cmdAnnotation.description())).append("\n\n");

            if (!cmdAnnotation.helpText().isEmpty()) {
                sb.append(CliTexts.resolve(cmdAnnotation.helpText())).append("\n\n");
            }
        }

        CmdRoutes routesAnnotation = cmdClass.getAnnotation(CmdRoutes.class);
        if (routesAnnotation != null) {
            Map<String, List<CmdRoutes.Route>> categories = new LinkedHashMap<>();
            List<CmdRoutes.Route> flatRoutes = new ArrayList<>();

            for (CmdRoutes.Route route : routesAnnotation.value()) {
                String[] parts = route.path().split("/");
                if (parts.length >= 2) {
                    String category = parts[0];
                    categories.computeIfAbsent(category, k -> new ArrayList<>()).add(route);
                } else {
                    flatRoutes.add(route);
                }
            }

            // ========== 子命令（带分类前缀 + 内联位置参数）==========
            sb.append(CliMessages.HELP_SUBCOMMANDS.text());
            for (Map.Entry<String, List<CmdRoutes.Route>> entry : categories.entrySet()) {
                String category = entry.getKey();
                sb.append(String.format("  %s:\n", category));
                for (CmdRoutes.Route route : entry.getValue()) {
                    String sig = buildRouteSignature(route);
                    sb.append("    ").append(padColumn(sig, 37))
                      .append(CliTexts.resolve(route.description())).append("\n");
                }
                sb.append("\n");
            }
            for (CmdRoutes.Route route : flatRoutes) {
                String sig = buildRouteSignature(route);
                sb.append("  ").append(padColumn(sig, 37))
                  .append(CliTexts.resolve(route.description())).append("\n");
            }
            if (!flatRoutes.isEmpty()) sb.append("\n");

            // ========== 参数详情（逐子命令列出所有参数描述和约束）==========
            sb.append(CliMessages.HELP_PARAM_DETAILS.text());
            boolean hasAnyDetails = false;

            for (Map.Entry<String, List<CmdRoutes.Route>> entry : categories.entrySet()) {
                String category = entry.getKey();

                for (CmdRoutes.Route route : entry.getValue()) {
                    String action = route.path().substring(category.length() + 1);
                    List<CmdParamProcessor.FieldInfo> allParams = CmdParamProcessor.getCmdParamFields(route.request());

                    if (!allParams.isEmpty()) {
                        sb.append(String.format("  %s/%s:\n", category, action));
                        hasAnyDetails = true;

                        for (CmdParamProcessor.FieldInfo fi : allParams) {
                            sb.append("    ").append(formatParamDetail(fi)).append("\n");
                        }
                        sb.append("\n");
                    }
                }
            }

            for (CmdRoutes.Route route : flatRoutes) {
                List<CmdParamProcessor.FieldInfo> allParams = CmdParamProcessor.getCmdParamFields(route.request());

                if (!allParams.isEmpty()) {
                    sb.append(String.format("  %s:\n", route.path()));
                    hasAnyDetails = true;

                    for (CmdParamProcessor.FieldInfo fi : allParams) {
                        sb.append("    ").append(formatParamDetail(fi)).append("\n");
                    }
                    sb.append("\n");
                }
            }

            if (!hasAnyDetails) {
                sb.append(CliMessages.HELP_NO_PARAMS.text());
            }
        } else if (cmdAnnotation == null) {
            // 兼容：对于没有 @Cmd/@CmdRoutes 的 Request 类，直接显示其参数
            List<CmdParamProcessor.FieldInfo> params = CmdParamProcessor.getCmdParamFields(cmdClass);
            if (!params.isEmpty()) {
                String className = cmdClass.getSimpleName();
                sb.append(CliMessages.HELP_CLASS_PARAMS.format(className));

                for (CmdParamProcessor.FieldInfo fi : params) {
                    sb.append("  ").append(formatParamHelp(fi)).append("\n");
                }

                sb.append("\n");
            }
        }

        return sb.toString();
    }

    private static String formatParamDetail(CmdParamProcessor.FieldInfo fi) {
        CmdParam p = fi.param();
        StringBuilder sb = new StringBuilder();

        String nameStr = p.name();
        if (p.aliases().length > 0) {
            nameStr += ", " + String.join(", ", p.aliases());
        }
        sb.append("  ").append(padRight(nameStr, 22));

        sb.append("  ").append(padColumn(CliTexts.resolve(p.description()), 30));

        List<String> attrs = new ArrayList<>();
        attrs.add(p.required() ? CliMessages.PARAM_REQUIRED.text() : CliMessages.PARAM_OPTIONAL.text());

        String typeName = inferTypeName(fi.field().getType());
        attrs.add(typeName);

        if (!p.required()) {
            String defVal = p.defaultValue();
            if (!defVal.isEmpty()) {
                attrs.add(CliMessages.PARAM_DEFAULT_PREFIX.text() + defVal);
            }
        }

        if (p.varArgs()) {
            attrs.add(CliMessages.PARAM_VARARGS.text());
        }

        if (p.min() > Double.NEGATIVE_INFINITY || p.max() < Double.POSITIVE_INFINITY) {
            attrs.add(CliMessages.PARAM_RANGE_PREFIX.text() + formatRange(p.min(), p.max()));
        }

        if (p.allowedValues().length > 0) {
            attrs.add(CliMessages.PARAM_ENUM_PREFIX.text() + String.join("|", p.allowedValues()) + "}");
        }

        if (p.isOperator()) {
            // "(N args)" 里的 args 本来就是英文缩写，不随语言变，所以留在代码里
            String opStr = CliMessages.PARAM_OPERATOR.text();
            if (p.operatorArgs() > 0) opStr += "(" + p.operatorArgs() + " args)";
            attrs.add(opStr);
        }

        if (p.readMode() != CmdParam.ReadMode.STRIPPED) {
            attrs.add("mode=" + p.readMode().name());
        }

        sb.append(String.join(" | ", attrs));

        return sb.toString();
    }

    private static int displayWidth(String s) {
        int w = 0;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            w += (c >= '\u4e00' && c <= '\u9fff') || (c >= '\u3000' && c <= '\u303f')
                    || (c >= '\uff00' && c <= '\uffef') ? 2 : 1;
        }
        return w;
    }

    private static String padRight(String s, int targetWidth) {
        int pad = targetWidth - displayWidth(s);
        StringBuilder sb = new StringBuilder(s);
        for (int i = 0; i < pad; i++) sb.append(' ');
        return sb.toString();
    }

    /**
     * 用来排多列的行：右侧补空格到目标宽度，但即使内容本身已经超宽也至少留一个空格。
     *
     * <p>{@link #padRight} 在超宽时一格都不补，两列就会粘成一个词（英文描述普遍比中文长，
     * i18n 之后才露出来，比如 {@code Number of rows in the CPU TOP listoptional}）。</p>
     */
    private static String padColumn(String s, int targetWidth) {
        return displayWidth(s) >= targetWidth ? s + " " : padRight(s, targetWidth);
    }

    private static String inferTypeName(Class<?> type) {
        if (type == int.class || type == Integer.class) return CliMessages.TYPE_INT.text();
        if (type == long.class || type == Long.class) return CliMessages.TYPE_LONG.text();
        if (type == double.class || type == Double.class) return CliMessages.TYPE_DOUBLE.text();
        if (type == float.class || type == Float.class) return CliMessages.TYPE_DOUBLE.text();
        if (type == boolean.class || type == Boolean.class) return CliMessages.TYPE_BOOLEAN.text();
        if (type == String.class) return CliMessages.TYPE_STRING.text();
        if (type.isArray()) return inferTypeName(type.getComponentType()) + CliMessages.TYPE_LIST.text();
        if (List.class.isAssignableFrom(type)) return CliMessages.TYPE_LIST.text();
        return type.getSimpleName();
    }

    private static String formatRange(double min, double max) {
        String minStr = (min == Double.NEGATIVE_INFINITY) ? "-inf"
                : (min == (long) min) ? String.valueOf((long) min) : String.valueOf(min);
        String maxStr = (max == Double.POSITIVE_INFINITY) ? "+inf"
                : (max == (long) max) ? String.valueOf((long) max) : String.valueOf(max);
        return minStr + "~" + maxStr;
    }

    /**
     *  生成指定子命令的帮助文档（增强版：集成 @SubCommandInfo 信息）
     */
    public static String generateHelpForRoute(Class<?> cmdClass, CommandRouter.RouteConfig routeConfig) {
        StringBuilder sb = new StringBuilder();

        Cmd cmdAnnotation = cmdClass.getAnnotation(Cmd.class);
        if (cmdAnnotation == null) return CliMessages.HELP_NO_ANNOTATION.text();
        String name = cmdAnnotation.name();
        String fullPath = routeConfig.path();
        // 路由 key 统一用 "/" 分层（如 class/info）；path 为空的路由其 key 就等于命令名。
        String subCommandPath;
        if (fullPath.equals(name)) {
            subCommandPath = "";
        } else if (fullPath.startsWith(name + "/")) {
            subCommandPath = fullPath.substring(name.length() + 1);
        } else {
            subCommandPath = fullPath;
        }

        // 1. 标题行
        sb.append(name);
        if (!subCommandPath.isEmpty()) {
            sb.append(" ").append(subCommandPath);
        }

        // 尝试从 handler 类获取 @SubCommandInfo 注解
        SubCommandInfo subCmdInfo = routeConfig.handlerType().getAnnotation(SubCommandInfo.class);
        if (subCmdInfo == null) {
            subCmdInfo = routeConfig.requestType().getAnnotation(SubCommandInfo.class);
        }

        // 注解里没写 description 时回落到路由注册的那一行（@SubCommandInfo 的默认值已改成空串，
        // 所以「没写」和「写了空」是同一件事，不会再出现「这个命令没有描述信息...」这种占位串）
        String description = subCmdInfo != null
                ? CliTexts.resolve(subCmdInfo.description())
                : CliTexts.resolve(routeConfig.description());
        if (description.isEmpty() && subCmdInfo != null) {
            description = CliTexts.resolve(routeConfig.description());
        }
        sb.append(" - ").append(description).append("\n\n");

        // 2. 用法说明（优先使用 @SubCommandInfo.usage）
        //    用法多数是纯命令语法（memory gc [options]），不含中文，所以 resolve 通常原样返回；
        //    万一某条用法里写了中文，把它拆成 id 也能直接被这里解析，不需要改这一行。
        String usage = subCmdInfo != null ? CliTexts.resolve(subCmdInfo.usage()) : "";
        if (!usage.isEmpty()) {
            sb.append(CliMessages.HELP_USAGE.text());
            sb.append("  ").append(usage).append("\n\n");
        } else {
            // 自动生成用法
            sb.append(CliMessages.HELP_USAGE.text());
            sb.append("  ").append(cmdAnnotation.name()).append(" ").append(subCommandPath);

            List<CmdParamProcessor.FieldInfo> routeParams = CmdParamProcessor.getCmdParamFields(routeConfig.requestType());
            List<CmdParamProcessor.FieldInfo> positionalParams = routeParams.stream()
                    .filter(fi -> fi.param().position() > 0)
                    .sorted(Comparator.comparingInt(a -> a.param().position()))
                    .collect(Collectors.toList());

            for (CmdParamProcessor.FieldInfo fi : positionalParams) {
                sb.append(" <").append(CliTexts.resolve(fi.param().description())).append(">");
            }

            List<CmdParamProcessor.FieldInfo> optionalParams = routeParams.stream()
                .filter(fi -> !fi.param().required() && fi.param().position() == 0)
                .collect(Collectors.toList());

            if (!optionalParams.isEmpty()) {
                sb.append(CliMessages.HELP_OPTIONS_SUFFIX.text());
            }

            sb.append("\n\n");
        }

        // 3. 实际示例（来自 @SubCommandInfo.examples。示例是命令语法，不随语言变，不走 resolve）
        if (subCmdInfo != null && subCmdInfo.examples().length > 0) {
            sb.append(CliMessages.HELP_EXAMPLES.text());
            for (String example : subCmdInfo.examples()) {
                sb.append("  ").append(example).append("\n");
            }
            sb.append("\n");
        }

        // 4. 参数列表（始终显示）
        List<CmdParamProcessor.FieldInfo> routeParams = CmdParamProcessor.getCmdParamFields(routeConfig.requestType());
        if (!routeParams.isEmpty()) {
            sb.append(CliMessages.HELP_OPTIONS.text());

            for (CmdParamProcessor.FieldInfo fi : routeParams) {
                sb.append("  ").append(formatParamHelp(fi)).append("\n");
            }

            sb.append("\n");
        }

        // 5. 选项详细说明（来自 @SubCommandInfo.optionsDesc）
        //    以前这里靠 startsWith("这个命令没有帮助信息") 判断「注解没写」，
        //    那是拿中文内容当标记用；现在默认值是空串，「没写」就是 isEmpty()。
        String optionsDesc = subCmdInfo != null ? CliTexts.resolve(subCmdInfo.optionsDesc()) : "";
        if (!optionsDesc.isEmpty()) {
            sb.append(CliMessages.HELP_OPTION_DETAILS.text());
            // 缩进每一行
            for (String line : optionsDesc.trim().split("\n")) {
                if (!line.trim().isEmpty()) {
                    sb.append("  ").append(line.trim()).append("\n");
                }
            }
            sb.append("\n");
        }

        // 6. 相关命令（来自 @SubCommandInfo.seeAlso。命令名不随语言变，不走 resolve）
        if (subCmdInfo != null && subCmdInfo.seeAlso().length > 0) {
            sb.append(CliMessages.HELP_SEE_ALSO.text());
            for (String related : subCmdInfo.seeAlso()) {
                sb.append("  ").append(related).append("\n");
            }
            sb.append("\n");
        }

        return sb.toString();
    }

    private static String buildRouteSignature(CmdRoutes.Route route) {
        List<CmdParamProcessor.FieldInfo> allParams = CmdParamProcessor.getCmdParamFields(route.request());

        List<CmdParamProcessor.FieldInfo> positional = allParams.stream()
                .filter(fi -> fi.param().position() > 0)
                .sorted(Comparator.comparingInt(a -> a.param().position()))
                .collect(Collectors.toList());

        boolean hasOptions = allParams.stream().anyMatch(fi -> fi.param().position() <= 0);

        StringBuilder sig = new StringBuilder();
        String[] parts = route.path().split("/");
        sig.append(parts[parts.length - 1]);

        for (CmdParamProcessor.FieldInfo fi : positional) {
            String name = fi.param().name();
            if (fi.param().required()) {
                sig.append(" <").append(name).append(">");
            } else {
                String defVal = fi.param().defaultValue();
                if (!defVal.isEmpty()) {
                    sig.append(" [").append(name).append("=").append(defVal).append("]");
                } else {
                    sig.append(" [").append(name).append("]");
                }
            }
        }

        if (hasOptions) {
            sig.append(" [options...]");
        }

        return sig.toString();
    }

    private static String formatParamHelp(CmdParamProcessor.FieldInfo fi) {
        CmdParam p = fi.param();
        StringBuilder sb = new StringBuilder();

        sb.append("  ").append(padRight(p.name(), 20));

        if (p.aliases().length > 0) {
            sb.append(", ").append(String.join(", ", p.aliases()));
        }

        sb.append("  ").append(CliTexts.resolve(p.description()));

        if (!p.required()) {
            String defVal = p.defaultValue();
            if (!defVal.isEmpty()) {
                sb.append(CliMessages.PARAM_DEFAULT_INLINE.text()).append(defVal).append(")");
            }
        }

        if (p.min() > Double.NEGATIVE_INFINITY || p.max() < Double.POSITIVE_INFINITY) {
            sb.append(" [").append(p.min()).append("-").append(p.max()).append("]");
        }

        if (p.allowedValues().length > 0) {
            sb.append(" {").append(String.join("|", p.allowedValues())).append("}");
        }

        // 显示引号处理模式（非默认模式时）
        if (p.readMode() != CmdParam.ReadMode.STRIPPED) {
            sb.append(" [mode=").append(p.readMode().name()).append("]");
        }

        // 显示操作符信息（isOperator 时）
        if (p.isOperator()) {
            sb.append(CliMessages.PARAM_OPERATOR_INLINE.text());
            if (p.operatorArgs() > 0) {
                sb.append(CliMessages.PARAM_OPERATOR_CONSUMES_PREFIX.text())
                  .append(p.operatorArgs())
                  .append(CliMessages.PARAM_OPERATOR_CONSUMES_SUFFIX.text());
            }
            sb.append("]");
        }

        // 显示互斥约束
        if (p.mutexWith().length > 0) {
            sb.append(CliMessages.PARAM_MUTEX_PREFIX.text())
              .append(String.join(", ", p.mutexWith())).append("]");
        }

        return sb.toString();
    }
}
