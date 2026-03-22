package com.justnothing.testmodule.command.functions.script_new.ast.nodes;

import com.justnothing.testmodule.command.functions.script_new.ast.ASTNode;
import com.justnothing.testmodule.command.functions.script_new.ast.SourceLocation;
import com.justnothing.testmodule.command.functions.script_new.ast.visitor.ASTVisitor;

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
        StringBuilder sb = new StringBuilder();
        sb.append(indent(indent)).append("MethodReferenceNode\n");
        sb.append(indent(indent + 1)).append("methodName: ").append(methodName).append("\n");
        sb.append(indent(indent + 1)).append("target:\n");
        sb.append(target.formatString(indent + 2)).append("\n");
        return sb.toString().strip();
    }
}
