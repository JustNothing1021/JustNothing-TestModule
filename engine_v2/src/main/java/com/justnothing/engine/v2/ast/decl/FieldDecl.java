package com.justnothing.engine.v2.ast.decl;

import com.justnothing.engine.v2.ast.ASTNode;
import com.justnothing.engine.v2.ast.ASTVisitor;
import com.justnothing.engine.v2.ast.Resolvable;
import com.justnothing.engine.v2.ast.SourceLocation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class FieldDecl extends ASTNode {

    private final String name;
    private final Resolvable typeRef;
    private final ASTNode initializer;
    private final List<FieldDecl> additionalVars;
    private final int modifiers;
    private final List<AnnotationVal> annotations = new ArrayList<>();

    private FieldDecl(SourceLocation location, String name, Resolvable typeRef,
                      ASTNode initializer, List<FieldDecl> additionalVars, int modifiers) {
        super(location);
        this.name = name;
        this.typeRef = typeRef;
        this.initializer = initializer;
        this.additionalVars = additionalVars;
        this.modifiers = modifiers;
    }

    public String getName() {
        return name;
    }

    public Resolvable getTypeRef() {
        return typeRef;
    }

    /** 便捷方法：获取类型名 */
    public String getTypeName() {
        return typeRef != null ? typeRef.name() : null;
    }

    public ASTNode getInitializer() {
        return initializer;
    }

    public int getModifiers() {
        return modifiers;
    }

    public List<FieldDecl> getAdditionalVars() { return additionalVars; }

    public boolean hasAdditionalVars() { return additionalVars != null && !additionalVars.isEmpty(); }

    public List<AnnotationVal> getAnnotations() { return annotations; }
    public void addAnnotation(AnnotationVal a) { annotations.add(a); }

    @Override
    public <T> T accept(ASTVisitor<T> visitor) {
        return visitor.visit(this);
    }

    @Override
    public String nodeName() {
        return "FieldDecl";
    }

    @Override
    public List<ASTNode> getChildren() {
        return ASTNode.children(initializer, annotations);
    }

    public static class Builder extends ASTNode.Builder<Builder> {
        private String name;
        private Resolvable typeRef;
        private ASTNode initializer;
        private List<FieldDecl> additionalVars = Collections.emptyList();
        private int modifiers;

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder typeName(String typeName) {
            this.typeRef = typeName != null ? Resolvable.byName(typeName) : null;
            return this;
        }

        public Builder typeRef(Resolvable typeRef) {
            this.typeRef = typeRef;
            return this;
        }

        public Builder initializer(ASTNode initializer) {
            this.initializer = initializer;
            return this;
        }

        public Builder additionalVars(List<FieldDecl> additionalVars) {
            this.additionalVars = additionalVars;
            return this;
        }

        public Builder modifiers(int modifiers) {
            this.modifiers = modifiers;
            return this;
        }

        public FieldDecl build() {
            return new FieldDecl(location, name, typeRef, initializer, additionalVars, modifiers);
        }
    }
}
