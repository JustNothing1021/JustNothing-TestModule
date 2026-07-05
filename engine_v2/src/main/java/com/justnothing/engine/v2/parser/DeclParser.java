package com.justnothing.engine.v2.parser;

import com.justnothing.engine.v2.ast.ASTNode;
import com.justnothing.engine.v2.ast.SourceLocation;
import com.justnothing.engine.v2.ast.decl.*;
import com.justnothing.engine.v2.lexer.Keywords;
import com.justnothing.engine.v2.lexer.TokenType;
import com.justnothing.engine.v2.parser.trie.DecisionTree;
import com.justnothing.engine.v2.parser.trie.TokenStream;

import java.util.*;

import static com.justnothing.engine.v2.lexer.TokenType.*;

/**
 * 声明解析器（DecisionTree 顶层分发）。
 *
 * @author JustNothing1021
 */
public class DeclParser {

    private final ParseContext ctx;
    private final ExprParser exprParser;
    private final ClassParser classParser;
    private final DecisionTree<ParseContext, ASTNode> declTree;

    public DeclParser(ParseContext ctx, ExprParser exprParser, StmtParser stmtParser, TypeParser typeParser) {
        this.ctx = ctx;
        this.exprParser = exprParser;
        this.classParser = new ClassParser(ctx, exprParser, stmtParser, typeParser);
        this.declTree = buildDeclTree();
    }

    public ClassParser getClassParser() {
        return classParser;
    }

    private DecisionTree<ParseContext, ASTNode> buildDeclTree() {
        return DecisionTree.<ParseContext, ASTNode>builder("decl")
                .on(KEYWORD_CLASS, this::parseClassDecl)
                .on(KEYWORD_INTERFACE, this::parseClassDecl)
                .on(KEYWORD_ENUM, this::parseClassDecl)
                // record 作为上下文关键字：record Name(...) 或 record Name{...} 才是 record 声明
                .whenPredicate("recordDecl", s ->
                        s.peekType() == KEYWORD_RECORD
                            && (s.peekType(1) == IDENTIFIER || s.peekType(1) == QUALIFIED_NAME),
                        this::parseClassDecl)
                .on(KEYWORD_IMPORT, this::parseImportDecl)
                .on(KEYWORD_PACKAGE, this::parsePackageDecl)
                // function 不是关键字，用谓词检测：IDENTIFIER("function") + IDENTIFIER
                .whenPredicate("function", s ->
                        s.peekType() == IDENTIFIER
                            && Keywords.FUNCTION.equals(s.peek().text())
                            && s.peekType(1) == IDENTIFIER,
                        this::parseFunctionDecl)
                .on(KEYWORD_VAR, this::parseVarDecl)
                .on(KEYWORD_AUTO, this::parseVarDecl)
                .fallback("fieldOrMethod", this::parseFieldOrMethod)
                .build();
    }

    public ASTNode parseDeclaration() {
        return declTree.dispatch(ctx, ctx.stream());
    }

    // ========== 顶层 handler ==========

    private ASTNode parseClassDecl(ParseContext ctx, TokenStream ignored) {
        int mods = classParser.parseModifiers();
        return classParser.parseClassDecl(mods);
    }

    private MethodDecl parseFunctionDecl(ParseContext ctx, TokenStream ignored) {
        ctx.advance(); // 消费 function（IDENTIFIER 文本 "function"）
        int mods = classParser.parseModifiers();
        return classParser.parseMethodDecl(mods);
    }

    private VarDecl parseVarDecl(ParseContext ctx, TokenStream ignored) {
        SourceLocation loc = ctx.location();
        ctx.advance(); // var/auto
        String name = ctx.advance().text();
        ctx.expect(OPERATOR_ASSIGN);
        ASTNode initializer = exprParser.parse();
        ctx.match(DELIMITER_SEMICOLON);
        return new VarDecl.Builder().name(name).initializer(initializer).location(loc).build();
    }

