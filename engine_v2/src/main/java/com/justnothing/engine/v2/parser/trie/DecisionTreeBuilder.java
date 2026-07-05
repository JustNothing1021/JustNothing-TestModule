package com.justnothing.engine.v2.parser.trie;

import com.justnothing.engine.v2.lexer.TokenType;

import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Supplier;

/**
 * 增强版决策树构建器：支持嵌套 ifMatches/otherwise/endIf + doFromHere/doFromRoot。
 * <p>
 * 核心设计：
 * <ul>
 *   <li>{@code .on(type, action)} — 顶层快捷：token 类型 → 叶子节点</li>
 *   <li>{@code .ifMatches(types...)} — 开始一个前瞻决策块</li>
 *   <li>{@code .orIfMatches(types...)} — 在当前决策块中添加候选模式</li>
 *   <li>{@code .doFromHere(name, action)} — 匹配成功后从当前位置开始解析</li>
 *   <li>{@code .doFromRoot(name, action)} — 匹配成功后从头开始解析</li>
 *   <li>{@code .otherwise()} — 所有 ifMatches 都不匹配时的兜底</li>
 *   <li>{@code .endIf()} — 结束当前决策块</li>
 *   <li>{@code .path(type, ...)} — 传统 Trie 路径构建</li>
 * </ul>
 * </p>
 *
 * <h3>嵌套决策示例</h3>
 * <pre>{@code
 * DecisionTree<ParseContext, ASTNode> tree = DecisionTree.builder("stmt")
 *     // 嵌套决策：for vs for-each
 *     .ifMatches(KEYWORD_FOR, DELIMITER_LEFT_PAREN)
 *         .ifMatches(KEYWORD_INT, IDENTIFIER, OPERATOR_COLON)
 *         .orIfMatches(IDENTIFIER, OPERATOR_COLON)
 *             .doFromHere("forEach", (ctx, s) -> parseForEach(ctx, s))
 *         .otherwise()
 *             .doFromRoot("forClassic", (ctx, s) -> parseClassicFor(ctx, s))
 *         .endIf()
 *     .endIf()
 *     // 简单映射
 *     .on(KEYWORD_IF, (ctx, s) -> parseIf(ctx, s))
 *     .build();
 * }</pre>
 *
 * <h3>内部机制</h3>
 * <p>
 * ifMatches 在 Trie 中创建一条 token 路径（BranchNode 链），
 * 然后在路径末端挂载 PredicateNode。嵌套的 ifMatches 会在
 * 外层路径末端继续延伸。otherwise 创建 fallback 叶子。
 * </p>
 *
 * @param <C> 解析上下文类型
 * @param <R> 解析结果类型
 * @author JustNothing1021
 */
public class DecisionTreeBuilder<C, R> {

    private final BranchNode<C, R> root;
    private boolean built;

    // 嵌套决策上下文栈
    private final Deque<DecisionContext> decisionStack = new ArrayDeque<>();

    // 延迟挂载列表（mountLazy 注册的 supplier，在 build() 时处理）
    private final List<Supplier<DecisionTree<C, R>>> lazyMounts = new ArrayList<>();

    DecisionTreeBuilder(String name) {
        this.root = new BranchNode<>(name);
    }

    // ========== 顶层快捷方法 ==========

    /**
     * 在根节点下注册一个 TokenType → 叶子节点的映射（doFromRoot 模式）
     */
    public DecisionTreeBuilder<C, R> on(TokenType type, BiFunction<C, TokenStream, R> action) {
        root.addChild(type, new LeafNode<>(type.name(), action, false, false));
        return this;
    }

    /**
     * 在根节点下注册一个 TokenType → 叶子节点的映射（doFromHere 模式）
     */
    public DecisionTreeBuilder<C, R> onFromHere(TokenType type, BiFunction<C, TokenStream, R> action) {
        root.addChild(type, new LeafNode<>(type.name(), action, false, true));
        return this;
    }

    /**
     * 在根节点下注册一个谓词子节点
     */
    public DecisionTreeBuilder<C, R> whenPredicate(String name,
                                                    java.util.function.Predicate<TokenStream> predicate,
                                                    BiFunction<C, TokenStream, R> action) {
        root.addPredicate(new PredicateNode<>(name, predicate, action));
        return this;
    }

