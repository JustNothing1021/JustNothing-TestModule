package com.justnothing.testmodule.command.functions.script.engine_new;

import com.justnothing.testmodule.command.functions.script.engine_new.ast.nodes.BlockNode;
import com.justnothing.testmodule.command.functions.script.engine_new.evaluator.ASTEvaluator;
import com.justnothing.testmodule.command.functions.script.engine_new.evaluator.ExecutionContext;
import com.justnothing.testmodule.command.functions.script.engine_new.lexer.Lexer;
import com.justnothing.testmodule.command.functions.script.engine_new.parser.Parser;

public class PerfTest {
    
    public static void main(String[] args) {
        System.out.println("=== Array Type Checking Test ===\n");
        
        test("new int[] {1, 2, 3};", true, true);
        test("new int[] {1, 2, \"114514\"};", false, true);
        test("new int[3] {1, 2, 3};", true, true);
        test("new int[3] {1, 2, \"3\"};", false, true);
        test("new String[] {\"a\", \"b\", \"c\"};", true, false);
        test("new String[] {\"a\", 1, \"c\"};", false, false);
        test("new int[][] {{1, 2}, {3, 4}};", true, false);
        test("new int[][] {{1, 2}, {\"3\", 4}};", false, true);
        test("new int[][][] {{{1, 2}, {3, 4}}, {{5, 6}, {7, 8}}};", true, false);
    }
    
    private static void test(String code, boolean expectSuccess, boolean isPrimitiveArray) {
        System.out.println("Testing: " + code);
        try {
            Lexer lexer = new Lexer(code);
            var tokens = lexer.tokenize();
            Parser parser = new Parser(tokens, PerfTest.class.getClassLoader());
            BlockNode block = parser.parse();
            
            System.out.println("AST: " + block.formatString().split("\n")[0]);
            
            ExecutionContext context = new ExecutionContext(PerfTest.class.getClassLoader());
            Object result = ASTEvaluator.evaluate(block, context);
            
            if (isPrimitiveArray && result.getClass().isArray()) {
                System.out.println("Result: " + formatPrimitiveArray(result));
            } else {
                System.out.println("Result: " + java.util.Arrays.deepToString((Object[]) result));
            }
            
            if (!expectSuccess) {
                System.out.println("ERROR: Expected failure but succeeded!");
            } else {
                System.out.println("OK");
            }
        } catch (Exception e) {
            if (expectSuccess) {
                System.out.println("ERROR: Expected success but failed: " + e.getMessage());
            } else {
                System.out.println("OK (expected error: " + e.getMessage() + ")");
            }
        }
        System.out.println();
    }
    
    private static String formatPrimitiveArray(Object array) {
        if (array instanceof int[] arr) {
            return java.util.Arrays.toString(arr);
        } else if (array instanceof long[] arr) {
            return java.util.Arrays.toString(arr);
        } else if (array instanceof double[] arr) {
            return java.util.Arrays.toString(arr);
        } else if (array instanceof float[] arr) {
            return java.util.Arrays.toString(arr);
        } else if (array instanceof boolean[] arr) {
            return java.util.Arrays.toString(arr);
        } else if (array instanceof char[] arr) {
            return java.util.Arrays.toString(arr);
        } else if (array instanceof short[] arr) {
            return java.util.Arrays.toString(arr);
        } else if (array instanceof byte[] arr) {
            return java.util.Arrays.toString(arr);
        } else if (array.getClass().isArray()) {
            StringBuilder sb = new StringBuilder("[");
            int len = java.lang.reflect.Array.getLength(array);
            for (int i = 0; i < len; i++) {
                if (i > 0) sb.append(", ");
                sb.append(formatPrimitiveArray(java.lang.reflect.Array.get(array, i)));
            }
            sb.append("]");
            return sb.toString();
        }
        return String.valueOf(array);
    }
}
