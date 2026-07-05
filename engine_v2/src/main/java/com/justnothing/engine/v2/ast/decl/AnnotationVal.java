package com.justnothing.engine.v2.ast.decl;

import com.justnothing.engine.v2.ast.ASTNode;
import com.justnothing.engine.v2.ast.ASTVisitor;
import com.justnothing.engine.v2.ast.SourceLocation;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class AnnotationVal extends ASTNode {

    private final String name;
    private final Map<String, ASTNode> attributes;

    private AnnotationVal(SourceLocation location, String name, Map<String, ASTNode> attributes) {
        super(location);
        this.name = name;
        this.attributes = attributes;
    }

    public String getName() {
        return name;
    }

    public Map<String, ASTNode> getAttributes() {
        return attributes;
    }

    @Override
    public <T> T accept(ASTVisitor<T> visitor) {
        return visitor.visit(this);
    }

    @Override
    public String nodeName() {
        return "Annotation";
    }

    @Override
    public List<ASTNode> getChildren() {
        if (attributes == null || attributes.isEmpty()) return List.of();
        return ASTNode.children(new ArrayList<>(attributes.values()));
    }

    public static class Builder extends ASTNode.Builder<Builder> {
        private String name;
        private Map<String, ASTNode> attributes;

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder attributes(Map<String, ASTNode> attributes) {
            this.attributes = attributes;
            return this;
        }

        public AnnotationVal build() {
            return new AnnotationVal(location, name, attributes);
        }
    }
}
