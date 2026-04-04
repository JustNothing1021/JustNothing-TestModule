package com.justnothing.javainterpreter.ast.nodes;

import com.justnothing.javainterpreter.ast.ASTNode;
import com.justnothing.javainterpreter.ast.SourceLocation;
import com.justnothing.javainterpreter.ast.visitor.ASTVisitor;

public class ArrayAccessNode extends ASTNode {
    private final ASTNode array;
    private final ASTNode index;
    
    public ArrayAccessNode(ASTNode array, ASTNode index, SourceLocation location) {
        super(location);
        this.array = array;
        this.index = index;
    }
    
    public ASTNode getArray() {
        return array;
    }
    
    public ASTNode getIndex() {
        return index;
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
        String sb = indent(indent) + "ArrayAccessNode\n" +
                indent(indent + 1) + "array:\n" +
                array.formatString(indent + 2) + "\n" +
                indent(indent + 1) + "index:\n" +
                index.formatString(indent + 2) + "\n";
        return sb.stripTrailing();
    }
}
