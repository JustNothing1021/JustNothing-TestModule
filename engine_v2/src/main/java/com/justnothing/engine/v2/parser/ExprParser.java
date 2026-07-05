package com.justnothing.engine.v2.parser;

import com.justnothing.engine.v2.ast.ASTNode;
import com.justnothing.engine.v2.ast.OperatorSymbol;
import com.justnothing.engine.v2.ast.SourceLocation;
import com.justnothing.engine.v2.ast.expr.*;
import com.justnothing.engine.v2.ast.stmt.*;
import com.justnothing.engine.v2.lexer.Token;
import com.justnothing.engine.v2.lexer.TokenType;
import com.justnothing.engine.v2.parser.trie.DecisionTree;
import com.justnothing.engine.v2.parser.trie.TokenStream;

import java.util.*;

import static com.justnothing.engine.v2.ast.OperatorSymbol.*;

/**
 * 表达式解析器（Pratt Parser + DecisionTree 前置分发）。
 * <p>
 * 核心思路：
 * <ul>
 *   <li>每个运算符有绑定力（binding power），值越大优先级越高</li>
 *   <li>parseExpr(minBP) 解析所有优先级 >= minBP 的表达式</li>
 *   <li>前缀运算符由 DecisionTree 分发，中缀运算符由查找表分发</li>
 * </ul>
 * </p>
 *
 * @author JustNothing1021
 */
public class ExprParser {

    private final ParseContext ctx;
    private final DecisionTree<ParseContext, ASTNode> prefixTree;
    private StmtParser stmtParser;
    private ClassParser classParser;
    private final TypeParser typeParser;

    public ExprParser(ParseContext ctx) {
        this.ctx = ctx;
        this.typeParser = new TypeParser(ctx);
        this.prefixTree = buildPrefixTree();
    }

    /** 注入 StmtParser，使 switch 表达式臂体能解析语句块 */
    public void setStmtParser(StmtParser stmtParser) {
        this.stmtParser = stmtParser;
    }

    /** 注入 ClassParser，使匿名类体能解析成员 */
    public void setClassParser(ClassParser classParser) {
        this.classParser = classParser;
    }

    /**
     * 解析表达式，最低优先级入口
     */
    public ASTNode parse() {
        return parseExpr(0);
    }

    /**
     * 从已解析的前缀节点继续解析中缀运算符（用于上下文关键字回退）。
     */
    public ASTNode continueFrom(ASTNode left) {
        left = parsePostfix(left);
        while (true) {
            InfixInfo infix = peekInfix();
            if (infix == null || infix.leftBP < 0) break;
            SourceLocation opLoc = ctx.location();
            ctx.advance();
            left = parseInfix(left, infix, opLoc);
        }
        return left;
    }

    /** 从 .class 左侧的 AST 节点提取类名字符串 */
    private static String extractClassName(ASTNode node) {
        if (node instanceof VariableExpr ve) {
            return ve.getName();
        }
        if (node instanceof FieldAccessExpr fa) {
            return extractClassName(fa.getTarget()) + "." + fa.getField().name();
        }
        return node.toString();
    }

    // ========== 前缀分发（DecisionTree） ==========

