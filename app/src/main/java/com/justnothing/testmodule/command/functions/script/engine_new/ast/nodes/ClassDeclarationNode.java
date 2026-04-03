package com.justnothing.testmodule.command.functions.script.engine_new.ast.nodes;

import com.justnothing.testmodule.command.functions.script.engine_new.ast.ASTNode;
import com.justnothing.testmodule.command.functions.script.engine_new.ast.SourceLocation;
import com.justnothing.testmodule.command.functions.script.engine_new.ast.visitor.ASTVisitor;

import java.util.ArrayList;
import java.util.List;

public class ClassDeclarationNode extends ASTNode {
    
    private final String className;
    private final String superClassName;
    private final List<String> interfaceNames;
    private final List<FieldDeclarationNode> fields;
    private final List<MethodDeclarationNode> methods;
    private final List<ConstructorDeclarationNode> constructors;
    private final ClassModifiers modifiers;
    
    public ClassDeclarationNode(String className, String superClassName, 
                                 List<String> interfaceNames, SourceLocation location) {
        super(location);
        this.className = className;
        this.superClassName = superClassName;
        this.interfaceNames = interfaceNames != null ? interfaceNames : new ArrayList<>();
        this.fields = new ArrayList<>();
        this.methods = new ArrayList<>();
        this.constructors = new ArrayList<>();
        this.modifiers = new ClassModifiers();
    }
    
    public String getClassName() {
        return className;
    }
    
    public String getSuperClassName() {
        return superClassName;
    }
    
    public List<String> getInterfaceNames() {
        return interfaceNames;
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
    
    public void addField(FieldDeclarationNode field) {
        fields.add(field);
    }
    
    public void addMethod(MethodDeclarationNode method) {
        methods.add(method);
    }
    
    public void addConstructor(ConstructorDeclarationNode constructor) {
        constructors.add(constructor);
    }
    
    @Override
    public <T> T accept(ASTVisitor<T> visitor) {
        return visitor.visit(this);
    }
    
    @Override
    public String formatString(int indent) {
        StringBuilder sb = new StringBuilder();
        sb.append(indent(indent)).append("ClassDeclarationNode\n");
        sb.append(indent(indent + 1)).append("className: ").append(className).append("\n");
        if (superClassName != null) {
            sb.append(indent(indent + 1)).append("superClassName: ").append(superClassName).append("\n");
        }
        if (!interfaceNames.isEmpty()) {
            sb.append(indent(indent + 1)).append("interfaces: ").append(String.join(", ", interfaceNames)).append("\n");
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
