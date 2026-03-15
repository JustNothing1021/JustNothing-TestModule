package com.justnothing.testmodule.command.functions.script_new.parser;

import com.justnothing.testmodule.command.functions.script_new.ast.SourceLocation;
import com.justnothing.testmodule.command.functions.script_new.ast.ASTNode;
import com.justnothing.testmodule.command.functions.script_new.ast.nodes.*;
import com.justnothing.testmodule.command.functions.script_new.exception.ParseException;
import com.justnothing.testmodule.command.functions.script_new.lexer.Token;
import com.justnothing.testmodule.command.functions.script_new.lexer.TokenType;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

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
    
    private final List<Token> tokens;
    private int position;
    private final ParseContext context;
    private final Deque<Integer> savedPositions;
    
    public Parser(List<Token> tokens) {
        this.tokens = tokens;
        this.position = 0;
        this.context = new ParseContext();
        this.savedPositions = new ArrayDeque<>();
    }
    
    public Parser(List<Token> tokens, ParseContext context) {
        this.tokens = tokens;
        this.position = 0;
        this.context = context;
        this.savedPositions = new ArrayDeque<>();
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
                com.justnothing.testmodule.command.functions.script_new.exception.ErrorCode.PARSE_INVALID_SYNTAX
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
        if (check(TokenType.KEYWORD_INT) || check(TokenType.KEYWORD_LONG) ||
            check(TokenType.KEYWORD_FLOAT) || check(TokenType.KEYWORD_DOUBLE) ||
            check(TokenType.KEYWORD_BOOLEAN) || check(TokenType.KEYWORD_CHAR) ||
            check(TokenType.KEYWORD_BYTE) || check(TokenType.KEYWORD_SHORT) ||
            check(TokenType.KEYWORD_AUTO) || check(TokenType.KEYWORD_FINAL)) {
            return parseVariableDeclaration();
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
            return parseBlock();
        }
        
        ASTNode expression = parseExpression();
        consume(TokenType.DELIMITER_SEMICOLON, "Expected semicolon after statement");
        return expression;
    }
    
    /**
     * 解析变量声明
     */
    private AssignmentNode parseVariableDeclaration() throws ParseException {
        SourceLocation location = createLocation();
        boolean isFinal = false;
        
        if (peek().getType() == TokenType.KEYWORD_FINAL) {
            isFinal = true;
            advance();
        }
        
        Token typeToken = advance();
        Class<?> type = parseTypeFromToken(typeToken);
        
        String varName = consume(TokenType.IDENTIFIER, "Expected variable name").getText();
        
        ASTNode value = null;
        if (match(TokenType.OPERATOR_ASSIGN)) {
            value = parseExpression();
        } else if (type == Object.class && typeToken.getType() == TokenType.KEYWORD_AUTO) {
            throw error("Auto variable must have initial value");
        }
        
        consume(TokenType.DELIMITER_SEMICOLON, "Expected semicolon after variable declaration");
        
        return new AssignmentNode(varName, value, true, type, location);
    }
    
    /**
     * 从token解析类型
     * 支持基本类型、类名、数组类型、泛型
     */
    private Class<?> parseTypeFromToken(Token token) throws ParseException {
        TokenType type = token.getType();
        
        if (type == TokenType.KEYWORD_INT) return int.class;
        if (type == TokenType.KEYWORD_LONG) return long.class;
        if (type == TokenType.KEYWORD_FLOAT) return float.class;
        if (type == TokenType.KEYWORD_DOUBLE) return double.class;
        if (type == TokenType.KEYWORD_BOOLEAN) return boolean.class;
        if (type == TokenType.KEYWORD_CHAR) return char.class;
        if (type == TokenType.KEYWORD_BYTE) return byte.class;
        if (type == TokenType.KEYWORD_SHORT) return short.class;
        if (type == TokenType.KEYWORD_VOID) return void.class;
        if (type == TokenType.KEYWORD_AUTO) return Object.class;
        
        if (type == TokenType.IDENTIFIER) {
            return resolveType(token.getText());
        }
        
        throw error("Expected type");
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
                if (check(TokenType.IDENTIFIER) || check(TokenType.OPERATOR_DOT)) {
                    typeName.append(advance().getText());
                } else if (check(TokenType.DELIMITER_LEFT_BRACKET)) {
                    advance();
                    if (check(TokenType.DELIMITER_RIGHT_BRACKET)) {
                        advance();
                        typeName.append("[]");
                    } else {
                        restorePosition();
                        break;
                    }
                } else if (check(TokenType.OPERATOR_LESS_THAN)) {
                    advance();
                    typeName.append("<");
                    parseTypeArguments(typeName);
                    if (check(TokenType.OPERATOR_GREATER_THAN)) {
                        advance();
                        typeName.append(">");
                    }
                } else {
                    break;
                }
            }
            
            String typeStr = typeName.toString();
            if (typeStr.isEmpty()) {
                restorePosition();
                throw error("Expected type");
            }
            
            releasePosition();
            return resolveType(typeStr);
            
        } catch (ParseException e) {
            restorePosition();
            throw e;
        }
    }
    
    /**
     * 解析泛型参数
     */
    private void parseTypeArguments(StringBuilder typeName) throws ParseException {
        while (!check(TokenType.OPERATOR_GREATER_THAN) && !isAtEnd()) {
            parseType();
            
            if (check(TokenType.DELIMITER_COMMA)) {
                advance();
                typeName.append(", ");
            }
        }
    }
    
    /**
     * 判断字符串是否是类引用，如果是简单类名则尝试添加java.lang前缀
     */
    private String resolveClassName(String className) {
        try {
            Class<?> clazz = Class.forName(className);
            return className;
        } catch (ClassNotFoundException e) {
            try {
                Class<?> clazz = Class.forName("java.lang." + className);
                return "java.lang." + className;
            } catch (ClassNotFoundException e2) {
                return null;
            }
        }
    }
    
    /**
     * 解析类型字符串为Class对象
     */
    private Class<?> resolveType(String typeName) throws ParseException {
        switch (typeName) {
            case "int":
                return int.class;
            case "long":
                return long.class;
            case "float":
                return float.class;
            case "double":
                return double.class;
            case "boolean":
                return boolean.class;
            case "char":
                return char.class;
            case "byte":
                return byte.class;
            case "short":
                return short.class;
            case "void":
                return void.class;
            case "auto":
                return Object.class;
            case "Integer":
                return Integer.class;
            case "Long":
                return Long.class;
            case "Float":
                return Float.class;
            case "Double":
                return Double.class;
            case "Boolean":
                return Boolean.class;
            case "Character":
                return Character.class;
            case "Byte":
                return Byte.class;
            case "Short":
                return Short.class;
            case "String":
                return String.class;
            case "Object":
                return Object.class;
            default:
                throw error("Unsupported type: " + typeName);
        }
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
     */
    private ForNode parseForStatement() throws ParseException {
        SourceLocation location = createLocation();
        
        consume(TokenType.DELIMITER_LEFT_PAREN, "Expected '(' after for");
        
        ASTNode initialization = null;
        if (!check(TokenType.DELIMITER_SEMICOLON)) {
            initialization = parseExpression();
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
    private ASTNode parseWhileStatement() throws ParseException {
        SourceLocation location = createLocation();
        
        consume(TokenType.DELIMITER_LEFT_PAREN, "Expected '(' after while");
        ASTNode condition = parseExpression();
        consume(TokenType.DELIMITER_RIGHT_PAREN, "Expected ')' after condition");
        
        ASTNode body = parseStatement();
        
        return body;
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
        return value;
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
        return parseAssignment();
    }
    
    /**
     * 解析赋值表达式
     */
    private ASTNode parseAssignment() throws ParseException {
        int savedPos = savePosition();
        
        try {
            ASTNode left = parseLogicalOr();
            
            if (match(TokenType.OPERATOR_ASSIGN) || 
                match(TokenType.OPERATOR_PLUS_ASSIGN) ||
                match(TokenType.OPERATOR_MINUS_ASSIGN) ||
                match(TokenType.OPERATOR_MULTIPLY_ASSIGN) ||
                match(TokenType.OPERATOR_DIVIDE_ASSIGN) ||
                match(TokenType.OPERATOR_MODULO_ASSIGN)) {
                
                Token opToken = tokens.get(position - 1);
                ASTNode right = parseLogicalOr();
                
                if (left instanceof VariableNode) {
                    String varName = ((VariableNode) left).getName();
                    return new AssignmentNode(varName, right, false, null, left.getLocation());
                }
                
                restorePosition();
            }
            
            return left;
        } finally {
            releasePosition();
        }
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
        ASTNode left = parseEquality();
        
        while (match(TokenType.OPERATOR_LOGICAL_AND)) {
            SourceLocation location = createLocation();
            ASTNode right = parseEquality();
            left = new BinaryOpNode(BinaryOpNode.Operator.LOGICAL_AND, left, right, location);
        }
        
        return left;
    }
    
    /**
     * 解析相等性表达式
     */
    private ASTNode parseEquality() throws ParseException {
        ASTNode left = parseComparison();
        
        while (match(TokenType.OPERATOR_EQUAL) || match(TokenType.OPERATOR_NOT_EQUAL)) {
            Token opToken = tokens.get(position - 1);
            SourceLocation location = createLocation();
            ASTNode right = parseComparison();
            
            BinaryOpNode.Operator operator = opToken.getType() == TokenType.OPERATOR_EQUAL ? 
                    BinaryOpNode.Operator.EQUAL : BinaryOpNode.Operator.NOT_EQUAL;
            left = new BinaryOpNode(operator, left, right, location);
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
               match(TokenType.OPERATOR_GREATER_THAN_OR_EQUAL)) {
            
            Token opToken = tokens.get(position - 1);
            SourceLocation location = createLocation();
            ASTNode right = parseShift();
            
            BinaryOpNode.Operator operator;
            switch (opToken.getType()) {
                case OPERATOR_LESS_THAN:
                    operator = BinaryOpNode.Operator.LESS_THAN;
                    break;
                case OPERATOR_LESS_THAN_OR_EQUAL:
                    operator = BinaryOpNode.Operator.LESS_THAN_OR_EQUAL;
                    break;
                case OPERATOR_GREATER_THAN:
                    operator = BinaryOpNode.Operator.GREATER_THAN;
                    break;
                case OPERATOR_GREATER_THAN_OR_EQUAL:
                    operator = BinaryOpNode.Operator.GREATER_THAN_OR_EQUAL;
                    break;
                default:
                    throw error("Invalid comparison operator");
            }
            
            left = new BinaryOpNode(operator, left, right, location);
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
            
            BinaryOpNode.Operator operator;
            switch (opToken.getType()) {
                case OPERATOR_LEFT_SHIFT:
                    operator = BinaryOpNode.Operator.LEFT_SHIFT;
                    break;
                case OPERATOR_RIGHT_SHIFT:
                    operator = BinaryOpNode.Operator.RIGHT_SHIFT;
                    break;
                case OPERATOR_UNSIGNED_RIGHT_SHIFT:
                    operator = BinaryOpNode.Operator.UNSIGNED_RIGHT_SHIFT;
                    break;
                default:
                    throw error("Invalid shift operator");
            }
            
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
        ASTNode left = parseUnary();
        
        while (match(TokenType.OPERATOR_MULTIPLY) || 
               match(TokenType.OPERATOR_DIVIDE) || 
               match(TokenType.OPERATOR_MODULO)) {
            
            Token opToken = tokens.get(position - 1);
            SourceLocation location = createLocation();
            ASTNode right = parseUnary();
            
            BinaryOpNode.Operator operator;
            switch (opToken.getType()) {
                case OPERATOR_MULTIPLY:
                    operator = BinaryOpNode.Operator.MULTIPLY;
                    break;
                case OPERATOR_DIVIDE:
                    operator = BinaryOpNode.Operator.DIVIDE;
                    break;
                case OPERATOR_MODULO:
                    operator = BinaryOpNode.Operator.MODULO;
                    break;
                default:
                    throw error("Invalid multiplicative operator");
            }
            
            left = new BinaryOpNode(operator, left, right, location);
        }
        
        return left;
    }
    
    /**
     * 解析一元表达式
     */
    private ASTNode parseUnary() throws ParseException {
        if (match(TokenType.OPERATOR_PLUS) || match(TokenType.OPERATOR_MINUS) ||
            match(TokenType.OPERATOR_LOGICAL_NOT) || match(TokenType.OPERATOR_BITWISE_NOT)) {
            
            Token opToken = tokens.get(position - 1);
            SourceLocation location = createLocation();
            ASTNode operand = parseUnary();
            
            UnaryOpNode.Operator operator;
            switch (opToken.getType()) {
                case OPERATOR_PLUS:
                    operator = UnaryOpNode.Operator.POSITIVE;
                    break;
                case OPERATOR_MINUS:
                    operator = UnaryOpNode.Operator.NEGATIVE;
                    break;
                case OPERATOR_LOGICAL_NOT:
                    operator = UnaryOpNode.Operator.LOGICAL_NOT;
                    break;
                case OPERATOR_BITWISE_NOT:
                    operator = UnaryOpNode.Operator.BITWISE_NOT;
                    break;
                default:
                    throw error("Invalid unary operator");
            }
            
            return new UnaryOpNode(operator, operand, location);
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
        
        return expr;
    }
    
    /**
     * 解析主表达式
     */
    private ASTNode parsePrimary() throws ParseException {
        if (match(TokenType.LITERAL_INTEGER)) {
            Token token = tokens.get(position - 1);
            return new LiteralNode(token.getValue(), int.class, token.getLocation());
        }
        
        if (match(TokenType.LITERAL_DECIMAL)) {
            Token token = tokens.get(position - 1);
            return new LiteralNode(token.getValue(), double.class, token.getLocation());
        }
        
        if (match(TokenType.LITERAL_STRING)) {
            Token token = tokens.get(position - 1);
            return new LiteralNode(token.getValue(), String.class, token.getLocation());
        }
        
        if (match(TokenType.LITERAL_BOOLEAN)) {
            Token token = tokens.get(position - 1);
            return new LiteralNode(token.getValue(), boolean.class, token.getLocation());
        }
        
        if (match(TokenType.LITERAL_NULL)) {
            Token token = tokens.get(position - 1);
            return new LiteralNode(null, null, token.getLocation());
        }
        
        if (match(TokenType.IDENTIFIER)) {
            Token token = tokens.get(position - 1);
            
            savePosition();
            try {
                StringBuilder className = new StringBuilder(token.getText());
                
                while (check(TokenType.OPERATOR_DOT) && !isAtEnd()) {
                    advance();
                    if (check(TokenType.IDENTIFIER)) {
                        className.append(".").append(advance().getText());
                    } else {
                        break;
                    }
                }
                
                String fullClassName = className.toString();
                String resolvedClassName = resolveClassName(fullClassName);
                
                if (resolvedClassName != null) {
                    releasePosition();
                    return new ClassReferenceNode(resolvedClassName, token.getLocation());
                } else {
                    restorePosition();
                    return new VariableNode(token.getText(), token.getLocation());
                }
            } catch (ParseException e) {
                restorePosition();
                return new VariableNode(token.getText(), token.getLocation());
            }
        }
        
        if (match(TokenType.DELIMITER_LEFT_PAREN)) {
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
        SourceLocation location = createLocation();
        ASTNode expression = parseExpression();
        consume(TokenType.DELIMITER_RIGHT_PAREN, "Expected ')' after expression");
        return expression;
    }
    
    /**
     * 解析构造函数调用
     */
    private ASTNode parseConstructorCall() throws ParseException {
        SourceLocation location = createLocation();
        
        Class<?> type = parseType();
        consume(TokenType.DELIMITER_LEFT_PAREN, "Expected '(' after type");
        
        List<ASTNode> arguments = new ArrayList<>();
        if (!check(TokenType.DELIMITER_RIGHT_PAREN)) {
            do {
                arguments.add(parseExpression());
            } while (match(TokenType.DELIMITER_COMMA));
        }
        
        consume(TokenType.DELIMITER_RIGHT_PAREN, "Expected ')' after constructor arguments");
        
        return new MethodCallNode(new VariableNode(type.getSimpleName(), location), "new", arguments, location);
    }
    
    /**
     * 解析成员访问
     */
    private ASTNode parseMemberAccess(ASTNode target) throws ParseException {
        SourceLocation location = createLocation();
        String memberName = consume(TokenType.IDENTIFIER, "Expected member name").getText();
        return new MethodCallNode(target, memberName, new ArrayList<>(), location);
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
        
        String methodName = target instanceof VariableNode ? ((VariableNode) target).getName() : null;
        return new MethodCallNode(target, methodName, arguments, location);
    }
    
    /**
     * 解析数组访问
     */
    private ASTNode parseArrayAccess(ASTNode array) throws ParseException {
        SourceLocation location = createLocation();
        ASTNode index = parseExpression();
        consume(TokenType.DELIMITER_RIGHT_BRACKET, "Expected ']' after array index");
        return array;
    }
}
