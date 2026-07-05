package com.justnothing.engine.v2.parser;

import com.justnothing.engine.v2.ast.ASTNode;
import com.justnothing.engine.v2.ast.SourceLocation;
import com.justnothing.engine.v2.ast.decl.AnnotationVal;
import com.justnothing.engine.v2.ast.expr.ArrayLiteralExpr;
import com.justnothing.engine.v2.ast.expr.LiteralExpr;
import com.justnothing.engine.v2.ast.expr.VariableExpr;
import com.justnothing.engine.v2.lexer.Token;
import com.justnothing.engine.v2.lexer.TokenType;
import static com.justnothing.engine.v2.lexer.TokenType.*;
import com.justnothing.engine.v2.parser.trie.TokenStream;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 解析器共享上下文。
 * <p>
 * 持有 Token 流、错误收集器和源文件名，
 * 所有子解析器（ExprParser/StmtParser/TypeParser/DeclParser）共享同一个实例。
 * </p>
 *
 * @author JustNothing1021
 */
public class ParseContext {

    private final TokenStream stream;
    private final String sourceName;
    private final List<ParseError> errors;

    public ParseContext(List<Token> tokens, String sourceName) {
        this.stream = new TokenStream(tokens);
        this.sourceName = sourceName;
        this.errors = new ArrayList<>();
    }

    // ========== Token 流代理 ==========

    /**
     * 获取内部 TokenStream，供 DecisionTree 分发使用。
     */
    public TokenStream stream() { return stream; }

    public Token peek() { return stream.peek(); }
    public TokenType peekType() { return stream.peekType(); }
    public Token peek(int offset) { return stream.peek(offset); }
    public TokenType peekType(int offset) { return stream.peekType(offset); }
    public Token advance() { return stream.advance(); }
    public boolean match(TokenType expected) { return stream.match(expected); }
    public boolean check(TokenType expected) { return stream.check(expected); }
    public boolean isEOF() { return stream.isEOF(); }
    public String text() { return stream.text(); }
    public String text(int offset) { return stream.text(offset); }
    public int position() { return stream.position(); }
    public void restore(int pos) { stream.restore(pos); }

    // ========== AST 构建辅助 ==========

    /**
     * 当前 peek 位置（可用于构建 AST 节点时填充 location）。
     */
    public SourceLocation location() {
        return peek().location();
    }

    // ========== 解析模式 ==========

    // ========== 解析模式 ==========

    private boolean lambdaEnabled = true;

    public boolean isLambdaEnabled() { return lambdaEnabled; }

    /**
     * 临时禁用单参 Lambda 解析（如 switch case 值），结束后自动恢复。
     */
    public void withoutLambda(Runnable action) {
        boolean saved = lambdaEnabled;
        lambdaEnabled = false;
        try { action.run(); } finally { lambdaEnabled = saved; }
    }

    /** 收集前缀注解，返回列表。调用方负责将注解附加到声明节点上。 */
    public List<AnnotationVal> parseAnnotations() {
        List<AnnotationVal> anns = new ArrayList<>();
        while (peekType() == DELIMITER_AT) {
            // @interface 不是注解，是注解类型声明的关键字组合
            if (peekType(1) == KEYWORD_INTERFACE) break;
            SourceLocation loc = peek().location();
            advance(); // @
            String name = advance().text(); // 注解名
            Map<String, ASTNode> attrs = null;
            if (check(DELIMITER_LEFT_PAREN)) {
                advance(); // (
                if (!check(DELIMITER_RIGHT_PAREN)) {
                    attrs = new java.util.LinkedHashMap<>();
                    do {
                        // 区分 key = value 和 bare value（如 @SuppressWarnings("unchecked")）
                        if (peekType(1) == OPERATOR_ASSIGN) {
                            String key = advance().text();
                            advance(); // =
                            ASTNode val = parseAnnotationValue();
                            attrs.put(key, val);
                        } else {
                            ASTNode val = parseAnnotationValue();
                            attrs.put("value", val);
                        }
                    } while (match(DELIMITER_COMMA));
                }
                expect(DELIMITER_RIGHT_PAREN);
            }
            anns.add(new AnnotationVal.Builder()
                .name(name).attributes(attrs).location(loc).build());
        }
        return anns;
    }

