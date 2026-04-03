package com.justnothing.testmodule.command.functions.script.engine_new.ast.nodes;

import com.justnothing.testmodule.command.functions.script.engine_new.ast.ASTNode;
import com.justnothing.testmodule.command.functions.script.engine_new.ast.SourceLocation;
import com.justnothing.testmodule.command.functions.script.engine_new.ast.visitor.ASTVisitor;

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
        String sb = indent(indent) + "ArrayAssignmentNode\n" +
                indent(indent + 1) + "array:\n" +
                array.formatString(indent + 2) + "\n" +
                indent(indent + 1) + "index:\n" +
                index.formatString(indent + 2) + "\n" +
                indent(indent + 1) + "value:\n" +
                value.formatString(indent + 2) + "\n";
        return sb.stripTrailing();
    }
}
