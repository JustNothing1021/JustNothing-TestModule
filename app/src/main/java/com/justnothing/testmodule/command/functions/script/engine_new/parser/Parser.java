package com.justnothing.testmodule.command.functions.script.engine_new.parser;

import com.justnothing.testmodule.command.functions.script.engine_new.ast.GenericType;
import com.justnothing.testmodule.command.functions.script.engine_new.ast.SourceLocation;
import com.justnothing.testmodule.command.functions.script.engine_new.ast.ASTNode;
import com.justnothing.testmodule.command.functions.script.engine_new.ast.nodes.ArrayAccessNode;
import com.justnothing.testmodule.command.functions.script.engine_new.ast.nodes.ArrayAssignmentNode;
import com.justnothing.testmodule.command.functions.script.engine_new.ast.nodes.ArrayLiteralNode;
import com.justnothing.testmodule.command.functions.script.engine_new.ast.nodes.AssignmentNode;
import com.justnothing.testmodule.command.functions.script.engine_new.ast.nodes.AsyncNode;
import com.justnothing.testmodule.command.functions.script.engine_new.ast.nodes.AwaitNode;
import com.justnothing.testmodule.command.functions.script.engine_new.ast.nodes.BinaryOpNode;
import com.justnothing.testmodule.command.functions.script.engine_new.ast.nodes.BlockNode;
import com.justnothing.testmodule.command.functions.script.engine_new.ast.nodes.CaseNode;
import com.justnothing.testmodule.command.functions.script.engine_new.ast.nodes.CastNode;
import com.justnothing.testmodule.command.functions.script.engine_new.ast.nodes.CatchClause;
import com.justnothing.testmodule.command.functions.script.engine_new.ast.nodes.ClassDeclarationNode;
import com.justnothing.testmodule.command.functions.script.engine_new.ast.nodes.ClassModifiers;
import com.justnothing.testmodule.command.functions.script.engine_new.ast.nodes.ClassReferenceNode;
import com.justnothing.testmodule.command.functions.script.engine_new.ast.nodes.ConditionalAssignNode;
import com.justnothing.testmodule.command.functions.script.engine_new.ast.nodes.ConstructorCallNode;
import com.justnothing.testmodule.command.functions.script.engine_new.ast.nodes.ConstructorDeclarationNode;
import com.justnothing.testmodule.command.functions.script.engine_new.ast.nodes.DeleteNode;
import com.justnothing.testmodule.command.functions.script.engine_new.ast.nodes.DoWhileNode;
import com.justnothing.testmodule.command.functions.script.engine_new.ast.nodes.FieldAccessNode;
import com.justnothing.testmodule.command.functions.script.engine_new.ast.nodes.FieldAssignmentNode;
import com.justnothing.testmodule.command.functions.script.engine_new.ast.nodes.FieldDeclarationNode;
import com.justnothing.testmodule.command.functions.script.engine_new.ast.nodes.ForEachNode;
import com.justnothing.testmodule.command.functions.script.engine_new.ast.nodes.ForNode;
import com.justnothing.testmodule.command.functions.script.engine_new.ast.nodes.FunctionCallNode;
import com.justnothing.testmodule.command.functions.script.engine_new.ast.nodes.IfNode;
import com.justnothing.testmodule.command.functions.script.engine_new.ast.nodes.ImportNode;
import com.justnothing.testmodule.command.functions.script.engine_new.ast.nodes.InstanceofNode;
import com.justnothing.testmodule.command.functions.script.engine_new.ast.nodes.InterpolatedStringNode;
import com.justnothing.testmodule.command.functions.script.engine_new.ast.nodes.LambdaNode;
import com.justnothing.testmodule.command.functions.script.engine_new.ast.nodes.LiteralNode;
import com.justnothing.testmodule.command.functions.script.engine_new.ast.nodes.MapLiteralNode;
import com.justnothing.testmodule.command.functions.script.engine_new.ast.nodes.MethodCallNode;
import com.justnothing.testmodule.command.functions.script.engine_new.ast.nodes.MethodDeclarationNode;
import com.justnothing.testmodule.command.functions.script.engine_new.ast.nodes.MethodReferenceNode;
import com.justnothing.testmodule.command.functions.script.engine_new.ast.nodes.NewArrayNode;
import com.justnothing.testmodule.command.functions.script.engine_new.ast.nodes.NullCoalescingAssignNode;
import com.justnothing.testmodule.command.functions.script.engine_new.ast.nodes.ParameterNode;
import com.justnothing.testmodule.command.functions.script.engine_new.ast.nodes.PipelineNode;
import com.justnothing.testmodule.command.functions.script.engine_new.ast.nodes.ResourceDeclaration;
import com.justnothing.testmodule.command.functions.script.engine_new.ast.nodes.ReturnNode;
import com.justnothing.testmodule.command.functions.script.engine_new.ast.nodes.SafeFieldAccessNode;
import com.justnothing.testmodule.command.functions.script.engine_new.ast.nodes.SafeMethodCallNode;
import com.justnothing.testmodule.command.functions.script.engine_new.ast.nodes.SwitchNode;
import com.justnothing.testmodule.command.functions.script.engine_new.ast.nodes.TernaryNode;
import com.justnothing.testmodule.command.functions.script.engine_new.ast.nodes.ThrowNode;
import com.justnothing.testmodule.command.functions.script.engine_new.ast.nodes.TryNode;
import com.justnothing.testmodule.command.functions.script.engine_new.ast.nodes.UnaryOpNode;
import com.justnothing.testmodule.command.functions.script.engine_new.ast.nodes.VariableNode;
import com.justnothing.testmodule.command.functions.script.engine_new.ast.nodes.WhileNode;
import com.justnothing.testmodule.command.functions.script.engine_new.evaluator.AutoClass;
import com.justnothing.testmodule.command.functions.script.engine_new.evaluator.ClassFinder;
import com.justnothing.testmodule.command.functions.script.engine_new.exception.ParseException;
import com.justnothing.testmodule.command.functions.script.engine_new.lexer.Lexer;
import com.justnothing.testmodule.command.functions.script.engine_new.lexer.Token;
import com.justnothing.testmodule.command.functions.script.engine_new.lexer.TokenType;
import com.justnothing.testmodule.utils.reflect.ClassResolver;
import com.justnothing.testmodule.command.functions.script.engine_new.exception.ErrorCode;

import java.lang.reflect.Array;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 语法分析器（Parser）
 * <p>
 * 将token流转换为抽象语法树（AST）。
 * </p>
 * 
 * @author JustNothing1021
 * @since 1.0.0
 */
public class Parser {
    
    private static final ConcurrentHashMap<String, String> classNameCache = new ConcurrentHashMap<>();
    private static final String NOT_A_CLASS = "__NOT_A_CLASS__";
    
    public static void clearClassNameCache() {
        classNameCache.clear();
    }
    
    public static void clearFailedClassCache() {
        classNameCache.entrySet().removeIf(entry -> NOT_A_CLASS.equals(entry.getValue()));
    }
    
    private final List<Token> tokens;
    private int position;
    private final ParseContext context;
    private final Deque<Integer> savedPositions;
    private final ClassLoader classLoader;

    public Parser(List<Token> tokens) {
        this(tokens, Thread.currentThread().getContextClassLoader());
    }
    
    public Parser(List<Token> tokens, ClassLoader classLoader) {
        this.tokens = tokens;
        this.position = 0;
        this.context = new ParseContext();
        this.context.setClassLoader(classLoader);
        this.savedPositions = new ArrayDeque<>();
        this.classLoader = classLoader;
    }
    
    public Parser(List<Token> tokens, ParseContext context) {
        this(tokens, context, Thread.currentThread().getContextClassLoader());
    }
    
    public Parser(List<Token> tokens, ParseContext context, ClassLoader classLoader) {
        this.tokens = tokens;
        this.position = 0;
        this.context = context;
        if (context.getClassLoader() == null && classLoader != null) {
            context.setClassLoader(classLoader);
        }
        this.savedPositions = new ArrayDeque<>();
        this.classLoader = classLoader;
    }
    
    /**
     * 解析整个脚本
     * @return AST根节点（BlockNode）
     */
    public BlockNode parse() throws ParseException {
        List<ASTNode> statements = new ArrayList<>();
        
        while (!isAtEnd() && !peek().is(TokenType.EOF)) {
            statements.add(parseStatement());
        }
        
        SourceLocation location = createLocation();
        return new BlockNode(statements, location);
    }
    
    private boolean isAtEnd() {
        return position >= tokens.size();
    }
    
    private Token peek() {
        if (isAtEnd()) {
            return tokens.get(tokens.size() - 1);
        }
        return tokens.get(position);
    }
    
    private Token peekNext() {
        if (position + 1 >= tokens.size()) {
            return tokens.get(tokens.size() - 1);
        }
        return tokens.get(position + 1);
    }
    
    private Token advance() {
        if (isAtEnd()) {
            return tokens.get(tokens.size() - 1);
        }
        return tokens.get(position++);
    }
    
    private boolean match(TokenType type) {
        if (check(type)) {
            advance();
            return true;
        }
        return false;
    }
    
    private boolean check(TokenType type) {
        if (isAtEnd()) {
            return false;
        }
        return peek().getType() == type;
    }
    
    private Token consume(TokenType type, String message) throws ParseException {
        if (check(type)) {
            return advance();
        }
        throw error(message);
    }
    
    private ParseException error(String message) {
        Token token = peek();
        return new ParseException(
                message,
                token.getLocation().getLine(),
                token.getLocation().getColumn(),
                ErrorCode.PARSE_INVALID_SYNTAX
        );
    }

    private ParseException error(String message, ErrorCode errorCode) {
        Token token = peek();
        return new ParseException(
                message,
                token.getLocation().getLine(),
                token.getLocation().getColumn(),
                errorCode
        );
    }
    
    private SourceLocation createLocation() {
        if (isAtEnd()) {
            return tokens.get(tokens.size() - 1).getLocation();
        }
        return peek().getLocation();
    }
    
    private int savePosition() {
        savedPositions.push(position);
        return position;
    }
    
    private void restorePosition() {
        if (!savedPositions.isEmpty()) {
            position = savedPositions.pop();
        }
    }
    
    private void releasePosition() {
        if (!savedPositions.isEmpty()) {
            savedPositions.pop();
        }
    }
    
