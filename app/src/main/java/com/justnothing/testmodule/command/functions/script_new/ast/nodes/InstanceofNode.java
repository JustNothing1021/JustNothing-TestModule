package com.justnothing.testmodule.command.functions.script_new.ast.nodes;

import com.justnothing.testmodule.command.functions.script_new.ast.ASTNode;
import com.justnothing.testmodule.command.functions.script_new.ast.SourceLocation;
import com.justnothing.testmodule.command.functions.script_new.ast.visitor.ASTVisitor;

public class InstanceofNode extends ASTNode {
    
    private final ASTNode expression;
    private final String typeName;
    
    public InstanceofNode(ASTNode expression, String typeName, SourceLocation location) {
        super(location);
        this.expression = expression;
        this.typeName = typeName;
    }
    
    public ASTNode getExpression() {
        return expression;
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
        sb.append(indent(indent)).append("InstanceofNode\n");
        sb.append(indent(indent + 1)).append("typeName: ").append(typeName).append("\n");
        sb.append(indent(indent + 1)).append("expression:\n");
        sb.append(expression.formatString(indent + 2)).append("\n");
        return sb.toString().stripTrailing();
    }
}
