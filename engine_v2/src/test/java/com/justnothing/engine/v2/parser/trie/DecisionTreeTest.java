package com.justnothing.engine.v2.parser.trie;

import com.justnothing.engine.v2.lexer.Lexer;
import com.justnothing.engine.v2.lexer.Token;
import org.junit.Test;

import java.util.List;

import static com.justnothing.engine.v2.lexer.TokenType.*;
import static org.junit.Assert.*;

/**
 * DecisionTree 增强版测试：嵌套决策 + doFromHere/doFromRoot。
 *
 * @author JustNothing1021
 */
public class DecisionTreeTest {

    // ========== for vs for-each：核心场景 ==========

    @Test
    public void forVsForEach() {
        DecisionTree<TokenStream, String> tree = DecisionTree.<TokenStream, String>builder("for-dispatch")
                .ifMatches(KEYWORD_FOR, DELIMITER_LEFT_PAREN)
                    .ifMatches(KEYWORD_INT, IDENTIFIER, OPERATOR_COLON)
                    .orIfMatches(QUALIFIED_NAME, IDENTIFIER, OPERATOR_COLON)
                    .orIfMatches(IDENTIFIER, OPERATOR_COLON)
                        .doFromHere("forEach", (ctx, s) -> "for-each:" + s.peek().text())
                    .otherwise()
                        .doOtherwiseFromRoot("forClassic", (ctx, s) -> "for-classic:" + s.peek().text())
                    .endIf()
                .endIf()
                .build();

        // for-each: for (int i : list)
        List<Token> forEachTokens = new Lexer("for ( int i : list )", "<test>").tokenize();
        TokenStream forEachStream = new TokenStream(forEachTokens);
        String forEachResult = tree.dispatch(forEachStream, forEachStream);
        // doFromHere: stream 被前进到 COLON 之后，下一个 token 是 "list"
        assertEquals("for-each:list", forEachResult);

        // 传统 for: for (i = 0; i < 10; i++)
        List<Token> forTokens = new Lexer("for ( i = 0 ; i < 10 ; i ++ )", "<test>").tokenize();
        TokenStream forStream = new TokenStream(forTokens);
        String forResult = tree.dispatch(forStream, forStream);
        // doFromRoot: stream 不前进，从头开始，第一个 token 是 "for"
        assertEquals("for-classic:for", forResult);
    }

    // ========== doFromHere 游标验证 ==========

    @Test
    public void doFromHereAdvancesStream() {
        DecisionTree<TokenStream, String> tree = DecisionTree.<TokenStream, String>builder("cursor-test")
                .ifMatches(KEYWORD_IF, DELIMITER_LEFT_PAREN)
                    .doFromHere("ifStmt", (ctx, s) -> s.peek().text())
                .endIf()
                .build();

        // if (condition)
        List<Token> tokens = new Lexer("if ( condition )", "<test>").tokenize();
        TokenStream stream = new TokenStream(tokens);
        String result = tree.dispatch(stream, stream);
        // doFromHere 前进 2（IF + LPAREN），下一个是 "condition"
        assertEquals("condition", result);
    }

    @Test
    public void doFromRootDoesNotAdvanceStream() {
        DecisionTree<TokenStream, String> tree = DecisionTree.<TokenStream, String>builder("cursor-test")
                .ifMatches(KEYWORD_IF, DELIMITER_LEFT_PAREN)
                    .doFromRoot("ifStmt", (ctx, s) -> s.peek().text())
                .endIf()
                .build();

        List<Token> tokens = new Lexer("if ( condition )", "<test>").tokenize();
        TokenStream stream = new TokenStream(tokens);
        String result = tree.dispatch(stream, stream);
        // doFromRoot 不前进，第一个 token 是 "if"
        assertEquals("if", result);
    }

    // ========== otherwise 分支的 offset ==========

