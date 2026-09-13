package com.justnothing.testmodule.command.framework.utils;

import com.justnothing.testmodule.command.framework.annotation.CmdParam;
import com.justnothing.testmodule.command.framework.model.CommandRouter;
import com.justnothing.testmodule.command.framework.model.CommandRequest;
import com.justnothing.testmodule.command.framework.model.CustomCommandLineParser;
import com.justnothing.testmodule.utils.logging.Logger;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 门面类：保留对外公开 API，实际逻辑委托给 {@link CmdArgParser}、{@link CmdHelpGenerator} 等。
 */
public class CmdParamProcessor {

    private static final Logger logger = Logger.getLoggerForName("CmdParamProcessor");

    private static final Map<Class<?>, List<FieldInfo>> fieldCache = new HashMap<>();

    public static List<FieldInfo> getCmdParamFields(Class<?> clazz) {
        return fieldCache.computeIfAbsent(clazz, CmdParamProcessor::scanFields);
    }

    private static List<FieldInfo> scanFields(Class<?> clazz) {
        List<FieldInfo> fields = new ArrayList<>();
        Class<?> current = clazz;

        while (current != null && current != Object.class) {
            for (Field field : current.getDeclaredFields()) {
                if (java.lang.reflect.Modifier.isStatic(field.getModifiers())) continue;

                // 检查 @CmdParam 注解
                CmdParam param = field.getAnnotation(CmdParam.class);
                if (param != null) {
                    fields.add(new FieldInfo(field, param));
                }
            }
            current = current.getSuperclass();
        }

        return fields;
    }

    /**
     *  智能解析入口（公共方法）
     * <p>
     * 自动选择最佳解析策略：
     * - 如果 request 实现了 CustomCommandLineParser → 使用自定义解析器
     * - 否则 → 使用标准 @CmdParam 声明式解析
     *
     * @param request 请求对象
     * @param args 命令行参数数组
     * @return 解析完成后的请求对象（可能是新实例）
     * @throws IllegalArgumentException 参数验证错误
     */
    public static CommandRequest<?> parseRequest(CommandRequest<?> request, String[] args) throws IllegalArgumentException {
        if (request == null) {
            throw new IllegalArgumentException("请求对象不能为 null");
        }

        // 注意：args 为空时也必须继续解析——否则 required 参数会被静默放行、
        // defaultValue 也不会套用（历史上这里直接 return request 导致校验失效）。
        if (args == null) {
            args = new String[0];
        }

        String requestTypeName = request.getClass().getSimpleName();

        try {
            if (request instanceof CustomCommandLineParser) {
                // 模式A: 自定义解析器（用于复杂参数逻辑）
                logger.debug("🔧 [parseRequest] 使用 CustomCommandLineParser: " + requestTypeName);

                List<String> argList = Arrays.asList(args);
                CustomCommandLineParser.ParseContext parseContext =
                    new CustomCommandLineParser.ParseContext(args, argList, new HashMap<>());

                CommandRequest<?> parsedRequest = ((CustomCommandLineParser) request).customParse(parseContext);

                if (parsedRequest != null) {
                    logger.debug("✅ [parseRequest] 自定义解析完成");
                    return parsedRequest;
                } else {
                    logger.debug("✅ [parseRequest] 自定义解析返回null，使用原对象");
                    return request;
                }
            } else {
                // 模式B: 标准声明式解析（@CmdParam 自动处理）
                logger.debug(" [parseRequest] 使用 CmdParamProcessor: " + requestTypeName);

                CmdArgParser.parse(request, args);

                logger.debug("✅ [parseRequest] 标准解析完成");
                return request;
            }
        } catch (IllegalArgumentException e) {
            // 参数验证错误：直接向上抛出，让上层显示帮助文档
            logger.warn("⚠️ [parseRequest] 参数验证失败: " + e.getMessage());
            throw e;
        } catch (Exception e) {
            // 其他错误：包装后抛出
            logger.error("❌ [parseRequest] 解析失败: " + e.getMessage(), e);
            throw new IllegalArgumentException(
                "参数解析失败 (" + requestTypeName + "): " + e.getMessage(), e
            );
        }
    }

    public static void parseCommandLineArgs(CommandRequest<?> request, String[] args) throws IllegalArgumentException {
        CmdArgParser.parse(request, args);
    }

    public static String generateHelpText(Class<?> cmdClass) {
        return CmdHelpGenerator.generateHelpText(cmdClass);
    }

    public static String generateHelpForRoute(Class<?> cmdClass, CommandRouter.RouteConfig routeConfig) {
        return CmdHelpGenerator.generateHelpForRoute(cmdClass, routeConfig);
    }

    public record FieldInfo(Field field, CmdParam param) {
    }
}
