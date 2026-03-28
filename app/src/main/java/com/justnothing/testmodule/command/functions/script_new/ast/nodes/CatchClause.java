package com.justnothing.testmodule.command.functions.script_new.ast.nodes;

import com.justnothing.testmodule.command.functions.script_new.ast.ASTNode;
import com.justnothing.testmodule.command.functions.script_new.ast.SourceLocation;

import java.util.List;

public class CatchClause {
    private final List<Class<?>> exceptionTypes;
    private final String variableName;
    private final ASTNode body;
    private final SourceLocation location;
    
    public CatchClause(List<Class<?>> exceptionTypes, String variableName, ASTNode body, SourceLocation location) {
        this.exceptionTypes = exceptionTypes;
        this.variableName = variableName;
        this.body = body;
        this.location = location;
    }
    
    public List<Class<?>> getExceptionTypes() {
        return exceptionTypes;
    }
    
    public String getVariableName() {
        return variableName;
    }
    
    public ASTNode getBody() {
        return body;
    }
    
    public SourceLocation getLocation() {
        return location;
    }
    
    private String indent(int level) {
        return "  ".repeat(Math.max(0, level));
    }
    
    public String formatString(int indent) {
        StringBuilder sb = new StringBuilder();
        sb.append(indent(indent)).append("CatchClause\n");
        
        StringBuilder types = new StringBuilder();
        for (int i = 0; i < exceptionTypes.size(); i++) {
            if (i > 0) types.append(" | ");
            types.append(exceptionTypes.get(i).getSimpleName());
        }
        sb.append(indent(indent + 1)).append("exceptionTypes: ").append(types).append("\n");
        sb.append(indent(indent + 1)).append("variableName: ").append(variableName).append("\n");
        sb.append(indent(indent + 1)).append("body:\n");
        sb.append(body.formatString(indent + 2)).append("\n");
        return sb.toString().stripTrailing();
    }
}
