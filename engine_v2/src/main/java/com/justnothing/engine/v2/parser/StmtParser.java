package com.justnothing.engine.v2.parser;

import com.justnothing.engine.v2.ast.ASTNode;
import com.justnothing.engine.v2.ast.Resolvable;
import com.justnothing.engine.v2.ast.SourceLocation;
import com.justnothing.engine.v2.ast.decl.AnnotationVal;
import com.justnothing.engine.v2.ast.decl.VarDecl;
import com.justnothing.engine.v2.ast.expr.AsyncExpr;
import com.justnothing.engine.v2.ast.stmt.*;
import com.justnothing.engine.v2.parser.trie.DecisionTree;
import com.justnothing.engine.v2.parser.trie.TokenMatcher;
import com.justnothing.engine.v2.parser.trie.TokenStream;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.justnothing.engine.v2.lexer.TokenType.*;

import com.justnothing.engine.v2.lexer.TokenType;


/**
 * 语句解析器，使用 DecisionTree 决策树分发。
 *
 * @author JustNothing1021
 */
public class StmtParser {

    /** 完整类型表示：基本类型关键字 + var/auto + async/await + 标识符 + 限定名 */
    static final TokenMatcher TYPE_KIND = TypeParser.GENERIC_TYPE_KIND;

    /** 上下文关键字：在 Java 中可作为标识符使用 */
    private static final TokenMatcher CONTEXTUAL_ID = TokenMatcher.any(
            KEYWORD_VAR, KEYWORD_AUTO, KEYWORD_ASYNC, KEYWORD_AWAIT,
            KEYWORD_RECORD, KEYWORD_SEALED, KEYWORD_YIELD,
            KEYWORD_DELETE, KEYWORD_GOTO, KEYWORD_USING,
            KEYWORD_PERMITS
    );

    /** 判断 token 类型是否可作为变量名（标识符或上下文关键字） */
    private static boolean isVarNameToken(TokenType t) {
        return t == IDENTIFIER || t == QUALIFIED_NAME || CONTEXTUAL_ID.matches(t);
    }

    private final ParseContext ctx;
    private final ExprParser exprParser;
    private final TypeParser typeParser;
    private final DecisionTree<ParseContext, ASTNode> stmtTree;
    private ClassParser classParser;

    public StmtParser(ParseContext ctx, ExprParser exprParser) {
        this.ctx = ctx;
        this.exprParser = exprParser;
        this.typeParser = new TypeParser(ctx);
        this.stmtTree = buildStmtTree();
    }

    public void setClassParser(ClassParser classParser) {
        this.classParser = classParser;
    }

    /**
     * 构建语句分发决策树。
     */
    private DecisionTree<ParseContext, ASTNode> buildStmtTree() {
        return DecisionTree.<ParseContext, ASTNode>builder("stmt")
                // === 简单关键字：匹配即消费，doFromHere 自动前进 1 个 token ===
                .onFromHere(KEYWORD_IF, this::parseIf)
                .onFromHere(KEYWORD_WHILE, this::parseWhile)
                .onFromHere(KEYWORD_DO, this::parseDoWhile)
                .onFromHere(KEYWORD_SWITCH, this::parseSwitch)
                .onFromHere(KEYWORD_TRY, this::parseTry)
                .onFromHere(KEYWORD_THROW, this::parseThrow)
                .onFromHere(KEYWORD_ASSERT, this::parseAssert)
                .onFromHere(KEYWORD_RETURN, this::parseReturn)
                .onFromHere(KEYWORD_SYNCHRONIZED, this::parseSynchronized)
                .onFromHere(KEYWORD_BREAK, this::parseBreak)
                .onFromHere(KEYWORD_CONTINUE, this::parseContinue)
                .onFromHere(DELIMITER_LEFT_BRACE, this::parseBlockBody)
                // async 语句：async { ... } 或 async stmt
                .onFromHere(KEYWORD_ASYNC, this::parseAsyncStmt)
                // 本地 record 声明：record Name(params) { ... }（record 后必须跟标识符才是声明）
                .whenPredicate("localRecordDecl", s -> s.peekType() == KEYWORD_RECORD
                        && (s.peekType(1) == IDENTIFIER || s.peekType(1) == QUALIFIED_NAME), this::parseLocalRecordDecl)
                // 标签语句：label: while (true) { ... }
                .whenPredicate("labeledStmt", s -> s.peekType() == IDENTIFIER && s.peekType(1) == OPERATOR_COLON, this::parseLabeledStmt)
                .mountInto(buildLocalVarDeclTree())
                .mountInto(buildForLoopTree())

                // === 兜底：表达式语句 ===
                .fallback("exprStmt", this::parseExprStmt)
                .build();
    }