    private DecisionTree<ParseContext, ASTNode> buildPrefixTree() {
        return DecisionTree.<ParseContext, ASTNode>builder("prefix")
            // 字面量
            .on(TokenType.LITERAL_INTEGER, this::parseLiteral)
            .on(TokenType.LITERAL_LONG, this::parseLiteral)
            .on(TokenType.LITERAL_DECIMAL, this::parseLiteral)
            .on(TokenType.LITERAL_STRING, this::parseLiteral)
            .on(TokenType.LITERAL_CHAR, this::parseLiteral)
            .on(TokenType.LITERAL_BOOLEAN, this::parseLiteral)
            .on(TokenType.LITERAL_NULL, this::parseLiteral)
            // 标识符 / 限定名 / 上下文关键字（var, record, sealed, yield 在 Java 中可作为标识符）
            .on(TokenType.IDENTIFIER, this::parseNameOrCall) // <==== 这里还有 x -> expr 的lambda解析
            .on(TokenType.QUALIFIED_NAME, this::parseNameOrCall)
            .on(TokenType.KEYWORD_VAR, this::parseNameOrCall)
            .on(TokenType.KEYWORD_AUTO, this::parseNameOrCall)
            .on(TokenType.KEYWORD_RECORD, this::parseNameOrCall)
            .on(TokenType.KEYWORD_SEALED, this::parseNameOrCall)
            .on(TokenType.KEYWORD_YIELD, this::parseNameOrCall)
            .on(TokenType.KEYWORD_DELETE, this::parseNameOrCall)
            .on(TokenType.KEYWORD_GOTO, this::parseNameOrCall)
            .on(TokenType.KEYWORD_USING, this::parseNameOrCall)
            .on(TokenType.KEYWORD_PERMITS, this::parseNameOrCall)
            .on(TokenType.KEYWORD_RECORD, this::parseNameOrCall)
            // this / super / new
            .on(TokenType.KEYWORD_THIS, this::parseThis)
            .on(TokenType.KEYWORD_SUPER, this::parseSuper)
            .on(TokenType.KEYWORD_NEW, this::parseNewExpr)
            // 括号 / 数组
            .on(TokenType.DELIMITER_LEFT_PAREN, this::parseParenOrLambda)
            .on(TokenType.DELIMITER_LEFT_BRACKET, this::parseArrayLiteral)
            // 一元前缀运算符
            .on(TokenType.OPERATOR_MINUS, this::parseUnaryPrefix)
            .on(TokenType.OPERATOR_PLUS, this::parseUnaryPrefix)
            .on(TokenType.OPERATOR_LOGICAL_NOT, this::parseUnaryPrefix)
            .on(TokenType.OPERATOR_BITWISE_NOT, this::parseUnaryPrefix)
            .on(TokenType.OPERATOR_INCREMENT, this::parsePrefixIncrement)
            .on(TokenType.OPERATOR_DECREMENT, this::parsePrefixIncrement)
            // 字符串插值 + switch 表达式
            .on(TokenType.LITERAL_INTERPOLATED_STRING, this::parseInterpolatedString)
            .on(TokenType.LITERAL_MULTI_LINE_INTERPOLATED_STRING, this::parseInterpolatedString)
            .on(TokenType.LITERAL_MULTI_LINE_STRING, this::parseLiteral)
            .on(TokenType.KEYWORD_SWITCH, this::parseSwitchExpr)
            // async / await 表达式
            .on(TokenType.KEYWORD_ASYNC, this::parseAsyncExpr)
            .on(TokenType.KEYWORD_AWAIT, this::parseAwaitExpr)
            // 数组初始化器：{ expr, expr, ... }
            .on(TokenType.DELIMITER_LEFT_BRACE, this::parseArrayInitializerExpr)
            // 基本类型作为表达式（如 void.class, int.class）
            .whenPredicate("primitiveType", s -> TypeParser.PRIMITIVE.matches(s.peekType()), this::parsePrimitiveAsExpr)
            // 兜底
            .fallback("prefixError", this::parsePrefixError)
            .build();
    }

    private ASTNode parsePrefix() {
        return prefixTree.dispatch(ctx, ctx.stream());
    }

    private ASTNode parsePrimitiveAsExpr(ParseContext ctx, TokenStream ignored) {
        Token token = ctx.advance();
        return new VariableExpr.Builder().name(token.text()).location(token.location()).build();
    }

    private ASTNode parsePrefixError(ParseContext ctx, TokenStream ignored) {
        ctx.error("意外的 token: " + ctx.peekType() + " '" + ctx.text() + "'");
        SourceLocation loc = ctx.location();
        ctx.advance();
        return new LiteralExpr.Builder().value(null).type(Object.class).location(loc).build();
    }

    // ========== Pratt 核心 ==========

    /**
     * 解析表达式，最低优先级为 minBP
     */
    private ASTNode parseExpr(int minBP) {
        ASTNode left = parsePrefix();
        left = parsePostfix(left); // 所有 prefix 节点统一走一遍后缀链

        while (true) {
            InfixInfo infix = peekInfix();
            if (infix == null || infix.leftBP < minBP) break;

            SourceLocation opLoc = ctx.location();
            ctx.advance(); // 消费运算符
            left = parseInfix(left, infix, opLoc);
        }

        return left;
    }

    // ========== 字面量 ==========

    private ASTNode parseLiteral(ParseContext ctx, TokenStream ignored) {
        Token token = ctx.advance();
        Object value = token.value();
        Class<?> type = value != null ? value.getClass() : Object.class;
        return new LiteralExpr.Builder()
                .value(value)
                .type(type)
                .location(token.location())
                .build();
    }

    // ========== 标识符 / 方法调用 / 字段访问 ==========

    private ASTNode parseNameOrCall(ParseContext ctx, TokenStream ignored) {
        Token nameToken = ctx.advance();

        // 单参数无括号 Lambda：x -> expr（上下文允许时）
        if (ctx.isLambdaEnabled() && ctx.check(TokenType.DELIMITER_ARROW)) {
            ctx.advance(); // 消费 ->
            ASTNode body;
            if (ctx.check(TokenType.DELIMITER_LEFT_BRACE)) {
                body = parseBlock();
            } else {
                body = parse();
            }
            return new LambdaExpr.Builder()
                    .parameters(List.of(nameToken.text()))
                    .body(body).location(nameToken.location()).build();
        }

        return new VariableExpr.Builder()
                .name(nameToken.text())
                .location(nameToken.location())
                .build(); // parsePostfix 由 parseExpr 统一调用
    }

