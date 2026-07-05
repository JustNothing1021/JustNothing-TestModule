package com.justnothing.engine.v2.parser.trie;

import com.justnothing.engine.v2.lexer.TokenType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.BiFunction;

/**
 * 前瞻决策构建器：在 Trie 的某个节点上定义"如果后续 token 匹配 A 则走左，否则走右"。
 * <p>
 * 核心思想：不是在第一个 token 就分发，而是沿着 token 流走多步，
 * 在"分水岭"处分叉——零回溯，O(1) 决策。
 * </p>
 *
 * <h3>典型用例：for vs for-each 消歧</h3>
 * <pre>{@code
 * // for (int i = 0; i < 10; i++) {}  ← 传统 for
 * // for (int i : list) {}            ← for-each
 * // 分水岭在第 4 个 token：= vs :
 *
 * .path(KEYWORD_FOR, DELIMITER_LEFT_PAREN)
 *     .decision("for-dispatch")
 *         // for-each: Type var :
 *         .ifMatches(KEYWORD_INT, IDENTIFIER, OPERATOR_COLON)
 *         .orIfMatches(QUALIFIED_NAME, IDENTIFIER, OPERATOR_COLON)
 *         .orIfMatches(IDENTIFIER, OPERATOR_COLON)  // var i : (无类型)
 *             .doAction("forEach", (ctx, s) -> parseForEach(ctx, s))
 *         .otherwise()
 *             .doAction("forClassic", (ctx, s) -> parseClassicFor(ctx, s))
 *     .endDecision()
 * .end()
 * }</pre>
 * <p>
 * 注：因为这个东西直接操作 Token 流实在太费劲了，所以最后我给这东西当高级的 switch case 用了（误）
 * 后续的话，应该会加强一下谓词节点对 TokenStream 的可操作性（如果我还记得这东西的话）
 * </p>
 *
 * @param <C> 解析上下文类型
 * @param <R> 解析结果类型
 * @author JustNothing1021
 */
public class DecisionBuilder<C, R> {

    private final TrieBuilder<C, R>.PathBuilder parent;
    private final BranchNode<C, R> decisionPoint;
    private final List<Pattern<C, R>> patterns;
    private Pattern<C, R> currentPattern;
    private boolean hasOtherwise;

    DecisionBuilder(TrieBuilder<C, R>.PathBuilder parent, BranchNode<C, R> decisionPoint) {
        this.parent = parent;
        this.decisionPoint = decisionPoint;
        this.patterns = new ArrayList<>();
        this.hasOtherwise = false;
    }

    /**
     * 定义一个前瞻匹配模式：从当前位置开始，如果后续 token 序列匹配指定类型，
     * 则走此路径。
     * <p>
     * 匹配规则：从 decisionPoint 对应的 offset 开始，
     * 逐个检查 stream.peekType(offset + i) == pattern[i]。
     * </p>
     *
     * @param types 期望的 token 类型序列
     */
    public DecisionBuilder<C, R> ifMatches(TokenType... types) {
        currentPattern = new Pattern<>(Arrays.asList(types));
        patterns.add(currentPattern);
        return this;
    }

    /**
     * 在当前 ifMatches 组中添加另一个候选模式。
     * <p>
     * 多个 ifMatches/orIfMatches 之间是"或"的关系——
     * 只要有任何一个匹配，就走此路径。
     * </p>
     */
    public DecisionBuilder<C, R> orIfMatches(TokenType... types) {
        if (currentPattern == null) {
            throw new IllegalStateException("orIfMatches 必须在 ifMatches 之后调用");
        }
        currentPattern.addAlternative(Arrays.asList(types));
        return this;
    }

    /**
     * 为当前 ifMatches 组设置匹配成功时的解析动作。
     */
    public DecisionBuilder<C, R> doAction(String name, BiFunction<C, TokenStream, R> action) {
        if (currentPattern == null) {
            throw new IllegalStateException("doAction 必须在 ifMatches 之后调用");
        }
        currentPattern.setAction(name, action);
        currentPattern = null;
        return this;
    }

    /**
     * 定义"所有 ifMatches 都不匹配时"的兜底路径。
     * <p>
     * 只能调用一次，且必须在所有 ifMatches 之后。
     * </p>
     */
    public DecisionBuilder<C, R> otherwise() {
        if (hasOtherwise) {
            throw new IllegalStateException("otherwise 只能调用一次");
        }
        hasOtherwise = true;
        return this;
    }

    /**
     * 为 otherwise 设置解析动作。
     */
    public DecisionBuilder<C, R> doOtherwise(String name, BiFunction<C, TokenStream, R> action) {
        if (!hasOtherwise) {
            throw new IllegalStateException("必须先调用 otherwise()");
        }
        // otherwise 作为 fallback 挂在 decisionPoint 上
        decisionPoint.setFallback(new LeafNode<>(name, action));
        return this;
    }

    /**
     * 结束决策定义，回到 PathBuilder。
     * <p>
     * 将所有 ifMatches 模式注册为 decisionPoint 上的 PredicateNode。
     * </p>
     */
    public TrieBuilder<C, R>.PathBuilder endDecision() {
        if (currentPattern != null) {
            throw new IllegalStateException("最后一个 ifMatches 缺少 doAction");
        }

        // 将每个 Pattern 注册为 decisionPoint 上的 PredicateNode
        for (Pattern<C, R> pattern : patterns) {
            decisionPoint.addPredicate(pattern.toPredicateNode());
        }

        return parent;
    }

    // ========== 内部 Pattern 类 ==========

    /**
     * 一个前瞻匹配模式，包含多个候选 token 序列和一个解析动作。
     */
    private static class Pattern<C, R> {
        private final List<List<TokenType>> alternatives;
        private String name;
        private BiFunction<C, TokenStream, R> action;

        Pattern(List<TokenType> firstAlternative) {
            this.alternatives = new ArrayList<>();
            this.alternatives.add(firstAlternative);
        }

        void addAlternative(List<TokenType> alt) {
            alternatives.add(alt);
        }

        void setAction(String name, BiFunction<C, TokenStream, R> action) {
            this.name = name;
            this.action = action;
        }

        /**
         * 将此 Pattern 转换为 PredicateNode。
         * <p>
         * 谓词逻辑：从 stream 的 offset 位置开始前瞻，
         * 检查是否有任何一个候选序列匹配。
         * </p>
         */
        PredicateNode<C, R> toPredicateNode() {
            List<List<TokenType>> immutableAlts = List.copyOf(alternatives);

            return new PredicateNode<>(
                    name,
                    (stream, offset) -> {
                        for (List<TokenType> alt : immutableAlts) {
                            if (matchesPattern(stream, alt, offset)) {
                                return true;
                            }
                        }
                        return false;
                    },
                    action
            );
        }

        /**
         * 检查 stream 从指定 offset 开始是否匹配 token 类型序列。
         */
        private boolean matchesPattern(TokenStream stream, List<TokenType> pattern, int offset) {
            for (int i = 0; i < pattern.size(); i++) {
                if (stream.peekType(offset + i) != pattern.get(i)) {
                    return false;
                }
            }
            return true;
        }
    }
}
