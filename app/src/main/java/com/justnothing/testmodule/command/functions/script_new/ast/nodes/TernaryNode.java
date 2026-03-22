package com.justnothing.testmodule.command.functions.script_new.ast.nodes;

import com.justnothing.testmodule.command.functions.script_new.ast.ASTNode;
import com.justnothing.testmodule.command.functions.script_new.ast.SourceLocation;
import com.justnothing.testmodule.command.functions.script_new.ast.visitor.ASTVisitor;

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
        StringBuilder sb = new StringBuilder();
        sb.append(indent(indent)).append("TernaryNode\n");
        sb.append(indent(indent + 1)).append("condition:\n");
        sb.append(condition.formatString(indent + 2)).append("\n");
        sb.append(indent(indent + 1)).append("then:\n");
        sb.append(thenExpr.formatString(indent + 2)).append("\n");
        sb.append(indent(indent + 1)).append("else:\n");
        sb.append(elseExpr.formatString(indent + 2)).append("\n");
        return sb.toString().strip();
    }
}
