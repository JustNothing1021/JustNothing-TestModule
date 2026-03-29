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
        StringBuilder sb = new StringBuilder();
        sb.append(indent(indent)).append("PipelineNode\n");
        sb.append(indent(indent + 1)).append("input:\n");
        sb.append(input.formatString(indent + 2));
        sb.append("\n").append(indent(indent + 1)).append("function:\n");
        sb.append(function.formatString(indent + 2));
        return sb.toString().stripTrailing();
    }
}
