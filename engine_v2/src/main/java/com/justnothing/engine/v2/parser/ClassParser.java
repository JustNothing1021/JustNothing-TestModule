package com.justnothing.engine.v2.parser;

import com.justnothing.engine.v2.ast.ASTNode;
import com.justnothing.engine.v2.ast.SourceLocation;
import com.justnothing.engine.v2.ast.decl.*;
import com.justnothing.engine.v2.ast.expr.VariableExpr;
import com.justnothing.engine.v2.ast.stmt.BlockStmt;
import com.justnothing.engine.v2.lexer.TokenType;
import com.justnothing.engine.v2.parser.trie.TokenMatcher;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.justnothing.engine.v2.lexer.TokenType.*;

/**
 * 类/接口/枚举体解析器。
 * <p>
 * 独立于 DeclParser，供顶层类声明和匿名类体共用。
 * </p>
 *
 * @author JustNothing1021
 */
public class ClassParser {

    private static final TokenMatcher TYPE_CLASS = TokenMatcher.any(
            KEYWORD_CLASS, KEYWORD_INTERFACE, KEYWORD_ENUM, KEYWORD_RECORD
    );

    private final ParseContext ctx;
    private final ExprParser exprParser;
    private final StmtParser stmtParser;
    private final TypeParser typeParser;

    public ClassParser(ParseContext ctx, ExprParser exprParser, StmtParser stmtParser, TypeParser typeParser) {
        this.ctx = ctx;
        this.exprParser = exprParser;
        this.stmtParser = stmtParser;
        this.typeParser = typeParser;
    }

    // ========== 类/接口/枚举 ==========

    public ClassDecl parseClassDecl(int modifiers) {
        SourceLocation loc = ctx.location();
        boolean isInterface = ctx.peekType() == KEYWORD_INTERFACE;
        boolean isEnum = ctx.peekType() == KEYWORD_ENUM;
        boolean isRecord = ctx.peekType() == KEYWORD_RECORD;
        ctx.advance(); // 消费 class/interface/enum/record

        String name = ctx.advance().text();

        // 泛型参数（在 record 组件参数之前）：record Name<T>(...)
        List<String> typeParams = Collections.emptyList();
        if (ctx.check(OPERATOR_LESS_THAN)) {
            typeParams = parseTypeParamList();
        }

        // record 组件参数：record Name(param1, param2) { ... }
        List<ParamDecl> recordComponents = Collections.emptyList();
        if (isRecord && ctx.check(DELIMITER_LEFT_PAREN)) {
            recordComponents = parseParamList();
        }

        String superClassName = null;
        List<String> interfaces = Collections.emptyList();
        if (ctx.match(KEYWORD_EXTENDS)) {
            if (isInterface) {
                // 接口多继承：interface A extends B, C, D
                interfaces = new ArrayList<>();
                do {
                    interfaces.add(typeParser.parseTypeName());
                } while (ctx.match(DELIMITER_COMMA));
            } else {
                superClassName = typeParser.parseTypeName();
            }
        }

        if (!isInterface && ctx.match(KEYWORD_IMPLEMENTS)) {
            interfaces = new ArrayList<>();
            do {
                interfaces.add(typeParser.parseTypeName());
            } while (ctx.match(DELIMITER_COMMA));
        }

        // permits A, B, C — sealed 类/接口的许可子类型
        List<String> permitted = Collections.emptyList();
        if (ctx.match(KEYWORD_PERMITS)) {
            permitted = new ArrayList<>();
            do {
                permitted.add(typeParser.parseTypeName());
            } while (ctx.match(DELIMITER_COMMA));
        }

        ctx.expect(DELIMITER_LEFT_BRACE);

        // 枚举常量
        List<String> enumConstants = null;
        if (isEnum && !ctx.check(DELIMITER_RIGHT_BRACE)) {
            enumConstants = parseEnumConstants();
        }

        // 类成员
        List<ASTNode> members = parseClassBody(name);

        ctx.expect(DELIMITER_RIGHT_BRACE);

        ClassKind kind = isInterface ? ClassKind.INTERFACE
                : isEnum ? ClassKind.ENUM
                : isRecord ? ClassKind.RECORD
                : ClassKind.CLASS;

        return new ClassDecl.Builder()
                .name(name)
                .kind(kind)
                .typeParameters(typeParams)
                .recordComponents(recordComponents)
                .superClassName(superClassName)
                .interfaces(interfaces)
                .permitted(permitted)
                .enumConstants(enumConstants)
                .modifiers(modifiers)
                .members(members)
                .location(loc).build();
    }

