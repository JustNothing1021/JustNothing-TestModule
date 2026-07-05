package com.justnothing.engine.v2;

import com.justnothing.engine.v2.ast.ASTNode;
import com.justnothing.engine.v2.parser.Parser;
import com.justnothing.engine.v2.resolver.ScopeResolver;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/** 解析器冒烟 — 逐步推进 + 多文件测试 */
public class SmokeTest {
    public static void main(String[] args) throws Exception {
        // === 递进测试 ===
        testI("import+空class",
            "package p; import java.util.List; public class AstRepl { }");
        testI("class+void方法",
            "package p; import java.util.List; public class AstRepl { public static void main(String[] args) { } }");
        testI("+while+switchStmt",
            "package p; import java.util.List; public class AstRepl { public static void main(String[] args) { while(true){ switch(x){ case 1 -> { } default -> { } } } } }");
        testI("+switchExpr+yield+多行block",
            "package p; import java.util.List; public class AstRepl { public static void main(String[] args) { String r = switch(x) { case EXPR -> { ASTNode n = parse(s); yield format(n); } default -> 0; }; } }");
        testI("+forInit+tryCatch+StringBuilder",
            "package p; import java.util.List; public class AstRepl { public static void main(String[] args) { Scanner s = new Scanner(System.in); Mode m = Mode.STMT; while(true){ if(!line.endsWith(\";\")) { while(true){} } try { String r = switch(x) { case EXPR -> { ASTNode n = parse(s); yield format(n); } default -> 0; }; } catch(Exception e){} } } }");

        // === 真实文件测试 ===
        String base = "engine_v2/src/main/java/com/justnothing/engine/v2/";
        testF("AstRepl.java",       base + "AstRepl.java");
        testF("ParseContext.java",  base + "parser/ParseContext.java");
        testF("ExprParser.java",    base + "parser/ExprParser.java");
        testF("StmtParser.java",    base + "parser/StmtParser.java");
        testF("TypeParser.java",    base + "parser/TypeParser.java");
        testF("DecisionTree.java",  base + "parser/trie/DecisionTree.java");
        testF("DecisionTreeBuilder.java", base + "parser/trie/DecisionTreeBuilder.java");
    }

    static void testI(String label, String src) {
        System.out.print(label);
        Parser p = new Parser(src, "test");
        List<ASTNode> decls = p.parseProgram();
        int errs = p.getErrors().size();
        System.out.println(errs == 0 ? " ✓" : " ✗ " + errs + " 错误, " + decls.size() + " 声明");
        if (errs > 0) p.getErrors().stream().limit(3).forEach(e -> System.out.println("    " + e));
    }

    static void testF(String label, String path) throws Exception {
        String src = Files.readString(Path.of(path));
        System.out.print(label + " (" + src.lines().count() + " 行)");
        Parser p = new Parser(src, label);
        List<ASTNode> decls = p.parseProgram();
        int errs = p.getErrors().size();
        System.out.print(errs == 0 ? " ✓" : " ✗ " + errs + " 错误, " + decls.size() + " 声明");
        if (errs > 0) p.getErrors().stream().limit(5).forEach(e -> System.out.println("    " + e));

        // Resolver 验证
        if (errs == 0) {
            try {
                ScopeResolver resolver = new ScopeResolver();
                resolver.resolve(decls);
                System.out.println("  resolver ✓");
            } catch (Exception e) {
                System.out.println("  resolver ✗ " + e.getClass().getSimpleName() + ": " + e.getMessage());
            }
        } else {
            System.out.println();
        }
    }
}