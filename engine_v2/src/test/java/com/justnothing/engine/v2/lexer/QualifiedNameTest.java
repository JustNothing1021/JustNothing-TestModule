package com.justnothing.engine.v2.lexer;

import org.junit.Test;

import java.util.List;

import static org.junit.Assert.*;

/**
 * Lexer 限定名贪心解析测试
 */
public class QualifiedNameTest {

    @Test
    public void simpleIdentifier() {
        // 单个标识符 → IDENTIFIER
        List<Token> tokens = lex("x");
        assertEquals(TokenType.IDENTIFIER, tokens.get(0).type());
        assertEquals("x", tokens.get(0).text());
    }

    @Test
    public void qualifiedName() {
        // java.util.Map → QUALIFIED_NAME
        List<Token> tokens = lex("java.util.Map");
        assertEquals(TokenType.QUALIFIED_NAME, tokens.get(0).type());
        assertEquals("java.util.Map", tokens.get(0).text());
    }

    @Test
    public void threePartQualifiedName() {
        // java.util.HashMap → QUALIFIED_NAME
        List<Token> tokens = lex("java.util.HashMap");
        assertEquals(TokenType.QUALIFIED_NAME, tokens.get(0).type());
        assertEquals("java.util.HashMap", tokens.get(0).text());
    }

    @Test
    public void fieldChain() {
        // System.out.println → QUALIFIED_NAME
        List<Token> tokens = lex("System.out.println");
        assertEquals(TokenType.QUALIFIED_NAME, tokens.get(0).type());
        assertEquals("System.out.println", tokens.get(0).text());
    }

    @Test
    public void innerClass() {
        // Map.Entry → QUALIFIED_NAME
        List<Token> tokens = lex("Map.Entry");
        assertEquals(TokenType.QUALIFIED_NAME, tokens.get(0).type());
        assertEquals("Map.Entry", tokens.get(0).text());
    }

    @Test
    public void qualifiedNameFollowedByGeneric() {
        // java.util.Map<String, Integer>
        // 限定名到 Map 为止，< 是下一个 token
        List<Token> tokens = lex("java.util.Map<String, Integer>");
        assertEquals(TokenType.QUALIFIED_NAME, tokens.get(0).type());
        assertEquals("java.util.Map", tokens.get(0).text());
        assertEquals(TokenType.OPERATOR_LESS_THAN, tokens.get(1).type());
    }

    @Test
    public void qualifiedNameFollowedByMethodCall() {
        // System.out.println("hello")
        // 限定名到 println 为止，( 是下一个 token
        List<Token> tokens = lex("System.out.println(\"hello\")");
        assertEquals(TokenType.QUALIFIED_NAME, tokens.get(0).type());
        assertEquals("System.out.println", tokens.get(0).text());
        assertEquals(TokenType.DELIMITER_LEFT_PAREN, tokens.get(1).type());
    }

    @Test
    public void qualifiedNameFollowedByAssignment() {
        // java.util.Map map = ...
        // 限定名到 Map 为止，空格后是 IDENTIFIER
        List<Token> tokens = lex("java.util.Map map");
        assertEquals(TokenType.QUALIFIED_NAME, tokens.get(0).type());
        assertEquals("java.util.Map", tokens.get(0).text());
        assertEquals(TokenType.IDENTIFIER, tokens.get(1).type());
        assertEquals("map", tokens.get(1).text());
    }

    @Test
    public void dotNotFollowedByIdentifier() {
        // x.5 → x 是 IDENTIFIER，. 是 DOT，5 是数字
        List<Token> tokens = lex("x.5");
        assertEquals(TokenType.IDENTIFIER, tokens.get(0).type());
        assertEquals("x", tokens.get(0).text());
        // .5 会被解析为小数 0.5
    }

