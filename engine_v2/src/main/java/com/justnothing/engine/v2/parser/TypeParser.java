package com.justnothing.engine.v2.parser;

import com.justnothing.engine.v2.ast.Resolvable;
import com.justnothing.engine.v2.lexer.TokenType;
import com.justnothing.engine.v2.parser.trie.TokenMatcher;
import com.justnothing.engine.v2.parser.trie.TokenStream;

import static com.justnothing.engine.v2.lexer.TokenType.*;
import static com.justnothing.engine.v2.parser.StmtParser.isClosingAngle;

/**
 * 类型语法解析器。
 * <p>
 * 解析类型表达式，如：
 * <ul>
 *   <li>基本类型：int, long, double, boolean 等</li>
 *   <li>类名：String, List, java.util.Map</li>
 *   <li>泛型：List&lt;String&gt;, Map&lt;String, Integer&gt;</li>
 *   <li>数组：int[], String[][]</li>
 *   <li>通配符：? extends Number, ? super T</li>
 * </ul>
 * </p>
 *
 * @author JustNothing1021
 */
public class TypeParser {

    /** 基本类型集合 */
    static final TokenMatcher PRIMITIVE = TokenMatcher.any(
            KEYWORD_INT, KEYWORD_LONG, KEYWORD_FLOAT, KEYWORD_DOUBLE,
            KEYWORD_BOOLEAN, KEYWORD_CHAR, KEYWORD_BYTE, KEYWORD_SHORT,
            KEYWORD_VOID
    );

    /**
     * 泛型类型起点：基本类型关键字 + var/auto + 标识符 + 限定名。
     * <p>
     * 用于在 DecisionTree 的谓词中快速判断"当前位置是否可能是一个类型声明的开始"。
     * 贪心 QUALIFIED_NAME 策略下，Map.Entry 是单个 Token，归 Resolver 消歧。
     * 配合 {@link #skipTypeSpec(TokenStream, int)} 可完成完整的前瞻类型跳读。
     * </p>
     */
    public static final TokenMatcher GENERIC_TYPE_KIND = TokenMatcher.any(
            KEYWORD_INT, KEYWORD_LONG, KEYWORD_FLOAT, KEYWORD_DOUBLE,
            KEYWORD_BOOLEAN, KEYWORD_CHAR, KEYWORD_BYTE, KEYWORD_SHORT,
            KEYWORD_VAR, KEYWORD_AUTO, KEYWORD_VOID,
            KEYWORD_ASYNC, KEYWORD_AWAIT,
            KEYWORD_DELETE, KEYWORD_GOTO, KEYWORD_USING,
            KEYWORD_PERMITS,
            IDENTIFIER, QUALIFIED_NAME
    );

    private final ParseContext ctx;

    public TypeParser(ParseContext ctx) {
        this.ctx = ctx;
    }

    /**
     * 解析类型引用，返回 Resolvable。
     */
    public Resolvable parseTypeRef() {
        return Resolvable.byName(parseTypeName());
    }

