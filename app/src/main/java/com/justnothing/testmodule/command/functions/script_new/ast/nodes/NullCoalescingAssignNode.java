package com.justnothing.testmodule.command.functions.script_new.ast.nodes;

import com.justnothing.testmodule.command.functions.script_new.ast.ASTNode;
import com.justnothing.testmodule.command.functions.script_new.ast.SourceLocation;
import com.justnothing.testmodule.command.functions.script_new.ast.visitor.ASTVisitor;

public class NullCoalescingAssignNode extends ASTNode {
    
    private final String variableName;
    private final ASTNode value;
    
    public NullCoalescingAssignNode(String variableName, ASTNode value, SourceLocation location) {
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
        StringBuilder sb = new StringBuilder();
        sb.append(indent(indent)).append("NullCoalescingAssignNode\n");
        sb.append(indent(indent + 1)).append("variable: ").append(variableName).append("\n");
        sb.append(indent(indent + 1)).append("value:\n");
        sb.append(value.formatString(indent + 2));
        return sb.toString().stripTrailing();
    }
}
