package com.justnothing.javainterpreter.lexer;

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
    KEYWORD_DO("do"),
    KEYWORD_RETURN("return"),
    KEYWORD_BREAK("break"),
    KEYWORD_CONTINUE("continue"),
    KEYWORD_TRUE("true"),
    KEYWORD_FALSE("false"),
    KEYWORD_NULL("null"),
    KEYWORD_FINAL("final"),
    KEYWORD_NEW("new"),
    KEYWORD_INSTANCEOF("instanceof"),
    KEYWORD_SWITCH("switch"),
    KEYWORD_CASE("case"),
    KEYWORD_DEFAULT("default"),
    KEYWORD_TRY("try"),
    KEYWORD_CATCH("catch"),
    KEYWORD_FINALLY("finally"),
    KEYWORD_THROW("throw"),
    KEYWORD_THROWS("throws"),
    KEYWORD_IMPORT("import"),
    KEYWORD_DELETE("delete"),
    KEYWORD_CLASS("class"),
    KEYWORD_INTERFACE("interface"),
    KEYWORD_EXTENDS("extends"),
    KEYWORD_IMPLEMENTS("implements"),
    KEYWORD_PUBLIC("public"),
    KEYWORD_PRIVATE("private"),
    KEYWORD_PROTECTED("protected"),
    KEYWORD_STATIC("static"),
    KEYWORD_ABSTRACT("abstract"),
    KEYWORD_NATIVE("native"),
    KEYWORD_SYNCHRONIZED("synchronized"),
    KEYWORD_SUPER("super"),
    KEYWORD_THIS("this"),
    KEYWORD_ASYNC("async"),
    KEYWORD_AWAIT("await"),
    
    OPERATOR_ASSIGN("="),
    OPERATOR_PLUS_ASSIGN("+="),
    OPERATOR_MINUS_ASSIGN("-="),
    OPERATOR_MULTIPLY_ASSIGN("*="),
    OPERATOR_DIVIDE_ASSIGN("/="),
    OPERATOR_MODULO_ASSIGN("%="),
    OPERATOR_BITWISE_AND_ASSIGN("&="),
    OPERATOR_BITWISE_OR_ASSIGN("|="),
    OPERATOR_BITWISE_XOR_ASSIGN("^="),
    OPERATOR_LEFT_SHIFT_ASSIGN("<<="),
    OPERATOR_RIGHT_SHIFT_ASSIGN(">>="),
    OPERATOR_UNSIGNED_RIGHT_SHIFT_ASSIGN(">>>="),
    
    OPERATOR_EQUAL("=="),
    OPERATOR_NOT_EQUAL("!="),
    OPERATOR_LESS_THAN("<"),
    OPERATOR_LESS_THAN_OR_EQUAL("<="),
    OPERATOR_GREATER_THAN(">"),
    OPERATOR_GREATER_THAN_OR_EQUAL(">="),
    OPERATOR_SPACESHIP("<=>"),
    
    OPERATOR_LOGICAL_AND("&&"),
    OPERATOR_LOGICAL_OR("||"),
    OPERATOR_LOGICAL_NOT("!"),
    OPERATOR_NOT_NULL("!!"),
    
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
    
    OPERATOR_POWER("**"),
    OPERATOR_INT_DIVIDE("//"),
    OPERATOR_MATH_MODULO("%%"),
    OPERATOR_RANGE(".."),
    OPERATOR_RANGE_EXCLUSIVE("..<"),
    
    OPERATOR_INCREMENT("++"),
    OPERATOR_DECREMENT("--"),
    
    OPERATOR_DOT("."),
    OPERATOR_SAFE_DOT("?."),
    OPERATOR_DOUBLE_COLON("::"),
    OPERATOR_QUESTION("?"),
    OPERATOR_COLON(":"),
    OPERATOR_DECLARE_ASSIGN(":="),
    OPERATOR_CONDITIONAL_ASSIGN("?="),
    OPERATOR_NULL_COALESCING_ASSIGN("??="),
    OPERATOR_NULL_COALESCING("??"),
    OPERATOR_ELVIS("?:"),
    
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
    OPERATOR_PIPELINE("|>"),
    DELIMITER_AT("@"),
    
    LITERAL_INTEGER("integer literal"),
    LITERAL_LONG("long literal"),
    LITERAL_DECIMAL("decimal literal"),
    LITERAL_STRING("string literal"),
    LITERAL_INTERPOLATED_STRING("interpolated string literal"),
    LITERAL_CHAR("char literal"),
    LITERAL_BOOLEAN("boolean literal"),
    LITERAL_NULL("null literal"),
    
    COMMENT("comment"),
    
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
            "void", "auto", "if", "else", "for", "while", "do",
            "return", "break", "continue", "true", "false", "null", "final",
            "new", "instanceof", "switch", "case", "default",
            "try", "catch", "finally", "throw", "throws", "import", "delete",
            "class", "interface", "extends", "implements",
            "public", "private", "protected", "static", "abstract", "native", "synchronized",
            "super", "this", "async", "await"
    ));
    
    public static boolean isKeyword(String text) {
        return KEYWORDS.contains(text);
    }
    
    public static TokenType getKeywordType(String text) {
        return switch (text) {
            case "int" -> KEYWORD_INT;
            case "long" -> KEYWORD_LONG;
            case "float" -> KEYWORD_FLOAT;
            case "double" -> KEYWORD_DOUBLE;
            case "boolean" -> KEYWORD_BOOLEAN;
            case "char" -> KEYWORD_CHAR;
            case "byte" -> KEYWORD_BYTE;
            case "short" -> KEYWORD_SHORT;
            case "void" -> KEYWORD_VOID;
            case "auto" -> KEYWORD_AUTO;
            case "if" -> KEYWORD_IF;
            case "else" -> KEYWORD_ELSE;
            case "for" -> KEYWORD_FOR;
            case "while" -> KEYWORD_WHILE;
            case "do" -> KEYWORD_DO;
            case "return" -> KEYWORD_RETURN;
            case "break" -> KEYWORD_BREAK;
            case "continue" -> KEYWORD_CONTINUE;
            case "true" -> KEYWORD_TRUE;
            case "false" -> KEYWORD_FALSE;
            case "null" -> KEYWORD_NULL;
            case "final" -> KEYWORD_FINAL;
            case "new" -> KEYWORD_NEW;
            case "instanceof" -> KEYWORD_INSTANCEOF;
            case "switch" -> KEYWORD_SWITCH;
            case "case" -> KEYWORD_CASE;
            case "default" -> KEYWORD_DEFAULT;
            case "try" -> KEYWORD_TRY;
            case "catch" -> KEYWORD_CATCH;
            case "finally" -> KEYWORD_FINALLY;
            case "throw" -> KEYWORD_THROW;
            case "throws" -> KEYWORD_THROWS;
            case "import" -> KEYWORD_IMPORT;
            case "delete" -> KEYWORD_DELETE;
            case "class" -> KEYWORD_CLASS;
            case "interface" -> KEYWORD_INTERFACE;
            case "extends" -> KEYWORD_EXTENDS;
            case "implements" -> KEYWORD_IMPLEMENTS;
            case "public" -> KEYWORD_PUBLIC;
            case "private" -> KEYWORD_PRIVATE;
            case "protected" -> KEYWORD_PROTECTED;
            case "static" -> KEYWORD_STATIC;
            case "abstract" -> KEYWORD_ABSTRACT;
            case "native" -> KEYWORD_NATIVE;
            case "synchronized" -> KEYWORD_SYNCHRONIZED;
            case "super" -> KEYWORD_SUPER;
            case "this" -> KEYWORD_THIS;
            case "async" -> KEYWORD_ASYNC;
            case "await" -> KEYWORD_AWAIT;
            default -> IDENTIFIER;
        };
    }
}
