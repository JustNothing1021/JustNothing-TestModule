package com.justnothing.engine.v2.scope;

import com.justnothing.engine.v2.ast.ASTNode;

import java.util.HashMap;
import java.util.Map;

public class BaseScope implements Scope {

    private final Map<String, ASTNode> symbols = new HashMap<>();
    private Scope parent;

    public BaseScope() {
    }

    public BaseScope(Scope parent) {
        this.parent = parent;
    }

    @Override
    public void define(String name, ASTNode declaration) {
        symbols.put(name, declaration);
    }

    @Override
    public ASTNode resolveLocal(String name) {
        return symbols.get(name);
    }

    @Override
    public ASTNode resolve(String name) {
        ASTNode result = symbols.get(name);
        if (result != null) return result;
        if (parent != null) return parent.resolve(name);
        return null;
    }

    @Override
    public Scope getParent() {
        return parent;
    }

    @Override
    public void setParent(Scope parent) {
        this.parent = parent;
    }
}