    /**
     * 解析语句
     */
    private ASTNode parseStatement() throws ParseException {
        if (match(TokenType.KEYWORD_CLASS)) {
            return parseClassDeclaration();
        }
        
        if (check(TokenType.KEYWORD_INT) || check(TokenType.KEYWORD_LONG) ||
            check(TokenType.KEYWORD_FLOAT) || check(TokenType.KEYWORD_DOUBLE) ||
            check(TokenType.KEYWORD_BOOLEAN) || check(TokenType.KEYWORD_CHAR) ||
            check(TokenType.KEYWORD_BYTE) || check(TokenType.KEYWORD_SHORT) ||
            check(TokenType.KEYWORD_AUTO) || check(TokenType.KEYWORD_FINAL)) {
            if (isTypeFollowedByClass()) {
                ASTNode expression = parseExpression();
                if (check(TokenType.DELIMITER_SEMICOLON)) {
                    advance();
                }
                return expression;
            }
            return parseVariableDeclaration();
        }
        
        if (check(TokenType.IDENTIFIER) && checkNext(TokenType.OPERATOR_DECLARE_ASSIGN)) {
            return parseShortDeclaration();
        }
        
        if (check(TokenType.IDENTIFIER)) {
            if (checkNext(TokenType.DELIMITER_ARROW)) {
                ASTNode expression = parseExpression();
                if (expression instanceof LambdaNode) {
                    if (check(TokenType.DELIMITER_SEMICOLON)) {
                        advance();
                    }
                } else {
                    consume(TokenType.DELIMITER_SEMICOLON, "Expected semicolon after statement");
                }
                return expression;
            } else if (checkNext(TokenType.IDENTIFIER) || checkNext(TokenType.KEYWORD_AUTO) ||
                       checkNext(TokenType.KEYWORD_INT) || checkNext(TokenType.KEYWORD_LONG) ||
                       checkNext(TokenType.KEYWORD_FLOAT) || checkNext(TokenType.KEYWORD_DOUBLE) ||
                       checkNext(TokenType.KEYWORD_BOOLEAN) || checkNext(TokenType.KEYWORD_CHAR) ||
                       checkNext(TokenType.KEYWORD_BYTE) || checkNext(TokenType.KEYWORD_SHORT)) {
                return parseVariableDeclaration();
            } else {
                ASTNode expression = parseExpression();
                if (expression instanceof LambdaNode) {
                    if (check(TokenType.DELIMITER_SEMICOLON)) {
                        advance();
                    }
                } else {
                    consume(TokenType.DELIMITER_SEMICOLON, "Expected semicolon after statement");
                }
                return expression;
            }
        }
        
        if (match(TokenType.KEYWORD_IF)) {
            return parseIfStatement();
        }
        
        if (match(TokenType.KEYWORD_FOR)) {
            return parseForStatement();
        }
        
        if (match(TokenType.KEYWORD_WHILE)) {
            return parseWhileStatement();
        }
        
        if (match(TokenType.KEYWORD_DO)) {
            return parseDoWhileStatement();
        }
        
        if (match(TokenType.KEYWORD_SWITCH)) {
            return parseSwitchStatement();
        }
        
        if (match(TokenType.KEYWORD_TRY)) {
            return parseTryStatement();
        }
        
        if (match(TokenType.KEYWORD_THROW)) {
            return parseThrowStatement();
        }
        
        if (match(TokenType.KEYWORD_IMPORT)) {
            return parseImportStatement();
        }
        
        if (match(TokenType.KEYWORD_DELETE)) {
            return parseDeleteStatement();
        }
        
        if (match(TokenType.KEYWORD_RETURN)) {
            return parseReturnStatement();
        }
        
        if (match(TokenType.KEYWORD_BREAK)) {
            return parseBreakStatement();
        }
        
        if (match(TokenType.KEYWORD_CONTINUE)) {
            return parseContinueStatement();
        }
        
        if (check(TokenType.DELIMITER_LEFT_BRACE)) {
            int savedPos = position;
            try {
                advance();
                if (check(TokenType.DELIMITER_RIGHT_BRACE)) {
                    advance();
                    if (check(TokenType.DELIMITER_SEMICOLON)) {
                        advance();
                        return new ArrayLiteralNode(new ArrayList<>(), createLocation());
                    }
                    position = savedPos;
                    advance();
                    return parseBlock();
                }
                
                try {
                    parseExpression();
                    if (check(TokenType.OPERATOR_COLON)) {
                        position = savedPos;
                        ASTNode expr = parseExpression();
                        if (check(TokenType.DELIMITER_SEMICOLON)) {
                            advance();
                        }
                        return expr;
                    }
                    if (check(TokenType.DELIMITER_COMMA) || check(TokenType.DELIMITER_RIGHT_BRACE)) {
                        position = savedPos;
                        ASTNode expr = parseExpression();
                        if (check(TokenType.DELIMITER_SEMICOLON)) {
                            advance();
                        }
                        return expr;
                    }
                } catch (ParseException ignored) {
                }
                position = savedPos;
                advance();
                return parseBlock();
            } catch (ParseException e) {
                position = savedPos;
                advance();
                return parseBlock();
            }
        }
        
        ASTNode expression = parseExpression();
        if (expression instanceof LambdaNode) {
            if (check(TokenType.DELIMITER_SEMICOLON)) {
                advance();
            }
        } else {
            consume(TokenType.DELIMITER_SEMICOLON, "Expected semicolon after statement");
        }
        return expression;
    }
    
    /**
     * 解析变量声明
     */
    private ASTNode parseVariableDeclaration() throws ParseException {
        return parseVariableDeclaration(true);
    }
    
    private ASTNode parseShortDeclaration() throws ParseException {
        SourceLocation location = createLocation();
        String varName = consume(TokenType.IDENTIFIER, "Expected variable name").getText();
        consume(TokenType.OPERATOR_DECLARE_ASSIGN, "Expected :=");
        ASTNode value = parseExpression();
        consume(TokenType.DELIMITER_SEMICOLON, "Expected semicolon after short declaration");
        
        return new AssignmentNode(varName, value, true,
            new GenericType(AutoClass.class, Collections.emptyList(), 0, "auto"), location);
    }
    
    private ASTNode parseVariableDeclaration(boolean consumeSemicolon) throws ParseException {
        SourceLocation location = createLocation();
        boolean isFinal = false;
        
        if (peek().getType() == TokenType.KEYWORD_FINAL) {
            isFinal = true;
            advance();
        }
        
        GenericType type;
        boolean isAuto = false;
        
        if (peek().getType() == TokenType.KEYWORD_AUTO) {
            advance();
            type = new GenericType(AutoClass.class, Collections.emptyList(), 0, "auto");
            isAuto = true;
        } else {
            type = parseGenericType();
        }
        
        List<AssignmentNode> declarations = new ArrayList<>();
        
        do {
            SourceLocation varLocation = createLocation();
            String varName = consume(TokenType.IDENTIFIER, "Expected variable name").getText();
            
            ASTNode value = null;
            if (match(TokenType.OPERATOR_ASSIGN)) {
                value = parseExpression();
            } else if (isAuto) {
                throw error("Auto variable must have initial value");
            }
            
            declarations.add(new AssignmentNode(varName, value, true, type, varLocation));
            
        } while (match(TokenType.DELIMITER_COMMA));
        
        if (consumeSemicolon) {
            consume(TokenType.DELIMITER_SEMICOLON, "Expected semicolon after variable declaration");
        }
        
        if (declarations.size() == 1) {
            return declarations.get(0);
        }
        
        return new BlockNode(new ArrayList<>(declarations), location);
    }

    
    /**
     * 解析类型表达式
     * 支持基本类型、类名、数组类型、泛型
     */
    private Class<?> parseType() throws ParseException {
        savePosition();
        
        try {
            StringBuilder typeName = new StringBuilder();
            
            while (true) {
                if (check(TokenType.IDENTIFIER) || isTypeKeyword(peek().getType())) {
                    if (typeName.length() > 0 && !typeName.toString().endsWith(".")) {
                        break;
                    }
                    String text = advance().getText();
                    typeName.append(text);
                    
                    if (check(TokenType.OPERATOR_DOT)) {
                        advance();
                        typeName.append(".");
                        continue;
                    }
                }
                
                if (check(TokenType.DELIMITER_LEFT_BRACKET)) {
                    advance();
                    if (check(TokenType.DELIMITER_RIGHT_BRACKET)) {
                        advance();
                        typeName.append("[]");
                        continue;
                    } else {
                        position--;
                        break;
                    }
                }
                
                if (check(TokenType.OPERATOR_LESS_THAN)) {
                    String currentType = typeName.toString();
                    if (isPrimitiveType(currentType)) {
                        break;
                    }
                    advance();
                    typeName.append("<");
                    parseTypeArguments(typeName);
                    continue;
                }
                
                break;
            }
            
            String typeStr = typeName.toString();
            if (typeStr.isEmpty()) {
                throw error("Expected type");
            }
            
            if (typeStr.endsWith(".")) {
                typeStr = typeStr.substring(0, typeStr.length() - 1);
            }
            
            Class<?> result = resolveType(typeStr);
            releasePosition();
            return result;
            
        } catch (ParseException e) {
            restorePosition();
            throw e;
        }
    }
    
    private boolean isTypeKeyword(TokenType type) {
        return type == TokenType.KEYWORD_INT || type == TokenType.KEYWORD_LONG ||
               type == TokenType.KEYWORD_FLOAT || type == TokenType.KEYWORD_DOUBLE ||
               type == TokenType.KEYWORD_BOOLEAN || type == TokenType.KEYWORD_CHAR ||
               type == TokenType.KEYWORD_BYTE || type == TokenType.KEYWORD_SHORT ||
               type == TokenType.KEYWORD_VOID || type == TokenType.KEYWORD_AUTO;
    }
    
    private boolean isPrimitiveType(String typeName) {
        return "int".equals(typeName) || "long".equals(typeName) ||
               "float".equals(typeName) || "double".equals(typeName) ||
               "boolean".equals(typeName) || "char".equals(typeName) ||
               "byte".equals(typeName) || "short".equals(typeName) ||
               "void".equals(typeName);
    }
    
    /**
     * 解析泛型类型，返回完整的类型信息
     */
    private GenericType parseGenericType() throws ParseException {
        savePosition();
        
        try {
            StringBuilder typeName = new StringBuilder();
            
            while (true) {
                if (check(TokenType.IDENTIFIER) || isTypeKeyword(peek().getType())) {
                    if (typeName.length() > 0 && !typeName.toString().endsWith(".")) {
                        break;
                    }
                    String text = advance().getText();
                    typeName.append(text);
                    
                    if (check(TokenType.OPERATOR_DOT)) {
                        advance();
                        typeName.append(".");
                        continue;
                    }
                }
                
                if (check(TokenType.DELIMITER_LEFT_BRACKET)) {
                    advance();
                    if (check(TokenType.DELIMITER_RIGHT_BRACKET)) {
                        advance();
                        typeName.append("[]");
                        continue;
                    } else {
                        position--;
                        break;
                    }
                }
                
                if (check(TokenType.OPERATOR_LESS_THAN)) {
                    String currentType = typeName.toString();
                    if (isPrimitiveType(currentType)) {
                        break;
                    }
                    advance();
                    typeName.append("<");
                    parseTypeArguments(typeName);
                    continue;
                }
                
                break;
            }
            
            String typeStr = typeName.toString();
            if (typeStr.isEmpty()) {
                restorePosition();
                throw error("Expected type");
            }
            
            if (typeStr.endsWith(".")) {
                typeStr = typeStr.substring(0, typeStr.length() - 1);
            }
            
            try {
                GenericType result = resolveGenericType(typeStr);
                releasePosition();
                return result;
            } catch (ParseException e) {
                restorePosition();
                throw e;
            }
            
        } catch (ParseException e) {
            restorePosition();
            throw e;
        }
    }
    
    
    /**
     * 解析泛型参数
     */
    private void parseTypeArguments(StringBuilder typeName) throws ParseException {
        int depth = 1;
        
        while (depth > 0 && !isAtEnd()) {
            if (check(TokenType.OPERATOR_GREATER_THAN)) {
                advance();
                depth--;
                typeName.append(">");
            } else if (check(TokenType.OPERATOR_RIGHT_SHIFT)) {
                advance();
                depth -= 2;
                typeName.append(">>");
            } else if (check(TokenType.OPERATOR_UNSIGNED_RIGHT_SHIFT)) {
                advance();
                depth -= 3;
                typeName.append(">>>");
            } else if (check(TokenType.OPERATOR_LESS_THAN)) {
                advance();
                depth++;
                typeName.append("<");
            } else if (check(TokenType.DELIMITER_COMMA)) {
                advance();
                typeName.append(", ");
            } else {
                typeName.append(parseTypeNameForGeneric());
            }
        }
    }
    
    /**
     * 解析泛型参数中的类型名称（返回字符串而非Class）
     */
    private String parseTypeNameForGeneric() throws ParseException {
        StringBuilder typeName = new StringBuilder();
        
        if (check(TokenType.IDENTIFIER)) {
            typeName.append(advance().getText());
            
            while (check(TokenType.OPERATOR_DOT)) {
                advance();
                typeName.append(".");
                if (check(TokenType.IDENTIFIER)) {
                    typeName.append(advance().getText());
                } else {
                    throw error("Expected identifier after '.'");
                }
            }
        } else if (check(TokenType.KEYWORD_INT)) {
            advance();
            typeName.append("int");
        } else if (check(TokenType.KEYWORD_LONG)) {
            advance();
            typeName.append("long");
        } else if (check(TokenType.KEYWORD_FLOAT)) {
            advance();
            typeName.append("float");
        } else if (check(TokenType.KEYWORD_DOUBLE)) {
            advance();
            typeName.append("double");
        } else if (check(TokenType.KEYWORD_BOOLEAN)) {
            advance();
            typeName.append("boolean");
        } else if (check(TokenType.KEYWORD_CHAR)) {
            advance();
            typeName.append("char");
        } else if (check(TokenType.KEYWORD_BYTE)) {
            advance();
            typeName.append("byte");
        } else if (check(TokenType.KEYWORD_SHORT)) {
            advance();
            typeName.append("short");
        } else if (check(TokenType.KEYWORD_VOID)) {
            advance();
            typeName.append("void");
        } else {
            throw error("Expected type name in generic argument");
        }
        
        while (check(TokenType.DELIMITER_LEFT_BRACKET)) {
            advance();
            consume(TokenType.DELIMITER_RIGHT_BRACKET, "Expected ']' in array type");
            typeName.append("[]");
        }
        
        return typeName.toString();
    }
    
