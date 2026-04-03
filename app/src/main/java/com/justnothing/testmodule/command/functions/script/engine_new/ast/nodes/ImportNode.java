package com.justnothing.testmodule.command.functions.script.engine_new.ast.nodes;

import com.justnothing.testmodule.command.functions.script.engine_new.ast.ASTNode;
import com.justnothing.testmodule.command.functions.script.engine_new.ast.SourceLocation;
import com.justnothing.testmodule.command.functions.script.engine_new.ast.visitor.ASTVisitor;

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
        String sb = indent(indent) + "ImportNode\n" +
                indent(indent + 1) + "packageName: " + packageName + "\n";
        return sb.stripTrailing();
    }
}