    /** 解析枚举常量列表，消费到分号 */
    private List<String> parseEnumConstants() {
        List<String> constants = new ArrayList<>();
        do {
            constants.add(ctx.advance().text());
            // 跳过枚举构造函数参数
            if (ctx.check(DELIMITER_LEFT_PAREN)) {
                int depth = 1;
                ctx.advance();
                while (depth > 0 && !ctx.isEOF()) {
                    if (ctx.peekType() == DELIMITER_LEFT_PAREN) depth++;
                    else if (ctx.peekType() == DELIMITER_RIGHT_PAREN) depth--;
                    ctx.advance();
                }
            }
            // 跳过枚举常量的匿名类体：NANOSECONDS { @Override ... }
            if (ctx.check(DELIMITER_LEFT_BRACE)) {
                int depth = 1;
                ctx.advance();
                while (depth > 0 && !ctx.isEOF()) {
                    if (ctx.peekType() == DELIMITER_LEFT_BRACE) depth++;
                    else if (ctx.peekType() == DELIMITER_RIGHT_BRACE) depth--;
                    ctx.advance();
                }
            }
        } while (ctx.match(DELIMITER_COMMA));
        ctx.match(DELIMITER_SEMICOLON);
        return constants;
    }

    /** 解析类体成员列表（用于具名类声明） */
    public List<ASTNode> parseClassBody(String className) {
        List<ASTNode> members = new ArrayList<>();
        while (!ctx.check(DELIMITER_RIGHT_BRACE) && !ctx.isEOF()) {
            members.add(parseClassMember(className));
        }
        return members;
    }

    /** 解析单个类体成员 */
    public ASTNode parseClassMember(String className) {
        SourceLocation loc = ctx.location();
        List<AnnotationVal> anns = new ArrayList<>(ctx.parseAnnotations());
        int mods = parseModifiers();
        anns.addAll(ctx.parseAnnotations()); // 修饰符和返回类型间的注解：public @Nullable int foo()

        // @interface 注解类型声明（作为嵌套类型出现）
        if (ctx.check(DELIMITER_AT) && ctx.peekType(1) == KEYWORD_INTERFACE) {
            return parseAnnotationTypeDecl(anns, mods);
        }

        TokenType type = ctx.peekType();

        // 嵌套类
        if (TYPE_CLASS.matches(type)) {
            ASTNode n = parseClassDecl(mods);
            DeclParser.attachAnnotations(n, anns);
            return n;
        }
        // 构造函数：标识符与类名相同，且后面直接跟 (
        // 也允许上下文关键字（var, async, await 等）作为类名/构造函数名
        // 也支持泛型构造函数：<T> ClassName(...)
        if (isConstructor(className)) {
            ASTNode n = parseConstructorDecl(mods);
            DeclParser.attachAnnotations(n, anns);
            return n;
        }
        // 方法声明检测
        if (isMethodDeclaration()) {
            ASTNode n = parseMethodDecl(mods);
            DeclParser.attachAnnotations(n, anns);
            return n;
        }
        // 匿名类构造函数回退：JADX 反编译产物如 C01381(...)
        // 当 Identifier 后面直接跟 ( 且不是方法声明时，当作构造函数处理
        if (isVarNameToken(ctx.peekType()) && ctx.peekType(1) == DELIMITER_LEFT_PAREN) {
            ASTNode n = parseConstructorDecl(mods);
            DeclParser.attachAnnotations(n, anns);
            return n;
        }
        // 初始化块：static { ... } 或 { ... }
        if (ctx.check(DELIMITER_LEFT_BRACE)) {
            BlockStmt body = stmtParser.parseBlock();
            if ((mods & DeclParser.Modifier.STATIC) != 0) {
                return new StaticInitDecl.Builder().statements(body).location(loc).build();
            } else {
                return new InstanceInitDecl.Builder().statements(body).location(loc).build();
            }
        }
        // 字段
        ASTNode n = parseFieldDecl(mods);
        DeclParser.attachAnnotations(n, anns);
        return n;
    }