    /**
     * 判断字符串是否是类引用，支持嵌套类和导入
     */
    private String resolveClassName(String className) {
        String cached = classNameCache.get(className);
        if (cached != null) {
            return cached == NOT_A_CLASS ? null : cached;
        }
        
        Class<?> clazz = ClassFinder.findClassWithImports(className, context.getClassLoader(), context.getImports());
        if (clazz != null) {
            String result = clazz.getName();
            classNameCache.put(className, result);
            return result;
        }
        
        classNameCache.put(className, NOT_A_CLASS);
        return null;
    }
    
    /**
     * 解析类型字符串为Class对象
     */
    private Class<?> resolveType(String typeName) throws ParseException {
        String baseTypeName = typeName;
        int arrayDepth = 0;
        
        while (baseTypeName.endsWith("[]")) {
            arrayDepth++;
            baseTypeName = baseTypeName.substring(0, baseTypeName.length() - 2);
        }
        
        int genericIndex = baseTypeName.indexOf('<');
        if (genericIndex > 0) {
            baseTypeName = baseTypeName.substring(0, genericIndex);
        }
        
        if ("auto".equals(baseTypeName)) {
            return AutoClass.class;
        }
        
        Class<?> baseType;

        baseType = ClassFinder.findClassWithImports(baseTypeName, context.getClassLoader(), context.getImports());
        if (baseType == null) {
            if (context.isClassDeclared(baseTypeName)) {
                baseType = Object.class;
            } else {
                throw error("Class not found: " + baseTypeName, ErrorCode.PARSE_CLASS_NOT_FOUND);
            }
        }
        
        if (arrayDepth > 0) {
            for (int i = 0; i < arrayDepth; i++) {
                baseType = Array.newInstance(baseType, 0).getClass();
            }
        }
        
        return baseType;
    }
    
    /**
     * 解析类型字符串为GenericType对象
     */
    private GenericType resolveGenericType(String typeName) throws ParseException {
        String baseTypeName = typeName;
        int arrayDepth = 0;
        
        while (baseTypeName.endsWith("[]")) {
            arrayDepth++;
            baseTypeName = baseTypeName.substring(0, baseTypeName.length() - 2);
        }
        
        List<GenericType> typeArguments = Collections.emptyList();
        int genericStart = baseTypeName.indexOf('<');
        if (genericStart > 0) {
            int genericEnd = baseTypeName.lastIndexOf('>');
            if (genericEnd > genericStart) {
                String argsStr = baseTypeName.substring(genericStart + 1, genericEnd);
                typeArguments = parseTypeArgumentStrings(argsStr);
                baseTypeName = baseTypeName.substring(0, genericStart);
            }
        }
        
        if ("auto".equals(baseTypeName)) {
            return new GenericType(AutoClass.class, typeArguments, arrayDepth, "auto");
        }
        
        Class<?> baseType;
        baseType = ClassFinder.findClassWithImports(baseTypeName, context.getClassLoader(), context.getImports());
        if (baseType == null) {
            if (context.isClassDeclared(baseTypeName)) {
                baseType = Object.class;
            } else {
                throw error("Class not found: " + baseTypeName, ErrorCode.PARSE_CLASS_NOT_FOUND);
            }
            return new GenericType(baseType, typeArguments, arrayDepth, baseTypeName);
        }
        
        return new GenericType(baseType, typeArguments, arrayDepth);
    }
    
    /**
     * 解析泛型参数字符串为GenericType列表
     */
    private List<GenericType> parseTypeArgumentStrings(String argsStr) throws ParseException {
        List<GenericType> result = new ArrayList<>();
        int depth = 0;
        int start = 0;
        
        for (int i = 0; i < argsStr.length(); i++) {
            char c = argsStr.charAt(i);
            if (c == '<') {
                depth++;
            } else if (c == '>') {
                depth--;
            } else if (c == ',' && depth == 0) {
                String arg = argsStr.substring(start, i).trim();
                if (!arg.isEmpty()) {
                    result.add(resolveGenericType(arg));
                }
                start = i + 1;
            }
        }
        
        if (start < argsStr.length()) {
            String arg = argsStr.substring(start).trim();
            if (!arg.isEmpty()) {
                result.add(resolveGenericType(arg));
            }
        }
        
        return result;
    }
    
    /**
     * 解析if语句
     */
    private IfNode parseIfStatement() throws ParseException {
        SourceLocation location = createLocation();
        
        consume(TokenType.DELIMITER_LEFT_PAREN, "Expected '(' after if");
        ASTNode condition = parseExpression();
        consume(TokenType.DELIMITER_RIGHT_PAREN, "Expected ')' after condition");
        
        ASTNode thenBlock = parseStatement();
        
        ASTNode elseBlock = null;
        if (match(TokenType.KEYWORD_ELSE)) {
            elseBlock = parseStatement();
        }
        
        return new IfNode(condition, thenBlock, elseBlock, location);
    }
    
    /**
     * 解析for语句
     * 支持传统for循环和for-each循环
     */
    private ASTNode parseForStatement() throws ParseException {
        SourceLocation location = createLocation();
        
        consume(TokenType.DELIMITER_LEFT_PAREN, "Expected '(' after for");
        
        if (check(TokenType.DELIMITER_SEMICOLON)) {
            return parseTraditionalFor(location);
        }
        
        if (isTypeKeyword(peek().getType()) || peek().getType() == TokenType.IDENTIFIER) {
            int savedPos = position;
            
            try {
                Class<?> itemType = parseType();
                String itemName = consume(TokenType.IDENTIFIER, "Expected variable name").getText();
                
                if (check(TokenType.OPERATOR_COLON)) {
                    advance();
                    ASTNode collection = parseExpression();
                    consume(TokenType.DELIMITER_RIGHT_PAREN, "Expected ')' after for-each collection");
                    ASTNode body = parseStatement();
                    return new ForEachNode(itemType, itemName, collection, body, location);
                }
            } catch (ParseException ignored) {
            }
            
            position = savedPos;
        }
        
        return parseTraditionalFor(location);
    }
    
    /**
     * 解析传统for循环
     */
    private ForNode parseTraditionalFor(SourceLocation location) throws ParseException {
        ASTNode initialization = null;
        if (!check(TokenType.DELIMITER_SEMICOLON)) {
            if (isTypeKeyword(peek().getType()) || peek().getType() == TokenType.IDENTIFIER) {
                savePosition();
                try {
                    initialization = parseVariableDeclaration(false);
                } catch (ParseException e) {
                    restorePosition();
                    initialization = parseExpression();
                }
            } else {
                initialization = parseExpression();
            }
        }
        consume(TokenType.DELIMITER_SEMICOLON, "Expected semicolon after for initialization");
        
        ASTNode condition = null;
        if (!check(TokenType.DELIMITER_SEMICOLON)) {
            condition = parseExpression();
        }
        consume(TokenType.DELIMITER_SEMICOLON, "Expected semicolon after for condition");
        
        ASTNode update = null;
        if (!check(TokenType.DELIMITER_RIGHT_PAREN)) {
            update = parseExpression();
        }
        consume(TokenType.DELIMITER_RIGHT_PAREN, "Expected ')' after for clauses");
        
        ASTNode body = parseStatement();
        
        return new ForNode(initialization, condition, update, body, location);
    }
    
    /**
     * 解析while语句
     */
    private WhileNode parseWhileStatement() throws ParseException {
        SourceLocation location = createLocation();
        
        consume(TokenType.DELIMITER_LEFT_PAREN, "Expected '(' after while");
        ASTNode condition = parseExpression();
        consume(TokenType.DELIMITER_RIGHT_PAREN, "Expected ')' after condition");
        
        ASTNode body = parseStatement();
        
        return new WhileNode(condition, body, location);
    }
    
    /**
     * 解析do-while语句
     */
    private DoWhileNode parseDoWhileStatement() throws ParseException {
        SourceLocation location = createLocation();
        
        ASTNode body = parseStatement();
        
        consume(TokenType.KEYWORD_WHILE, "Expected 'while' after do body");
        consume(TokenType.DELIMITER_LEFT_PAREN, "Expected '(' after while");
        ASTNode condition = parseExpression();
        consume(TokenType.DELIMITER_RIGHT_PAREN, "Expected ')' after condition");
        consume(TokenType.DELIMITER_SEMICOLON, "Expected semicolon after do-while");
        
        return new DoWhileNode(body, condition, location);
    }
    
    /**
     * 解析switch语句
     */
    private SwitchNode parseSwitchStatement() throws ParseException {
        SourceLocation location = createLocation();
        
        consume(TokenType.DELIMITER_LEFT_PAREN, "Expected '(' after switch");
        ASTNode expression = parseExpression();
        consume(TokenType.DELIMITER_RIGHT_PAREN, "Expected ')' after switch expression");
        
        consume(TokenType.DELIMITER_LEFT_BRACE, "Expected '{' after switch expression");
        
        List<CaseNode> cases = new ArrayList<>();
        ASTNode defaultCase = null;
        
        while (!check(TokenType.DELIMITER_RIGHT_BRACE) && !isAtEnd()) {
            if (match(TokenType.KEYWORD_CASE)) {
                ASTNode caseValue = parseExpression();
                consume(TokenType.OPERATOR_COLON, "Expected ':' after case value");
                
                List<ASTNode> statements = new ArrayList<>();
                while (!check(TokenType.KEYWORD_CASE) && 
                       !check(TokenType.KEYWORD_DEFAULT) && 
                       !check(TokenType.DELIMITER_RIGHT_BRACE)) {
                    statements.add(parseStatement());
                }
                
                cases.add(new CaseNode(caseValue, statements, location));
            } else if (match(TokenType.KEYWORD_DEFAULT)) {
                consume(TokenType.OPERATOR_COLON, "Expected ':' after default");
                
                List<ASTNode> statements = new ArrayList<>();
                while (!check(TokenType.KEYWORD_CASE) && 
                       !check(TokenType.KEYWORD_DEFAULT) && 
                       !check(TokenType.DELIMITER_RIGHT_BRACE)) {
                    statements.add(parseStatement());
                }
                
                defaultCase = new BlockNode(statements, location);
            } else {
                throw error("Expected 'case' or 'default' in switch body");
            }
        }
        
        consume(TokenType.DELIMITER_RIGHT_BRACE, "Expected '}' at end of switch");
        
        return new SwitchNode(expression, cases, defaultCase, location);
    }
    
    /**
     * 解析switch表达式 (支持箭头语法)
     * 语法: switch (expr) { case pattern -> result; ... }
     */
    private SwitchNode parseSwitchExpression() throws ParseException {
        SourceLocation location = createLocation();
        
        consume(TokenType.DELIMITER_LEFT_PAREN, "Expected '(' after switch");
        ASTNode expression = parseExpression();
        consume(TokenType.DELIMITER_RIGHT_PAREN, "Expected ')' after switch expression");
        
        consume(TokenType.DELIMITER_LEFT_BRACE, "Expected '{' after switch expression");
        
        List<CaseNode> cases = new ArrayList<>();
        ASTNode defaultCase = null;
        
        while (!check(TokenType.DELIMITER_RIGHT_BRACE) && !isAtEnd()) {
            if (match(TokenType.KEYWORD_CASE)) {
                ASTNode caseValue = parseTernary();
                List<ASTNode> caseStatements;
                
                if (match(TokenType.DELIMITER_ARROW)) {
                    ASTNode caseExpr = parseExpression();
                    caseStatements = new ArrayList<>();
                    caseStatements.add(caseExpr);
                    if (check(TokenType.DELIMITER_SEMICOLON)) {
                        advance();
                    }
                } else {
                    consume(TokenType.OPERATOR_COLON, "Expected '->' or ':' after case value");
                    caseStatements = new ArrayList<>();
                    while (!check(TokenType.KEYWORD_CASE) && 
                           !check(TokenType.KEYWORD_DEFAULT) && 
                           !check(TokenType.DELIMITER_RIGHT_BRACE)) {
                        caseStatements.add(parseStatement());
                    }
                }
                
                cases.add(new CaseNode(caseValue, caseStatements, location));
            } else if (match(TokenType.KEYWORD_DEFAULT)) {
                if (match(TokenType.DELIMITER_ARROW)) {
                    ASTNode defaultExpr = parseExpression();
                    defaultCase = defaultExpr;
                    if (check(TokenType.DELIMITER_SEMICOLON)) {
                        advance();
                    }
                } else {
                    consume(TokenType.OPERATOR_COLON, "Expected '->' or ':' after default");
                    List<ASTNode> statements = new ArrayList<>();
                    while (!check(TokenType.KEYWORD_CASE) && 
                           !check(TokenType.KEYWORD_DEFAULT) && 
                           !check(TokenType.DELIMITER_RIGHT_BRACE)) {
                        statements.add(parseStatement());
                    }
                    defaultCase = new BlockNode(statements, location);
                }
            } else {
                throw error("Expected 'case' or 'default' in switch body");
            }
        }
        
        consume(TokenType.DELIMITER_RIGHT_BRACE, "Expected '}' at end of switch");
        
        return new SwitchNode(expression, cases, defaultCase, location);
    }
    
