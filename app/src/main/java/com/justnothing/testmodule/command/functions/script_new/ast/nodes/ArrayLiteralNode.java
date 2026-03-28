package com.justnothing.testmodule.command.functions.script_new.ast.nodes;

import com.justnothing.testmodule.command.functions.script_new.ast.ASTNode;
import com.justnothing.testmodule.command.functions.script_new.ast.SourceLocation;
import com.justnothing.testmodule.command.functions.script_new.ast.visitor.ASTVisitor;

import java.util.List;

public class ArrayLiteralNode extends ASTNode {
    private final List<ASTNode> elements;
    
    public ArrayLiteralNode(List<ASTNode> elements, SourceLocation location) {
        super(location);
        this.elements = elements;
    }
    
    public List<ASTNode> getElements() {
        return elements;
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
        sb.append(indent(indent + 1)).append("elements: ").append(elements.size()).append("\n");
        for (int i = 0; i < elements.size(); i++) {
            sb.append(indent(indent + 1)).append("element[").append(i).append("]:\n");
            sb.append(elements.get(i).formatString(indent + 2)).append("\n");
        }
        return sb.toString().stripTrailing();
    }
}