    /** 解析 @interface 注解类型声明 */
    public ASTNode parseAnnotationTypeDecl(List<AnnotationVal> anns, int mods) {
        SourceLocation loc = ctx.location();
        ctx.advance(); // @
        ctx.advance(); // interface
        String name = ctx.advance().text(); // 注解类型名

        ctx.expect(DELIMITER_LEFT_BRACE);

        List<ASTNode> members = new ArrayList<>();
        while (!ctx.check(DELIMITER_RIGHT_BRACE) && !ctx.isEOF()) {
            // 复用 parseClassMember 处理嵌套类型（enum、@interface、class 等）
            // 但注解类型的普通成员是 Type name() [default value]; 格式
            SourceLocation memberLoc = ctx.location();
            List<AnnotationVal> memberAnns = new ArrayList<>(ctx.parseAnnotations());
            int memberMods = parseModifiers();
            memberAnns.addAll(ctx.parseAnnotations());

            // 嵌套 @interface
            if (ctx.check(DELIMITER_AT) && ctx.peekType(1) == KEYWORD_INTERFACE) {
                ASTNode n = parseAnnotationTypeDecl(memberAnns, memberMods);
                members.add(n);
                continue;
            }

            TokenType type = ctx.peekType();

            // 嵌套 class/interface/enum/record
            if (TYPE_CLASS.matches(type)) {
                ASTNode n = parseClassDecl(memberMods);
                DeclParser.attachAnnotations(n, memberAnns);
                members.add(n);
                continue;
            }

            // 注解类型成员：Type name() [default value]; 或常量字段：Type name = value;
            StringBuilder returnType = new StringBuilder(typeParser.parseTypeName());
            // 消费数组维度：int[]、String[] 等
            while (ctx.match(DELIMITER_LEFT_BRACKET)) {
                ctx.expect(DELIMITER_RIGHT_BRACKET);
                returnType.append("[]");
            }
            String memberName = ctx.advance().text();

            if (ctx.check(DELIMITER_LEFT_PAREN)) {
                // 注解元素：Type name() [default value];
                ctx.advance(); // (
                ctx.expect(DELIMITER_RIGHT_PAREN);

                ASTNode defaultValue = null;
                if (ctx.match(KEYWORD_DEFAULT)) {
                    // 支持 default @Annotation（JADX 反编译可能省略括号）
                    if (ctx.check(DELIMITER_AT)) {
                        defaultValue = parseAnnotationDefaultValue();
                    } else {
                        defaultValue = exprParser.parse();
                    }
                }
                ctx.expect(DELIMITER_SEMICOLON);

                MethodDecl md = new MethodDecl.Builder()
                        .name(memberName)
                        .returnType(returnType.toString())
                        .parameters(List.of())
                        .throwsList(List.of())
                        .body(null)
                        .defaultValue(defaultValue)
                        .modifiers(memberMods)
                        .location(memberLoc)
                        .build();
                DeclParser.attachAnnotations(md, memberAnns);
                members.add(md);
            } else {
                // 常量字段：Type name = value;
                ASTNode initializer = null;
                if (ctx.match(OPERATOR_ASSIGN)) {
                    initializer = exprParser.parse();
                }
                ctx.match(DELIMITER_SEMICOLON);

                FieldDecl fd = new FieldDecl.Builder()
                        .name(memberName)
                        .typeName(returnType.toString())
                        .initializer(initializer)
                        .modifiers(memberMods)
                        .location(memberLoc)
                        .build();
                DeclParser.attachAnnotations(fd, memberAnns);
                members.add(fd);
            }
        }

        ctx.expect(DELIMITER_RIGHT_BRACE);

        ClassDecl cd = new ClassDecl.Builder()
                .name(name)
                .kind(ClassKind.ANNOTATION_TYPE)
                .modifiers(mods)
                .members(members)
                .location(loc)
                .build();
        DeclParser.attachAnnotations(cd, anns);
        return cd;
    }

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

