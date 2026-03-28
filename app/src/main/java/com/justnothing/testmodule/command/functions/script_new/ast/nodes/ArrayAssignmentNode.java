package com.justnothing.testmodule.command.functions.script_new.ast.nodes;

import com.justnothing.testmodule.command.functions.script_new.ast.ASTNode;
import com.justnothing.testmodule.command.functions.script_new.ast.SourceLocation;
import com.justnothing.testmodule.command.functions.script_new.ast.visitor.ASTVisitor;

public class ArrayAssignmentNode extends ASTNode {
    
    private final ASTNode array;
    private final ASTNode index;
    private final ASTNode value;
    
    public ArrayAssignmentNode(ASTNode array, ASTNode index, ASTNode value, SourceLocation location) {
        super(location);
        this.array = array;
        this.index = index;
        this.value = value;
    }
    
    public ASTNode getArray() {
        return array;
    }
    
    public ASTNode getIndex() {
        return index;
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
        sb.append(indent(indent)).append("ArrayAssignmentNode\n");
        sb.append(indent(indent + 1)).append("array:\n");
        sb.append(array.formatString(indent + 2)).append("\n");
        sb.append(indent(indent + 1)).append("index:\n");
        sb.append(index.formatString(indent + 2)).append("\n");
        sb.append(indent(indent + 1)).append("value:\n");
        sb.append(value.formatString(indent + 2)).append("\n");
        return sb.toString().stripTrailing();
    }
}