    // ========== 嵌套决策 API ==========

    /**
     * 开始一个前瞻决策块。
     * <p>
     * 语义：如果从当前位置开始的 token 序列匹配 types，
     * 则走此路径。匹配后游标前进 types.length 个位置。
     * </p>
     * <p>
     * 有两种使用模式：
     * <ul>
     *   <li>顶层路径型：不在任何 ifMatches 内，创建 BranchNode 链作为 Trie 路径</li>
     *   <li>内层决策型：在 ifMatches 内，只注册谓词模式，不创建 BranchNode 链</li>
     * </ul>
     * </p>
     *
     * @param types 期望的 token 类型序列
     */
    public DecisionTreeBuilder<C, R> ifMatches(TokenType... types) {
        if (decisionStack.isEmpty()) {
            // 顶层：创建 BranchNode 链作为 Trie 路径
            BranchNode<C, R> chainEnd = createChain(root, types);
            DecisionContext ctx = new DecisionContext(chainEnd, types.length);
            decisionStack.push(ctx);
        } else {
            // 内层：只注册谓词模式，不创建 BranchNode 链
            DecisionContext outerCtx = decisionStack.peek();
            DecisionContext ctx = new DecisionContext(outerCtx.attachPoint, outerCtx.trieDepth);
            decisionStack.push(ctx);
            // 自动开始第一个模式
            ctx.startPattern(Arrays.asList(types));
        }
        return this;
    }

    /**
     * 开始一个前瞻决策块，首元素使用 TokenMatcher（如 {@code any(INT, VAR)}）。
     * <p>
     * 仅在内层使用——匹配时不创建 BranchNode 链，
     * 而是注册带 TokenMatcher 的 PredicateNode。
     * </p>
     *
     * @param first 首 token 的匹配器（any/is）
     * @param rest  后续 token 类型序列
     */
    public DecisionTreeBuilder<C, R> ifMatches(TokenMatcher first, TokenType... rest) {
        if (decisionStack.isEmpty()) {
            // 顶层：创建 PredicateNode（TokenMatcher 无法映射为 BranchNode 子节点）
            // 语义等价于 whenPredicate，但集成到 ifMatches/doFromRoot/endIf 流中
            DecisionContext ctx = new DecisionContext(root, 0);
            decisionStack.push(ctx);
            ctx.startPattern(toObjectList(first, rest));
            return this;
        }
        // 内层：现有逻辑
        DecisionContext outerCtx = decisionStack.peek();
        DecisionContext ctx = new DecisionContext(outerCtx.attachPoint, outerCtx.trieDepth);
        decisionStack.push(ctx);
        ctx.startPattern(toObjectList(first, rest));
        return this;
    }

    /**
     * 在当前决策块中添加另一个候选模式。
     * <p>
     * 多个 ifMatches/orIfMatches 之间是"或"的关系——
     * 只要有任何一个匹配，就走此路径。
     * </p>
     * <p>
     * 注意：orIfMatches 的 types 是从当前 ifMatches 块的起始位置开始匹配的，
     * 不是从上一个 orIfMatches 结束的位置。
     * </p>
     */
    public DecisionTreeBuilder<C, R> orIfMatches(TokenType... types) {
        DecisionContext ctx = currentContext();
        if (ctx.currentPattern == null || ctx.currentAction != null) {
            // 之前的 pattern 已完成（有 action）或不存在，开始新的 pattern
            ctx.startPattern(Arrays.asList(types));
        } else {
            // 之前的 pattern 没有 action，添加为候选（共享同一个 action）
            ctx.currentPattern.addAlternative(Arrays.asList(types));
        }
        return this;
    }

    /**
     * 在当前决策块中添加一个带 TokenMatcher 的候选模式。
     */
    public DecisionTreeBuilder<C, R> orIfMatches(TokenMatcher first, TokenType... rest) {
        DecisionContext ctx = currentContext();
        if (ctx.currentPattern == null || ctx.currentAction != null) {
            ctx.startPattern(toObjectList(first, rest));
        } else {
            ctx.currentPattern.addAlternative(toObjectList(first, rest));
        }
        return this;
    }

