package com.justnothing.javainterpreter.ast.nodes;


import com.justnothing.javainterpreter.ast.ASTNode;
import com.justnothing.javainterpreter.ast.GenericType;
import com.justnothing.javainterpreter.ast.SourceLocation;
import com.justnothing.javainterpreter.ast.visitor.ASTVisitor;

public class ClassReferenceNode extends ASTNode {
    private final GenericType type;
    
    public ClassReferenceNode(GenericType type, SourceLocation location) {
        super(location);
        this.type = type;
    }
    
    public GenericType getType() {
        return type;
    }
    
    public String getClassName() {
        return type.getTypeName();
    }
    
    @Override
    public <T> T accept(ASTVisitor<T> visitor) {
        return visitor.visit(this);
    }
    
    @Override
    public String formatString(int indent) {
        return indent(indent) + "ClassReferenceNode: " + type.getTypeName();
    }
    
    @Override
    public String toString() {
        return "ClassReferenceNode(" + type.getTypeName() + ")";
    }
}
