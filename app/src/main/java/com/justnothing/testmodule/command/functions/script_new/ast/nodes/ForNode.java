package com.justnothing.testmodule.command.functions.script_new.ast.nodes;

import com.justnothing.testmodule.command.functions.script_new.ast.ASTNode;
import com.justnothing.testmodule.command.functions.script_new.ast.SourceLocation;
import com.justnothing.testmodule.command.functions.script_new.ast.visitor.ASTVisitor;

/**
 * for循环节点
 * <p>
 * 表示for循环，支持传统for循环和增强for循环。
 * </p>
 * 
 * @author JustNothing1021
 * @since 1.0.0
 */
public class ForNode extends ASTNode {
    
    private final ASTNode initialization;
    private final ASTNode condition;
    private final ASTNode update;
    private final ASTNode body;
    private final boolean isEnhanced;
    private final String variableName;
    private final ASTNode iterable;
    
    public ForNode(ASTNode initialization, ASTNode condition, ASTNode update,
                 ASTNode body, SourceLocation location) {
        super(location);
        this.initialization = initialization;
        this.condition = condition;
        this.update = update;
        this.body = body;
        this.isEnhanced = false;
        this.variableName = null;
        this.iterable = null;
    }
    
    public ForNode(String variableName, ASTNode iterable, ASTNode body,
                 SourceLocation location) {
        super(location);
        this.initialization = null;
        this.condition = null;
        this.update = null;
        this.body = body;
        this.isEnhanced = true;
        this.variableName = variableName;
        this.iterable = iterable;
    }
    
    public ASTNode getInitialization() {
        return initialization;
    }
    
    public ASTNode getCondition() {
        return condition;
    }
    
    public ASTNode getUpdate() {
        return update;
    }
    
    public ASTNode getBody() {
        return body;
    }
    
    public boolean isEnhanced() {
        return isEnhanced;
    }
    
    public String getVariableName() {
        return variableName;
    }
    
    public ASTNode getIterable() {
        return iterable;
    }
    
    @Override
    public <T> T accept(ASTVisitor<T> visitor) {
        return visitor.visit(this);
    }
    
    @Override
    public String formatString(int indent) {
        StringBuilder sb = new StringBuilder();
        sb.append(indent(indent)).append("ForNode\n");
        sb.append(indent(indent + 1)).append("type: ").append(isEnhanced ? "enhanced" : "traditional").append("\n");
        
        if (isEnhanced) {
            sb.append(indent(indent + 1)).append("variable: ").append(variableName).append("\n");
            sb.append(indent(indent + 1)).append("iterable:\n");
            assert iterable != null;
            sb.append(iterable.formatString(indent + 2)).append("\n");
        } else {
            sb.append(indent(indent + 1)).append("initialization:\n");
            if (initialization != null) {
                sb.append(initialization.formatString(indent + 2)).append("\n");
            } else {
                sb.append(indent(indent + 2)).append("null\n");
            }
            sb.append(indent(indent + 1)).append("condition:\n");
            if (condition != null) {
                sb.append(condition.formatString(indent + 2)).append("\n");
            } else {
                sb.append(indent(indent + 2)).append("null\n");
            }
            sb.append(indent(indent + 1)).append("update:\n");
            if (update != null) {
                sb.append(update.formatString(indent + 2)).append("\n");
            } else {
                sb.append(indent(indent + 2)).append("null\n");
            }
        }
        
        sb.append(indent(indent + 1)).append("body:\n");
        sb.append(body.formatString(indent + 2)).append("\n");
        
        return sb.toString().stripTrailing();
    }
    
    public static class Builder extends ASTNode.Builder<Builder> {
        private ASTNode initialization;
        private ASTNode condition;
        private ASTNode update;
        private ASTNode body;
        private boolean isEnhanced;
        private String variableName;
        private ASTNode iterable;
        
        public Builder initialization(ASTNode initialization) {
            this.initialization = initialization;
            return this;
        }
        
        public Builder condition(ASTNode condition) {
            this.condition = condition;
            return this;
        }
        
        public Builder update(ASTNode update) {
            this.update = update;
            return this;
        }
        
        public Builder body(ASTNode body) {
            this.body = body;
            return this;
        }
        
        public Builder enhanced(String variableName, ASTNode iterable) {
            this.isEnhanced = true;
            this.variableName = variableName;
            this.iterable = iterable;
            return this;
        }
        
        @Override
        public ForNode build() {
            if (isEnhanced) {
                return new ForNode(variableName, iterable, body, location);
            } else {
                return new ForNode(initialization, condition, update, body, location);
            }
        }
    }
}
