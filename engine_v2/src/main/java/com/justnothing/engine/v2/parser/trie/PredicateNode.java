package com.justnothing.engine.v2.parser.trie;

import com.justnothing.engine.v2.lexer.TokenType;

import java.util.List;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.function.Predicate;

/**
 * 谓词节点：按运行时条件消歧。
 * <p>
 * 当 TokenType 无法精确区分时（比如 IDENTIFIER 后面跟 DOT，
 * 可能是字段访问也可能是限定类型名），用谓词做二次判断。
 * </p>
 *
 * <h3>增强功能</h3>
 * <ul>
 *   <li>offset 感知：谓词从指定 offset 开始前瞻匹配</li>
 *   <li>matchLength：返回匹配到的模式长度，供 dispatch 计算 doFromHere 的前进量</li>
 *   <li>variablePatterns：orIfMatches 候选长度不同时，逐个检查返回实际匹配长度</li>
 * </ul>
 *
 * @param <C> 解析上下文类型
 * @param <R> 解析结果类型
 * @author JustNothing1021
 */
public final class PredicateNode<C, R> implements TrieNode<C, R> {

    private final String name;
    private final BiPredicate<TokenStream, Integer> offsetAwarePredicate;
    private final Predicate<TokenStream> legacyPredicate;
    private final LeafNode<C, R> target;
    private final int patternLength;
    /**
     * 可变长度候选模式列表（orIfMatches 长度不同时使用）。
     * 为 null 表示固定长度，使用 patternLength。
     * 不为 null 时，matchLength 逐个检查候选并返回实际匹配长度。
     */
    private final List<List<Object>> variablePatterns;

    /**
     * 创建带 offset 感知的谓词节点（推荐）
     *
     * @param name           节点名称
     * @param predicate      offset 感知谓词
     * @param action         匹配成功时的解析动作
     * @param patternLength  匹配的 token 数量（用于 doFromHere 计算 offset）
     */
    public PredicateNode(String name, BiPredicate<TokenStream, Integer> predicate,
                         LeafNode<C, R> leaf, int patternLength) {
        this.name = name;
        this.offsetAwarePredicate = predicate;
        this.legacyPredicate = null;
        this.target = leaf;
        this.patternLength = patternLength;
        this.variablePatterns = null;
    }

    /**
     * 创建带 offset 感知的谓词节点（默认 patternLength=0，不自动前进）
     */
    public PredicateNode(String name, BiPredicate<TokenStream, Integer> predicate,
                         BiFunction<C, TokenStream, R> action) {
        this(name, predicate, new LeafNode<>(name, action, false, false), 0);
    }

    /**
     * 创建传统谓词节点（兼容旧 API）
     */
    public PredicateNode(String name, Predicate<TokenStream> predicate,
                         BiFunction<C, TokenStream, R> action) {
        this.name = name;
        this.offsetAwarePredicate = null;
        this.legacyPredicate = predicate;
        this.target = new LeafNode<>(name, action);
        this.patternLength = 0;
        this.variablePatterns = null;
    }

    /**
     * 创建可变长度谓词节点（orIfMatches 候选长度不同时使用）。
     * <p>
     * matchLength 会逐个检查 variablePatterns 中的候选，
     * 返回第一个匹配的候选的长度。
     * </p>
     * <p>
     * 候选模式中的每个元素可以是 TokenType 或 TokenMatcher。
     * </p>
     *
     * @param name             节点名称
     * @param predicate        offset 感知谓词
     * @param leaf             叶子节点（已包含 action 和 advanceOnDispatch）
     * @param variablePatterns 可变长度候选模式列表
     */
    @SuppressWarnings("unchecked")
    public PredicateNode(String name, BiPredicate<TokenStream, Integer> predicate,
                         LeafNode<C, R> leaf, List<? extends List<?>> variablePatterns) {
        this.name = name;
        this.offsetAwarePredicate = predicate;
        this.legacyPredicate = null;
        this.target = leaf;
        this.patternLength = -1;  // 标记为可变长度
        this.variablePatterns = (List<List<Object>>) (List<?>) variablePatterns;
    }

    @Override
    public boolean isLeaf() {
        return false;
    }

    @Override
    public String name() {
        return name;
    }

    /**
     * 测试谓词是否匹配（带 offset）
     */
    public boolean test(TokenStream stream, int offset) {
        if (offsetAwarePredicate != null) {
            return offsetAwarePredicate.test(stream, offset);
        }
        return legacyPredicate.test(stream);
    }

    /**
     * 测试谓词是否匹配（兼容旧调用）
     */
    public boolean test(TokenStream stream) {
        return test(stream, 0);
    }

    /**
     * 返回匹配到的模式长度。
     * <p>
     * 对于固定长度的模式（如 ifMatches(INT, ID, COLON) → 3），
     * 直接返回 patternLength。
     * 对于可变长度的模式（orIfMatches 候选长度不同），
     * 逐个检查 variablePatterns 并返回实际匹配的候选长度。
     * </p>
     *
     * @param stream token 流
     * @param offset 前瞻起始偏移
     * @return 匹配的模式长度，未匹配返回 -1
     */
    public int matchLength(TokenStream stream, int offset) {
        if (variablePatterns != null) {
            // 可变长度：逐个检查候选（支持 TokenType 和 TokenMatcher）
            for (List<Object> pattern : variablePatterns) {
                if (matchesPattern(stream, pattern, offset)) {
                    return pattern.size();
                }
            }
            return -1;
        }
        // 固定长度
        if (test(stream, offset)) {
            return patternLength;
        }
        return -1;
    }

    /**
     * 执行解析动作（兼容旧调用）
     */
    public R parse(C context, TokenStream stream) {
        return target.parse(context, stream);
    }

    /**
     * 执行解析动作（offset 感知版）。
     * <p>
     * 将 offset 传递给内部 LeafNode，由 LeafNode 决定是否前进 stream。
     * </p>
     *
     * @param context 解析上下文
     * @param stream  token 流
     * @param offset  dispatch 累积的前瞻偏移量（trie 深度 + 谓词匹配长度）
     */
    public R parse(C context, TokenStream stream, int offset) {
        return target.parse(context, stream, offset);
    }

    /**
     * 获取内部叶子节点
     */
    public LeafNode<C, R> target() {
        return target;
    }

    /**
     * 获取模式长度（固定长度模式；可变长度返回 -1）
     */
    public int patternLength() {
        return patternLength;
    }

    /**
     * 是否为可变长度模式
     */
    public boolean isVariableLength() {
        return variablePatterns != null;
    }

    /**
     * 检查模式是否匹配（支持 TokenType 和 TokenMatcher 混合）。
     */
    private boolean matchesPattern(TokenStream stream, List<Object> pattern, int offset) {
        for (int i = 0; i < pattern.size(); i++) {
            Object elem = pattern.get(i);
            TokenType actual = stream.peekType(offset + i);
            if (elem instanceof TokenMatcher tm) {
                if (!tm.matches(actual)) return false;
            } else if (elem instanceof TokenType tt) {
                if (actual != tt) return false;
            }
        }
        return true;
    }

    @Override
    public String toString() {
        if (variablePatterns != null) {
            return "Predicate[" + name + ", varLen=" + variablePatterns.size() + " patterns]";
        }
        return "Predicate[" + name + ", len=" + patternLength + "]";
    }
}
