package com.justnothing.engine.v2.ast.stmt;

import com.justnothing.engine.v2.ast.ASTNode;
import com.justnothing.engine.v2.ast.ASTVisitor;
import com.justnothing.engine.v2.ast.SourceLocation;

import java.util.List;

public class SwitchStmt extends ASTNode {

    private final ASTNode subject;
    private final List<SwitchCase> cases;

    private SwitchStmt(SourceLocation location, ASTNode subject, List<SwitchCase> cases) {
        super(location);
        this.subject = subject;
        this.cases = cases;
    }

    public ASTNode getSubject() {
        return subject;
    }

    public List<SwitchCase> getCases() {
        return cases;
    }

    @Override
    public <T> T accept(ASTVisitor<T> visitor) {
        return visitor.visit(this);
    }

    @Override
    public String nodeName() {
        return "Switch";
    }

    @Override
    public List<ASTNode> getChildren() {
        return ASTNode.children(subject, cases);
    }

    public static class Builder extends ASTNode.Builder<Builder> {
        private ASTNode subject;
        private List<SwitchCase> cases;

        public Builder subject(ASTNode subject) {
            this.subject = subject;
            return this;
        }

        public Builder cases(List<SwitchCase> cases) {
            this.cases = cases;
            return this;
        }

        public SwitchStmt build() {
            return new SwitchStmt(location, subject, cases);
        }
    }
}
