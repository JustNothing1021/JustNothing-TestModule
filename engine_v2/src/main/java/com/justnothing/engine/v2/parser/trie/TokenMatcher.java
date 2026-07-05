package com.justnothing.engine.v2.parser.trie;

import com.justnothing.engine.v2.lexer.TokenType;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Token 类型匹配器：用于 ifMatches/orIfMatches 中的"或"逻辑。
 * <p>
 * 把 {code .ifMatches(KEYWORD_INT, ...).orIfMatches(KEYWORD_VAR, ...).orIfMatches(...)}
 * 简化为 {code .ifMatches(TokenMatcher.any(KEYWORD_INT, KEYWORD_VAR, ...), ...)}。
 * </p>
 *
 * @author JustNothing1021
 */
@FunctionalInterface
public interface TokenMatcher {

    boolean matches(TokenType type);

    /**
     * 匹配任意一个给定的 TokenType（"或"关系）。
     * <pre>{@code
     * import static com.justnothing.engine.v2.parser.trie.TokenMatcher.*;
     * // ...
     * .ifMatches(any(KEYWORD_INT, KEYWORD_VAR, QUALIFIED_NAME, IDENTIFIER),
     *            IDENTIFIER, OPERATOR_COLON)
     * }</pre>
     */
    static TokenMatcher any(TokenType first, TokenType... rest) {
        if (rest.length == 0) {
            return first::equals;
        }
        Set<TokenType> set = new HashSet<>();
        set.add(first);
        Collections.addAll(set, rest);
        return set::contains;
    }

    /**
     * 匹配指定的单一 TokenType，等价于直接传 TokenType，但方便 API 一致。
     */
    static TokenMatcher is(TokenType type) {
        return type::equals;
    }

    /**
     * 从一组 TokenType 创建匹配器。
     */
    static TokenMatcher of(TokenType... types) {
        return any(types[0], Arrays.copyOfRange(types, 1, types.length));
    }
}
