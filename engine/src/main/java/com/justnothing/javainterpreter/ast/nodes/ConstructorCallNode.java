package com.justnothing.javainterpreter.ast.nodes;

import com.justnothing.javainterpreter.ast.ASTNode;
import com.justnothing.javainterpreter.ast.GenericType;
import com.justnothing.javainterpreter.ast.SourceLocation;
import com.justnothing.javainterpreter.ast.visitor.ASTVisitor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ConstructorCallNode extends ASTNode {
    
    private final GenericType type;
    private final List<ASTNode> arguments;
    private final ASTNode arrayInitializer;
    
    public ConstructorCallNode(GenericType type, List<ASTNode> arguments, 
                             ASTNode arrayInitializer, SourceLocation location) {
        super(location);
        this.type = type;
        this.arguments = arguments != null ? 
            Collections.unmodifiableList(new ArrayList<>(arguments)) : 
            Collections.emptyList();
        this.arrayInitializer = arrayInitializer;
    }
    
    public GenericType getType() {
        return type;
    }
    
    public String getClassName() {
        return type.getTypeName();
    }
    
    public List<ASTNode> getArguments() {
        return arguments;
    }
    
    public ASTNode getArrayInitializer() {
        return arrayInitializer;
    }
    
    public boolean isArrayConstructor() {
        return type.isArray() || arrayInitializer != null;
    }
    
    @Override
    public String formatString(int indent) {
        StringBuilder sb = new StringBuilder();
        sb.append(indent(indent)).append("ConstructorCallNode\n");
        sb.append(indent(indent + 1)).append("className: ").append(type.getTypeName()).append("\n");
        if (type.getOriginalTypeName() != null) {
            sb.append(indent(indent + 1)).append("originalTypeName: ").append(type.getOriginalTypeName()).append("\n");
        }
        sb.append(indent(indent + 1)).append("arguments: ").append(arguments.size()).append("\n");
        if (arrayInitializer != null) {
            sb.append(indent(indent + 1)).append("arrayInitializer:\n");
            sb.append(arrayInitializer.formatString(indent + 2));
        }
        return sb.toString().stripTrailing();
    }
    
    @Override
    public <T> T accept(ASTVisitor<T> visitor) {
        return visitor.visit(this);
    }
}