    private DecisionTree<ParseContext, ASTNode> buildLocalVarDeclTree() {
        return DecisionTree.<ParseContext, ASTNode>builder("localVarDecl")
                // yield expr; → 视为表达式语句（语义留给 Resolver）
                .onFromHere(KEYWORD_YIELD, this::parseYieldExpr)
                // === 有注解的局部变量声明：@Annotation ... TYPE_KIND [+ 泛型] + IDENTIFIER ===
                .whenPredicate("annotatedLocalVarDecl", s -> {
                    int off = skipAnnotations(s);
                    if (off == 0) return false;
                    if (s.peekType(off) == KEYWORD_FINAL) off++;
                    TokenType t0 = s.peekType(off);
                    if (!TYPE_KIND.matches(t0)) return false;
                    int skipped = TypeParser.skipTypeSpec(s, off);
                    if (skipped < 0) return false;
                    return isVarNameToken(s.peekType(off + skipped));
                }, this::parseAnyLocalVarDecl)
                // === 无注解的局部变量声明：TYPE_KIND [+ 泛型] + IDENTIFIER ===
                .whenPredicate("localVarDecl", s -> {
                    TokenType t0 = s.peekType();
                    int off = 0;
                    if (t0 == KEYWORD_FINAL) {
                        t0 = s.peekType(1);
                        off = 1;
                    }
                    if (!TYPE_KIND.matches(t0)) return false;
                    int skipped = TypeParser.skipTypeSpec(s, off);
                    if (skipped < 0) return false;
                    int off2 = off + skipped;
                    return isVarNameToken(s.peekType(off2));
                }, this::parseAnyLocalVarDecl)
                .build();
    }

    /** 在前瞻中跳过注解：@Name 或 @Name(...)，返回跳过后的偏移量 */
    private static int skipAnnotations(TokenStream s) {
        int off = 0;
        while (s.peekType(off) == DELIMITER_AT) {
            off++; // @
            off++; // name
            if (s.peekType(off) == DELIMITER_LEFT_PAREN) {
                int paren = 1;
                off++;
                while (paren > 0 && s.peekType(off) != EOF) {
                    TokenType t = s.peekType(off);
                    if (t == DELIMITER_LEFT_PAREN) paren++;
                    else if (t == DELIMITER_RIGHT_PAREN) paren--;
                    off++;
                }
            }
        }
        return off;
    }

    /** 在 token 流上前瞻跳过泛型参数 <...>，返回跳过后的偏移量 */
    static int skipGenerics(TokenStream s, int off) {
        if (s.peekType(off) == OPERATOR_LESS_THAN) {
            int depth = 1;
            off++;
            while (depth > 0 && s.peekType(off) != EOF) {
                TokenType t = s.peekType(off);
                if (t == OPERATOR_LESS_THAN) depth++;
                else if (t == OPERATOR_UNSIGNED_RIGHT_SHIFT) depth -= 3;
                else if (t == OPERATOR_RIGHT_SHIFT) depth -= 2;
                else if (t == OPERATOR_GREATER_THAN) depth--;
                off++;
            }
        }
        return off;
    }

    static boolean isClosingAngle(TokenType t) {
        return t == OPERATOR_GREATER_THAN || t == OPERATOR_RIGHT_SHIFT
            || t == OPERATOR_UNSIGNED_RIGHT_SHIFT;
    }

    /**
     * for 语句子树：匹配 for( 前缀后交给 parseForLoop 统一消歧。
     * <p>
     * 消歧逻辑不再依赖固定 token 模式（泛型类型会打乱序列），
     * 而是通过前瞻括号/尖括号深度找到第一个裸 : 或 ; 来区分 for-each / 传统 for。
     * </p>
     */
    private DecisionTree<ParseContext, ASTNode> buildForLoopTree() {
        return DecisionTree.<ParseContext, ASTNode>builder("for")
            .ifMatches(KEYWORD_FOR, DELIMITER_LEFT_PAREN)
                .doFromRoot("forHandler", this::parseForLoop)
            .endIf()
            .build();
    }

