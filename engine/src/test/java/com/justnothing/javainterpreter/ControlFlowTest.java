package com.justnothing.javainterpreter;

import com.justnothing.javainterpreter.api.DefaultOutputHandler;
import com.justnothing.javainterpreter.evaluator.MethodBodyExecutor;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;


public class ControlFlowTest {

    private ScriptRunner runner;
    private DefaultOutputHandler outputHandler;

    @Before
    public void setUp() {
        runner = new ScriptRunner();
        outputHandler = new DefaultOutputHandler();
    }

    private Object eval(String code) throws Exception {
        runner.getExecutionContext().clearVariables();
        MethodBodyExecutor.clearAll();
        return runner.executeWithResult(code, outputHandler, outputHandler);
    }


    // ========== continue 测试 ==========

    @Test
    public void continue_inForLoop_skipEvenNumbers() throws Exception {
        // 用户的原始用例: int sum=0; for(int k=0;k<4;k++){ if(k%2==0) continue; sum+=k; } sum
        Object result = eval("""
            int sum = 0;
            for (int k = 0; k < 4; k++) {
                if (k % 2 == 0) continue;
                sum += k;
            }
            sum
        """);
        assertEquals(4, ((Number) result).intValue());
    }

    @Test
    public void continue_inForLoop_skipAll() throws Exception {
        Object result = eval("""
            int count = 0;
            for (int i = 0; i < 10; i++) {
                continue;
                count++;
            }
            count
        """);
        assertEquals(0, ((Number) result).intValue());
    }

    @Test
    public void continue_inWhileLoop() throws Exception {
        Object result = eval("""
            int i = 0;
            int sum = 0;
            while (i < 10) {
                i++;
                if (i % 2 == 0) continue;
                sum += i;
            }
            sum
        """);
        assertEquals(25, ((Number) result).intValue());
    }

    @Test
    public void continue_inDoWhileLoop() throws Exception {
        Object result = eval("""
            int i = 0;
            int sum = 0;
            do {
                i++;
                if (i > 5) continue;
                sum += i;
            } while (i < 10);
            sum
        """);
        assertEquals(15, ((Number) result).intValue());
    }

    @Test
    public void continue_inForEachLoop_withList() throws Exception {
        Object result = eval("""
            import java.util.ArrayList;
            ArrayList list = ArrayList.new();
            list.add(1);
            list.add(2);
            list.add(3);
            list.add(4);
            int sum = 0;
            for (Object item : list) {
                int val = (int)item;
                if (val % 2 == 0) continue;
                sum = sum + val;
            }
            sum
        """);
        assertEquals(4, ((Number) result).intValue());
    }

    @Test
    public void continue_inForEachLoop_withArray() throws Exception {
        Object result = eval("""
            int[] arr = {1, 2, 3, 4, 5};
            int sum = 0;
            for (int x : arr) {
                if (x == 3) continue;
                sum += x;
            }
            sum
        """);
        assertEquals(12, ((Number) result).intValue());
    }

    @Test
    public void continue_inNestedLoops_onlyAffectsInner() throws Exception {
        Object result = eval("""
            int sum = 0;
            for (int i = 0; i < 3; i++) {
                for (int j = 0; j < 3; j++) {
                    if (j == 1) continue;
                    sum++;
                }
                sum += 10;
            }
            sum
        """);
        assertEquals(36, ((Number) result).intValue());
    }

    @Test
    public void continue_inForLoop_stillExecutesUpdateExpression() throws Exception {
        Object result = eval("""
            int[] arr = {0, 0, 0, 0};
            int idx = 0;
            for (int i = 0; i < 4; i++) {
                if (i % 2 == 0) continue;
                arr[idx] = i * 10;
                idx++;
            }
            arr[0]
        """);
        assertEquals(10, ((Number) result).intValue());
    }

    // ========== break 测试 ==========

    @Test
    public void break_inForLoop_exitEarly() throws Exception {
        Object result = eval("""
            int sum = 0;
            for (int i = 0; i < 100; i++) {
                if (i == 5) break;
                sum += i;
            }
            sum
        """);
        assertEquals(10, ((Number) result).intValue());
    }

    @Test
    public void break_inForLoop_firstIteration() throws Exception {
        Object result = eval("""
            int sum = 0;
            for (int i = 0; i < 10; i++) {
                break;
                sum += i;
            }
            sum
        """);
        assertEquals(0, ((Number) result).intValue());
    }

    @Test
    public void break_inWhileLoop() throws Exception {
        Object result = eval("""
            int i = 0;
            while (true) {
                if (i >= 7) break;
                i++;
            }
            i
        """);
        assertEquals(7, ((Number) result).intValue());
    }

    @Test
    public void break_inDoWhileLoop() throws Exception {
        Object result = eval("""
            int i = 0;
            do {
                i++;
                if (i == 5) break;
            } while (true);
            i
        """);
        assertEquals(5, ((Number) result).intValue());
    }

