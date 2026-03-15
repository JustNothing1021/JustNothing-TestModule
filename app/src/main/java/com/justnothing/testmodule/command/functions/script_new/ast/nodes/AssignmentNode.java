package com.justnothing.testmodule.command.functions.script_new.ast.nodes;

import com.justnothing.testmodule.command.functions.script_new.ast.ASTNode;
import com.justnothing.testmodule.command.functions.script_new.ast.SourceLocation;
import com.justnothing.testmodule.command.functions.script_new.ast.visitor.ASTVisitor;

/**
 * 赋值节点
 * <p>
 * 表示变量赋值操作。
 * </p>
 * 
 * @author JustNothing1021
 * @since 1.0.0
 */
public class AssignmentNode extends ASTNode {
    
    private final String variableName;
    private final ASTNode value;
    private final boolean isDeclaration;
    private final Class<?> declaredType;
    
    public AssignmentNode(String variableName, ASTNode value, boolean isDeclaration, 
                        Class<?> declaredType, SourceLocation location) {
        super(location);
        this.variableName = variableName;
        this.value = value;
        this.isDeclaration = isDeclaration;
        this.declaredType = declaredType;
    }
    
    public String getVariableName() {
        return variableName;
    }
    
    public ASTNode getValue() {
        return value;
    }
    
    public boolean isDeclaration() {
        return isDeclaration;
    }
    
    public Class<?> getDeclaredType() {
        return declaredType;
    }
    
    @Override
    public <T> T accept(ASTVisitor<T> visitor) {
        return visitor.visit(this);
    }
    
    public static class Builder extends ASTNode.Builder<Builder> {
        private String variableName;
        private ASTNode value;
        private boolean isDeclaration;
        private Class<?> declaredType;
        
        public Builder variableName(String variableName) {
            this.variableName = variableName;
            return this;
        }
        
        public Builder value(ASTNode value) {
            this.value = value;
            return this;
        }
        
        public Builder isDeclaration(boolean isDeclaration) {
            this.isDeclaration = isDeclaration;
            return this;
        }
        
        public Builder declaredType(Class<?> declaredType) {
            this.declaredType = declaredType;
            return this;
        }
        
        @Override
        public AssignmentNode build() {
            return new AssignmentNode(variableName, value, isDeclaration, declaredType, location);
        }
    }
}
