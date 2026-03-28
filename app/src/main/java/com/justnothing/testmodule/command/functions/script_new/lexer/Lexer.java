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
        
        if (peek() == '0' && !isAtEnd()) {
            char next = peekNext();
            if (next == 'x' || next == 'X') {
                advance();
                advance();
                while (!isAtEnd() && isHexDigit(peek())) {
                    sb.append(advance());
                }
                String hexStr = sb.toString();
                SourceLocation location = new SourceLocation(startLine, startColumn);
                try {
                    long value = Long.parseLong(hexStr, 16);
                    addToken(TokenType.LITERAL_INTEGER, value, location);
                } catch (NumberFormatException e) {
                    throw error("Invalid hexadecimal number: 0x" + hexStr);
                }
                return;
            } else if (next == 'b' || next == 'B') {
                advance();
                advance();
                while (!isAtEnd() && (peek() == '0' || peek() == '1')) {
                    sb.append(advance());
                }
                String binStr = sb.toString();
                SourceLocation location = new SourceLocation(startLine, startColumn);
                try {
                    long value = Long.parseLong(binStr, 2);
                    addToken(TokenType.LITERAL_INTEGER, value, location);
                } catch (NumberFormatException e) {
                    throw error("Invalid binary number: 0b" + binStr);
                }
                return;
            } else if (next == 'o' || next == 'O') {
                advance();
                advance();
                while (!isAtEnd() && isOctalDigit(peek())) {
                    sb.append(advance());
                }
                String octStr = sb.toString();
                SourceLocation location = new SourceLocation(startLine, startColumn);
                try {
                    long value = Long.parseLong(octStr, 8);
                    addToken(TokenType.LITERAL_INTEGER, value, location);
                } catch (NumberFormatException e) {
                    throw error("Invalid octal number: 0o" + octStr);
                }
                return;
            }
        }
        
        while (!isAtEnd() && (Character.isDigit(peek()) || peek() == '.')) {
            sb.append(advance());
        }
        
        if (!isAtEnd() && (peek() == 'e' || peek() == 'E')) {
            sb.append(advance());
            if (!isAtEnd() && (peek() == '+' || peek() == '-')) {
                sb.append(advance());
            }
            while (!isAtEnd() && Character.isDigit(peek())) {
                sb.append(advance());
            }
        }
        
        String numberStr = sb.toString();
        SourceLocation location = new SourceLocation(startLine, startColumn);
        
        boolean isFloat = false;
        boolean isLong = false;
        boolean isDouble = false;
        boolean hasExponent = numberStr.contains("e") || numberStr.contains("E");
        
        if (!isAtEnd()) {
            char suffix = Character.toLowerCase(peek());
            if (suffix == 'f') {
                advance();
                isFloat = true;
            } else if (suffix == 'l') {
                advance();
                isLong = true;
            } else if (suffix == 'd') {
                advance();
                isDouble = true;
            }
        }
        
        if (isFloat) {
            try {
                float value = Float.parseFloat(numberStr);
                addToken(TokenType.LITERAL_DECIMAL, (double) value, location);
            } catch (NumberFormatException e) {
                throw error("Invalid float number: " + numberStr);
            }
        } else if (isLong) {
            try {
                long value = Long.parseLong(numberStr);
                addToken(TokenType.LITERAL_LONG, value, location);
            } catch (NumberFormatException e) {
                throw error("Invalid long number: " + numberStr);
            }
        } else if (isDouble || numberStr.contains(".") || hasExponent) {
            try {
                double value = Double.parseDouble(numberStr);
                addToken(TokenType.LITERAL_DECIMAL, value, location);
            } catch (NumberFormatException e) {
                throw error("Invalid decimal number: " + numberStr);
            }
        } else {
            try {
                long longValue = Long.parseLong(numberStr);
                if (longValue >= Integer.MIN_VALUE && longValue <= Integer.MAX_VALUE) {
                    addToken(TokenType.LITERAL_INTEGER, (int) longValue, location);
                } else {
                    addToken(TokenType.LITERAL_LONG, longValue, location);
                }
            } catch (NumberFormatException e) {
                throw error("Invalid integer number: " + numberStr);
            }
        }
    }
    
    private boolean isHexDigit(char c) {
        return (c >= '0' && c <= '9') || (c >= 'a' && c <= 'f') || (c >= 'A' && c <= 'F');
    }
    
    private boolean isOctalDigit(char c) {
        return c >= '0' && c <= '7';
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
                        case 'x': {
                            StringBuilder hex = new StringBuilder();
                            for (int i = 0; i < 2 && !isAtEnd(); i++) {
                                char h = peek();
                                if ((h >= '0' && h <= '9') || 
                                    (h >= 'a' && h <= 'f') || 
                                    (h >= 'A' && h <= 'F')) {
                                    hex.append(advance());
                                } else {
                                    break;
                                }
                            }
                            if (hex.length() > 0) {
                                try {
                                    int codePoint = Integer.parseInt(hex.toString(), 16);
                                    sb.append((char) codePoint);
                                } catch (NumberFormatException e) {
                                    sb.append("\\x").append(hex);
                                }
                            } else {
                                sb.append("\\x");
                            }
                            break;
                        }
                        case 'u': {
                            StringBuilder hex = new StringBuilder();
                            for (int i = 0; i < 4 && !isAtEnd(); i++) {
                                char h = peek();
                                if ((h >= '0' && h <= '9') || 
                                    (h >= 'a' && h <= 'f') || 
                                    (h >= 'A' && h <= 'F')) {
                                    hex.append(advance());
                                } else {
                                    break;
                                }
                            }
                            if (hex.length() == 4) {
                                try {
                                    int codePoint = Integer.parseInt(hex.toString(), 16);
                                    sb.append((char) codePoint);
                                } catch (NumberFormatException e) {
                                    sb.append("\\u").append(hex);
                                }
                            } else {
                                sb.append("\\u").append(hex);
                            }
                            break;
                        }
                        case '0': case '1': case '2': case '3':
                        case '4': case '5': case '6': case '7': {
                            int octal = next - '0';
                            for (int i = 0; i < 2 && !isAtEnd(); i++) {
                                char d = peek();
                                if (d >= '0' && d <= '7') {
                                    octal = octal * 8 + (advance() - '0');
                                } else {
                                    break;
                                }
                            }
                            sb.append((char) octal);
                            break;
                        }
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
        
        if (quote == '\'') {
            if (sb.length() != 1) {
                throw error("Invalid character literal: must contain exactly one character");
            }
            addToken(TokenType.LITERAL_CHAR, sb.charAt(0), location);
        } else {
            addToken(TokenType.LITERAL_STRING, sb.toString(), location);
        }
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
                addToken(type, text, location);
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
                else if (match('<')) {
                    if (match('=')) addToken(TokenType.OPERATOR_LEFT_SHIFT_ASSIGN, location);
                    else addToken(TokenType.OPERATOR_LEFT_SHIFT, location);
                }
                else addToken(TokenType.OPERATOR_LESS_THAN, location);
                break;
            case '>':
                if (match('=')) addToken(TokenType.OPERATOR_GREATER_THAN_OR_EQUAL, location);
                else if (match('>')) {
                    if (match('>')) {
                        if (match('=')) addToken(TokenType.OPERATOR_UNSIGNED_RIGHT_SHIFT_ASSIGN, location);
                        else addToken(TokenType.OPERATOR_UNSIGNED_RIGHT_SHIFT, location);
                    } else if (match('=')) {
                        addToken(TokenType.OPERATOR_RIGHT_SHIFT_ASSIGN, location);
                    } else {
                        addToken(TokenType.OPERATOR_RIGHT_SHIFT, location);
                    }
                } else {
                    addToken(TokenType.OPERATOR_GREATER_THAN, location);
                }
                break;
            case '&':
                if (match('&')) addToken(TokenType.OPERATOR_LOGICAL_AND, location);
                else if (match('=')) addToken(TokenType.OPERATOR_BITWISE_AND_ASSIGN, location);
                else addToken(TokenType.OPERATOR_BITWISE_AND, location);
                break;
            case '|':
                if (match('|')) addToken(TokenType.OPERATOR_LOGICAL_OR, location);
                else if (match('=')) addToken(TokenType.OPERATOR_BITWISE_OR_ASSIGN, location);
                else addToken(TokenType.OPERATOR_BITWISE_OR, location);
                break;
            case '^':
                if (match('=')) addToken(TokenType.OPERATOR_BITWISE_XOR_ASSIGN, location);
                else addToken(TokenType.OPERATOR_BITWISE_XOR, location);
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
                else addToken(TokenType.OPERATOR_COLON, location);
                break;
            case '?':
                addToken(TokenType.OPERATOR_QUESTION, location);
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