    /**
     * 解析一条语句
     */
    public ASTNode parse() {
        return stmtTree.dispatch(ctx, ctx.stream());
    }

    // ========== 语句块 ==========

    /**
     * 解析语句块 { ... }，调用方负责消费 '{'
     */
    public BlockStmt parseBlock() {
        SourceLocation loc = ctx.location();
        ctx.expect(DELIMITER_LEFT_BRACE);
        return parseBlockBodyImpl(loc);
    }

    /**
     * 解析语句块体（{ 已被消费），供 DecisionTree 的 doFromHere 使用
     */
    private BlockStmt parseBlockBody(ParseContext ctx, TokenStream ignored) {
        SourceLocation loc = ctx.location();
        return parseBlockBodyImpl(loc);
    }

    private BlockStmt parseBlockBodyImpl(SourceLocation loc) {
        List<ASTNode> stmts = new ArrayList<>();
        while (!ctx.check(DELIMITER_RIGHT_BRACE) && !ctx.isEOF()) {
            stmts.add(parse());
        }
        ctx.expect(DELIMITER_RIGHT_BRACE);
        return new BlockStmt.Builder().statements(stmts).location(loc).build();
    }

    // ========== if ==========

    private ASTNode parseIf(ParseContext ctx, TokenStream ignored) {
        SourceLocation loc = ctx.location();
        ctx.expect(DELIMITER_LEFT_PAREN);
        ASTNode condition = exprParser.parse();
        ctx.expect(DELIMITER_RIGHT_PAREN);

        ASTNode thenBlock = parse();
        ASTNode elseBlock = null;
        if (ctx.match(KEYWORD_ELSE)) {
            elseBlock = parse();
        }
        return new IfStmt.Builder()
                .condition(condition)
                .thenBlock(thenBlock)
                .elseBlock(elseBlock)
                .location(loc).build();
    }

    // ========== while ==========

    private ASTNode parseWhile(ParseContext ctx, TokenStream ignored) {
        SourceLocation loc = ctx.location();
        ctx.expect(DELIMITER_LEFT_PAREN);
        ASTNode condition = exprParser.parse();
        ctx.expect(DELIMITER_RIGHT_PAREN);
        ASTNode body = parse();
        return new WhileStmt.Builder()
                .condition(condition)
                .body(body)
                .location(loc).build();
    }

    // ========== do-while ==========

    private ASTNode parseDoWhile(ParseContext ctx, TokenStream ignored) {
        SourceLocation loc = ctx.location();
        ASTNode body = parse();
        ctx.expect(KEYWORD_WHILE);
        ctx.expect(DELIMITER_LEFT_PAREN);
        ASTNode condition = exprParser.parse();
        ctx.expect(DELIMITER_RIGHT_PAREN);
        ctx.consumeSemicolon();
        return new DoWhileStmt.Builder()
                .body(body)
                .condition(condition)
                .location(loc).build();
    }

    // ========== for ==========

    /**
     * 统一 for 处理入口（doFromRoot 模式）。
     * <p>
     * DecisionTree 匹配了 for( 但未前进 stream，此处手动消费后，
     * 通过 TypeParser.skipTypeSpec 做泛型感知的前瞻消歧。
     * </p>
     */
    private ASTNode parseForLoop(ParseContext ctx, TokenStream ignored) {
        SourceLocation loc = ctx.location();
        ctx.advance(); // for
        ctx.advance(); // (
        if (looksLikeForEach(ctx)) return parseForEach(ctx, ignored, loc);
        return parseClassicFor(ctx, ignored, loc);
    }

    /** 用 TypeParser.skipTypeSpec 判断 for-each 模式 */
    private boolean looksLikeForEach(ParseContext ctx) {
        int off = 0;
        // 跳过 final 修饰符
        if (ctx.peekType(off) == KEYWORD_FINAL) off++;
        // 短格式：for (x : ...)
        if (isVarNameToken(ctx.peekType(off)) && ctx.peekType(off + 1) == OPERATOR_COLON)
            return true;
        // 长格式：for (TypeName [泛型] varName : ...)
        int skipped = TypeParser.skipTypeSpec(ctx.stream(), off);
        if (skipped < 0) return false;
        return isVarNameToken(ctx.peekType(off + skipped))
            && ctx.peekType(off + skipped + 1) == OPERATOR_COLON;
    }

