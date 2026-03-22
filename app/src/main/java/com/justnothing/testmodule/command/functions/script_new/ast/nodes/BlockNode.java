package com.justnothing.testmodule.command.functions.script_new.ast.nodes;

import com.justnothing.testmodule.command.functions.script_new.ast.ASTNode;
import com.justnothing.testmodule.command.functions.script_new.ast.SourceLocation;
import com.justnothing.testmodule.command.functions.script_new.ast.visitor.ASTVisitor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 代码块节点
 * <p>
 * 表示一个代码块，包含多个语句。
 * </p>
 * 
 * @author JustNothing1021
 * @since 1.0.0
 */
public class BlockNode extends ASTNode {
    
    private final List<ASTNode> statements;
    
    public BlockNode(List<ASTNode> statements, SourceLocation location) {
        super(location);
        this.statements = statements != null ? 
            Collections.unmodifiableList(new ArrayList<>(statements)) : 
            Collections.emptyList();
    }
    
    public List<ASTNode> getStatements() {
        return statements;
    }
    
    @Override
    public <T> T accept(ASTVisitor<T> visitor) {
        return visitor.visit(this);
    }
    
    @Override
    public String formatString(int indent) {
        StringBuilder sb = new StringBuilder();
        sb.append(indent(indent)).append("BlockNode\n");
        sb.append(indent(indent + 1)).append("statements: ").append(statements.size()).append("\n");
        for (int i = 0; i < statements.size(); i++) {
            sb.append(indent(indent + 1)).append("stmt[").append(i).append("]:\n");
            sb.append(statements.get(i).formatString(indent + 2)).append("\n");
        }
        return sb.toString().strip();
    }
    
    public static class Builder extends ASTNode.Builder<Builder> {
        private List<ASTNode> statements;
        
        public Builder statements(List<ASTNode> statements) {
            this.statements = statements;
            return this;
        }
        
        public Builder addStatement(ASTNode statement) {
            if (this.statements == null) {
                this.statements = new ArrayList<>();
            }
            this.statements.add(statement);
            return this;
        }
        
        @Override
        public BlockNode build() {
            return new BlockNode(statements, location);
        }
    }
}