    // ========== 后缀链 ==========

    private ASTNode parsePostfix(ASTNode node) {
        while (true) {
            TokenType type = ctx.peekType();

            if (type == TokenType.OPERATOR_DOT || type == TokenType.OPERATOR_SAFE_DOT) {
                boolean safeAccess = type == TokenType.OPERATOR_SAFE_DOT;
                SourceLocation loc = ctx.location();
                ctx.advance(); // 消费 . 或 ?.

                // 消费可选的泛型见证 expr.<T, U>
                List<String> typeArgs = Collections.emptyList();
                if (ctx.check(TokenType.OPERATOR_LESS_THAN)) {
                    String genericArgs = typeParser.parseGenericArgs();
                    // parseGenericArgs 返回 "<T, U>" 格式，去除尖括号
                    if (genericArgs.length() > 2) {
                        String inner = genericArgs.substring(1, genericArgs.length() - 1);
                        typeArgs = List.of(inner.split(",\\s*"));
                    }
                }

                Token fieldToken = ctx.advance();
                String fieldName = fieldToken.text();

                // 方法调用？
                if (ctx.check(TokenType.DELIMITER_LEFT_PAREN)) {
                    List<ASTNode> args = parseArgumentList();
                    if (safeAccess) {
                        node = new SafeMethodCallExpr.Builder()
                                .target(node).methodName(fieldName).arguments(args)
                                .typeArguments(typeArgs).location(loc).build();
                    } else {
                        node = new MethodCallExpr.Builder()
                                .target(node).methodName(fieldName).arguments(args)
                                .typeArguments(typeArgs).location(loc).build();
                    }
                } else if (!safeAccess && fieldName.equals("class")) {
                    // Foo.class → ClassReferenceExpr
                    String className = extractClassName(node);
                    node = new ClassReferenceExpr.Builder()
                            .className(className).location(loc).build();
                } else {
                    if (safeAccess) {
                        node = new SafeFieldAccessExpr.Builder()
                                .target(node).fieldName(fieldName).location(loc).build();
                    } else {
                        node = new FieldAccessExpr.Builder()
                                .target(node).fieldName(fieldName).location(loc).build();
                    }
                }
            } else if (type == TokenType.DELIMITER_LEFT_BRACKET) {
                SourceLocation loc = ctx.location();
                // 检查是否是数组类型后缀：Object[] 或 Object[][].class
                // 如果 [ 后面紧跟 ]，这是数组类型声明而非数组访问
                if (ctx.peekType(1) == TokenType.DELIMITER_RIGHT_BRACKET) {
                    // 消费所有连续的 [] 对
                    StringBuilder arraySuffix = new StringBuilder();
                    while (ctx.check(TokenType.DELIMITER_LEFT_BRACKET)
                            && ctx.peekType(1) == TokenType.DELIMITER_RIGHT_BRACKET) {
                        ctx.advance(); // [
                        ctx.advance(); // ]
                        arraySuffix.append("[]");
                    }
                    // 将数组类型信息附加到节点名中
                    if (node instanceof VariableExpr ve) {
                        node = new VariableExpr.Builder()
                                .name(ve.getName() + arraySuffix).location(ve.getLocation()).build();
                    } else if (node instanceof FieldAccessExpr fa) {
                        // 如 com.example.Foo[].class
                        node = new FieldAccessExpr.Builder()
                                .target(fa.getTarget())
                                .fieldName(fa.getField().name() + arraySuffix)
                                .location(fa.getLocation()).build();
                    }
                    // 继续后缀链（可能还有 .class 或 ::new）
                    continue;
                }
                ctx.advance(); // 消费 [
                ASTNode index = parse();
                ctx.expect(TokenType.DELIMITER_RIGHT_BRACKET);
                node = new ArrayAccessExpr.Builder()
                        .target(node).index(index).location(loc).build();
            } else if (type == TokenType.OPERATOR_INCREMENT) {
                SourceLocation loc = ctx.location();
                ctx.advance();
                node = new UnaryOpExpr.Builder()
                        .operator(INCREMENT).operand(node).prefix(false).location(loc).build();
            } else if (type == TokenType.OPERATOR_DECREMENT) {
                SourceLocation loc = ctx.location();
                ctx.advance();
                node = new UnaryOpExpr.Builder()
                        .operator(DECREMENT).operand(node).prefix(false).location(loc).build();
            } else if (type == TokenType.DELIMITER_LEFT_PAREN && isCallTarget(node)) {
                SourceLocation loc = ctx.location();
                List<ASTNode> args = parseArgumentList();
                if (node instanceof VariableExpr ve) {
                    node = new MethodCallExpr.Builder()
                            .methodName(ve.getName()).arguments(args).location(loc).build();
                } else {
                    node = new MethodCallExpr.Builder()
                            .target(node).methodName("apply").arguments(args).location(loc).build();
                }
            } else {
                break;
            }
        }
        return node;
    }

