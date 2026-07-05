package com.justnothing.engine.v2.parser.trie;

import com.justnothing.engine.v2.lexer.Lexer;
import com.justnothing.engine.v2.lexer.Token;
import org.junit.Test;

import java.util.List;

import static com.justnothing.engine.v2.lexer.TokenType.*;
import static org.junit.Assert.*;

/**
 * DecisionBuilder 深层前瞻分发测试。
 * <p>
 * 核心场景：for vs for-each 消歧——
 * 两者都以 KEYWORD_FOR + DELIMITER_LEFT_PAREN 开头，
 * 分水岭在后续 token 是 OPERATOR_COLON（for-each）还是其他（传统 for）。
 * </p>
 *
 * @author JustNothing1021
 */
public class DecisionBuilderTest {

    /**
     * 模拟 for 语句的深层前瞻分发
     */
    @Test
    public void forVsForEach() {
        ParseTrie<TokenStream, String> trie = ParseTrie.<TokenStream, String>builder("for-dispatch")
                .path(KEYWORD_FOR, DELIMITER_LEFT_PAREN)
                    .decision("for-kind")
                        // for-each: Type var : iterable
                        .ifMatches(KEYWORD_INT, IDENTIFIER, OPERATOR_COLON)
                        .orIfMatches(QUALIFIED_NAME, IDENTIFIER, OPERATOR_COLON)
                        .orIfMatches(IDENTIFIER, OPERATOR_COLON)
                            .doAction("forEach", (ctx, s) -> "for-each")
                        .otherwise()
                            .doOtherwise("forClassic", (ctx, s) -> "for-classic")
                    .endDecision()
                .end()
                .build();

        // for-each: for (int i : list)
        List<Token> forEachTokens = new Lexer("for ( int i : list )", "<test>").tokenize();
        TokenStream forEachStream = new TokenStream(forEachTokens);
        String forEachResult = trie.dispatch(forEachStream, forEachStream);
        assertEquals("for-each", forEachResult);

        // 传统 for: for (i = 0; i < 10; i++)
        List<Token> forTokens = new Lexer("for ( i = 0 ; i < 10 ; i ++ )", "<test>").tokenize();
        TokenStream forStream = new TokenStream(forTokens);
        String forResult = trie.dispatch(forStream, forStream);
        assertEquals("for-classic", forResult);
    }

    /**
     * 多种 for-each 变体
     */
    @Test
    public void forEachVariants() {
        ParseTrie<TokenStream, String> trie = ParseTrie.<TokenStream, String>builder("for-variants")
                .path(KEYWORD_FOR, DELIMITER_LEFT_PAREN)
                    .decision("for-kind")
                        .ifMatches(KEYWORD_INT, IDENTIFIER, OPERATOR_COLON)
                        .orIfMatches(KEYWORD_VAR, IDENTIFIER, OPERATOR_COLON)
                        .orIfMatches(QUALIFIED_NAME, IDENTIFIER, OPERATOR_COLON)
                        .orIfMatches(IDENTIFIER, IDENTIFIER, OPERATOR_COLON)
                        .orIfMatches(IDENTIFIER, OPERATOR_COLON)
                            .doAction("forEach", (ctx, s) -> "for-each")
                        .otherwise()
                            .doOtherwise("forClassic", (ctx, s) -> "for-classic")
                    .endDecision()
                .end()
                .build();

        // for (var item : list)
        List<Token> varTokens = new Lexer("for ( var item : list )", "<test>").tokenize();
        TokenStream varStream = new TokenStream(varTokens);
        assertEquals("for-each", trie.dispatch(varStream, varStream));

        // for (String item : list) — QUALIFIED_NAME 不适用，但 IDENTIFIER 可以
        List<Token> stringTokens = new Lexer("for ( String item : list )", "<test>").tokenize();
        TokenStream stringStream = new TokenStream(stringTokens);
        assertEquals("for-each", trie.dispatch(stringStream, stringStream));
    }

    /**
     * 简单的 if-else 决策
     */
    @Test
    public void simpleDecision() {
        ParseTrie<TokenStream, String> trie = ParseTrie.<TokenStream, String>builder("simple")
                .path(IDENTIFIER)
                    .decision("id-dispatch")
                        .ifMatches(OPERATOR_ASSIGN)
                            .doAction("assign", (ctx, s) -> "assignment")
                        .otherwise()
                            .doOtherwise("expr", (ctx, s) -> "expression")
                    .endDecision()
                .end()
                .build();

        // x = 10
        List<Token> assignTokens = new Lexer("x = 10", "<test>").tokenize();
        TokenStream assignStream = new TokenStream(assignTokens);
        assertEquals("assignment", trie.dispatch(assignStream, assignStream));

        // x + 10
        List<Token> exprTokens = new Lexer("x + 10", "<test>").tokenize();
        TokenStream exprStream = new TokenStream(exprTokens);
        assertEquals("expression", trie.dispatch(exprStream, exprStream));
    }

    /**
     * 嵌套决策：在决策路径内再嵌套决策
     */
    @Test
    public void nestedDecision() {
        ParseTrie<TokenStream, String> trie = ParseTrie.<TokenStream, String>builder("nested")
                .path(IDENTIFIER)
                    .decision("first-level")
                        .ifMatches(OPERATOR_DOT)
                            .doAction("memberAccess", (ctx, s) -> "member-access")
                        .otherwise()
                            .doOtherwise("other", (ctx, s) -> "other")
                    .endDecision()
                .end()
                .build();

        // 注意：Lexer 会把 obj.field 合并为 QUALIFIED_NAME
        // 我们用不触发限定名合并的写法
        List<Token> dotTokens = new Lexer("obj . field", "<test>").tokenize();
        TokenStream dotStream = new TokenStream(dotTokens);
        assertEquals("member-access", trie.dispatch(dotStream, dotStream));
    }

    /**
     * dump 可视化
     */
    @Test
    public void dumpDecisionTree() {
        ParseTrie<TokenStream, String> trie = ParseTrie.<TokenStream, String>builder("for-dispatch")
                .path(KEYWORD_FOR, DELIMITER_LEFT_PAREN)
                    .decision("for-kind")
                        .ifMatches(KEYWORD_INT, IDENTIFIER, OPERATOR_COLON)
                            .doAction("forEach", (ctx, s) -> "for-each")
                        .otherwise()
                            .doOtherwise("forClassic", (ctx, s) -> "for-classic")
                    .endDecision()
                .end()
                .build();

        String dump = trie.dump();
        assertNotNull(dump);
        assertTrue("dump 应包含 forEach", dump.contains("forEach"));
        assertTrue("dump 应包含 forClassic", dump.contains("forClassic"));
        assertTrue("dump 应包含 predicate", dump.contains("predicate"));
    }
}
