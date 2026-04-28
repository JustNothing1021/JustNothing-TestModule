package com.justnothing.javainterpreter.ast.nodes;

import com.justnothing.javainterpreter.ast.ASTNode;
import com.justnothing.javainterpreter.ast.SourceLocation;
import com.justnothing.javainterpreter.ast.visitor.ASTVisitor;

public class BreakNode extends ASTNode {

    private final String label;

    public BreakNode(SourceLocation location) {
        super(location);
        this.label = null;
    }

    public BreakNode(String label, SourceLocation location) {
        super(location);
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    public boolean isLabeled() {
        return label != null;
    }

    @Override
    public <T> T accept(ASTVisitor<T> visitor) {
        return visitor.visit(this);
    }

    @Override
    public String formatString(int indent) {
        if (label != null) {
            return indent(indent) + "BreakNode[label=" + label + "]";
        }
        return indent(indent) + "BreakNode";
    }

    @Override
    public String toString() {
        if (label != null) {
            return "BreakNode[label=" + label + "]";
        }
        return "BreakNode";
    }
}
