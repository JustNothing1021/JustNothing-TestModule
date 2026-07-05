package com.justnothing.engine.v2.parser.trie;

/**
 * Trie 决策树节点。
 * <p>
 * 三种实现：
 * <ul>
 *   <li>{@link BranchNode} — 内部分发节点，按 TokenType 或谓词走子树</li>
 *   <li>{@link LeafNode} — 叶子节点，持有具体的解析动作</li>
 *   <li>{@link PredicateNode} — 谓词节点，按运行时条件消歧</li>
 * </ul>
 * </p>
 *
 * @param <C> 解析上下文类型
 * @param <R> 解析结果类型
 * @author JustNothing1021
 */
public sealed interface TrieNode<C, R> permits BranchNode, LeafNode, PredicateNode {

    /**
     * 是否为叶子节点（可直接执行解析动作）
     */
    boolean isLeaf();

    /**
     * 节点名称，用于调试和可视化
     */
    String name();
}
