package com.justnothing.engine.v2.ast.decl;

import com.justnothing.engine.v2.ast.ASTNode;
import com.justnothing.engine.v2.ast.ASTVisitor;
import com.justnothing.engine.v2.ast.SourceLocation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ConstructorDecl extends ASTNode {

    private final String name;
    private final List<String> typeParameters;
    private final List<ParamDecl> parameters;
    private final List<String> throwsList;
    private final ASTNode body;
    private final int modifiers;
    private final List<AnnotationVal> annotations;

    private ConstructorDecl(SourceLocation location, String name,
                            List<String> typeParameters,
                            List<ParamDecl> parameters, List<String> throwsList,
                            ASTNode body, int modifiers) {
        super(location);
        this.name = name;
        this.typeParameters = typeParameters;
        this.parameters = parameters;
        this.throwsList = throwsList;
        this.body = body;
        this.modifiers = modifiers;
        this.annotations = new ArrayList<>();
    }

    public String getName() {
        return name;
    }

    public List<String> getTypeParameters() { return typeParameters; }

    public List<ParamDecl> getParameters() {
        return parameters;
    }

    public List<String> getThrowsList() {
        return throwsList;
    }

    public ASTNode getBody() {
        return body;
    }

    public int getModifiers() {
        return modifiers;
    }

    public List<AnnotationVal> getAnnotations() {
        return annotations;
    }

    public void addAnnotation(AnnotationVal ann) {
        annotations.add(ann);
    }

    @Override
    public <T> T accept(ASTVisitor<T> visitor) {
        return visitor.visit(this);
    }

    @Override
    public String nodeName() {
        return "ConstructorDecl";
    }

    @Override
    public List<ASTNode> getChildren() {
        return ASTNode.children(parameters, body, annotations);
    }

    public static class Builder extends ASTNode.Builder<Builder> {
        private String name;
        private List<String> typeParameters = Collections.emptyList();
        private List<ParamDecl> parameters;
        private List<String> throwsList;
        private ASTNode body;
        private int modifiers;
        private List<AnnotationVal> annotations = List.of();

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder typeParameters(List<String> typeParameters) {
            this.typeParameters = typeParameters;
            return this;
        }

        public Builder parameters(List<ParamDecl> parameters) {
            this.parameters = parameters;
            return this;
        }

        public Builder throwsList(List<String> throwsList) {
            this.throwsList = throwsList;
            return this;
        }

        public Builder body(ASTNode body) {
            this.body = body;
            return this;
        }

        public Builder modifiers(int modifiers) {
            this.modifiers = modifiers;
            return this;
        }

        public Builder annotations(List<AnnotationVal> annotations) {
            this.annotations = annotations;
            return this;
        }

        public ConstructorDecl build() {
            ConstructorDecl d = new ConstructorDecl(location, name, typeParameters, parameters, throwsList, body, modifiers);
            if (annotations != null) {
                for (AnnotationVal a : annotations) d.addAnnotation(a);
            }
            return d;
        }
    }
}
