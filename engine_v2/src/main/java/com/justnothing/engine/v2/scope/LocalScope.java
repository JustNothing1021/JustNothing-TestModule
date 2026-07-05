package com.justnothing.engine.v2.scope;

/** 局部作用域（方法体、块、循环体等） */
public class LocalScope extends BaseScope {
    public LocalScope(Scope parent) {
        super(parent);
    }
}
