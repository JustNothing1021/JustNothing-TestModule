package com.justnothing.engine.v2.ast.stmt;

import com.justnothing.engine.v2.ast.ASTNode;
import com.justnothing.engine.v2.ast.ASTVisitor;
import com.justnothing.engine.v2.ast.Resolvable;
import com.justnothing.engine.v2.ast.SourceLocation;
import com.justnothing.engine.v2.ast.decl.AnnotationVal;

import java.util.ArrayList;
import java.util.List;

public class ForEachStmt extends ASTNode {

    private final String variableName;
    private final Resolvable itemType;
    private final ASTNode iterable;
    private final ASTNode body;
    private final boolean isFinal;
    private final List<AnnotationVal> annotations = new ArrayList<>();

    private ForEachStmt(SourceLocation location, String variableName,
                        Resolvable itemType, ASTNode iterable, ASTNode body,
                        boolean isFinal) {
        super(location);
        this.variableName = variableName;
        this.itemType = itemType;
        this.iterable = iterable;
        this.body = body;
        this.isFinal = isFinal;
    }

    public String getVariableName() {
        return variableName;
    }

    public ASTNode getIterable() {
        return iterable;
    }

    public ASTNode getBody() {
        return body;
    }

    public Resolvable getItemType() {
        return itemType;
    }

    public boolean isFinal() { return isFinal; }

    public List<AnnotationVal> getAnnotations() { return annotations; }
    public void addAnnotation(AnnotationVal a) { annotations.add(a); }

    @Override
    public <T> T accept(ASTVisitor<T> visitor) {
        return visitor.visit(this);
    }

    @Override
    public String nodeName() {
        return "ForEach";
    }

    @Override
    public List<ASTNode> getChildren() {
        return ASTNode.children(iterable, body);
    }

    public static class Builder extends ASTNode.Builder<Builder> {
        private String variableName;
        private Resolvable itemType;
        private ASTNode iterable;
        private ASTNode body;
        private boolean isFinal;

        public Builder variableName(String variableName) {
            this.variableName = variableName;
            return this;
        }

        public Builder itemType(Resolvable itemType) {
            this.itemType = itemType;
            return this;
        }

        public Builder iterable(ASTNode iterable) {
            this.iterable = iterable;
            return this;
        }

        public Builder body(ASTNode body) {
            this.body = body;
            return this;
        }

        public Builder isFinal(boolean isFinal) {
            this.isFinal = isFinal;
            return this;
        }

        public ForEachStmt build() {
            return new ForEachStmt(location, variableName, itemType, iterable, body, isFinal);
        }
    }
}
