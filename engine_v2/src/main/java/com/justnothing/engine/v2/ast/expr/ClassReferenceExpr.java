package com.justnothing.engine.v2.ast.expr;

import com.justnothing.engine.v2.ast.ASTNode;
import com.justnothing.engine.v2.ast.ASTVisitor;
import com.justnothing.engine.v2.ast.Resolvable;

import java.util.List;

public class ClassReferenceExpr extends ASTNode {

    private final Resolvable classRef;

    private ClassReferenceExpr(Builder builder) {
        super(builder.getLocation());
        this.classRef = builder.classRef;
    }

    public Resolvable getClassRef() {
        return classRef;
    }

    /** 便捷方法：获取类名 */
    public String getClassName() {
        return classRef.name();
    }

    @Override
    public <T> T accept(ASTVisitor<T> visitor) {
        return visitor.visit(this);
    }

    @Override
    public String nodeName() {
        return "ClassReference";
    }

    @Override
    public List<ASTNode> getChildren() {
        return List.of();
    }

    public static class Builder extends ASTNode.Builder<Builder> {
        private Resolvable classRef;

        public Builder className(String className) {
            this.classRef = Resolvable.byName(className);
            return this;
        }

        public Builder classRef(Resolvable classRef) {
            this.classRef = classRef;
            return this;
        }

        public ClassReferenceExpr build() {
            return new ClassReferenceExpr(this);
        }
    }
}
