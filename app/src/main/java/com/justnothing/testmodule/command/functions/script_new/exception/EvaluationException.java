package com.justnothing.testmodule.command.functions.script_new.exception;

import com.justnothing.testmodule.command.functions.script_new.ast.SourceLocation;

/**
 * 求值异常
 * <p>
 * 在求值AST节点时抛出的异常。
 * </p>
 * 
 * @author JustNothing1021
 * @since 1.0.0
 */
public class EvaluationException extends ScriptException {
    
    public EvaluationException(String message, int line, int column, ErrorCode errorCode) {
        super(message, line, column, errorCode);
    }
    
    public EvaluationException(String message, int line, int column, ErrorCode errorCode, Throwable cause) {
        super(message, line, column, errorCode, cause);
    }
    
    public EvaluationException(String message, SourceLocation location, ErrorCode errorCode) {
        super(message, location, errorCode);
    }
    
    public EvaluationException(String message, SourceLocation location, ErrorCode errorCode, Throwable cause) {
        super(message, location, errorCode, cause);
    }
}
