package com.justnothing.testmodule.command.functions.script.engine_new.ast.nodes;

import com.justnothing.testmodule.command.functions.script.engine_new.ast.ASTNode;
import com.justnothing.testmodule.command.functions.script.engine_new.ast.SourceLocation;
import com.justnothing.testmodule.command.functions.script.engine_new.ast.visitor.ASTVisitor;

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
        String sb = indent(indent) + "DoWhileNode\n" +
                indent(indent + 1) + "body:\n" +
                body.formatString(indent + 2) + "\n" +
                indent(indent + 1) + "condition:\n" +
                condition.formatString(indent + 2) + "\n";
        return sb.stripTrailing();

    }
}