    /**
     * 解析try语句
     * 语法: 
     *   try { } catch (Type e) { } [finally { }]
     *   try (Resource r = expr) { } catch ... finally ...
     */
    private TryNode parseTryStatement() throws ParseException {
        SourceLocation location = createLocation();
        
        List<ResourceDeclaration> resources = new ArrayList<>();
        if (match(TokenType.DELIMITER_LEFT_PAREN)) {
            if (!check(TokenType.DELIMITER_RIGHT_PAREN)) {
                do {
                    resources.add(parseResourceDeclaration());
                } while (match(TokenType.DELIMITER_SEMICOLON));
            }
            consume(TokenType.DELIMITER_RIGHT_PAREN, "Expected ')' after resources");
        }
        
        ASTNode tryBlock = parseStatement();
        
        List<CatchClause> catchClauses = new ArrayList<>();
        while (match(TokenType.KEYWORD_CATCH)) {
            catchClauses.add(parseCatchClause());
        }
        
        ASTNode finallyBlock = null;
        if (match(TokenType.KEYWORD_FINALLY)) {
            finallyBlock = parseStatement();
        }
        
        if (catchClauses.isEmpty() && finallyBlock == null) {
            throw error("Try statement must have at least one catch or finally clause");
        }
        
        return new TryNode(resources, tryBlock, catchClauses, finallyBlock, location);
    }
    
    /**
     * 解析资源声明
     * 语法: 
     *   Type var = expression
     *   var (Java 9+ 风格，引用已存在的变量)
     */
    private ResourceDeclaration parseResourceDeclaration() throws ParseException {
        SourceLocation location = createLocation();
        
        if (isTypeKeyword(peek().getType())) {
            Class<?> type = parseType();
            String varName = consume(TokenType.IDENTIFIER, "Expected variable name").getText();
            consume(TokenType.OPERATOR_ASSIGN, "Expected '=' in resource declaration");
            ASTNode initializer = parseExpression();
            return new ResourceDeclaration(type, varName, initializer, location);
        }
        
        if (check(TokenType.IDENTIFIER)) {
            int savedPos = position;
            try {
                Class<?> type = parseType();
                if (check(TokenType.IDENTIFIER) && checkNext(TokenType.OPERATOR_ASSIGN)) {
                    String varName = consume(TokenType.IDENTIFIER, "Expected variable name").getText();
                    consume(TokenType.OPERATOR_ASSIGN, "Expected '=' in resource declaration");
                    ASTNode initializer = parseExpression();
                    return new ResourceDeclaration(type, varName, initializer, location);
                }
            } catch (ParseException ignored) {
            }
            position = savedPos;
        }
        
        String varName = consume(TokenType.IDENTIFIER, "Expected resource variable name").getText();
        return new ResourceDeclaration(varName, location);
    }
    
    /**
     * 解析catch子句
     * 语法: catch (Type1 | Type2 e) { }
     */
    private CatchClause parseCatchClause() throws ParseException {
        SourceLocation location = createLocation();
        
        consume(TokenType.DELIMITER_LEFT_PAREN, "Expected '(' after catch");
        
        List<Class<?>> exceptionTypes = new ArrayList<>();

        do {
            exceptionTypes.add(parseType());
        } while (match(TokenType.OPERATOR_BITWISE_OR));
        
        String variableName = consume(TokenType.IDENTIFIER, "Expected exception variable name").getText();
        
        consume(TokenType.DELIMITER_RIGHT_PAREN, "Expected ')' after catch clause");
        
        ASTNode body = parseStatement();
        
        return new CatchClause(exceptionTypes, variableName, body, location);
    }
    
    /**
     * 解析throw语句
     * 语法: throw expression;
     */
    private ASTNode parseThrowStatement() throws ParseException {
        SourceLocation location = createLocation();
        
        ASTNode expression = parseExpression();
        
        consume(TokenType.DELIMITER_SEMICOLON, "Expected semicolon after throw");
        
        return new ThrowNode(expression, location);
    }
    
    /**
     * 解析import语句
     * 语法: import package.name.*; 或 import package.name.ClassName;
     */
    private ASTNode parseImportStatement() throws ParseException {
        SourceLocation location = createLocation();
        
        StringBuilder packageName = new StringBuilder();
        
        while (true) {
            if (check(TokenType.IDENTIFIER)) {
                packageName.append(advance().getText());
            } else if (check(TokenType.OPERATOR_MULTIPLY)) {
                advance();
                packageName.append("*");
                break;
            } else {
                break;
            }
            
            if (check(TokenType.OPERATOR_DOT)) {
                advance();
                packageName.append(".");
            } else {
                break;
            }
        }
        
        consume(TokenType.DELIMITER_SEMICOLON, "Expected semicolon after import");
        
        String importStmt = packageName.toString();
        context.addImport(importStmt);
        clearFailedClassCache();
        
        return new ImportNode(importStmt, location);
    }
    
    /**
     * 解析delete语句
     * 语法: delete variableName; 或 delete *;
     */
    private ASTNode parseDeleteStatement() throws ParseException {
        SourceLocation location = createLocation();
        
        if (check(TokenType.OPERATOR_MULTIPLY)) {
            advance();
            consume(TokenType.DELIMITER_SEMICOLON, "Expected semicolon after delete *");
            return new DeleteNode(true, location);
        }
        
        String varName = consume(TokenType.IDENTIFIER, "Expected variable name after delete").getText();
        consume(TokenType.DELIMITER_SEMICOLON, "Expected semicolon after delete");
        
        return new DeleteNode(varName, location);
    }
    
    /**
     * 解析class声明
     * 语法: class ClassName [extends SuperClass] [implements Interface1, Interface2] { ... }
     */
    private ASTNode parseClassDeclaration() throws ParseException {
        SourceLocation location = createLocation();
        
        String className = consume(TokenType.IDENTIFIER, "Expected class name").getText();
        
        context.declareClass(className);
        
        String superClassName = null;
        if (match(TokenType.KEYWORD_EXTENDS)) {
            superClassName = parseTypeName();
        }
        
        List<String> interfaceNames = new ArrayList<>();
        if (match(TokenType.KEYWORD_IMPLEMENTS)) {
            do {
                interfaceNames.add(parseTypeName());
            } while (match(TokenType.DELIMITER_COMMA));
        }
        
        ClassDeclarationNode classDecl = new ClassDeclarationNode(className, superClassName, interfaceNames, location);
        
        consume(TokenType.DELIMITER_LEFT_BRACE, "Expected '{' after class declaration");
        
        while (!check(TokenType.DELIMITER_RIGHT_BRACE) && !isAtEnd()) {
            ClassModifiers modifiers = parseModifiers();
            
            if (check(TokenType.IDENTIFIER) && checkNext(TokenType.DELIMITER_LEFT_PAREN)) {
                String constructorName = consume(TokenType.IDENTIFIER, "Expected constructor name").getText();
                if (!constructorName.equals(className)) {
                    throw error("Constructor name must match class name");
                }
                ConstructorDeclarationNode constructor = parseConstructorDeclaration(className, modifiers);
                classDecl.addConstructor(constructor);
            } else if (isTypeStart()) {
                String typeName = parseTypeName();
                String memberName = consume(TokenType.IDENTIFIER, "Expected member name").getText();
                
                if (check(TokenType.DELIMITER_LEFT_PAREN)) {
                    MethodDeclarationNode method = parseMethodDeclaration(memberName, typeName, modifiers);
                    classDecl.addMethod(method);
                } else {
                    FieldDeclarationNode field = parseFieldDeclaration(memberName, typeName, modifiers);
                    classDecl.addField(field);
                }
            } else {
                throw error("Unexpected token in class body");
            }
        }
        
        consume(TokenType.DELIMITER_RIGHT_BRACE, "Expected '}' at end of class declaration");
        
        return classDecl;
    }
    
    /**
     * 解析修饰符
     */
    private ClassModifiers parseModifiers() throws ParseException {
        ClassModifiers modifiers = new ClassModifiers();
        
        while (true) {
            if (match(TokenType.KEYWORD_PUBLIC)) {
                modifiers.setPublic(true);
            } else if (match(TokenType.KEYWORD_PRIVATE)) {
                modifiers.setPrivate(true);
            } else if (match(TokenType.KEYWORD_PROTECTED)) {
                modifiers.setProtected(true);
            } else if (match(TokenType.KEYWORD_STATIC)) {
                modifiers.setStatic(true);
            } else if (match(TokenType.KEYWORD_FINAL)) {
                modifiers.setFinal(true);
            } else if (match(TokenType.KEYWORD_ABSTRACT)) {
                modifiers.setAbstract(true);
            } else if (match(TokenType.KEYWORD_NATIVE)) {
                modifiers.setNative(true);
            } else if (match(TokenType.KEYWORD_SYNCHRONIZED)) {
                modifiers.setSynchronized(true);
            } else {
                break;
            }
        }
        
        return modifiers;
    }
    
    /**
     * 检查是否是类型开始
     */
    private boolean isTypeStart() {
        return check(TokenType.KEYWORD_INT) || check(TokenType.KEYWORD_LONG) ||
               check(TokenType.KEYWORD_FLOAT) || check(TokenType.KEYWORD_DOUBLE) ||
               check(TokenType.KEYWORD_BOOLEAN) || check(TokenType.KEYWORD_CHAR) ||
               check(TokenType.KEYWORD_BYTE) || check(TokenType.KEYWORD_SHORT) ||
               check(TokenType.KEYWORD_VOID) || check(TokenType.IDENTIFIER);
    }
    
    /**
     * 解析类型名称
     * 支持完整类名、泛型、数组等
     * 例如: java.lang.String, List<String>, Map<String, Integer>, int[], java.util.List<java.lang.String>
     */
    private String parseTypeName() throws ParseException {
        StringBuilder typeName = new StringBuilder();
        
        if (check(TokenType.IDENTIFIER)) {
            typeName.append(advance().getText());
            
            while (check(TokenType.OPERATOR_DOT)) {
                advance();
                typeName.append(".");
                if (check(TokenType.IDENTIFIER)) {
                    typeName.append(advance().getText());
                } else {
                    throw error("Expected identifier after '.'");
                }
            }
            
            if (check(TokenType.OPERATOR_LESS_THAN)) {
                typeName.append(parseGenericString());
            }
        } else if (check(TokenType.KEYWORD_INT)) {
            advance();
            typeName.append("int");
        } else if (check(TokenType.KEYWORD_LONG)) {
            advance();
            typeName.append("long");
        } else if (check(TokenType.KEYWORD_FLOAT)) {
            advance();
            typeName.append("float");
        } else if (check(TokenType.KEYWORD_DOUBLE)) {
            advance();
            typeName.append("double");
        } else if (check(TokenType.KEYWORD_BOOLEAN)) {
            advance();
            typeName.append("boolean");
        } else if (check(TokenType.KEYWORD_CHAR)) {
            advance();
            typeName.append("char");
        } else if (check(TokenType.KEYWORD_BYTE)) {
            advance();
            typeName.append("byte");
        } else if (check(TokenType.KEYWORD_SHORT)) {
            advance();
            typeName.append("short");
        } else if (check(TokenType.KEYWORD_VOID)) {
            advance();
            typeName.append("void");
        } else {
            throw error("Expected type name");
        }
        
        while (check(TokenType.DELIMITER_LEFT_BRACKET)) {
            advance();
            consume(TokenType.DELIMITER_RIGHT_BRACKET, "Expected ']' in array type");
            typeName.append("[]");
        }
        
        return typeName.toString();
    }
    
    /**
     * 解析泛型参数字符串
     * 例如: <String>, <String, Integer>, <?>, <? extends Number>
     */
    private String parseGenericString() throws ParseException {
        StringBuilder generic = new StringBuilder();
        
        consume(TokenType.OPERATOR_LESS_THAN, "Expected '<'");
        generic.append("<");
        
        int depth = 1;
        while (depth > 0 && !isAtEnd()) {
            if (check(TokenType.OPERATOR_GREATER_THAN)) {
                advance();
                depth--;
                generic.append(">");
            } else if (check(TokenType.OPERATOR_LESS_THAN)) {
                advance();
                depth++;
                generic.append("<");
            } else if (check(TokenType.DELIMITER_COMMA)) {
                advance();
                generic.append(", ");
            } else if (check(TokenType.OPERATOR_MULTIPLY)) {
                advance();
                generic.append("?");
                
                if (match(TokenType.KEYWORD_EXTENDS)) {
                    generic.append(" extends ");
                    generic.append(parseTypeName());
                } else if (match(TokenType.KEYWORD_SUPER)) {
                    generic.append(" super ");
                    generic.append(parseTypeName());
                }
            } else {
                generic.append(parseTypeName());
            }
        }
        
        return generic.toString();
    }
    