    /** 判断当前位置是否为构造函数声明（支持泛型构造函数 <T> ClassName(...)） */
    private boolean isConstructor(String className) {
        int offset = 0;
        // 跳过泛型参数 <T, U extends Foo>
        if (ctx.peekType(offset) == OPERATOR_LESS_THAN) {
            offset++;
            int depth = 1;
            while (depth > 0 && ctx.peekType(offset) != EOF) {
                TokenType t = ctx.peekType(offset);
                if (t == OPERATOR_LESS_THAN) depth++;
                else if (t == OPERATOR_UNSIGNED_RIGHT_SHIFT) depth -= 3;
                else if (t == OPERATOR_RIGHT_SHIFT) depth -= 2;
                else if (t == OPERATOR_GREATER_THAN) depth--;
                offset++;
            }
        }
        // 检查：类名 + (
        return isVarNameToken(ctx.peekType(offset))
                && ctx.text(offset).equals(className)
                && ctx.peekType(offset + 1) == DELIMITER_LEFT_PAREN;
    }

    // ========== 修饰符 ==========

    static final TokenMatcher MODIFIER = TokenMatcher.any(
            KEYWORD_PUBLIC, KEYWORD_PRIVATE, KEYWORD_PROTECTED,
            KEYWORD_STATIC, KEYWORD_FINAL, KEYWORD_ABSTRACT,
            KEYWORD_NATIVE, KEYWORD_SYNCHRONIZED, KEYWORD_SEALED,
            KEYWORD_DEFAULT, KEYWORD_VOLATILE, KEYWORD_TRANSIENT, KEYWORD_STRICTFP
    );

    public int parseModifiers() {
        int mods = 0;
        while (MODIFIER.matches(ctx.peekType())) {
            mods |= DeclParser.Modifier.maskOf(ctx.advance().type());
        }
        return mods;
    }

    // ========== 方法声明 ==========

    public MethodDecl parseMethodDecl(int modifiers) {
        SourceLocation loc = ctx.location();
        // 可选的方法泛型参数：<T, U extends Foo>
        List<String> typeParams = Collections.emptyList();
        if (ctx.check(OPERATOR_LESS_THAN)) {
            typeParams = parseTypeParamList();
        }
        String returnType = typeParser.parseTypeName();
        String name = ctx.advance().text();
        List<ParamDecl> params = parseParamList();

        List<String> throwsList = Collections.emptyList();
        if (ctx.match(KEYWORD_THROWS)) {
            throwsList = new ArrayList<>();
            do {
                throwsList.add(typeParser.parseTypeName());
            } while (ctx.match(DELIMITER_COMMA));
        }

        BlockStmt body = null;
        if (ctx.check(DELIMITER_LEFT_BRACE)) {
            body = stmtParser.parseBlock();
        } else {
            ctx.match(DELIMITER_SEMICOLON);
        }

        return new MethodDecl.Builder()
                .name(name).typeParameters(typeParams).returnType(returnType).parameters(params)
                .throwsList(throwsList).body(body).modifiers(modifiers).location(loc).build();
    }

    // ========== 构造函数 ==========

    public ConstructorDecl parseConstructorDecl(int modifiers) {
        SourceLocation loc = ctx.location();
        // 可选的泛型参数：<T> ClassName(...)
        List<String> typeParams = Collections.emptyList();
        if (ctx.check(OPERATOR_LESS_THAN)) {
            typeParams = parseTypeParamList();
        }
        String name = ctx.advance().text();
        List<ParamDecl> params = parseParamList();
        List<String> throwsList = Collections.emptyList();
        if (ctx.match(KEYWORD_THROWS)) {
            throwsList = new ArrayList<>();
            throwsList.add(typeParser.parseTypeName());
            while (ctx.match(DELIMITER_COMMA)) {
                throwsList.add(typeParser.parseTypeName());
            }
        }
        BlockStmt body = stmtParser.parseBlock();
        return new ConstructorDecl.Builder()
                .name(name).typeParameters(typeParams).parameters(params).throwsList(throwsList).body(body).modifiers(modifiers).location(loc).build();
    }

    // ========== 字段声明 ==========

