package com.justnothing.testmodule.command.framework.utils;

import com.justnothing.testmodule.command.framework.annotation.CmdParam;

/**
 * 字符串 → Java 类型转换，支持引号处理模式。
 */
public class CmdValueConverter {

    /**
     *  增强版类型转换，支持引号处理模式
     * @param valueStr 输入字符串
     * @param targetType 目标类型
     * @param readMode 引号处理模式
     * @return 转换后的对象
     */
    public static Object convertValue(String valueStr, Class<?> targetType, CmdParam.ReadMode readMode) {
        String processed = processQuotes(valueStr, readMode);

        if (targetType == String.class) return processed;
        if (targetType == int.class || targetType == Integer.class) return Integer.parseInt(processed);
        if (targetType == long.class || targetType == Long.class) return Long.parseLong(processed);
        if (targetType == double.class || targetType == Double.class) return Double.parseDouble(processed);
        if (targetType == float.class || targetType == Float.class) return Float.parseFloat(processed);
        if (targetType == boolean.class || targetType == Boolean.class) return Boolean.parseBoolean(processed);

        return processed;
    }

    /**
     *  保持向后兼容的旧接口（默认使用 STRIPPED 模式）
     */
    public static Object convertValue(String valueStr, Class<?> targetType) {
        return convertValue(valueStr, targetType, CmdParam.ReadMode.STRIPPED);
    }

    /**
     *  根据不同的 ReadMode 处理引号
     * <p>
     * 三种模式对比：
     * <p>
     * RAW (原始):
     *   test -> "test"
     *   "test" -> "\"test\""
     *   arg with spaces -> "\"arg" (按空格分割后的第一个token)
     * <p>
     * STRIPPED (智能去引号，默认):
     *   test -> "test"
     *   "test" -> test
     *   "arg with spaces" -> arg with spaces
     * <p>
     * PRESERVED (完整保留):
     *   test -> "test"
     *   "test" -> "\"test\""
     *   "arg with spaces" -> "\"arg with spaces\""
     */
    private static String processQuotes(String input, CmdParam.ReadMode mode) {
        if (input == null) return null;

        boolean isSurroundedByQuotes = (input.startsWith("\"") && input.endsWith("\"")) ||
                (input.startsWith("'") && input.endsWith("'"));
        switch (mode) {
            case RAW:
                // 原始模式：不处理引号，直接返回原始字符串
                return input;

            case STRIPPED:
                // 智能去引号模式（默认）
                if (isSurroundedByQuotes) {
                    return input.substring(1, input.length() - 1);
                }
                return input;

            case PRESERVED:
                // 完整保留模式：如果已有引号则保留并转义内部引号
                if (isSurroundedByQuotes) {
                    // 已经有外层引号：转义内部的引号
                    String content = input.substring(1, input.length() - 1);
                    char quote = input.charAt(0);
                    // 转义内部相同类型的引号
                    content = content.replace(String.valueOf(quote), "\\" + quote);
                    return input.charAt(0) + content + input.charAt(input.length() - 1);
                } else {
                    // 没有外层引号：添加引号包裹
                    return "\"" + input.replace("\"", "\\\"") + "\"";
                }

            default:
                return input;
        }
    }
}
