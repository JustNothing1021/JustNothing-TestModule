package com.justnothing.javainterpreter.evaluator;

import com.justnothing.javainterpreter.ast.ASTNode;

import com.justnothing.javainterpreter.exception.EvaluationException;
import com.justnothing.javainterpreter.exception.ErrorCode;

@FunctionalInterface
public interface NodeEvaluator<T extends ASTNode> {
    
    Object evaluate(T node, ExecutionContext context) throws EvaluationException;
}
