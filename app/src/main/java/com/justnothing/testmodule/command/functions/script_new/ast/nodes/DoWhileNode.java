package com.justnothing.testmodule.command.functions.script_new.ast.nodes;

import com.justnothing.testmodule.command.functions.script_new.ast.ASTNode;
import com.justnothing.testmodule.command.functions.script_new.ast.SourceLocation;
import com.justnothing.testmodule.command.functions.script_new.ast.visitor.ASTVisitor;

public class DoWhileNode extends ASTNode {
    private final ASTNode body;
    private final ASTNode condition;
    
    public DoWhileNode(ASTNode body, ASTNode condition, SourceLocation location) {
        super(location);
        this.body = body;
        this.condition = condition;
    }
    
    public ASTNode getBody() {
        return body;
    }
    
    public ASTNode getCondition() {
        return condition;
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
        sb.append(indent(indent)).append("DoWhileNode\n");
        sb.append(indent(indent + 1)).append("body:\n");
        sb.append(body.formatString(indent + 2)).append("\n");
        sb.append(indent(indent + 1)).append("condition:\n");
        sb.append(condition.formatString(indent + 2)).append("\n");
        return sb.toString().stripTrailing();

    }
}
