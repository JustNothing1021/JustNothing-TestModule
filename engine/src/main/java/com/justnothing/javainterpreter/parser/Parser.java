package com.justnothing.javainterpreter.parser;

import com.justnothing.javainterpreter.api.ClassResolver;
import com.justnothing.javainterpreter.ast.GenericType;
import com.justnothing.javainterpreter.ast.SourceLocation;
import com.justnothing.javainterpreter.ast.ASTNode;
import com.justnothing.javainterpreter.ast.nodes.*;
import com.justnothing.javainterpreter.evaluator.AutoClass;
import com.justnothing.javainterpreter.exception.ParseException;
import com.justnothing.javainterpreter.lexer.Lexer;
import com.justnothing.javainterpreter.lexer.Token;
import com.justnothing.javainterpreter.lexer.TokenType;
import com.justnothing.javainterpreter.exception.ErrorCode;
import com.justnothing.javainterpreter.utils.TypeUtils;

import java.lang.reflect.Array;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

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
    

    public static void clearFailedClassCache() {
        classNameCache.entrySet().removeIf(entry -> NOT_A_CLASS.equals(entry.getValue()));
    }
    
    private final List<Token> tokens;
    private int position;
    private final ParseContext context;
    private final Deque<Integer> savedPositions;
    private final ClassLoader classLoader;
    private final String sourceFileName;


    public Parser(List<Token> tokens, String sourceFileName) {
        this(tokens, Thread.currentThread().getContextClassLoader(), sourceFileName);
    }

    public Parser(List<Token> tokens, ClassLoader classLoader, String sourceFileName) {
        this.sourceFileName = sourceFileName;
        this.tokens = tokens;
        this.position = 0;
        this.context = new ParseContext();
        this.context.setClassLoader(classLoader);
        this.savedPositions = new ArrayDeque<>();
        this.classLoader = classLoader;
    }
    
    public Parser(List<Token> tokens, ParseContext context, String sourceFileName) {
        this(tokens, context, Thread.currentThread().getContextClassLoader(), sourceFileName);
    }
    
    public Parser(List<Token> tokens, ParseContext context, ClassLoader classLoader, String sourceFileName) {
        this.sourceFileName = sourceFileName;
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
    
    @SuppressWarnings("unused")
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
        return peek().type() == type;
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
                token.location(),
                ErrorCode.PARSE_INVALID_SYNTAX
        );
    }

    @SuppressWarnings("SameParameterValue")
    private ParseException error(String message, ErrorCode errorCode) {
        Token token = peek();
        return new ParseException(
                message,
                token.location(),
                errorCode
        );
    }
    
    private SourceLocation createLocation() {
        if (isAtEnd()) {
            return tokens.get(tokens.size() - 1).location();
        }
        return peek().location();
    }
    
    private VariableNode createVariableNode(String name, SourceLocation location) {
        VariableNode node = new VariableNode(name, location);
        if (context.shouldResolveAsField(name)) {
            node.setFieldAccess(true);
        }
        return node;
    }
    
    private void savePosition() {
        savedPositions.push(position);
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
        List<AnnotationNode> annotations = parseAnnotations();
        
        if (match(TokenType.KEYWORD_ABSTRACT)) {
            if (match(TokenType.KEYWORD_CLASS)) {
                return parseClassDeclaration(annotations);
            } else if (match(TokenType.KEYWORD_INTERFACE)) {
                return parseInterfaceDeclaration(annotations);
            } else {
                throw error("Expected 'class' or 'interface' after 'abstract'");
            }
        }
        
        if (match(TokenType.KEYWORD_CLASS)) {
            return parseClassDeclaration(annotations);
        }
        
        if (match(TokenType.KEYWORD_INTERFACE)) {
            return parseInterfaceDeclaration(annotations);
        }
        
        if (!annotations.isEmpty()) {
            throw error("Annotations can only be applied to class, method, or field declarations");
        }
        
        if (isVariableDeclarationStart()) {
            savePosition();
            try {
                return parseVariableDeclaration();
            } catch (ParseException e) {
                if (e.getErrorCode() == ErrorCode.PARSE_CLASS_NOT_FOUND) {
                    restorePosition();
                } else {
                    throw e;
                }
            }
        }
        
        if (check(TokenType.IDENTIFIER)) {
            return parseExpressionStatement();
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

        int currentPosition = position;
        if (check(TokenType.DELIMITER_LEFT_BRACE)) {
            try {
                advance();
                return parseBlock();
            } catch (ParseException ignored) {
                position = currentPosition;
            }
        }
        return parseExpressionStatement();
    }
    
    /**
     * 解析表达式语句 (带 ASI 自动分号插入)
     */
    private ASTNode parseExpressionStatement() throws ParseException {
        ASTNode expression = parseExpression();
        consumeSemicolonASI();
        return expression;
    }
    
    /**
     * ASI (Automatic Semicolon Insertion): 自动分号插入
     * 如果下一个 token 是分号就吃掉，如果是 EOF 或流结束也允许省略
     */
    private void consumeSemicolonASI() throws ParseException {
        if (check(TokenType.DELIMITER_SEMICOLON)) {
            advance();
        } else if (!isAtEnd() && !peek().is(TokenType.EOF)) {
            throw error("Expected semicolon after statement");
        }
    }
    
    /**
     * 可选分号: 有就吃掉，没有也不报错
     */
    private void consumeOptionalSemicolon() {
        if (check(TokenType.DELIMITER_SEMICOLON)) {
            advance();
        }
    }
    
    /**
     * 解析变量声明
     */
    private ASTNode parseVariableDeclaration() throws ParseException {
        return parseVariableDeclaration(true);
    }
    
    private ASTNode parseVariableDeclaration(boolean consumeSemicolon) throws ParseException {
        SourceLocation location = createLocation();
        
        if (check(TokenType.IDENTIFIER) && checkNext(TokenType.OPERATOR_DECLARE_ASSIGN)) {
            String varName = consume(TokenType.IDENTIFIER, "Expected variable name").text();
            consume(TokenType.OPERATOR_DECLARE_ASSIGN, "Expected :=");
            ASTNode value = parseExpression();
            if (consumeSemicolon) {
                consume(TokenType.DELIMITER_SEMICOLON, "Expected semicolon after short declaration");
            }
            return new AssignmentNode(varName, value, true,
                new GenericType(AutoClass.class, Collections.emptyList(), 0, "auto"), location);
        }

        boolean isFinal = false;
        if (peek().type() == TokenType.KEYWORD_FINAL) {
            isFinal = true;
            advance();
        }
        
        GenericType type;
        boolean isAuto = false;
        
        if (peek().type() == TokenType.KEYWORD_AUTO) {
            advance();
            type = new GenericType(AutoClass.class, Collections.emptyList(), 0, "auto");
            isAuto = true;
        } else {
            type = parseGenericType();
        }
        
        List<AssignmentNode> declarations = new ArrayList<>();
        
        do {
            SourceLocation varLocation = createLocation();
            String varName = consume(TokenType.IDENTIFIER, "Expected variable name").text();
            
            ASTNode value = null;
            if (match(TokenType.OPERATOR_ASSIGN)) {
                value = parseExpression();
            } else if (isAuto) {
                throw error("Auto variable must have initial value");
            }
            
            declarations.add(new AssignmentNode(varName, value, true, type, isFinal, varLocation));
            
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
                if (check(TokenType.IDENTIFIER) || isTypeKeyword(peek().type())) {
                    if (typeName.length() > 0 && !typeName.toString().endsWith(".")) {
                        break;
                    }
                    String text = advance().text();
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
                if (check(TokenType.IDENTIFIER) || isTypeKeyword(peek().type())) {
                    if (typeName.length() > 0 && !typeName.toString().endsWith(".")) {
                        break;
                    }
                    String text = advance().text();
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
        
        if (check(TokenType.OPERATOR_QUESTION)) {
            advance();
            typeName.append("?");
            if (check(TokenType.KEYWORD_EXTENDS)) {
                advance();
                typeName.append(" extends ");
                typeName.append(parseTypeNameForGeneric());
            } else if (check(TokenType.KEYWORD_SUPER)) {
                advance();
                typeName.append(" super ");
                typeName.append(parseTypeNameForGeneric());
            }
            return typeName.toString();
        }
        
        if (check(TokenType.IDENTIFIER)) {
            typeName.append(advance().text());
            
            while (check(TokenType.OPERATOR_DOT)) {
                advance();
                typeName.append(".");
                if (check(TokenType.IDENTIFIER)) {
                    typeName.append(advance().text());
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
            return cached.equals(NOT_A_CLASS) ? null : cached;
        }
        
        Class<?> clazz = context.getClassFinder().findClassWithImports(className, context.getClassLoader(), context.getImports());
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

        baseType = context.getClassFinder().findClassWithImports(baseTypeName, context.getClassLoader(), context.getImports());
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
        
        if ("?".equals(baseTypeName)) {
            return new GenericType(Object.class, Collections.emptyList(), arrayDepth, "?");
        }
        
        if (baseTypeName.startsWith("? extends ")) {
            String boundType = baseTypeName.substring("? extends ".length());
            GenericType bound = resolveGenericType(boundType);
            return new GenericType(bound.getRuntimeType(), Collections.emptyList(), arrayDepth, "? extends " + boundType);
        }
        
        if (baseTypeName.startsWith("? super ")) {
            String boundType = baseTypeName.substring("? super ".length());
            GenericType bound = resolveGenericType(boundType);
            return new GenericType(bound.getRuntimeType(), Collections.emptyList(), arrayDepth, "? super " + boundType);
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
        baseType = context.getClassFinder().findClassWithImports(baseTypeName, context.getClassLoader(), context.getImports());
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
        
        if (isTypeKeyword(peek().type()) || peek().type() == TokenType.IDENTIFIER) {
            int savedPos = position;
            
            try {
                Class<?> itemType = parseType();
                String itemName = consume(TokenType.IDENTIFIER, "Expected variable name").text();
                
                if (check(TokenType.OPERATOR_COLON)) {
                    advance();
                    ASTNode collection = parseExpression();
                    consume(TokenType.DELIMITER_RIGHT_PAREN, "Expected ')' after for-each collection");
                    ASTNode body = parseStatement();
                    return new ForEachNode(itemType, itemName, collection, body, location);
                }
            } catch (ParseException e) {
                if (e.getErrorCode() == ErrorCode.PARSE_CLASS_NOT_FOUND)
                    throw e;
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
            if (isTypeKeyword(peek().type()) || peek().type() == TokenType.IDENTIFIER) {
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
        
        SwitchBodyResult result = parseSwitchBody(location);
        return new SwitchNode(expression, result.cases(), result.defaultCase(), location);
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
        
        SwitchBodyResult result = parseSwitchBody(location);
        return new SwitchNode(expression, result.cases(), result.defaultCase(), location);
    }

    /**
     * 解析 switch body (花括号内的 case/default 列表)
     */
    private SwitchBodyResult parseSwitchBody(SourceLocation location) throws ParseException {
        consume(TokenType.DELIMITER_LEFT_BRACE, "Expected '{' after switch expression");
        
        List<CaseNode> cases = new ArrayList<>();
        ASTNode defaultCase = null;
        
        while (!check(TokenType.DELIMITER_RIGHT_BRACE) && !isAtEnd()) {
            if (match(TokenType.KEYWORD_CASE)) {
                ASTNode caseValue = parseTernary();
                
                if (match(TokenType.DELIMITER_ARROW)) {
                    ASTNode caseExpr = parseExpression();
                    List<ASTNode> caseStatements = new ArrayList<>();
                    caseStatements.add(caseExpr);
                    consumeOptionalSemicolon();
                    cases.add(new CaseNode(caseValue, caseStatements, location));
                } else {
                    consume(TokenType.OPERATOR_COLON, "Expected '->' or ':' after case value");
                    cases.add(new CaseNode(caseValue, parseCaseBlockStatements(), location));
                }
            } else if (match(TokenType.KEYWORD_DEFAULT)) {
                if (match(TokenType.DELIMITER_ARROW)) {
                    defaultCase = parseExpression();
                    consumeOptionalSemicolon();
                } else {
                    consume(TokenType.OPERATOR_COLON, "Expected '->' or ':' after default");
                    defaultCase = new BlockNode(parseCaseBlockStatements(), location);
                }
            } else {
                throw error("Expected 'case' or 'default' in switch body");
            }
        }
        
        consume(TokenType.DELIMITER_RIGHT_BRACE, "Expected '}' at end of switch");
        
        return new SwitchBodyResult(cases, defaultCase);
    }
    
    /**
     * 收集 case/default 的语句块 (冒号语法)
     * 遇到 case、default 或 } 时停止
     */
    private List<ASTNode> parseCaseBlockStatements() throws ParseException {
        List<ASTNode> statements = new ArrayList<>();
        while (!check(TokenType.KEYWORD_CASE) && 
               !check(TokenType.KEYWORD_DEFAULT) && 
               !check(TokenType.DELIMITER_RIGHT_BRACE) &&
               !isAtEnd()) {
            statements.add(parseStatement());
        }
        return statements;
    }
    
    private record SwitchBodyResult(List<CaseNode> cases, ASTNode defaultCase) {}
    
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
        
        if (isTypeKeyword(peek().type())) {
            Class<?> type = parseType();
            String varName = consume(TokenType.IDENTIFIER, "Expected variable name").text();
            consume(TokenType.OPERATOR_ASSIGN, "Expected '=' in resource declaration");
            ASTNode initializer = parseExpression();
            return new ResourceDeclaration(type, varName, initializer, location);
        }
        
        if (check(TokenType.IDENTIFIER)) {
            int savedPos = position;
            try {
                Class<?> type = parseType();
                if (check(TokenType.IDENTIFIER) && checkNext(TokenType.OPERATOR_ASSIGN)) {
                    String varName = consume(TokenType.IDENTIFIER, "Expected variable name").text();
                    consume(TokenType.OPERATOR_ASSIGN, "Expected '=' in resource declaration");
                    ASTNode initializer = parseExpression();
                    return new ResourceDeclaration(type, varName, initializer, location);
                }
            } catch (ParseException ignored) {
            }
            position = savedPos;
        }
        
        String varName = consume(TokenType.IDENTIFIER, "Expected resource variable name").text();
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
        
        String variableName = consume(TokenType.IDENTIFIER, "Expected exception variable name").text();
        
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
                packageName.append(advance().text());
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
        
        String varName = consume(TokenType.IDENTIFIER, "Expected variable name after delete").text();
        consume(TokenType.DELIMITER_SEMICOLON, "Expected semicolon after delete");
        
        return new DeleteNode(varName, location);
    }
    
    /**
     * 解析class声明
     * 语法: [@Annotation] class ClassName [extends SuperClass] [implements Interface1, Interface2] { ... }
     */
    private ASTNode parseClassDeclaration(List<AnnotationNode> annotations) throws ParseException {
        SourceLocation location = createLocation();
        
        String className = consume(TokenType.IDENTIFIER, "Expected class name").text();
        
        List<String> typeParams = Collections.emptyList();
        if (check(TokenType.OPERATOR_LESS_THAN)) {
            typeParams = parseClassTypeParameters();
        }
        
        context.declareClass(className);
        for (String typeParam : typeParams) {
            context.declareClass(typeParam);
        }
        context.enterClass(className);
        
        try {
            ClassReferenceNode superClass = null;
            if (match(TokenType.KEYWORD_EXTENDS)) {
                superClass = parseTypeName();
            }
            
            List<ClassReferenceNode> interfaces = new ArrayList<>();
            if (match(TokenType.KEYWORD_IMPLEMENTS)) {
                do {
                    interfaces.add(parseTypeName());
                } while (match(TokenType.DELIMITER_COMMA));
            }
            
            ClassDeclarationNode classDecl = new ClassDeclarationNode(className, superClass, interfaces, location);
            
            for (AnnotationNode annotation : annotations) {
                classDecl.addAnnotation(annotation);
            }
            
            consume(TokenType.DELIMITER_LEFT_BRACE, "Expected '{' after class declaration");
            
            while (!check(TokenType.DELIMITER_RIGHT_BRACE) && !isAtEnd()) {
                List<AnnotationNode> memberAnnotations = parseAnnotations();
                ClassModifiers modifiers = parseModifiers();
                
                if (check(TokenType.IDENTIFIER) && checkNext(TokenType.DELIMITER_LEFT_PAREN)) {
                    String constructorName = consume(TokenType.IDENTIFIER, "Expected constructor name").text();
                    if (!constructorName.equals(className)) {
                        throw error("Constructor name must match class name");
                    }
                    ConstructorDeclarationNode constructor = parseConstructorDeclaration(className, modifiers);
                    for (AnnotationNode annotation : memberAnnotations) {
                        constructor.addAnnotation(annotation);
                    }
                    classDecl.addConstructor(constructor);
                } else if (isTypeStart()) {
                    ClassReferenceNode type = parseTypeName();
                    String memberName = consume(TokenType.IDENTIFIER, "Expected member name").text();
                    
                    if (check(TokenType.DELIMITER_LEFT_PAREN)) {
                        MethodDeclarationNode method = parseMethodDeclaration(memberName, type, modifiers);
                        for (AnnotationNode annotation : memberAnnotations) {
                            method.addAnnotation(annotation);
                        }
                        classDecl.addMethod(method);
                    } else {
                        FieldDeclarationNode field = parseFieldDeclaration(memberName, type, modifiers);
                        for (AnnotationNode annotation : memberAnnotations) {
                            field.addAnnotation(annotation);
                        }
                        classDecl.addField(field);
                        context.addField(memberName);
                    }
                } else {
                    throw error("Unexpected token in class body");
                }
            }
            
            consume(TokenType.DELIMITER_RIGHT_BRACE, "Expected '}' at end of class declaration");
            
            return classDecl;
        } finally {
            context.exitClass();
        }
    }
    
    private List<String> parseClassTypeParameters() throws ParseException {
        List<String> typeParams = new ArrayList<>();
        consume(TokenType.OPERATOR_LESS_THAN, "Expected '<'");
        int depth = 1;
        while (depth > 0 && !isAtEnd()) {
            TokenType type = peek().type();
            if (type == TokenType.OPERATOR_LESS_THAN) {
                advance();
                depth++;
            } else if (type == TokenType.OPERATOR_GREATER_THAN) {
                advance();
                depth--;
            } else if (type == TokenType.OPERATOR_RIGHT_SHIFT) {
                advance();
                depth -= 2;
            } else if (type == TokenType.OPERATOR_UNSIGNED_RIGHT_SHIFT) {
                advance();
                depth -= 3;
            } else if (type == TokenType.IDENTIFIER && depth == 1) {
                String paramName = advance().text();
                typeParams.add(paramName);
                if (check(TokenType.OPERATOR_LESS_THAN)) {
                    advance();
                    depth++;
                    continue;
                }
                if (check(TokenType.KEYWORD_EXTENDS)) {
                    advance();
                    parseTypeName();
                }
                if (check(TokenType.DELIMITER_COMMA)) {
                    advance();
                }
            } else {
                advance();
            }
        }
        return typeParams;
    }
    /**
     * 解析接口声明
     */
    private ASTNode parseInterfaceDeclaration(List<AnnotationNode> annotations) throws ParseException {
        SourceLocation location = createLocation();
        
        String interfaceName = consume(TokenType.IDENTIFIER, "Expected interface name").text();
        
        List<String> typeParams = Collections.emptyList();
        if (check(TokenType.OPERATOR_LESS_THAN)) {
            typeParams = parseClassTypeParameters();
        }
        
        context.declareClass(interfaceName);
        for (String typeParam : typeParams) {
            context.declareClass(typeParam);
        }
        context.enterClass(interfaceName);
        
        try {
            List<ClassReferenceNode> superInterfaces = new ArrayList<>();
            if (match(TokenType.KEYWORD_EXTENDS)) {
                do {
                    superInterfaces.add(parseTypeName());
                } while (match(TokenType.DELIMITER_COMMA));
            }
            
            ClassDeclarationNode interfaceDecl = new ClassDeclarationNode(interfaceName, null, superInterfaces, location);
            interfaceDecl.setInterface(true);
            
            for (AnnotationNode annotation : annotations) {
                interfaceDecl.addAnnotation(annotation);
            }
            
            consume(TokenType.DELIMITER_LEFT_BRACE, "Expected '{' after interface declaration");
            
            while (!check(TokenType.DELIMITER_RIGHT_BRACE) && !isAtEnd()) {
                List<AnnotationNode> memberAnnotations = parseAnnotations();
                ClassModifiers modifiers = parseModifiers();
                
                if (match(TokenType.KEYWORD_DEFAULT)) {
                    ClassReferenceNode type = parseTypeName();
                    String memberName = consume(TokenType.IDENTIFIER, "Expected member name").text();
                    MethodDeclarationNode method = parseMethodDeclaration(memberName, type, modifiers);
                    for (AnnotationNode annotation : memberAnnotations) {
                        method.addAnnotation(annotation);
                    }
                    interfaceDecl.addMethod(method);
                } else if (isTypeStart()) {
                    ClassReferenceNode type = parseTypeName();
                    String memberName = consume(TokenType.IDENTIFIER, "Expected member name").text();
                    
                    if (check(TokenType.DELIMITER_LEFT_PAREN)) {
                        // 接口方法默认是抽象的
                        if (!modifiers.isAbstract()) {
                            modifiers.setAbstract(true);
                        }
                        MethodDeclarationNode method = parseMethodDeclaration(memberName, type, modifiers);
                        for (AnnotationNode annotation : memberAnnotations) {
                            method.addAnnotation(annotation);
                        }
                        interfaceDecl.addMethod(method);
                    } else {
                        // 接口字段默认是 public static final
                        if (!modifiers.isPublic()) {
                            modifiers.setPublic(true);
                        }
                        if (!modifiers.isStatic()) {
                            modifiers.setStatic(true);
                        }
                        if (!modifiers.isFinal()) {
                            modifiers.setFinal(true);
                        }
                        FieldDeclarationNode field = parseFieldDeclaration(memberName, type, modifiers);
                        for (AnnotationNode annotation : memberAnnotations) {
                            field.addAnnotation(annotation);
                        }
                        interfaceDecl.addField(field);
                        context.addField(memberName);
                    }
                } else {
                    throw error("Unexpected token in interface body");
                }
            }
            
            consume(TokenType.DELIMITER_RIGHT_BRACE, "Expected '}' at end of interface declaration");
            
            return interfaceDecl;
        } finally {
            context.exitClass();
        }
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
     * 解析注解
     * 语法: @AnnotationName 或 @AnnotationName(value) 或 @AnnotationName(key1 = value1, key2 = value2)
     */
    private List<AnnotationNode> parseAnnotations() throws ParseException {
        List<AnnotationNode> annotations = new ArrayList<>();
        
        while (check(TokenType.DELIMITER_AT)) {
            annotations.add(parseAnnotation());
        }
        
        return annotations;
    }
    
    /**
     * 解析单个注解
     */
    private AnnotationNode parseAnnotation() throws ParseException {
        SourceLocation location = createLocation();
        
        consume(TokenType.DELIMITER_AT, "Expected '@'");
        String annotationName = consume(TokenType.IDENTIFIER, "Expected annotation name").text();
        
        if (!check(TokenType.DELIMITER_LEFT_PAREN)) {
            return new AnnotationNode(annotationName, location);
        }
        
        advance();
        
        if (check(TokenType.DELIMITER_RIGHT_PAREN)) {
            advance();
            return new AnnotationNode(annotationName, location);
        }
        
        Map<String, Object> values = new LinkedHashMap<>();
        
        if (check(TokenType.IDENTIFIER) && checkNext(TokenType.OPERATOR_ASSIGN)) {
            do {
                String key = consume(TokenType.IDENTIFIER, "Expected parameter name").text();
                consume(TokenType.OPERATOR_ASSIGN, "Expected '=' in annotation parameter");
                Object value = parseAnnotationValue();
                values.put(key, value);

            } while (match(TokenType.DELIMITER_COMMA));
        } else {
            Object singleValue = parseAnnotationValue();
            consume(TokenType.DELIMITER_RIGHT_PAREN, "Expected ')' after annotation value");
            return new AnnotationNode(annotationName, singleValue, "value", location);
        }
        
        consume(TokenType.DELIMITER_RIGHT_PAREN, "Expected ')' after annotation parameters");
        
        return new AnnotationNode(annotationName, values, location);
    }
    
    /**
     * 解析注解值
     */
    private Object parseAnnotationValue() throws ParseException {
        if (check(TokenType.DELIMITER_LEFT_BRACE)) {
            advance();
            List<Object> values = new ArrayList<>();
            
            if (!check(TokenType.DELIMITER_RIGHT_BRACE)) {
                do {
                    values.add(parseAnnotationValue());
                } while (match(TokenType.DELIMITER_COMMA));
            }
            
            consume(TokenType.DELIMITER_RIGHT_BRACE, "Expected '}' after array values");
            return values.toArray();
        }
        
        if (check(TokenType.LITERAL_STRING)) {
            return advance().value();
        }
        
        if (check(TokenType.LITERAL_INTEGER) || check(TokenType.LITERAL_LONG)) {
            return advance().value();
        }
        
        if (check(TokenType.LITERAL_DECIMAL)) {
            return advance().value();
        }
        
        if (check(TokenType.LITERAL_BOOLEAN)) {
            return advance().value();
        }
        
        if (check(TokenType.LITERAL_CHAR)) {
            return advance().value();
        }
        
        if (check(TokenType.KEYWORD_TRUE)) {
            advance();
            return true;
        }
        
        if (check(TokenType.KEYWORD_FALSE)) {
            advance();
            return false;
        }
        
        if (check(TokenType.KEYWORD_NULL)) {
            advance();
            return null;
        }
        
        if (check(TokenType.IDENTIFIER)) {
            String name = advance().text();
            
            if (check(TokenType.DELIMITER_DOT)) {
                StringBuilder fullName = new StringBuilder(name);
                while (match(TokenType.DELIMITER_DOT)) {
                    fullName.append(".");
                    fullName.append(consume(TokenType.IDENTIFIER, "Expected identifier after '.'").text());
                }
                
                if (check(TokenType.DELIMITER_DOT) || 
                    (peek().text() != null && peek().text().equals("class"))) {
                    if (peek().text() != null && peek().text().equals("class")) {
                        advance();
                        try {
                            Class<?> clazz = context.getClassFinder().findClassWithImports(
                                fullName.toString(), context.getClassLoader(), context.getImports());
                            if (clazz != null) {
                                return clazz;
                            }
                        } catch (Exception ignored) {
                        }
                    }
                }
                
                if (peek().text() != null && peek().text().equals("class")) {
                    advance();
                    try {
                        Class<?> clazz = context.getClassFinder().findClassWithImports(
                            fullName.toString(), context.getClassLoader(), context.getImports());
                        if (clazz != null) {
                            return clazz;
                        }
                    } catch (Exception ignored) {
                    }
                }
                
                return fullName.toString();
            }
            
            if (peek().text() != null && peek().text().equals("class")) {
                advance();
                try {
                    Class<?> clazz = context.getClassFinder().findClassWithImports(
                        name, context.getClassLoader(), context.getImports());
                    if (clazz != null) {
                        return clazz;
                    }
                } catch (Exception ignored) {
                }
            }
            
            return name;
        }
        
        if (check(TokenType.OPERATOR_MINUS)) {
            advance();
            Object value = parseAnnotationValue();
            if (value instanceof Number) {
                if (value instanceof Integer) return -((Integer) value);
                if (value instanceof Long) return -((Long) value);
                if (value instanceof Double) return -((Double) value);
                if (value instanceof Float) return -((Float) value);
            }
            throw error("Invalid negative value in annotation");
        }
        
        throw error("Invalid annotation value");
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
    private ClassReferenceNode parseTypeName() throws ParseException {
        StringBuilder typeNameBuilder = new StringBuilder();
        boolean isPrimitive = false;
        
        if (check(TokenType.IDENTIFIER)) {
            typeNameBuilder.append(advance().text());
            
            while (check(TokenType.OPERATOR_DOT)) {
                advance();
                typeNameBuilder.append(".");
                if (check(TokenType.IDENTIFIER)) {
                    typeNameBuilder.append(advance().text());
                } else {
                    throw error("Expected identifier after '.'");
                }
            }
            
        } else if (check(TokenType.KEYWORD_INT)) {
            advance();
            typeNameBuilder.append("int");
            isPrimitive = true;
        } else if (check(TokenType.KEYWORD_LONG)) {
            advance();
            typeNameBuilder.append("long");
            isPrimitive = true;
        } else if (check(TokenType.KEYWORD_FLOAT)) {
            advance();
            typeNameBuilder.append("float");
            isPrimitive = true;
        } else if (check(TokenType.KEYWORD_DOUBLE)) {
            advance();
            typeNameBuilder.append("double");
            isPrimitive = true;
        } else if (check(TokenType.KEYWORD_BOOLEAN)) {
            advance();
            typeNameBuilder.append("boolean");
            isPrimitive = true;
        } else if (check(TokenType.KEYWORD_CHAR)) {
            advance();
            typeNameBuilder.append("char");
            isPrimitive = true;
        } else if (check(TokenType.KEYWORD_BYTE)) {
            advance();
            typeNameBuilder.append("byte");
            isPrimitive = true;
        } else if (check(TokenType.KEYWORD_SHORT)) {
            advance();
            typeNameBuilder.append("short");
            isPrimitive = true;
        } else if (check(TokenType.KEYWORD_VOID)) {
            advance();
            typeNameBuilder.append("void");
            isPrimitive = true;
        } else {
            throw error("Expected type name");
        }
        
        String typeName = typeNameBuilder.toString();
        List<ClassReferenceNode> typeArguments = null;
        
        if (check(TokenType.OPERATOR_LESS_THAN)) {
            typeArguments = parseGenericTypeArguments();
        }
        
        int arrayDepth = 0;
        while (check(TokenType.DELIMITER_LEFT_BRACKET)) {
            advance();
            consume(TokenType.DELIMITER_RIGHT_BRACKET, "Expected ']' in array type");
            arrayDepth++;
        }
        
        Class<?> resolvedClass;
        if (isPrimitive) {
            // 基本类型使用对应的包装类或原始类型
            resolvedClass = switch (typeName) {
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
        } else {
            resolvedClass = context.resolveClass(typeName);
            if (resolvedClass == null) {
                // 如果找不到类，使用 Object 作为默认类型
                resolvedClass = Object.class;
            }
        }
        
        if (typeArguments != null && !typeArguments.isEmpty()) {
            return ClassReferenceNode.generic(typeName, resolvedClass, isPrimitive, typeArguments, createLocation());
        } else if (arrayDepth > 0) {
            ClassReferenceNode elementType = ClassReferenceNode.of(typeName, resolvedClass, isPrimitive, createLocation());
            return ClassReferenceNode.arrayOf(elementType, arrayDepth);
        } else {
            return ClassReferenceNode.of(typeName, resolvedClass, isPrimitive, createLocation());
        }
    }
    
    /**
     * 解析泛型类型参数
     */
    private List<ClassReferenceNode> parseGenericTypeArguments() throws ParseException {
        List<ClassReferenceNode> typeArguments = new ArrayList<>();
        
        consume(TokenType.OPERATOR_LESS_THAN, "Expected '<'");
        
        int depth = 1;
        while (depth > 0 && !isAtEnd()) {
            if (check(TokenType.OPERATOR_GREATER_THAN)) {
                advance();
                depth--;
            } else if (check(TokenType.OPERATOR_LESS_THAN)) {
                advance();
                depth++;
                typeArguments.add(parseTypeName());
            } else if (check(TokenType.DELIMITER_COMMA)) {
                advance();
            } else if (check(TokenType.OPERATOR_QUESTION)) {
                advance();
                ClassReferenceNode wildcard = ClassReferenceNode.of("?", Object.class, false, createLocation());
                
                if (match(TokenType.KEYWORD_EXTENDS)) {
                    ClassReferenceNode bound = parseTypeName();
                    typeArguments.add(ClassReferenceNode.of("? extends " + bound.getOriginalTypeName(), bound.getResolvedClass(), false, createLocation()));
                } else if (match(TokenType.KEYWORD_SUPER)) {
                    ClassReferenceNode bound = parseTypeName();
                    typeArguments.add(ClassReferenceNode.of("? super " + bound.getOriginalTypeName(), bound.getResolvedClass(), false, createLocation()));
                } else {
                    typeArguments.add(wildcard);
                }
            } else {
                typeArguments.add(parseTypeName());
            }
        }
        
        return typeArguments;
    }
    
    /**
     * 解析构造函数声明
     */
    private ConstructorDeclarationNode parseConstructorDeclaration(String className, ClassModifiers modifiers) throws ParseException {
        SourceLocation location = createLocation();
        
        consume(TokenType.DELIMITER_LEFT_PAREN, "Expected '(' after constructor name");
        List<ParameterNode> parameters = parseParameterList();
        consume(TokenType.DELIMITER_RIGHT_PAREN, "Expected ')' after constructor parameters");
        
        Set<String> paramNames = new HashSet<>();
        for (ParameterNode p : parameters) {
            paramNames.add(p.getParameterName());
        }
        context.enterMethod(paramNames);
        
        try {
            consume(TokenType.DELIMITER_LEFT_BRACE, "Expected '{' before constructor body");
            BlockNode body = parseBlock();
            
            return new ConstructorDeclarationNode(className, parameters, body, modifiers, location);
        } finally {
            context.exitMethod();
        }
    }
    
    /**
     * 解析方法声明
     */
    private MethodDeclarationNode parseMethodDeclaration(String methodName, ClassReferenceNode returnType, ClassModifiers modifiers) throws ParseException {
        SourceLocation location = createLocation();
        
        consume(TokenType.DELIMITER_LEFT_PAREN, "Expected '(' after method name");
        List<ParameterNode> parameters = parseParameterList();
        consume(TokenType.DELIMITER_RIGHT_PAREN, "Expected ')' after method parameters");
        
        Set<String> paramNames = new HashSet<>();
        for (ParameterNode p : parameters) {
            paramNames.add(p.getParameterName());
        }

        // 处理抽象方法
        if (modifiers.isAbstract()) {
            consume(TokenType.DELIMITER_SEMICOLON, "Expected ';' after abstract method declaration");
            return new MethodDeclarationNode(methodName, returnType, parameters, null, modifiers, location);
        }
        
        context.enterMethod(paramNames);
        
        try {
            if (match(TokenType.KEYWORD_THROWS)) {
                do {
                    parseTypeName();
                } while (match(TokenType.DELIMITER_COMMA));
            }
            
            consume(TokenType.DELIMITER_LEFT_BRACE, "Expected '{' before method body");
            BlockNode body = parseBlock();
            
            return new MethodDeclarationNode(methodName, returnType, parameters, body, modifiers, location);
        } finally {
            context.exitMethod();
        }
    }
    
    /**
     * 解析字段声明
     */
    private FieldDeclarationNode parseFieldDeclaration(String fieldName, ClassReferenceNode type, ClassModifiers modifiers) throws ParseException {
        SourceLocation location = createLocation();
        
        ASTNode initialValue = null;
        if (match(TokenType.OPERATOR_ASSIGN)) {
            initialValue = parseExpression();
        }
        
        consume(TokenType.DELIMITER_SEMICOLON, "Expected ';' after field declaration");
        
        return new FieldDeclarationNode(fieldName, type, initialValue, modifiers, location);
    }
    
    /**
     * 解析参数列表
     */
    private List<ParameterNode> parseParameterList() throws ParseException {
        List<ParameterNode> parameters = new ArrayList<>();
        
        if (!check(TokenType.DELIMITER_RIGHT_PAREN)) {
            do {
                ClassReferenceNode type = parseTypeName();
                String paramName = consume(TokenType.IDENTIFIER, "Expected parameter name").text();
                parameters.add(new ParameterNode(paramName, type, createLocation()));
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
        return new BreakNode(location);
    }

    private ASTNode parseContinueStatement() throws ParseException {
        SourceLocation location = createLocation();
        consume(TokenType.DELIMITER_SEMICOLON, "Expected semicolon after continue");
        return new ContinueNode(location);
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
            
            if (opToken.type() == TokenType.OPERATOR_NULL_COALESCING) {
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
            
            BinaryOpNode.Operator operator = opToken.type() == TokenType.OPERATOR_EQUAL ?
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
            
            BinaryOpNode.Operator operator = switch (opToken.type()) {
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
                if (check(TokenType.IDENTIFIER) || isTypeKeyword(peek().type())) {
                    if (typeName.length() > 0 && !typeName.toString().endsWith(".")) {
                        break;
                    }
                    String text = advance().text();
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
            
            BinaryOpNode.Operator operator = switch (opToken.type()) {
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
            
            BinaryOpNode.Operator operator = opToken.type() == TokenType.OPERATOR_PLUS ?
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
            
            BinaryOpNode.Operator operator = switch (opToken.type()) {
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
            
            UnaryOpNode.Operator operator = switch (opToken.type()) {
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
                // 如果已经是方法调用节点，不再解析
                if (!(expr instanceof MethodCallNode)) {
                    expr = parseMethodCall(expr);
                    matched = true;
                }
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
                methodName = advance().text();
            } else if (check(TokenType.KEYWORD_NEW)) {
                methodName = advance().text();
            } else {
                throw error("Expected method name after '::'");
            }
            
            expr = new MethodReferenceNode(expr, methodName, location);
        }
        
        return expr;
    }
    
    private ASTNode parseInterpolatedString(Token token) throws ParseException {
        List<?> parts = (List<?>) token.value();
        InterpolatedStringNode.Builder builder = new InterpolatedStringNode.Builder();
        builder.location(token.location());
        
        for (Object part : parts) {
            if (part instanceof String) {
                builder.addLiteral((String) part);
            } else if (part instanceof Lexer.InterpolationPart) {
                String exprStr = ((Lexer.InterpolationPart) part).getExpression();
                Lexer subLexer = new Lexer(exprStr, sourceFileName);
                List<Token> subTokens = subLexer.tokenize();
                Parser subParser = new Parser(subTokens, context, classLoader, sourceFileName);
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
            return new LiteralNode(token.value(), int.class, token.location());
        }
        
        if (match(TokenType.LITERAL_LONG)) {
            Token token = tokens.get(position - 1);
            return new LiteralNode(token.value(), long.class, token.location());
        }
        
        if (match(TokenType.LITERAL_DECIMAL)) {
            Token token = tokens.get(position - 1);
            return new LiteralNode(token.value(), double.class, token.location());
        }
        
        if (match(TokenType.LITERAL_STRING)) {
            Token token = tokens.get(position - 1);
            return new LiteralNode(token.value(), String.class, token.location());
        }
        
        if (match(TokenType.LITERAL_INTERPOLATED_STRING)) {
            Token token = tokens.get(position - 1);
            return parseInterpolatedString(token);
        }
        
        if (match(TokenType.LITERAL_CHAR)) {
            Token token = tokens.get(position - 1);
            return new LiteralNode(token.value(), char.class, token.location());
        }
        
        if (match(TokenType.LITERAL_BOOLEAN)) {
            Token token = tokens.get(position - 1);
            return new LiteralNode(token.value(), boolean.class, token.location());
        }
        
        if (match(TokenType.LITERAL_NULL)) {
            Token token = tokens.get(position - 1);
            return new LiteralNode(null, null, token.location());
        }
        
        if (match(TokenType.KEYWORD_THIS)) {
            return createVariableNode("this", createLocation());
        }
        
        if (match(TokenType.KEYWORD_SUPER)) {
            return createVariableNode("super", createLocation());
        }
        
        if (check(TokenType.KEYWORD_INT) || check(TokenType.KEYWORD_LONG) ||
            check(TokenType.KEYWORD_FLOAT) || check(TokenType.KEYWORD_DOUBLE) ||
            check(TokenType.KEYWORD_BOOLEAN) || check(TokenType.KEYWORD_CHAR) ||
            check(TokenType.KEYWORD_BYTE) || check(TokenType.KEYWORD_SHORT) ||
            check(TokenType.KEYWORD_VOID)) {
            Token token = advance();
            String typeName = token.text();
            Class<?> primitiveClass = TypeUtils.getPrimitiveType(typeName);
            return ClassReferenceNode.of(typeName, primitiveClass, true, token.location());
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
                ASTNode lambda = parseLambda();
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
                StringBuilder className = new StringBuilder(token.text());
                String lastValidClassName = null;
                int lastValidPosition = position;
                
                String simpleResolved = resolveClassName(token.text());
                if (simpleResolved != null && !context.isClassDeclared(token.text())) {
                    lastValidClassName = token.text();
                    lastValidPosition = position;
                }
                
                while (check(TokenType.OPERATOR_DOT) && !isAtEnd()) {
                    advance();
                    if (check(TokenType.IDENTIFIER)) {
                        className.append(".").append(advance().text());
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
                
                if (lastValidClassName != null && !context.isClassDeclared(token.text())) {
                    position = lastValidPosition;
                    
                    if (check(TokenType.OPERATOR_LESS_THAN)) {
                        advance();
                        StringBuilder genericType = new StringBuilder(lastValidClassName).append("<");
                        parseTypeArguments(genericType);
                        releasePosition();
                        Class<?> resolvedClass = context.resolveClass(genericType.toString());
                        if (resolvedClass == null) {
                            resolvedClass = Object.class;
                        }
                        return ClassReferenceNode.of(genericType.toString(), resolvedClass, false, token.location());
                    }
                    
                    releasePosition();
                    Class<?> resolvedClass = context.resolveClass(lastValidClassName);
                    if (resolvedClass == null) {
                        resolvedClass = Object.class;
                    }
                    return ClassReferenceNode.of(lastValidClassName, resolvedClass, false, token.location());
                }
                
                // 检查是否是已声明的类
                if (context.isClassDeclared(token.text())) {
                    // 不直接返回，继续解析后面的成员访问
                    restorePosition();
                    return new VariableNode(token.text(), token.location());
                }
                
                restorePosition();
                return new VariableNode(token.text(), token.location());
            } catch (ParseException e) {
                restorePosition();
                return new VariableNode(token.text(), token.location());
            }
        }
        
        if (check(TokenType.DELIMITER_LEFT_PAREN)) {
            savePosition();
            try {
                ASTNode lambda = parseLambda();
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
     * 收集逗号分隔的元素列表
     * 不会消耗掉开始标记（如 {, [）
     *
     * @param endToken      结束标记 (如 }, ])
     * @param elementParser 元素解析器
     */
    private List<ASTNode> collectCommaSeparatedElements(
            TokenType endToken, 
            Supplier<ASTNode> elementParser) throws ParseException {
        List<ASTNode> elements = new ArrayList<>();
        
        if (check(endToken)) {
            return elements;
        }

        if (check(TokenType.DELIMITER_COMMA)) {
            advance();  // 吃掉调用者可能遗漏的逗号
        }

        do {
            elements.add(elementParser.get());
        } while (match(TokenType.DELIMITER_COMMA) && !check(endToken));
        
        return elements;
    }
    
    /**
     * 解析数组字面量
     * 语法: [element1, element2, ...]
     */
    private ArrayLiteralNode parseArrayLiteral() throws ParseException {
        SourceLocation location = createLocation();

        consume(TokenType.DELIMITER_LEFT_BRACKET, "Expected '['");

        List<ASTNode> elements = collectCommaSeparatedElements(
            TokenType.DELIMITER_RIGHT_BRACKET,
            this::parseExpression
        );

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
        elements.addAll(collectCommaSeparatedElements(
            TokenType.DELIMITER_RIGHT_BRACE,
            this::parseExpression
        ));
        
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
        
        if (check(TokenType.DELIMITER_LEFT_BRACE)) {
            return parseAnonymousClass(genericType, arguments, location);
        }
        
        return new ConstructorCallNode(genericType, arguments, null, location);
    }
    
    /**
     * 解析匿名内部类
     * 语法: new ClassName(args) { field/method declarations }
     */
    private ConstructorCallNode parseAnonymousClass(GenericType superType, List<ASTNode> arguments, 
                                                      SourceLocation location) throws ParseException {
        String superClassName = superType.getTypeName();
        Class<?> resolvedSuperClass = superType.getRawType() != null ? 
            superType.getRawType() : null;
        String anonymousClassName = superClassName.replaceAll("[^a-zA-Z0-9_$]", "_") + "__Anonymous" + System.nanoTime();
        
        consume(TokenType.DELIMITER_LEFT_BRACE, "Expected '{' for anonymous class body");
        
        context.enterClass(anonymousClassName);
        context.addField("this$0");
        
        try {
            if (resolvedSuperClass == null) {
                // 如果找不到父类，使用 Object 作为默认父类
                resolvedSuperClass = Object.class;
            }
            ClassReferenceNode superClassRef = ClassReferenceNode.of(superClassName, resolvedSuperClass, false, location);
            ClassDeclarationNode classDecl = new ClassDeclarationNode(
                anonymousClassName, superClassRef, new ArrayList<>(), location);
            
            while (!check(TokenType.DELIMITER_RIGHT_BRACE) && !isAtEnd()) {
                List<AnnotationNode> memberAnnotations = parseAnnotations();
                ClassModifiers modifiers = parseModifiers();
                
                if (check(TokenType.IDENTIFIER) && checkNext(TokenType.DELIMITER_LEFT_PAREN)) {
                    String constructorName = consume(TokenType.IDENTIFIER, "Expected constructor name").text();
                    if (!constructorName.equals(anonymousClassName)) {
                        throw error("Constructor name must match class name in anonymous class");
                    }
                    ConstructorDeclarationNode constructor = parseConstructorDeclaration(anonymousClassName, modifiers);
                    for (AnnotationNode annotation : memberAnnotations) {
                        constructor.addAnnotation(annotation);
                    }
                    classDecl.addConstructor(constructor);
                } else if (isTypeStart()) {
                    ClassReferenceNode type = parseTypeName();
                    String memberName = consume(TokenType.IDENTIFIER, "Expected member name").text();
                    
                    if (check(TokenType.DELIMITER_LEFT_PAREN)) {
                        MethodDeclarationNode method = parseMethodDeclaration(memberName, type, modifiers);
                        for (AnnotationNode annotation : memberAnnotations) {
                            method.addAnnotation(annotation);
                        }
                        classDecl.addMethod(method);
                    } else {
                        FieldDeclarationNode field = parseFieldDeclaration(memberName, type, modifiers);
                        for (AnnotationNode annotation : memberAnnotations) {
                            field.addAnnotation(annotation);
                        }
                        classDecl.addField(field);
                        context.addField(memberName);
                    }
                } else {
                    throw error("Unexpected token in anonymous class body");
                }
            }
            
            consume(TokenType.DELIMITER_RIGHT_BRACE, "Expected '}' at end of anonymous class");
            
            return new ConstructorCallNode(superType, arguments, classDecl, location);
        } finally {
            context.exitClass();
        }
    }
    
    /**
     * 解析带大小的数组初始化器
     * 语法: new int[3] {1, 2, 3}
     */
    private ASTNode parseArrayInitializerWithSize(Class<?> type, ASTNode size, SourceLocation location) throws ParseException {
        consume(TokenType.DELIMITER_LEFT_BRACE, "Expected '{'");
        
        Class<?> componentType = type.isArray() ? type.getComponentType() : type;
        
        List<ASTNode> elements = collectCommaSeparatedElements(
            TokenType.DELIMITER_RIGHT_BRACE,
            () -> {
                if (check(TokenType.DELIMITER_LEFT_BRACE) && componentType.isArray()) {
                    return parseArrayInitializer(componentType, createLocation());
                }
                return parseExpression();
            }
        );
        
        consume(TokenType.DELIMITER_RIGHT_BRACE, "Expected '}' after array initializer");
        
        return new ArrayLiteralNode(elements, componentType, size, location);
    }
    
    /**
     * 解析数组初始化器
     * 语法: {element1, element2, ...}
     */
    private ArrayLiteralNode parseArrayInitializer(Class<?> type, SourceLocation location) throws ParseException {
        consume(TokenType.DELIMITER_LEFT_BRACE, "Expected '{'");
        
        Class<?> componentType = type.isArray() ? type.getComponentType() : type;
        
        List<ASTNode> elements = collectCommaSeparatedElements(
            TokenType.DELIMITER_RIGHT_BRACE,
            () -> {
                if (check(TokenType.DELIMITER_LEFT_BRACE) && componentType.isArray()) {
                    return parseArrayInitializer(componentType, createLocation());
                }
                return parseExpression();
            }
        );
        
        consume(TokenType.DELIMITER_RIGHT_BRACE, "Expected '}' after array initializer");
        
        return new ArrayLiteralNode(elements, componentType, location);
    }
    
    /**
     * 解析成员访问
     */
    @SuppressWarnings("CheckResult")
    private ASTNode parseMemberAccess(ASTNode target) throws ParseException {
        SourceLocation location = createLocation();
        
        if (check(TokenType.KEYWORD_CLASS)) {
            advance();
            if (target instanceof ClassReferenceNode classRef) {
                Class<?> clazz = classRef.getResolvedClass();
                if (clazz != null) {
                    return new LiteralNode(clazz, Class.class, location);
                }
                throw new ParseException(
                    "Cannot resolve class: " + classRef.getTypeName(),
                    location,
                    ErrorCode.PARSE_INVALID_SYNTAX
                );
            } else if (target instanceof VariableNode) {
                String varName = ((VariableNode) target).getName();
                Class<?> clazz = context.getClassFinder().findClassWithImports(varName, context.getClassLoader(), context.getImports());
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
                // 尝试解析类引用为实际的 Class 对象
                try {
                    Class<?> clazz = context.resolveClass(classRef.getTypeName());
                    if (clazz != null) {
                        return new ConstructorCallNode(new GenericType(clazz), arguments, null, location);
                    }
                } catch (Exception e) {
                    // 解析失败，抛出异常
                    throw new ParseException(
                        "Cannot resolve class: " + classRef.getTypeName(),
                        location,
                        ErrorCode.PARSE_INVALID_SYNTAX
                    );
                }
                
                // 如果无法解析类，抛出异常
                throw new ParseException(
                    "Cannot resolve class: " + classRef.getTypeName(),
                    location,
                    ErrorCode.PARSE_INVALID_SYNTAX
                );
            } else if (target instanceof VariableNode) {
                String varName = ((VariableNode) target).getName();
                Class<?> clazz = context.getClassFinder().findClassWithImports(varName, context.getClassLoader(), context.getImports());
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
        
        String memberName = consume(TokenType.IDENTIFIER, "Expected member name").text();
        
        if (target instanceof ClassReferenceNode classRef) {
            // 首先检查是否是方法调用（带括号）
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
            }
            
            String nestedClassName = classRef.getTypeName() + "." + memberName;
            
            String resolved = resolveClassName(nestedClassName);
            if (resolved != null) {
                Class<?> resolvedClass = context.resolveClass(resolved);
                if (resolvedClass == null) {
                    resolvedClass = Object.class;
                }
                return ClassReferenceNode.of(nestedClassName, resolvedClass, false, location);
            }
            
            String innerClassName = classRef.getTypeName() + "$" + memberName;
            String resolvedInner = resolveClassName(innerClassName);
            if (resolvedInner != null) {
                Class<?> resolvedClass = context.resolveClass(resolvedInner);
                if (resolvedClass == null) {
                    resolvedClass = Object.class;
                }
                return ClassReferenceNode.of(nestedClassName, resolvedClass, false, location);
            }
            
            try {
                String className = classRef.getTypeName();
                Class<?> clazz = ClassResolver.findClassWithImports(className, classLoader, context.getImports());
                
                if (clazz != null) {
                    try {
                        clazz.getDeclaredField(memberName);
                        return new FieldAccessNode(target, memberName, location);
                    } catch (NoSuchFieldException e) {
                        // TODO: 记录一下警告
                        return new FieldAccessNode(target, memberName, location);
                    }
                }
            } catch (Exception ignored) {
            }
            return new FieldAccessNode(target, memberName, location);
        }
        
        // 检查是否是方法调用（带括号）
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
            // 字段访问
            return new FieldAccessNode(target, memberName, location);
        }
    }
    
    private ASTNode parseSafeMemberAccess(ASTNode target) throws ParseException {
        SourceLocation location = createLocation();
        String memberName = consume(TokenType.IDENTIFIER, "Expected member name").text();
        
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
            String variableName = ((VariableNode) target).getName();
            if ("super".equals(variableName)) {
                return new SuperMethodCallNode("<init>", arguments, location);
            } else {
                return new FunctionCallNode(variableName, arguments, location);
            }
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
    private ASTNode parseLambda() throws ParseException {
        SourceLocation location = createLocation();
        List<LambdaNode.Parameter> parameters = new ArrayList<>();
        
        if (check(TokenType.IDENTIFIER) && checkNext(TokenType.DELIMITER_ARROW)) {
            Token paramToken = peek();
            String paramName = paramToken.text();
            
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
                    parameters.add(new LambdaNode.Parameter(paramToken.text(), Object.class));
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
            statements.add(parseStatement());
        }
        
        consume(TokenType.DELIMITER_RIGHT_BRACE, "Expected '}' at end of lambda body");
        
        return new BlockNode(statements, location);
    }
    
    private boolean checkNext(TokenType type) {
        if (position + 1 >= tokens.size()) {
            return false;
        }
        return tokens.get(position + 1).type() == type;
    }

    private boolean isGenericVariableDeclaration() {
        if (!check(TokenType.IDENTIFIER)) {
            return false;
        }
        if (position + 1 >= tokens.size() || tokens.get(position + 1).type() != TokenType.OPERATOR_LESS_THAN) {
            return false;
        }
        int depth = 0;
        int pos = position + 1;
        while (pos < tokens.size()) {
            TokenType type = tokens.get(pos).type();
            if (type == TokenType.OPERATOR_LESS_THAN) {
                depth++;
            } else if (type == TokenType.OPERATOR_GREATER_THAN) {
                depth--;
                if (depth == 0) {
                    int afterGeneric = pos + 1;
                    if (afterGeneric < tokens.size()) {
                        TokenType nextType = tokens.get(afterGeneric).type();
                        return nextType == TokenType.IDENTIFIER ||
                               nextType == TokenType.KEYWORD_INT || nextType == TokenType.KEYWORD_LONG ||
                               nextType == TokenType.KEYWORD_FLOAT || nextType == TokenType.KEYWORD_DOUBLE ||
                               nextType == TokenType.KEYWORD_BOOLEAN || nextType == TokenType.KEYWORD_CHAR ||
                               nextType == TokenType.KEYWORD_BYTE || nextType == TokenType.KEYWORD_SHORT;
                    }
                    return false;
                }
            } else if (type == TokenType.OPERATOR_RIGHT_SHIFT) {
                depth -= 2;
                if (depth == 0) {
                    int afterGeneric = pos + 1;
                    if (afterGeneric < tokens.size()) {
                        TokenType nextType = tokens.get(afterGeneric).type();
                        return nextType == TokenType.IDENTIFIER ||
                               nextType == TokenType.KEYWORD_INT || nextType == TokenType.KEYWORD_LONG ||
                               nextType == TokenType.KEYWORD_FLOAT || nextType == TokenType.KEYWORD_DOUBLE ||
                               nextType == TokenType.KEYWORD_BOOLEAN || nextType == TokenType.KEYWORD_CHAR ||
                               nextType == TokenType.KEYWORD_BYTE || nextType == TokenType.KEYWORD_SHORT;
                    }
                    return false;
                }
            } else if (type == TokenType.OPERATOR_UNSIGNED_RIGHT_SHIFT) {
                depth -= 3;
                if (depth == 0) {
                    int afterGeneric = pos + 1;
                    if (afterGeneric < tokens.size()) {
                        TokenType nextType = tokens.get(afterGeneric).type();
                        return nextType == TokenType.IDENTIFIER ||
                               nextType == TokenType.KEYWORD_INT || nextType == TokenType.KEYWORD_LONG ||
                               nextType == TokenType.KEYWORD_FLOAT || nextType == TokenType.KEYWORD_DOUBLE ||
                               nextType == TokenType.KEYWORD_BOOLEAN || nextType == TokenType.KEYWORD_CHAR ||
                               nextType == TokenType.KEYWORD_BYTE || nextType == TokenType.KEYWORD_SHORT;
                    }
                    return false;
                }
            } else if (type == TokenType.DELIMITER_SEMICOLON || type == TokenType.DELIMITER_LEFT_PAREN ||
                       type == TokenType.OPERATOR_ASSIGN || type == TokenType.DELIMITER_RIGHT_BRACE) {
                return false;
            }
            pos++;
        }
        return false;
    }

    private boolean isQualifiedTypeVariableDeclaration() {
        if (!check(TokenType.IDENTIFIER)) {
            return false;
        }
        
        int pos = position;
        int genericDepth;
        boolean hadTypePart = false;
        
        while (pos < tokens.size()) {
            TokenType type = tokens.get(pos).type();
            
            if (type == TokenType.IDENTIFIER) {
                pos++;
                if (pos < tokens.size()) {
                    TokenType nextType = tokens.get(pos).type();
                    if (nextType == TokenType.OPERATOR_DOT) {
                        hadTypePart = true;
                        pos++;
                        continue;
                    } else if (nextType == TokenType.OPERATOR_LESS_THAN) {
                        hadTypePart = true;
                        genericDepth = 1;
                        pos++;
                        while (pos < tokens.size() && genericDepth > 0) {
                            TokenType gt = tokens.get(pos).type();
                            if (gt == TokenType.OPERATOR_LESS_THAN) {
                                genericDepth++;
                            } else if (gt == TokenType.OPERATOR_GREATER_THAN) {
                                genericDepth--;
                            } else if (gt == TokenType.OPERATOR_RIGHT_SHIFT) {
                                genericDepth -= 2;
                            } else if (gt == TokenType.OPERATOR_UNSIGNED_RIGHT_SHIFT) {
                                genericDepth -= 3;
                            }
                            pos++;
                        }
                        continue;
                    } else if (nextType == TokenType.DELIMITER_LEFT_BRACKET) {
                        hadTypePart = true;
                        pos++;
                        if (pos < tokens.size() && tokens.get(pos).type() == TokenType.DELIMITER_RIGHT_BRACKET) {
                            pos++;
                            continue;
                        }
                        return false;
                    } else if (nextType == TokenType.IDENTIFIER) {
                        return hadTypePart;
                    } else if (isTypeKeyword(nextType)) {
                        return hadTypePart;
                    } else return (nextType == TokenType.OPERATOR_ASSIGN || nextType == TokenType.DELIMITER_SEMICOLON) && hadTypePart;
                }
                return false;
            } else if (type == TokenType.DELIMITER_LEFT_BRACKET) {
                pos++;
                if (pos < tokens.size() && tokens.get(pos).type() == TokenType.DELIMITER_RIGHT_BRACKET) {
                    pos++;
                    if (pos < tokens.size()) {
                        TokenType nextType = tokens.get(pos).type();
                        return nextType == TokenType.IDENTIFIER || isTypeKeyword(nextType);
                    }
                }
                return false;
            } else {
                break;
            }
        }
        
        return false;
    }
    
    private boolean isVariableDeclarationStart() {
        if (check(TokenType.KEYWORD_INT) || check(TokenType.KEYWORD_LONG) ||
            check(TokenType.KEYWORD_FLOAT) || check(TokenType.KEYWORD_DOUBLE) ||
            check(TokenType.KEYWORD_BOOLEAN) || check(TokenType.KEYWORD_CHAR) ||
            check(TokenType.KEYWORD_BYTE) || check(TokenType.KEYWORD_SHORT) ||
            check(TokenType.KEYWORD_AUTO) || check(TokenType.KEYWORD_FINAL)) {
            return true;
        }
        
        if (check(TokenType.IDENTIFIER) && checkNext(TokenType.OPERATOR_DECLARE_ASSIGN)) {
            return true;
        }
        
        if (check(TokenType.IDENTIFIER)) {
            if (checkNext(TokenType.IDENTIFIER) || checkNext(TokenType.KEYWORD_AUTO) ||
                isGenericVariableDeclaration() || isQualifiedTypeVariableDeclaration()) {
                return true;
            }

            return isTypeKeyword(peekNext().type());
        }
        
        return false;
    }

}
