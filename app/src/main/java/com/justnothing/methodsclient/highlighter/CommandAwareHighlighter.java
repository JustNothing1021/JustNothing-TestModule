package com.justnothing.methodsclient.highlighter;

import org.jline.reader.Highlighter;
import org.jline.reader.LineReader;
import org.jline.utils.AttributedString;
import org.jline.utils.AttributedStringBuilder;
import org.jline.utils.AttributedStyle;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 命令模式专用高亮器。
 * <p>
 * 与 {@link JavaSyntaxHighlighter} 不同，此高亮器针对命令行输入优化：
 * <ul>
 *   <li>命令名（如 class, script）→ 青色加粗</li>
 *   <li>子路由（如 info, run）→ 绿色</li>
 *   <li>参数标志（如 --class, -v）→ 蓝色</li>
 *   <li>参数值 → 黄色（字符串）/ 品红（数字）</li>
 * </ul>
 */
public class CommandAwareHighlighter implements Highlighter {

    private static final AttributedStyle STYLE_COMMAND = AttributedStyle.DEFAULT
            .foreground(AttributedStyle.CYAN).bold();
    private static final AttributedStyle STYLE_SUBROUTE = AttributedStyle.DEFAULT
            .foreground(AttributedStyle.GREEN);
    private static final AttributedStyle STYLE_FLAG = AttributedStyle.DEFAULT
            .foreground(AttributedStyle.BLUE);
    private static final AttributedStyle STYLE_STRING_VALUE = AttributedStyle.DEFAULT
            .foreground(AttributedStyle.YELLOW);
    private static final AttributedStyle STYLE_NUMERIC = AttributedStyle.DEFAULT
            .foreground(AttributedStyle.MAGENTA);

    // 匹配 --xxx 或 -x 格式的参数标志
    private static final Pattern FLAG_PATTERN = Pattern.compile("(--?[a-zA-Z][a-zA-Z0-9-]*)");
    // 匹配引号包裹的字符串值
    private static final Pattern STRING_VALUE_PATTERN = Pattern.compile("(\"[^\"]*\"|'[^']*')");
    // 匹配数字
    private static final Pattern NUMBER_PATTERN = Pattern.compile("\\b\\d+(\\.\\d+)?\\b");

    @Override
    public AttributedString highlight(LineReader reader, String buffer) {
        if (buffer == null || buffer.isEmpty()) {
            return new AttributedString(buffer);
        }

        AttributedStringBuilder sb = new AttributedStringBuilder(buffer.length() * 2);

        // 简单分词着色：按空格分割，逐 token 判断类型
        String[] tokens = buffer.split("\\s+", -1);
        int pos = 0;

        for (int i = 0; i < tokens.length; i++) {
            String token = tokens[i];
            int tokenStart = buffer.indexOf(token, pos);
            if (tokenStart < 0) tokenStart = pos;

            // token 之间的分隔符原样输出
            if (tokenStart > pos) {
                sb.append(buffer.substring(pos, tokenStart));
            }

            if (i == 0 && !token.startsWith("-")) {
                // 第一个非 flag token → 当作命令名
                // 检查是否含冒号（子路由形式 class:info）
                if (token.contains(":")) {
                    int colonIdx = token.indexOf(':');
                    String cmdPart = token.substring(0, colonIdx);
                    String subPart = token.substring(colonIdx + 1);
                    sb.append(cmdPart, STYLE_COMMAND);
                    sb.append(":", AttributedStyle.DEFAULT);
                    sb.append(subPart, STYLE_SUBROUTE);
                } else {
                    sb.append(token, STYLE_COMMAND);
                }
            } else if (token.startsWith("-") && !isNumeric(token)) {
                // 参数标志
                if (token.contains("=")) {
                    // --key=value 格式
                    int eqIdx = token.indexOf('=');
                    String key = token.substring(0, eqIdx);
                    String val = token.substring(eqIdx + 1);
                    sb.append(key, STYLE_FLAG);
                    sb.append("=", AttributedStyle.DEFAULT);
                    if (val.startsWith("\"") || val.startsWith("'")) {
                        sb.append(val, STYLE_STRING_VALUE);
                    } else if (isNumeric(val)) {
                        sb.append(val, STYLE_NUMERIC);
                    } else {
                        sb.append(val, STYLE_STRING_VALUE);
                    }
                } else {
                    sb.append(token, STYLE_FLAG);
                }
            } else if ((token.startsWith("\"") || token.startsWith("'")) || 
                       (i > 0 && isPreviousTokenFlag(tokens, i))) {
                // 参数值（引号包裹 或 跟在 flag 后面）
                sb.append(token, STYLE_STRING_VALUE);
            } else if (isNumeric(token)) {
                sb.append(token, STYLE_NUMERIC);
            } else {
                sb.append(token, AttributedStyle.DEFAULT);
            }

            pos = tokenStart + token.length();
        }

        // 处理尾部剩余字符
        if (pos < buffer.length()) {
            sb.append(buffer.substring(pos));
        }

        return sb.toAttributedString();
    }

    private boolean isPreviousTokenFlag(String[] tokens, int currentIndex) {
        if (currentIndex <= 0) return false;
        String prev = tokens[currentIndex - 1];
        return prev.startsWith("-") && !prev.contains("=") && !isNumeric(prev);
    }

    private boolean isNumeric(String s) {
        try {
            Double.parseDouble(s);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}
