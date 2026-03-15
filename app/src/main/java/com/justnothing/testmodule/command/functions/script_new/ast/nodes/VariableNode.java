package com.justnothing.testmodule.command.functions.script_new.ast.nodes;

import com.justnothing.testmodule.command.functions.script_new.ast.ASTNode;
import com.justnothing.testmodule.command.functions.script_new.ast.SourceLocation;
import com.justnothing.testmodule.command.functions.script_new.ast.visitor.ASTVisitor;

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
    
    public VariableNode(String name, SourceLocation location) {
        super(location);
        this.name = name;
    }
    
    public String getName() {
        return name;
    }
    
    @Override
    public <T> T accept(ASTVisitor<T> visitor) {
        return visitor.visit(this);
    }
    
    public static class Builder extends ASTNode.Builder<Builder> {
        private String name;
        
        public Builder name(String name) {
            this.name = name;
            return this;
        }
        
        @Override
        public VariableNode build() {
            return new VariableNode(name, location);
        }
    }
}
