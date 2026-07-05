package com.justnothing.engine.v2.ast.decl;

import com.justnothing.engine.v2.ast.ASTNode;
import com.justnothing.engine.v2.ast.ASTVisitor;
import com.justnothing.engine.v2.ast.SourceLocation;

import java.util.List;

public class ImportDecl extends ASTNode {

    private final String importPath;
    private final boolean isStatic;
    private final boolean isWildcard;

    private ImportDecl(SourceLocation location, String importPath, boolean isStatic, boolean isWildcard) {
        super(location);
        this.importPath = importPath;
        this.isStatic = isStatic;
        this.isWildcard = isWildcard;
    }

    public String getImportPath() {
        return importPath;
    }

    public boolean isStatic() {
        return isStatic;
    }

    public boolean isWildcard() {
        return isWildcard;
    }

    @Override
    public <T> T accept(ASTVisitor<T> visitor) {
        return visitor.visit(this);
    }

    @Override
    public String nodeName() {
        return "Import";
    }

    @Override
    public List<ASTNode> getChildren() {
        return ASTNode.children();
    }

    public static class Builder extends ASTNode.Builder<Builder> {
        private String importPath;
        private boolean isStatic;
        private boolean isWildcard;

        public Builder importPath(String importPath) {
            this.importPath = importPath;
            return this;
        }

        public Builder isStatic(boolean isStatic) {
            this.isStatic = isStatic;
            return this;
        }

        public Builder isWildcard(boolean isWildcard) {
            this.isWildcard = isWildcard;
            return this;
        }

        public ImportDecl build() {
            return new ImportDecl(location, importPath, isStatic, isWildcard);
        }
    }
}
