package com.justnothing.engine.v2.ast.decl;

import com.justnothing.engine.v2.ast.ASTNode;
import com.justnothing.engine.v2.ast.ASTVisitor;
import com.justnothing.engine.v2.ast.Resolvable;
import com.justnothing.engine.v2.ast.SourceLocation;

import java.util.Collections;
import java.util.List;

public class ParamDecl extends ASTNode {

    private final String name;
    private final Resolvable typeRef;
    private final ASTNode defaultValue;
    private final boolean isFinal;
    private final List<AnnotationVal> annotations;

    private ParamDecl(SourceLocation location, String name, Resolvable typeRef, ASTNode defaultValue, boolean isFinal, List<AnnotationVal> annotations) {
        super(location);
        this.name = name;
        this.typeRef = typeRef;
        this.defaultValue = defaultValue;
        this.isFinal = isFinal;
        this.annotations = annotations;
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

    public ASTNode getDefaultValue() {
        return defaultValue;
    }

    public boolean isFinal() { return isFinal; }

    public List<AnnotationVal> getAnnotations() {
        return annotations;
    }

    @Override
    public <T> T accept(ASTVisitor<T> visitor) {
        return visitor.visit(this);
    }

    @Override
    public String nodeName() {
        return "Param";
    }

    @Override
    public List<ASTNode> getChildren() {
        return ASTNode.children(defaultValue, annotations);
    }

    public static class Builder extends ASTNode.Builder<Builder> {
        private String name;
        private Resolvable typeRef;
        private ASTNode defaultValue;
        private boolean isFinal;
        private List<AnnotationVal> annotations;

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

        public Builder defaultValue(ASTNode defaultValue) {
            this.defaultValue = defaultValue;
            return this;
        }

        public Builder isFinal(boolean isFinal) {
            this.isFinal = isFinal;
            return this;
        }

        public Builder annotations(List<AnnotationVal> annotations) {
            this.annotations = annotations;
            return this;
        }

        public ParamDecl build() {
            return new ParamDecl(location, name, typeRef, defaultValue, isFinal,
                    annotations != null ? annotations : Collections.emptyList());
        }
    }
}
