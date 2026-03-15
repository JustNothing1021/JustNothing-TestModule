package com.justnothing.testmodule.command.functions.script_new.ast.nodes;

import com.justnothing.testmodule.command.functions.script_new.ast.ASTNode;
import com.justnothing.testmodule.command.functions.script_new.ast.SourceLocation;
import com.justnothing.testmodule.command.functions.script_new.ast.visitor.ASTVisitor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 方法调用节点
 * <p>
 * 表示方法调用，包括目标对象、方法名和参数列表。
 * </p>
 * 
 * @author JustNothing1021
 * @since 1.0.0
 */
public class MethodCallNode extends ASTNode {
    
    private final ASTNode target;
    private final String methodName;
    private final List<ASTNode> arguments;
    
    public MethodCallNode(ASTNode target, String methodName, 
                       List<ASTNode> arguments, SourceLocation location) {
        super(location);
        this.target = target;
        this.methodName = methodName;
        this.arguments = arguments != null ? 
            Collections.unmodifiableList(new ArrayList<>(arguments)) : 
            Collections.emptyList();
    }
    
    public ASTNode getTarget() {
        return target;
    }
    
    public String getMethodName() {
        return methodName;
    }
    
    public List<ASTNode> getArguments() {
        return arguments;
    }
    
    @Override
    public <T> T accept(ASTVisitor<T> visitor) {
        return visitor.visit(this);
    }
    
    public static class Builder extends ASTNode.Builder<Builder> {
        private ASTNode target;
        private String methodName;
        private List<ASTNode> arguments;
        
        public Builder target(ASTNode target) {
            this.target = target;
            return this;
        }
        
        public Builder methodName(String methodName) {
            this.methodName = methodName;
            return this;
        }
        
        public Builder arguments(List<ASTNode> arguments) {
            this.arguments = arguments;
            return this;
        }
        
        @Override
        public MethodCallNode build() {
            return new MethodCallNode(target, methodName, arguments, location);
        }
    }
}