    @Test
    public void break_inForEachLoop_withList() throws Exception {
        Object result = eval("""
            import java.util.ArrayList;
            ArrayList list = ArrayList.new();
            list.add(1);
            list.add(2);
            list.add(3);
            list.add(100);
            list.add(200);
            int sum = 0;
            for (Object item : list) {
                int val = (int)item;
                if (val > 10) break;
                sum = sum + val;
            }
            sum
        """);
        // 1+2+3 = 6, 遇到 100 时 break
        assertEquals(6, ((Number) result).intValue());
    }

    @Test
    public void break_inForEachLoop_withArray() throws Exception {
        Object result = eval("""
            int[] arr = {10, 20, 30, 40, 50};
            int seen = 0;
            for (int x : arr) {
                seen++;
                if (x == 30) break;
            }
            seen
        """);
        assertEquals(3, ((Number) result).intValue());
    }

    @Test
    public void break_inNestedLoops_onlyAffectsInner() throws Exception {
        Object result = eval("""
            int outerCount = 0;
            for (int i = 0; i < 5; i++) {
                outerCount++;
                for (int j = 0; j < 100; j++) {
                    if (j == 3) break;
                }
            }
            outerCount
        """);
        assertEquals(5, ((Number) result).intValue());
    }

    // ========== break + continue 组合测试 ==========

    @Test
    public void breakAndContinue_together_findFirstOddMultiple() throws Exception {
        Object result = eval("""
            int result = -1;
            for (int i = 0; i < 50; i++) {
                if (i % 2 == 0) continue;
                if (i % 7 == 0) {
                    result = i;
                    break;
                }
            }
            result
        """);
        assertEquals(7, ((Number) result).intValue());
    }

    @Test
    public void continue_thenBreak_fizzBuzzStyle() throws Exception {
        Object result = eval("""
            String output = "";
            for (int i = 1; i <= 20; i++) {
                if (i <= 15) continue;
                if (output.length() > 0) break;
                output = "found:" + i;
            }
            output
        """);
        assertEquals("found:16", result);
    }

    // ========== 边界情况测试 ==========

    @Test
    public void continue_inIfInsideLoop_conditionNotMet() throws Exception {
        Object result = eval("""
            int sum = 0;
            for (int i = 0; i < 5; i++) {
                if (false) continue;
                sum += i;
            }
            sum
        """);
        assertEquals(10, ((Number) result).intValue());
    }

    @Test
    public void break_neverTriggered_loopCompletes() throws Exception {
        Object result = eval("""
            int sum = 0;
            for (int i = 0; i < 5; i++) {
                if (false) break;
                sum += i;
            }
            sum
        """);
        assertEquals(10, ((Number) result).intValue());
    }

    @Test
    public void continue_atEndOfLoopBody_noEffect() throws Exception {
        Object result = eval("""
            int sum = 0;
            for (int i = 0; i < 5; i++) {
                sum += i;
                continue;
            }
            sum
        """);
        assertEquals(10, ((Number) result).intValue());
    }

    @Test
    public void multipleContinues_inSingleLoop() throws Exception {
        Object result = eval("""
            int sum = 0;
            for (int i = 0; i < 10; i++) {
                if (i < 3) continue;
                if (i > 7) continue;
                if (i == 5) continue;
                sum += i;
            }
            sum
        """);
        assertEquals(20, ((Number) result).intValue());
    }

    @Test
    public void forLoopWithoutCondition_breakOnlyExit() throws Exception {
        Object result = eval("""
            int count = 0;
            for (;;) {
                count++;
                if (count >= 42) break;
            }
            count
        """);
        assertEquals(42, ((Number) result).intValue());
    }

    @Test
    public void whileTrue_breakExit() throws Exception {
        Object result = eval("""
            int x = 1;
            while (true) {
                x = x * 2;
                if (x > 100) break;
            }
            x
        """);
        assertEquals(128, ((Number) result).intValue());
    }

    // ========== 实际应用场景测试 ==========

    @Test
    public void practical_findFirstPrime() throws Exception {
        Object result = eval("""
            boolean isPrime = false;
            int candidate = 2;
            for (; candidate < 100; candidate++) {
                isPrime = true;
                for (int d = 2; d * d <= candidate; d++) {
                    if (candidate % d == 0) {
                        isPrime = false;
                        break;
                    }
                }
                if (isPrime) break;
            }
            candidate
        """);
        assertEquals(2, ((Number) result).intValue());
    }

    @Test
    public void practical_sumUntilNegative() throws Exception {
        Object result = eval("""
            int[] values = {3, 7, 2, -1, 5, 8};
            int sum = 0;
            for (int v : values) {
                if (v < 0) break;
                sum += v;
            }
            sum
        """);
        assertEquals(12, ((Number) result).intValue());
    }

    @Test
    public void practical_filterAndSum() throws Exception {
        Object result = eval("""
            import java.util.ArrayList;
            ArrayList numbers = ArrayList.new();
            numbers.add(1);
            numbers.add(-2);
            numbers.add(3);
            numbers.add(-4);
            numbers.add(5);
            int positiveSum = 0;
            int negativeCount = 0;
            for (Object n : numbers) {
                int val = (int)n;
                if (val < 0) {
                    negativeCount++;
                    continue;
                }
                positiveSum += val;
            }
            positiveSum * 10 + negativeCount
        """);
        assertEquals(92, ((Number) result).intValue());
    }
}
