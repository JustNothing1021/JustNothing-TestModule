package com.justnothing.engine.v2.parser.trie;

import com.justnothing.engine.v2.lexer.TokenType;

import java.util.function.BiFunction;
import java.util.function.Predicate;

/**
 * ParseTrie 声明式构建器。
 * <p>
 * 用法示例：
 * <pre>{@code
 * ParseTrie<ParseContext, ASTNode> trie = ParseTrie.<ParseContext, ASTNode>builder("stmt")
 *     .on(KEYWORD_IF,    (ctx, s) -> parseIf(ctx, s))
 *     .on(KEYWORD_WHILE, (ctx, s) -> parseWhile(ctx, s))
 *     .on(KEYWORD_FOR,   (ctx, s) -> parseFor(ctx, s))
 *     .path(IDENTIFIER)
 *         .when(ASSIGN)  .to("assign", (ctx, s) -> parseAssign(ctx, s))
 *         .when(DOT)     .to("field",  (ctx, s) -> parseFieldAccess(ctx, s))
 *         .when(LPAREN)  .to("call",   (ctx, s) -> parseCall(ctx, s))
 *         .fallback("exprStmt", (ctx, s) -> parseExprStmt(ctx, s))
 *     .build();
 * }</pre>
 * </p>
 *
 * <h3>设计要点</h3>
 * <ul>
 *   <li>{@code .on(type, action)} — 顶层快捷：token 类型 → 叶子节点</li>
 *   <li>{@code .path(type)} — 开始一条路径，创建分支节点</li>
 *   <li>{@code .when(type).to(name, action)} — 在路径下添加一条分支</li>
 *   <li>{@code .when(type).then(type).to(...)} — 多级路径</li>
 *   <li>{@code .fallback(name, action)} — 兜底路径</li>
 * </ul>
 *
 * @param <C> 解析上下文类型
 * @param <R> 解析结果类型
 * @author JustNothing1021
 */
public class TrieBuilder<C, R> {

    private final BranchNode<C, R> root;
    private boolean built;

    TrieBuilder(String name) {
        this.root = new BranchNode<>(name);
        this.built = false;
    }

    // ========== 顶层快捷方法 ==========

    /**
     * 在根节点下注册一个 TokenType → 叶子节点的映射
     */
    public TrieBuilder<C, R> on(TokenType type, BiFunction<C, TokenStream, R> action) {
        root.addChild(type, new LeafNode<>(type.name(), action));
        return this;
    }

    /**
     * 在根节点下注册一个 TokenType → 叶子节点的映射（带回退标记）
     */
    public TrieBuilder<C, R> on(TokenType type, String name, BiFunction<C, TokenStream, R> action, boolean needsBacktrack) {
        root.addChild(type, new LeafNode<>(name, action, needsBacktrack));
        return this;
    }

    /**
     * 在根节点下注册一个 TokenType → 子分支的映射
     */
    public TrieBuilder<C, R> branch(TokenType type, String name) {
        BranchNode<C, R> child = new BranchNode<>(name);
        root.addChild(type, child);
        return this;
    }

    /**
     * 在根节点下注册一个谓词子节点
     */
    public TrieBuilder<C, R> whenPredicate(String name, Predicate<TokenStream> predicate,
                                           BiFunction<C, TokenStream, R> action) {
        root.addPredicate(new PredicateNode<>(name, predicate, action));
        return this;
    }

    // ========== 路径构建 ==========

    /**
     * 开始一条路径。返回一个 {@link PathBuilder} 用于构建该路径下的分支。
     * <p>
     * 路径构建完成后（调用 {@link PathBuilder#end()}）回到根节点。
     * </p>
     */
    public PathBuilder path(TokenType first) {
        return new PathBuilder(first);
    }

    /**
     * 开始一条多 token 路径。每个 token 对应 Trie 的一层。
     * <p>
     * 示例：{@code .path(KEYWORD_FOR, DELIMITER_LEFT_PAREN)} 创建两层分支。
     * </p>
     */
    public PathBuilder path(TokenType first, TokenType... rest) {
        PathBuilder pb = new PathBuilder(first);
        for (TokenType type : rest) {
            pb = pb.thenCreate(type);
        }
        return pb;
    }

    // ========== Fallback ==========

    /**
     * 设置根节点的 fallback
     */
    public TrieBuilder<C, R> fallback(String name, BiFunction<C, TokenStream, R> action) {
        root.setFallback(new LeafNode<>(name, action));
        return this;
    }

    // ========== 构建 ==========

    /**
     * 构建并返回 ParseTrie
     */
    public ParseTrie<C, R> build() {
        if (built) {
            throw new IllegalStateException("TrieBuilder already used; create a new one");
        }
        built = true;
        return new ParseTrie<>(root);
    }

    // ========== 内部 PathBuilder ==========

