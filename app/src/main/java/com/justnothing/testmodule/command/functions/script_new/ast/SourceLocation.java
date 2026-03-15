package com.justnothing.testmodule.command.functions.script_new.ast;

import java.util.Objects;

/**
 * 源代码位置
 * <p>
 * 用于标记AST节点在源代码中的位置，便于错误定位和调试。
 * </p>
 * 
 * @author JustNothing1021
 * @since 1.0.0
 */
public class SourceLocation {
    
    private final int line;
    private final int column;
    
    public SourceLocation(int line, int column) {
        this.line = line;
        this.column = column;
    }
    
    public int getLine() {
        return line;
    }
    
    public int getColumn() {
        return column;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SourceLocation that = (SourceLocation) o;
        return line == that.line && column == that.column;
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(line, column);
    }
    
    @Override
    public String toString() {
        return "SourceLocation{" +
                "line=" + line +
                ", column=" + column +
                '}';
    }
}
