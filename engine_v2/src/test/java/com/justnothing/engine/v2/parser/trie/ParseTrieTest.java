package com.justnothing.engine.v2.parser.trie;

import com.justnothing.engine.v2.lexer.Lexer;
import com.justnothing.engine.v2.lexer.Token;
import com.justnothing.engine.v2.lexer.TokenType;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.*;

/**
 * ParseTrie 决策树核心功能测试
 */
public class ParseTrieTest {

    // ========== 基础分发测试 ==========

    @Test
    public void simpleKeywordDispatch() {
        ParseTrie<Object, String> trie = ParseTrie.<Object, String>builder("stmt")
            .on(TokenType.KEYWORD_IF,    (ctx, s) -> "IF_STMT")
            .on(TokenType.KEYWORD_WHILE, (ctx, s) -> "WHILE_STMT")
            .on(TokenType.KEYWORD_FOR,   (ctx, s) -> "FOR_STMT")
            .build();

        assertEquals("IF_STMT", trie.dispatch(null, lex("if")));
        assertEquals("WHILE_STMT", trie.dispatch(null, lex("while")));
        assertEquals("FOR_STMT", trie.dispatch(null, lex("for")));
    }

    // ========== 多级路径测试 ==========

    @Test
    public void multiLevelPath() {
        // IDENTIFIER → ASSIGN → 赋值
        // QUALIFIED_NAME → 字段访问（Lexer 贪心合并 x.y 为单个 QUALIFIED_NAME token）
        // IDENTIFIER → fallback → 表达式语句
        ParseTrie<Object, String> trie = ParseTrie.<Object, String>builder("stmt")
            .on(TokenType.KEYWORD_IF, (ctx, s) -> "IF_STMT")
            .on(TokenType.QUALIFIED_NAME, (ctx, s) -> "FIELD_ACCESS")
            .path(TokenType.IDENTIFIER)
                .when(TokenType.OPERATOR_ASSIGN)
                    .to("assign", (ctx, s) -> "ASSIGN_STMT")
                .fallback("exprStmt", (ctx, s) -> "EXPR_STMT")
                .end()
            .build();

        // x = 1 → 赋值
        assertEquals("ASSIGN_STMT", trie.dispatch(null, lex("x = 1")));

        // x.y → 字段访问（Lexer 合并为 QUALIFIED_NAME）
        assertEquals("FIELD_ACCESS", trie.dispatch(null, lex("x.y")));

        // x + 1 → 表达式语句（fallback）
        assertEquals("EXPR_STMT", trie.dispatch(null, lex("x + 1")));
    }

    // ========== Fallback 测试 ==========

    @Test
    public void fallbackDispatch() {
        ParseTrie<Object, String> trie = ParseTrie.<Object, String>builder("stmt")
            .on(TokenType.KEYWORD_IF, (ctx, s) -> "IF_STMT")
            .fallback("default", (ctx, s) -> "DEFAULT")
            .build();

        assertEquals("DEFAULT", trie.dispatch(null, lex("x")));
    }

    // ========== 无匹配异常测试 ==========

    @Test(expected = ParseTrie.TrieDispatchException.class)
    public void noMatchThrows() {
        ParseTrie<Object, String> trie = ParseTrie.<Object, String>builder("stmt")
            .on(TokenType.KEYWORD_IF, (ctx, s) -> "IF_STMT")
            .build();

        trie.dispatch(null, lex("x"));
    }

    // ========== tryDispatch 测试 ==========

    @Test
    public void tryDispatchReturnsNullOnNoMatch() {
        ParseTrie<Object, String> trie = ParseTrie.<Object, String>builder("stmt")
            .on(TokenType.KEYWORD_IF, (ctx, s) -> "IF_STMT")
            .build();

        assertNull(trie.tryDispatch(null, lex("x")));
        assertEquals("IF_STMT", trie.tryDispatch(null, lex("if")));
    }

    // ========== 可视化测试 ==========

