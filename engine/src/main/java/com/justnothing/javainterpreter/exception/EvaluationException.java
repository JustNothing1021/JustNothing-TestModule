package com.justnothing.javainterpreter.exception;

import com.justnothing.javainterpreter.ast.ASTNode;
import org.jetbrains.annotations.Nullable;

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
    private final ASTNode node;

    public EvaluationException(String message, ErrorCode errorCode, ASTNode node) {
        super(message, node.getLocation(), errorCode);
        this.node = node;
    }


    public EvaluationException(String message, ErrorCode errorCode, Throwable cause, ASTNode node) {
        super(message, node.getLocation(), errorCode, cause);
        this.node = node;
    }

    public @Nullable ASTNode getNode() {
        return node;
    }
}
