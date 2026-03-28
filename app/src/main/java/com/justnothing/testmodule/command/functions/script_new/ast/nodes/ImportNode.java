package com.justnothing.testmodule.command.functions.script_new.ast.nodes;

import com.justnothing.testmodule.command.functions.script_new.ast.ASTNode;
import com.justnothing.testmodule.command.functions.script_new.ast.SourceLocation;
import com.justnothing.testmodule.command.functions.script_new.ast.visitor.ASTVisitor;

public class ImportNode extends ASTNode {
    
    private final String packageName;
    
    public ImportNode(String packageName, SourceLocation location) {
        super(location);
        this.packageName = packageName;
    }
    
    public String getPackageName() {
        return packageName;
    }
    
    @Override
    public <T> T accept(ASTVisitor<T> visitor) {
        return visitor.visit(this);
    }
    
    @Override
    public String formatString(int indent) {
        StringBuilder sb = new StringBuilder();
        sb.append(indent(indent)).append("ImportNode\n");
        sb.append(indent(indent + 1)).append("packageName: ").append(packageName).append("\n");
        return sb.toString().stripTrailing();
    }
}