    /**
     * 解析构造函数声明
     */
    private ConstructorDeclarationNode parseConstructorDeclaration(String className, ClassModifiers modifiers) throws ParseException {
        SourceLocation location = createLocation();
        
        consume(TokenType.DELIMITER_LEFT_PAREN, "Expected '(' after constructor name");
        List<ParameterNode> parameters = parseParameterList();
        consume(TokenType.DELIMITER_RIGHT_PAREN, "Expected ')' after constructor parameters");
        
        consume(TokenType.DELIMITER_LEFT_BRACE, "Expected '{' before constructor body");
        BlockNode body = parseBlock();
        
        return new ConstructorDeclarationNode(className, parameters, body, modifiers, location);
    }
    
    /**
     * 解析方法声明
     */
    private MethodDeclarationNode parseMethodDeclaration(String methodName, String returnTypeName, ClassModifiers modifiers) throws ParseException {
        SourceLocation location = createLocation();
        
        consume(TokenType.DELIMITER_LEFT_PAREN, "Expected '(' after method name");
        List<ParameterNode> parameters = parseParameterList();
        consume(TokenType.DELIMITER_RIGHT_PAREN, "Expected ')' after method parameters");
        
        consume(TokenType.DELIMITER_LEFT_BRACE, "Expected '{' before method body");
        BlockNode body = parseBlock();
        
        return new MethodDeclarationNode(methodName, returnTypeName, parameters, body, modifiers, location);
    }
    
    /**
     * 解析字段声明
     */
    private FieldDeclarationNode parseFieldDeclaration(String fieldName, String typeName, ClassModifiers modifiers) throws ParseException {
        SourceLocation location = createLocation();
        
        ASTNode initialValue = null;
        if (match(TokenType.OPERATOR_ASSIGN)) {
            initialValue = parseExpression();
        }
        
        consume(TokenType.DELIMITER_SEMICOLON, "Expected ';' after field declaration");
        
        return new FieldDeclarationNode(fieldName, typeName, initialValue, modifiers, location);
    }
    
    /**
     * 解析参数列表
     */
    private List<ParameterNode> parseParameterList() throws ParseException {
        List<ParameterNode> parameters = new ArrayList<>();
        
        if (!check(TokenType.DELIMITER_RIGHT_PAREN)) {
            do {
                String typeName = parseTypeName();
                String paramName = consume(TokenType.IDENTIFIER, "Expected parameter name").getText();
                parameters.add(new ParameterNode(paramName, typeName, createLocation()));
            } while (match(TokenType.DELIMITER_COMMA));
        }
        
        return parameters;
    }
    
    /**
     * 解析return语句
     */
    private ASTNode parseReturnStatement() throws ParseException {
        SourceLocation location = createLocation();
        
        ASTNode value = null;
        if (!check(TokenType.DELIMITER_SEMICOLON)) {
            value = parseExpression();
        }
        
        consume(TokenType.DELIMITER_SEMICOLON, "Expected semicolon after return");
        return new ReturnNode(value, location);
    }
    
    /**
     * 解析break语句
     */
    private ASTNode parseBreakStatement() throws ParseException {
        SourceLocation location = createLocation();
        consume(TokenType.DELIMITER_SEMICOLON, "Expected semicolon after break");
        return new LiteralNode(null, null, location);
    }
    
    /**
     * 解析continue语句
     */
    private ASTNode parseContinueStatement() throws ParseException {
        SourceLocation location = createLocation();
        consume(TokenType.DELIMITER_SEMICOLON, "Expected semicolon after continue");
        return new LiteralNode(null, null, location);
    }
    
    /**
     * 解析代码块
     */
    private BlockNode parseBlock() throws ParseException {
        SourceLocation location = createLocation();
        List<ASTNode> statements = new ArrayList<>();
        
        while (!check(TokenType.DELIMITER_RIGHT_BRACE) && !check(TokenType.EOF)) {
            statements.add(parseStatement());
        }
        
        consume(TokenType.DELIMITER_RIGHT_BRACE, "Expected '}' at end of block");
        
        return new BlockNode(statements, location);
    }
    
    /**
     * 解析表达式（最低优先级）
     */
    private ASTNode parseExpression() throws ParseException {
        if (match(TokenType.KEYWORD_ASYNC)) {
            SourceLocation location = createLocation();
            ASTNode expr;
            if (check(TokenType.DELIMITER_LEFT_BRACE)) {
                savePosition();
                try {
                    expr = parseExpression();
                    if (!check(TokenType.DELIMITER_RIGHT_BRACE)) {
                        restorePosition();
                        match(TokenType.DELIMITER_LEFT_BRACE);
                        expr = parseBlock();
                    }
                } catch (ParseException e) {
                    restorePosition();
                    match(TokenType.DELIMITER_LEFT_BRACE);
                    expr = parseBlock();
                }
            } else {
                expr = parseExpression();
            }
            return new AsyncNode(expr, location);
        }
        
        if (match(TokenType.KEYWORD_AWAIT)) {
            SourceLocation location = createLocation();
            ASTNode expr = parseExpression();
            return new AwaitNode(expr, location);
        }
        
        if (match(TokenType.KEYWORD_SWITCH)) {
            return parseSwitchExpression();
        }
        
        return parseAssignment();
    }
    
    /**
     * 解析赋值表达式
     */
    private ASTNode parseAssignment() throws ParseException {
        ASTNode left = parseTernary();
            
            
            if (match(TokenType.OPERATOR_ASSIGN)) {
                ASTNode right = parseTernary();
                
                if (left instanceof VariableNode) {
                    String varName = ((VariableNode) left).getName();
                    return new AssignmentNode(varName, right, false, null, left.getLocation());
                }
                
                if (left instanceof FieldAccessNode fieldAccess) {
                    return new FieldAssignmentNode(fieldAccess.getTarget(), fieldAccess.getFieldName(), right, left.getLocation());
                }
                
                if (left instanceof ArrayAccessNode arrayAccess) {
                    return new ArrayAssignmentNode(arrayAccess.getArray(), arrayAccess.getIndex(), right, left.getLocation());
                }
            }
            
            BinaryOpNode.Operator compoundOp = null;
            if (match(TokenType.OPERATOR_PLUS_ASSIGN)) {
                compoundOp = BinaryOpNode.Operator.ADD;
            } else if (match(TokenType.OPERATOR_MINUS_ASSIGN)) {
                compoundOp = BinaryOpNode.Operator.SUBTRACT;
            } else if (match(TokenType.OPERATOR_MULTIPLY_ASSIGN)) {
                compoundOp = BinaryOpNode.Operator.MULTIPLY;
            } else if (match(TokenType.OPERATOR_DIVIDE_ASSIGN)) {
                compoundOp = BinaryOpNode.Operator.DIVIDE;
            } else if (match(TokenType.OPERATOR_MODULO_ASSIGN)) {
                compoundOp = BinaryOpNode.Operator.MODULO;
            } else if (match(TokenType.OPERATOR_BITWISE_AND_ASSIGN)) {
                compoundOp = BinaryOpNode.Operator.BITWISE_AND;
            } else if (match(TokenType.OPERATOR_BITWISE_OR_ASSIGN)) {
                compoundOp = BinaryOpNode.Operator.BITWISE_OR;
            } else if (match(TokenType.OPERATOR_BITWISE_XOR_ASSIGN)) {
                compoundOp = BinaryOpNode.Operator.BITWISE_XOR;
            } else if (match(TokenType.OPERATOR_LEFT_SHIFT_ASSIGN)) {
                compoundOp = BinaryOpNode.Operator.LEFT_SHIFT;
            } else if (match(TokenType.OPERATOR_RIGHT_SHIFT_ASSIGN)) {
                compoundOp = BinaryOpNode.Operator.RIGHT_SHIFT;
            } else if (match(TokenType.OPERATOR_UNSIGNED_RIGHT_SHIFT_ASSIGN)) {
                compoundOp = BinaryOpNode.Operator.UNSIGNED_RIGHT_SHIFT;
            }
            
            if (compoundOp != null) {
                ASTNode right = parseTernary();
                BinaryOpNode combinedValue = new BinaryOpNode(compoundOp, left, right, left.getLocation());
                
                if (left instanceof VariableNode) {
                    String varName = ((VariableNode) left).getName();
                    return new AssignmentNode(varName, combinedValue, false, null, left.getLocation());
                }
                
                if (left instanceof FieldAccessNode fieldAccess) {
                    return new FieldAssignmentNode(fieldAccess.getTarget(), fieldAccess.getFieldName(), combinedValue, left.getLocation());
                }
                
                if (left instanceof ArrayAccessNode arrayAccess) {
                    return new ArrayAssignmentNode(arrayAccess.getArray(), arrayAccess.getIndex(), combinedValue, left.getLocation());
                }
            }
            
            if (match(TokenType.OPERATOR_CONDITIONAL_ASSIGN)) {
                ASTNode right = parseTernary();
                if (left instanceof VariableNode) {
                    return new ConditionalAssignNode(((VariableNode) left).getName(), right, left.getLocation());
                }
            }
            
            if (match(TokenType.OPERATOR_NULL_COALESCING_ASSIGN)) {
                ASTNode right = parseTernary();
                if (left instanceof VariableNode) {
                    return new NullCoalescingAssignNode(((VariableNode) left).getName(), right, left.getLocation());
                }
            }
            
            return left;
    }
    
    /**
     * 解析三元表达式
     * condition ? thenExpr : elseExpr
     */
    private ASTNode parseTernary() throws ParseException {
        ASTNode condition = parseNullCoalescing();
        
        if (match(TokenType.OPERATOR_QUESTION)) {
            SourceLocation location = createLocation();
            ASTNode thenExpr = parseTernary();
            consume(TokenType.OPERATOR_COLON, "Expected ':' in ternary expression");
            ASTNode elseExpr = parseTernary();
            return new TernaryNode(condition, thenExpr, elseExpr, location);
        }
        
        while (match(TokenType.OPERATOR_PIPELINE)) {
            SourceLocation location = createLocation();
            ASTNode function = parsePostfix();
            condition = new PipelineNode(condition, function, location);
        }
        
        return condition;
    }
    
    private ASTNode parseNullCoalescing() throws ParseException {
        ASTNode left = parseLogicalOr();
        
        while (match(TokenType.OPERATOR_NULL_COALESCING) || match(TokenType.OPERATOR_ELVIS)) {
            Token opToken = tokens.get(position - 1);
            SourceLocation location = createLocation();
            ASTNode right = parseLogicalOr();
            
            if (opToken.getType() == TokenType.OPERATOR_NULL_COALESCING) {
                left = new BinaryOpNode(BinaryOpNode.Operator.NULL_COALESCING, left, right, location);
            } else {
                left = new BinaryOpNode(BinaryOpNode.Operator.ELVIS, left, right, location);
            }
        }
        
        return left;
    }
    
    /**
     * 解析逻辑或表达式
     */
    private ASTNode parseLogicalOr() throws ParseException {
        ASTNode left = parseLogicalAnd();
        
        while (match(TokenType.OPERATOR_LOGICAL_OR)) {
            SourceLocation location = createLocation();
            ASTNode right = parseLogicalAnd();
            left = new BinaryOpNode(BinaryOpNode.Operator.LOGICAL_OR, left, right, location);
        }
        
        return left;
    }
    
    /**
     * 解析逻辑与表达式
     */
    private ASTNode parseLogicalAnd() throws ParseException {
        ASTNode left = parseBitwiseOr();
        
        while (match(TokenType.OPERATOR_LOGICAL_AND)) {
            SourceLocation location = createLocation();
            ASTNode right = parseBitwiseOr();
            left = new BinaryOpNode(BinaryOpNode.Operator.LOGICAL_AND, left, right, location);
        }
        
        return left;
    }
    
