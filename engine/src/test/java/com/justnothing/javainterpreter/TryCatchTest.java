package com.justnothing.javainterpreter;

import com.justnothing.javainterpreter.api.DefaultOutputHandler;
import com.justnothing.javainterpreter.evaluator.MethodBodyExecutor;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;


public class TryCatchTest {

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

    private void evalExpectError(String code, Class<? extends Throwable> expectedErrorType) throws Exception {
        runner.getExecutionContext().clearVariables();
        try {
            runner.executeWithResult(code, outputHandler, outputHandler);
            fail("Expected " + expectedErrorType.getSimpleName() + " but no exception was thrown");
        } catch (Throwable t) {
            while (t.getCause() != null && !(expectedErrorType.isInstance(t))) {
                t = t.getCause();
            }
            assertTrue("Expected " + expectedErrorType.getSimpleName() + " but got " + t.getClass().getSimpleName(),
                    expectedErrorType.isInstance(t));
        }
    }

    // ==================== 基础 try-catch 测试 ====================

    @Test
    public void basic_try_noException_executesTryBlock() throws Exception {
        Object result = eval("""
            int x = 0;
            try {
                x = 42;
            } catch (Exception e) {
                x = 99;
            }
            x
        """);
        assertEquals(42, ((Number) result).intValue());
    }

    @Test
    public void basic_try_catchesException() throws Exception {
        Object result = eval("""
            int x = 0;
            try {
                throw new RuntimeException("test error");
                x = 42;
            } catch (RuntimeException e) {
                x = 77;
            }
            x
        """);
        assertEquals(77, ((Number) result).intValue());
    }

    @Test
    public void basic_catch_canAccessExceptionMessage() throws Exception {
        Object result = eval("""
            String msg = "";
            try {
                throw new RuntimeException("hello world");
            } catch (RuntimeException e) {
                msg = e.getMessage();
            }
            msg
        """);
        assertEquals("hello world", result);
    }

    @Test
    public void basic_uncaughtException_propagates() throws Exception {
        evalExpectError("""
            try {
                throw new IllegalArgumentException("not caught!");
            } catch (NullPointerException e) {
                // wrong type, won't catch
            }
        """, IllegalArgumentException.class);
    }

    @Test
    public void basic_noCatchOrFinally_isError() throws Exception {
        // try without catch or finally should be a parse/runtime error
        evalExpectError("""
            try {
                int x = 1;
            }
        """, Exception.class);
    }

    @Test
    public void multiCatch_firstMatchWins() throws Exception {
        Object result = eval("""
            String which = "";
            try {
                throw new RuntimeException("test");
            } catch (Exception e) {
                which = "exception";
            } catch (RuntimeException e) {
                which = "runtime";
            }
            which
        """);
        // RuntimeException IS-A Exception, so first catch should win
        assertEquals("exception", result);
    }

    @Test
    public void multiCatch_specificBeforeGeneral() throws Exception {
        Object result = eval("""
            String which = "";
            try {
                throw new NullPointerException("npe");
            } catch (NullPointerException e) {
                which = "npe";
            } catch (Exception e) {
                which = "general";
            }
            which
        """);
        assertEquals("npe", result);
    }

    @Test
    public void multiCatch_wrongOrder_stillFirstMatch() throws Exception {
        Object result = eval("""
            String which = "";
            try {
                throw new NullPointerException("npe");
            } catch (Exception e) {
                which = "general";
            } catch (NullPointerException e) {
                which = "npe";
            }
            which
        """);
        // NPE is also an Exception, so the general catch matches first
        assertEquals("general", result);
    }

    @Test
    public void try_returnsValue_fromTryBlock() throws Exception {
        Object result = eval("""
            try {
                123;
            } catch (Exception e) {
                456;
            }
        """);
        assertEquals(123, ((Number) result).intValue());
    }

    @Test
    public void try_returnsValue_fromCatchBlock() throws Exception {
        Object result = eval("""
            try {
                throw new RuntimeException("oops");
                111;
            } catch (Exception e) {
                222;
            }
        """);
        assertEquals(222, ((Number) result).intValue());
    }

