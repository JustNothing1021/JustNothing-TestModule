package com.justnothing.testmodule.command.functions.script.engine_new.ast.nodes;

import com.justnothing.testmodule.command.functions.script.engine_new.ast.ASTNode;
import com.justnothing.testmodule.command.functions.script.engine_new.ast.SourceLocation;
import com.justnothing.testmodule.command.functions.script.engine_new.ast.visitor.ASTVisitor;

public class ParameterNode extends ASTNode {
    
    private final String parameterName;
    private final String typeName;
    
    public ParameterNode(String parameterName, String typeName, SourceLocation location) {
        super(location);
        this.parameterName = parameterName;
        this.typeName = typeName;
    }
    
    public String getParameterName() {
        return parameterName;
    }
    
    public String getTypeName() {
        return typeName;
    }
    
    @Override
    public <T> T accept(ASTVisitor<T> visitor) {
        return visitor.visit(this);
    }
    
    @Override
    public String formatString(int indent) {
        StringBuilder sb = new StringBuilder();
        sb.append(indent(indent)).append("ParameterNode\n");
        sb.append(indent(indent + 1)).append("parameterName: ").append(parameterName).append("\n");
        sb.append(indent(indent + 1)).append("typeName: ").append(typeName).append("\n");
        return sb.toString().stripTrailing();
    }
}
