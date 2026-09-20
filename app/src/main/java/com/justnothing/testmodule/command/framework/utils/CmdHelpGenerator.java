package com.justnothing.testmodule.command.framework.utils;

import com.justnothing.testmodule.command.framework.annotation.Cmd;
import com.justnothing.testmodule.command.framework.annotation.CmdParam;
import com.justnothing.testmodule.command.framework.annotation.CmdRoutes;
import com.justnothing.testmodule.command.framework.annotation.SubCommandInfo;
import com.justnothing.testmodule.command.framework.model.CommandRouter;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 帮助文档生成。
 */
public class CmdHelpGenerator {

    public static String generateHelpText(Class<?> cmdClass) {
        StringBuilder sb = new StringBuilder();

        Cmd cmdAnnotation = cmdClass.getAnnotation(Cmd.class);
        if (cmdAnnotation != null) {
            sb.append(cmdAnnotation.name()).append(" - ").append(cmdAnnotation.description()).append("\n\n");

            if (!cmdAnnotation.helpText().isEmpty()) {
                sb.append(cmdAnnotation.helpText()).append("\n\n");
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
            sb.append("子命令:\n");
            for (Map.Entry<String, List<CmdRoutes.Route>> entry : categories.entrySet()) {
                String category = entry.getKey();
                sb.append(String.format("  %s:\n", category));
                for (CmdRoutes.Route route : entry.getValue()) {
                    String sig = buildRouteSignature(route);
                    sb.append("    ").append(padRight(sig, 37)).append(route.description()).append("\n");
                }
                sb.append("\n");
            }
            for (CmdRoutes.Route route : flatRoutes) {
                String sig = buildRouteSignature(route);
                sb.append("  ").append(padRight(sig, 37)).append(route.description()).append("\n");
            }
            if (!flatRoutes.isEmpty()) sb.append("\n");

            // ========== 参数详情（逐子命令列出所有参数描述和约束）==========
            sb.append("参数详情:\n");
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
                sb.append("  (所有子命令暂无参数)\n\n");
            }
        } else if (cmdAnnotation == null) {
            // 兼容：对于没有 @Cmd/@CmdRoutes 的 Request 类，直接显示其参数
            List<CmdParamProcessor.FieldInfo> params = CmdParamProcessor.getCmdParamFields(cmdClass);
            if (!params.isEmpty()) {
                String className = cmdClass.getSimpleName();
                sb.append(className).append(" 参数:\n\n");

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

        sb.append("  ").append(padRight(p.description(), 30));

        List<String> attrs = new ArrayList<>();
        attrs.add(p.required() ? "必需" : "可选");

        String typeName = inferTypeName(fi.field().getType());
        attrs.add(typeName);

        if (!p.required()) {
            String defVal = p.defaultValue();
            if (!defVal.isEmpty()) {
                attrs.add("默认=" + defVal);
            }
        }

        if (p.varArgs()) {
            attrs.add("可变参数");
        }

        if (p.min() > Double.NEGATIVE_INFINITY || p.max() < Double.POSITIVE_INFINITY) {
            attrs.add("范围: " + formatRange(p.min(), p.max()));
        }

        if (p.allowedValues().length > 0) {
            attrs.add("枚举: {" + String.join("|", p.allowedValues()) + "}");
        }

        if (p.isOperator()) {
            String opStr = "操作符";
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

    private static String inferTypeName(Class<?> type) {
        if (type == int.class || type == Integer.class) return "整数";
        if (type == long.class || type == Long.class) return "长整数";
        if (type == double.class || type == Double.class) return "浮点数";
        if (type == float.class || type == Float.class) return "浮点数";
        if (type == boolean.class || type == Boolean.class) return "布尔值";
        if (type == String.class) return "字符串";
        if (type.isArray()) return inferTypeName(type.getComponentType()) + "列表";
        if (List.class.isAssignableFrom(type)) return "列表";
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
        if (cmdAnnotation == null) return "错误: 无法生成帮助文档; 该命令没有打上 @Cmd 注解";
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

        if (subCmdInfo != null) {
            // 使用 @SubCommandInfo 的详细描述
            sb.append(" - ").append(subCmdInfo.description()).append("\n\n");
        } else {
            // 回退到 RouteConfig 的简单描述
            sb.append(" - ").append(routeConfig.description()).append("\n\n");
        }

        // 2. 用法说明（优先使用 @SubCommandInfo.usage）
        if (subCmdInfo != null && !subCmdInfo.usage().isEmpty()) {
            sb.append("用法:\n");
            sb.append("  ").append(subCmdInfo.usage()).append("\n\n");
        } else {
            // 自动生成用法
            sb.append("用法:\n");
            sb.append("  ").append(cmdAnnotation.name()).append(" ").append(subCommandPath);

            List<CmdParamProcessor.FieldInfo> routeParams = CmdParamProcessor.getCmdParamFields(routeConfig.requestType());
            List<CmdParamProcessor.FieldInfo> positionalParams = routeParams.stream()
                    .filter(fi -> fi.param().position() > 0)
                    .sorted(Comparator.comparingInt(a -> a.param().position()))
                    .collect(Collectors.toList());

            for (CmdParamProcessor.FieldInfo fi : positionalParams) {
                sb.append(" <").append(fi.param().description()).append(">");
            }

            List<CmdParamProcessor.FieldInfo> optionalParams = routeParams.stream()
                .filter(fi -> !fi.param().required() && fi.param().position() == 0)
                .collect(Collectors.toList());

            if (!optionalParams.isEmpty()) {
                sb.append(" [选项]");
            }

            sb.append("\n\n");
        }

        // 3. 实际示例（来自 @SubCommandInfo.examples）
        if (subCmdInfo != null && subCmdInfo.examples().length > 0) {
            sb.append("示例:\n");
            for (String example : subCmdInfo.examples()) {
                sb.append("  ").append(example).append("\n");
            }
            sb.append("\n");
        }

        // 4. 参数列表（始终显示）
        List<CmdParamProcessor.FieldInfo> routeParams = CmdParamProcessor.getCmdParamFields(routeConfig.requestType());
        if (!routeParams.isEmpty()) {
            sb.append("选项:\n");

            for (CmdParamProcessor.FieldInfo fi : routeParams) {
                sb.append("  ").append(formatParamHelp(fi)).append("\n");
            }

            sb.append("\n");
        }

        // 5. 选项详细说明（来自 @SubCommandInfo.optionsDesc）
        if (subCmdInfo != null && !subCmdInfo.optionsDesc().isEmpty() &&
            !subCmdInfo.optionsDesc().startsWith("这个命令没有帮助信息")) {
            sb.append("选项详情:\n");
            String optionsText = subCmdInfo.optionsDesc().trim();
            // 缩进每一行
            for (String line : optionsText.split("\n")) {
                if (!line.trim().isEmpty()) {
                    sb.append("  ").append(line.trim()).append("\n");
                }
            }
            sb.append("\n");
        }

        // 6. 相关命令（来自 @SubCommandInfo.seeAlso）
        if (subCmdInfo != null && subCmdInfo.seeAlso().length > 0) {
            sb.append("相关命令:\n");
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

        sb.append("  ").append(p.description());

        if (!p.required()) {
            String defVal = p.defaultValue();
            if (!defVal.isEmpty()) {
                sb.append(" (默认: ").append(defVal).append(")");
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
            sb.append(" [操作符");
            if (p.operatorArgs() > 0) {
                sb.append(", 消费").append(p.operatorArgs()).append("个参数");
            }
            sb.append("]");
        }

        // 显示互斥约束
        if (p.mutexWith().length > 0) {
            sb.append(" [互斥: ").append(String.join(", ", p.mutexWith())).append("]");
        }

        return sb.toString();
    }
}