    /**
     * for-each 解析（for 和 ( 已被 parseForLoop 消费）。
     */
    private ASTNode parseForEach(ParseContext ctx, TokenStream ignored, SourceLocation loc) {
        List<AnnotationVal> anns = ctx.parseAnnotations();
        boolean isFinal = ctx.match(KEYWORD_FINAL); // 消费可选的 final 修饰符
        String varName;
        String itemTypeName = null;
        if (ctx.peekType(1) != OPERATOR_COLON) {
            // 长格式：for (int x : list) / for (Map.Entry<K,V> e : map)
            itemTypeName = typeParser.parseTypeName();
        }
        // 短格式：for (x : list)
        varName = ctx.advance().text();
        ctx.expect(OPERATOR_COLON);
        ASTNode iterable = exprParser.parse();
        ctx.expect(DELIMITER_RIGHT_PAREN);
        ASTNode body = parse();
        ForEachStmt stmt = new ForEachStmt.Builder()
                .variableName(varName)
                .itemType(itemTypeName != null ? Resolvable.byName(itemTypeName) : null)
                .iterable(iterable)
                .body(body)
                .isFinal(isFinal)
                .location(loc).build();
        for (AnnotationVal a : anns) stmt.addAnnotation(a);
        return stmt;
    }

    /**
     * 传统 for 解析（for 和 ( 已被 parseForLoop 消费）。
     */
    private ASTNode parseClassicFor(ParseContext ctx, TokenStream ignored, SourceLocation loc) {
        ASTNode initializer = null;
        if (!ctx.check(DELIMITER_SEMICOLON)) {
            initializer = parseForInit(); // 支持 int i=0 和 i=0 两种
        }
        ctx.expect(DELIMITER_SEMICOLON);

        ASTNode condition = null;
        if (!ctx.check(DELIMITER_SEMICOLON)) {
            condition = exprParser.parse();
        }
        ctx.expect(DELIMITER_SEMICOLON);

        ASTNode update = null;
        if (!ctx.check(DELIMITER_RIGHT_PAREN)) {
            update = exprParser.parse();
        }
        ctx.expect(DELIMITER_RIGHT_PAREN);

        ASTNode body = parse();
        return new ForStmt.Builder()
                .initializer(initializer)
                .condition(condition)
                .update(update)
                .body(body)
                .location(loc).build();
    }

    /** 解析 for-init 子句：局部变量声明 或 表达式 */
    private ASTNode parseForInit() {
        SourceLocation loc = ctx.location();
        int off = 0;
        if (ctx.peekType(off) == KEYWORD_FINAL) off++;
        TokenType t0 = ctx.peekType(off);
        int skipped = TypeParser.skipTypeSpec(ctx.stream(), off);
        if (skipped > 0 && isVarNameToken(ctx.peekType(off + skipped))) {
            boolean isFinal = ctx.match(KEYWORD_FINAL); // 消费可选的 final
            String typeName = typeParser.parseTypeName();
            String varName = ctx.advance().text();
            ASTNode init = null;
            if (ctx.match(OPERATOR_ASSIGN)) {
                init = exprParser.parse();
            }
            return new VarDecl.Builder().name(varName).typeName(typeName).initializer(init).isFinal(isFinal).location(loc).build();
        }
        return exprParser.parse();
    }

    // ========== switch ==========

    private ASTNode parseSwitch(ParseContext ctx, TokenStream ignored) {
        SourceLocation loc = ctx.location();
        ctx.expect(DELIMITER_LEFT_PAREN);
        ASTNode subject = exprParser.parse();
        ctx.expect(DELIMITER_RIGHT_PAREN);
        ctx.expect(DELIMITER_LEFT_BRACE);

        List<SwitchCase> cases = new ArrayList<>();
        // switch 体中禁用 lambda 解析，避免 case X -> 被误解析为 lambda
        ctx.withoutLambda(() -> {
            while (!ctx.check(DELIMITER_RIGHT_BRACE) && !ctx.isEOF()) {
                cases.add(parseSwitchCase());
            }
        });
        ctx.expect(DELIMITER_RIGHT_BRACE);
        return new SwitchStmt.Builder()
                .subject(subject)
                .cases(cases)
                .location(loc).build();
    }

