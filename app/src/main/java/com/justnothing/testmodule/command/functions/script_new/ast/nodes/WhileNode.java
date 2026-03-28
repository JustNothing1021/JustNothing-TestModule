package com.justnothing.testmodule.command.functions.script_new.ast.nodes;

import com.justnothing.testmodule.command.functions.script_new.ast.ASTNode;
import com.justnothing.testmodule.command.functions.script_new.ast.SourceLocation;
import com.justnothing.testmodule.command.functions.script_new.ast.visitor.ASTVisitor;

public class WhileNode extends ASTNode {
    private final ASTNode condition;
    private final ASTNode body;
    
    public WhileNode(ASTNode condition, ASTNode body, SourceLocation location) {
        super(location);
        this.condition = condition;
        this.body = body;
    }
    
    public ASTNode getCondition() {
        return condition;
    }
    
    public ASTNode getBody() {
        return body;
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
        sb.append(indent(indent)).append("WhileNode\n");
        sb.append(indent(indent + 1)).append("condition:\n");
        sb.append(condition.formatString(indent + 2)).append("\n");
        sb.append(indent(indent + 1)).append("body:\n");
        sb.append(body.formatString(indent + 2)).append("\n");
        return sb.toString().stripTrailing();
    }
}
