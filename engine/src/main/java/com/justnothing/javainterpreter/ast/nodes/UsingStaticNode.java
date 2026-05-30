package com.justnothing.javainterpreter.ast.nodes;

import com.justnothing.javainterpreter.ast.ASTNode;
import com.justnothing.javainterpreter.ast.SourceLocation;
import com.justnothing.javainterpreter.ast.visitor.ASTVisitor;

public class UsingStaticNode extends ASTNode {
    
    private final String className;
    
    public UsingStaticNode(String className, SourceLocation location) {
        super(location);
        this.className = className;
    }
    
    public String getClassName() {
        return className;
    }
    
    @Override
    public <T> T accept(ASTVisitor<T> visitor) {
        return visitor.visit(this);
    }
    
    @Override
    public String formatString(int indent) {
        return indent(indent) + "UsingStaticNode\n" +
                indent(indent + 1) + "className: " + className + "\n";
    }
}