    /**
     * 解析按位或表达式
     */
    private ASTNode parseBitwiseOr() throws ParseException {
        ASTNode left = parseBitwiseXor();
        
        while (match(TokenType.OPERATOR_BITWISE_OR)) {
            SourceLocation location = createLocation();
            ASTNode right = parseBitwiseXor();
            left = new BinaryOpNode(BinaryOpNode.Operator.BITWISE_OR, left, right, location);
        }
        
        return left;
    }
    
    /**
     * 解析按位异或表达式
     */
    private ASTNode parseBitwiseXor() throws ParseException {
        ASTNode left = parseBitwiseAnd();
        
        while (match(TokenType.OPERATOR_BITWISE_XOR)) {
            SourceLocation location = createLocation();
            ASTNode right = parseBitwiseAnd();
            left = new BinaryOpNode(BinaryOpNode.Operator.BITWISE_XOR, left, right, location);
        }
        
        return left;
    }
    
    /**
     * 解析按位与表达式
     */
    private ASTNode parseBitwiseAnd() throws ParseException {
        ASTNode left = parseEquality();
        
        while (match(TokenType.OPERATOR_BITWISE_AND)) {
            SourceLocation location = createLocation();
            ASTNode right = parseEquality();
            left = new BinaryOpNode(BinaryOpNode.Operator.BITWISE_AND, left, right, location);
        }
        
        return left;
    }
    
    private ASTNode parseEquality() throws ParseException {
        ASTNode left = parseRange();
        
        while (match(TokenType.OPERATOR_EQUAL) || match(TokenType.OPERATOR_NOT_EQUAL)) {
            Token opToken = tokens.get(position - 1);
            SourceLocation location = createLocation();
            ASTNode right = parseRange();
            
            BinaryOpNode.Operator operator = opToken.getType() == TokenType.OPERATOR_EQUAL ? 
                    BinaryOpNode.Operator.EQUAL : BinaryOpNode.Operator.NOT_EQUAL;
            left = new BinaryOpNode(operator, left, right, location);
        }
        
        return left;
    }
    
    private ASTNode parseRange() throws ParseException {
        ASTNode left = parseComparison();
        
        if (match(TokenType.OPERATOR_RANGE)) {
            SourceLocation location = createLocation();
            ASTNode right = parseComparison();
            return new BinaryOpNode(BinaryOpNode.Operator.RANGE, left, right, location);
        }
        
        if (match(TokenType.OPERATOR_RANGE_EXCLUSIVE)) {
            SourceLocation location = createLocation();
            ASTNode right = parseComparison();
            return new BinaryOpNode(BinaryOpNode.Operator.RANGE_EXCLUSIVE, left, right, location);
        }
        
        return left;
    }
    
    /**
     * 解析比较表达式
     */
    private ASTNode parseComparison() throws ParseException {
        ASTNode left = parseShift();
        
        while (match(TokenType.OPERATOR_LESS_THAN) || 
               match(TokenType.OPERATOR_LESS_THAN_OR_EQUAL) ||
               match(TokenType.OPERATOR_GREATER_THAN) ||
               match(TokenType.OPERATOR_GREATER_THAN_OR_EQUAL) ||
               match(TokenType.OPERATOR_SPACESHIP)) {
            
            Token opToken = tokens.get(position - 1);
            SourceLocation location = createLocation();
            ASTNode right = parseShift();
            
            BinaryOpNode.Operator operator = switch (opToken.getType()) {
                case OPERATOR_LESS_THAN -> BinaryOpNode.Operator.LESS_THAN;
                case OPERATOR_LESS_THAN_OR_EQUAL -> BinaryOpNode.Operator.LESS_THAN_OR_EQUAL;
                case OPERATOR_GREATER_THAN -> BinaryOpNode.Operator.GREATER_THAN;
                case OPERATOR_GREATER_THAN_OR_EQUAL -> BinaryOpNode.Operator.GREATER_THAN_OR_EQUAL;
                case OPERATOR_SPACESHIP -> BinaryOpNode.Operator.SPACESHIP;
                default -> throw error("Invalid comparison operator");
            };

            left = new BinaryOpNode(operator, left, right, location);
        }
        
        if (match(TokenType.KEYWORD_INSTANCEOF)) {
            SourceLocation location = createLocation();
            
            StringBuilder typeName = new StringBuilder();
            
            while (true) {
                if (check(TokenType.IDENTIFIER) || isTypeKeyword(peek().getType())) {
                    if (typeName.length() > 0 && !typeName.toString().endsWith(".")) {
                        break;
                    }
                    String text = advance().getText();
                    typeName.append(text);
                    
                    if (check(TokenType.OPERATOR_DOT)) {
                        advance();
                        typeName.append(".");
                        continue;
                    }
                }
                
                if (check(TokenType.DELIMITER_LEFT_BRACKET)) {
                    advance();
                    if (check(TokenType.DELIMITER_RIGHT_BRACKET)) {
                        advance();
                        typeName.append("[]");
                        continue;
                    } else {
                        position--;
                        break;
                    }
                }
                
                break;
            }
            
            String typeStr = typeName.toString();
            if (typeStr.endsWith(".")) {
                typeStr = typeStr.substring(0, typeStr.length() - 1);
            }
            
            left = new InstanceofNode(left, typeStr, location);
        }
        
        return left;
    }
    
    /**
     * 解析移位表达式
     */
    private ASTNode parseShift() throws ParseException {
        ASTNode left = parseAdditive();
        
        while (match(TokenType.OPERATOR_LEFT_SHIFT) || 
               match(TokenType.OPERATOR_RIGHT_SHIFT) ||
               match(TokenType.OPERATOR_UNSIGNED_RIGHT_SHIFT)) {
            
            Token opToken = tokens.get(position - 1);
            SourceLocation location = createLocation();
            ASTNode right = parseAdditive();
            
            BinaryOpNode.Operator operator = switch (opToken.getType()) {
                case OPERATOR_LEFT_SHIFT -> BinaryOpNode.Operator.LEFT_SHIFT;
                case OPERATOR_RIGHT_SHIFT -> BinaryOpNode.Operator.RIGHT_SHIFT;
                case OPERATOR_UNSIGNED_RIGHT_SHIFT -> BinaryOpNode.Operator.UNSIGNED_RIGHT_SHIFT;
                default -> throw error("Invalid shift operator");
            };

            left = new BinaryOpNode(operator, left, right, location);
        }
        
        return left;
    }
    
    /**
     * 解析加法表达式
     */
    private ASTNode parseAdditive() throws ParseException {
        ASTNode left = parseMultiplicative();
        
        while (match(TokenType.OPERATOR_PLUS) || match(TokenType.OPERATOR_MINUS)) {
            Token opToken = tokens.get(position - 1);
            SourceLocation location = createLocation();
            ASTNode right = parseMultiplicative();
            
            BinaryOpNode.Operator operator = opToken.getType() == TokenType.OPERATOR_PLUS ? 
                    BinaryOpNode.Operator.ADD : BinaryOpNode.Operator.SUBTRACT;
            left = new BinaryOpNode(operator, left, right, location);
        }
        
        return left;
    }
    
    /**
     * 解析乘法表达式
     */
    private ASTNode parseMultiplicative() throws ParseException {
        ASTNode left = parsePower();
        
        while (match(TokenType.OPERATOR_MULTIPLY) || 
               match(TokenType.OPERATOR_DIVIDE) || 
               match(TokenType.OPERATOR_MODULO) ||
               match(TokenType.OPERATOR_INT_DIVIDE) ||
               match(TokenType.OPERATOR_MATH_MODULO)) {
            
            Token opToken = tokens.get(position - 1);
            SourceLocation location = createLocation();
            ASTNode right = parsePower();
            
            BinaryOpNode.Operator operator = switch (opToken.getType()) {
                case OPERATOR_MULTIPLY -> BinaryOpNode.Operator.MULTIPLY;
                case OPERATOR_DIVIDE -> BinaryOpNode.Operator.DIVIDE;
                case OPERATOR_MODULO -> BinaryOpNode.Operator.MODULO;
                case OPERATOR_INT_DIVIDE -> BinaryOpNode.Operator.INT_DIVIDE;
                case OPERATOR_MATH_MODULO -> BinaryOpNode.Operator.MATH_MODULO;
                default -> throw error("Invalid multiplicative operator");
            };

            left = new BinaryOpNode(operator, left, right, location);
        }
        
        return left;
    }
    
    /**
     * 解析幂运算表达式（右结合）
     */
    private ASTNode parsePower() throws ParseException {
        ASTNode left = parseUnary();
        
        if (match(TokenType.OPERATOR_POWER)) {
            SourceLocation location = createLocation();
            ASTNode right = parsePower();
            return new BinaryOpNode(BinaryOpNode.Operator.POWER, left, right, location);
        }
        
        return left;
    }
    
    /**
     * 解析一元表达式
     */
    private ASTNode parseUnary() throws ParseException {
        if (match(TokenType.OPERATOR_INCREMENT)) {
            SourceLocation location = createLocation();
            ASTNode operand = parseUnary();
            return new UnaryOpNode(UnaryOpNode.Operator.PRE_INCREMENT, operand, location);
        }
        
        if (match(TokenType.OPERATOR_DECREMENT)) {
            SourceLocation location = createLocation();
            ASTNode operand = parseUnary();
            return new UnaryOpNode(UnaryOpNode.Operator.PRE_DECREMENT, operand, location);
        }
        
        if (match(TokenType.OPERATOR_NOT_NULL)) {
            SourceLocation location = createLocation();
            ASTNode operand = parseUnary();
            return new UnaryOpNode(UnaryOpNode.Operator.NOT_NULL, operand, location);
        }
        
        if (match(TokenType.OPERATOR_PLUS) || match(TokenType.OPERATOR_MINUS) ||
            match(TokenType.OPERATOR_LOGICAL_NOT) || match(TokenType.OPERATOR_BITWISE_NOT)) {
            
            Token opToken = tokens.get(position - 1);
            SourceLocation location = createLocation();
            ASTNode operand = parseUnary();
            
            UnaryOpNode.Operator operator = switch (opToken.getType()) {
                case OPERATOR_PLUS -> UnaryOpNode.Operator.POSITIVE;
                case OPERATOR_MINUS -> UnaryOpNode.Operator.NEGATIVE;
                case OPERATOR_LOGICAL_NOT -> UnaryOpNode.Operator.LOGICAL_NOT;
                case OPERATOR_BITWISE_NOT -> UnaryOpNode.Operator.BITWISE_NOT;
                default -> throw error("Invalid unary operator");
            };

            return new UnaryOpNode(operator, operand, location);
        }
        
        if (check(TokenType.DELIMITER_LEFT_PAREN)) {
            savePosition();
            try {
                advance();
                Class<?> targetType = parseType();
                
                if (check(TokenType.DELIMITER_RIGHT_PAREN)) {
                    advance();
                    SourceLocation location = createLocation();
                    ASTNode expression = parseUnary();
                    releasePosition();
                    return new CastNode(targetType, expression, location);
                } else {
                    restorePosition();
                }
            } catch (ParseException e) {
                restorePosition();
            }
        }
        
        return parsePostfix();
    }
    
    /**
     * 解析后缀表达式（链式调用）
     */
    private ASTNode parsePostfix() throws ParseException {
        ASTNode expr = parsePrimary();
        
        while (true) {
            boolean matched = false;
            
            if (match(TokenType.OPERATOR_INCREMENT)) {
                SourceLocation location = createLocation();
                expr = new UnaryOpNode(UnaryOpNode.Operator.POST_INCREMENT, expr, location);
                matched = true;
            } else if (match(TokenType.OPERATOR_DECREMENT)) {
                SourceLocation location = createLocation();
                expr = new UnaryOpNode(UnaryOpNode.Operator.POST_DECREMENT, expr, location);
                matched = true;
            } else if (match(TokenType.OPERATOR_DOT)) {
                expr = parseMemberAccess(expr);
                matched = true;
            } else if (match(TokenType.OPERATOR_SAFE_DOT)) {
                expr = parseSafeMemberAccess(expr);
                matched = true;
            } else if (match(TokenType.DELIMITER_LEFT_PAREN)) {
                expr = parseMethodCall(expr);
                matched = true;
            } else if (match(TokenType.DELIMITER_LEFT_BRACKET)) {
                expr = parseArrayAccess(expr);
                matched = true;
            }
            
            if (!matched) {
                break;
            }
        }
        
        while (match(TokenType.OPERATOR_DOUBLE_COLON)) {
            SourceLocation location = createLocation();
            String methodName;
            
            if (check(TokenType.IDENTIFIER)) {
                methodName = advance().getText();
            } else if (check(TokenType.KEYWORD_NEW)) {
                methodName = advance().getText();
            } else {
                throw error("Expected method name after '::'");
            }
            
            expr = new MethodReferenceNode(expr, methodName, location);
        }
        
        return expr;
    }
    