    private SwitchCase parseSwitchCase() {
        SourceLocation loc = ctx.location();
        if (ctx.match(KEYWORD_DEFAULT)) {
            return finishSwitchCase(true, List.of(), loc);
        }
        ctx.expect(KEYWORD_CASE);

        // 多值 case：case A, B, C:
        List<ASTNode> values = new ArrayList<>();
        do {
            values.add(exprParser.parse());
        } while (ctx.match(DELIMITER_COMMA));
        return finishSwitchCase(false, values, loc);
    }

    /**
     * 消费 case/default 的 : 或 ->，并解析后续体。
     */
    private SwitchCase finishSwitchCase(boolean isDefault, List<ASTNode> values, SourceLocation loc) {
        ASTNode body;
        SourceLocation bodyLoc = ctx.location();
        if (ctx.match(DELIMITER_ARROW)) {
            // Arrow 语法: case X -> expr/block/throw
            if (ctx.check(DELIMITER_LEFT_BRACE)) {
                body = parseBlock();
            } else if (ctx.check(KEYWORD_THROW)) {
                // case X -> throw expr;
                SourceLocation throwLoc = ctx.location();
                ctx.advance(); // 消费 throw
                ASTNode expr = exprParser.parse();
                ctx.consumeSemicolon();
                body = new ThrowStmt.Builder().expression(expr).location(throwLoc).build();
            } else {
                ASTNode expr = exprParser.parse();
                ctx.consumeSemicolon();
                body = new ExprStmt.Builder().expression(expr).location(bodyLoc).build();
            }
        } else {
            // 传统 : 语法
            ctx.expect(OPERATOR_COLON);
            List<ASTNode> bodyStmts = parseCaseBody();
            body = new BlockStmt.Builder().statements(bodyStmts).location(bodyLoc).build();
        }
        return new SwitchCase.Builder()
                .isDefault(isDefault).values(values).body(body).location(loc).build();
    }

    private List<ASTNode> parseCaseBody() {
        List<ASTNode> stmts = new ArrayList<>();
        while (!ctx.check(KEYWORD_CASE)
                && !ctx.check(KEYWORD_DEFAULT)
                && !ctx.check(DELIMITER_RIGHT_BRACE)
                && !ctx.isEOF()) {
            stmts.add(parse());
        }
        return stmts;
    }

    // ========== try-catch-finally ==========

    private ASTNode parseTry(ParseContext ctx, TokenStream ignored) {
        SourceLocation loc = ctx.location();
        // try-with-resources：try (resource1; resource2; ...) { }
        List<ASTNode> resources = null;
        if (ctx.check(DELIMITER_LEFT_PAREN)) {
            resources = new ArrayList<>();
            ctx.advance(); // (
            while (!ctx.check(DELIMITER_RIGHT_PAREN)) {
                if (ctx.check(KEYWORD_FINAL)) ctx.advance(); // final 修饰符
                // 区分 Type name = expr 和 单独的表达式
                TokenType t0 = ctx.peekType();
                if (TypeParser.GENERIC_TYPE_KIND.matches(t0)
                        && ctx.peekType(1) == IDENTIFIER
                        && (ctx.peekType(2) == OPERATOR_ASSIGN || ctx.peekType(2) == DELIMITER_SEMICOLON || ctx.peekType(2) == DELIMITER_RIGHT_PAREN)) {
                    SourceLocation resLoc = ctx.location();
                    String typeName = typeParser.parseTypeName(); // type
                    String varName = ctx.advance().text(); // name
                    ASTNode init = null;
                    if (ctx.match(OPERATOR_ASSIGN)) {
                        init = exprParser.parse(); // init
                    }
                    resources.add(new VarDecl.Builder().name(varName).typeName(typeName).initializer(init).location(resLoc).build());
                } else {
                    resources.add(exprParser.parse()); // expression as resource
                }
                ctx.match(DELIMITER_SEMICOLON); // Java 9+ 用分号分隔
            }
            ctx.expect(DELIMITER_RIGHT_PAREN);
        }

        ASTNode tryBlock = parseBlock();

        List<TryStmt.CatchClause> catchClauses = new ArrayList<>();
        while (ctx.match(KEYWORD_CATCH)) {
            catchClauses.add(parseCatchClause());
        }

        ASTNode finallyBlock = null;
        if (ctx.match(KEYWORD_FINALLY)) {
            finallyBlock = parseBlock();
        }

        return new TryStmt.Builder()
                .tryBlock(tryBlock)
                .catchClauses(catchClauses)
                .finallyBlock(finallyBlock)
                .resources(resources)
                .location(loc).build();
    }

