package com.justnothing.testmodule.command.functions.script_new.ast.nodes;

import com.justnothing.testmodule.command.functions.script_new.ast.ASTNode;
import com.justnothing.testmodule.command.functions.script_new.ast.SourceLocation;
import com.justnothing.testmodule.command.functions.script_new.ast.visitor.ASTVisitor;

public class FieldAccessNode extends ASTNode {
    private final ASTNode target;
    private final String fieldName;
    
    public FieldAccessNode(ASTNode target, String fieldName, SourceLocation location) {
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
    public String formatString() {
        return formatString(0);
    }
    
    @Override
    public String formatString(int indent) {
        StringBuilder sb = new StringBuilder();
        sb.append(indent(indent)).append("FieldAccessNode\n");
        sb.append(indent(indent + 1)).append("fieldName: ").append(fieldName).append("\n");
        sb.append(indent(indent + 1)).append("target:\n");
        sb.append(target.formatString(indent + 2)).append("\n");
        return sb.toString().strip();
    }
}
