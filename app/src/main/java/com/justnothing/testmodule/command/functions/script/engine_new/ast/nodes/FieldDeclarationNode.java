package com.justnothing.testmodule.command.functions.script.engine_new.ast.nodes;

import com.justnothing.testmodule.command.functions.script.engine_new.ast.ASTNode;
import com.justnothing.testmodule.command.functions.script.engine_new.ast.SourceLocation;
import com.justnothing.testmodule.command.functions.script.engine_new.ast.visitor.ASTVisitor;

public class FieldDeclarationNode extends ASTNode {
    
    private final String fieldName;
    private final String typeName;
    private final ASTNode initialValue;
    private final ClassModifiers modifiers;
    
    public FieldDeclarationNode(String fieldName, String typeName, ASTNode initialValue, 
                                 ClassModifiers modifiers, SourceLocation location) {
        super(location);
        this.fieldName = fieldName;
        this.typeName = typeName;
        this.initialValue = initialValue;
        this.modifiers = modifiers != null ? modifiers : new ClassModifiers();
    }
    
    public String getFieldName() {
        return fieldName;
    }
    
    public String getTypeName() {
        return typeName;
    }
    
    public ASTNode getInitialValue() {
        return initialValue;
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
        sb.append(indent(indent)).append("FieldDeclarationNode\n");
        sb.append(indent(indent + 1)).append("fieldName: ").append(fieldName).append("\n");
        sb.append(indent(indent + 1)).append("typeName: ").append(typeName).append("\n");
        sb.append(indent(indent + 1)).append("modifiers: ").append(modifiers.toModifierString()).append("\n");
        if (initialValue != null) {
            sb.append(indent(indent + 1)).append("initialValue:\n");
            sb.append(initialValue.formatString(indent + 2)).append("\n");
        }
        return sb.toString().stripTrailing();
    }
}
