package com.justnothing.testmodule.command.functions.script_new.ast.nodes;

import com.justnothing.testmodule.command.functions.script_new.ast.ASTNode;
import com.justnothing.testmodule.command.functions.script_new.ast.SourceLocation;
import com.justnothing.testmodule.command.functions.script_new.ast.visitor.ASTVisitor;

public class ClassReferenceNode extends ASTNode {
    private final String className;
    
    public ClassReferenceNode(String className, SourceLocation location) {
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
        return indent(indent) + "ClassReferenceNode: " + className;
    }
    
    @Override
    public String toString() {
        return "ClassReferenceNode(" + className + ")";
    }
}