    private boolean isCallTarget(ASTNode node) {
        return node instanceof VariableExpr || node instanceof FieldAccessExpr;
    }

    // ========== 参数列表 ==========

    private List<ASTNode> parseArgumentList() {
        ctx.expect(TokenType.DELIMITER_LEFT_PAREN);
        List<ASTNode> args = new ArrayList<>();
        if (!ctx.check(TokenType.DELIMITER_RIGHT_PAREN)) {
            do {
                args.add(parse());
            } while (ctx.match(TokenType.DELIMITER_COMMA));
        }
        ctx.expect(TokenType.DELIMITER_RIGHT_PAREN);
        return args;
    }

    // ========== this / super ==========

    private ASTNode parseThis(ParseContext ctx, TokenStream ignored) {
        Token token = ctx.advance();
        return new VariableExpr.Builder()
                .name("this").location(token.location()).build();
        // parsePostfix 由 parseExpr 统一调用
    }

    private ASTNode parseSuper(ParseContext ctx, TokenStream ignored) {
        Token token = ctx.advance(); // super

        // super(args) → 构造函数调用（仅可在构造函数首条语句）
        if (ctx.check(TokenType.DELIMITER_LEFT_PAREN)) {
            List<ASTNode> args = parseArgumentList();
            return new SuperConstructorCallExpr.Builder()
                    .arguments(args).location(token.location()).build();
        }

        // super.field / super.method() → 普通 super 引用
        // parsePostfix 由 parseExpr 统一调用
        return new SuperExpr.Builder()
                .location(token.location()).build();
    }

    // ========== switch 表达式 ==========

    private ASTNode parseSwitchExpr(ParseContext ctx, TokenStream ignored) {
        SourceLocation loc = ctx.location();
        ctx.advance(); // 消费 switch
        ctx.expect(TokenType.DELIMITER_LEFT_PAREN);
        ASTNode subject = parse();
        ctx.expect(TokenType.DELIMITER_RIGHT_PAREN);
        ctx.expect(TokenType.DELIMITER_LEFT_BRACE);

        List<SwitchCase> cases = new ArrayList<>();
        ctx.withoutLambda(() -> {
            while (!ctx.check(TokenType.DELIMITER_RIGHT_BRACE) && !ctx.isEOF()) {
                List<ASTNode> values = new ArrayList<>();
                boolean isDefault;
                if (!ctx.match(TokenType.KEYWORD_DEFAULT)) {
                    ctx.expect(TokenType.KEYWORD_CASE);
                    do {
                        values.add(parse());
                    } while (ctx.match(TokenType.DELIMITER_COMMA));
                    isDefault = false;
                } else {
                    isDefault = true;
                }
                ctx.expect(TokenType.DELIMITER_ARROW);
                SourceLocation caseLoc = ctx.location();
                cases.add(new SwitchCase.Builder()
                        .isDefault(isDefault).values(values).body(parseSwitchArmBody())
                        .location(caseLoc).build());
            }
        });

        ctx.expect(TokenType.DELIMITER_RIGHT_BRACE);
        return new SwitchStmt.Builder()
                .subject(subject).cases(cases).location(loc).build();
    }

    private ASTNode parseSwitchArmBody() {
        SourceLocation loc = ctx.location();
        if (ctx.check(TokenType.DELIMITER_LEFT_BRACE)) {
            if (stmtParser != null) {
                return stmtParser.parseBlock();
            }
            return parseBlock();
        }
        // throw 在 switch arm 中可作为"表达式"（case X -> throw ...）
        if (ctx.check(TokenType.KEYWORD_THROW) && stmtParser != null) {
            return parseThrowStmt(ctx);
        }
        ASTNode expr = parse();
        ctx.consumeSemicolon();
        return new ExprStmt.Builder().expression(expr).location(loc).build();
    }

    /** switch arm 中 inline throw 处理 */
    private ASTNode parseThrowStmt(ParseContext ctx) {
        SourceLocation loc = ctx.location();
        ctx.advance(); // 消费 throw
        ASTNode expr = parse();
        ctx.expect(TokenType.DELIMITER_SEMICOLON);
        return new com.justnothing.engine.v2.ast.stmt.ThrowStmt.Builder()
                .expression(expr).location(loc).build();
    }

    // ========== async / await 表达式 ==========

