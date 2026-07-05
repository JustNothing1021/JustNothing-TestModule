package com.justnothing.engine.v2.ast;

/**
 * 包级桥接：供外部 Resolver 调用 ASTNode 包级私有方法。
 */
public class ResolverAccess {

    public static void setBinding(ASTNode node, ASTNode binding) {
        node.setBinding(binding);
    }

    public static void setResolvedType(ASTNode node, com.justnothing.engine.v2.type.Type type) {
        node.setResolvedType(type);
    }
}
