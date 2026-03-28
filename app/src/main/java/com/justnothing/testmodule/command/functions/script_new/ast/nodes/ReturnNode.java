package com.justnothing.testmodule.command.functions.script_new.ast.nodes;

import com.justnothing.testmodule.command.functions.script_new.ast.ASTNode;
import com.justnothing.testmodule.command.functions.script_new.ast.SourceLocation;
import com.justnothing.testmodule.command.functions.script_new.ast.visitor.ASTVisitor;

public class ReturnNode extends ASTNode {
    private final ASTNode value;
    
    public ReturnNode(ASTNode value, SourceLocation location) {
        super(location);
        this.value = value;
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
        String ind = indent(indent);
        if (value != null) {
            return ind + "ReturnNode{\n" + value.formatString(indent + 1) + "\n" + ind + "}";
        }
        return ind + "ReturnNode{null}";
    }
    
    @Override
    public String toString() {
        return "ReturnNode{value=" + value + "}";
    }
}
