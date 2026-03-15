package com.justnothing.testmodule.command.functions.script_new.parser;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * 解析上下文
 * <p>
 * 在解析过程中维护的上下文信息，包括：
 * - 当前位置（行号、列号）
 * - 期望的参数类型（用于方法引用解析）
 * - 嵌套层级（用于括号、花括号等）
 * </p>
 * 
 * @author JustNothing1021
 * @since 1.0.0
 */
public class ParseContext {
    
    private int currentLine;
    private int currentColumn;
    private Class<?> expectedParamType;
    private final Deque<NestingLevel> nestingStack;
    
    public ParseContext() {
        this.currentLine = 1;
        this.currentColumn = 1;
        this.expectedParamType = null;
        this.nestingStack = new ArrayDeque<>();
    }
    
    public int getCurrentLine() {
        return currentLine;
    }
    
    public void setCurrentLine(int currentLine) {
        this.currentLine = currentLine;
    }
    
    public int getCurrentColumn() {
        return currentColumn;
    }
    
    public void setCurrentColumn(int currentColumn) {
        this.currentColumn = currentColumn;
    }
    
    public Class<?> getExpectedParamType() {
        return expectedParamType;
    }
    
    public void setExpectedParamType(Class<?> expectedParamType) {
        this.expectedParamType = expectedParamType;
    }
    
    public void clearExpectedParamType() {
        this.expectedParamType = null;
    }
    
    public void pushNestingLevel(NestingLevel level) {
        nestingStack.push(level);
    }
    
    public NestingLevel popNestingLevel() {
        return nestingStack.pop();
    }
    
    public NestingLevel peekNestingLevel() {
        return nestingStack.peek();
    }
    
    public boolean isNestingStackEmpty() {
        return nestingStack.isEmpty();
    }
    
    /**
     * 嵌套层级类型
     */
    public enum NestingLevel {
        PARENTHESIS,  // 圆括号 ()
        BRACE,        // 花括号 {}
        BRACKET,       // 方括号 []
        LAMBDA,        // Lambda表达式
        METHOD_CALL    // 方法调用
    }
}
