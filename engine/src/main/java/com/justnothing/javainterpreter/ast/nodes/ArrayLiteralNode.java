package com.justnothing.javainterpreter.ast.nodes;

import com.justnothing.javainterpreter.ast.ASTNode;
import com.justnothing.javainterpreter.ast.SourceLocation;
import com.justnothing.javainterpreter.ast.visitor.ASTVisitor;

import java.util.List;

public class ArrayLiteralNode extends ASTNode {
    private final List<ASTNode> elements;
    private final Class<?> expectedElementType;
    
    public ArrayLiteralNode(List<ASTNode> elements, SourceLocation location) {
        this(elements, null, location);
    }
    
    public ArrayLiteralNode(List<ASTNode> elements, Class<?> expectedElementType, SourceLocation location) {
        super(location);
        this.elements = elements;
        this.expectedElementType = expectedElementType;
    }
    
    public List<ASTNode> getElements() {
        return elements;
    }
    
    public Class<?> getExpectedElementType() {
        return expectedElementType;
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
        sb.append(indent(indent)).append("ArrayLiteralNode\n");
        if (expectedElementType != null) {
            sb.append(indent(indent + 1)).append("expectedType: ").append(expectedElementType.getSimpleName()).append("\n");
        }
        sb.append(indent(indent + 1)).append("elements: ").append(elements.size()).append("\n");
        for (int i = 0; i < elements.size(); i++) {
            sb.append(indent(indent + 1)).append("element[").append(i).append("]:\n");
            sb.append(elements.get(i).formatString(indent + 2)).append("\n");
        }
        return sb.toString().stripTrailing();
    }
}
