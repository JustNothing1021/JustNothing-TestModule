package com.justnothing.engine.v2;

import com.justnothing.engine.v2.ast.ASTNode;
import com.justnothing.engine.v2.lexer.Lexer;
import com.justnothing.engine.v2.parser.Parser;
import com.justnothing.engine.v2.resolver.ScopeResolver;

import java.nio.file.*;
import java.util.*;

public class ProjectScan {
    public static void main(String[] args) throws Exception {
        Path projectRoot = Paths.get("").toAbsolutePath();
        System.out.println("扫描项目: " + projectRoot);
        System.out.println();

        List<Path> allFiles = new ArrayList<>();
        try (var walk = Files.walk(projectRoot)) {
            walk.filter(p -> p.toString().endsWith(".java")
                    && !p.toString().contains("\\build\\")
                    && !p.toString().contains("/build/")
                    && !p.toString().contains("\\.gradle\\")
                    && !p.toString().contains("/.gradle/")
                    // && !p.toString().contains("\\system_source\\")
                    // && !p.toString().contains("/system_source/")
                    )
                 .forEach(allFiles::add);
        }

        System.out.println("找到 " + allFiles.size() + " 个 .java 文件");
        System.out.println();

        record FileResult(String path, int lines, int chars,
                          long lexerMs, long parserMs, long resolverMs,
                          int errors, boolean resolved) {}

        List<FileResult> results = new ArrayList<>();
        int parseOk = 0, parseFail = 0, resolveOk = 0, resolveFail = 0;

        for (int i = 0; i < allFiles.size(); i++) {
            Path f = allFiles.get(i);
            String rel = projectRoot.relativize(f).toString().replace('\\', '/');
            String src = Files.readString(f);
            int lines = (int) src.lines().count();
            int chars = src.length();

            System.out.printf("[%3d/%d] %s (%d行)%n", i + 1, allFiles.size(), rel, lines);

            long t0 = System.nanoTime();
            List<com.justnothing.engine.v2.lexer.Token> tokens;
            try { tokens = new Lexer(src, rel).tokenize(); }
            catch (Exception e) {
                long ms = (System.nanoTime() - t0) / 1_000_000;
                System.out.printf("  LEXER ERROR: %s (%dms)%n", e.getMessage(), ms);
                results.add(new FileResult(rel, lines, chars, ms, 0, 0, -1, false));
                parseFail++;
                continue;
            }
            long lexerMs = (System.nanoTime() - t0) / 1_000_000;

            long t1 = System.nanoTime();
            List<ASTNode> decls;
            try {
                Parser p = new Parser(tokens, rel);
                decls = p.parseProgram();
                long parserMs = (System.nanoTime() - t1) / 1_000_000;
                int errs = p.getErrors().size();

                if (errs == 0) {
                    parseOk++;
                    // Resolver
                    long t2 = System.nanoTime();
                    boolean resolved = false;
                    try {
                        new ScopeResolver().resolve(decls);
                        resolved = true;
                        resolveOk++;
                    } catch (Exception e) {
                        System.out.printf("  RESOLVER: %s%n", e.getClass().getSimpleName() + ": " + e.getMessage());
                        resolveFail++;
                    }
                    long resolverMs = (System.nanoTime() - t2) / 1_000_000;
                    results.add(new FileResult(rel, lines, chars, lexerMs, parserMs, resolverMs, 0, resolved));
                    System.out.printf("  OK  lex=%dms parse=%dms resolve=%dms%n", lexerMs, parserMs, resolverMs);
                } else {
                    parseFail++;
                    results.add(new FileResult(rel, lines, chars, lexerMs, parserMs, 0, errs, false));
                    System.out.printf("  PARSE: %d errors%n", errs);
                    p.getErrors().stream().limit(5).forEach(e -> System.out.println("    " + e));
                }
            } catch (Exception e) {
                long parserMs = (System.nanoTime() - t1) / 1_000_000;
                System.out.printf("  CRASH: %s (%dms)%n", e.getClass().getSimpleName() + ": " + e.getMessage(), parserMs);
                parseFail++;
                results.add(new FileResult(rel, lines, chars, lexerMs, parserMs, 0, -1, false));
            }
        }

        // 汇总
        System.out.println();
        System.out.println("═══════════════════════════════════════");
        System.out.println("  总计文件: " + allFiles.size());
        System.out.println("  解析通过: " + parseOk);
        System.out.println("  解析失败: " + parseFail);
        System.out.println("  Resolver通过: " + resolveOk);
        System.out.println("  Resolver失败: " + resolveFail);
        System.out.println();

        if (parseFail > 0) {
            System.out.println("── 解析失败文件 ──");
            for (var r : results) {
                if (r.errors() != 0) System.out.println("  " + r.path() + " (" + r.errors() + " errors)");
            }
        }
        if (resolveFail > 0) {
            System.out.println("── Resolver失败文件 ──");
            for (var r : results) {
                if (r.errors() == 0 && !r.resolved()) System.out.println("  " + r.path());
            }
        }

        // 性能统计
        var parsed = results.stream().filter(r -> r.errors() == 0).toList();
        if (!parsed.isEmpty()) {
            long totalLines = parsed.stream().mapToLong(FileResult::lines).sum();
            long totalChars = parsed.stream().mapToLong(FileResult::chars).sum();
            long totalLexer = parsed.stream().mapToLong(FileResult::lexerMs).sum();
            long totalParser = parsed.stream().mapToLong(FileResult::parserMs).sum();
            long totalResolver = parsed.stream().mapToLong(FileResult::resolverMs).sum();
            System.out.println();
            System.out.println("── 性能汇总（仅解析通过的文件）──");
            System.out.printf("  总行数: %d%n", totalLines);
            System.out.printf("  总字符: %d%n", totalChars);
            System.out.printf("  词法:   %dms%n", totalLexer);
            System.out.printf("  语法:   %dms%n", totalParser);
            System.out.printf("  Resolver: %dms%n", totalResolver);
            System.out.printf("  解析吞吐: %.0f 行/秒%n", totalLines / (Math.max(totalLexer + totalParser, 1) / 1000.0));
        }
    }
}