    @Test
    public void doubleDotRange() {
        // 1..10 → 不应该被限定名干扰
        List<Token> tokens = lex("1..10");
        assertEquals(TokenType.LITERAL_INTEGER, tokens.get(0).type());
        assertEquals(TokenType.OPERATOR_RANGE, tokens.get(1).type());
    }

    @Test
    public void methodReference() {
        // String::valueOf → String 是 IDENTIFIER，:: 是 DOUBLE_COLON
        List<Token> tokens = lex("String::valueOf");
        assertEquals(TokenType.IDENTIFIER, tokens.get(0).type());
        assertEquals("String", tokens.get(0).text());
        assertEquals(TokenType.OPERATOR_DOUBLE_COLON, tokens.get(1).type());
        assertEquals(TokenType.IDENTIFIER, tokens.get(2).type());
        assertEquals("valueOf", tokens.get(2).text());
    }

    @Test
    public void qualifiedMethodReference() {
        // java.util.Objects::requireNonNull
        List<Token> tokens = lex("java.util.Objects::requireNonNull");
        assertEquals(TokenType.QUALIFIED_NAME, tokens.get(0).type());
        assertEquals("java.util.Objects", tokens.get(0).text());
        assertEquals(TokenType.OPERATOR_DOUBLE_COLON, tokens.get(1).type());
        assertEquals(TokenType.IDENTIFIER, tokens.get(2).type());
        assertEquals("requireNonNull", tokens.get(2).text());
    }

    @Test
    public void keywordNotQualifiedName() {
        // if → KEYWORD_IF，不是 IDENTIFIER 也不是 QUALIFIED_NAME
        List<Token> tokens = lex("if");
        assertEquals(TokenType.KEYWORD_IF, tokens.get(0).type());
    }

    @Test
    public void varDeclaration() {
        // auto map = java.util.HashMap.new
        List<Token> tokens = lex("auto map = java.util.HashMap.new");
        assertEquals(TokenType.KEYWORD_AUTO, tokens.get(0).type());
        assertEquals(TokenType.IDENTIFIER, tokens.get(1).type());
        assertEquals("map", tokens.get(1).text());
        assertEquals(TokenType.OPERATOR_ASSIGN, tokens.get(2).type());
        assertEquals(TokenType.QUALIFIED_NAME, tokens.get(3).type());
        assertEquals("java.util.HashMap.new", tokens.get(3).text());
    }

    // ========== 方法引用 + 泛型参数 ==========

    @Test
    public void methodReferenceWithGenericArgs() {
        // Comparator::<String>comparing
        // :: 后面跟泛型参数，再跟方法名
        List<Token> tokens = lex("Comparator::<String>comparing");
        assertEquals(TokenType.IDENTIFIER, tokens.get(0).type());
        assertEquals("Comparator", tokens.get(0).text());
        assertEquals(TokenType.OPERATOR_DOUBLE_COLON, tokens.get(1).type());
        assertEquals(TokenType.OPERATOR_LESS_THAN, tokens.get(2).type());
        assertEquals(TokenType.IDENTIFIER, tokens.get(3).type());
        assertEquals("String", tokens.get(3).text());
        assertEquals(TokenType.OPERATOR_GREATER_THAN, tokens.get(4).type());
        assertEquals(TokenType.IDENTIFIER, tokens.get(5).type());
        assertEquals("comparing", tokens.get(5).text());
    }

    @Test
    public void qualifiedMethodReferenceWithGenericArgs() {
        // java.util.function.Function::<String, Integer>apply
        List<Token> tokens = lex("java.util.function.Function::<String, Integer>apply");
        assertEquals(TokenType.QUALIFIED_NAME, tokens.get(0).type());
        assertEquals("java.util.function.Function", tokens.get(0).text());
        assertEquals(TokenType.OPERATOR_DOUBLE_COLON, tokens.get(1).type());
        assertEquals(TokenType.OPERATOR_LESS_THAN, tokens.get(2).type());
        assertEquals(TokenType.IDENTIFIER, tokens.get(3).type());
        assertEquals("String", tokens.get(3).text());
        assertEquals(TokenType.DELIMITER_COMMA, tokens.get(4).type());
        assertEquals(TokenType.IDENTIFIER, tokens.get(5).type());
        assertEquals("Integer", tokens.get(5).text());
        assertEquals(TokenType.OPERATOR_GREATER_THAN, tokens.get(6).type());
        assertEquals(TokenType.IDENTIFIER, tokens.get(7).type());
        assertEquals("apply", tokens.get(7).text());
    }

