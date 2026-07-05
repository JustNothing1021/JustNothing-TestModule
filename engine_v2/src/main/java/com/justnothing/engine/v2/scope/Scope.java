package com.justnothing.engine.v2.scope;

import com.justnothing.engine.v2.ast.ASTNode;

/**
 * 作用域接口。
 * <p>
 * 支持按名称查找声明，遵循词法作用域链。
 * </p>
 */
public interface Scope {

    /** 将声明注册到当前作用域 */
    void define(String name, ASTNode declaration);

    /** 在当前作用域查找（不回溯父作用域） */
    ASTNode resolveLocal(String name);

    /** 沿作用域链向上查找（当前 → 父 → 祖父 → …） */
    ASTNode resolve(String name);

    /** 获取父作用域 */
    Scope getParent();

    /** 设置父作用域 */
    void setParent(Scope parent);
}
