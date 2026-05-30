package com.justnothing.javainterpreter.ast.nodes;

import com.justnothing.javainterpreter.ast.ASTNode;
import com.justnothing.javainterpreter.ast.SourceLocation;
import com.justnothing.javainterpreter.ast.visitor.ASTVisitor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class FunctionDefNode extends ASTNode {

    private final String functionName;
    private final ClassReferenceNode returnType;
    private final List<LambdaNode.Parameter> parameters;
    private final ASTNode body;

    public FunctionDefNode(String functionName, ClassReferenceNode returnType,
                           List<LambdaNode.Parameter> parameters, ASTNode body,
                           SourceLocation location) {
        super(location);
        this.functionName = functionName;
        this.returnType = returnType;
        this.parameters = parameters != null ?
            Collections.unmodifiableList(new ArrayList<>(parameters)) :
            Collections.emptyList();
        this.body = body;
    }

    public String getFunctionName() {
        return functionName;
    }

    public ClassReferenceNode getReturnType() {
        return returnType;
    }

    public List<LambdaNode.Parameter> getParameters() {
        return parameters;
    }

    public ASTNode getBody() {
        return body;
    }

    @Override
    public <T> T accept(ASTVisitor<T> visitor) {
        return visitor.visit(this);
    }

    @Override
    public String formatString(int indent) {
        StringBuilder sb = new StringBuilder();
        sb.append(indent(indent)).append("FunctionDefNode\n");
        sb.append(indent(indent + 1)).append("name: ").append(functionName).append("\n");
        if (returnType != null) {
            sb.append(indent(indent + 1)).append("returnType: ").append(returnType.getTypeName()).append("\n");
        }
        sb.append(indent(indent + 1)).append("parameters: ").append(parameters.size()).append("\n");
        for (int i = 0; i < parameters.size(); i++) {
            LambdaNode.Parameter param = parameters.get(i);
            sb.append(indent(indent + 2)).append("param[").append(i).append("]: ");
            sb.append(param.getType().getSimpleName()).append(" ").append(param.getName()).append("\n");
        }
        if (body != null) {
            sb.append(indent(indent + 1)).append("body:\n");
            sb.append(body.formatString(indent + 2)).append("\n");
        }
        return sb.toString().stripTrailing();
    }
}
