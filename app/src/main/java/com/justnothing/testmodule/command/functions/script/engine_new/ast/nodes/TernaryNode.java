package com.justnothing.testmodule.command.functions.script.engine_new.ast.nodes;

import com.justnothing.testmodule.command.functions.script.engine_new.ast.ASTNode;
import com.justnothing.testmodule.command.functions.script.engine_new.ast.SourceLocation;
import com.justnothing.testmodule.command.functions.script.engine_new.ast.visitor.ASTVisitor;

public class TernaryNode extends ASTNode {
    private final ASTNode condition;
    private final ASTNode thenExpr;
    private final ASTNode elseExpr;
    
    public TernaryNode(ASTNode condition, ASTNode thenExpr, ASTNode elseExpr, SourceLocation location) {
        super(location);
        this.condition = condition;
        this.thenExpr = thenExpr;
        this.elseExpr = elseExpr;
    }
    
    public ASTNode getCondition() {
        return condition;
    }
    
    public ASTNode getThenExpr() {
        return thenExpr;
    }
    
    public ASTNode getElseExpr() {
        return elseExpr;
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
        String sb = indent(indent) + "TernaryNode\n" +
                indent(indent + 1) + "condition:\n" +
                condition.formatString(indent + 2) + "\n" +
                indent(indent + 1) + "then:\n" +
                thenExpr.formatString(indent + 2) + "\n" +
                indent(indent + 1) + "else:\n" +
                elseExpr.formatString(indent + 2) + "\n";
        return sb.stripTrailing();
    }
}
