package com.justnothing.engine.v2;

import com.justnothing.engine.v2.ast.ASTNode;
import com.justnothing.engine.v2.ast.ASTTreeFormatter;
import com.justnothing.engine.v2.parser.Parser;

import java.util.List;
import java.util.Scanner;

/**
 * AST 可视化 REPL —— 输入代码，输出语法树。
 * <p>
 * 使用方式：<br>
 *   gradlew :engine_v2:classes<br>
 *   java -cp engine_v2/build/classes/java/main com.justnothing.engine.v2.AstRepl
 * </p>
 */
public class AstRepl {

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        System.out.println("╔════════════════════════════════════╗");
        System.out.println("║     engine_v2 AST REPL             ║");
        System.out.println("╠════════════════════════════════════╣");
        System.out.println("║  expr       → 解析为表达式         ║");
        System.out.println("║  stmt       → 解析为语句           ║");
        System.out.println("║  prog       → 解析为程序（声明）   ║");
        System.out.println("║  :tree expr → 切换为 tree 格式     ║");
        System.out.println("║  :flat expr → 切换为 flat 格式     ║");
        System.out.println("║  :exit      → 退出                 ║");
        System.out.println("║  空行       → 结束多行输入         ║");
        System.out.println("╚════════════════════════════════════╝");
        System.out.println();

        Mode mode = Mode.STMT;
        Style style = Style.TREE;

        while (true) {
            System.out.print(style == Style.TREE ? "tree> " : "flat> ");
            String line = scanner.nextLine().trim();

            if (line.isEmpty()) continue;

            // 命令
            if (line.startsWith(":")) {
                switch (line) {
                    case ":exit", ":q" -> { System.out.println("Bye!"); return; }
                    case ":tree" -> style = Style.TREE;
                    case ":flat" -> style = Style.FLAT;
                    case ":tree expr" -> { style = Style.TREE; mode = Mode.EXPR; }
                    case ":tree stmt" -> { style = Style.TREE; mode = Mode.STMT; }
                    case ":tree prog" -> { style = Style.TREE; mode = Mode.PROG; }
                    case ":flat expr" -> { style = Style.FLAT; mode = Mode.EXPR; }
                    case ":flat stmt" -> { style = Style.FLAT; mode = Mode.STMT; }
                    case ":flat prog" -> { style = Style.FLAT; mode = Mode.PROG; }
                    default -> System.out.println("未知命令: " + line);
                }
                continue;
            }

            if (line.equals("expr"))  { mode = Mode.EXPR; continue; }
            if (line.equals("stmt"))  { mode = Mode.STMT; continue; }
            if (line.equals("prog"))  { mode = Mode.PROG; continue; }

            // 收集多行输入
            StringBuilder sb = new StringBuilder(line);
            if (!line.endsWith(";") && !line.endsWith("}")) {
                while (true) {
                    System.out.print("...   ");
                    String next = scanner.nextLine();
                    if (next.trim().isEmpty()) break;
                    sb.append('\n').append(next);
                    if (next.trim().endsWith(";") || next.trim().endsWith("}")) break;
                }
            }

            String source = sb.toString();
            try {
                String result = switch (mode) {
                    case EXPR -> {
                        ASTNode node = parseExpr(source);
                        yield format(node, style);
                    }
                    case STMT -> {
                        ASTNode node = parseStmt(source);
                        yield format(node, style);
                    }
                    case PROG -> {
                        List<ASTNode> nodes = parseProg(source);
                        StringBuilder out = new StringBuilder();
                        for (int i = 0; i < nodes.size(); i++) {
                            out.append(format(nodes.get(i), style));
                            if (i < nodes.size() - 1) out.append('\n');
                        }
                        yield out.toString();
                    }
                };
                System.out.println(result);
            } catch (Exception e) {
                System.out.println("Error: " + e.getMessage());
            }
            System.out.println();
        }
    }

    private static ASTNode parseExpr(String src) {
        Parser p = new Parser(src, "<repl>");
        ASTNode result = p.parseExpression();
        if (p.hasErrors()) throw new RuntimeException(p.getErrors().get(0).toString());
        return result;
    }

    private static ASTNode parseStmt(String src) {
        Parser p = new Parser(src, "<repl>");
        ASTNode result = p.parseStatement();
        if (p.hasErrors()) throw new RuntimeException(p.getErrors().get(0).toString());
        return result;
    }

    private static List<ASTNode> parseProg(String src) {
        Parser p = new Parser(src, "<repl>");
        List<ASTNode> result = p.parseProgram();
        if (p.hasErrors()) throw new RuntimeException(p.getErrors().get(0).toString());
        return result;
    }

    private static String format(ASTNode node, Style style) {
        return switch (style) {
            case TREE -> ASTTreeFormatter.format(node);
            case FLAT -> node.nodeName() + " " + node.getClass().getSimpleName();
        };
    }

    enum Mode { EXPR, STMT, PROG }
    enum Style { TREE, FLAT }
}