    private ASTNode parseAsyncExpr(ParseContext ctx, TokenStream ignored) {
        SourceLocation loc = ctx.location();
        // 检查 async 是否作为变量名使用（如 AsyncNode async = ... 中的 async.getName()）
        // async 作为关键字时后面应该跟 { 或表达式前缀
        TokenType next = ctx.peekType(1);
        if (next != TokenType.DELIMITER_LEFT_BRACE
                && next != TokenType.IDENTIFIER && next != TokenType.QUALIFIED_NAME
                && next != TokenType.DELIMITER_LEFT_PAREN && next != TokenType.KEYWORD_THIS
                && next != TokenType.KEYWORD_NEW && next != TokenType.KEYWORD_SUPER
                && next != TokenType.OPERATOR_MINUS && next != TokenType.OPERATOR_PLUS
                && next != TokenType.OPERATOR_LOGICAL_NOT && next != TokenType.OPERATOR_BITWISE_NOT
                && !TypeParser.PRIMITIVE.matches(next)) {
            // async 后面不跟表达式前缀，视为变量名
            Token token = ctx.advance();
            return new VariableExpr.Builder().name(token.text()).location(token.location()).build();
        }
        ctx.advance(); // 消费 async
        ASTNode expr;
        if (ctx.check(TokenType.DELIMITER_LEFT_BRACE)) {
            expr = stmtParser != null ? stmtParser.parseBlock() : parseBlock();
        } else {
            expr = parseExpr(PREFIX_BP);
        }
        return new com.justnothing.engine.v2.ast.expr.AsyncExpr.Builder()
                .expression(expr).location(loc).build();
    }

    private ASTNode parseAwaitExpr(ParseContext ctx, TokenStream ignored) {
        SourceLocation loc = ctx.location();
        // 检查 await 是否作为变量名使用（如 AwaitNode await = ... 中的 await.getExpression()）
        TokenType next = ctx.peekType(1);
        if (next != TokenType.IDENTIFIER && next != TokenType.QUALIFIED_NAME
                && next != TokenType.DELIMITER_LEFT_PAREN && next != TokenType.KEYWORD_THIS
                && next != TokenType.KEYWORD_NEW && next != TokenType.KEYWORD_SUPER
                && next != TokenType.OPERATOR_MINUS && next != TokenType.OPERATOR_PLUS
                && next != TokenType.OPERATOR_LOGICAL_NOT && next != TokenType.OPERATOR_BITWISE_NOT
                && !TypeParser.PRIMITIVE.matches(next)) {
            // await 后面不跟表达式前缀，视为变量名
            Token token = ctx.advance();
            return new VariableExpr.Builder().name(token.text()).location(token.location()).build();
        }
        ctx.advance(); // 消费 await
        ASTNode expr = parseExpr(PREFIX_BP);
        return new com.justnothing.engine.v2.ast.expr.AwaitExpr.Builder()
                .expression(expr).location(loc).build();
    }

    // ========== new 表达式 ==========

    private ASTNode parseNewExpr(ParseContext ctx, TokenStream ignored) {
        Token newToken = ctx.advance(); // 消费 new
        String className = ctx.advance().text();

        // 泛型参数：new ArrayList<String>() 或 new ArrayList<>()
        if (ctx.check(TokenType.OPERATOR_LESS_THAN)) {
            className += typeParser.parseGenericArgs();
        }

        // 数组创建：new int[5] 或 new String[]{"a","b"}
        if (ctx.check(TokenType.DELIMITER_LEFT_BRACKET)) {
            return parseNewArray(newToken, className);
        }

        List<ASTNode> args = Collections.emptyList();
        if (ctx.check(TokenType.DELIMITER_LEFT_PAREN)) {
            args = parseArgumentList();
        }

        // 匿名类体：new Foo() { ... }
        ASTNode anonymousBody = null;
        if (ctx.check(TokenType.DELIMITER_LEFT_BRACE)) {
            SourceLocation braceLoc = ctx.location();
            ctx.advance(); // {
            anonymousBody = new com.justnothing.engine.v2.ast.stmt.BlockStmt.Builder()
                    .statements(classParser.parseClassBody(className))
                    .location(braceLoc).build();
            ctx.expect(TokenType.DELIMITER_RIGHT_BRACE); // }
        }

        return new NewExpr.Builder()
                .className(className).arguments(args).anonymousBody(anonymousBody)
                .location(newToken.location()).build();
    }

