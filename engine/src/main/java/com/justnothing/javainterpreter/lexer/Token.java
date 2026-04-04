package com.justnothing.javainterpreter.lexer;

import com.justnothing.javainterpreter.ast.SourceLocation;

import java.util.Locale;
import java.util.Objects;

/**
 * Token类
 * <p>
 * 表示词法分析产生的token，包含类型、值和位置信息。
 * </p>
 * 
 * @author JustNothing1021
 * @since 1.0.0
 */
public class Token {
    
    private final TokenType type;
    private final String text;
    private final Object value;
    private final SourceLocation location;
    
    public Token(TokenType type, String text, Object value, SourceLocation location) {
        this.type = type;
        this.text = text;
        this.value = value;
        this.location = location;
    }
    
    public Token(TokenType type, String text, SourceLocation location) {
        this(type, text, text, location);
    }
    
    public Token(TokenType type, Object value, SourceLocation location) {
        this(type, value != null ? value.toString() : null, value, location);
    }
    
    public Token(TokenType type, SourceLocation location) {
        this(type, null, null, location);
    }
    
    public TokenType getType() {
        return type;
    }
    
    public String getText() {
        return text;
    }
    
    public Object getValue() {
        return value;
    }
    
    public SourceLocation getLocation() {
        return location;
    }
    
    public boolean is(TokenType type) {
        return this.type == type;
    }
    
    public boolean isKeyword() {
        return type.isKeyword();
    }
    
    public boolean isOperator() {
        return type.isOperator();
    }
    
    public boolean isDelimiter() {
        return type.isDelimiter();
    }
    
    public boolean isLiteral() {
        return type.isLiteral();
    }
    
    public boolean isEOF() {
        return type == TokenType.EOF;
    }
    
    @Override
    public String toString() {
        if (value != null && !value.equals(text)) {
            return String.format(
                    Locale.getDefault(),
                    "Token[%s, %s, value=%s, line=%d, col=%d]",
                    type, text, value, location.getLine(), location.getColumn());
        }
        return String.format(
                Locale.getDefault(),
                "Token[%s, %s, line=%d, col=%d]",
                type, text, location.getLine(), location.getColumn());
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Token token = (Token) obj;
        return type == token.type &&
               (Objects.equals(text, token.text)) &&
               (Objects.equals(value, token.value));
    }
    
    @Override
    public int hashCode() {
        int result = type != null ? type.hashCode() : 0;
        result = 31 * result + (text != null ? text.hashCode() : 0);
        result = 31 * result + (value != null ? value.hashCode() : 0);
        return result;
    }
}
