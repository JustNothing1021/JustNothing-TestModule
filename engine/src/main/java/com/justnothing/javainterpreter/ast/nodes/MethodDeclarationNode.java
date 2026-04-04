package com.justnothing.javainterpreter.ast.nodes;

import com.justnothing.javainterpreter.ast.ASTNode;
import com.justnothing.javainterpreter.ast.SourceLocation;
import com.justnothing.javainterpreter.ast.visitor.ASTVisitor;

import java.util.ArrayList;
import java.util.List;

public class MethodDeclarationNode extends ASTNode {
    
    private final String methodName;
    private final String returnTypeName;
    private final List<ParameterNode> parameters;
    private final ASTNode body;
    private final ClassModifiers modifiers;
    
    public MethodDeclarationNode(String methodName, String returnTypeName, 
                                  List<ParameterNode> parameters, ASTNode body,
                                  ClassModifiers modifiers, SourceLocation location) {
        super(location);
        this.methodName = methodName;
        this.returnTypeName = returnTypeName;
        this.parameters = parameters != null ? parameters : new ArrayList<>();
        this.body = body;
        this.modifiers = modifiers != null ? modifiers : new ClassModifiers();
    }
    
    public String getMethodName() {
        return methodName;
    }
    
    public String getReturnTypeName() {
        return returnTypeName;
    }
    
    public List<ParameterNode> getParameters() {
        return parameters;
    }
    
    public ASTNode getBody() {
        return body;
    }
    
    public ClassModifiers getModifiers() {
        return modifiers;
    }
    
    @Override
    public <T> T accept(ASTVisitor<T> visitor) {
        return visitor.visit(this);
    }
    
    @Override
    public String formatString(int indent) {
        StringBuilder sb = new StringBuilder();
        sb.append(indent(indent)).append("MethodDeclarationNode\n");
        sb.append(indent(indent + 1)).append("methodName: ").append(methodName).append("\n");
        sb.append(indent(indent + 1)).append("returnTypeName: ").append(returnTypeName).append("\n");
        sb.append(indent(indent + 1)).append("modifiers: ").append(modifiers.toModifierString()).append("\n");
        sb.append(indent(indent + 1)).append("parameters: ").append(parameters.size()).append("\n");
        for (int i = 0; i < parameters.size(); i++) {
            sb.append(indent(indent + 2)).append("param[").append(i).append("]:\n");
            sb.append(parameters.get(i).formatString(indent + 3)).append("\n");
        }
        if (body != null) {
            sb.append(indent(indent + 1)).append("body:\n");
            sb.append(body.formatString(indent + 2)).append("\n");
        }
        return sb.toString().stripTrailing();
    }
}
