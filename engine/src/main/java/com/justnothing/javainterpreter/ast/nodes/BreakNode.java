package com.justnothing.javainterpreter.ast.nodes;

import com.justnothing.javainterpreter.ast.ASTNode;
import com.justnothing.javainterpreter.ast.SourceLocation;
import com.justnothing.javainterpreter.ast.visitor.ASTVisitor;

public class BreakNode extends ASTNode {

    public BreakNode(SourceLocation location) {
        super(location);
    }

    @Override
    public <T> T accept(ASTVisitor<T> visitor) {
        return visitor.visit(this);
    }

    @Override
    public String formatString(int indent) {
        return indent(indent) + "BreakNode";
    }

    @Override
    public String toString() {
        return "BreakNode";
    }
}
