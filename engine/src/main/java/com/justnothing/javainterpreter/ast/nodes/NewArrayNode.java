package com.justnothing.javainterpreter.ast.nodes;

import com.justnothing.javainterpreter.ast.ASTNode;
import com.justnothing.javainterpreter.ast.SourceLocation;
import com.justnothing.javainterpreter.ast.visitor.ASTVisitor;

public class NewArrayNode extends ASTNode {
    private final Class<?> elementType;
    private final ASTNode size;
    
    public NewArrayNode(Class<?> elementType, ASTNode size, SourceLocation location) {
        super(location);
        this.elementType = elementType;
        this.size = size;
    }
    
    public Class<?> getElementType() {
        return elementType;
    }
    
    public ASTNode getSize() {
        return size;
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
        String sb = indent(indent) + "NewArrayNode\n" +
                indent(indent + 1) + "elementType: " + elementType.getSimpleName() + "\n" +
                indent(indent + 1) + "size:\n" +
                size.formatString(indent + 2) + "\n";
        return sb.stripTrailing();
    }
}
