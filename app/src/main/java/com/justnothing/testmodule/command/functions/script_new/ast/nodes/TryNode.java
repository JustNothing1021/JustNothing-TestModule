package com.justnothing.testmodule.command.functions.script_new.ast.nodes;

import com.justnothing.testmodule.command.functions.script_new.ast.ASTNode;
import com.justnothing.testmodule.command.functions.script_new.ast.SourceLocation;
import com.justnothing.testmodule.command.functions.script_new.ast.visitor.ASTVisitor;

import java.util.List;

public class TryNode extends ASTNode {
    private final List<ResourceDeclaration> resources;
    private final ASTNode tryBlock;
    private final List<CatchClause> catchClauses;
    private final ASTNode finallyBlock;
    
    public TryNode(List<ResourceDeclaration> resources, ASTNode tryBlock, List<CatchClause> catchClauses, ASTNode finallyBlock, SourceLocation location) {
        super(location);
        this.resources = resources;
        this.tryBlock = tryBlock;
        this.catchClauses = catchClauses;
        this.finallyBlock = finallyBlock;
    }
    
    public List<ResourceDeclaration> getResources() {
        return resources;
    }
    
    public ASTNode getTryBlock() {
        return tryBlock;
    }
    
    public List<CatchClause> getCatchClauses() {
        return catchClauses;
    }
    
    public ASTNode getFinallyBlock() {
        return finallyBlock;
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
        sb.append(indent(indent)).append("TryNode\n");
        if (!resources.isEmpty()) {
            sb.append(indent(indent + 1)).append("resources: ").append(resources.size()).append("\n");
            for (int i = 0; i < resources.size(); i++) {
                sb.append(indent(indent + 1)).append("resource[").append(i).append("]:\n");
                sb.append(resources.get(i).formatString(indent + 2)).append("\n");
            }
        }
        sb.append(indent(indent + 1)).append("tryBlock:\n");
        sb.append(tryBlock.formatString(indent + 2)).append("\n");
        sb.append(indent(indent + 1)).append("catchClauses: ").append(catchClauses.size()).append("\n");
        for (int i = 0; i < catchClauses.size(); i++) {
            sb.append(indent(indent + 1)).append("catch[").append(i).append("]:\n");
            sb.append(catchClauses.get(i).formatString(indent + 2)).append("\n");
        }
        if (finallyBlock != null) {
            sb.append(indent(indent + 1)).append("finallyBlock:\n");
            sb.append(finallyBlock.formatString(indent + 2)).append("\n");
        }
        return sb.toString().stripTrailing();
    }
}
