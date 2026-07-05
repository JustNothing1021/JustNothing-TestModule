package com.justnothing.engine.v2.parser.trie;

import java.util.function.BiFunction;

/**
 * 叶子节点：持有具体的解析动作。
 * <p>
 * 当决策树走到叶子节点时，执行 {@link #action} 产生解析结果。
 * </p>
 *
 * @param <C> 解析上下文类型
 * @param <R> 解析结果类型
 * @author JustNothing1021
 */
public final class LeafNode<C, R> implements TrieNode<C, R> {

    private final String name;
    private final BiFunction<C, TokenStream, R> action;
    private final boolean needsBacktrack;
    private final boolean advanceOnDispatch;

    /**
     * @param name          节点名称（调试用）
     * @param action        解析动作：(context, tokenStream) → 解析结果
     * @param needsBacktrack 此路径是否需要回退能力（savePosition/restorePosition）
     */
    public LeafNode(String name, BiFunction<C, TokenStream, R> action, boolean needsBacktrack) {
        this(name, action, needsBacktrack, false);
    }

    public LeafNode(String name, BiFunction<C, TokenStream, R> action) {
        this(name, action, false, false);
    }

    /**
     * 完整构造函数
     *
     * @param name              节点名称
     * @param action            解析动作
     * @param needsBacktrack    是否需要回退能力
     * @param advanceOnDispatch doFromHere 时为 true：dispatch 到达此叶子后，
     *                          自动将 stream 前进 offset 个 token 再调用 action；
     *                          doFromRoot 时为 false：不前进，action 从头解析
     */
    public LeafNode(String name, BiFunction<C, TokenStream, R> action,
                    boolean needsBacktrack, boolean advanceOnDispatch) {
        this.name = name;
        this.action = action;
        this.needsBacktrack = needsBacktrack;
        this.advanceOnDispatch = advanceOnDispatch;
    }

    @Override
    public boolean isLeaf() {
        return true;
    }

    @Override
    public String name() {
        return name;
    }

    /**
     * 执行解析动作（兼容旧调用，不自动前进 stream）
     */
    public R parse(C context, TokenStream stream) {
        return action.apply(context, stream);
    }

    /**
     * 执行解析动作（offset 感知版）。
     * <p>
     * 如果 advanceOnDispatch=true（doFromHere），先将 stream 前进 offset 个 token，
     * 再调用 action。否则直接调用 action（doFromRoot）。
     * </p>
     *
     * @param context 解析上下文
     * @param stream  token 流
     * @param offset  dispatch 过程中累积的前瞻偏移量
     */
    public R parse(C context, TokenStream stream, int offset) {
        if (advanceOnDispatch && offset > 0) {
            for (int i = 0; i < offset; i++) {
                stream.advance();
            }
        }
        return action.apply(context, stream);
    }

    /**
     * 此路径是否需要回退能力
     */
    public boolean needsBacktrack() {
        return needsBacktrack;
    }

    /**
     * 是否在 dispatch 时自动前进 stream（doFromHere）
     */
    public boolean advanceOnDispatch() {
        return advanceOnDispatch;
    }

    @Override
    public String toString() {
        String mode = advanceOnDispatch ? "fromHere" : "fromRoot";
        return "Leaf[" + name + ", " + mode + "]";
    }
}
