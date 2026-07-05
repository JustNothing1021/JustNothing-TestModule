package com.justnothing.engine.v2.parser.trie;

import com.justnothing.engine.v2.lexer.Token;
import com.justnothing.engine.v2.lexer.TokenType;
import static com.justnothing.engine.v2.lexer.TokenType.*;

import java.util.List;

/**
 * Token 流的只读视图，支持前瞻和位置标记。
 * <p>
 * 与 engine_new 的词法分析器输出对接，提供决策树所需的 peek/advance/mark/restore 操作。
 * </p>
 *
 * @author JustNothing1021
 */
public class TokenStream {

    private final List<Token> tokens;
    private int position;
    private final List<Token> pending = new java.util.ArrayList<>(); // pushBack 缓存

    public TokenStream(List<Token> tokens) {
        this.tokens = tokens;
        this.position = 0;
    }

    // ========== 基础操作 ==========

    /**
     * 当前 token
     */
    public Token peek() {
        if (!pending.isEmpty()) return pending.get(0);
        return tokens.get(position);
    }

    /**
     * 当前 token 类型
     */
    public TokenType peekType() {
        if (!pending.isEmpty()) return pending.get(0).type();
        return tokens.get(position).type();
    }

    /**
     * 前方第 n 个 token（0 = 当前）
     */
    public Token peek(int offset) {
        if (offset == 0) return peek();
        int idx = position + offset - pending.size();
        return idx < tokens.size() ? tokens.get(idx) : tokens.get(tokens.size() - 1);
    }

    /**
     * 前方第 n 个 token 的类型
     */
    public TokenType peekType(int offset) {
        return peek(offset).type();
    }

    /**
     * 消费当前 token 并返回
     */
    public Token advance() {
        if (!pending.isEmpty()) return pending.remove(0);
        return tokens.get(position++);
    }

    /**
     * 消费当前 token，如果类型匹配
     */
    public boolean match(TokenType expected) {
        if (peekType() == expected) {
            advance();
            return true;
        }
        return false;
    }

    /**
     * 检查当前 token 类型是否为 expected（不消费）
     */
    public boolean check(TokenType expected) {
        return peekType() == expected;
    }

    // ========== 位置管理 ==========

    /**
     * 获取当前位置（用于外部 save/restore）
     */
    public int position() {
        return position;
    }

    /**
     * 恢复到指定位置
     */
    public void restore(int savedPosition) {
        this.position = savedPosition;
        this.pending.clear();
    }

    /**
     * 缓存即将到来的 extra 个取值为 > 的 token（处理 >>> 歧义）。
     * sourceToken 提供位置信息，用于构造一个 OPERATOR_GREATER_THAN 类型的 token 并重复 extra 次插入缓存队列头部。
     */
    public void pushBack(int extra, Token sourceToken) {
        for (int i = 0; i < extra; i++) {
            pending.add(new Token(OPERATOR_GREATER_THAN, ">", sourceToken.location()));
        }
    }

    /**
     * 是否已到末尾
     */
    public boolean isEOF() {
        return peekType() == TokenType.EOF;
    }

    // ========== 便捷方法 ==========

    /**
     * 当前 token 的文本
     */
    public String text() {
        return peek().text();
    }

    /**
     * 前方第 n 个 token 的文本
     */
    public String text(int offset) {
        return peek(offset).text();
    }

    /**
     * 剩余 token 数
     */
    public int remaining() {
        return tokens.size() - position;
    }

    @Override
    public String toString() {
        return "TokenStream[pos=" + position + ", current=" + peekType() + "]";
    }
}
