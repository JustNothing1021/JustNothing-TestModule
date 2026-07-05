package com.justnothing.engine.v2;

import com.justnothing.engine.v2.ast.ASTNode;
import com.justnothing.engine.v2.lexer.Lexer;
import com.justnothing.engine.v2.parser.Parser;
import com.justnothing.engine.v2.resolver.ScopeResolver;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

/**
 * engine_v2 解析器 + Resolver 性能基准测试。
 */
public class ParseBenchmark {

    private static final int WARMUP = 3;
    private static final int ROUNDS = 30;

    record Sample(String label, String source) {}

    public static void main(String[] args) throws Exception {
        Sample[] samples = {
            new Sample("v1 Parser.java (3798行)",
                Files.readString(Paths.get("engine/src/main/java/com/justnothing/javainterpreter/parser/Parser.java"))),
            new Sample("engine_v2 Lexer.java (1012行)",
                Files.readString(Paths.get("engine_v2/src/main/java/com/justnothing/engine/v2/lexer/Lexer.java"))),
            new Sample("engine_v2 ExprParser.java (751行)",
                Files.readString(Paths.get("engine_v2/src/main/java/com/justnothing/engine/v2/parser/ExprParser.java"))),
            new Sample("engine_v2 StmtParser.java (616行)",
                Files.readString(Paths.get("engine_v2/src/main/java/com/justnothing/engine/v2/parser/StmtParser.java"))),
            new Sample("v1 ASTNode.java (256行)",
                Files.readString(Paths.get("engine/src/main/java/com/justnothing/javainterpreter/ast/ASTNode.java"))),
        };

        for (Sample s : samples) {
            benchmark(s);
        }
    }

    static void benchmark(Sample s) {
        String src = s.source();
        int lines = (int) src.lines().count();
        int chars = src.length();

        System.out.println("═══════ " + s.label() + " ═══════");
        System.out.printf("  %d 行, %d 字符%n", lines, chars);

        // 预热
        for (int i = 0; i < WARMUP; i++) {
            runOnce(src, true, true);
        }

        // 测试: parse only
        long[] parTimes = new long[ROUNDS];
        long[] lexTimes = new long[ROUNDS];
        for (int i = 0; i < ROUNDS; i++) {
            var r = runOnce(src, true, false);
            lexTimes[i] = r.lexerNs;
            parTimes[i] = r.parserNs;
        }

        // 测试: parse + resolver
        long[] resTimes = new long[ROUNDS];
        for (int i = 0; i < ROUNDS; i++) {
            var r = runOnce(src, true, true);
            resTimes[i] = r.lexerNs + r.parserNs + r.resolverNs;
        }

        Arrays.sort(parTimes);
        Arrays.sort(lexTimes);
        Arrays.sort(resTimes);

        double avgLex = avg(lexTimes) / 1e6;
        double avgPar = avg(parTimes) / 1e6;
        double avgRes = avg(resTimes) / 1e6;

        System.out.println("┌──────────────────────────────────────────┐");
        System.out.printf("│  词法   %6.2f ms                         │%n", avgLex);
        System.out.printf("│  语法   %6.2f ms  (p50 %6.2f  p90 %6.2f) │%n",
                avgPar, parTimes[ROUNDS/2]/1e6, parTimes[(int)(ROUNDS*0.9)]/1e6);
        System.out.printf("│  吞吐   %7.0f 行/秒                    │%n", lines / (avgPar / 1000));
        System.out.printf("│  解析器 %6.2f ms (lex+parse)             │%n", avgLex + avgPar);
        System.out.printf("│  +Resolver %6.2f ms                    │%n", avgRes);
        System.out.println("└──────────────────────────────────────────┘");
        System.out.println();
    }

    record Result(long lexerNs, long parserNs, long resolverNs) {}

    static Result runOnce(String source, boolean runResolver, boolean warmup) {
        List<com.justnothing.engine.v2.lexer.Token> tokens;
        long t0 = System.nanoTime();
        tokens = new Lexer(source, "<bench>").tokenize();
        long lexerNs = System.nanoTime() - t0;

        long t1 = System.nanoTime();
        Parser parser = new Parser(tokens, "<bench>");
        List<ASTNode> nodes = parser.parseProgram();
        long parserNs = System.nanoTime() - t1;
        if (parser.hasErrors()) {
            System.out.println("  PARSE ERROR: " + parser.getErrors().size());
            return new Result(0, 0, 0);
        }

        long resolverNs = 0;
        if (runResolver) {
            long t2 = System.nanoTime();
            new ScopeResolver().resolve(nodes);
            resolverNs = System.nanoTime() - t2;
        }

        return new Result(lexerNs, parserNs, resolverNs);
    }

    private static double avg(long[] arr) {
        return Arrays.stream(arr).average().orElse(0);
    }


}
