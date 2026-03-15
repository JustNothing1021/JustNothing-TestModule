package com.justnothing.testmodule.command.functions.script_new.ast.nodes;

import com.justnothing.testmodule.command.functions.script_new.ast.ASTNode;
import com.justnothing.testmodule.command.functions.script_new.ast.SourceLocation;
import com.justnothing.testmodule.command.functions.script_new.ast.visitor.ASTVisitor;

/**
 * 字面量节点
 * <p>
 * 表示各种字面量值，如数字、字符串、布尔值、null等。
 * </p>
 * 
 * @author JustNothing1021
 * @since 1.0.0
 */
public class LiteralNode extends ASTNode {
    
    private final Object value;
    private final Class<?> type;
    
    public LiteralNode(Object value, Class<?> type, SourceLocation location) {
        super(location);
        this.value = value;
        this.type = type;
    }
    
    public Object getValue() {
        return value;
    }
    
    public Class<?> getType() {
        return type;
    }
    
    @Override
    public <T> T accept(ASTVisitor<T> visitor) {
        return visitor.visit(this);
    }
    
    public static class Builder extends ASTNode.Builder<Builder> {
        private Object value;
        private Class<?> type;
        
        public Builder value(Object value) {
            this.value = value;
            return this;
        }
        
        public Builder type(Class<?> type) {
            this.type = type;
            return this;
        }
        
        @Override
        public LiteralNode build() {
            return new LiteralNode(value, type, location);
        }
    }
}