    @Test
    public void otherwiseOffsetIsTrieDepth() {
        DecisionTree<TokenStream, String> tree = DecisionTree.<TokenStream, String>builder("otherwise-offset")
                .ifMatches(KEYWORD_FOR, DELIMITER_LEFT_PAREN)
                    .ifMatches(KEYWORD_INT, IDENTIFIER, OPERATOR_COLON)
                        .doFromHere("forEach", (ctx, s) -> "forEach:" + s.peek().text())
                    .otherwise()
                        // otherwise 的 offset = trie 深度 = 2 (FOR + LPAREN)
                        .doOtherwiseFromHere("forClassic", (ctx, s) -> "forClassic:" + s.peek().text())
                    .endIf()
                .endIf()
                .build();

        // 传统 for: for (i = 0; ...)
        List<Token> forTokens = new Lexer("for ( i = 0 ; i < 10 ; i ++ )", "<test>").tokenize();
        TokenStream forStream = new TokenStream(forTokens);
        String result = tree.dispatch(forStream, forStream);
        // otherwise + doFromHere: 前进 2 (FOR + LPAREN)，下一个是 "i"
        assertEquals("forClassic:i", result);
    }

    // ========== 多种 for-each 变体 ==========

    @Test
    public void forEachVariants() {
        DecisionTree<TokenStream, String> tree = DecisionTree.<TokenStream, String>builder("for-variants")
                .ifMatches(KEYWORD_FOR, DELIMITER_LEFT_PAREN)
                    .ifMatches(KEYWORD_INT, IDENTIFIER, OPERATOR_COLON)
                    .orIfMatches(KEYWORD_VAR, IDENTIFIER, OPERATOR_COLON)
                    .orIfMatches(QUALIFIED_NAME, IDENTIFIER, OPERATOR_COLON)
                    .orIfMatches(IDENTIFIER, IDENTIFIER, OPERATOR_COLON)
                    .orIfMatches(IDENTIFIER, OPERATOR_COLON)
                        .doFromHere("forEach", (ctx, s) -> "for-each")
                    .otherwise()
                        .doOtherwiseFromRoot("forClassic", (ctx, s) -> "for-classic")
                    .endIf()
                .endIf()
                .build();

        // for (int i : list)
        List<Token> intTokens = new Lexer("for ( int i : list )", "<test>").tokenize();
        assertEquals("for-each", tree.dispatch(new TokenStream(intTokens), new TokenStream(intTokens)));

        // for (var item : list)
        List<Token> varTokens = new Lexer("for ( var item : list )", "<test>").tokenize();
        assertEquals("for-each", tree.dispatch(new TokenStream(varTokens), new TokenStream(varTokens)));

        // for (String item : list) — String 是 IDENTIFIER
        List<Token> stringTokens = new Lexer("for ( String item : list )", "<test>").tokenize();
        assertEquals("for-each", tree.dispatch(new TokenStream(stringTokens), new TokenStream(stringTokens)));

        // 传统 for
        List<Token> forTokens = new Lexer("for ( i = 0 ; i < 10 ; i ++ )", "<test>").tokenize();
        assertEquals("for-classic", tree.dispatch(new TokenStream(forTokens), new TokenStream(forTokens)));
    }

    // ========== 简单 on() 映射 ==========

    @Test
    public void simpleOnMapping() {
        DecisionTree<TokenStream, String> tree = DecisionTree.<TokenStream, String>builder("simple")
                .on(KEYWORD_IF, (ctx, s) -> "if-stmt")
                .on(KEYWORD_WHILE, (ctx, s) -> "while-stmt")
                .build();

        List<Token> ifTokens = new Lexer("if ( x )", "<test>").tokenize();
        assertEquals("if-stmt", tree.dispatch(new TokenStream(ifTokens), new TokenStream(ifTokens)));

        List<Token> whileTokens = new Lexer("while ( x )", "<test>").tokenize();
        assertEquals("while-stmt", tree.dispatch(new TokenStream(whileTokens), new TokenStream(whileTokens)));
    }

    // ========== orIfMatches 可变长度 ==========