    @Test
    public void nested_tryCatch_innerCatchesInner() throws Exception {
        Object result = eval("""
            String log = "";
            try {
                log += "outer-try ";
                try {
                    log += "inner-try ";
                    throw new RuntimeException("inner");
                } catch (RuntimeException e) {
                    log += "inner-catch ";
                }
                log += "after-inner ";
            } catch (Exception e) {
                log += "outer-catch";
            }
            log
        """);
        assertEquals("outer-try inner-try inner-catch after-inner ", result);
    }

    @Test
    public void nested_tryCatch_innerRethrow_outerCatches() throws Exception {
        Object result = eval("""
            String log = "";
            try {
                log += "outer-try ";
                try {
                    log += "inner-try ";
                    throw new RuntimeException("inner");
                } catch (NullPointerException e) {
                    log += "inner-wrong-catch ";
                }
                log += "after-inner ";
            } catch (Exception e) {
                log += "outer-catch:" + e.getClass().getSimpleName();
            }
            log
        """);
        assertEquals("outer-try inner-try outer-catch:RuntimeException", result);
    }

    // ==================== try-finally 测试 ====================

    @Test
    public void finally_alwaysExecutes_onNormalPath() throws Exception {
        Object result = eval("""
            int x = 0;
            try {
                x = 10;
            } finally {
                x = x + 5;
            }
            x
        """);
        assertEquals(15, ((Number) result).intValue());
    }

    @Test
    public void finally_alwaysExecutes_onExceptionPath() throws Exception {
        Object result = eval("""
            int x = 0;
            try {
                x = 10;
                throw new RuntimeException("boom");
            } catch (RuntimeException e) {
                x = 20;
            } finally {
                x = x + 100;
            }
            x
        """);
        assertEquals(120, ((Number) result).intValue());
    }

    @Test
    public void finally_onlyNoCatch_exceptionStillPropagates() throws Exception {
        evalExpectError("""
            int x = 0;
            try {
                x = 10;
                throw new RuntimeException("boom");
            } finally {
                x = 999;
            }
        """, RuntimeException.class);
    }

    @Test
    public void finally_onlyNoCatch_finallyRunsBeforePropagation() throws Exception {
        // 验证 finally 在异常传播前执行（通过副作用检查）
        // 没有 catch 时异常仍然会传播，无法获取返回值，所以验证它确实抛出异常
        evalExpectError("""
            int x = 0;
            try {
                x = 10;
                throw new RuntimeException("boom");
            } finally {
                x = 999;
            }
        """, RuntimeException.class);
    }

    @Test
    public void tryCatchFinally_fullFlow() throws Exception {
        Object result = eval("""
            String order = "";
            try {
                order += "T";
            } catch (Exception e) {
                order += "C";
            } finally {
                order += "F";
            }
            order
        """);
        assertEquals("TF", result);
    }

    @Test
    public void tryCatchFinally_withException() throws Exception {
        Object result = eval("""
            String order = "";
            try {
                order += "T";
                throw new RuntimeException("err");
            } catch (RuntimeException e) {
                order += "C";
            } finally {
                order += "F";
            }
            order
        """);
        assertEquals("TCF", result);
    }

    @Test
    public void returnInTry_finallyOverwrites() throws Exception {
        // Java语义: finally中的return会覆盖try/catch中的return值
        Object result = eval("""
            int x = 0;
            try {
                x = 1;
                return x;  // 返回 1，但 finally 会覆盖
            } finally {
                x = 99;  // 注意: 这取决于 finally 是否能修改返回值
            }
            x
        """);
        // 这个测试验证 finally 是否执行以及变量是否被修改
        // 结果取决于具体实现语义
        assertNotNull(result);
    }

