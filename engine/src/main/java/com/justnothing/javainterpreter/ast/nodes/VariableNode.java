package com.justnothing.javainterpreter.ast.nodes;

import com.justnothing.javainterpreter.ast.ASTNode;
import com.justnothing.javainterpreter.ast.SourceLocation;
import com.justnothing.javainterpreter.ast.visitor.ASTVisitor;

/**
 * 变量节点
 * <p>
 * 表示对变量的引用。
 * </p>
 * 
 * @author JustNothing1021
 * @since 1.0.0
 */
public class VariableNode extends ASTNode {
    
    private final String name;
    private boolean isFieldAccess = false;
    
    public VariableNode(String name, SourceLocation location) {
        super(location);
        this.name = name;
    }
    
    public String getName() {
        return name;
    }
    
    public boolean isFieldAccess() {
        return isFieldAccess;
    }
    
    public void setFieldAccess(boolean fieldAccess) {
        this.isFieldAccess = fieldAccess;
    }
    
    @Override
    public <T> T accept(ASTVisitor<T> visitor) {
        return visitor.visit(this);
    }
    
    @Override
    public String formatString(int indent) {
        return indent(indent) + "VariableNode: " + name + (isFieldAccess ? " (field)" : "");
    }
    
    public static class Builder extends ASTNode.Builder<Builder> {
        private String name;
        private boolean isFieldAccess;
        
        public Builder name(String name) {
            this.name = name;
            return this;
        }
        
        public Builder fieldAccess(boolean isFieldAccess) {
            this.isFieldAccess = isFieldAccess;
            return this;
        }
        
        @Override
        public VariableNode build() {
            VariableNode node = new VariableNode(name, location);
            node.setFieldAccess(isFieldAccess);
            return node;
        }
    }
}