    private ASTNode parseNewArray(Token newToken, String elementType) {
        List<ASTNode> dimensions = new ArrayList<>();
        while (ctx.check(TokenType.DELIMITER_LEFT_BRACKET)) {
            ctx.advance();
            if (ctx.check(TokenType.DELIMITER_RIGHT_BRACKET)) {
                ctx.advance();
                dimensions.add(null);
            } else {
                dimensions.add(parse());
                ctx.expect(TokenType.DELIMITER_RIGHT_BRACKET);
            }
        }

        List<ASTNode> initializer = null;
        if (ctx.check(TokenType.DELIMITER_LEFT_BRACE)) {
            initializer = new ArrayList<>();
            ctx.advance();
            if (!ctx.check(TokenType.DELIMITER_RIGHT_BRACE)) {
                do {
                    initializer.add(parse());
                } while (ctx.match(TokenType.DELIMITER_COMMA));
            }
            ctx.expect(TokenType.DELIMITER_RIGHT_BRACE);
        }

        return new NewArrayExpr.Builder()
                .elementType(elementType).dimensions(dimensions)
                .initializer(initializer).location(newToken.location()).build();
    }

    // ========== 括号 / Lambda ==========

    private ASTNode parseParenOrLambda(ParseContext ctx, TokenStream ignored) {
        int saved = ctx.position();
        ctx.advance(); // 消费 (
        int afterParen = ctx.position();

        // 1. 尝试类型转换：(Type) expr
        ASTNode cast = tryCast(afterParen);
        if (cast != null) return cast;

        // 2. 尝试 Lambda：(params) -> body
        LambdaExpr lambda = tryParseLambda(afterParen);
        if (lambda != null) return lambda;

        // 3. 回退，按括号表达式解析
        ctx.restore(saved);
        ctx.advance(); // 消费 (
        ASTNode expr = parse();
        ctx.expect(TokenType.DELIMITER_RIGHT_PAREN);
        return expr;
    }

    /**
     * 尝试解析为类型转换：(Type) expr。
     * 如果不是类型转换，将 stream 恢复到 afterParen（'(' 之后）位置。
     */
    private ASTNode tryCast(int afterParen) {
        SourceLocation loc = ctx.location();
        TokenType t = ctx.peekType();

        if (!TypeParser.GENERIC_TYPE_KIND.matches(t)) {
            return null;
        }

        // 先用 skipTypeSpec 做非消费前瞻，确认括号内是 Type + ) 模式
        // 避免 parseTypeName() 误将 < 当作泛型而触发幽灵错误
        int skipped = TypeParser.skipTypeSpec(ctx.stream(), 0);
        if (skipped < 0 || ctx.peekType(skipped) != TokenType.DELIMITER_RIGHT_PAREN) {
            return null;
        }

        // ) 后面不能是 -> 或 ID -> 形式（带类型标注的 Lambda 参数）
        if (ctx.peekType(skipped + 1) == TokenType.DELIMITER_ARROW
                || (ctx.peekType(skipped + 1) == TokenType.IDENTIFIER
                    && ctx.peekType(skipped + 2) == TokenType.DELIMITER_ARROW)) {
            return null;
        }

        // 前瞻确认是 cast，现在安全地实际消费
        String typeName = typeParser.parseTypeName();
        ctx.advance(); // 消费 )

        ASTNode expr = parseExpr(16);
        return new CastExpr.Builder()
                .targetType(typeName).expr(expr).location(loc).build();
    }

    private LambdaExpr tryParseLambda(int afterParen) {
        SourceLocation loc = ctx.location();
        // 无参 Lambda: () -> body
        if (ctx.check(TokenType.DELIMITER_RIGHT_PAREN)) {
            int saved = ctx.position();
            ctx.advance(); // )
            if (ctx.check(TokenType.DELIMITER_ARROW)) {
                ctx.advance(); // ->
                ASTNode body;
                if (ctx.check(TokenType.DELIMITER_LEFT_BRACE)) {
                    body = parseBlock();
                } else {
                    body = parse();
                }
                return new LambdaExpr.Builder()
                        .parameters(List.of()).body(body).location(loc).build();
            }
            ctx.restore(saved);
        }

        List<String> params = new ArrayList<>();
        boolean isLambda = false;

        if (ctx.check(TokenType.IDENTIFIER)) {
            params.add(ctx.advance().text());
            while (ctx.match(TokenType.DELIMITER_COMMA)) {
                if (!ctx.check(TokenType.IDENTIFIER)) {
                    ctx.restore(afterParen);
                    return null;
                }
                params.add(ctx.advance().text());
            }
            if (ctx.match(TokenType.DELIMITER_RIGHT_PAREN) && ctx.match(TokenType.DELIMITER_ARROW)) {
                isLambda = true;
            }
        }

        if (!isLambda) {
            ctx.restore(afterParen);
            return null;
        }

        ASTNode body;
        if (ctx.check(TokenType.DELIMITER_LEFT_BRACE)) {
            body = parseBlock();
        } else {
            body = parse();
        }
        return new LambdaExpr.Builder()
                .parameters(params).body(body).location(loc).build();
    }