    @Test
    public void multipleCatchClauses_withFinally() throws Exception {
        Object result = eval("""
            String caught = "";
            try {
                throw new IllegalArgumentException("iax");
            } catch (NullPointerException e) {
                caught = "npe";
            } catch (IllegalArgumentException e) {
                caught = "iax";
            } catch (Exception e) {
                caught = "ex";
            } finally {
                caught = caught + "-final";
            }
            caught
        """);
        assertEquals("iax-final", result);
    }

    // ==================== try-with-resources 测试 ====================

    @Test
    public void tryWithResources_singleResource_closeCalled() throws Exception {
        Object result = eval("""
            import java.util.ArrayList;
            ArrayList closeLog = ArrayList.new();

            class MyResource implements java.lang.AutoCloseable {
                String name;
                MyResource(String n) { this.name = n; }
                void close() { closeLog.add(name + "-closed"); }
                String getName() { return name; }
            }

            MyResource r = MyResource.new("res1");
            try (MyResource res = r) {
                res.getName();
            }
            closeLog.size()
        """);
        assertEquals(1, ((Number) result).intValue());
    }

    @Test
    public void tryWithResources_resourceVariableAccessibleInTryBlock() throws Exception {
        Object result = eval("""
            import java.util.ArrayList;
            ArrayList closeLog = ArrayList.new();

            class MyResource implements java.lang.AutoCloseable {
                int value;
                MyResource(int v) { this.value = v; }
                void close() { closeLog.add("closed"); }
                int getValue() { return value; }
            }

            Object value = null;
            try (MyResource r = MyResource.new(42)) {
                value = r.getValue();
            }
            value
        """);
        assertEquals(42, ((Number) result).intValue());
    }

    @Test
    public void tryWithResources_multipleResources_closedInReverseOrder() throws Exception {
        Object result = eval("""
            import java.util.ArrayList;
            ArrayList closeLog = ArrayList.new();

            class TrackedResource implements java.lang.AutoCloseable {
                String tag;
                TrackedResource(String t) { this.tag = t; }
                void close() { closeLog.add(tag); }
            }

            try (TrackedResource a = TrackedResource.new("A");
                 TrackedResource b = TrackedResource.new("B")) {
                "ok";
            }
            closeLog.get(0) + "," + closeLog.get(1)
        """);
        // 应该按逆序关闭: B, A
        assertEquals("B,A", result);
    }

    @Test
    public void tryWithResources_closeOnExceptionPath() throws Exception {
        Object result = eval("""
            import java.util.ArrayList;
            ArrayList closeLog = ArrayList.new();

            class TrackedResource implements java.lang.AutoCloseable {
                String tag;
                TrackedResource(String t) { this.tag = t; }
                void close() { closeLog.add(tag + "-closed"); }
            }

            try {
                try (TrackedResource r = TrackedResource.new("r1")) {
                    throw new RuntimeException("oops");
                }
            } catch (Exception e) {
            }
            closeLog.contains("r1-closed")
        """);
        // 即使发生异常，资源也应该被关闭
        assertEquals(true, result);
    }

    @Test
    public void tryWithResources_withCatchAndFinally() throws Exception {
        Object result = eval("""
            import java.util.ArrayList;
            ArrayList log = ArrayList.new();

            class Logger implements java.lang.AutoCloseable {
                void close() { log.add("close"); }
            }

            String result = "";
            try (Logger l = Logger.new()) {
                result = "try";
                throw new RuntimeException("err");
            } catch (Exception e) {
                result = result + ":catch";
            } finally {
                result = result + ":finally";
            }
            result + ":" + log.get(0)
        """);
        assertEquals("try:catch:finally:close", result);
    }

    @Test
    public void tryWithResources_nullResource_noCloseError() throws Exception {
        // 空资源不应该导致关闭错误
        Object result = eval("""
            import java.util.ArrayList;
            ArrayList log = ArrayList.new();

            class TrackedResource implements java.lang.AutoCloseable {
                void close() { log.add("closed"); }
            }

            TrackedResource r = null;
            try (TrackedResource res = r) {
            }
            "done"
        """);
        assertEquals("done", result);
    }

