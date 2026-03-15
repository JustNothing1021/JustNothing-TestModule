package com.justnothing.testmodule.command.functions.script_new.lexer;

import com.justnothing.testmodule.command.functions.script_new.ast.SourceLocation;
import com.justnothing.testmodule.command.functions.script_new.exception.ErrorCode;
import com.justnothing.testmodule.command.functions.script_new.exception.ParseException;

import java.util.ArrayList;
import java.util.List;

/**
 * 词法分析器（Lexer）
 * <p>
 * 将源代码字符串转换为token流。
 * </p>
 * 
 * @author JustNothing1021
 * @since 1.0.0
 */
public class Lexer {
    
    private final String source;
    private final int length;
    private int position;
    private int line;
    private int column;
    private final List<Token> tokens;
    
    public Lexer(String source) {
        this.source = source;
        this.length = source.length();
        this.position = 0;
        this.line = 1;
        this.column = 1;
        this.tokens = new ArrayList<>();
    }
    
    public List<Token> tokenize() throws ParseException {
        while (!isAtEnd()) {
            skipWhitespace();
            
            if (isAtEnd()) {
                break;
            }
            
            char c = peek();
            
            if (Character.isDigit(c)) {
                readNumber();
            } else if (c == '"' || c == '\'') {
                readString();
            } else if (Character.isLetter(c) || c == '_') {
                readIdentifier();
            } else if (isOperatorChar(c)) {
                readOperator();
            } else if (isDelimiterChar(c)) {
                readDelimiter();
            } else {
                throw error("Unexpected character: '" + c + "'");
            }
        }
        
        addToken(TokenType.EOF, createLocation());
        return tokens;
    }
    
    private boolean isAtEnd() {
        return position >= length;
    }
    
    private char peek() {
        if (isAtEnd()) {
            return '\0';
        }
        return source.charAt(position);
    }
    
    private char peekNext() {
        if (position + 1 >= length) {
            return '\0';
        }
        return source.charAt(position + 1);
    }
    
    private char advance() {
        char c = peek();
        position++;
        if (c == '\n') {
            line++;
            column = 1;
        } else {
            column++;
        }
        return c;
    }
    
    private void skipWhitespace() {
        while (!isAtEnd() && Character.isWhitespace(peek())) {
            advance();
        }
    }
    
    private boolean match(char expected) {
        if (isAtEnd() || peek() != expected) {
            return false;
        }
        advance();
        return true;
    }
    
    private void addToken(TokenType type, SourceLocation location) {
        addToken(type, null, location);
    }
    
    private void addToken(TokenType type, Object value, SourceLocation location) {
        tokens.add(new Token(type, value, location));
    }
    
    private SourceLocation createLocation() {
        return new SourceLocation(line, column);
    }
    
    private ParseException error(String message) {
        return new ParseException(
                message,
                createLocation(),
                ErrorCode.LEXICAL_ERROR
        );
    }
    
    private boolean isOperatorChar(char c) {
        return "+-*/%=!&|^~<>?:.".indexOf(c) >= 0;
    }
    
    private boolean isDelimiterChar(char c) {
        return ";,(){}[]".indexOf(c) >= 0;
    }
    
    private void readNumber() {
        int startLine = line;
        int startColumn = column;
        StringBuilder sb = new StringBuilder();
        
        while (!isAtEnd() && (Character.isDigit(peek()) || peek() == '.')) {
            sb.append(advance());
        }
        
        String numberStr = sb.toString();
        SourceLocation location = new SourceLocation(startLine, startColumn);
        
        if (numberStr.contains(".")) {
            try {
                double value = Double.parseDouble(numberStr);
                addToken(TokenType.LITERAL_DECIMAL, value, location);
            } catch (NumberFormatException e) {
                throw error("Invalid decimal number: " + numberStr);
            }
        } else {
            try {
                long value = Long.parseLong(numberStr);
                addToken(TokenType.LITERAL_INTEGER, value, location);
            } catch (NumberFormatException e) {
                throw error("Invalid integer number: " + numberStr);
            }
        }
    }
    
    private void readString() {
        char quote = advance();
        int startLine = line;
        int startColumn = column;
        StringBuilder sb = new StringBuilder();
        
        while (!isAtEnd() && peek() != quote) {
            char c = advance();
            if (c == '\\') {
                if (!isAtEnd()) {
                    char next = advance();
                    switch (next) {
                        case 'n': sb.append('\n'); break;
                        case 't': sb.append('\t'); break;
                        case 'r': sb.append('\r'); break;
                        case 'b': sb.append('\b'); break;
                        case 'f': sb.append('\f'); break;
                        case '\\': sb.append('\\'); break;
                        case '\'': sb.append('\''); break;
                        case '\"': sb.append('\"'); break;
                        default: sb.append(next); break;
                    }
                }
            } else {
                sb.append(c);
            }
        }
        
        if (isAtEnd()) {
            throw error("Unterminated string literal");
        }
        
        advance();
        SourceLocation location = new SourceLocation(startLine, startColumn);
        addToken(TokenType.LITERAL_STRING, sb.toString(), location);
    }
    