    /**
     * 路径构建器：用于在某个分支节点下定义多条子路径。
     * <p>
     * 核心设计：
     * <ul>
     *   <li>{@code .when(type)} — 在路径根节点下添加一个子分支，cursor 移到该分支</li>
     *   <li>{@code .then(type)} — 在当前 cursor 下继续深入一级</li>
     *   <li>{@code .to(name, action)} — 在当前 cursor 处添加叶子节点，cursor 回到路径根</li>
     *   <li>{@code .fallback(name, action)} — 在路径根节点设置 fallback</li>
     * </ul>
     * </p>
     *
     * <p>示例：</p>
     * <pre>{@code
     * .path(IDENTIFIER)
     *     // 单级: IDENTIFIER → ASSIGN → 叶子
     *     .when(ASSIGN).to("assign", (ctx, s) -> ...)
     *     // 多级: IDENTIFIER → DOT → IDENTIFIER → ASSIGN → 叶子
     *     .when(DOT).then(IDENTIFIER).then(ASSIGN).to("qualAssign", (ctx, s) -> ...)
     *     // 兜底
     *     .fallback("exprStmt", (ctx, s) -> ...)
     *     .end()
     * }</pre>
     */
    public class PathBuilder {

        private final BranchNode<C, R> pathRoot;
        private BranchNode<C, R> cursor;

        PathBuilder(TokenType first) {
            TrieNode<C, R> existing = root.children().get(first);
            if (existing instanceof BranchNode<C, R> br) {
                pathRoot = br;
            } else {
                pathRoot = new BranchNode<>(first.name());
                root.addChild(first, pathRoot);
            }
            cursor = pathRoot;
        }

        /**
         * 在路径根节点下添加一个子分支，cursor 移到该分支。
         * 用于定义一条新的子路径。
         */
        public PathBuilder when(TokenType type) {
            TrieNode<C, R> existing = pathRoot.children().get(type);
            if (existing instanceof BranchNode<C, R> br) {
                cursor = br;
            } else {
                BranchNode<C, R> child = new BranchNode<>(type.name());
                pathRoot.addChild(type, child);
                cursor = child;
            }
            return this;
        }

        /**
         * 在当前 cursor 下继续深入一级。
         * 用于构建多级路径。
         */
        public PathBuilder then(TokenType type) {
            TrieNode<C, R> existing = cursor.children().get(type);
            if (existing instanceof BranchNode<C, R> br) {
                cursor = br;
            } else {
                BranchNode<C, R> child = new BranchNode<>(type.name());
                cursor.addChild(type, child);
                cursor = child;
            }
            return this;
        }

        /**
         * 在当前 cursor 下继续深入一级（创建新分支节点）。
         * 与 {@link #then(TokenType)} 不同，此方法总是创建新节点，
         * 用于 path(TokenType, TokenType...) 的链式调用。
         */
        public PathBuilder thenCreate(TokenType type) {
            BranchNode<C, R> child = new BranchNode<>(type.name());
            cursor.addChild(type, child);
            cursor = child;
            return this;
        }

        /**
         * 在当前 cursor 处创建一个前瞻决策点。
         * <p>
         * 决策点允许根据后续 token 序列（而非仅当前 token）来选择解析路径，
         * 实现"分水岭"式零回溯分发。
         * </p>
         *
         * @param name 决策点名称（用于调试/可视化）
         * @return DecisionBuilder 用于定义 ifMatches/otherwise 规则
         */
        public DecisionBuilder<C, R> decision(String name) {
            return new DecisionBuilder<>(this, cursor);
        }

        /**
         * 在当前 cursor 处添加叶子节点（作为 fallback），cursor 回到路径根。
         * <p>
         * 使用 fallback 而非精确 TokenType 匹配，因为叶子节点
         * 代表"到达此路径末端，执行解析"的语义。
         * </p>
         */
        public PathBuilder to(String name, BiFunction<C, TokenStream, R> action) {
            cursor.setFallback(new LeafNode<>(name, action));
            cursor = pathRoot;
            return this;
        }

        /**
         * 在当前 cursor 处添加叶子节点（指定匹配的 TokenType），cursor 回到路径根。
         */
        public PathBuilder to(TokenType matchType, String name, BiFunction<C, TokenStream, R> action) {
            cursor.addChild(matchType, new LeafNode<>(name, action));
            cursor = pathRoot;
            return this;
        }

        /**
         * 在路径根节点下添加一个谓词子节点。
         * <p>
         * 当 TokenType 无法精确区分时，用谓词做二次判断。
         * 谓词在精确匹配之后、fallback 之前被尝试。
         * </p>
         *
         * @param name      节点名称
         * @param predicate 谓词：给定 TokenStream，判断此路径是否匹配
         * @param action    匹配成功时的解析动作
         */
        public PathBuilder whenPredicate(String name, Predicate<TokenStream> predicate,
                                         BiFunction<C, TokenStream, R> action) {
            pathRoot.addPredicate(new PredicateNode<>(name, predicate, action));
            return this;
        }

        /**
         * 设置路径根节点的 fallback
         */
        public PathBuilder fallback(String name, BiFunction<C, TokenStream, R> action) {
            pathRoot.setFallback(new LeafNode<>(name, action));
            return this;
        }

        /**
         * 结束路径，回到 TrieBuilder 根节点
         */
        public TrieBuilder<C, R> end() {
            return TrieBuilder.this;
        }
    }
}