    @Test
    public void variableLengthOrIfMatches() {
        DecisionTree<TokenStream, String> tree = DecisionTree.<TokenStream, String>builder("var-len")
                .ifMatches(KEYWORD_FOR, DELIMITER_LEFT_PAREN)
                    .ifMatches(KEYWORD_INT, IDENTIFIER, OPERATOR_COLON)    // 长度 3
                    .orIfMatches(IDENTIFIER, OPERATOR_COLON)               // 长度 2
                        .doFromHere("forEach", (ctx, s) -> "for-each:" + s.peek().text())
                    .otherwise()
                        .doOtherwiseFromRoot("forClassic", (ctx, s) -> "for-classic")
                    .endIf()
                .endIf()
                .build();

        // for (int i : list) — 匹配 INT ID COLON（长度 3），前进 2+3=5
        List<Token> intTokens = new Lexer("for ( int i : list )", "<test>").tokenize();
        TokenStream intStream = new TokenStream(intTokens);
        String intResult = tree.dispatch(intStream, intStream);
        assertEquals("for-each:list", intResult);

        // for (x : list) — 匹配 ID COLON（长度 2），前进 2+2=4
        List<Token> idTokens = new Lexer("for ( x : list )", "<test>").tokenize();
        TokenStream idStream = new TokenStream(idTokens);
        String idResult = tree.dispatch(idStream, idStream);
        assertEquals("for-each:list", idResult);
    }

    // ========== dump 可视化 ==========

    @Test
    public void dumpDecisionTree() {
        DecisionTree<TokenStream, String> tree = DecisionTree.<TokenStream, String>builder("dump-test")
                .ifMatches(KEYWORD_FOR, DELIMITER_LEFT_PAREN)
                    .ifMatches(KEYWORD_INT, IDENTIFIER, OPERATOR_COLON)
                        .doFromHere("forEach", (ctx, s) -> "for-each")
                    .otherwise()
                        .doOtherwiseFromRoot("forClassic", (ctx, s) -> "for-classic")
                    .endIf()
                .endIf()
                .build();

        String dump = tree.dump();
        assertNotNull(dump);
        assertTrue("dump 应包含 forEach", dump.contains("forEach"));
        assertTrue("dump 应包含 forClassic", dump.contains("forClassic"));
        assertTrue("dump 应包含 predicate", dump.contains("predicate"));
    }

    // ========== 错误使用检测 ==========

    @Test(expected = IllegalStateException.class)
    public void unclosedIfMatchesThrows() {
        DecisionTree.<TokenStream, String>builder("error")
                .ifMatches(KEYWORD_IF)
                .build();  // 缺少 endIf
    }

    @Test(expected = IllegalStateException.class)
    public void doWithoutIfMatchesThrows() {
        DecisionTree.<TokenStream, String>builder("error")
                .doFromHere("test", (ctx, s) -> "test");  // 不在 ifMatches 内
    }

    // ========== 传统 path() API ==========

    @Test
    public void pathBuilder() {
        DecisionTree<TokenStream, String> tree = DecisionTree.<TokenStream, String>builder("path-test")
                .path(IDENTIFIER)
                    .when(OPERATOR_ASSIGN).to("assign", (ctx, s) -> "assignment")
                    .when(OPERATOR_DOT).to("member", (ctx, s) -> "member-access")
                    .fallback("exprStmt", (ctx, s) -> "expression")
                .end()
                .build();

        // x = 10
        List<Token> assignTokens = new Lexer("x = 10", "<test>").tokenize();
        assertEquals("assignment", tree.dispatch(new TokenStream(assignTokens), new TokenStream(assignTokens)));

        // obj . field（用空格避免限定名合并）
        List<Token> memberTokens = new Lexer("obj . field", "<test>").tokenize();
        assertEquals("member-access", tree.dispatch(new TokenStream(memberTokens), new TokenStream(memberTokens)));

        // x + 10
        List<Token> exprTokens = new Lexer("x + 10", "<test>").tokenize();
        assertEquals("expression", tree.dispatch(new TokenStream(exprTokens), new TokenStream(exprTokens)));
    }

    // ========== mountInto 树挂载 ==========