    private ASTNode parseAnnotationValue() {
        if (check(DELIMITER_LEFT_BRACE)) {
            // 数组值：{ a, b }
            SourceLocation loc = location();
            advance();
            List<ASTNode> elems = new ArrayList<>();
            if (!check(DELIMITER_RIGHT_BRACE)) {
                do {
                    if (check(DELIMITER_RIGHT_BRACE)) break; // 处理末尾逗号：{a, b,}
                    elems.add(parseAnnotationValue());
                } while (match(DELIMITER_COMMA));
            }
            expect(DELIMITER_RIGHT_BRACE);
            return new ArrayLiteralExpr.Builder().elements(elems).location(loc).build();
        }
        if (check(LITERAL_STRING) || check(LITERAL_INTEGER) || check(LITERAL_BOOLEAN)
                || check(KEYWORD_NULL)) {
            SourceLocation loc = location();
            return new LiteralExpr.Builder().value(advance().value()).type(Object.class).location(loc).build();
        }
        if (check(OPERATOR_MINUS)) {
            SourceLocation loc = location();
            advance();
            Object v = advance().value();
            return new LiteralExpr.Builder().value(
                v instanceof Integer ? -(Integer)v : v).type(v.getClass()).location(loc).build();
        }
        // 嵌套注解：@Foo, @Bar(key = "val")
        if (check(DELIMITER_AT)) {
            SourceLocation loc = location();
            List<AnnotationVal> nested = parseAnnotations();
            return nested.isEmpty() ? new LiteralExpr.Builder().value(null).type(Object.class).location(loc).build()
                                   : nested.get(0);
        }
        // 类引用 / 枚举值
        return parseNameOrCallRef();
    }

    /** 解析限定名或类引用（用于注解值） */
    private ASTNode parseNameOrCallRef() {
        SourceLocation loc = location();
        StringBuilder sb = new StringBuilder(advance().text());
        while (match(OPERATOR_DOT)) {
            sb.append('.').append(advance().text());
        }
        // 如果有 .class 后缀
        return new VariableExpr.Builder().name(sb.toString()).location(loc).build();
    }

    /** 处理泛型闭合的 >>> 歧义：>> → > + 缓存一个 >，>>> → > + 缓存两个 > */
    public Token expectClosingAngle() {
        TokenType t = peekType();
        if (t == OPERATOR_GREATER_THAN) return advance();
        if (t == OPERATOR_RIGHT_SHIFT || t == OPERATOR_UNSIGNED_RIGHT_SHIFT) {
            int extra = t == OPERATOR_UNSIGNED_RIGHT_SHIFT ? 2 : 1;
            Token token = advance();
            stream.pushBack(extra, token);
            return token; // 视为一个 '>'
        }
        error("期望 '>'，实际为 " + t);
        return peek();
    }

    /**
     * 消费当前 token，期望类型为 expected，否则报错
     */
    public Token expect(TokenType expected, String message) {
        if (peekType() == expected) {
            return advance();
        }
        error(message);
        return peek();
    }

    /**
     * 消费当前 token，期望类型为 expected
     */
    public Token expect(TokenType expected) {
        return expect(expected, "期望 " + expected + "，实际为 " + peekType());
    }

    /**
     * ASI (Automatic Semicolon Insertion): 消费语句结尾的分号。
     * 当前 token 为 ';' 时消费之；为 EOF 时允许省略（ASI）；
     * 否则报错。
     */
    public void consumeSemicolon() {
        if (peekType() == TokenType.DELIMITER_SEMICOLON) {
            advance();
            return;
        }
        if (peekType() == TokenType.EOF) {
            return; // ASI: 输入结束，允许省略分号
        }
        error("期望 ';'，实际为 " + peekType());
    }

    // ========== 错误收集 ==========

    public void error(String message) {
        Token current = peek();
        errors.add(new ParseError(
                current.location().getLine(),
                current.location().getColumn(),
                sourceName, message
        ));
    }

    public void error(int line, int column, String message) {
        errors.add(new ParseError(line, column, sourceName, message));
    }

    public boolean hasErrors() {
        return !errors.isEmpty();
    }

    public List<ParseError> getErrors() {
        return errors;
    }

    public String getSourceName() {
        return sourceName;
    }

    // ========== 错误记录 ==========

    public record ParseError(int line, int column, String source, String message) {
        @Override
        public String toString() {
            return source + ":" + line + ":" + column + ": " + message;
        }
    }
}
