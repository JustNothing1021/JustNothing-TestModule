package com.justnothing.testmodule.command.functions.script_new.ast.nodes;

import com.justnothing.testmodule.command.functions.script_new.ast.ASTNode;
import com.justnothing.testmodule.command.functions.script_new.ast.SourceLocation;
import com.justnothing.testmodule.command.functions.script_new.ast.visitor.ASTVisitor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Lambda表达式节点
 * <p>
 * 表示Lambda表达式，包括参数列表和函数体。
 * </p>
 * 
 * @author JustNothing1021
 * @since 1.0.0
 */
public class LambdaNode extends ASTNode {
    
    /**
     * Lambda参数
     */
    public static class Parameter {
        private final String name;
        private final Class<?> type;
        
        public Parameter(String name, Class<?> type) {
            this.name = name;
            this.type = type;
        }
        
        public String getName() {
            return name;
        }
        
        public Class<?> getType() {
            return type;
        }
    }
    
    private final List<Parameter> parameters;
    private final ASTNode body;
    
    public LambdaNode(List<Parameter> parameters, ASTNode body, SourceLocation location) {
        super(location);
        this.parameters = parameters != null ? 
            Collections.unmodifiableList(new ArrayList<>(parameters)) : 
            Collections.emptyList();
        this.body = body;
    }
    
    public List<Parameter> getParameters() {
        return parameters;
    }
    
    public ASTNode getBody() {
        return body;
    }
    
    @Override
    public <T> T accept(ASTVisitor<T> visitor) {
        return visitor.visit(this);
    }
    
    public static class Builder extends ASTNode.Builder<Builder> {
        private List<Parameter> parameters;
        private ASTNode body;
        
        public Builder parameters(List<Parameter> parameters) {
            this.parameters = parameters;
            return this;
        }
        
        public Builder addParameter(String name, Class<?> type) {
            if (this.parameters == null) {
                this.parameters = new ArrayList<>();
            }
            this.parameters.add(new Parameter(name, type));
            return this;
        }
        
        public Builder body(ASTNode body) {
            this.body = body;
            return this;
        }
        
        @Override
        public LambdaNode build() {
            return new LambdaNode(parameters, body, location);
        }
    }
}