    public FieldDecl parseFieldDecl(int modifiers) {
        SourceLocation loc = ctx.location();
        String fieldType = typeParser.parseTypeName();
        // 消费数组维度：String[] lines, int[][] matrix
        while (ctx.match(DELIMITER_LEFT_BRACKET)) {
            ctx.expect(DELIMITER_RIGHT_BRACKET);
            fieldType += "[]";
        }
        String name = ctx.advance().text();
        ASTNode initializer = null;
        if (ctx.match(OPERATOR_ASSIGN)) {
            initializer = exprParser.parse();
        }
        // 逗号分隔的多变量声明：int a = 1, b = 2, c;
        List<FieldDecl> additionalVars = Collections.emptyList();
        if (ctx.match(DELIMITER_COMMA)) {
            additionalVars = new ArrayList<>();
            do {
                SourceLocation extraLoc = ctx.location();
                String extraName = ctx.advance().text();
                ASTNode extraInit = null;
                if (ctx.match(OPERATOR_ASSIGN)) {
                    extraInit = exprParser.parse();
                }
                additionalVars.add(new FieldDecl.Builder()
                        .name(extraName).typeName(fieldType).initializer(extraInit)
                        .modifiers(modifiers).location(extraLoc).build());
            } while (ctx.match(DELIMITER_COMMA));
        }
        ctx.match(DELIMITER_SEMICOLON);
        return new FieldDecl.Builder()
                .name(name).typeName(fieldType).initializer(initializer)
                .additionalVars(additionalVars)
                .modifiers(modifiers).location(loc).build();
    }

    // ========== 参数列表 ==========

    public List<ParamDecl> parseParamList() {
        ctx.expect(DELIMITER_LEFT_PAREN);
        List<ParamDecl> params = new ArrayList<>();
        if (!ctx.check(DELIMITER_RIGHT_PAREN)) {
            do {
                params.add(parseParam());
            } while (ctx.match(DELIMITER_COMMA));
        }
        ctx.expect(DELIMITER_RIGHT_PAREN);
        return params;
    }

    private ParamDecl parseParam() {
        SourceLocation loc = ctx.location();
        List<AnnotationVal> anns = new ArrayList<>(ctx.parseAnnotations());
        boolean isFinal = ctx.match(KEYWORD_FINAL);
        String typeName = typeParser.parseTypeName();
        // 变长参数：TokenType... types
        boolean varargs = ctx.match(DELIMITER_ELLIPSIS);
        if (varargs) typeName += "...";
        String name = ctx.advance().text();
        ASTNode defaultValue = null;
        if (ctx.match(OPERATOR_ASSIGN)) {
            defaultValue = exprParser.parse();
        }
        return new ParamDecl.Builder()
                .name(name).typeName(typeName).defaultValue(defaultValue)
                .isFinal(isFinal).annotations(anns)
                .location(loc)
                .build();
    }

    // ========== 方法声明检测 ==========

