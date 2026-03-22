package com.justnothing.testmodule.command.functions.script_new.ast.nodes;

import com.justnothing.testmodule.command.functions.script_new.ast.ASTNode;
import com.justnothing.testmodule.command.functions.script_new.ast.SourceLocation;
import com.justnothing.testmodule.command.functions.script_new.ast.visitor.ASTVisitor;

public class ForEachNode extends ASTNode {
    private final Class<?> itemType;
    private final String itemName;
    private final ASTNode collection;
    private final ASTNode body;
    
    public ForEachNode(Class<?> itemType, String itemName, ASTNode collection, ASTNode body, SourceLocation location) {
        super(location);
        this.itemType = itemType;
        this.itemName = itemName;
        this.collection = collection;
        this.body = body;
    }
    
    public Class<?> getItemType() {
        return itemType;
    }
    
    public String getItemName() {
        return itemName;
    }
    
    public ASTNode getCollection() {
        return collection;
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
        sb.append(indent(indent)).append("ForEachNode\n");
        sb.append(indent(indent + 1)).append("itemType: ").append(itemType != null ? itemType.getSimpleName() : "auto").append("\n");
        sb.append(indent(indent + 1)).append("itemName: ").append(itemName).append("\n");
        sb.append(indent(indent + 1)).append("collection:\n");
        sb.append(collection.formatString(indent + 2)).append("\n");
        sb.append(indent(indent + 1)).append("body:\n");
        sb.append(body.formatString(indent + 2)).append("\n");
        return sb.toString().strip();
    }
}