    // ========== 数组字面量 ==========

    private ASTNode parseArrayLiteral(ParseContext ctx, TokenStream ignored) {
        SourceLocation loc = ctx.location();
        ctx.advance(); // 消费 [
        List<ASTNode> elements = new ArrayList<>();
        if (!ctx.check(TokenType.DELIMITER_RIGHT_BRACKET)) {
            do {
                elements.add(parse());
            } while (ctx.match(TokenType.DELIMITER_COMMA) && !ctx.check(TokenType.DELIMITER_RIGHT_BRACKET));
        }
        ctx.expect(TokenType.DELIMITER_RIGHT_BRACKET);
        return new ArrayLiteralExpr.Builder().elements(elements).location(loc).build();
    }

    // ========== 数组初始化器 { ... } ==========

    private ASTNode parseArrayInitializerExpr(ParseContext ctx, TokenStream ignored) {
        SourceLocation loc = ctx.location();
        ctx.advance(); // {
        List<ASTNode> elements = new ArrayList<>();
        if (!ctx.check(TokenType.DELIMITER_RIGHT_BRACE)) {
            do {
                elements.add(parse());
            } while (ctx.match(TokenType.DELIMITER_COMMA) && !ctx.check(TokenType.DELIMITER_RIGHT_BRACE));
        }
        ctx.expect(TokenType.DELIMITER_RIGHT_BRACE);
        return new ArrayLiteralExpr.Builder().elements(elements).location(loc).build();
    }

    // ========== 一元前缀运算符 ==========

    private ASTNode parseUnaryPrefix(ParseContext ctx, TokenStream ignored) {
        Token op = ctx.advance();
        ASTNode operand = parseExpr(PREFIX_BP);
        return new UnaryOpExpr.Builder()
                .operator(op.text()).operand(operand).prefix(true)
                .location(op.location()).build();
    }

    private ASTNode parsePrefixIncrement(ParseContext ctx, TokenStream ignored) {
        Token op = ctx.advance();
        ASTNode operand = parseExpr(PREFIX_BP);
        return new UnaryOpExpr.Builder()
                .operator(op.text()).operand(operand).prefix(true)
                .location(op.location()).build();
    }

    // ========== 字符串插值 ==========

    private ASTNode parseInterpolatedString(ParseContext ctx, TokenStream ignored) {
        Token token = ctx.advance();
        @SuppressWarnings("unchecked")
        List<Object> parts = (List<Object>) token.value();
        return new InterpolatedStringExpr.Builder()
                .parts(parts)
                .location(token.location()).build();
    }

    // ========== 中缀解析 ==========

    private ASTNode parseInfix(ASTNode left, InfixInfo infix, SourceLocation opLoc) {
        return switch (infix.kind) {
            case BINARY -> parseBinaryOp(left, infix, opLoc);
            case TERNARY -> parseTernary(left, opLoc);
            case ASSIGNMENT -> parseAssignment(left, infix, opLoc);
            case TYPE_CHECK -> parseInstanceof(left, opLoc);
            case METHOD_REF -> parseMethodReference(left, opLoc);
        };
    }

    private ASTNode parseBinaryOp(ASTNode left, InfixInfo infix, SourceLocation opLoc) {
        ASTNode right = parseExpr(infix.rightBP);
        return new BinaryOpExpr.Builder()
                .operator(infix.operator).left(left).right(right).location(opLoc).build();
    }

    private ASTNode parseTernary(ASTNode condition, SourceLocation opLoc) {
        ASTNode trueExpr = parse();
        ctx.expect(TokenType.OPERATOR_COLON);
        ASTNode falseExpr = parse();
        return new TernaryExpr.Builder()
                .condition(condition).trueExpr(trueExpr).falseExpr(falseExpr).location(opLoc).build();
    }

    private ASTNode parseAssignment(ASTNode target, InfixInfo infix, SourceLocation opLoc) {
        ASTNode value = parseExpr(infix.rightBP);
        return new AssignmentExpr.Builder()
                .target(target).operator(infix.operator).value(value).location(opLoc).build();
    }

    private ASTNode parseInstanceof(ASTNode left, SourceLocation opLoc) {
        // 消费类型名（含限定名 + 泛型）
        String typeName = typeParser.parseTypeName();
        // 模式变量：x instanceof BranchNode<C, R> br
        String bindingVar = null;
        if (ctx.check(TokenType.IDENTIFIER)
                && !TokenType.isKeyword(ctx.text())) {
            bindingVar = ctx.advance().text();
        }
        return new InstanceofExpr.Builder()
                .expr(left).typeName(typeName).bindingVar(bindingVar).location(opLoc).build();
    }