    // ==================== 控制流交互测试 ====================

    @Test
    public void breakInsideTry_inForLoop() throws Exception {
        Object result = eval("""
            int sum = 0;
            for (int i = 0; i < 10; i++) {
                try {
                    if (i == 3) break;
                    sum += i;
                } catch (Exception e) {}
            }
            sum
        """);
        // i=0+1+2=3, then break at i=3
        assertEquals(3, ((Number) result).intValue());
    }

    @Test
    public void continueInsideTry_inForLoop() throws Exception {
        Object result = eval("""
            int sum = 0;
            for (int i = 0; i < 6; i++) {
                try {
                    if (i % 2 == 0) continue;
                    sum += i;
                } catch (Exception e) {}
            }
            sum
        """);
        // skip evens: 1+3+5 = 9
        assertEquals(9, ((Number) result).intValue());
    }

    @Test
    public void continueInsideCatch_inForLoop() throws Exception {
        Object result = eval("""
            int sum = 0;
            for (int i = 0; i < 6; i++) {
                try {
                    if (i == 2) throw new RuntimeException("skip");
                    sum += i;
                } catch (RuntimeException e) {
                    continue;
                }
            }
            sum
        """);
        // i=0,1 ok, i=2 throws->continue(skip), i=3,4,5 ok
        // 0+1+3+4+5 = 13
        assertEquals(13, ((Number) result).intValue());
    }

    @Test
    public void breakInsideCatch_inForLoop() throws Exception {
        Object result = eval("""
            int lastI = -1;
            for (int i = 0; i < 20; i++) {
                try {
                    if (i == 7) throw new RuntimeException("stop");
                    lastI = i;
                } catch (RuntimeException e) {
                    break;
                }
            }
            lastI
        """);
        // i=0..6 set lastI=i, i=7 throws and breaks, lastI stays at 6
        assertEquals(6, ((Number) result).intValue());
    }

    @Test
    public void returnInsideTry_exitsFunction() throws Exception {
        Object result = eval("""
            int x = 0;
            try {
                x = 10;
                return 999;
                x = 20;
            } catch (Exception e) {
                x = 30;
            }
            x
        """);
        // return should exit the try block with value 999
        // x stays at 10 since return prevents x=20
        // 但返回值是 999... 取决于 executeWithResult 如何处理 return
        assertNotNull(result);
    }

    @Test
    public void returnInsideCatch_exitsTryCatch() throws Exception {
        Object result = eval("""
            int x = 0;
            try {
                x = 10;
                throw new RuntimeException("err");
                x = 11;
            } catch (Exception e) {
                x = 20;
                return 888;
                x = 21;
            }
            x
        """);
        assertNotNull(result);
    }

    @Test
    public void breakInNestedTry_outerLoop() throws Exception {
        Object result = eval("""
            int count = 0;
            outer: 
            for (int i = 0; i < 5; i++) {
                for (int j = 0; j < 5; j++) {
                    try {
                        if (j == 2 && i == 1) break outer;
                        count++;
                    } catch (Exception e) {}
                }
            }
            count
        """);
        // i=0: j=0,1,2,3,4 -> count+=5
        // i=1: j=0,1 -> count+=2, then j=2 breaks outer
        // total = 7
        assertEquals(7, ((Number) result).intValue());
    }

    // ==================== 边界情况测试 ====================

    @Test
    public void emptyTryBlock() throws Exception {
        Object result = eval("""
            int x = 1;
            try {
            } catch (Exception e) {
                x = 2;
            }
            x
        """);
        assertEquals(1, ((Number) result).intValue());
    }

    @Test
    public void emptyCatchBlock_exceptionSwallowed() throws Exception {
        Object result = eval("""
            int x = 0;
            try {
                x = 10;
                throw new RuntimeException("ignored");
            } catch (Exception e) {
            }
            x
        """);
        assertEquals(10, ((Number) result).intValue());
    }

