package com.justnothing.engine.v2.resolver;

import com.justnothing.engine.v2.ast.ASTNode;
import com.justnothing.engine.v2.ast.ASTTreeFormatter;
import com.justnothing.engine.v2.parser.Parser;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public class FileBatchTest {
    public static void main(String[] args) throws Exception {
        String[][] files = {
            {"engine_v2 Parser.java", "engine_v2/src/main/java/com/justnothing/engine/v2/parser/Parser.java"},
            {"engine_v2 ExprParser.java", "engine_v2/src/main/java/com/justnothing/engine/v2/parser/ExprParser.java"},
            {"engine_v2 StmtParser.java", "engine_v2/src/main/java/com/justnothing/engine/v2/parser/StmtParser.java"},
            {"engine_v2 ClassParser.java", "engine_v2/src/main/java/com/justnothing/engine/v2/parser/ClassParser.java"},
            {"engine_v2 Lexer.java", "engine_v2/src/main/java/com/justnothing/engine/v2/lexer/Lexer.java"},
            {"engine_v2 ASTNode.java", "engine_v2/src/main/java/com/justnothing/engine/v2/ast/ASTNode.java"},
            {"v1 Parser.java", "engine/src/main/java/com/justnothing/javainterpreter/parser/Parser.java"},
            {"v1 REPL.java", "engine/src/main/java/com/justnothing/javainterpreter/REPL.java"},
            {"v1 ScriptRunner.java", "engine/src/main/java/com/justnothing/javainterpreter/ScriptRunner.java"},
            {"v1 ClassDeclNode.java", "engine/src/main/java/com/justnothing/javainterpreter/ast/nodes/ClassDeclarationNode.java"},
            {"v1 InterpolatedStringNode.java", "engine/src/main/java/com/justnothing/javainterpreter/ast/nodes/InterpolatedStringNode.java"},
            {"v1 MethodCallNode.java", "engine/src/main/java/com/justnothing/javainterpreter/ast/nodes/MethodCallNode.java"},
        };

        int pass = 0, fail = 0;
        for (var f : files) {
            String label = f[0];
            Path path = Paths.get(f[1]);
            if (!Files.exists(path)) {
                System.out.println(label + " — SKIP (not found)");
                continue;
            }
            String src = Files.readString(path);
            long lines = src.lines().count();
            System.out.print(label + " (" + lines + " 行)");

            Parser p = new Parser(src, label);
            List<ASTNode> decls = p.parseProgram();
            int errs = p.getErrors().size();
            if (errs > 0) {
                System.out.println(" — PARSE FAIL: " + errs + " errors");
                p.getErrors().stream().limit(3).forEach(e -> System.out.println("    " + e));
                fail++;
                continue;
            }
            System.out.print(" ✓ parse");

            try {
                ScopeResolver r = new ScopeResolver();
                r.resolve(decls);
                System.out.println(" ✓ resolver");
                pass++;
            } catch (Exception e) {
                System.out.println(" — RESOLVER FAIL: " + e.getClass().getSimpleName() + ": " + e.getMessage());
                fail++;
            }
        }
        System.out.println("\n=== " + pass + " passed, " + fail + " failed ===");
    }
}
