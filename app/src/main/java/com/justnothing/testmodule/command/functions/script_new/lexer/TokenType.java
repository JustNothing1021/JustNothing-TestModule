package com.justnothing.testmodule.command.functions.script_new.lexer;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Token类型枚举
 * <p>
 * 定义脚本语言中所有可能的token类型。
 * </p>
 * 
 * @author JustNothing1021
 * @since 1.0.0
 */
public enum TokenType {
    
    KEYWORD("keyword"),
    IDENTIFIER("identifier"),
    LITERAL("literal"),
    OPERATOR("operator"),
    DELIMITER("delimiter"),
    
    KEYWORD_INT("int"),
    KEYWORD_LONG("long"),
    KEYWORD_FLOAT("float"),
    KEYWORD_DOUBLE("double"),
    KEYWORD_BOOLEAN("boolean"),
    KEYWORD_CHAR("char"),
    KEYWORD_BYTE("byte"),
    KEYWORD_SHORT("short"),
    KEYWORD_VOID("void"),
    KEYWORD_AUTO("auto"),
    KEYWORD_IF("if"),
    KEYWORD_ELSE("else"),
    KEYWORD_FOR("for"),
    KEYWORD_WHILE("while"),
    KEYWORD_RETURN("return"),
    KEYWORD_BREAK("break"),
    KEYWORD_CONTINUE("continue"),
    KEYWORD_TRUE("true"),
    KEYWORD_FALSE("false"),
    KEYWORD_NULL("null"),
    KEYWORD_FINAL("final"),
    KEYWORD_NEW("new"),
    KEYWORD_INSTANCEOF("instanceof"),
    
    OPERATOR_ASSIGN("="),
    OPERATOR_PLUS_ASSIGN("+="),
    OPERATOR_MINUS_ASSIGN("-="),
    OPERATOR_MULTIPLY_ASSIGN("*="),
    OPERATOR_DIVIDE_ASSIGN("/="),
    OPERATOR_MODULO_ASSIGN("%="),
    
    OPERATOR_EQUAL("=="),
    OPERATOR_NOT_EQUAL("!="),
    OPERATOR_LESS_THAN("<"),
    OPERATOR_LESS_THAN_OR_EQUAL("<="),
    OPERATOR_GREATER_THAN(">"),
    OPERATOR_GREATER_THAN_OR_EQUAL(">="),
    
    OPERATOR_LOGICAL_AND("&&"),
    OPERATOR_LOGICAL_OR("||"),
    OPERATOR_LOGICAL_NOT("!"),
    
    OPERATOR_BITWISE_AND("&"),
    OPERATOR_BITWISE_OR("|"),
    OPERATOR_BITWISE_XOR("^"),
    OPERATOR_BITWISE_NOT("~"),
    
    OPERATOR_LEFT_SHIFT("<<"),
    OPERATOR_RIGHT_SHIFT(">>"),
    OPERATOR_UNSIGNED_RIGHT_SHIFT(">>>"),
    
    OPERATOR_PLUS("+"),
    OPERATOR_MINUS("-"),
    OPERATOR_MULTIPLY("*"),
    OPERATOR_DIVIDE("/"),
    OPERATOR_MODULO("%"),
    
    OPERATOR_INCREMENT("++"),
    OPERATOR_DECREMENT("--"),
    
    OPERATOR_DOT("."),
    OPERATOR_DOUBLE_COLON("::"),
    
    DELIMITER_SEMICOLON(";"),
    DELIMITER_COMMA(","),
    DELIMITER_DOT("."),
    
    DELIMITER_LEFT_PAREN("("),
    DELIMITER_RIGHT_PAREN(")"),
    DELIMITER_LEFT_BRACE("{"),
    DELIMITER_RIGHT_BRACE("}"),
    DELIMITER_LEFT_BRACKET("["),
    DELIMITER_RIGHT_BRACKET("]"),
    
    DELIMITER_ARROW("->"),
    
    LITERAL_INTEGER("integer literal"),
    LITERAL_DECIMAL("decimal literal"),
    LITERAL_STRING("string literal"),
    LITERAL_BOOLEAN("boolean literal"),
    LITERAL_NULL("null literal"),
    
    EOF("end of file"),
    UNKNOWN("unknown token");
    
    private final String description;
    
    TokenType(String description) {
        this.description = description;
    }
    
    public String getDescription() {
        return description;
    }
    
    public boolean isKeyword() {
        return this.name().startsWith("KEYWORD_");
    }
    
    public boolean isOperator() {
        return this.name().startsWith("OPERATOR_");
    }
    
    public boolean isDelimiter() {
        return this.name().startsWith("DELIMITER_");
    }
    
    public boolean isLiteral() {
        return this.name().startsWith("LITERAL_");
    }
    
    private static final Set<String> KEYWORDS = new HashSet<>(Arrays.asList(
            "int", "long", "float", "double", "boolean", "char", "byte", "short",
            "void", "auto", "if", "else", "for", "while",
            "return", "break", "continue", "true", "false", "null", "final",
            "new", "instanceof"
    ));
    
    public static boolean isKeyword(String text) {
        return KEYWORDS.contains(text);
    }
    
    public static TokenType getKeywordType(String text) {
        switch (text) {
            case "int": return KEYWORD_INT;
            case "long": return KEYWORD_LONG;
            case "float": return KEYWORD_FLOAT;
            case "double": return KEYWORD_DOUBLE;
            case "boolean": return KEYWORD_BOOLEAN;
            case "char": return KEYWORD_CHAR;
            case "byte": return KEYWORD_BYTE;
            case "short": return KEYWORD_SHORT;
            case "void": return KEYWORD_VOID;
            case "auto": return KEYWORD_AUTO;
            case "if": return KEYWORD_IF;
            case "else": return KEYWORD_ELSE;
            case "for": return KEYWORD_FOR;
            case "while": return KEYWORD_WHILE;
            case "return": return KEYWORD_RETURN;
            case "break": return KEYWORD_BREAK;
            case "continue": return KEYWORD_CONTINUE;
            case "true": return KEYWORD_TRUE;
            case "false": return KEYWORD_FALSE;
            case "null": return KEYWORD_NULL;
            case "final": return KEYWORD_FINAL;
            case "new": return KEYWORD_NEW;
            case "instanceof": return KEYWORD_INSTANCEOF;
            default: return IDENTIFIER;
        }
    }
}
