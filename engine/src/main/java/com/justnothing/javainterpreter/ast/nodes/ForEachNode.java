package com.justnothing.javainterpreter.ast.nodes;

import com.justnothing.javainterpreter.ast.ASTNode;
import com.justnothing.javainterpreter.ast.SourceLocation;
import com.justnothing.javainterpreter.ast.visitor.ASTVisitor;

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
        String sb = indent(indent) + "ForEachNode\n" +
                indent(indent + 1) + "itemType: " + (itemType != null ? itemType.getSimpleName() : "auto") + "\n" +
                indent(indent + 1) + "itemName: " + itemName + "\n" +
                indent(indent + 1) + "collection:\n" +
                collection.formatString(indent + 2) + "\n" +
                indent(indent + 1) + "body:\n" +
                body.formatString(indent + 2) + "\n";
        return sb.stripTrailing();
    }
}
