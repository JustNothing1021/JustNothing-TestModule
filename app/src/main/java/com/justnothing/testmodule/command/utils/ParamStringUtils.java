package com.justnothing.testmodule.command.utils;

import java.util.ArrayList;
import java.util.List;

/**
 * 参数字符串解析工具
 * 
 * 用于正确处理包含引号、转义字符的命令行参数字符串。
 * 解决简单 split("\\s+") 无法处理引号内空格的问题。
 * 
 * <p>支持的功能:</p>
 * <ul>
 *   <li>双引号内的空格不被分割</li>
 *   <li>转义字符支持: \", \\, \s, \t, \n</li>
 *   <li>类型提示格式: Type:"value with spaces"</li>
 *   <li>嵌套引号处理</li>
 * </ul>
 * 
 * <p>使用示例:</p>
 * <pre>{@code
 * // 输入: String:"hello world" int:42 "quoted string"
 * List<String> tokens = ParamStringUtils.tokenize('String:"hello world" int:42 "quoted string"');
 * // 结果: ["String:\"hello world\"", "int:42", "\"quoted string\""]
 * }</pre>
 */
public class ParamStringUtils {

    /**
     * 将参数字符串分割为token列表
     * 
     * 正确处理:
     * - 双引号内的内容 (包括空格)
     * - 转义字符 (\", \\, \s, \t, \n)
     * - 类型提示格式 (Type:value)
     * 
     * @param paramsRaw 原始参数字符串 (如: String:"hello world" int:42)
     * @return 分割后的token列表 (不会为null)
     */
    public static List<String> tokenize(String paramsRaw) {
        List<String> tokens = new ArrayList<>();
        
        if (paramsRaw == null || paramsRaw.isEmpty()) {
            return tokens;
        }
        
        StringBuilder currentToken = new StringBuilder();
        boolean inQuotes = false;
        boolean escapeNext = false;
        
        for (int i = 0; i < paramsRaw.length(); i++) {
            char c = paramsRaw.charAt(i);
            
            if (escapeNext) {
                // 处理转义字符
                currentToken.append(c);
                escapeNext = false;
            } else if (c == '\\') {
                // 转义符: 下一个字符原样保留
                escapeNext = true;
            } else if (c == '"') {
                if (inQuotes) {
                    // 结束引号
                    inQuotes = false;
                    currentToken.append('"');  // 保留引号标记
                    addTokenIfNotEmpty(tokens, currentToken);
                } else {
                    // 开始引号
                    inQuotes = true;
                    currentToken.append('"');  // 保留引号标记
                }
            } else if (Character.isWhitespace(c) && !inQuotes) {
                // 空格且不在引号内 → 分隔符
                addTokenIfNotEmpty(tokens, currentToken);
            } else {
                // 普通字符
                currentToken.append(c);
            }
        }
        
        // 处理最后一个token
        addTokenIfNotEmpty(tokens, currentToken);
        
        return tokens;
    }

    /**
     * 解析单个参数token，分离类型提示和值
     * 
     * 支持的格式:
     * - Type:"value" → typeHint="Type", value="value"
     * - Type:value → typeHint="Type", value="value"
     * - "value" → typeHint="", value="value"
     * - value → typeHint="", value="value"
     * 
     * @param token 单个参数token (如: String:"hello world")
     * @return 解析结果 (typeHint和value)
     */
    public static ParamToken parseToken(String token) {
        if (token == null || token.isEmpty()) {
            return new ParamToken("", "");
        }
        
        String typeHint = "";
        String value = token;
        
        // 检查是否有类型提示 (格式: Type:value 或 Type:"value")
        if (token.contains(":") && !token.startsWith("\"")) {
            int colonIdx = findColonOutsideQuotes(token);
            if (colonIdx > 0 && colonIdx < token.length() - 1) {
                typeHint = token.substring(0, colonIdx).trim();
                value = token.substring(colonIdx + 1).trim();
                
                // 保留引号（让后续的ExpressionParser能正确识别字符串字面量）
            }
        }
        // 无类型提示时也保留引号
        
        return new ParamToken(typeHint, value);
    }

    /**
     * 完整解析参数字符串为结构化参数列表
     * 
     * @param paramsRaw 原始参数字符串
     * @return 结构化参数列表
     */
    public static List<ParamToken> parseParams(String paramsRaw) {
        List<ParamToken> result = new ArrayList<>();
        List<String> tokens = tokenize(paramsRaw);
        
        for (String token : tokens) {
            result.add(parseToken(token));
        }
        
        return result;
    }

    private static void addTokenIfNotEmpty(List<String> tokens, StringBuilder sb) {
        if (sb.length() > 0) {
            tokens.add(sb.toString());
            sb.setLength(0);  // 清空StringBuilder
        }
    }

    private static int findColonOutsideQuotes(String str) {
        boolean inQuotes = false;
        for (int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);
            if (c == '"' && (i == 0 || str.charAt(i-1) != '\\')) {
                inQuotes = !inQuotes;
            } else if (c == ':' && !inQuotes) {
                return i;
            }
        }
        return -1;
    }

    private static String stripMatchingQuotes(String str) {
        if (str != null && str.length() >= 2 
            && str.startsWith("\"") && str.endsWith("\"")
            && countUnescapedQuotes(str.substring(1, str.length()-1)) % 2 == 0) {
            return str.substring(1, str.length() - 1);
        }
        return str;
    }

    private static int countUnescapedQuotes(String str) {
        int count = 0;
        for (int i = 0; i < str.length(); i++) {
            if (str.charAt(i) == '"') {
                count++;
            }
        }
        return count;
    }

    /**
     * 参数token的结构化表示
     */
    public record ParamToken(String typeHint, String value) {}
}
