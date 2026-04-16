package com.justnothing.javainterpreter.ast.nodes;

import com.justnothing.javainterpreter.ast.ASTNode;
import com.justnothing.javainterpreter.ast.SourceLocation;
import com.justnothing.javainterpreter.ast.visitor.ASTVisitor;

import java.util.ArrayList;
import java.util.List;

public class ConstructorDeclarationNode extends ASTNode {
    
    private final String className;
    private final List<ParameterNode> parameters;
    private final ASTNode body;
    private final ClassModifiers modifiers;
    private final List<AnnotationNode> annotations;
    
    public ConstructorDeclarationNode(String className, List<ParameterNode> parameters, 
                                       ASTNode body, ClassModifiers modifiers, SourceLocation location) {
        super(location);
        this.className = className;
        this.parameters = parameters != null ? parameters : new ArrayList<>();
        this.body = body;
        this.modifiers = modifiers != null ? modifiers : new ClassModifiers();
        this.annotations = new ArrayList<>();
    }
    
    public String getClassName() {
        return className;
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
        sb.append(indent(indent)).append("ConstructorDeclarationNode\n");
        
        if (!annotations.isEmpty()) {
            sb.append(indent(indent + 1)).append("annotations:\n");
            for (AnnotationNode annotation : annotations) {
                sb.append(annotation.formatString(indent + 2)).append("\n");
            }
        }
        
        sb.append(indent(indent + 1)).append("className: ").append(className).append("\n");
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