    private TryStmt.CatchClause parseCatchClause() {
        ctx.expect(DELIMITER_LEFT_PAREN);
        List<String> exceptionTypes = new ArrayList<>();
        do {
            exceptionTypes.add(typeParser.parseTypeName());
        } while (ctx.match(OPERATOR_BITWISE_OR));
        String varName = ctx.advance().text();
        ctx.expect(DELIMITER_RIGHT_PAREN);
        ASTNode body = parseBlock();
        return new TryStmt.CatchClause(exceptionTypes, varName, body);
    }

    // ========== throw ==========

    private ASTNode parseThrow(ParseContext ctx, TokenStream ignored) {
        SourceLocation loc = ctx.location();
        ASTNode expr = exprParser.parse();
        ctx.consumeSemicolon();
        return new ThrowStmt.Builder().expression(expr).location(loc).build();
    }

    // ========== assert ==========

    private ASTNode parseSynchronized(ParseContext ctx, TokenStream ignored) {
        SourceLocation loc = ctx.location();
        ctx.expect(DELIMITER_LEFT_PAREN);
        ASTNode expr = exprParser.parse();
        ctx.expect(DELIMITER_RIGHT_PAREN);
        ASTNode body = parseBlock();
        return new SynchronizedStmt.Builder()
                .statements(body)
                .lock(expr)
                .location(loc).build();
    }

    private ASTNode parseAssert(ParseContext ctx, TokenStream ignored) {
        SourceLocation loc = ctx.location();
        ASTNode condition = exprParser.parse();
        ASTNode message = null;
        if (ctx.match(OPERATOR_COLON)) {
            message = exprParser.parse();
        }
        ctx.consumeSemicolon();
        return new AssertStmt.Builder()
                .condition(condition).message(message).location(loc).build();
    }

    private ASTNode parseAsyncStmt(ParseContext ctx, TokenStream ignored) {
        SourceLocation loc = ctx.location();
        // KEYWORD_ASYNC 已被 onFromHere 消费
        // 检查 async 是否作为变量名使用（如 async = stat;）
        // async 作为关键字时后面应该跟 { 或语句/表达式前缀
        TokenType next = ctx.peekType();
        if (next == OPERATOR_ASSIGN || next == OPERATOR_PLUS_ASSIGN || next == OPERATOR_MINUS_ASSIGN
                || next == OPERATOR_MULTIPLY_ASSIGN || next == OPERATOR_DIVIDE_ASSIGN
                || next == OPERATOR_DOT || next == DELIMITER_LEFT_BRACKET
                || next == OPERATOR_INCREMENT || next == OPERATOR_DECREMENT
                || next == DELIMITER_SEMICOLON) {
            // async 后面是赋值/后缀/分号，视为变量名
            ASTNode varExpr = new com.justnothing.engine.v2.ast.expr.VariableExpr.Builder()
                    .name("async").location(loc).build();
            return parseExprStmtFromExpr(varExpr, loc);
        }
        ASTNode body;
        if (ctx.check(DELIMITER_LEFT_BRACE)) {
            body = parseBlock();
        } else {
            body = parse();
        }
        return new AsyncExpr.Builder()
                .expression(body)
                .location(loc).build();
    }

    private ASTNode parseLocalRecordDecl(ParseContext ctx, TokenStream ignored) {
        // KEYWORD_RECORD 未被消费，直接委托给 ClassParser
        return classParser.parseClassDecl(0);
    }

    // ========== return ==========

    private ASTNode parseReturn(ParseContext ctx, TokenStream ignored) {
        SourceLocation loc = ctx.location();
        ASTNode value = null;
        if (!ctx.check(DELIMITER_SEMICOLON) && !ctx.check(DELIMITER_RIGHT_BRACE)) {
            value = exprParser.parse();
        }
        ctx.consumeSemicolon();
        return new ReturnStmt.Builder().value(value).location(loc).build();
    }

    // ========== break / continue ==========

    private ASTNode parseBreak(ParseContext ctx, TokenStream ignored) {
        SourceLocation loc = ctx.location();
        String label = null;
        if (ctx.peekType() == IDENTIFIER) {
            label = ctx.advance().text();
        }
        ctx.consumeSemicolon();
        return new BreakStmt.Builder().label(label).location(loc).build();
    }

