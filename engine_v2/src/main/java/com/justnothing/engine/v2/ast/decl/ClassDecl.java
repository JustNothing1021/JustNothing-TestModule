package com.justnothing.engine.v2.ast.decl;

import com.justnothing.engine.v2.ast.ASTNode;
import com.justnothing.engine.v2.ast.ASTVisitor;
import com.justnothing.engine.v2.ast.SourceLocation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ClassDecl extends ASTNode {

    private final String name;
    private final ClassKind kind;
    private final List<String> typeParameters;
    private final List<ParamDecl> recordComponents;
    private final String superClassName;
    private final List<String> interfaces;
    private final List<String> permitted;
    private final List<String> enumConstants;
    private final int modifiers;
    private final List<ASTNode> members;
    private final List<AnnotationVal> annotations = new ArrayList<>();

    private ClassDecl(SourceLocation location, String name, ClassKind kind,
                      List<String> typeParameters, List<ParamDecl> recordComponents,
                      String superClassName, List<String> interfaces,
                      List<String> permitted, List<String> enumConstants,
                      int modifiers, List<ASTNode> members) {
        super(location);
        this.name = name;
        this.kind = kind;
        this.typeParameters = typeParameters;
        this.recordComponents = recordComponents;
        this.superClassName = superClassName;
        this.interfaces = interfaces;
        this.permitted = permitted;
        this.enumConstants = enumConstants;
        this.modifiers = modifiers;
        this.members = members;
    }

    public String getName() {
        return name;
    }

    public ClassKind getKind() {
        return kind;
    }

    public boolean isInterface() { return kind == ClassKind.INTERFACE; }
    public boolean isEnum() { return kind == ClassKind.ENUM; }
    public boolean isRecord() { return kind == ClassKind.RECORD; }

    public List<String> getTypeParameters() { return typeParameters; }

    public List<ParamDecl> getRecordComponents() { return recordComponents; }

    public String getSuperClassName() {
        return superClassName;
    }

    public List<String> getInterfaces() {
        return interfaces;
    }

    public List<String> getPermitted() {
        return permitted;
    }

    public List<String> getEnumConstants() {
        return enumConstants;
    }

    public boolean hasEnumConstants() {
        return enumConstants != null;
    }

    public int getModifiers() {
        return modifiers;
    }

    public List<ASTNode> getMembers() {
        return members;
    }

    public List<AnnotationVal> getAnnotations() { return annotations; }
    public void addAnnotation(AnnotationVal a) { annotations.add(a); }

    @Override
    public <T> T accept(ASTVisitor<T> visitor) {
        return visitor.visit(this);
    }

    @Override
    public String nodeName() {
        return "ClassDecl";
    }

    @Override
    public List<ASTNode> getChildren() {
        return ASTNode.children(members, annotations);
    }

    public static class Builder extends ASTNode.Builder<Builder> {
        private String name;
        private ClassKind kind = ClassKind.CLASS;
        private List<String> typeParameters = Collections.emptyList();
        private List<ParamDecl> recordComponents = Collections.emptyList();
        private String superClassName;
        private List<String> interfaces;
        private List<String> permitted;
        private List<String> enumConstants;
        private int modifiers;
        private List<ASTNode> members;

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder kind(ClassKind kind) {
            this.kind = kind;
            return this;
        }

        public Builder typeParameters(List<String> typeParameters) {
            this.typeParameters = typeParameters;
            return this;
        }

        public Builder recordComponents(List<ParamDecl> recordComponents) {
            this.recordComponents = recordComponents;
            return this;
        }

        public Builder superClassName(String superClassName) {
            this.superClassName = superClassName;
            return this;
        }

        public Builder interfaces(List<String> interfaces) {
            this.interfaces = interfaces;
            return this;
        }

        public Builder permitted(List<String> permitted) {
            this.permitted = permitted;
            return this;
        }

        public Builder enumConstants(List<String> enumConstants) {
            this.enumConstants = enumConstants;
            return this;
        }

        public Builder modifiers(int modifiers) {
            this.modifiers = modifiers;
            return this;
        }

        public Builder members(List<ASTNode> members) {
            this.members = members;
            return this;
        }

        public ClassDecl build() {
            return new ClassDecl(location, name, kind, typeParameters, recordComponents,
                    superClassName, interfaces, permitted, enumConstants, modifiers, members);
        }
    }
}