    @Test
    public void mountIntoBasic() {
        DecisionTree<TokenStream, String> stmtTree = DecisionTree.<TokenStream, String>builder("stmt")
                .on(KEYWORD_IF, (ctx, s) -> "if-stmt")
                .on(KEYWORD_WHILE, (ctx, s) -> "while-stmt")
                .build();

        DecisionTree<TokenStream, String> exprTree = DecisionTree.<TokenStream, String>builder("expr")
                .on(IDENTIFIER, (ctx, s) -> "expr-stmt")
                .build();

        DecisionTree<TokenStream, String> root = DecisionTree.<TokenStream, String>builder("root")
                .mountInto(stmtTree)
                .mountInto(exprTree)
                .build();

        // 关键字优先
        assertEquals("if-stmt", dispatch(root, "if ( x )"));
        assertEquals("while-stmt", dispatch(root, "while ( x )"));
        // 标识符表达式兜底
        assertEquals("expr-stmt", dispatch(root, "x + 10"));
    }

    @Test
    public void mountIntoFallbackAdoption() {
        DecisionTree<TokenStream, String> fallbackTree = DecisionTree.<TokenStream, String>builder("fallback")
                .fallback("unknown", (ctx, s) -> "unknown-stmt")
                .build();

        DecisionTree<TokenStream, String> root = DecisionTree.<TokenStream, String>builder("root")
                .mountInto(fallbackTree)
                .build();

        // 任何输入都走 fallback
        assertEquals("unknown-stmt", dispatch(root, "anything here"));
    }

    @Test
    public void mountIntoChildPriority() {
        DecisionTree<TokenStream, String> treeA = DecisionTree.<TokenStream, String>builder("a")
                .on(IDENTIFIER, (ctx, s) -> "from-a")
                .build();

        DecisionTree<TokenStream, String> treeB = DecisionTree.<TokenStream, String>builder("b")
                .on(IDENTIFIER, (ctx, s) -> "from-b")
                .build();

        // treeA 先挂，treeA 的 IDENTIFIER 子节点优先
        DecisionTree<TokenStream, String> root = DecisionTree.<TokenStream, String>builder("root")
                .mountInto(treeA)
                .mountInto(treeB)
                .build();

        assertEquals("from-a", dispatch(root, "x"));
    }

    @Test
    public void mountIntoWithPredicates() {
        // for-each 消歧决策树
        DecisionTree<TokenStream, String> forEachTree = DecisionTree.<TokenStream, String>builder("for-each")
                .ifMatches(KEYWORD_FOR, DELIMITER_LEFT_PAREN)
                    .ifMatches(KEYWORD_INT, IDENTIFIER, OPERATOR_COLON)
                    .orIfMatches(IDENTIFIER, OPERATOR_COLON)
                        .doFromHere("forEach", (ctx, s) -> "for-each")
                    .otherwise()
                        .doOtherwiseFromRoot("forClassic", (ctx, s) -> "for-classic")
                    .endIf()
                .endIf()
                .build();

        DecisionTree<TokenStream, String> stmtTree = DecisionTree.<TokenStream, String>builder("stmt")
                .on(KEYWORD_IF, (ctx, s) -> "if-stmt")
                .build();

        DecisionTree<TokenStream, String> root = DecisionTree.<TokenStream, String>builder("root")
                .mountInto(stmtTree)
                .mountInto(forEachTree)
                .build();

        // if 语句走 stmtTree
        assertEquals("if-stmt", dispatch(root, "if ( x )"));
        // for-each 走 forEachTree 的谓词匹配
        assertEquals("for-each", dispatch(root, "for ( int i : list )"));
        // 传统 for 走 otherwise
        assertEquals("for-classic", dispatch(root, "for ( i = 0 ; i < 10 ; i ++ )"));
    }

    @Test
    public void mountLazyBasic() {
        // 模拟循环引用场景：两个 builder 互相引用
        DecisionTree<TokenStream, String> exprTree = DecisionTree.<TokenStream, String>builder("expr")
                .on(IDENTIFIER, (ctx, s) -> "expr")
                .build();

        // 用 lazy 引用已经构建好的树
        DecisionTree<TokenStream, String> root = DecisionTree.<TokenStream, String>builder("root")
                .mountLazy(() -> exprTree)
                .build();

        assertEquals("expr", dispatch(root, "x"));
    }

    /** 用 Lexer 词法化并 dispatch 的快捷方法 */
    private static String dispatch(DecisionTree<TokenStream, String> tree, String source) {
        List<Token> tokens = new Lexer(source, "<test>").tokenize();
        return tree.dispatch(new TokenStream(tokens), new TokenStream(tokens));
    }
}
