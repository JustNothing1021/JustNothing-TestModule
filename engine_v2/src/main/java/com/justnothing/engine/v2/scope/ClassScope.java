package com.justnothing.engine.v2.scope;

import com.justnothing.engine.v2.ast.decl.ClassDecl;

/** 类作用域，持有对 ClassDecl 的引用 */
public class ClassScope extends BaseScope {

    private final ClassDecl classDecl;

    public ClassScope(Scope parent, ClassDecl classDecl) {
        super(parent);
        this.classDecl = classDecl;
    }

    public ClassDecl getClassDecl() {
        return classDecl;
    }
}
