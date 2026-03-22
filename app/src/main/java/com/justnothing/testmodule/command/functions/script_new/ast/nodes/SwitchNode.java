package com.justnothing.testmodule.command.functions.script_new.ast.nodes;

import com.justnothing.testmodule.command.functions.script_new.ast.ASTNode;
import com.justnothing.testmodule.command.functions.script_new.ast.SourceLocation;
import com.justnothing.testmodule.command.functions.script_new.ast.visitor.ASTVisitor;

import java.util.List;

public class SwitchNode extends ASTNode {
    private final ASTNode expression;
    private final List<CaseNode> cases;
    private final ASTNode defaultCase;
    
    public SwitchNode(ASTNode expression, List<CaseNode> cases, ASTNode defaultCase, SourceLocation location) {
        super(location);
        this.expression = expression;
        this.cases = cases;
        this.defaultCase = defaultCase;
    }
    
    public ASTNode getExpression() {
        return expression;
    }
    
    public List<CaseNode> getCases() {
        return cases;
    }
    
    public ASTNode getDefaultCase() {
        return defaultCase;
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
        sb.append(indent(indent)).append("SwitchNode\n");
        sb.append(indent(indent + 1)).append("expression:\n");
        sb.append(expression.formatString(indent + 2)).append("\n");
        sb.append(indent(indent + 1)).append("cases: ").append(cases.size()).append("\n");
        for (int i = 0; i < cases.size(); i++) {
            sb.append(indent(indent + 1)).append("case[").append(i).append("]:\n");
            sb.append(cases.get(i).formatString(indent + 2)).append("\n");
        }
        if (defaultCase != null) {
            sb.append(indent(indent + 1)).append("default:\n");
            sb.append(defaultCase.formatString(indent + 2)).append("\n");
        }
        return sb.toString().strip();
    }
}