    public boolean isMethodDeclaration() {
        int offset = 0;
        // 可选的方法泛型参数：<T, U extends Foo>
        if (ctx.peekType(offset) == OPERATOR_LESS_THAN) {
            offset++;
            int depth = 1;
            while (depth > 0 && ctx.peekType(offset) != EOF) {
                TokenType t = ctx.peekType(offset);
                if (t == OPERATOR_LESS_THAN) depth++;
                else if (t == OPERATOR_UNSIGNED_RIGHT_SHIFT) depth -= 3;
                else if (t == OPERATOR_RIGHT_SHIFT) depth -= 2;
                else if (t == OPERATOR_GREATER_THAN) depth--;
                offset++;
            }
        }
        TokenType t0 = ctx.peekType(offset);
        if (!TypeParser.GENERIC_TYPE_KIND.matches(t0)) {
            return false;
        }
        offset++;
        // 跳过泛型
        if (ctx.peekType(offset) == OPERATOR_LESS_THAN) {
            int depth = 1;
            offset++;
            while (depth > 0 && ctx.peekType(offset) != EOF) {
                TokenType t = ctx.peekType(offset);
                if (t == OPERATOR_LESS_THAN) depth++;
                else if (t == OPERATOR_UNSIGNED_RIGHT_SHIFT) depth -= 3;
                else if (t == OPERATOR_RIGHT_SHIFT) depth -= 2;
                else if (t == OPERATOR_GREATER_THAN) depth--;
                offset++;
            }
        }
        // 跳过限定名后缀：TrieBuilder<C,R>.PathBuilder → 消费 .Identifier
        while (ctx.peekType(offset) == OPERATOR_DOT
                && (ctx.peekType(offset + 1) == IDENTIFIER || ctx.peekType(offset + 1) == QUALIFIED_NAME)) {
            offset += 2;
            if (ctx.peekType(offset) == OPERATOR_LESS_THAN) {
                int depth = 1;
                offset++;
                while (depth > 0 && ctx.peekType(offset) != EOF) {
                    TokenType t = ctx.peekType(offset);
                    if (t == OPERATOR_LESS_THAN) depth++;
                    else if (t == OPERATOR_UNSIGNED_RIGHT_SHIFT) depth -= 3;
                    else if (t == OPERATOR_RIGHT_SHIFT) depth -= 2;
                    else if (t == OPERATOR_GREATER_THAN) depth--;
                    offset++;
                }
            }
        }
        // 跳过数组
        while (ctx.peekType(offset) != EOF
                && ctx.peekType(offset) == DELIMITER_LEFT_BRACKET
                && ctx.peekType(offset + 1) == DELIMITER_RIGHT_BRACKET) {
            offset += 2;
        }
        if (isVarNameToken(ctx.peekType(offset))) {
            offset++;
            return ctx.peekType(offset) == DELIMITER_LEFT_PAREN;
        }
        return false;
    }

    // ========== 泛型参数 ==========

    List<String> parseTypeParamList() {
        List<String> params = new ArrayList<>();
        ctx.advance(); // <
        do {
            StringBuilder sb = new StringBuilder(ctx.advance().text()); // param name (T, U, E, ...)
            // 消费可选的 extends 边界：<T extends Foo>
            if (ctx.match(KEYWORD_EXTENDS)) {
                sb.append(" extends ");
                sb.append(readTypeSpec());
                while (ctx.match(OPERATOR_BITWISE_AND)) { // T extends A & B
                    sb.append(" & ");
                    sb.append(readTypeSpec());
                }
            }
            params.add(sb.toString());
        } while (ctx.match(DELIMITER_COMMA));
        ctx.expectClosingAngle();
        return params;
    }

    /** 消费并返回一个类型引用文本（可能带泛型参数和数组后缀） */
    private String readTypeSpec() {
        StringBuilder sb = new StringBuilder(ctx.advance().text()); // 类型名
        if (ctx.check(OPERATOR_LESS_THAN)) {
            sb.append(typeParser.parseGenericArgs()); // 泛型参数使用（如 Comparable<? super T>），不是类型参数声明
        }
        // 限定名后缀：Outer<K,V>.Inner
        while (ctx.check(OPERATOR_DOT)
                && (ctx.peekType(1) == IDENTIFIER || ctx.peekType(1) == QUALIFIED_NAME)) {
            sb.append(ctx.advance().text()); // .
            sb.append(ctx.advance().text()); // Identifier
            if (ctx.check(OPERATOR_LESS_THAN)) {
                sb.append(typeParser.parseGenericArgs());
            }
        }
        while (ctx.match(DELIMITER_LEFT_BRACKET)) {
            ctx.expect(DELIMITER_RIGHT_BRACKET);
            sb.append("[]");
        }
        return sb.toString();
    }

    /** 解析注解默认值中的 @Annotation（JADX 反编译可能省略括号） */
    private ASTNode parseAnnotationDefaultValue() {
        SourceLocation loc = ctx.location();
        ctx.advance(); // @
        String name = ctx.advance().text(); // Annotation name
        if (ctx.check(DELIMITER_LEFT_PAREN)) {
            // 有括号，交给表达式解析器处理剩余部分
            // 但我们已经消费了 @ 和 name，所以需要构造一个简单的注解节点
            ctx.advance(); // (
            if (!ctx.check(DELIMITER_RIGHT_PAREN)) {
                exprParser.parse(); // 跳过括号内内容
            }
            ctx.expect(DELIMITER_RIGHT_PAREN);
        }
        return new VariableExpr.Builder().name("@" + name).location(loc).build();
    }
}
