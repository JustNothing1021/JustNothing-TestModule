package com.justnothing.javainterpreter.ast.nodes;

import com.justnothing.javainterpreter.ast.ASTNode;
import com.justnothing.javainterpreter.ast.SourceLocation;
import com.justnothing.javainterpreter.ast.visitor.ASTVisitor;

import java.util.List;

public class ArrayLiteralNode extends ASTNode {
    private final List<ASTNode> elements;
    private final Class<?> expectedElementType;
    private final ASTNode arrayLength;
    
    public ArrayLiteralNode(List<ASTNode> elements, SourceLocation location) {
        this(elements, null, null, location);
    }
    
    public ArrayLiteralNode(List<ASTNode> elements, Class<?> expectedElementType, SourceLocation location) {
        this(elements, expectedElementType, null, location);
    }
    
    public ArrayLiteralNode(List<ASTNode> elements, Class<?> expectedElementType, ASTNode arrayLength, SourceLocation location) {
        super(location);
        this.elements = elements;
        this.expectedElementType = expectedElementType;
        this.arrayLength = arrayLength;
    }
    
    public List<ASTNode> getElements() {
        return elements;
    }
    
    public Class<?> getExpectedElementType() {
        return expectedElementType;
    }
    
    public ASTNode getArrayLength() {
        return arrayLength;
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
            sb.append(indent(indent + 1)).append("expectedType: " + expectedElementType.getSimpleName() + "\n");
        }
        if (arrayLength != null) {
            sb.append(indent(indent + 1)).append("arrayLength: " + arrayLength.formatString(indent + 2) + "\n");
        }
        sb.append(indent(indent + 1)).append("elements: " + elements.size() + "\n");
        for (int i = 0; i < elements.size(); i++) {
            sb.append(indent(indent + 1)).append("element[" + i + "]:\n");
            sb.append(elements.get(i).formatString(indent + 2) + "\n");
        }
        return sb.toString().stripTrailing();
    }
}
