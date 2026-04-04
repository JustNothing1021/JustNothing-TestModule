package com.justnothing.javainterpreter.ast.nodes;

import com.justnothing.javainterpreter.ast.ASTNode;
import com.justnothing.javainterpreter.ast.SourceLocation;
import com.justnothing.javainterpreter.ast.visitor.ASTVisitor;

public class SafeFieldAccessNode extends ASTNode {
    
    private final ASTNode target;
    private final String fieldName;
    
    public SafeFieldAccessNode(ASTNode target, String fieldName, SourceLocation location) {
        super(location);
        this.target = target;
        this.fieldName = fieldName;
    }
    
    public ASTNode getTarget() {
        return target;
    }
    
    public String getFieldName() {
        return fieldName;
    }
    
    @Override
    public <T> T accept(ASTVisitor<T> visitor) {
        return visitor.visit(this);
    }
    
    @Override
    public String formatString(int indent) {
        String sb = indent(indent) + "SafeFieldAccessNode\n" +
                indent(indent + 1) + "target:\n" +
                target.formatString(indent + 2) +
                "\n" + indent(indent + 1) + "field: " + fieldName;
        return sb.stripTrailing();
    }
}