    private ASTNode parseContinue(ParseContext ctx, TokenStream ignored) {
        SourceLocation loc = ctx.location();
        String label = null;
        if (ctx.peekType() == IDENTIFIER) {
            label = ctx.advance().text();
        }
        ctx.consumeSemicolon();
        return new ContinueStmt.Builder().label(label).location(loc).build();
    }

    // ========== 统一局部变量声明（类型关键字 / var / auto / 标识符类型）==========

    /**
     * yield expr 语句——暂作为表达式语句处理，语义校验留给 Resolver。
     */
    private ASTNode parseYieldExpr(ParseContext ctx, TokenStream ignored) {
        // KEYWORD_YIELD 已被 onFromHere 消费
        SourceLocation loc = ctx.location();
        ASTNode expr = exprParser.parse();
        ctx.consumeSemicolon();
        return new YieldStmt.Builder().value(expr).location(loc).build();
    }

    /**
     * 统一入口：处理所有局部变量声明。
     * <ul>
     *   <li>{@code int x;} / {@code int x = 1;} — 基本类型，初始值可选</li>
     *   <li>{@code var x = 1;} / {@code auto x = foo();} — 类型推断，必须带 =</li>
     *   <li>{@code Scanner s = ...;} / {@code String x = "hello";} — 引用类型</li>
     * </ul>
     */
    private VarDecl parseAnyLocalVarDecl(ParseContext ctx, TokenStream ignored) {
        SourceLocation loc = ctx.location();
        List<AnnotationVal> anns = ctx.parseAnnotations(); // 消费前置注解（@SuppressWarnings 等）
        boolean isFinal = ctx.match(KEYWORD_FINAL); // final 局部变量
        StringBuilder typeName = new StringBuilder(typeParser.parseTypeName()); // TypeParser 统一处理类型名+泛型
        // 消费数组维度：String[] lines, int[][] matrix
        while (ctx.match(DELIMITER_LEFT_BRACKET)) {
            ctx.expect(DELIMITER_RIGHT_BRACKET);
            typeName.append("[]");
        }
        String varName = ctx.advance().text();        // 变量名

        ASTNode initializer = null;
        if (ctx.match(OPERATOR_ASSIGN)) {
            initializer = exprParser.parse();
        }
        // 逗号分隔的多变量声明：int a = 1, b = 2;
        List<VarDecl> additionalVars = Collections.emptyList();
        if (ctx.match(DELIMITER_COMMA)) {
            additionalVars = new ArrayList<>();
            do {
                SourceLocation extraLoc = ctx.location();
                String extraName = ctx.advance().text();
                ASTNode extraInit = null;
                if (ctx.match(OPERATOR_ASSIGN)) {
                    extraInit = exprParser.parse();
                }
                additionalVars.add(new VarDecl.Builder()
                        .name(extraName).typeName(typeName.toString()).initializer(extraInit)
                        .location(extraLoc).build());
            } while (ctx.match(DELIMITER_COMMA));
        }
        ctx.consumeSemicolon();
        // var/auto 无初始化器的语义错误留给 Resolver 层
        VarDecl decl = new VarDecl.Builder()
                .name(varName).typeName(typeName.toString()).initializer(initializer)
                .isFinal(isFinal).additionalVars(additionalVars).location(loc).build();
        DeclParser.attachAnnotations(decl, anns);
        return decl;
    }

    // ========== 表达式语句（兜底） ==========

    private ASTNode parseLabeledStmt(ParseContext ctx, TokenStream ignored) {
        SourceLocation loc = ctx.location();
        String label = ctx.advance().text(); // label name
        ctx.advance(); // :
        ASTNode body = parse();
        return new LabeledStmt.Builder().label(label).body(body).location(loc).build();
    }

    private ASTNode parseExprStmt(ParseContext ctx, TokenStream ignored) {
        SourceLocation loc = ctx.location();
        ASTNode expr = exprParser.parse();
        ctx.consumeSemicolon();
        return new ExprStmt.Builder().expression(expr).location(loc).build();
    }

    /** 从已解析的前缀表达式继续解析为表达式语句（用于上下文关键字回退） */
    private ASTNode parseExprStmtFromExpr(ASTNode prefix, SourceLocation loc) {
        // 继续解析中缀运算符
        ASTNode expr = exprParser.continueFrom(prefix);
        ctx.consumeSemicolon();
        return new ExprStmt.Builder().expression(expr).location(loc).build();
    }
}
