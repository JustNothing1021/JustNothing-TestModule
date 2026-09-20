package com.justnothing.methodsclient.highlighter;

import org.jline.reader.Highlighter;
import org.jline.reader.LineReader;
import org.jline.utils.AttributedString;
import org.jline.utils.AttributedStringBuilder;
import org.jline.utils.AttributedStyle;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class JavaSyntaxHighlighter implements Highlighter {

    private static final int MAX_HIGHLIGHT_LENGTH = 4096;

    private static final AttributedStyle STRING_STYLE = new AttributedStyle().foreground(202);
    private static final AttributedStyle ESCAPE_VALID_STYLE = new AttributedStyle().foreground(11);
    private static final AttributedStyle ESCAPE_INVALID_STYLE = new AttributedStyle().foreground(1);
    private static final AttributedStyle EXTRA_BRACKET_STYLE = new AttributedStyle().foreground(1);

    private static final int[] BRACKET_COLORS = {
            AttributedStyle.BLUE, AttributedStyle.YELLOW, AttributedStyle.MAGENTA
    };

    private static final AttributedStyle STYLE_MODIFIER = AttributedStyle.DEFAULT.foreground(AttributedStyle.BLUE).bold();
    private static final AttributedStyle STYLE_PRIMITIVE = AttributedStyle.DEFAULT.foreground(AttributedStyle.GREEN);
    private static final AttributedStyle STYLE_CONTROL = AttributedStyle.DEFAULT.foreground(AttributedStyle.MAGENTA).bold();
    private static final AttributedStyle STYLE_SPECIAL = AttributedStyle.DEFAULT.foreground(AttributedStyle.MAGENTA);
    private static final AttributedStyle STYLE_FUNCTION = AttributedStyle.DEFAULT.foreground(AttributedStyle.YELLOW);
    private static final AttributedStyle STYLE_CLASS_NAME = AttributedStyle.DEFAULT.foreground(AttributedStyle.GREEN).bold();
    private static final AttributedStyle STYLE_IDENTIFIER = AttributedStyle.DEFAULT.foreground(AttributedStyle.CYAN);

    private static final Map<String, AttributedStyle> KEYWORDS;

    static {
        Map<String, AttributedStyle> map = new HashMap<>();
        for (String kw : Arrays.asList("public", "protected", "private", "static", "final",
                "abstract", "synchronized", "native", "strictfp", "transient", "volatile",
                "extends", "implements", "import", "package", "super", "this",
                "true", "false", "null")) {
            map.put(kw, STYLE_MODIFIER);
        }
        for (String kw : Arrays.asList("byte", "short", "int", "long", "float", "double", "boolean", "char", "void")) {
            map.put(kw, STYLE_PRIMITIVE);
        }
        for (String kw : Arrays.asList("if", "else", "for", "while", "do", "switch", "case", "default",
                "break", "continue", "return", "throw", "throws", "try", "catch", "finally",
                "new")) {
            map.put(kw, STYLE_CONTROL);
        }
        for (String cmd : Arrays.asList("exit", "quit", ":multi", ":eval", ":clear")) {
            map.put(cmd, STYLE_SPECIAL);
        }
        KEYWORDS = Collections.unmodifiableMap(map);
    }

    private static final Pattern NUMBER_PATTERN = Pattern.compile(
            "\\b(?:0[xX][0-9a-fA-F]+|0[oO][0-7]+|0[bB][01]+|\\d+\\.?\\d*(?:[eE][+-]?\\d+)?)\\b"
    );

    private static final Pattern IDENTIFIER_PATTERN = Pattern.compile(
            "\\b[a-zA-Z_][a-zA-Z0-9_]*\\b"
    );

    private final Deque<Character> globalStack = new ArrayDeque<>();

    private long lastHighlightTime = 0;
    private static final long DEBOUNCE_MS = 30;

    public void resetBracketStack() {
        globalStack.clear();
    }

    @Override
    public AttributedString highlight(LineReader reader, String buffer) {
        long now = System.currentTimeMillis();
        if (buffer.length() > MAX_HIGHLIGHT_LENGTH || (now - lastHighlightTime < DEBOUNCE_MS && buffer.length() > 50)) {
            lastHighlightTime = now;
            return new AttributedString(buffer);
        }
        lastHighlightTime = now;

        AttributedStringBuilder sb = new AttributedStringBuilder(buffer.length() * 2);
        Deque<Character> stack = new ArrayDeque<>(globalStack);
        int length = buffer.length();
        int pos = 0;

        while (pos < length) {
            char ch = buffer.charAt(pos);

            if ((ch == '"' || ch == '\'') && (pos == 0 || buffer.charAt(pos - 1) != '\\')) {
                ParsedString parsed = parseString(buffer, pos, ch);
                sb.append(parsed.attributed);
                pos = parsed.newPos;
                continue;
            }

            if (isBracket(ch)) {
                if (isOpenBracket(ch)) {
                    int depth = stack.size();
                    sb.append(String.valueOf(ch), AttributedStyle.DEFAULT.foreground(BRACKET_COLORS[depth % BRACKET_COLORS.length]));
                    stack.push(ch);
                } else if (!stack.isEmpty() && isMatchingPair(stack.peek(), ch)) {
                    int depth = stack.size() - 1;
                    sb.append(String.valueOf(ch), AttributedStyle.DEFAULT.foreground(BRACKET_COLORS[depth % BRACKET_COLORS.length]));
                    stack.pop();
                } else {
                    sb.append(String.valueOf(ch), EXTRA_BRACKET_STYLE);
                }
                pos++;
                continue;
            }

            Matcher numMatcher = NUMBER_PATTERN.matcher(buffer);
            if (numMatcher.find(pos) && numMatcher.start() == pos) {
                String num = numMatcher.group();
                sb.append(num, AttributedStyle.DEFAULT.foreground(AttributedStyle.GREEN));
                pos += num.length();
                continue;
            }

            Matcher identMatcher = IDENTIFIER_PATTERN.matcher(buffer);
            if (identMatcher.find(pos) && identMatcher.start() == pos) {
                String ident = identMatcher.group();
                int end = pos + ident.length();

                AttributedStyle kwStyle = KEYWORDS.get(ident);
                if (kwStyle != null) {
                    sb.append(ident, kwStyle);
                } else {
                    int next = end;
                    while (next < length && Character.isWhitespace(buffer.charAt(next))) next++;
                    boolean isFunctionCall = (next < length && buffer.charAt(next) == '(');
                    if (isFunctionCall) {
                        sb.append(ident, STYLE_FUNCTION);
                    } else if (!ident.isEmpty() && Character.isUpperCase(ident.charAt(0))) {
                        sb.append(ident, STYLE_CLASS_NAME);
                    } else {
                        sb.append(ident, STYLE_IDENTIFIER);
                    }
                }
                pos = end;
                continue;
            }

            sb.append(ch);
            pos++;
        }

        globalStack.clear();
        globalStack.addAll(stack);

        return sb.toAttributedString();
    }

    private static boolean isBracket(char c) {
        return c == '(' || c == ')' || c == '[' || c == ']' || c == '{' || c == '}';
    }

    private static boolean isOpenBracket(char c) {
        return c == '(' || c == '[' || c == '{';
    }

    private static boolean isMatchingPair(char open, char close) {
        return (open == '(' && close == ')') ||
                (open == '[' && close == ']') ||
                (open == '{' && close == '}');
    }

    private static boolean isHex(char c) {
        return (c >= '0' && c <= '9') || (c >= 'A' && c <= 'F') || (c >= 'a' && c <= 'f');
    }

    private static class ParsedString {
        final AttributedString attributed;
        final int newPos;
        ParsedString(AttributedString a, int pos) { this.attributed = a; this.newPos = pos; }
    }

    private static ParsedString parseString(String buffer, int start, char quote) {
        AttributedStringBuilder sb = new AttributedStringBuilder();
        sb.append(String.valueOf(quote), STRING_STYLE);
        int i = start + 1;
        int len = buffer.length();
        while (i < len) {
            char c = buffer.charAt(i);
            if (c == '\\') {
                if (i + 1 >= len) {
                    sb.append("\\", ESCAPE_INVALID_STYLE);
                    break;
                }
                char next = buffer.charAt(i + 1);
                int escapeLen;
                boolean valid = true;
                String escapeSeq;
                switch (next) {
                    case '\\': case '"': case '\'': case 'n': case 't': case 'r': case 'b': case 'f':
                        escapeSeq = "\\" + next; escapeLen = 2;
                        break;
                    case 'u':
                        if (i + 5 < len && isHex(buffer.charAt(i+2)) && isHex(buffer.charAt(i+3))
                                && isHex(buffer.charAt(i+4)) && isHex(buffer.charAt(i+5))) {
                            escapeSeq = buffer.substring(i, i + 6); escapeLen = 6;
                        } else {
                            escapeSeq = buffer.substring(i, Math.min(i + 6, len));
                            escapeLen = escapeSeq.length();
                            valid = false;
                        }
                        break;
                    case 'x':
                        if (i + 3 < len && isHex(buffer.charAt(i+2)) && isHex(buffer.charAt(i+3))) {
                            escapeSeq = buffer.substring(i, i + 4); escapeLen = 4;
                        } else {
                            escapeSeq = buffer.substring(i, Math.min(i + 4, len));
                            escapeLen = escapeSeq.length();
                            valid = false;
                        }
                        break;
                    case '0': case '1': case '2': case '3':
                        int octEnd = i + 1;
                        while (octEnd < len && octEnd - i - 1 < 3 &&
                                buffer.charAt(octEnd) >= '0' && buffer.charAt(octEnd) <= '7') {
                            octEnd++;
                        }
                        escapeSeq = buffer.substring(i, octEnd);
                        escapeLen = escapeSeq.length();
                        break;
                    default:
                        escapeSeq = "\\" + next;
                        escapeLen = 2;
                        valid = false;
                }
                sb.append(escapeSeq, valid ? ESCAPE_VALID_STYLE : ESCAPE_INVALID_STYLE);
                i += escapeLen;
            } else if (c == quote) {
                sb.append(String.valueOf(quote), STRING_STYLE);
                i++;
                break;
            } else {
                sb.append(String.valueOf(c), STRING_STYLE);
                i++;
            }
        }
        return new ParsedString(sb.toAttributedString(), i);
    }
}
