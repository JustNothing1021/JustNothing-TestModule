package com.justnothing.javainterpreter.ast.nodes;

import com.justnothing.javainterpreter.ast.ASTNode;
import com.justnothing.javainterpreter.ast.SourceLocation;
import com.justnothing.javainterpreter.ast.visitor.ASTVisitor;

public class ConditionalAssignNode extends ASTNode {
    
    private final String variableName;
    private final ASTNode value;
    
    public ConditionalAssignNode(String variableName, ASTNode value, SourceLocation location) {
        super(location);
        this.variableName = variableName;
        this.value = value;
    }
    
    public String getVariableName() {
        return variableName;
    }
    
    public ASTNode getValue() {
        return value;
    }
    
    @Override
    public <T> T accept(ASTVisitor<T> visitor) {
        return visitor.visit(this);
    }
    
    @Override
    public String formatString(int indent) {
        String sb = indent(indent) + "ConditionalAssignNode\n" +
                indent(indent + 1) + "variable: " + variableName + "\n" +
                indent(indent + 1) + "value:\n" +
                value.formatString(indent + 2);
        return sb.stripTrailing();
    }
}
