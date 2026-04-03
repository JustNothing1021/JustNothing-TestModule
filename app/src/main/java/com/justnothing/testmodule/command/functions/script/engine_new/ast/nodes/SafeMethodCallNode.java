package com.justnothing.testmodule.command.functions.script.engine_new.ast.nodes;

import java.util.ArrayList;
import java.util.List;

import com.justnothing.testmodule.command.functions.script.engine_new.ast.ASTNode;
import com.justnothing.testmodule.command.functions.script.engine_new.ast.SourceLocation;
import com.justnothing.testmodule.command.functions.script.engine_new.ast.visitor.ASTVisitor;

public class SafeMethodCallNode extends ASTNode {
    
    private final ASTNode target;
    private final String methodName;
    private final List<ASTNode> arguments;
    
    public SafeMethodCallNode(ASTNode target, String methodName, List<ASTNode> arguments, SourceLocation location) {
        super(location);
        this.target = target;
        this.methodName = methodName;
        this.arguments = new ArrayList<>(arguments);
    }
    
    public ASTNode getTarget() {
        return target;
    }
    
    public String getMethodName() {
        return methodName;
    }
    
    public List<ASTNode> getArguments() {
        return arguments;
    }
    
    @Override
    public <T> T accept(ASTVisitor<T> visitor) {
        return visitor.visit(this);
    }
    
    @Override
    public String formatString(int indent) {
        StringBuilder sb = new StringBuilder();
        sb.append(indent(indent)).append("SafeMethodCallNode\n");
        sb.append(indent(indent + 1)).append("target:\n");
        sb.append(target.formatString(indent + 2));
        sb.append("\n").append(indent(indent + 1)).append("method: ").append(methodName);
        sb.append("\n").append(indent(indent + 1)).append("arguments: ").append(arguments.size());
        for (int i = 0; i < arguments.size(); i++) {
            sb.append("\n").append(indent(indent + 2)).append("arg[").append(i).append("]:\n");
            sb.append(arguments.get(i).formatString(indent + 3));
        }
        return sb.toString().stripTrailing();
    }
}
