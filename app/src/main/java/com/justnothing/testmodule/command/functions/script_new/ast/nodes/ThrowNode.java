package com.justnothing.testmodule.command.functions.script_new.ast.nodes;

import com.justnothing.testmodule.command.functions.script_new.ast.ASTNode;
import com.justnothing.testmodule.command.functions.script_new.ast.SourceLocation;
import com.justnothing.testmodule.command.functions.script_new.ast.visitor.ASTVisitor;

public class ThrowNode extends ASTNode {
    private final ASTNode expression;
    
    public ThrowNode(ASTNode expression, SourceLocation location) {
        super(location);
        this.expression = expression;
    }
    
    public ASTNode getExpression() {
        return expression;
    }
    
    @Override
    public <T> T accept(ASTVisitor<T> visitor) {
        return visitor.visit(this);
    }
    
    @Override
    public String formatString() {
        return formatString(0);
    }
    
    @Override
    public String formatString(int indent) {
        StringBuilder sb = new StringBuilder();
        sb.append(indent(indent)).append("ThrowNode\n");
        sb.append(indent(indent + 1)).append("expression:\n");
        sb.append(expression.formatString(indent + 2)).append("\n");
        return sb.toString().strip();
    }
}
