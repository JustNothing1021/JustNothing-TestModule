package com.justnothing.testmodule.command.functions.script.engine_new.ast.nodes;

import com.justnothing.testmodule.command.functions.script.engine_new.ast.ASTNode;
import com.justnothing.testmodule.command.functions.script.engine_new.ast.SourceLocation;
import com.justnothing.testmodule.command.functions.script.engine_new.ast.visitor.ASTVisitor;

import java.util.List;

public class CaseNode extends ASTNode {
    private final ASTNode value;
    private final List<ASTNode> statements;
    
    public CaseNode(ASTNode value, List<ASTNode> statements, SourceLocation location) {
        super(location);
        this.value = value;
        this.statements = statements;
    }
    
    public ASTNode getValue() {
        return value;
    }
    
    public List<ASTNode> getStatements() {
        return statements;
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
        sb.append(indent(indent)).append("CaseNode\n");
        sb.append(indent(indent + 1)).append("value:\n");
        sb.append(value.formatString(indent + 2)).append("\n");
        sb.append(indent(indent + 1)).append("statements: ").append(statements.size()).append("\n");
        for (int i = 0; i < statements.size(); i++) {
            sb.append(indent(indent + 1)).append("stmt[").append(i).append("]:\n");
            sb.append(statements.get(i).formatString(indent + 2)).append("\n");
        }
        return sb.toString().stripTrailing();
    }
}
