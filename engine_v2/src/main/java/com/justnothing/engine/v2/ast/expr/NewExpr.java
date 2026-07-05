package com.justnothing.engine.v2.ast.expr;

import com.justnothing.engine.v2.ast.ASTNode;
import com.justnothing.engine.v2.ast.ASTVisitor;
import com.justnothing.engine.v2.ast.Resolvable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class NewExpr extends ASTNode {

    private final Resolvable classRef;
    private final List<ASTNode> arguments;
    private final ASTNode anonymousBody;

    private NewExpr(Builder builder) {
        super(builder.getLocation());
        this.classRef = builder.classRef;
        this.arguments = Collections.unmodifiableList(new ArrayList<>(builder.arguments));
        this.anonymousBody = builder.anonymousBody;
    }

    public Resolvable getClassRef() {
        return classRef;
    }

    public String getClassName() {
        return classRef.name();
    }

    public List<ASTNode> getArguments() {
        return arguments;
    }

    /** 匿名类体（null 表示普通 new 表达式） */
    public ASTNode getAnonymousBody() {
        return anonymousBody;
    }

    public boolean isAnonymousClass() {
        return anonymousBody != null;
    }

    @Override
    public <T> T accept(ASTVisitor<T> visitor) {
        return visitor.visit(this);
    }

    @Override
    public String nodeName() {
        return "New";
    }

    @Override
    public List<ASTNode> getChildren() {
        return ASTNode.children(arguments);
    }

    public static class Builder extends ASTNode.Builder<Builder> {
        private Resolvable classRef;
        private List<ASTNode> arguments = Collections.emptyList();
        private ASTNode anonymousBody;

        public Builder className(String className) {
            this.classRef = Resolvable.byName(className);
            return this;
        }

        public Builder classRef(Resolvable classRef) {
            this.classRef = classRef;
            return this;
        }

        public Builder arguments(List<ASTNode> arguments) {
            this.arguments = arguments;
            return this;
        }

        public Builder anonymousBody(ASTNode body) {
            this.anonymousBody = body;
            return this;
        }

        public NewExpr build() {
            return new NewExpr(this);
        }
    }
}