    /**
     * 为当前 ifMatches/orIfMatches 组设置匹配成功时的解析动作（doFromHere 模式）。
     * <p>
     * doFromHere：dispatch 到达此叶子后，自动将 stream 前进
     * （trie 深度 + 谓词匹配长度）个 token，然后调用 action。
     * </p>
     * <p>
     * 有两种使用场景：
     * <ul>
     *   <li>简单路径型：ifMatches 后直接跟 doFromHere，在路径末端设置 fallback 叶子</li>
     *   <li>决策型：orIfMatches 后跟 doFromHere，为当前候选模式设置 action</li>
     * </ul>
     * </p>
     */
    public DecisionTreeBuilder<C, R> doFromHere(String name, BiFunction<C, TokenStream, R> action) {
        DecisionContext ctx = currentContext();
        if (ctx.currentPattern != null) {
            // 决策型：为当前候选模式设置 action
            ctx.setCurrentAction(name, action, true);
        } else {
            // 简单路径型：在 attachPoint 上设置 fallback 叶子
            ctx.directLeaf = new LeafNode<>(name, action, false, true);
        }
        return this;
    }

    /**
     * 为当前 ifMatches/orIfMatches 组设置匹配成功时的解析动作（doFromRoot 模式）。
     * <p>
     * doFromRoot：dispatch 到达此叶子后，不前进 stream，
     * action 从 token 流的当前位置（即 Trie 走之前的原始位置）开始解析。
     * </p>
     */
    public DecisionTreeBuilder<C, R> doFromRoot(String name, BiFunction<C, TokenStream, R> action) {
        DecisionContext ctx = currentContext();
        if (ctx.currentPattern != null) {
            // 决策型：为当前候选模式设置 action
            ctx.setCurrentAction(name, action, false);
        } else {
            // 简单路径型：在 attachPoint 上设置 fallback 叶子
            ctx.directLeaf = new LeafNode<>(name, action, false, false);
        }
        return this;
    }

    /**
     * 定义"所有 ifMatches 都不匹配时"的兜底路径。
     * <p>
     * 只能在同一个决策块内调用一次，且必须在所有 ifMatches 之后。
     * </p>
     */
    public DecisionTreeBuilder<C, R> otherwise() {
        DecisionContext ctx = currentContext();
        if (ctx.hasOtherwise) {
            throw new IllegalStateException("otherwise() 只能调用一次");
        }
        ctx.hasOtherwise = true;
        ctx.pendingOtherwise = true;
        return this;
    }

    /**
     * otherwise 后跟 doFromHere：兜底路径从 trie 深度位置开始解析。
     */
    public DecisionTreeBuilder<C, R> doOtherwiseFromHere(String name, BiFunction<C, TokenStream, R> action) {
        DecisionContext ctx = currentContext();
        if (!ctx.pendingOtherwise) {
            throw new IllegalStateException("必须先调用 otherwise()");
        }
        // otherwise 的 offset = trie 深度（不含谓词匹配长度）
        ctx.otherwiseLeaf = new LeafNode<>(name, action, false, true);
        ctx.pendingOtherwise = false;
        return this;
    }

    /**
     * otherwise 后跟 doFromRoot：兜底路径从头开始解析。
     */
    public DecisionTreeBuilder<C, R> doOtherwiseFromRoot(String name, BiFunction<C, TokenStream, R> action) {
        DecisionContext ctx = currentContext();
        if (!ctx.pendingOtherwise) {
            throw new IllegalStateException("必须先调用 otherwise()");
        }
        ctx.otherwiseLeaf = new LeafNode<>(name, action, false, false);
        ctx.pendingOtherwise = false;
        return this;
    }

    /**
     * 结束当前决策块。
     * <p>
     * 将所有 ifMatches 模式注册为 PredicateNode，otherwise 注册为 fallback。
     * 如果当前 ifMatches 是路径型（没有 pattern 和 otherwise），则什么都不做。
     * </p>
     */
    public DecisionTreeBuilder<C, R> endIf() {
        DecisionContext ctx = decisionStack.pop();

        // 检查：如果当前有未完成的 pattern（有 pattern 但没有 action），报错
        if (ctx.currentPattern != null && ctx.currentAction == null) {
            throw new IllegalStateException("最后一个 ifMatches/orIfMatches 缺少 doFromHere/doFromRoot");
        }

        // 检查：otherwise 已设置 action
        if (ctx.hasOtherwise && ctx.pendingOtherwise) {
            throw new IllegalStateException("otherwise() 后必须调用 doOtherwiseFromHere 或 doOtherwiseFromRoot");
        }

        // 将所有 pattern 注册为 PredicateNode
        for (PatternDef pattern : ctx.patterns) {
            PredicateNode<C, R> pred = pattern.toPredicateNode();
            ctx.attachPoint.addPredicate(pred);
        }

        // 注册 otherwise 为 fallback
        if (ctx.otherwiseLeaf != null) {
            ctx.attachPoint.setFallback(ctx.otherwiseLeaf);
        }

        // 简单路径型：在 attachPoint 上设置直接叶子
        if (ctx.directLeaf != null) {
            ctx.attachPoint.setFallback(ctx.directLeaf);
        }

        return this;
    }

