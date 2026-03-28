package com.justnothing.testmodule.command.functions.script_new.ast.nodes;

import com.justnothing.testmodule.command.functions.script_new.ast.ASTNode;
import com.justnothing.testmodule.command.functions.script_new.ast.SourceLocation;
import com.justnothing.testmodule.command.functions.script_new.ast.visitor.ASTVisitor;

import java.util.Map;

public class MapLiteralNode extends ASTNode {
    
    private final Map<ASTNode, ASTNode> entries;
    
    public MapLiteralNode(Map<ASTNode, ASTNode> entries, SourceLocation location) {
        super(location);
        this.entries = entries;
    }
    
    public Map<ASTNode, ASTNode> getEntries() {
        return entries;
    }
    
    @Override
    public <T> T accept(ASTVisitor<T> visitor) {
        return visitor.visit(this);
    }
    
    @Override
    public String formatString(int indent) {
        StringBuilder sb = new StringBuilder();
        sb.append(indent(indent)).append("MapLiteralNode\n");
        sb.append(indent(indent + 1)).append("entries: ").append(entries.size()).append("\n");
        int i = 0;
        for (Map.Entry<ASTNode, ASTNode> entry : entries.entrySet()) {
            sb.append(indent(indent + 1)).append("entry[").append(i).append("]:\n");
            sb.append(indent(indent + 2)).append("key:\n");
            sb.append(entry.getKey().formatString(indent + 3)).append("\n");
            sb.append(indent(indent + 2)).append("value:\n");
            sb.append(entry.getValue().formatString(indent + 3)).append("\n");
            i++;
        }
        return sb.toString().stripTrailing();
    }
}
