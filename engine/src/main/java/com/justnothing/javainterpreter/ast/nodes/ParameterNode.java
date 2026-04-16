package com.justnothing.javainterpreter.ast.nodes;

import com.justnothing.javainterpreter.ast.ASTNode;
import com.justnothing.javainterpreter.ast.SourceLocation;
import com.justnothing.javainterpreter.ast.visitor.ASTVisitor;

public class ParameterNode extends ASTNode {
    
    private final String parameterName;
    private final ClassReferenceNode type;
    
    public ParameterNode(String parameterName, ClassReferenceNode type, SourceLocation location) {
        super(location);
        this.parameterName = parameterName;
        this.type = type;
    }
    
    public String getParameterName() {
        return parameterName;
    }
    
    public ClassReferenceNode getType() {
        return type;
    }
    
    @Override
    public <T> T accept(ASTVisitor<T> visitor) {
        return visitor.visit(this);
    }
    
    @Override
    public String formatString(int indent) {
        StringBuilder sb = new StringBuilder();
        sb.append(indent(indent)).append("ParameterNode\n");
        sb.append(indent(indent + 1)).append("parameterName: ").append(parameterName).append("\n");
        sb.append(indent(indent + 1)).append("type: ").append(type.getTypeName()).append("\n");
        return sb.toString().stripTrailing();
    }
}