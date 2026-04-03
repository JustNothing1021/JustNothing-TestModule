package com.justnothing.testmodule.command.functions.script.engine_new.exception;

import com.justnothing.testmodule.command.functions.script.engine_new.ast.SourceLocation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

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
    private List<String> scriptCallStack = new ArrayList<>();
    
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
        this(message, location != null ? location.getLine() : -1, location != null ? location.getColumn() : -1, errorCode);
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
    
    public void setScriptCallStack(List<String> callStack) {
        this.scriptCallStack = callStack != null ? new ArrayList<>(callStack) : new ArrayList<>();
    }
    
    public List<String> getScriptCallStack() {
        return Collections.unmodifiableList(scriptCallStack);
    }
    
    public boolean hasScriptCallStack() {
        return !scriptCallStack.isEmpty();
    }
    
    public String formatScriptCallStack() {
        if (scriptCallStack.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("脚本调用栈:\n");
        for (int i = scriptCallStack.size() - 1; i >= 0; i--) {
            sb.append("  at ").append(scriptCallStack.get(i)).append("\n");
        }
        return sb.toString();
    }
    
    @Override
    public String getMessage() {
        return String.format(
                Locale.getDefault(),
                "%s (Line %d, Column %d) [%s]",
                super.getMessage(), line, column, errorCode);
    }
}
