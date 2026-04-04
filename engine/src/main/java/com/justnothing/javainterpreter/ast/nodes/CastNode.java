package com.justnothing.javainterpreter.ast.nodes;

import com.justnothing.javainterpreter.ast.ASTNode;
import com.justnothing.javainterpreter.ast.SourceLocation;
import com.justnothing.javainterpreter.ast.visitor.ASTVisitor;

public class CastNode extends ASTNode {
    private final Class<?> targetType;
    private final ASTNode expression;
    
    public CastNode(Class<?> targetType, ASTNode expression, SourceLocation location) {
        super(location);
        this.targetType = targetType;
        this.expression = expression;
    }
    
    public Class<?> getTargetType() {
        return targetType;
    }
    
    public ASTNode getExpression() {
        return expression;
    }
    
    @Override
    public <T> T accept(ASTVisitor<T> visitor) {
        return visitor.visit(this);
    }
    
    @Override
    public String formatString() {
        return formatString(0);
    }
    
    @Override
    public String formatString(int indent) {
        String sb = indent(indent) + "CastNode\n" +
                indent(indent + 1) + "targetType: " + targetType.getSimpleName() + "\n" +
                indent(indent + 1) + "expression:\n" +
                expression.formatString(indent + 2) + "\n";
        return sb.stripTrailing();
    }
}
