package com.justnothing.testmodule.command.functions.script_new.ast.nodes;

import com.justnothing.testmodule.command.functions.script_new.ast.ASTNode;
import com.justnothing.testmodule.command.functions.script_new.ast.SourceLocation;
import com.justnothing.testmodule.command.functions.script_new.ast.visitor.ASTVisitor;

public class PipelineNode extends ASTNode {
    
    private final ASTNode input;
    private final ASTNode function;
    
    public PipelineNode(ASTNode input, ASTNode function, SourceLocation location) {
        super(location);
        this.input = input;
        this.function = function;
    }
    
    public ASTNode getInput() {
        return input;
    }
    
    public ASTNode getFunction() {
        return function;
    }
    
    @Override
    public <T> T accept(ASTVisitor<T> visitor) {
        return visitor.visit(this);
    }
    
    @Override
    public String formatString(int indent) {
        String sb = indent(indent) + "PipelineNode\n" +
                indent(indent + 1) + "input:\n" +
                input.formatString(indent + 2) +
                "\n" + indent(indent + 1) + "function:\n" +
                function.formatString(indent + 2);
        return sb.stripTrailing();
    }
}
