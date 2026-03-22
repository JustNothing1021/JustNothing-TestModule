package com.justnothing.testmodule.command.functions.script_new.ast.nodes;

import com.justnothing.testmodule.command.functions.script_new.ast.ASTNode;
import com.justnothing.testmodule.command.functions.script_new.ast.SourceLocation;
import com.justnothing.testmodule.command.functions.script_new.ast.visitor.ASTVisitor;

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
        StringBuilder sb = new StringBuilder();
        sb.append(indent(indent)).append("NewArrayNode\n");
        sb.append(indent(indent + 1)).append("elementType: ").append(elementType.getSimpleName()).append("\n");
        sb.append(indent(indent + 1)).append("size:\n");
        sb.append(size.formatString(indent + 2)).append("\n");
        return sb.toString().strip();
    }
}
