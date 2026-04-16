package com.justnothing.javainterpreter.evaluator;

import com.justnothing.javainterpreter.ast.ASTNode;
import com.justnothing.javainterpreter.ast.nodes.ArrayAccessNode;
import com.justnothing.javainterpreter.ast.nodes.ArrayLiteralNode;
import com.justnothing.javainterpreter.ast.nodes.AssignmentNode;
import com.justnothing.javainterpreter.ast.nodes.BinaryOpNode;
import com.justnothing.javainterpreter.ast.nodes.BlockNode;
import com.justnothing.javainterpreter.ast.nodes.CastNode;
import com.justnothing.javainterpreter.ast.nodes.ForEachNode;
import com.justnothing.javainterpreter.ast.nodes.ForNode;
import com.justnothing.javainterpreter.ast.nodes.IfNode;
import com.justnothing.javainterpreter.ast.nodes.InstanceofNode;
import com.justnothing.javainterpreter.ast.nodes.InterpolatedStringNode;
import com.justnothing.javainterpreter.ast.nodes.LiteralNode;
import com.justnothing.javainterpreter.ast.nodes.MapLiteralNode;
import com.justnothing.javainterpreter.ast.nodes.NewArrayNode;
import com.justnothing.javainterpreter.ast.nodes.TernaryNode;
import com.justnothing.javainterpreter.ast.nodes.UnaryOpNode;
import com.justnothing.javainterpreter.ast.nodes.VariableNode;
import com.justnothing.javainterpreter.ast.nodes.WhileNode;
import com.justnothing.javainterpreter.exception.EvaluationException;
import com.justnothing.javainterpreter.exception.ErrorCode;

import java.util.HashSet;
import java.util.Set;

public class RestrictedEvaluator {

    private final EvaluatorRegistry registry;
    private final Set<Class<? extends ASTNode>> allowedTypes;

    public RestrictedEvaluator() {
        this(getDefaultAllowedTypes());
    }

    public RestrictedEvaluator(Set<Class<? extends ASTNode>> allowedTypes) {
        this.allowedTypes = new HashSet<>(allowedTypes);
        this.registry = EvaluatorRegistry.createRestrictedRegistry(allowedTypes);
    }

    public static Set<Class<? extends ASTNode>> getDefaultAllowedTypes() {
        Set<Class<? extends ASTNode>> types = new HashSet<>();
        types.add(LiteralNode.class);
        types.add(VariableNode.class);
        types.add(BinaryOpNode.class);
        types.add(UnaryOpNode.class);
        types.add(TernaryNode.class);
        types.add(InstanceofNode.class);
        types.add(CastNode.class);
        types.add(ArrayLiteralNode.class);
        types.add(MapLiteralNode.class);
        types.add(InterpolatedStringNode.class);
        types.add(ArrayAccessNode.class);
        types.add(NewArrayNode.class);
        types.add(BlockNode.class);
        types.add(IfNode.class);
        types.add(WhileNode.class);
        types.add(ForNode.class);
        types.add(ForEachNode.class);
        types.add(AssignmentNode.class);
        return types;
    }

    public static Set<Class<? extends ASTNode>> getExpressionOnlyTypes() {
        Set<Class<? extends ASTNode>> types = new HashSet<>();
        types.add(LiteralNode.class);
        types.add(VariableNode.class);
        types.add(BinaryOpNode.class);
        types.add(UnaryOpNode.class);
        types.add(TernaryNode.class);
        types.add(InstanceofNode.class);
        types.add(CastNode.class);
        types.add(ArrayLiteralNode.class);
        types.add(MapLiteralNode.class);
        types.add(InterpolatedStringNode.class);
        types.add(ArrayAccessNode.class);
        types.add(NewArrayNode.class);
        return types;
    }

    public Object evaluate(ASTNode node, ExecutionContext context) throws EvaluationException {
        return registry.evaluate(node, context);
    }

    public boolean isTypeAllowed(Class<? extends ASTNode> nodeType) {
        return allowedTypes.contains(nodeType);
    }

    public Set<Class<? extends ASTNode>> getAllowedTypes() {
        return new HashSet<>(allowedTypes);
    }

    public void allowType(Class<? extends ASTNode> nodeType) {
        allowedTypes.add(nodeType);
        registry.enableNodeType(nodeType);
    }

    public void disallowType(Class<? extends ASTNode> nodeType) {
        allowedTypes.remove(nodeType);
        registry.disableNodeType(nodeType);
    }

    public static RestrictedEvaluator createExpressionOnly() {
        return new RestrictedEvaluator(getExpressionOnlyTypes());
    }

    public static RestrictedEvaluator createWithCustomTypes(Set<Class<? extends ASTNode>> allowedTypes) {
        return new RestrictedEvaluator(allowedTypes);
    }
}
