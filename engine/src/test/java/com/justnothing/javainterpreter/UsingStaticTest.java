 package com.justnothing.javainterpreter;

import com.justnothing.javainterpreter.api.DefaultOutputHandler;
import com.justnothing.javainterpreter.exception.EvaluationException;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class UsingStaticTest {

    private ScriptRunner runner;
    private DefaultOutputHandler outputHandler;

    @Before
    public void setUp() {
        runner = new ScriptRunner();
        outputHandler = new DefaultOutputHandler();
    }

    private Object eval(String code) throws Exception {
        runner.getExecutionContext().clearVariables();
        return runner.executeWithResult(code, outputHandler, outputHandler);
    }

    // ========== using static 基础功能测试 ==========

    @Test
    public void usingStatic_mathMethods() throws Exception {
        Object result = eval("""
            using static java.lang.Math;
            max(10, 20)
            """);
        assertEquals(20, ((Number) result).intValue());
    }

    @Test
    public void usingStatic_mathFields() throws Exception {
        Object result = eval("""
            using static java.lang.Math;
            PI
            """);
        assertNotNull(result);
        assertTrue(result instanceof Number);
        assertEquals(Math.PI, ((Number) result).doubleValue(), 0.0001);
    }

    @Test
    public void usingStatic_integerMethods() throws Exception {
        Object result = eval("""
            using static java.lang.Integer;
            parseInt("42")
            """);
        assertEquals(42, result);
    }

    @Test
    public void usingStatic_multipleCalls() throws Exception {
        Object result = eval("""
            using static java.lang.Math;
            auto a = max(1, 5);
            auto b = min(3, 7);
            auto c = abs(-10);
            a + b + c
            """);
        assertEquals(18, ((Number) result).intValue());
    }

    @Test
    public void usingStatic_chainedWithExpression() throws Exception {
        Object result = eval("""
            using static java.lang.Math;
            sqrt(pow(2, 4))
            """);
        assertEquals(4.0, ((Number) result).doubleValue(), 0.001);
    }

    // ========== System.out 测试（最常用的场景）==========

    @Test
    public void usingStatic_systemOut() throws Exception {
        Object result = eval("""
            using static java.lang.System;
            out
            """);
        assertSame(System.out, result);
    }

    @Test
    public void usingStatic_systemErr() throws Exception {
        Object result = eval("""
            using static java.lang.System;
            err
            """);
        assertSame(System.err, result);
    }

    // ========== 回归测试：确保旧功能不受影响 ==========

    @Test
    public void regression_importStillWorks() throws Exception {
        Object result = eval("""
            import java.util.Arrays;
            Arrays.toString(new int[]{1, 2, 3})
            """);
        assertEquals("[1, 2, 3]", result);
    }

    @Test
    public void regression_variableDeclaration() throws Exception {
        Object result = eval("int x = 42; x");
        assertEquals(42, result);
    }

    @Test
    public void regression_methodCall() throws Exception {
        Object result = eval("\"hello\".length()");
        assertEquals(5, result);
    }

    @Test
    public void regression_mathDirectCall() throws Exception {
        Object result = eval("Math.max(10, 20)");
        assertEquals(20, result);
    }

    @Test
    public void regression_autoKeyword() throws Exception {
        Object result = eval("auto s = java.lang.System; s.out");
        assertSame(System.out, result);
    }

    @Test
    public void regression_lambda() throws Exception {
        Object result = eval("""
            auto add = (x, y) -> x + y;
            add(3, 4)
            """);
        assertEquals(7, result);
    }

    // ========== 错误处理测试 ==========

    @Test(expected = EvaluationException.class)
    public void error_classNotFound() throws Exception {
        eval("using static com.nonexistent.FakeClass;");
    }

    @Test
    public void error_missingStaticKeyword() throws Exception {
        try {
            eval("using java.lang.Math;");
            fail("Should have thrown ParseException");
        } catch (Exception e) {
            assertTrue(e.getMessage().contains("static") || e.getMessage().contains("Expected"));
        }
    }

    // ========== MethodReference 方法匹配优化测试 ==========

    @Test
    public void methodMatch_maxIntInt_returnsInt() throws Exception {
        Object result = eval("""
            using static java.lang.Math;
            max(10, 20)
            """);
        assertEquals(Integer.class, result.getClass());
        assertEquals(20, result);
    }

    @Test
    public void methodMatch_maxMixedIntDouble_returnsDouble() throws Exception {
        Object result = eval("""
            using static java.lang.Math;
            max(1, 2.0)
            """);
        assertEquals(Double.class, result.getClass());
        assertEquals(2.0, ((Number) result).doubleValue(), 0.001);
    }

    @Test
    public void methodMatch_absInt_returnsInt() throws Exception {
        Object result = eval("""
            using static java.lang.Math;
            abs(-42)
            """);
        assertEquals(Integer.class, result.getClass());
        assertEquals(42, result);
    }

    @Test
    public void methodMatch_absDouble_returnsDouble() throws Exception {
        Object result = eval("""
            using static java.lang.Math;
            abs(-3.14)
            """);
        assertEquals(Double.class, result.getClass());
        assertEquals(3.14, ((Number) result).doubleValue(), 0.001);
    }

    @Test
    public void methodReference_direct_maxIntInt() throws Exception {
        Object result = eval("auto ref = Math::max; ref(1, 2);");
        assertEquals(2, ((Number) result).intValue());
    }

    @Test
    public void methodMatch_diagnostic_maxCandidates() throws Exception {
        Object r1 = eval("using static java.lang.Math; max(10, 20)");
        System.out.println("max(10,20) type=" + r1.getClass().getName() + " val=" + r1);
        Object r2 = eval("using static java.lang.Math; max(1l, 2l)");
        System.out.println("max(1L,2L) type=" + r2.getClass().getName() + " val=" + r2);
        Object r3 = eval("using static java.lang.Math; max(1.0, 2.0)");
        System.out.println("max(1.0,2.0) type=" + r3.getClass().getName() + " val=" + r3);
    }

    @Test
    public void methodRef_directFindMethod_intArgs() throws Exception {
        com.justnothing.javainterpreter.builtins.MethodReference ref =
            new com.justnothing.javainterpreter.builtins.MethodReference(
                Math.class, null, "max", true, null);
        Object result = ref.invoke(Integer.valueOf(10), Integer.valueOf(20));
        System.out.println("max(Integer,Integer) type=" + result.getClass().getName() + " val=" + result);
        assertEquals(Integer.class, result.getClass());
        assertEquals(20, result);
    }

    @Test
    public void methodMatch_diagnostic_argTypes() throws Exception {
        Object r = eval("""
            using static java.lang.Math;
            auto a = 10;
            auto b = 20;
            System.out.println("[SCRIPT] a type=" + a.getClass().getName());
            System.out.println("[SCRIPT] b type=" + b.getClass().getName());
            max(a, b)
            """);
        System.out.println("max(Integer,Integer) type=" + r.getClass().getName() + " val=" + r);
    }

    @Test
    public void publicClassDeclaration() throws Exception {
        eval("public class MyClass { int x; }");
    }

    @Test
    public void publicClassWithMethod() throws Exception {
        Object result = eval("""
            public class Foo {
                int add(int a, int b) { return a + b; }
            }
            auto f = new Foo();
            f.add(3, 4)
            """);
        assertEquals(7, ((Number) result).intValue());
    }

    @Test
    public void finalClassDeclaration() throws Exception {
        eval("final class Bar {}");
    }

    @Test
    public void staticClassDeclaration() throws Exception {
        eval("static class Baz {}");
    }

    @Test
    public void arrayInference_commonSuperType_list() throws Exception {
        Object result = eval("{new java.util.ArrayList(), new java.util.LinkedList()}");
        assertNotNull(result);
        assertTrue(result.getClass().isArray());
        assertNotEquals(Object.class, result.getClass().getComponentType());
    }

    @Test
    public void arrayInference_commonSuperType_mapTypes() throws Exception {
        Object result = eval("{new java.util.HashMap(), new java.util.TreeMap()}");
        assertNotNull(result);
        assertTrue(result.getClass().isArray());
        assertNotEquals(Object.class, result.getClass().getComponentType());
    }

    @Test
    public void arrayInference_sameType_numbers() throws Exception {
        Object result = eval("{1, 2, 3}");
        assertNotNull(result);
        assertTrue(result.getClass().isArray());
        assertEquals(int.class, result.getClass().getComponentType());
    }

    // ========== 内建 max/min 类型感知测试 ==========

    @Test
    public void builtin_max_intArgs_returnsInt() throws Exception {
        Object result = eval("max(10, 20)");
        assertEquals(Integer.class, result.getClass());
        assertEquals(20, result);
    }

    @Test
    public void builtin_min_intArgs_returnsInt() throws Exception {
        Object result = eval("min(5, 3)");
        assertEquals(Integer.class, result.getClass());
        assertEquals(3, result);
    }

    @Test
    public void builtin_max_longArgs_returnsLong() throws Exception {
        Object result = eval("max(1l, 2l)");
        assertEquals(Long.class, result.getClass());
        assertEquals(2L, result);
    }

    @Test
    public void builtin_max_mixedIntDouble_returnsDouble() throws Exception {
        Object result = eval("max(1, 2.0)");
        assertEquals(Double.class, result.getClass());
        assertEquals(2.0, ((Number) result).doubleValue(), 0.001);
    }

    @Test
    public void builtin_max_multipleArgs() throws Exception {
        Object result = eval("max(1, 5, 3, 9, 2)");
        assertEquals(Integer.class, result.getClass());
        assertEquals(9, result);
    }

    // ========== using static 嵌套类解析测试 ==========

    @Test
    public void usingStatic_nestedClass_CharacterSubset() throws Exception {
        Object result = eval("""
            using static java.lang.Character;
            UnicodeBlock.BASIC_LATIN.toString()
            """);
        assertNotNull(result);
    }

    @Test
    public void usingStatic_nestedClass_AbstractMap_SimpleEntry() throws Exception {
        Object result = eval("""
            using static java.util.AbstractMap;
            auto e = new SimpleEntry("key", 42);
            e.getValue()
            """);
        assertEquals(42, ((Number) result).intValue());
    }

    @Test
    public void usingStatic_nestedClass_stillResolvesStaticMembers() throws Exception {
        Object result = eval("""
            using static java.lang.Math;
            auto e = null;
            max(10, 20)
            """);
        assertEquals(Integer.class, result.getClass());
            assertEquals(20, result);
    }

    @Test
    public void usingStatic_nestedClass_chainAccess_fieldThenMethod() throws Exception {
        Object result = eval("""
            using static java.lang.Character;
            UnicodeBlock.BASIC_LATIN.toString()
            """);
        assertNotNull(result);
        assertEquals("BASIC_LATIN", result);
    }

    @Test
    public void usingStatic_nestedClass_chainAccess_deepNesting() throws Exception {
        Object result = eval("""
            using static java.util.AbstractMap;
            SimpleEntry.class.getSimpleName()
            """);
        assertNotNull(result);
        assertEquals("SimpleEntry", result);
    }
}
