package com.justnothing.engine.v2.parser.trie;

import com.justnothing.engine.v2.lexer.TokenType;

import java.util.Map;

/**
 * 基于 Token 序列的解析决策树。
 * <p>
 * 核心思想：将 if-else 链的线性分发替换为 Trie 树的 O(1) 分发。
 * 每个 Token 类型对应树的一层，叶子节点持有具体的解析动作。
 * </p>
 *
 * <h3>分发优先级</h3>
 * <ol>
 *   <li>TokenType 精确匹配 — O(1)</li>
 *   <li>谓词匹配 — 按注册顺序 O(n)</li>
 *   <li>Fallback 兜底</li>
 * </ol>
 *
 * <h3>使用示例</h3>
 * <pre>{@code
 * ParseTrie<ParseContext, ASTNode> trie = ParseTrie.<ParseContext, ASTNode>builder("stmt")
 *     .on(KEYWORD_IF,    (ctx, s) -> parseIf(ctx, s))
 *     .on(KEYWORD_WHILE, (ctx, s) -> parseWhile(ctx, s))
 *     .path(IDENTIFIER)
 *         .when(ASSIGN)  .to("assign", (ctx, s) -> parseAssign(ctx, s))
 *         .when(DOT)     .to("field",  (ctx, s) -> parseFieldAccess(ctx, s))
 *         .whenPredicate("qualifiedType",
 *             stream -> looksLikeQualifiedType(stream),
 *             (ctx, s) -> parseQualifiedType(ctx, s))
 *         .fallback("exprStmt", (ctx, s) -> parseExprStmt(ctx, s))
 *     .build();
 * }</pre>
 *
 * @param <C> 解析上下文类型
 * @param <R> 解析结果类型
 * @author JustNothing1021
 */
public class ParseTrie<C, R> {

    private final BranchNode<C, R> root;

    ParseTrie(BranchNode<C, R> root) {
        this.root = root;
    }

    public static <C, R> TrieBuilder<C, R> builder(String name) {
        return new TrieBuilder<>(name);
    }

    /**
     * 核心分发方法：沿决策树走，找到匹配的叶子节点并执行解析。
     * <p>
     * 算法（使用前瞻，不消费 token）：
     * <ol>
     *   <li>从根节点开始，用 peek(offset) 按 token 类型查找子节点</li>
     *   <li>精确匹配优先，谓词次之，fallback 兜底</li>
     *   <li>如果找到 BranchNode，offset++ 继续深入</li>
     *   <li>如果找到 LeafNode 或 PredicateNode，执行解析动作并返回结果</li>
     * </ol>
     * <p>
     * 关键：dispatch 不消费任何 token。叶子节点的 action 函数
     * 从 stream 的当前位置开始解析，自行负责消费 token。
     * </p>
     */
    public R dispatch(C context, TokenStream stream) {
        TrieNode<C, R> node = root;
        int offset = 0;

        while (!node.isLeaf()) {
            if (node instanceof PredicateNode<C, R> pred) {
                // 谓词节点：直接执行解析
                return pred.parse(context, stream);
            }

            BranchNode<C, R> branch = (BranchNode<C, R>) node;
            TokenType type = stream.peekType(offset);
            TrieNode<C, R> next = branch.next(type, stream, offset);

            if (next == null) {
                throw new TrieDispatchException(
                    "No matching path at branch '" + branch.name()
                        + "' for token " + stream.peek(offset)
                        + " (lookahead offset=" + offset + ")"
                );
            }

            if (next.isLeaf()) {
                LeafNode<C, R> leaf = (LeafNode<C, R>) next;
                return leaf.parse(context, stream);
            }

            if (next instanceof PredicateNode<C, R> pred) {
                return pred.parse(context, stream);
            }

            // 内部分支：不消费 token，只增加前瞻偏移
            offset++;
            node = next;
        }

        // 根节点本身就是叶子（极端情况）
        LeafNode<C, R> leaf = (LeafNode<C, R>) node;
        return leaf.parse(context, stream);
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
     * 获取根节点（用于可视化/调试）
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
                sb.append(indent).append("│   [predicate: ").append(pred.name()).append("]\n");
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
