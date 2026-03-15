package com.justnothing.testmodule.command.functions.script_new.exception;

import com.justnothing.testmodule.command.functions.script_new.ast.SourceLocation;

/**
 * 脚本异常基类
 * <p>
 * 所有脚本相关异常的基类，包含错误位置和错误代码。
 * </p>
 * 
 * @author JustNothing1021
 * @since 1.0.0
 */
public abstract class ScriptException extends RuntimeException {
    
    private final int line;
    private final int column;
    private final ErrorCode errorCode;
    
    public ScriptException(String message, int line, int column, ErrorCode errorCode) {
        super(message);
        this.line = line;
        this.column = column;
        this.errorCode = errorCode;
    }
    
    public ScriptException(String message, int line, int column, ErrorCode errorCode, Throwable cause) {
        super(message, cause);
        this.line = line;
        this.column = column;
        this.errorCode = errorCode;
    }
    
    public ScriptException(String message, SourceLocation location, ErrorCode errorCode) {
        this(message, location.getLine(), location.getColumn(), errorCode);
    }
    
    public ScriptException(String message, SourceLocation location, ErrorCode errorCode, Throwable cause) {
        this(message, location.getLine(), location.getColumn(), errorCode, cause);
    }
    
    public int getLine() {
        return line;
    }
    
    public int getColumn() {
        return column;
    }
    
    public ErrorCode getErrorCode() {
        return errorCode;
    }
    
    @Override
    public String getMessage() {
        return String.format("%s (Line %d, Column %d) [%s]", 
                super.getMessage(), line, column, errorCode);
    }
}
