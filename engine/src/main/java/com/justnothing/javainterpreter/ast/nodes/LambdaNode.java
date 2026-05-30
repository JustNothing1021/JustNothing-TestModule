package com.justnothing.javainterpreter.ast.nodes;

import com.justnothing.javainterpreter.ast.ASTNode;
import com.justnothing.javainterpreter.ast.SourceLocation;
import com.justnothing.javainterpreter.ast.visitor.ASTVisitor;

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
    private Class<?> functionalInterfaceType;
    private final ClassReferenceNode returnType;
    
    public LambdaNode(List<Parameter> parameters, ASTNode body, SourceLocation location) {
        this(parameters, body, null, location);
    }

    public LambdaNode(List<Parameter> parameters, ASTNode body, ClassReferenceNode returnType, SourceLocation location) {
        super(location);
        this.parameters = parameters != null ? 
            Collections.unmodifiableList(new ArrayList<>(parameters)) : 
            Collections.emptyList();
        this.body = body;
        this.returnType = returnType;
    }
    
    public List<Parameter> getParameters() {
        return parameters;
    }
    
    public ASTNode getBody() {
        return body;
    }
    
    public ClassReferenceNode getReturnType() {
        return returnType;
    }
    
    public Class<?> getFunctionalInterfaceType() {
        return functionalInterfaceType;
    }
    
    public void setFunctionalInterfaceType(Class<?> functionalInterfaceType) {
        this.functionalInterfaceType = functionalInterfaceType;
    }
    
    @Override
    public <T> T accept(ASTVisitor<T> visitor) {
        return visitor.visit(this);
    }
    
    @Override
    public String formatString(int indent) {
        StringBuilder sb = new StringBuilder();
        sb.append(indent(indent)).append("LambdaNode\n");
        sb.append(indent(indent + 1)).append("parameters: ").append(parameters.size()).append("\n");
        for (int i = 0; i < parameters.size(); i++) {
            Parameter param = parameters.get(i);
            sb.append(indent(indent + 2)).append("param[").append(i).append("]: ");
            sb.append(param.getType().getSimpleName()).append(" ").append(param.getName()).append("\n");
        }
        if (functionalInterfaceType != null) {
            sb.append(indent(indent + 1)).append("targetInterface: ").append(functionalInterfaceType.getName()).append("\n");
        }
        if (returnType != null) {
            sb.append(indent(indent + 1)).append("returnType: ").append(returnType.getTypeName()).append("\n");
        }
        sb.append(indent(indent + 1)).append("body:\n");
        sb.append(body.formatString(indent + 2)).append("\n");
        return sb.toString().stripTrailing();
    }
    
    public static class Builder extends ASTNode.Builder<Builder> {
        private List<Parameter> parameters;
        private ASTNode body;
        private ClassReferenceNode returnType;
        
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
        
        public Builder returnType(ClassReferenceNode returnType) {
            this.returnType = returnType;
            return this;
        }
        
        @Override
        public LambdaNode build() {
            return new LambdaNode(parameters, body, returnType, location);
        }
    }
}