    // ========== 传统路径构建 ==========

    /**
     * 开始一条路径。返回 PathBuilder 用于构建该路径下的分支。
     */
    public PathBuilder path(TokenType first, TokenType... rest) {
        return new PathBuilder(first, rest);
    }

    /**
     * 设置根节点的 fallback
     */
    public DecisionTreeBuilder<C, R> fallback(String name, BiFunction<C, TokenStream, R> action) {
        root.setFallback(new LeafNode<>(name, action, false, false));
        return this;
    }

    // ========== 树挂载 API ==========

    /**
     * 将另一棵决策树合并到当前树的根节点。
     * <p>
     * 合并规则：
     * <ul>
     *   <li>子节点（TokenType 精确匹配）：已有路径优先，不覆盖</li>
     *   <li>谓词节点：追加到已有谓词之后（先注册先匹配）</li>
     *   <li>Fallback：已有 fallback 优先，不覆盖</li>
     *   <li>子树非根节点的深度路径：直接挂到根节点下（如果路径起点的 TokenType 不冲突）</li>
     * </ul>
     * </p>
     *
     * <h3>典型用例：组合 stmt/expr/decl 三棵树</h3>
     * <pre>{@code
     * DecisionTree<ParseContext, ASTNode> root = DecisionTree.builder("root")
     *     .mountInto(stmtTree)   // 关键字路径优先
     *     .mountInto(declTree)   // 声明路径次之
     *     .mountInto(exprTree)   // 标识符表达式兜底
     *     .build();
     * }</pre>
     *
     * @param other 要挂载的决策树
     */
    public DecisionTreeBuilder<C, R> mountInto(DecisionTree<C, R> other) {
        mergeRoot(other.root());
        return this;
    }

    /**
     * 延迟挂载：在 build() 时才解析树引用，解决循环依赖。
     * <p>
     * 典型场景：exprTree 引用 stmtTree（lambda 体内有语句），
     * stmtTree 引用 exprTree（语句中有表达式）。直接 mountInto 会 NPE。
     * mountLazy 推迟到 build() 时才调用 supplier，此时两棵树都已构建完成。
     * </p>
     *
     * <pre>{@code
     * DecisionTreeBuilder<ParseContext, ASTNode> stmtBuilder = DecisionTree.builder("stmt")
     *     ...;
     * DecisionTreeBuilder<ParseContext, ASTNode> exprBuilder = DecisionTree.builder("expr")
     *     .mountLazy(() -> stmtBuilder.build())  // 推迟到 build 时获取
     *     ...;
     * DecisionTree<...> stmtTree = stmtBuilder
     *     .mountLazy(() -> exprBuilder.build())
     *     .build();
     * }</pre>
     *
     * @param treeSupplier 提供待挂载决策树的供应商（在 build() 时调用）
     */
    public DecisionTreeBuilder<C, R> mountLazy(Supplier<DecisionTree<C, R>> treeSupplier) {
        lazyMounts.add(treeSupplier);
        return this;
    }

    /**
     * 合并另一棵树的所有子节点、谓词和 fallback 到根节点。
     */
    private void mergeRoot(BranchNode<C, R> otherRoot) {
        // 1. 合并精确匹配子节点：已有路径优先（不覆盖）
        for (Map.Entry<TokenType, TrieNode<C, R>> entry : otherRoot.children().entrySet()) {
            if (!root.hasChild(entry.getKey())) {
                root.addChild(entry.getKey(), entry.getValue());
            }
        }
        // 2. 追加谓词节点
        for (PredicateNode<C, R> pred : otherRoot.predicates()) {
            root.addPredicate(pred);
        }
        // 3. 合并 fallback：已有 fallback 优先
        if (root.fallback() == null && otherRoot.fallback() != null) {
            root.setFallback(otherRoot.fallback());
        }
    }

