package com.justnothing.engine.v2.ast.decl;

import com.justnothing.engine.v2.ast.ASTNode;
import com.justnothing.engine.v2.ast.ASTVisitor;
import com.justnothing.engine.v2.ast.SourceLocation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MethodDecl extends ASTNode {

    private final String name;
    private final List<String> typeParameters;
    private final String returnType;
    private final List<ParamDecl> parameters;
    private final List<String> throwsList;
    private final ASTNode body;
    private final ASTNode defaultValue;
    private final int modifiers;
    private final List<AnnotationVal> annotations = new ArrayList<>();

    private MethodDecl(SourceLocation location, String name, List<String> typeParameters,
                       String returnType,
                       List<ParamDecl> parameters, List<String> throwsList,
                       ASTNode body, ASTNode defaultValue, int modifiers) {
        super(location);
        this.name = name;
        this.typeParameters = typeParameters;
        this.returnType = returnType;
        this.parameters = parameters;
        this.throwsList = throwsList;
        this.body = body;
        this.defaultValue = defaultValue;
        this.modifiers = modifiers;
    }

    public String getName() {
        return name;
    }

    public List<String> getTypeParameters() { return typeParameters; }

    public String getReturnType() {
        return returnType;
    }

    public List<ParamDecl> getParameters() {
        return parameters;
    }

    public List<String> getThrowsList() {
        return throwsList;
    }

    public ASTNode getBody() {
        return body;
    }

    public ASTNode getDefaultValue() {
        return defaultValue;
    }

    public int getModifiers() {
        return modifiers;
    }

    public List<AnnotationVal> getAnnotations() { return annotations; }
    public void addAnnotation(AnnotationVal a) { annotations.add(a); }

    @Override
    public <T> T accept(ASTVisitor<T> visitor) {
        return visitor.visit(this);
    }

    @Override
    public String nodeName() {
        return "MethodDecl";
    }

    @Override
    public List<ASTNode> getChildren() {
        return ASTNode.children(parameters, body, annotations);
    }

    public static class Builder extends ASTNode.Builder<Builder> {
        private String name;
        private List<String> typeParameters = Collections.emptyList();
        private String returnType;
        private List<ParamDecl> parameters;
        private List<String> throwsList;
        private ASTNode body;
        private ASTNode defaultValue;
        private int modifiers;

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder typeParameters(List<String> typeParameters) {
            this.typeParameters = typeParameters;
            return this;
        }

        public Builder returnType(String returnType) {
            this.returnType = returnType;
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

        public Builder defaultValue(ASTNode defaultValue) {
            this.defaultValue = defaultValue;
            return this;
        }

        public Builder modifiers(int modifiers) {
            this.modifiers = modifiers;
            return this;
        }

        public MethodDecl build() {
            return new MethodDecl(location, name, typeParameters, returnType, parameters, throwsList, body, defaultValue, modifiers);
        }
    }
}
