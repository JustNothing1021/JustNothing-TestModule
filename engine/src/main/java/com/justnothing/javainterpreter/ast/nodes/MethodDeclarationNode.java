package com.justnothing.javainterpreter.ast.nodes;

import com.justnothing.javainterpreter.ast.ASTNode;
import com.justnothing.javainterpreter.ast.SourceLocation;
import com.justnothing.javainterpreter.ast.visitor.ASTVisitor;

import java.util.ArrayList;
import java.util.List;

public class MethodDeclarationNode extends ASTNode {
    
    private final String methodName;
    private final ClassReferenceNode returnType;
    private final List<ParameterNode> parameters;
    private final ASTNode body;
    private final ClassModifiers modifiers;
    private final List<AnnotationNode> annotations;
    
    public MethodDeclarationNode(String methodName, ClassReferenceNode returnType, 
                                  List<ParameterNode> parameters, ASTNode body,
                                  ClassModifiers modifiers, SourceLocation location) {
        super(location);
        this.methodName = methodName;
        this.returnType = returnType;
        this.parameters = parameters != null ? parameters : new ArrayList<>();
        this.body = body;
        this.modifiers = modifiers != null ? modifiers : new ClassModifiers();
        this.annotations = new ArrayList<>();
    }
    
    public String getMethodName() {
        return methodName;
    }
    
    public ClassReferenceNode getReturnType() {
        return returnType;
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
    
    public List<AnnotationNode> getAnnotations() {
        return annotations;
    }
    
    public void addAnnotation(AnnotationNode annotation) {
        annotations.add(annotation);
    }
    
    @Override
    public <T> T accept(ASTVisitor<T> visitor) {
        return visitor.visit(this);
    }
    
    @Override
    public String formatString(int indent) {
        StringBuilder sb = new StringBuilder();
        sb.append(indent(indent)).append("MethodDeclarationNode\n");
        
        if (!annotations.isEmpty()) {
            sb.append(indent(indent + 1)).append("annotations:\n");
            for (AnnotationNode annotation : annotations) {
                sb.append(annotation.formatString(indent + 2)).append("\n");
            }
        }
        
        sb.append(indent(indent + 1)).append("methodName: " + methodName + "\n");
        sb.append(indent(indent + 1)).append("returnType: " + (returnType != null ? returnType.getTypeName() : "void") + "\n");
        sb.append(indent(indent + 1)).append("modifiers: " + modifiers.toModifierString() + "\n");
        sb.append(indent(indent + 1)).append("parameters: " + parameters.size() + "\n");
        for (int i = 0; i < parameters.size(); i++) {
            sb.append(indent(indent + 2)).append("param[" + i + "]:\n");
            sb.append(parameters.get(i).formatString(indent + 3)).append("\n");
        }
        if (body != null) {
            sb.append(indent(indent + 1)).append("body:\n");
            sb.append(body.formatString(indent + 2)).append("\n");
        }
        return sb.toString().stripTrailing();
    }
}