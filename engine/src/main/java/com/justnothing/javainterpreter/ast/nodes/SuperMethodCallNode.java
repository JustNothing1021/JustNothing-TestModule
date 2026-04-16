package com.justnothing.javainterpreter.ast.nodes;

import com.justnothing.javainterpreter.ast.ASTNode;
import com.justnothing.javainterpreter.ast.SourceLocation;
import com.justnothing.javainterpreter.ast.visitor.ASTVisitor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SuperMethodCallNode extends ASTNode {
    
    private final String methodName;
    private final List<ASTNode> arguments;
    
    public SuperMethodCallNode(String methodName, List<ASTNode> arguments, SourceLocation location) {
        super(location);
        this.methodName = methodName;
        this.arguments = arguments != null ?
            Collections.unmodifiableList(new ArrayList<>(arguments)) :
            Collections.emptyList();
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
        sb.append(indent(indent)).append("SuperMethodCallNode\n");
        sb.append(indent(indent + 1)).append("methodName: super.").append(methodName).append("\n");
        sb.append(indent(indent + 1)).append("arguments: ").append(arguments.size()).append("\n");
        for (int i = 0; i < arguments.size(); i++) {
            sb.append(indent(indent + 2)).append("arg[").append(i).append("]:\n");
            sb.append(arguments.get(i).formatString(indent + 3)).append("\n");
        }
        return sb.toString().stripTrailing();
    }
}