    /**
     * 前瞻跳读类型声明（不消费任何 token）。
     * <p>
     * 格式：类型名 + 可选泛型 &lt;...&gt; + 可选数组后缀 [][]。
     * 用于 DecisionTree 谓词中判断"当前位置是否为类型声明"并跳过。
     * </p>
     *
     * @param stream token 流
     * @param offset 前瞻起始偏移
     * @return 跳过的 token 数量，-1 表示不匹配
     */
    public static int skipTypeSpec(TokenStream stream, int offset) {
        TokenType t = stream.peekType(offset);
        if (!GENERIC_TYPE_KIND.matches(t)) return -1;
        int o = offset + 1; // 跳过类型名本身

        // 跳过泛型参数 <...>（处理嵌套 >>> 歧义：>> 代表两个 >，>>> 代表三个 >）
        if (stream.peekType(o) == OPERATOR_LESS_THAN) {
            int depth = 1;
            o++;
            while (depth > 0 && stream.peekType(o) != EOF) {
                TokenType tt = stream.peekType(o);
                if (tt == OPERATOR_LESS_THAN) depth++;
                else if (tt == OPERATOR_UNSIGNED_RIGHT_SHIFT) depth -= 3;
                else if (tt == OPERATOR_RIGHT_SHIFT) depth -= 2;
                else if (tt == OPERATOR_GREATER_THAN) depth--;
                o++;
            }
        }

        // 跳过限定名后缀：Outer<K,V>.Inner, TrieBuilder<C,R>.PathBuilder
        while (stream.peekType(o) == OPERATOR_DOT
                && (stream.peekType(o + 1) == IDENTIFIER || stream.peekType(o + 1) == QUALIFIED_NAME)) {
            o += 2; // .Identifier
            // 跳过内部类的泛型参数
            if (stream.peekType(o) == OPERATOR_LESS_THAN) {
                int depth = 1;
                o++;
                while (depth > 0 && stream.peekType(o) != EOF) {
                    TokenType tt = stream.peekType(o);
                    if (tt == OPERATOR_LESS_THAN) depth++;
                    else if (tt == OPERATOR_UNSIGNED_RIGHT_SHIFT) depth -= 3;
                    else if (tt == OPERATOR_RIGHT_SHIFT) depth -= 2;
                    else if (tt == OPERATOR_GREATER_THAN) depth--;
                    o++;
                }
            }
        }

        // 跳过数组后缀 [][]
        while (stream.peekType(o) == DELIMITER_LEFT_BRACKET
                && stream.peekType(o + 1) == DELIMITER_RIGHT_BRACKET) {
            o += 2;
        }

        return o - offset;
    }

    /**
     * 解析类型名字符串（含泛型和数组后缀）。
     */
    public String parseTypeName() {
        StringBuilder sb = new StringBuilder();

        TokenType type = ctx.peekType();
        if (GENERIC_TYPE_KIND.matches(type)) {
            sb.append(ctx.advance().text());
        } else {
            ctx.error("期望类型名，实际为 " + type);
            return "Object";
        }

        // 泛型参数 <T, U, ...> 与限定名链交替：TrieBuilder<C,R>.PathBuilder
        for (;;) {
            // 泛型参数
            if (ctx.check(OPERATOR_LESS_THAN)) {
                sb.append(parseGenericArgs());
            }
            // 限定名链：Map.Entry, TrieBuilder<C,R>.PathBuilder
            if (ctx.check(OPERATOR_DOT) && (ctx.peekType(1) == IDENTIFIER
                    || ctx.peekType(1) == QUALIFIED_NAME)) {
                sb.append(ctx.advance().text()); // .
                sb.append(ctx.advance().text()); // Identifier
            } else {
                break;
            }
        }

        // 数组后缀 [][]...
        while (ctx.check(DELIMITER_LEFT_BRACKET)
                && ctx.peekType(1) == DELIMITER_RIGHT_BRACKET) {
            ctx.advance(); // [
            ctx.advance(); // ]
            sb.append("[]");
        }

        return sb.toString();
    }

    /**
     * 解析泛型参数列表 {@code <T, U, ? extends V>}，返回含尖括号的文本。
     */
    public String parseGenericArgs() {
        StringBuilder sb = new StringBuilder();
        sb.append(ctx.advance().text()); // <

        // 空泛型：<>
        if (isClosingAngle(ctx.peekType())) {
            ctx.expectClosingAngle();
            sb.append('>');
            return sb.toString();
        }

        // 第一个参数
        sb.append(parseGenericArg());

        while (ctx.match(DELIMITER_COMMA)) {
            sb.append(", ");
            sb.append(parseGenericArg());
        }

        ctx.expectClosingAngle(); // 处理 >>> 歧义
        sb.append('>');
        return sb.toString();
    }

    /**
     * 解析单个泛型参数：基本类型 | 类名 | 通配符(? extends/super T) | 嵌套泛型。
     */
    private String parseGenericArg() {
        // 通配符：? extends T 或 ? super T 或 ?
        if (ctx.check(OPERATOR_QUESTION)) {
            StringBuilder sb = new StringBuilder();
            sb.append(ctx.advance().text()); // ?
            if (ctx.match(KEYWORD_EXTENDS)) {
                sb.append(" extends ");
                sb.append(parseTypeName());
            } else if (ctx.match(KEYWORD_SUPER)) {
                sb.append(" super ");
                sb.append(parseTypeName());
            }
            return sb.toString();
        }
        // 普通类型（可嵌套泛型）
        return parseTypeName();
    }
}