    // ========== 构建 ==========

    /**
     * 构建并返回 DecisionTree
     */
    public DecisionTree<C, R> build() {
        if (built) {
            throw new IllegalStateException("DecisionTreeBuilder already used; create a new one");
        }
        if (!decisionStack.isEmpty()) {
            throw new IllegalStateException("有 " + decisionStack.size() + " 个未关闭的 ifMatches 块");
        }
        // 处理延迟挂载
        for (Supplier<DecisionTree<C, R>> supplier : lazyMounts) {
            mergeRoot(supplier.get().root());
        }
        built = true;
        return new DecisionTree<>(root);
    }

    // ========== 内部工具方法 ==========

    private DecisionContext currentContext() {
        if (decisionStack.isEmpty()) {
            throw new IllegalStateException("必须在 ifMatches 块内调用");
        }
        return decisionStack.peek();
    }

    /**
     * 将 TokenMatcher + TokenType... 转为 List&lt;Object&gt;
     */
    private static List<Object> toObjectList(TokenMatcher first, TokenType... rest) {
        List<Object> list = new ArrayList<>();
        list.add(first);
        Collections.addAll(list, (Object[]) rest);
        return list;
    }

    /**
     * 在 Trie 中创建一条 BranchNode 链。
     * <p>
     * 从 parent 开始，为 types 中的每个 token 创建一个 BranchNode，
     * 返回链的末端节点。
     * </p>
     * 如果路径已存在，则复用现有节点。
     */
    private BranchNode<C, R> createChain(BranchNode<C, R> parent, TokenType... types) {
        BranchNode<C, R> current = parent;
        for (TokenType type : types) {
            TrieNode<C, R> existing = current.children().get(type);
            if (existing instanceof BranchNode<C, R> br) {
                current = br;
            } else {
                BranchNode<C, R> child = new BranchNode<>(type.name());
                current.addChild(type, child);
                current = child;
            }
        }
        return current;
    }

    // ========== 内部类 ==========

    /**
     * 决策上下文：跟踪一个 ifMatches 块的状态
     */
    private class DecisionContext {
        /** PredicateNode 挂载的分支节点 */
        final BranchNode<C, R> attachPoint;
        /** 外层 ifMatches 贡献的 trie 深度 */
        final int trieDepth;
        /** 收集的所有模式 */
        final List<PatternDef> patterns = new ArrayList<>();
        /** 当前正在构建的模式 */
        PatternDef currentPattern;
        /** 当前模式的 action */
        BiFunction<C, TokenStream, R> currentAction;
        Boolean currentAdvanceOnDispatch;
        /** 是否已有 otherwise */
        boolean hasOtherwise;
        /** 是否正在等待 otherwise 的 action */
        boolean pendingOtherwise;
        /** otherwise 的叶子节点 */
        LeafNode<C, R> otherwiseLeaf;
        /** 简单路径型的直接叶子（ifMatches 后直接跟 doFromHere/doFromRoot） */
        LeafNode<C, R> directLeaf;

        DecisionContext(BranchNode<C, R> attachPoint, int trieDepth) {
            this.attachPoint = attachPoint;
            this.trieDepth = trieDepth;
        }

        void startPattern(List<?> types) {
            // 保存前一个 pattern 的 action
            flushCurrentPattern();

            @SuppressWarnings("unchecked")
            List<Object> objTypes = (List<Object>) (List<?>) types;
            currentPattern = new PatternDef(objTypes);
            currentAction = null;
            currentAdvanceOnDispatch = null;
        }

        void setCurrentAction(String name, BiFunction<C, TokenStream, R> action, boolean advanceOnDispatch) {
            if (currentPattern == null) {
                throw new IllegalStateException("doFromHere/doFromRoot 必须在 ifMatches/orIfMatches 之后调用");
            }
            currentAction = action;
            currentAdvanceOnDispatch = advanceOnDispatch;

            // 立即创建叶子并关联到 pattern
            currentPattern.setLeaf(new LeafNode<>(name, action, false, advanceOnDispatch));

            // 将完成的 pattern 加入列表
            patterns.add(currentPattern);
            currentPattern = null;
        }

