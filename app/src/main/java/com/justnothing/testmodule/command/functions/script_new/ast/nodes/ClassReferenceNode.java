package com.justnothing.testmodule.command.functions.script_new.ast.nodes;

import androidx.annotation.NonNull;

import com.justnothing.testmodule.command.functions.script_new.ast.ASTNode;
import com.justnothing.testmodule.command.functions.script_new.ast.GenericType;
import com.justnothing.testmodule.command.functions.script_new.ast.SourceLocation;
import com.justnothing.testmodule.command.functions.script_new.ast.visitor.ASTVisitor;

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
    
    @NonNull
    @Override
    public String toString() {
        return "ClassReferenceNode(" + type.getTypeName() + ")";
    }
}