    @Test
    public void methodReferenceWithNestedGeneric() {
        // Supplier::<List<String>>get
        // 注意：>> 会被 Lexer 解析为 OPERATOR_RIGHT_SHIFT，这是已知的角括号问题
        // Parser 层需要特殊处理
        List<Token> tokens = lex("Supplier::<List<String>>get");
        assertEquals(TokenType.IDENTIFIER, tokens.get(0).type());
        assertEquals("Supplier", tokens.get(0).text());
        assertEquals(TokenType.OPERATOR_DOUBLE_COLON, tokens.get(1).type());
        assertEquals(TokenType.OPERATOR_LESS_THAN, tokens.get(2).type());
        assertEquals(TokenType.IDENTIFIER, tokens.get(3).type());
        assertEquals("List", tokens.get(3).text());
        assertEquals(TokenType.OPERATOR_LESS_THAN, tokens.get(4).type());
        assertEquals(TokenType.IDENTIFIER, tokens.get(5).type());
        assertEquals("String", tokens.get(5).text());
        // >> 会被解析为 OPERATOR_RIGHT_SHIFT（已知限制）
        assertEquals(TokenType.OPERATOR_RIGHT_SHIFT, tokens.get(6).type());
        assertEquals(TokenType.IDENTIFIER, tokens.get(7).type());
        assertEquals("get", tokens.get(7).text());
    }

    @Test
    public void innerClassMethodReference() {
        // Map.Entry::<String>getKey
        List<Token> tokens = lex("Map.Entry::<String>getKey");
        assertEquals(TokenType.QUALIFIED_NAME, tokens.get(0).type());
        assertEquals("Map.Entry", tokens.get(0).text());
        assertEquals(TokenType.OPERATOR_DOUBLE_COLON, tokens.get(1).type());
        assertEquals(TokenType.OPERATOR_LESS_THAN, tokens.get(2).type());
        assertEquals(TokenType.IDENTIFIER, tokens.get(3).type());
        assertEquals("String", tokens.get(3).text());
        assertEquals(TokenType.OPERATOR_GREATER_THAN, tokens.get(4).type());
        assertEquals(TokenType.IDENTIFIER, tokens.get(5).type());
        assertEquals("getKey", tokens.get(5).text());
    }

    @Test
    public void constructorReference() {
        // ArrayList::new → 构造方法引用
        List<Token> tokens = lex("ArrayList::new");
        assertEquals(TokenType.IDENTIFIER, tokens.get(0).type());
        assertEquals("ArrayList", tokens.get(0).text());
        assertEquals(TokenType.OPERATOR_DOUBLE_COLON, tokens.get(1).type());
        assertEquals(TokenType.KEYWORD_NEW, tokens.get(2).type());
    }

    @Test
    public void qualifiedConstructorReference() {
        // java.util.ArrayList::new
        List<Token> tokens = lex("java.util.ArrayList::new");
        assertEquals(TokenType.QUALIFIED_NAME, tokens.get(0).type());
        assertEquals("java.util.ArrayList", tokens.get(0).text());
        assertEquals(TokenType.OPERATOR_DOUBLE_COLON, tokens.get(1).type());
        assertEquals(TokenType.KEYWORD_NEW, tokens.get(2).type());
    }

    // ========== 辅助方法 ==========

    private static List<Token> lex(String source) {
        Lexer lexer = new Lexer(source, "<test>");
        return lexer.tokenize();
    }
}