    @Test
    public void dumpTree() {
        ParseTrie<Object, String> trie = ParseTrie.<Object, String>builder("stmt")
            .on(TokenType.KEYWORD_IF,    (ctx, s) -> "IF_STMT")
            .on(TokenType.KEYWORD_WHILE, (ctx, s) -> "WHILE_STMT")
            .path(TokenType.IDENTIFIER)
                .when(TokenType.OPERATOR_ASSIGN)
                    .to("assign", (ctx, s) -> "ASSIGN_STMT")
                .fallback("exprStmt", (ctx, s) -> "EXPR_STMT")
                .end()
            .build();

        String dump = trie.dump();
        assertTrue(dump.contains("KEYWORD_IF"));
        assertTrue(dump.contains("KEYWORD_WHILE"));
        assertTrue(dump.contains("IDENTIFIER"));
        assertTrue(dump.contains("fallback"));
    }

    // ========== 谓词节点测试 ==========

    @Test
    public void predicateDispatch() {
        // 模拟：QUALIFIED_NAME 可能是类型名（如 java.util.Map）或字段链（如 System.out.println）
        // 用谓词根据文本内容区分
        ParseTrie<Object, String> trie = ParseTrie.<Object, String>builder("stmt")
            .on(TokenType.KEYWORD_IF, (ctx, s) -> "IF_STMT")
            .path(TokenType.QUALIFIED_NAME)
                .when(TokenType.DELIMITER_LEFT_PAREN)
                    .to("methodCall", (ctx, s) -> "METHOD_CALL")
                .whenPredicate("typeReference",
                    // 谓词：如果限定名后面跟着 < 或 空格+标识符，说明是类型引用
                    stream -> {
                        TokenType nextType = stream.peekType(1);
                        return nextType == TokenType.OPERATOR_LESS_THAN
                            || nextType == TokenType.IDENTIFIER;
                    },
                    (ctx, s) -> "TYPE_REFERENCE")
                .fallback("fieldChain", (ctx, s) -> "FIELD_CHAIN")
                .end()
            .path(TokenType.IDENTIFIER)
                .when(TokenType.OPERATOR_ASSIGN)
                    .to("assign", (ctx, s) -> "ASSIGN_STMT")
                .fallback("exprStmt", (ctx, s) -> "EXPR_STMT")
                .end()
            .build();

        // java.util.Map<String, Integer> → 类型引用（谓词匹配：< 后面）
        assertEquals("TYPE_REFERENCE", trie.dispatch(null, lex("java.util.Map<String, Integer>")));

        // java.util.Map map → 类型引用（谓词匹配：空格后是标识符）
        assertEquals("TYPE_REFERENCE", trie.dispatch(null, lex("java.util.Map map")));

        // System.out.println("hi") → 方法调用（精确匹配优先）
        assertEquals("METHOD_CALL", trie.dispatch(null, lex("System.out.println(\"hi\")")));

        // System.out.println → 字段链（fallback）
        assertEquals("FIELD_CHAIN", trie.dispatch(null, lex("System.out.println")));

        // x = 1 → 赋值
        assertEquals("ASSIGN_STMT", trie.dispatch(null, lex("x = 1")));

        // x + 1 → 表达式语句（fallback）
        assertEquals("EXPR_STMT", trie.dispatch(null, lex("x + 1")));
    }

    @Test
    public void predicateBeforeFallback() {
        // 谓词在 fallback 之前被尝试
        ParseTrie<Object, String> trie = ParseTrie.<Object, String>builder("stmt")
            .path(TokenType.IDENTIFIER)
                .whenPredicate("special",
                    stream -> stream.text().equals("special"),
                    (ctx, s) -> "SPECIAL")
                .fallback("default", (ctx, s) -> "DEFAULT")
                .end()
            .build();

        // "special" 匹配谓词
        assertEquals("SPECIAL", trie.dispatch(null, lex("special")));
        // "other" 不匹配谓词，走 fallback
        assertEquals("DEFAULT", trie.dispatch(null, lex("other")));
    }

    // ========== 辅助方法 ==========

    private static TokenStream lex(String source) {
        Lexer lexer = new Lexer(source, "<test>");
        List<Token> tokens = lexer.tokenize();
        return new TokenStream(tokens);
    }
}