    private ASTNode parseMethodReference(ASTNode target, SourceLocation opLoc) {
        List<String> typeArgs = Collections.emptyList();
        if (ctx.check(TokenType.OPERATOR_LESS_THAN)) {
            typeArgs = parseMethodRefTypeArgs();
        }
        String methodName = ctx.advance().text();
        return new MethodReferenceExpr.Builder()
                .target(target).methodName(methodName).typeArguments(typeArgs).location(opLoc).build();
    }

    private List<String> parseMethodRefTypeArgs() {
        ctx.advance(); // 消费 <
        List<String> typeArgs = new ArrayList<>();
        do {
            typeArgs.add(ctx.advance().text());
        } while (ctx.match(TokenType.DELIMITER_COMMA));
        ctx.expect(TokenType.OPERATOR_GREATER_THAN);
        return typeArgs;
    }

    // ========== Block（供 Lambda 使用）==========

    private ASTNode parseBlock() {
        if (stmtParser != null) {
            return stmtParser.parseBlock();
        }
        SourceLocation loc = ctx.location();
        ctx.expect(TokenType.DELIMITER_LEFT_BRACE);
        List<ASTNode> stmts = new ArrayList<>();
        while (!ctx.check(TokenType.DELIMITER_RIGHT_BRACE) && !ctx.isEOF()) {
            stmts.add(parse());
        }
        ctx.expect(TokenType.DELIMITER_RIGHT_BRACE);
        return new com.justnothing.engine.v2.ast.stmt.BlockStmt.Builder()
                .statements(stmts).location(loc).build();
    }

    // ========== 优先级查找表 ==========

    private static final int PREFIX_BP = 12;

    private enum InfixKind {
        BINARY, TERNARY, ASSIGNMENT, TYPE_CHECK, METHOD_REF
    }

    private record InfixInfo(InfixKind kind, String operator, int leftBP, int rightBP) {}

    private InfixInfo peekInfix() {
        TokenType type = ctx.peekType();
        if (type == TokenType.EOF) return null;
        String text = ctx.text();

        // 赋值（右结合）
        if (text != null && OperatorSymbol.isAssignment(text)) {
            return new InfixInfo(InfixKind.ASSIGNMENT, text, 1, 1);
        }
        // 三元
        if (type == TokenType.OPERATOR_QUESTION) {
            return new InfixInfo(InfixKind.TERNARY, "?", 2, 2);
        }
        // instanceof（支持模式变量：x instanceof List<String> lst）
        if (type == TokenType.KEYWORD_INSTANCEOF) {
            return new InfixInfo(InfixKind.TYPE_CHECK, "instanceof", 6, 7);
        }
        // 方法引用 ::
        if (type == TokenType.OPERATOR_DOUBLE_COLON) {
            return new InfixInfo(InfixKind.METHOD_REF, "::", 15, 15);
        }

        // 二元运算符（保序）
        return switch (type) {
            case OPERATOR_NULL_COALESCING, OPERATOR_ELVIS     -> b(text, 3, 4);
            case OPERATOR_LOGICAL_OR                          -> b(text, 4, 5);
            case OPERATOR_LOGICAL_AND                         -> b(text, 5, 6);
            case OPERATOR_BITWISE_OR                          -> b(text, 6, 7);
            case OPERATOR_BITWISE_XOR                         -> b(text, 7, 8);
            case OPERATOR_BITWISE_AND                         -> b(text, 8, 9);
            case OPERATOR_EQUAL, OPERATOR_NOT_EQUAL,
                 OPERATOR_LESS_THAN, OPERATOR_GREATER_THAN,
                 OPERATOR_LESS_THAN_OR_EQUAL, OPERATOR_GREATER_THAN_OR_EQUAL,
                 OPERATOR_SPACESHIP                           -> b(text, 9, 10);
            case OPERATOR_RANGE, OPERATOR_RANGE_EXCLUSIVE,
                 OPERATOR_LEFT_SHIFT, OPERATOR_RIGHT_SHIFT,
                 OPERATOR_UNSIGNED_RIGHT_SHIFT                -> b(text, 10, 11);
            case OPERATOR_PLUS, OPERATOR_MINUS                -> b(text, 11, 12);
            case OPERATOR_MULTIPLY, OPERATOR_DIVIDE, OPERATOR_MODULO,
                 OPERATOR_INT_DIVIDE, OPERATOR_MATH_MODULO     -> b(text, 12, 13);
            case OPERATOR_POWER                                -> b(text, 13, 13); // 右结合
            case OPERATOR_PIPELINE                             -> b(text, 1, 2);
            default                                            -> null;
        };
    }

    private static InfixInfo b(String op, int l, int r) {
        return new InfixInfo(InfixKind.BINARY, op, l, r);
    }
}
