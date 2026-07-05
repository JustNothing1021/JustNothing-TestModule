package com.justnothing.engine.v2.parser.trie;

import com.justnothing.engine.v2.lexer.TokenType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * 内部分发节点：按 TokenType + 谓词走子树。
 * <p>
 * 查询优先级：
 * <ol>
 *   <li>TokenType 精确匹配（{@link #children}）— O(1)</li>
 *   <li>谓词匹配（{@link #predicates}）— 按注册顺序 O(n)</li>
 *   <li>Fallback 兜底</li>
 * </ol>
 * </p>
 *
 * @param <C> 解析上下文类型
 * @param <R> 解析结果类型
 * @author JustNothing1021
 */
public final class BranchNode<C, R> implements TrieNode<C, R> {

    private final String name;
    private final Map<TokenType, TrieNode<C, R>> children;
    private final List<PredicateNode<C, R>> predicates;
    private TrieNode<C, R> fallback;

    public BranchNode(String name) {
        this.name = name;
        this.children = new EnumMap<>(TokenType.class);
        this.predicates = new ArrayList<>();
    }

    @Override
    public boolean isLeaf() {
        return false;
    }

    @Override
    public String name() {
        return name;
    }

    // ========== 子节点管理 ==========

    /**
     * 添加 TokenType 精确匹配的子节点
     */
    public void addChild(TokenType type, TrieNode<C, R> child) {
        children.put(type, child);
    }

    /**
     * 添加谓词子节点（TokenType 无法区分时的二次判断）
     */
    public void addPredicate(PredicateNode<C, R> predicate) {
        predicates.add(predicate);
    }

    /**
     * 设置 fallback 节点（当精确匹配和谓词都未命中时使用）
     */
    public void setFallback(TrieNode<C, R> fallback) {
        this.fallback = fallback;
    }

    // ========== 查询 ==========

    /**
     * 按 token 类型 + TokenStream 获取子节点。
     * <p>
     * 查询顺序：精确匹配 → 谓词匹配 → fallback
     * </p>
     *
     * @param type   当前前瞻位置的 token 类型
     * @param stream token 流
     * @param offset 当前前瞻偏移量（谓词需要知道从哪里开始检查）
     */
    public TrieNode<C, R> next(TokenType type, TokenStream stream, int offset) {
        // 1. 精确匹配
        TrieNode<C, R> child = children.get(type);
        if (child != null) return child;

        // 2. 谓词匹配（按注册顺序）
        for (PredicateNode<C, R> pred : predicates) {
            if (pred.test(stream, offset)) {
                return pred;
            }
        }

        // 3. Fallback
        return fallback;
    }

    /**
     * 按 token 类型 + TokenStream 获取子节点（不传 offset，兼容旧调用）。
     */
    public TrieNode<C, R> next(TokenType type, TokenStream stream) {
        return next(type, stream, 0);
    }

    /**
     * 仅按 token 类型获取子节点（不执行谓词）。
     * 用于不需要谓词消歧的简单场景。
     */
    public TrieNode<C, R> next(TokenType type) {
        TrieNode<C, R> child = children.get(type);
        return child != null ? child : fallback;
    }

    /**
     * 是否有指定 token 类型的子节点
     */
    public boolean hasChild(TokenType type) {
        return children.containsKey(type);
    }

    // ========== 只读视图 ==========

    public Map<TokenType, TrieNode<C, R>> children() {
        return Collections.unmodifiableMap(children);
    }

    public List<PredicateNode<C, R>> predicates() {
        return Collections.unmodifiableList(predicates);
    }

    public TrieNode<C, R> fallback() {
        return fallback;
    }

    @Override
    public String toString() {
        return "Branch[" + name
            + ", children=" + children.size()
            + ", predicates=" + predicates.size()
            + ", fallback=" + (fallback != null) + "]";
    }
}