    private ASTNode parseInterpolatedString(Token token) throws ParseException {
        List<?> parts = (List<?>) token.getValue();
        InterpolatedStringNode.Builder builder = new InterpolatedStringNode.Builder();
        builder.location(token.getLocation());
        
        for (Object part : parts) {
            if (part instanceof String) {
                builder.addLiteral((String) part);
            } else if (part instanceof Lexer.InterpolationPart) {
                String exprStr = ((Lexer.InterpolationPart) part).getExpression();
                Lexer subLexer = new Lexer(exprStr);
                List<Token> subTokens = subLexer.tokenize();
                Parser subParser = new Parser(subTokens, context, classLoader);
                ASTNode expr = subParser.parseExpression();
                builder.addExpression(expr);
            }
        }
        
        return builder.build();
    }
    
    /**
     * 解析主表达式
     */
    private ASTNode parsePrimary() throws ParseException {
        if (match(TokenType.LITERAL_INTEGER)) {
            Token token = tokens.get(position - 1);
            return new LiteralNode(token.getValue(), int.class, token.getLocation());
        }
        
        if (match(TokenType.LITERAL_LONG)) {
            Token token = tokens.get(position - 1);
            return new LiteralNode(token.getValue(), long.class, token.getLocation());
        }
        
        if (match(TokenType.LITERAL_DECIMAL)) {
            Token token = tokens.get(position - 1);
            return new LiteralNode(token.getValue(), double.class, token.getLocation());
        }
        
        if (match(TokenType.LITERAL_STRING)) {
            Token token = tokens.get(position - 1);
            return new LiteralNode(token.getValue(), String.class, token.getLocation());
        }
        
        if (match(TokenType.LITERAL_INTERPOLATED_STRING)) {
            Token token = tokens.get(position - 1);
            return parseInterpolatedString(token);
        }
        
        if (match(TokenType.LITERAL_CHAR)) {
            Token token = tokens.get(position - 1);
            return new LiteralNode(token.getValue(), char.class, token.getLocation());
        }
        
        if (match(TokenType.LITERAL_BOOLEAN)) {
            Token token = tokens.get(position - 1);
            return new LiteralNode(token.getValue(), boolean.class, token.getLocation());
        }
        
        if (match(TokenType.LITERAL_NULL)) {
            Token token = tokens.get(position - 1);
            return new LiteralNode(null, null, token.getLocation());
        }
        
        if (match(TokenType.KEYWORD_THIS)) {
            return new VariableNode("this", createLocation());
        }
        
        if (match(TokenType.KEYWORD_SUPER)) {
            return new VariableNode("super", createLocation());
        }
        
        if (check(TokenType.KEYWORD_INT) || check(TokenType.KEYWORD_LONG) ||
            check(TokenType.KEYWORD_FLOAT) || check(TokenType.KEYWORD_DOUBLE) ||
            check(TokenType.KEYWORD_BOOLEAN) || check(TokenType.KEYWORD_CHAR) ||
            check(TokenType.KEYWORD_BYTE) || check(TokenType.KEYWORD_SHORT) ||
            check(TokenType.KEYWORD_VOID)) {
            Token token = advance();
            String typeName = token.getText();
            Class<?> primitiveClass = getPrimitiveClass(typeName);
            return new ClassReferenceNode(new GenericType(primitiveClass), token.getLocation());
        }
        
        if (check(TokenType.DELIMITER_LEFT_BRACKET)) {
            return parseArrayLiteral();
        }
        
        if (check(TokenType.DELIMITER_LEFT_BRACE)) {
            return parseArrayInitializerBrace();
        }
        
        if (check(TokenType.IDENTIFIER)) {
            savePosition();
            try {
                ASTNode lambda = tryParseLambda();
                if (lambda != null) {
                    releasePosition();
                    return lambda;
                }
            } catch (ParseException e) {
                restorePosition();
            }
            
            Token token = advance();
            
            savePosition();
            try {
                StringBuilder className = new StringBuilder(token.getText());
                String lastValidClassName = null;
                int lastValidPosition = position;
                
                String simpleResolved = resolveClassName(token.getText());
                if (simpleResolved != null) {
                    lastValidClassName = token.getText();
                    lastValidPosition = position;
                }
                
                while (check(TokenType.OPERATOR_DOT) && !isAtEnd()) {
                    advance();
                    if (check(TokenType.IDENTIFIER)) {
                        className.append(".").append(advance().getText());
                        String currentClassName = className.toString();
                        
                        String resolved = resolveClassName(currentClassName);
                        
                        if (resolved != null) {
                            lastValidClassName = currentClassName;
                            lastValidPosition = position;
                        }
                    } else {
                        break;
                    }
                }
                
                if (lastValidClassName != null) {
                    position = lastValidPosition;
                    
                    if (check(TokenType.OPERATOR_LESS_THAN)) {
                        advance();
                        StringBuilder genericType = new StringBuilder(lastValidClassName).append("<");
                        parseTypeArguments(genericType);
                        releasePosition();
                        return new ClassReferenceNode(resolveGenericType(genericType.toString()), token.getLocation());
                    }
                    
                    releasePosition();
                    return new ClassReferenceNode(resolveGenericType(lastValidClassName), token.getLocation());
                }
                
                restorePosition();
                return new VariableNode(token.getText(), token.getLocation());
            } catch (ParseException e) {
                restorePosition();
                return new VariableNode(token.getText(), token.getLocation());
            }
        }
        
        if (check(TokenType.DELIMITER_LEFT_PAREN)) {
            savePosition();
            try {
                ASTNode lambda = tryParseLambda();
                if (lambda != null) {
                    releasePosition();
                    return lambda;
                } else {
                    restorePosition();
                }
            } catch (ParseException e) {
                restorePosition();
            }
            advance();
            return parseParenthesizedExpression();
        }
        
        if (match(TokenType.KEYWORD_NEW)) {
            return parseConstructorCall();
        }
        
        throw error("Expected expression");
    }
    
    /**
     * 解析括号表达式
     */
    private ASTNode parseParenthesizedExpression() throws ParseException {
        ASTNode expression = parseExpression();
        consume(TokenType.DELIMITER_RIGHT_PAREN, "Expected ')' after expression");
        return expression;
    }
    
    /**
     * 解析数组字面量
     * 语法: [element1, element2, ...]
     */
    private ArrayLiteralNode parseArrayLiteral() throws ParseException {
        SourceLocation location = createLocation();
        
        consume(TokenType.DELIMITER_LEFT_BRACKET, "Expected '['");
        
        List<ASTNode> elements = new ArrayList<>();
        
        if (!check(TokenType.DELIMITER_RIGHT_BRACKET)) {
            do {
                elements.add(parseExpression());
            } while (match(TokenType.DELIMITER_COMMA));
        }
        
        consume(TokenType.DELIMITER_RIGHT_BRACKET, "Expected ']' after array literal");
        
        return new ArrayLiteralNode(elements, location);
    }
    
    /**
     * 解析花括号数组初始化器或Map字面量
     * 语法: 
     *   数组: {element1, element2, ...}
     *   Map: {key: value, key2: value2, ...}
     */
    private ASTNode parseArrayInitializerBrace() throws ParseException {
        SourceLocation location = createLocation();
        
        consume(TokenType.DELIMITER_LEFT_BRACE, "Expected '{'");
        
        if (check(TokenType.DELIMITER_RIGHT_BRACE)) {
            consume(TokenType.DELIMITER_RIGHT_BRACE, "Expected '}'");
            return new ArrayLiteralNode(new ArrayList<>(), location);
        }
        
        ASTNode firstElement = parseExpression();
        
        
        if (check(TokenType.OPERATOR_COLON)) {
            return parseMapLiteral(firstElement, location);
        }
        
        List<ASTNode> elements = new ArrayList<>();
        elements.add(firstElement);
        
        while (match(TokenType.DELIMITER_COMMA)) {
            if (check(TokenType.DELIMITER_RIGHT_BRACE)) {
                break;
            }
            elements.add(parseExpression());
        }
        
        consume(TokenType.DELIMITER_RIGHT_BRACE, "Expected '}' after array initializer");
        
        return new ArrayLiteralNode(elements, location);
    }
    
    /**
     * 解析Map字面量
     * 语法: {key: value, key2: value2, ...}
     */
    private MapLiteralNode parseMapLiteral(ASTNode firstKey, SourceLocation location) throws ParseException {
        Map<ASTNode, ASTNode> entries = new LinkedHashMap<>();
        
        consume(TokenType.OPERATOR_COLON, "Expected ':' in map literal");
        ASTNode firstValue = parseExpression();
        entries.put(firstKey, firstValue);
        
        while (match(TokenType.DELIMITER_COMMA)) {
            if (check(TokenType.DELIMITER_RIGHT_BRACE)) {
                break;
            }
            ASTNode key = parseExpression();
            consume(TokenType.OPERATOR_COLON, "Expected ':' in map literal");
            ASTNode value = parseExpression();
            entries.put(key, value);
        }
        
        consume(TokenType.DELIMITER_RIGHT_BRACE, "Expected '}' after map literal");
        
        return new MapLiteralNode(entries, location);
    }
    
    /**
     * 解析构造函数调用
     * 支持:
     *   - new ClassName(args)
     *   - new type[size]
     *   - new type[] {elements}
     *   - new type[]{elements} (type already includes [])
     */
    private ASTNode parseConstructorCall() throws ParseException {
        SourceLocation location = createLocation();
        
        GenericType genericType = parseGenericType();
        
        if (genericType.isArray() && check(TokenType.DELIMITER_LEFT_BRACE)) {
            return parseArrayInitializer(genericType.getRuntimeType(), location);
        }
        
        if (check(TokenType.DELIMITER_LEFT_BRACKET)) {
            advance();
            
            if (check(TokenType.DELIMITER_RIGHT_BRACKET)) {
                advance();
                
                if (check(TokenType.DELIMITER_LEFT_BRACE)) {
                    return parseArrayInitializer(genericType.getRuntimeType(), location);
                } else {
                    throw error("Expected array initializer after 'new type[]'");
                }
            } else {
                ASTNode size = parseExpression();
                consume(TokenType.DELIMITER_RIGHT_BRACKET, "Expected ']' after array size");
                
                if (check(TokenType.DELIMITER_LEFT_BRACE)) {
                    return parseArrayInitializerWithSize(genericType.getRuntimeType(), size, location);
                }
                
                return new NewArrayNode(genericType.getRuntimeType(), size, location);
            }
        }
        
        consume(TokenType.DELIMITER_LEFT_PAREN, "Expected '(' after type");
        
        List<ASTNode> arguments = new ArrayList<>();
        if (!check(TokenType.DELIMITER_RIGHT_PAREN)) {
            do {
                arguments.add(parseExpression());
            } while (match(TokenType.DELIMITER_COMMA));
        }
        
        consume(TokenType.DELIMITER_RIGHT_PAREN, "Expected ')' after constructor arguments");
        
        return new ConstructorCallNode(genericType, arguments, null, location);
    }
    
    /**
     * 解析带大小的数组初始化器
     * 语法: new int[3] {1, 2, 3}
     */
    private ASTNode parseArrayInitializerWithSize(Class<?> type, ASTNode size, SourceLocation location) throws ParseException {
        consume(TokenType.DELIMITER_LEFT_BRACE, "Expected '{'");
        
        List<ASTNode> elements = new ArrayList<>();
        
        Class<?> componentType = type.isArray() ? type.getComponentType() : type;
        
        if (!check(TokenType.DELIMITER_RIGHT_BRACE)) {
            do {
                if (check(TokenType.DELIMITER_LEFT_BRACE) && componentType.isArray()) {
                    elements.add(parseArrayInitializer(componentType, createLocation()));
                } else {
                    elements.add(parseExpression());
                }
            } while (match(TokenType.DELIMITER_COMMA));
        }
        
        consume(TokenType.DELIMITER_RIGHT_BRACE, "Expected '}' after array initializer");
        
        return new ArrayLiteralNode(elements, componentType, location);
    }
    
