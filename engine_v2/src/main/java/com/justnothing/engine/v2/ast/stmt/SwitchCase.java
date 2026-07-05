package com.justnothing.engine.v2.ast.stmt;

import com.justnothing.engine.v2.ast.ASTNode;
import com.justnothing.engine.v2.ast.ASTVisitor;
import com.justnothing.engine.v2.ast.SourceLocation;

import java.util.List;

/**
 * switch 语句的 case/default 分支。
 * <p>
 * {@code isDefault = true} 时 values 为空；否则 values 包含 case 匹配值列表。
 * body 是分支体的 BlockStmt 或 ExprStmt（arrow 语法）。
 * </p>
 *
 * @author JustNothing1021
 */
public class SwitchCase extends ASTNode {

    private final boolean isDefault;
    private final List<ASTNode> values;
    private final ASTNode body;

    private SwitchCase(SourceLocation location, boolean isDefault, List<ASTNode> values, ASTNode body) {
        super(location);
        this.isDefault = isDefault;
        this.values = values;
        this.body = body;
    }

    public boolean isDefault() {
        return isDefault;
    }

    public List<ASTNode> getValues() {
        return values;
    }

    public ASTNode getBody() {
        return body;
    }

    @Override
    public <T> T accept(ASTVisitor<T> visitor) {
        return visitor.visit(this);
    }

    @Override
    public String nodeName() {
        return isDefault ? "DefaultCase" : "Case";
    }

    @Override
    public List<ASTNode> getChildren() {
        return ASTNode.children(body, values);
    }

    public static class Builder extends ASTNode.Builder<Builder> {
        private boolean isDefault;
        private List<ASTNode> values = List.of();
        private ASTNode body;

        public Builder isDefault(boolean isDefault) {
            this.isDefault = isDefault;
            return this;
        }

        public Builder values(List<ASTNode> values) {
            this.values = values;
            return this;
        }

        public Builder body(ASTNode body) {
            this.body = body;
            return this;
        }

        public SwitchCase build() {
            return new SwitchCase(location, isDefault, values, body);
        }
    }
}
