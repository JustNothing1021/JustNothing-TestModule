package com.justnothing.testmodule.command.functions.script.engine_new.ast.nodes;

import com.justnothing.testmodule.command.functions.script.engine_new.ast.ASTNode;
import com.justnothing.testmodule.command.functions.script.engine_new.ast.SourceLocation;
import com.justnothing.testmodule.command.functions.script.engine_new.ast.visitor.ASTVisitor;

public class InstanceofNode extends ASTNode {
    
    private final ASTNode expression;
    private final String typeName;
    
    public InstanceofNode(ASTNode expression, String typeName, SourceLocation location) {
        super(location);
        this.expression = expression;
        this.typeName = typeName;
    }
    
    public ASTNode getExpression() {
        return expression;
    }
    
    public String getTypeName() {
        return typeName;
    }
    
    @Override
    public <T> T accept(ASTVisitor<T> visitor) {
        return visitor.visit(this);
    }
    
    @Override
    public String formatString(int indent) {
        String sb = indent(indent) + "InstanceofNode\n" +
                indent(indent + 1) + "typeName: " + typeName + "\n" +
                indent(indent + 1) + "expression:\n" +
                expression.formatString(indent + 2) + "\n";
        return sb.stripTrailing();
    }
}
