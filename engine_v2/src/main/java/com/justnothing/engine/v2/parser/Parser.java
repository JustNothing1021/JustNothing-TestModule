package com.justnothing.engine.v2.parser;

import com.justnothing.engine.v2.ast.ASTNode;
import com.justnothing.engine.v2.ast.SourceLocation;
import com.justnothing.engine.v2.lexer.Lexer;
import com.justnothing.engine.v2.lexer.Token;
import com.justnothing.engine.v2.lexer.TokenType;

import java.util.ArrayList;
import java.util.List;

/**
 * 解析器入口，协调各子解析器。
 *
 * @author JustNothing1021
 */
public class Parser {

    private final ParseContext ctx;
    private final ExprParser exprParser;
    private final StmtParser stmtParser;
    private final DeclParser declParser;

    public Parser(List<Token> tokens, String sourceName) {
        this.ctx = new ParseContext(tokens, sourceName);
        this.exprParser = new ExprParser(ctx);
        this.stmtParser = new StmtParser(ctx, exprParser);
        this.exprParser.setStmtParser(stmtParser);
        TypeParser typeParser = new TypeParser(ctx);
        this.declParser = new DeclParser(ctx, exprParser, stmtParser, typeParser);
        this.exprParser.setClassParser(this.declParser.getClassParser());
        this.stmtParser.setClassParser(this.declParser.getClassParser());
    }

    public Parser(String source, String sourceName) {
        this(tokenizeOrEmpty(source, sourceName), sourceName);
    }

    private static List<Token> tokenizeOrEmpty(String source, String sourceName) {
        try {
            return new Lexer(source, sourceName).tokenize();
        } catch (Exception e) {
            return List.of(new Token(TokenType.EOF, "",
                    new SourceLocation(1, 1, sourceName)));
        }
    }

    /**
     * 解析完整程序，返回声明列表
     */
    public List<ASTNode> parseProgram() {
        List<ASTNode> declarations = new ArrayList<>();
        while (!ctx.isEOF()) {
            declarations.add(declParser.parseDeclaration());
        }
        return declarations;
    }

    /**
     * 解析单条语句
     */
    public ASTNode parseStatement() {
        return stmtParser.parse();
    }

    /**
     * 解析单个表达式
     */
    public ASTNode parseExpression() {
        return exprParser.parse();
    }

    /**
     * 是否有解析错误
     */
    public boolean hasErrors() {
        return ctx.hasErrors();
    }

    /**
     * 获取解析错误列表
     */
    public List<ParseContext.ParseError> getErrors() {
        return ctx.getErrors();
    }
}
