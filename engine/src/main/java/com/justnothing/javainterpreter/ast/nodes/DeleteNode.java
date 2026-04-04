package com.justnothing.javainterpreter.ast.nodes;

import com.justnothing.javainterpreter.ast.ASTNode;
import com.justnothing.javainterpreter.ast.SourceLocation;
import com.justnothing.javainterpreter.ast.visitor.ASTVisitor;

public class DeleteNode extends ASTNode {
    
    private final String variableName;
    private final boolean deleteAll;
    
    public DeleteNode(String variableName, SourceLocation location) {
        super(location);
        this.variableName = variableName;
        this.deleteAll = false;
    }
    
    public DeleteNode(boolean deleteAll, SourceLocation location) {
        super(location);
        this.variableName = null;
        this.deleteAll = deleteAll;
    }
    
    public String getVariableName() {
        return variableName;
    }
    
    public boolean isDeleteAll() {
        return deleteAll;
    }
    
    @Override
    public <T> T accept(ASTVisitor<T> visitor) {
        return visitor.visit(this);
    }
    
    @Override
    public String formatString(int indent) {
        StringBuilder sb = new StringBuilder();
        sb.append(indent(indent)).append("DeleteNode\n");
        if (deleteAll) {
            sb.append(indent(indent + 1)).append("deleteAll: true\n");
        } else {
            sb.append(indent(indent + 1)).append("variableName: ").append(variableName).append("\n");
        }
        return sb.toString().stripTrailing();
    }
}
