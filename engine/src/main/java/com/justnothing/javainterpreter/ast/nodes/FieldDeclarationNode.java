package com.justnothing.javainterpreter.ast.nodes;

import com.justnothing.javainterpreter.ast.ASTNode;
import com.justnothing.javainterpreter.ast.SourceLocation;
import com.justnothing.javainterpreter.ast.visitor.ASTVisitor;

import java.util.ArrayList;
import java.util.List;

public class FieldDeclarationNode extends ASTNode {
    
    private final String fieldName;
    private final ClassReferenceNode type;
    private final ASTNode initialValue;
    private final ClassModifiers modifiers;
    private final List<AnnotationNode> annotations;
    
    public FieldDeclarationNode(String fieldName, ClassReferenceNode type, ASTNode initialValue, 
                                 ClassModifiers modifiers, SourceLocation location) {
        super(location);
        this.fieldName = fieldName;
        this.type = type;
        this.initialValue = initialValue;
        this.modifiers = modifiers != null ? modifiers : new ClassModifiers();
        this.annotations = new ArrayList<>();
    }
    
    public String getFieldName() {
        return fieldName;
    }
    
    public ClassReferenceNode getType() {
        return type;
    }
    
    public ASTNode getInitialValue() {
        return initialValue;
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
        sb.append(indent(indent)).append("FieldDeclarationNode\n");
        
        if (!annotations.isEmpty()) {
            sb.append(indent(indent + 1)).append("annotations:\n");
            for (AnnotationNode annotation : annotations) {
                sb.append(annotation.formatString(indent + 2)).append("\n");
            }
        }
        
        sb.append(indent(indent + 1)).append("fieldName: " + fieldName + "\n");
        sb.append(indent(indent + 1)).append("type: " + type.getTypeName() + "\n");
        sb.append(indent(indent + 1)).append("modifiers: " + modifiers.toModifierString() + "\n");
        if (initialValue != null) {
            sb.append(indent(indent + 1)).append("initialValue:\n");
            sb.append(initialValue.formatString(indent + 2)).append("\n");
        }
        return sb.toString().stripTrailing();
    }
}