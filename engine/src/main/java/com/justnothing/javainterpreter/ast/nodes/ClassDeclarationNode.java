package com.justnothing.javainterpreter.ast.nodes;

import com.justnothing.javainterpreter.ast.ASTNode;
import com.justnothing.javainterpreter.ast.SourceLocation;
import com.justnothing.javainterpreter.ast.visitor.ASTVisitor;

import java.util.ArrayList;
import java.util.List;

public class ClassDeclarationNode extends ASTNode {
    
    private final String className;
    private final ClassReferenceNode superClass;
    private final List<ClassReferenceNode> interfaces;
    private final List<FieldDeclarationNode> fields;
    private final List<MethodDeclarationNode> methods;
    private final List<ConstructorDeclarationNode> constructors;
    private final ClassModifiers modifiers;
    private final List<AnnotationNode> annotations;
    private boolean interfaceFlag;
    
    public ClassDeclarationNode(String className, ClassReferenceNode superClass, 
                                 List<ClassReferenceNode> interfaces, SourceLocation location) {
        super(location);
        this.className = className;
        this.superClass = superClass;
        this.interfaces = interfaces != null ? interfaces : new ArrayList<>();
        this.fields = new ArrayList<>();
        this.methods = new ArrayList<>();
        this.constructors = new ArrayList<>();
        this.modifiers = new ClassModifiers();
        this.annotations = new ArrayList<>();
        this.interfaceFlag = false;
    }
    
    public void setInterface(boolean isInterface) {
        this.interfaceFlag = isInterface;
    }
    
    public boolean isInterface() {
        return interfaceFlag;
    }
    
    public String getClassName() {
        return className;
    }
    
    public ClassReferenceNode getSuperClass() {
        return superClass;
    }
    
    public List<ClassReferenceNode> getInterfaces() {
        return interfaces;
    }
    
    public List<FieldDeclarationNode> getFields() {
        return fields;
    }
    
    public List<MethodDeclarationNode> getMethods() {
        return methods;
    }
    
    public List<ConstructorDeclarationNode> getConstructors() {
        return constructors;
    }
    
    public ClassModifiers getModifiers() {
        return modifiers;
    }
    
    public List<AnnotationNode> getAnnotations() {
        return annotations;
    }
    
    public void addField(FieldDeclarationNode field) {
        fields.add(field);
    }
    
    public void addMethod(MethodDeclarationNode method) {
        methods.add(method);
    }
    
    public void addConstructor(ConstructorDeclarationNode constructor) {
        constructors.add(constructor);
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
        sb.append(indent(indent)).append("ClassDeclarationNode\n");

        if (!annotations.isEmpty()) {
            sb.append(indent(indent + 1)).append("annotations:\n");
            for (AnnotationNode annotation : annotations) {
                sb.append(annotation.formatString(indent + 2)).append("\n");
            }
        }

        sb.append(indent(indent + 1)).append("className: ").append(className).append("\n");
        if (superClass != null) {
            sb.append(indent(indent + 1)).append("superClass: ").append(superClass.getTypeName()).append("\n");
        }
        if (!interfaces.isEmpty()) {
            sb.append(indent(indent + 1)).append("interfaces: ");
            for (int i = 0; i < interfaces.size(); i++) {
                if (i > 0) sb.append(", ");
                sb.append(interfaces.get(i).getTypeName());
            }
            sb.append("\n");
        }
        sb.append(indent(indent + 1)).append("modifiers: ").append(modifiers.toModifierString()).append("\n");

        sb.append(indent(indent + 1)).append("fields: ").append(fields.size()).append("\n");
        for (int i = 0; i < fields.size(); i++) {
            sb.append(indent(indent + 2)).append("field[").append(i).append("]:\n");
            sb.append(fields.get(i).formatString(indent + 3)).append("\n");
        }

        sb.append(indent(indent + 1)).append("constructors: ").append(constructors.size()).append("\n");
        for (int i = 0; i < constructors.size(); i++) {
            sb.append(indent(indent + 2)).append("constructor[").append(i).append("]:\n");
            sb.append(constructors.get(i).formatString(indent + 3)).append("\n");
        }

        sb.append(indent(indent + 1)).append("methods: ").append(methods.size()).append("\n");
        for (int i = 0; i < methods.size(); i++) {
            sb.append(indent(indent + 2)).append("method[").append(i).append("]:\n");
            sb.append(methods.get(i).formatString(indent + 3)).append("\n");
        }

        return sb.toString().stripTrailing();
    }
}