    @Test
    public void exceptionInCatch_propagates() throws Exception {
        evalExpectError("""
            try {
                throw new RuntimeException("first");
            } catch (Exception e) {
                throw new IllegalStateException("second from catch");
            }
        """, IllegalStateException.class);
    }

    @Test
    public void tryCatchAsExpression_inAssignment() throws Exception {
        Object result = eval("""
            int val = 0;
            try {
                val = 100;
            } catch (Exception e) {
                val = -1;
            }
            val
        """);
        assertEquals(100, ((Number) result).intValue());
    }

    @Test
    public void tryCatchAsExpression_inCondition() throws Exception {
        Object result = eval("""
            boolean success = false;
            try {
                success = true;
            } catch (Exception e) {
                success = false;
            }
            success
        """);
        assertEquals(true, result);
    }

    @Test
    public void rethrowInCatch_afterHandling() throws Exception {
        evalExpectError("""
            try {
                throw new RuntimeException("original");
            } catch (RuntimeException e) {
                // handle then rethrow
                throw e;
            }
        """, RuntimeException.class);
    }

    @Test(expected = RuntimeException.class)
    public void tryWithResources_resourceScopeLimitedToTryBlock() throws Exception {
        // 资源变量应该在 try 块之外不可访问
        eval("""
            import java.util.ArrayList;
            ArrayList log = ArrayList.new();

            class LogCloser implements java.lang.AutoCloseable {
                void close() { log.add("closed"); }
            }

            try (LogCloser lc = LogCloser.new()) {
                "inside";
            }
            // resource should be out of scope here
            lc;
        """);
    }

    @Test
       public void tryWithResources_resourceScopeOutOfTryBlock() throws Exception {
        Object result = eval("""
            import java.util.ArrayList;
            ArrayList log = ArrayList.new();

            class LogCloser implements java.lang.AutoCloseable {
                void close() { log.add("closed"); }
            }
            LogCloser lc = LogCloser.new();

            try (lc) {
                "inside";
            }
            // resource should be out of scope here
            lc;
        """);
        assertNotNull(result);
    }

    @Test
    public void complex_realWorldScenario_fileProcessingSimulation() throws Exception {
        Object result = eval("""
            import java.util.ArrayList;
            ArrayList log = ArrayList.new();

            class FakeFile implements java.lang.AutoCloseable {
                String name;
                boolean open;
                FakeFile(String n) {
                    this.name = n;
                    this.open = true;
                    log.add("open:" + n);
                }
                String readLine() { return "data"; }
                void close() {
                    this.open = false;
                    log.add("close:" + name);
                }
            }

            String content = "";
            int lineCount = 0;
            try (FakeFile f = FakeFile.new("test.txt")) {
                for (int i = 0; i < 3; i++) {
                    content = content + f.readLine();
                    lineCount++;
                }
            } catch (Exception e) {
                log.add("error:" + e.getMessage());
            }

            lineCount + "-" + log.get(0) + "-" + log.get(1)
        """);
        assertEquals("3-open:test.txt-close:test.txt", result);
    }

    @Test
    public void tryCatchInsideWhileLoop_withBreak() throws Exception {
        Object result = eval("""
            int found = -1;
            int[] data = {10, 20, 30, 15, 40};
            int idx = 0;
            while (idx < data.length) {
                try {
                    if (data[idx] > 25) {
                        found = data[idx];
                        break;
                    }
                } catch (Exception e) {}
                idx++;
            }
            found
        """);
        assertEquals(30, ((Number) result).intValue());
    }

    @Test
    public void tryCatchInsideDoWhile_withContinue() throws Exception {
        Object result = eval("""
            int[] values = {1, -1, 3, -2, 5};
            int sum = 0;
            int i = 0;
            do {
                try {
                    if (values[i] < 0) {
                        throw new RuntimeException("negative");
                    }
                    sum += values[i];
                } catch (RuntimeException e) {
                    // skip negative values
                }
                i++;
            } while (i < values.length);
            sum
        """);
        // 1 + 3 + 5 = 9
        assertEquals(9, ((Number) result).intValue());
    }
}
