package com.justnothing.javainterpreter.ast.nodes;

import com.justnothing.javainterpreter.ast.ASTNode;
import com.justnothing.javainterpreter.ast.SourceLocation;
import com.justnothing.javainterpreter.ast.visitor.ASTVisitor;

public class UsingAliasNode extends ASTNode {

    private final String aliasName;
    private final String fullClassName;

    public UsingAliasNode(String aliasName, String fullClassName, SourceLocation location) {
        super(location);
        this.aliasName = aliasName;
        this.fullClassName = fullClassName;
    }

    public String getAliasName() {
        return aliasName;
    }

    public String getFullClassName() {
        return fullClassName;
    }

    @Override
    public <T> T accept(ASTVisitor<T> visitor) {
        return visitor.visit(this);
    }

    @Override
    public String formatString(int indent) {
        return indent(indent) + "UsingAliasNode\n" +
                indent(indent + 1) + "alias: " + aliasName + "\n" +
                indent(indent + 1) + "target: " + fullClassName + "\n";
    }
}
