package com.justnothing.testmodule.command.functions.script.engine_new.ast.nodes;


import androidx.annotation.NonNull;

import com.justnothing.testmodule.command.functions.script.engine_new.ast.ASTNode;
import com.justnothing.testmodule.command.functions.script.engine_new.ast.SourceLocation;
import com.justnothing.testmodule.command.functions.script.engine_new.ast.visitor.ASTVisitor;

public class MethodReferenceNode extends ASTNode {
    private final ASTNode target;
    private final String methodName;
    
    public MethodReferenceNode(ASTNode target, String methodName, SourceLocation location) {
        super(location);
        this.target = target;
        this.methodName = methodName;
    }
    
    public ASTNode getTarget() {
        return target;
    }
    
    public String getMethodName() {
        return methodName;
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
        String sb = indent(indent) + "MethodReferenceNode\n" +
                indent(indent + 1) + "methodName: " + methodName + "\n" +
                indent(indent + 1) + "target:\n" +
                target.formatString(indent + 2) + "\n";
        return sb.stripTrailing();
    }

    @NonNull
    @Override
    public String toString() {
        return "MethodReferenceNode[" +
                "target=" + target +
                ", methodName='" + methodName + '\'' +
                "]";
           }
    
}
