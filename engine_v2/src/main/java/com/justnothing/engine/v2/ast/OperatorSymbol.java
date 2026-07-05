package com.justnothing.engine.v2.ast;

import java.util.Set;

/**
 * 运算符符号常量。
 * <p>
 * 集中定义所有运算符的文本表示，避免在代码中硬编码字符串。
 * </p>
 *
 * @author JustNothing1021
 */
public final class OperatorSymbol {

    private OperatorSymbol() {}

    // ========== 算术运算 ==========

    public static final String ADD = "+";
    public static final String SUBTRACT = "-";
    public static final String MULTIPLY = "*";
    public static final String DIVIDE = "/";
    public static final String MODULO = "%";
    public static final String POWER = "**";
    public static final String INT_DIVIDE = "//";
    public static final String MATH_MODULO = "%%";

    // ========== 范围运算 ==========

    public static final String RANGE = "..";
    public static final String RANGE_EXCLUSIVE = "..<";

    // ========== 比较运算 ==========

    public static final String EQUAL = "==";
    public static final String NOT_EQUAL = "!=";
    public static final String LESS_THAN = "<";
    public static final String LESS_THAN_OR_EQUAL = "<=";
    public static final String GREATER_THAN = ">";
    public static final String GREATER_THAN_OR_EQUAL = ">=";
    public static final String SPACESHIP = "<=>";

    // ========== 逻辑运算 ==========

    public static final String LOGICAL_AND = "&&";
    public static final String LOGICAL_OR = "||";
    public static final String NOT = "!";

    // ========== 位运算 ==========

    public static final String BITWISE_AND = "&";
    public static final String BITWISE_OR = "|";
    public static final String BITWISE_XOR = "^";
    public static final String BITWISE_NOT = "~";
    public static final String LEFT_SHIFT = "<<";
    public static final String RIGHT_SHIFT = ">>";
    public static final String UNSIGNED_RIGHT_SHIFT = ">>>";

    // ========== 空值/Elvis ==========

    public static final String NULL_COALESCING = "??";
    public static final String ELVIS = "?:";

    // ========== 自增/自减 ==========

    public static final String INCREMENT = "++";
    public static final String DECREMENT = "--";

    // ========== 赋值运算 ==========

    public static final String ASSIGN = "=";
    public static final String ADD_ASSIGN = "+=";
    public static final String SUBTRACT_ASSIGN = "-=";
    public static final String MULTIPLY_ASSIGN = "*=";
    public static final String DIVIDE_ASSIGN = "/=";
    public static final String MODULO_ASSIGN = "%=";
    public static final String BITWISE_AND_ASSIGN = "&=";
    public static final String BITWISE_OR_ASSIGN = "|=";
    public static final String BITWISE_XOR_ASSIGN = "^=";
    public static final String LEFT_SHIFT_ASSIGN = "<<=";
    public static final String RIGHT_SHIFT_ASSIGN = ">>=";
    public static final String UNSIGNED_RIGHT_SHIFT_ASSIGN = ">>>=";
    public static final String NULL_COALESCING_ASSIGN = "??=";
    public static final String ELVIS_ASSIGN = "?:=";

    // ========== 运算符集合 ==========

    public static final Set<String> BINARY_OPERATORS = Set.of(
            ADD, SUBTRACT, MULTIPLY, DIVIDE, MODULO,
            POWER, INT_DIVIDE, MATH_MODULO,
            RANGE, RANGE_EXCLUSIVE,
            EQUAL, NOT_EQUAL, LESS_THAN, GREATER_THAN,
            LESS_THAN_OR_EQUAL, GREATER_THAN_OR_EQUAL, SPACESHIP,
            BITWISE_AND, BITWISE_OR, BITWISE_XOR,
            LEFT_SHIFT, RIGHT_SHIFT, UNSIGNED_RIGHT_SHIFT,
            LOGICAL_AND, LOGICAL_OR,
            NULL_COALESCING, ELVIS
    );

    public static final Set<String> UNARY_OPERATORS = Set.of(
            ADD, SUBTRACT, NOT, BITWISE_NOT, INCREMENT, DECREMENT
    );

    public static final Set<String> ASSIGN_OPERATORS = Set.of(
            ASSIGN, ADD_ASSIGN, SUBTRACT_ASSIGN, MULTIPLY_ASSIGN,
            DIVIDE_ASSIGN, MODULO_ASSIGN,
            BITWISE_AND_ASSIGN, BITWISE_OR_ASSIGN, BITWISE_XOR_ASSIGN,
            LEFT_SHIFT_ASSIGN, RIGHT_SHIFT_ASSIGN, UNSIGNED_RIGHT_SHIFT_ASSIGN,
            NULL_COALESCING_ASSIGN, ELVIS_ASSIGN
    );

    /**
     * 判断是否为二元运算符
     */
    public static boolean isBinary(String op) {
        return BINARY_OPERATORS.contains(op);
    }

    /**
     * 判断是否为一元运算符
     */
    public static boolean isUnary(String op) {
        return UNARY_OPERATORS.contains(op);
    }

    /**
     * 判断是否为赋值运算符
     */
    public static boolean isAssignment(String op) {
        return ASSIGN_OPERATORS.contains(op);
    }
}