    private ASTNode parseFieldOrMethod(ParseContext ctx, TokenStream ignored) {
        SourceLocation loc = ctx.location();

        List<AnnotationVal> anns = new ArrayList<>(ctx.parseAnnotations());
        int mods = classParser.parseModifiers();
        anns.addAll(ctx.parseAnnotations()); // 修饰符和返回类型之间的注解：public @Nullable int foo()

        // package-info.java：注解后跟 package 声明
        if (ctx.check(KEYWORD_PACKAGE)) {
            return parsePackageDecl(ctx, ignored);
        }

        // @interface 注解类型声明
        if (ctx.check(DELIMITER_AT) && ctx.peekType(1) == KEYWORD_INTERFACE) {
            return classParser.parseAnnotationTypeDecl(anns, mods);
        }

        TokenType type = ctx.peekType();

        ASTNode result;
        if (type == KEYWORD_CLASS || type == KEYWORD_INTERFACE || type == KEYWORD_ENUM
                || (type == KEYWORD_RECORD && (ctx.peekType(1) == IDENTIFIER || ctx.peekType(1) == QUALIFIED_NAME))) {
            result = classParser.parseClassDecl(mods);
        } else if (ctx.check(KEYWORD_VAR) || ctx.check(KEYWORD_AUTO)) {
            ctx.advance();
            String name = ctx.advance().text();
            ctx.expect(OPERATOR_ASSIGN);
            ASTNode init = exprParser.parse();
            ctx.match(DELIMITER_SEMICOLON);
            result = new VarDecl.Builder().name(name).initializer(init).location(loc).build();
        } else if (classParser.isMethodDeclaration()) {
            result = classParser.parseMethodDecl(mods);
        } else {
            result = classParser.parseFieldDecl(mods);
        }
        attachAnnotations(result, anns);
        return result;
    }

    static void attachAnnotations(ASTNode node, List<AnnotationVal> anns) {
        if (anns.isEmpty()) return;
        if (node instanceof ClassDecl cd) { anns.forEach(cd::addAnnotation); }
        else if (node instanceof MethodDecl md) { anns.forEach(md::addAnnotation); }
        else if (node instanceof FieldDecl fd) { anns.forEach(fd::addAnnotation); }
        else if (node instanceof VarDecl vd) { anns.forEach(vd::addAnnotation); }
    }

    // ========== import / package ==========

    private ImportDecl parseImportDecl(ParseContext ctx, TokenStream ignored) {
        SourceLocation loc = ctx.location();
        ctx.advance(); // import
        boolean isStatic = ctx.match(KEYWORD_STATIC);
        StringBuilder path = new StringBuilder(ctx.advance().text());
        while (ctx.match(OPERATOR_DOT)) {
            path.append('.').append(ctx.advance().text());
        }
        boolean isWildcard = ctx.match(OPERATOR_MULTIPLY);
        ctx.match(DELIMITER_SEMICOLON);
        return new ImportDecl.Builder()
                .importPath(path.toString()).isStatic(isStatic).isWildcard(isWildcard).location(loc).build();
    }

    private ImportDecl parsePackageDecl(ParseContext ctx, TokenStream ignored) {
        SourceLocation loc = ctx.location();
        ctx.advance();
        StringBuilder path = new StringBuilder(ctx.advance().text());
        while (ctx.match(OPERATOR_DOT)) {
            path.append('.').append(ctx.advance().text());
        }
        ctx.match(DELIMITER_SEMICOLON);
        return new ImportDecl.Builder()
                .importPath("package " + path).isStatic(false).isWildcard(false).location(loc).build();
    }

    // ========== 修饰符常量 ==========

    public static final class Modifier {
        public static final int PUBLIC       = 1;
        public static final int PRIVATE      = 1 << 1;
        public static final int PROTECTED    = 1 << 2;
        public static final int STATIC       = 1 << 3;
        public static final int FINAL        = 1 << 4;
        public static final int ABSTRACT     = 1 << 5;
        public static final int NATIVE       = 1 << 6;
        public static final int SYNCHRONIZED = 1 << 7;
        public static final int SEALED = 1 << 8;
        public static final int DEFAULT = 1 << 9;
        public static final int VOLATILE = 1 << 10;
        public static final int TRANSIENT = 1 << 11;
        public static final int STRICTFP = 1 << 12;

        static int maskOf(TokenType t) {
            return switch (t) {
                case KEYWORD_PUBLIC       -> PUBLIC;
                case KEYWORD_PRIVATE      -> PRIVATE;
                case KEYWORD_PROTECTED    -> PROTECTED;
                case KEYWORD_STATIC       -> STATIC;
                case KEYWORD_FINAL        -> FINAL;
                case KEYWORD_ABSTRACT     -> ABSTRACT;
                case KEYWORD_NATIVE       -> NATIVE;
                case KEYWORD_SYNCHRONIZED -> SYNCHRONIZED;
                case KEYWORD_SEALED       -> SEALED;
                case KEYWORD_DEFAULT      -> DEFAULT;
                case KEYWORD_VOLATILE     -> VOLATILE;
                case KEYWORD_TRANSIENT    -> TRANSIENT;
                case KEYWORD_STRICTFP     -> STRICTFP;
                default                   -> 0;
            };
        }
    }
}