    private void readIdentifier() {
        int startLine = line;
        int startColumn = column;
        StringBuilder sb = new StringBuilder();
        
        while (!isAtEnd() && (Character.isLetterOrDigit(peek()) || peek() == '_')) {
            sb.append(advance());
        }
        
        String text = sb.toString();
        SourceLocation location = new SourceLocation(startLine, startColumn);
        
        if (TokenType.isKeyword(text)) {
            TokenType type = TokenType.getKeywordType(text);
            if (type == TokenType.KEYWORD_TRUE) {
                addToken(TokenType.LITERAL_BOOLEAN, true, location);
            } else if (type == TokenType.KEYWORD_FALSE) {
                addToken(TokenType.LITERAL_BOOLEAN, false, location);
            } else if (type == TokenType.KEYWORD_NULL) {
                addToken(TokenType.LITERAL_NULL, null, location);
            } else {
                addToken(type, location);
            }
        } else {
            addToken(TokenType.IDENTIFIER, text, location);
        }
    }
    
    private void readOperator() {
        int startLine = line;
        int startColumn = column;
        char c = advance();
        char next = peek();
        
        SourceLocation location = new SourceLocation(startLine, startColumn);
        
        switch (c) {
            case '+':
                if (match('=')) addToken(TokenType.OPERATOR_PLUS_ASSIGN, location);
                else if (match('+')) addToken(TokenType.OPERATOR_INCREMENT, location);
                else addToken(TokenType.OPERATOR_PLUS, location);
                break;
            case '-':
                if (match('=')) addToken(TokenType.OPERATOR_MINUS_ASSIGN, location);
                else if (match('-')) addToken(TokenType.OPERATOR_DECREMENT, location);
                else if (match('>')) addToken(TokenType.DELIMITER_ARROW, location);
                else addToken(TokenType.OPERATOR_MINUS, location);
                break;
            case '*':
                if (match('=')) addToken(TokenType.OPERATOR_MULTIPLY_ASSIGN, location);
                else addToken(TokenType.OPERATOR_MULTIPLY, location);
                break;
            case '/':
                if (match('=')) addToken(TokenType.OPERATOR_DIVIDE_ASSIGN, location);
                else addToken(TokenType.OPERATOR_DIVIDE, location);
                break;
            case '%':
                if (match('=')) addToken(TokenType.OPERATOR_MODULO_ASSIGN, location);
                else addToken(TokenType.OPERATOR_MODULO, location);
                break;
            case '=':
                if (match('=')) addToken(TokenType.OPERATOR_EQUAL, location);
                else addToken(TokenType.OPERATOR_ASSIGN, location);
                break;
            case '!':
                if (match('=')) addToken(TokenType.OPERATOR_NOT_EQUAL, location);
                else addToken(TokenType.OPERATOR_LOGICAL_NOT, location);
                break;
            case '<':
                if (match('=')) addToken(TokenType.OPERATOR_LESS_THAN_OR_EQUAL, location);
                else if (match('<')) addToken(TokenType.OPERATOR_LEFT_SHIFT, location);
                else addToken(TokenType.OPERATOR_LESS_THAN, location);
                break;
            case '>':
                if (match('=')) addToken(TokenType.OPERATOR_GREATER_THAN_OR_EQUAL, location);
                else if (match('>')) {
                    if (match('>')) addToken(TokenType.OPERATOR_UNSIGNED_RIGHT_SHIFT, location);
                    else addToken(TokenType.OPERATOR_RIGHT_SHIFT, location);
                } else {
                    addToken(TokenType.OPERATOR_GREATER_THAN, location);
                }
                break;
            case '&':
                if (match('&')) addToken(TokenType.OPERATOR_LOGICAL_AND, location);
                else addToken(TokenType.OPERATOR_BITWISE_AND, location);
                break;
            case '|':
                if (match('|')) addToken(TokenType.OPERATOR_LOGICAL_OR, location);
                else addToken(TokenType.OPERATOR_BITWISE_OR, location);
                break;
            case '^':
                addToken(TokenType.OPERATOR_BITWISE_XOR, location);
                break;
            case '~':
                addToken(TokenType.OPERATOR_BITWISE_NOT, location);
                break;
            case '.':
                if (match('.')) {
                    if (match('.')) {
                        throw error("Spread operator not supported");
                    }
                } else {
                    addToken(TokenType.OPERATOR_DOT, location);
                }
                break;
            case ':':
                if (match(':')) addToken(TokenType.OPERATOR_DOUBLE_COLON, location);
                break;
            default:
                throw error("Unknown operator: '" + c + "'");
        }
    }
    
    private void readDelimiter() {
        int startLine = line;
        int startColumn = column;
        char c = advance();
        
        SourceLocation location = new SourceLocation(startLine, startColumn);
        
        switch (c) {
            case ';':
                addToken(TokenType.DELIMITER_SEMICOLON, location);
                break;
            case ',':
                addToken(TokenType.DELIMITER_COMMA, location);
                break;
            case '(':
                addToken(TokenType.DELIMITER_LEFT_PAREN, location);
                break;
            case ')':
                addToken(TokenType.DELIMITER_RIGHT_PAREN, location);
                break;
            case '{':
                addToken(TokenType.DELIMITER_LEFT_BRACE, location);
                break;
            case '}':
                addToken(TokenType.DELIMITER_RIGHT_BRACE, location);
                break;
            case '[':
                addToken(TokenType.DELIMITER_LEFT_BRACKET, location);
                break;
            case ']':
                addToken(TokenType.DELIMITER_RIGHT_BRACKET, location);
                break;
            default:
                throw error("Unknown delimiter: '" + c + "'");
        }
    }
}
