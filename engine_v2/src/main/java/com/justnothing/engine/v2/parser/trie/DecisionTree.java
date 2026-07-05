package com.justnothing.engine.v2.parser.trie;

import com.justnothing.engine.v2.lexer.TokenType;

import java.util.Map;

/**
 * 增强版决策树：支持嵌套 ifMatches、doFromHere/doFromRoot、mount 等高级特性。
 * <p>
 * 相比 {@link ParseTrie}，DecisionTree 的核心增强：
 * <ul>
 *   <li>嵌套决策：ifMatches 内可再嵌套 ifMatches，形成深层前瞻决策链</li>
 *   <li>doFromHere/doFromRoot：叶子动作可指定 TokenStream 的起始位置</li>
 *   <li>offset 感知：dispatch 自动追踪前瞻偏移量，精确计算 stream 前进量</li>
 * </ul>
 * </p>
 *
 * <h3>典型用例：for vs for-each 消歧</h3>
 * <pre>{@code
 * DecisionTree<ParseContext, ASTNode> tree = DecisionTree.builder("stmt")
 *     .ifMatches(KEYWORD_FOR, DELIMITER_LEFT_PAREN)
 *         .ifMatches(KEYWORD_INT, IDENTIFIER, OPERATOR_COLON)
 *         .orIfMatches(QUALIFIED_NAME, IDENTIFIER, OPERATOR_COLON)
 *         .orIfMatches(IDENTIFIER, OPERATOR_COLON)
 *             .doFromHere("forEach", (ctx, s) -> parseForEach(ctx, s))
 *         .otherwise()
 *             .doFromRoot("forClassic", (ctx, s) -> parseClassicFor(ctx, s))
 *         .endIf()
 *     .endIf()
 *     .build();
 * }</pre>
 *
 * @param <C> 解析上下文类型
 * @param <R> 解析结果类型
 * @author JustNothing1021
 */
public class DecisionTree<C, R> {

    private final BranchNode<C, R> root;

    DecisionTree(BranchNode<C, R> root) {
        this.root = root;
    }

    public static <C, R> DecisionTreeBuilder<C, R> builder(String name) {
        return new DecisionTreeBuilder<>(name);
    }

    /**
     * 核心分发方法：沿决策树走，找到匹配的叶子节点并执行解析。
     * <p>
     * 算法（使用前瞻，不消费 token 直到到达叶子）：
     * <ol>
     *   <li>从根节点开始，用 peek(offset) 按 token 类型查找子节点</li>
     *   <li>精确匹配优先，谓词次之，fallback 兜底</li>
     *   <li>如果找到 BranchNode，offset++ 继续深入</li>
     *   <li>如果找到 PredicateNode，计算 matchLength，offset += matchLength</li>
     *   <li>如果找到 LeafNode，根据 advanceOnDispatch 决定是否前进 stream</li>
     * </ol>
     *
     * <h4>offset 计算</h4>
     * <ul>
     *   <li>doFromHere：totalOffset = trieDepth + predicateMatchLength</li>
     *   <li>doFromRoot：totalOffset = 0（不前进 stream）</li>
     *   <li>otherwise 分支：totalOffset = trieDepth（不含谓词匹配）</li>
     * </ul>
     * </p>
     */
    public R dispatch(C context, TokenStream stream) {
        TrieNode<C, R> node = root;
        int offset = 0;

        while (true) {
            // 叶子节点：直接执行
            if (node.isLeaf()) {
                assert node instanceof LeafNode<C, R>;
                return ((LeafNode<C, R>) node).parse(context, stream, offset);
            }

            // 分支节点：按 token 类型查找子节点
            if (node instanceof BranchNode<C, R> branch) {
                TokenType type = stream.peekType(offset);
                TrieNode<C, R> next = branch.next(type, stream, offset);

                if (next == null) {
                    throw new TrieDispatchException(
                        "No matching path at branch '" + branch.name()
                            + "' for token " + stream.peek(offset)
                            + " (lookahead offset=" + offset + ")"
                    );
                }

                boolean nextIsFallback = (next == branch.fallback());

                if (next.isLeaf()) {
                    int leafOffset = nextIsFallback ? offset : offset + 1;
                    return ((LeafNode<C, R>) next).parse(context, stream, leafOffset);
                }

                if (next instanceof PredicateNode<C, R> pred) {
                    int matchLen = pred.matchLength(stream, offset);
                    if (matchLen >= 0) {
                        return pred.parse(context, stream, offset + matchLen);
                    }
                    TrieNode<C, R> fallback = branch.fallback();
                    if (fallback != null && fallback != next) {
                        if (fallback.isLeaf()) {
                            return ((LeafNode<C, R>) fallback).parse(context, stream, offset);
                        }
                        return ((PredicateNode<C, R>) fallback).parse(context, stream, offset);
                    }
                    throw new TrieDispatchException(
                        "Predicate '" + pred.name() + "' did not match and no fallback available"
                    );
                }

                offset++;
                node = next;
                continue;
            }

            // 密封接口保证：不是 Leaf/Branch → 必为 PredicateNode
            //noinspection ConstantValue
            if (node instanceof PredicateNode<C, R> pred) {
                int matchLen = pred.matchLength(stream, offset);
                if (matchLen >= 0) {
                    return pred.parse(context, stream, offset + matchLen);
                }
                throw new TrieDispatchException(
                    "Predicate '" + pred.name() + "' matched but failed in matchLength()"
                );
            }
            throw new TrieDispatchException(
                "Unexpected node type: " + node.getClass().getSimpleName()
            );
        }
    }

    /**
     * 尝试分发，不抛异常。匹配失败时返回 null。
     */
    public R tryDispatch(C context, TokenStream stream) {
        try {
            return dispatch(context, stream);
        } catch (TrieDispatchException e) {
            return null;
        }
    }

    /**
     * 获取根节点
     */
    public BranchNode<C, R> root() {
        return root;
    }

    /**
     * 打印决策树结构（调试用）
     */
    public String dump() {
        StringBuilder sb = new StringBuilder();
        dumpNode(root, 0, sb);
        return sb.toString();
    }

    private void dumpNode(TrieNode<C, R> node, int depth, StringBuilder sb) {
        String indent = "  ".repeat(depth);
        if (node.isLeaf()) {
            sb.append(indent).append("└── ").append(node).append("\n");
        } else if (node instanceof PredicateNode<C, R> pred) {
            sb.append(indent).append("└── ").append(pred).append("\n");
        } else {
            BranchNode<C, R> branch = (BranchNode<C, R>) node;
            sb.append(indent).append("├── ").append(branch).append("\n");
            for (Map.Entry<TokenType, TrieNode<C, R>> entry : branch.children().entrySet()) {
                sb.append(indent).append("│   [").append(entry.getKey()).append("]\n");
                dumpNode(entry.getValue(), depth + 1, sb);
            }
            for (PredicateNode<C, R> pred : branch.predicates()) {
                sb.append(indent).append("│   [predicate: ").append(pred).append("]\n");
            }
            if (branch.fallback() != null) {
                sb.append(indent).append("│   [fallback]\n");
                dumpNode(branch.fallback(), depth + 1, sb);
            }
        }
    }

    /**
     * 决策树分发异常
     */
    public static class TrieDispatchException extends RuntimeException {
        public TrieDispatchException(String message) {
            super(message);
        }
    }
}