        void flushCurrentPattern() {
            if (currentPattern != null && currentAction != null) {
                currentPattern.setLeaf(new LeafNode<>(
                    "auto", currentAction, false, currentAdvanceOnDispatch
                ));
                patterns.add(currentPattern);
            }
        }
    }

    /**
     * 模式定义：一组候选 token 序列 + 对应的叶子节点。
     * <p>
     * 候选中的每个元素可以是 TokenType 或 TokenMatcher。
     * </p>
     */
    private class PatternDef {
        final List<Object> primary;
        final List<List<Object>> alternatives = new ArrayList<>();
        LeafNode<C, R> leaf;

        PatternDef(List<Object> primary) {
            this.primary = List.copyOf(primary);
        }

        void addAlternative(List<?> alt) {
            @SuppressWarnings("unchecked")
            List<Object> objAlt = (List<Object>) (List<?>) alt;
            alternatives.add(List.copyOf(objAlt));
        }

        void setLeaf(LeafNode<C, R> leaf) {
            this.leaf = leaf;
        }

        /**
         * 转换为 PredicateNode。
         * <p>
         * 谓词逻辑：从 stream 的 offset 位置开始前瞻，
         * 检查是否有任何一个候选序列匹配。
         * 候选中的元素支持 TokenType（精确匹配）和 TokenMatcher（柔性匹配）。
         * </p>
         * <p>
         * matchLength：返回匹配到的候选序列的长度。
         * 对于 orIfMatches 的不同长度候选，需要逐个检查。
         * </p>
         */
        PredicateNode<C, R> toPredicateNode() {
            List<List<Object>> allPatterns = new ArrayList<>();
            allPatterns.add(primary);
            allPatterns.addAll(alternatives);

            boolean allSameLength = allPatterns.stream().allMatch(p -> p.size() == primary.size());
            List<List<Object>> immutablePatterns = List.copyOf(allPatterns);

            if (allSameLength) {
                // 所有候选长度相同：使用固定 patternLength
                return new PredicateNode<>(
                    leaf.name(),
                    (stream, offset) -> {
                        for (List<Object> pattern : immutablePatterns) {
                            if (matchesPattern(stream, pattern, offset)) {
                                return true;
                            }
                        }
                        return false;
                    },
                    leaf,  // 传 leaf 以保留 advanceOnDispatch 语义
                    primary.size()
                );
            } else {
                // 候选长度不同：使用可变长度构造函数
                return new PredicateNode<>(
                    leaf.name(),
                    (stream, offset) -> {
                        for (List<Object> pattern : immutablePatterns) {
                            if (matchesPattern(stream, pattern, offset)) {
                                return true;
                            }
                        }
                        return false;
                    },
                    leaf,
                    immutablePatterns
                );
            }
        }

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
    }

    // ========== PathBuilder（传统 Trie 路径） ==========

    /**
     * 路径构建器：用于在某个分支节点下定义多条子路径。
     * <p>
     * 与 TrieBuilder.PathBuilder 相同的 API，但产出 DecisionTree。
     * </p>
     */
    public class PathBuilder {
        private final BranchNode<C, R> pathRoot;
        private BranchNode<C, R> cursor;

        PathBuilder(TokenType first, TokenType... rest) {
            TrieNode<C, R> existing = root.children().get(first);
            if (existing instanceof BranchNode<C, R> br) {
                pathRoot = br;
            } else {
                pathRoot = new BranchNode<>(first.name());
                root.addChild(first, pathRoot);
            }
            cursor = pathRoot;

            for (TokenType type : rest) {
                BranchNode<C, R> child = new BranchNode<>(type.name());
                cursor.addChild(type, child);
                cursor = child;
            }
        }

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

        public PathBuilder to(String name, BiFunction<C, TokenStream, R> action) {
            cursor.setFallback(new LeafNode<>(name, action, false, false));
            cursor = pathRoot;
            return this;
        }

        public PathBuilder fallback(String name, BiFunction<C, TokenStream, R> action) {
            pathRoot.setFallback(new LeafNode<>(name, action, false, false));
            return this;
        }

        public DecisionTreeBuilder<C, R> end() {
            return DecisionTreeBuilder.this;
        }
    }
}