    /**
     * 解析数组初始化器
     * 语法: {element1, element2, ...}
     */
    private ArrayLiteralNode parseArrayInitializer(Class<?> type, SourceLocation location) throws ParseException {
        consume(TokenType.DELIMITER_LEFT_BRACE, "Expected '{'");
        
        List<ASTNode> elements = new ArrayList<>();
        
        Class<?> componentType = type.isArray() ? type.getComponentType() : type;
        
        if (!check(TokenType.DELIMITER_RIGHT_BRACE)) {
            do {
                if (check(TokenType.DELIMITER_LEFT_BRACE) && componentType.isArray()) {
                    elements.add(parseArrayInitializer(componentType, createLocation()));
                } else {
                    elements.add(parseExpression());
                }
            } while (match(TokenType.DELIMITER_COMMA));
        }
        
        consume(TokenType.DELIMITER_RIGHT_BRACE, "Expected '}' after array initializer");
        
        return new ArrayLiteralNode(elements, componentType, location);
    }
    
    /**
     * 解析成员访问
     */
    private ASTNode parseMemberAccess(ASTNode target) throws ParseException {
        SourceLocation location = createLocation();
        
        if (check(TokenType.KEYWORD_CLASS)) {
            advance();
            if (target instanceof ClassReferenceNode classRef) {
                Class<?> clazz = classRef.getType().getRawType();
                return new LiteralNode(clazz, Class.class, location);
            } else if (target instanceof VariableNode) {
                String varName = ((VariableNode) target).getName();
                Class<?> clazz = ClassFinder.findClassWithImports(varName, context.getClassLoader(), context.getImports());
                if (clazz != null) {
                    return new LiteralNode(clazz, Class.class, location);
                }
            }
            throw new ParseException(
                "Cannot resolve .class for: " + target,
                location,
                ErrorCode.PARSE_INVALID_SYNTAX
            );
        }
        
        if (check(TokenType.KEYWORD_NEW)) {
            advance();
            if (target instanceof ClassReferenceNode classRef) {
                consume(TokenType.DELIMITER_LEFT_PAREN, "Expected '(' after 'new'");
                List<ASTNode> arguments = new ArrayList<>();
                if (!check(TokenType.DELIMITER_RIGHT_PAREN)) {
                    do {
                        arguments.add(parseExpression());
                    } while (match(TokenType.DELIMITER_COMMA));
                }
                consume(TokenType.DELIMITER_RIGHT_PAREN, "Expected ')' after constructor arguments");
                return new ConstructorCallNode(classRef.getType(), arguments, null, location);
            } else if (target instanceof VariableNode) {
                String varName = ((VariableNode) target).getName();
                Class<?> clazz = ClassFinder.findClassWithImports(varName, context.getClassLoader(), context.getImports());
                if (clazz != null) {
                    consume(TokenType.DELIMITER_LEFT_PAREN, "Expected '(' after 'new'");
                    List<ASTNode> arguments = new ArrayList<>();
                    if (!check(TokenType.DELIMITER_RIGHT_PAREN)) {
                        do {
                            arguments.add(parseExpression());
                        } while (match(TokenType.DELIMITER_COMMA));
                    }
                    consume(TokenType.DELIMITER_RIGHT_PAREN, "Expected ')' after constructor arguments");
                    return new ConstructorCallNode(new GenericType(clazz), arguments, null, location);
                }
            }
            throw new ParseException(
                "Cannot resolve constructor for: " + target,
                location,
                ErrorCode.PARSE_INVALID_SYNTAX
            );
        }
        
        String memberName = consume(TokenType.IDENTIFIER, "Expected member name").getText();
        
        if (target instanceof ClassReferenceNode classRef) {
            String nestedClassName = classRef.getType().getTypeName() + "." + memberName;
            
            String resolved = resolveClassName(nestedClassName);
            if (resolved != null) {
                return new ClassReferenceNode(resolveGenericType(nestedClassName), location);
            }
            
            String innerClassName = classRef.getType().getTypeName() + "$" + memberName;
            String resolvedInner = resolveClassName(innerClassName);
            if (resolvedInner != null) {
                return new ClassReferenceNode(resolveGenericType(nestedClassName), location);
            }
            
            try {
                Class<?> clazz = ClassResolver.findClassOrFail(classRef.getType().getTypeName(), classLoader);
                try {
                    clazz.getDeclaredField(memberName);
                    FieldAccessNode fieldAccess = new FieldAccessNode(target, memberName, location);
                    
                    if (check(TokenType.DELIMITER_LEFT_PAREN)) {
                        advance();
                        List<ASTNode> arguments = new ArrayList<>();
                        if (!check(TokenType.DELIMITER_RIGHT_PAREN)) {
                            do {
                                arguments.add(parseExpression());
                            } while (match(TokenType.DELIMITER_COMMA));
                        }
                        consume(TokenType.DELIMITER_RIGHT_PAREN, "Expected ')' after method arguments");
                        return new MethodCallNode(fieldAccess, memberName, arguments, location);
                    }
                    
                    return fieldAccess;
                } catch (NoSuchFieldException e) {
                    // 不是字段，继续处理
                }
            } catch (ClassNotFoundException e) {
                // 类不存在，继续处理
            }
        }
        
        if (check(TokenType.DELIMITER_LEFT_PAREN)) {
            advance();
            List<ASTNode> arguments = new ArrayList<>();
            
            if (!check(TokenType.DELIMITER_RIGHT_PAREN)) {
                do {
                    arguments.add(parseExpression());
                } while (match(TokenType.DELIMITER_COMMA));
            }
            
            consume(TokenType.DELIMITER_RIGHT_PAREN, "Expected ')' after method arguments");
            
            return new MethodCallNode(target, memberName, arguments, location);
        } else {
            return new FieldAccessNode(target, memberName, location);
        }
    }
    
    private ASTNode parseSafeMemberAccess(ASTNode target) throws ParseException {
        SourceLocation location = createLocation();
        String memberName = consume(TokenType.IDENTIFIER, "Expected member name").getText();
        
        if (check(TokenType.DELIMITER_LEFT_PAREN)) {
            advance();
            List<ASTNode> arguments = new ArrayList<>();
            
            if (!check(TokenType.DELIMITER_RIGHT_PAREN)) {
                do {
                    arguments.add(parseExpression());
                } while (match(TokenType.DELIMITER_COMMA));
            }
            
            consume(TokenType.DELIMITER_RIGHT_PAREN, "Expected ')' after method arguments");
            return new SafeMethodCallNode(target, memberName, arguments, location);
        }
        
        return new SafeFieldAccessNode(target, memberName, location);
    }
    
    /**
     * 解析方法调用
     */
    private ASTNode parseMethodCall(ASTNode target) throws ParseException {
        SourceLocation location = createLocation();
        List<ASTNode> arguments = new ArrayList<>();
        
        if (!check(TokenType.DELIMITER_RIGHT_PAREN)) {
            do {
                arguments.add(parseExpression());
            } while (match(TokenType.DELIMITER_COMMA));
        }
        
        consume(TokenType.DELIMITER_RIGHT_PAREN, "Expected ')' after method arguments");
        
        if (target instanceof VariableNode) {
            String functionName = ((VariableNode) target).getName();
            return new FunctionCallNode(functionName, arguments, location);
        }
        String methodName = null;
        if (target instanceof FieldAccessNode) {
            methodName = ((FieldAccessNode) target).getFieldName();
        }
        return new MethodCallNode(target, methodName, arguments, location);
    }
    
    /**
     * 解析数组访问
     */
    private ASTNode parseArrayAccess(ASTNode array) throws ParseException {
        SourceLocation location = createLocation();
        ASTNode index = parseExpression();
        consume(TokenType.DELIMITER_RIGHT_BRACKET, "Expected ']' after array index");
        return new ArrayAccessNode(array, index, location);
    }
    
    /**
     * 尝试解析Lambda表达式
     * 支持以下格式：
     * - x -> x + 1
     * - (x, y) -> x + y
     * - (int x, String y) -> x + y
     * - (x, y) -> { return x + y; }
     * - () -> 42
     * 
     * @return LambdaNode如果解析成功，null如果不是Lambda表达式
     * @throws ParseException 如果解析过程中出错
     */
    private ASTNode tryParseLambda() throws ParseException {
        SourceLocation location = createLocation();
        List<LambdaNode.Parameter> parameters = new ArrayList<>();
        
        if (check(TokenType.IDENTIFIER) && checkNext(TokenType.DELIMITER_ARROW)) {
            Token paramToken = peek();
            String paramName = paramToken.getText();
            
            if (context != null && context.isClassDeclared(paramName)) {
                return null;
            }
            
            advance();
            parameters.add(new LambdaNode.Parameter(paramName, Object.class));
            advance();
        } else if (check(TokenType.DELIMITER_LEFT_PAREN)) {
            int savedPos = position;
            advance();
            
            if (!check(TokenType.DELIMITER_RIGHT_PAREN)) {
                do {
                    if (!check(TokenType.IDENTIFIER)) {
                        position = savedPos;
                        return null;
                    }
                    Token paramToken = advance();
                    parameters.add(new LambdaNode.Parameter(paramToken.getText(), Object.class));
                } while (match(TokenType.DELIMITER_COMMA));
            }
            
            if (!check(TokenType.DELIMITER_RIGHT_PAREN)) {
                position = savedPos;
                return null;
            }
            advance();
            
            if (!check(TokenType.DELIMITER_ARROW)) {
                position = savedPos;
                return null;
            }
            advance();
        } else {
            return null;
        }
        
        ASTNode body;
        if (check(TokenType.DELIMITER_LEFT_BRACE)) {
            body = parseLambdaBlock();
        } else {
            body = parseExpression();
        }
        
        return new LambdaNode(parameters, body, location);
    }
    
    private BlockNode parseLambdaBlock() throws ParseException {
        SourceLocation location = createLocation();
        List<ASTNode> statements = new ArrayList<>();
        
        consume(TokenType.DELIMITER_LEFT_BRACE, "Expected '{' at start of lambda body");
        
        while (!check(TokenType.DELIMITER_RIGHT_BRACE) && !check(TokenType.EOF)) {
            statements.add(parseLambdaStatement());
        }
        
        consume(TokenType.DELIMITER_RIGHT_BRACE, "Expected '}' at end of lambda body");
        
        return new BlockNode(statements, location);
    }
    
    private ASTNode parseLambdaStatement() throws ParseException {
        if (match(TokenType.KEYWORD_IF)) {
            return parseIfStatement();
        }
        
        if (match(TokenType.KEYWORD_FOR)) {
            return parseForStatement();
        }
        
        if (match(TokenType.KEYWORD_WHILE)) {
            return parseWhileStatement();
        }
        
        if (match(TokenType.KEYWORD_RETURN)) {
            return parseReturnStatement();
        }
        
        if (match(TokenType.KEYWORD_BREAK)) {
            return parseBreakStatement();
        }
        
        if (match(TokenType.KEYWORD_CONTINUE)) {
            return parseContinueStatement();
        }
        
        if (match(TokenType.DELIMITER_LEFT_BRACE)) {
            return parseLambdaBlock();
        }
        
        if (check(TokenType.IDENTIFIER) && checkNext(TokenType.OPERATOR_DECLARE_ASSIGN)) {
            return parseShortDeclaration();
        }
        
        if (check(TokenType.KEYWORD_INT) || check(TokenType.KEYWORD_LONG) ||
            check(TokenType.KEYWORD_FLOAT) || check(TokenType.KEYWORD_DOUBLE) ||
            check(TokenType.KEYWORD_BOOLEAN) || check(TokenType.KEYWORD_CHAR) ||
            check(TokenType.KEYWORD_BYTE) || check(TokenType.KEYWORD_SHORT) ||
            check(TokenType.KEYWORD_AUTO)) {
            return parseVariableDeclaration();
        }
        
        ASTNode expression = parseExpression();
        if (check(TokenType.DELIMITER_SEMICOLON)) {
            match(TokenType.DELIMITER_SEMICOLON);
        }
        return expression;
    }
    
    private boolean checkNext(TokenType type) {
        if (position + 1 >= tokens.size()) {
            return false;
        }
        return tokens.get(position + 1).getType() == type;
    }
    
    private boolean isTypeFollowedByClass() {
        if (position + 1 >= tokens.size()) {
            return false;
        }
        return tokens.get(position + 1).getType() == TokenType.OPERATOR_DOT &&
               position + 2 < tokens.size() &&
               tokens.get(position + 2).getType() == TokenType.KEYWORD_CLASS;
    }
    
    private Class<?> getPrimitiveClass(String typeName) {
        return switch (typeName) {
            case "int" -> int.class;
            case "long" -> long.class;
            case "float" -> float.class;
            case "double" -> double.class;
            case "boolean" -> boolean.class;
            case "char" -> char.class;
            case "byte" -> byte.class;
            case "short" -> short.class;
            case "void" -> void.class;
            default -> Object.class;
        };
    }
}
