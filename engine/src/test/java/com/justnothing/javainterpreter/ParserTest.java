package com.justnothing.javainterpreter;

import com.justnothing.javainterpreter.api.DefaultOutputHandler;
import com.justnothing.javainterpreter.builtins.MethodReference;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;


public class ParserTest {

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

    @Test
    public void switch_basicArrow() throws Exception {
        Object result = eval("""
            int x = 2;
            String result;
            switch (x) {
                case 1 -> result = "one";
                case 2 -> result = "two";
                default -> result = "other";
            }
            result
        """);
        assertEquals("two", result);
    }

    @Test
    public void switch_colonSyntax() throws Exception {
        Object result = eval("""
            int x = 1;
            String result = "default";
            switch (x) {
                case 1:
                    result = "one";
                    break;
                case 2:
                    result = "two";
                    break;
            }
            result
        """);
        assertEquals("one", result);
    }

    @Test
    public void switch_expressionForm() throws Exception {
        Object result = eval("""
            String s = switch (2) {
                case 1 -> "A";
                case 2 -> "B";
                default -> "?";
            };
            s
        """);
        assertEquals("B", result);
    }

    @Test
    public void switch_defaultBranch() throws Exception {
        Object result = eval("""
            String s = switch (99) {
                case 1 -> "A";
                default -> "DEFAULT";
            };
            s
        """);
        assertEquals("DEFAULT", result);
    }

    @Test
    public void switch_stringMatching() throws Exception {
        Object result = eval("""
            String s = switch ("hello") {
                case "foo" -> "F";
                case "hello" -> "H";
                default -> "?";
            };
            s
        """);
        assertEquals("H", result);
    }

    @Test
    public void asi_expressionWithoutSemicolon() throws Exception {
        assertEquals(42, eval("42"));
    }

    @Test
    public void asi_methodCallWithoutSemicolon() throws Exception {
        assertEquals(5, eval("\"hello\".length()"));
    }

    @Test
    public void asi_arithmeticExpression() throws Exception {
        assertEquals(7, eval("1 + 2 * 3"));
    }

    @Test
    public void varDecl_int() throws Exception {
        assertEquals(42, eval("int x = 42; x"));
    }

    @Test
    public void varDecl_string() throws Exception {
        assertEquals("hello", eval("String s = \"hello\"; s"));
    }

    @Test
    public void varDecl_shortDeclaration() throws Exception {
        assertEquals(100, eval("x := 100; x"));
    }

    @Test
    public void op_nullCoalescing() throws Exception {
        assertEquals("fallback", eval("null ?? \"fallback\""));
    }

    @Test
    public void ctrl_ifElse() throws Exception {
        Object result = eval("""
            int x = 10;
            String s;
            if (x > 5) { s = "big"; } else { s = "small"; }
            s
        """);
        assertEquals("big", result);
    }

    @Test
    public void ctrl_forLoop() throws Exception {
        Object result = eval("""
            int sum = 0;
            for (int i = 1; i <= 5; i++) { sum += i; }
            sum
        """);
        assertEquals(15, result);
    }

    @Test
    public void ctrl_whileLoop() throws Exception {
        Object result = eval("""
            int x = 0;
            while (x < 5) { x++; }
            x
        """);
        assertEquals(5, result);
    }

    @Test
    public void ctrl_doWhileLoop() throws Exception {
        Object result = eval("""
            int x = 0;
            do { x++; } while (x < 3);
            x
        """);
        assertEquals(3, result);
    }

    @Test
    public void expr_ternary() throws Exception {
        assertEquals("positive", eval("int x = 10; x > 0 ? \"positive\" : \"negative\""));
    }

    @Test
    public void expr_chainedMethodCalls() throws Exception {
        Object result = eval("\"hello world\".length()");
        assertNotNull(result);
        assertTrue(((Number) result).intValue() > 0);
    }

    @Test
    public void expr_arrayAccess() throws Exception {
        Object result = eval("int[] arr = new int[3]; arr[0] = 99; arr[0]");
        assertEquals(99, result);
    }

    // ========== 类型转换测试 ==========

    @Test
    public void type_casting() throws Exception {
        Object result = eval("double d = 3.14; (int) d");
        assertEquals(3, result);
    }

    // ========== 数组字面量测试 ==========
    @Test
    public void array_literalInBrackets() throws Exception {
        Object result = eval("{1, 2, 3}");
        assertEquals(3, Array.getLength(result));
        assertEquals(1, Array.get(result, 0));
        assertEquals(2, Array.get(result, 1));
        assertEquals(3, Array.get(result, 2));
    }

    @Test
    public void array_braceLiteral() throws Exception {
        Object result = eval("{1, 2, 3}");
        assertNotNull(result);
        assertTrue("应该是数组", result.getClass().isArray());
        assertEquals(3, Array.getLength(result));
        assertEquals(1, Array.get(result, 0));
        assertEquals(2, Array.get(result, 1));
        assertEquals(3, Array.get(result, 2));
    }

    @Test
    public void array_braceLiteralSingleElement() throws Exception {
        Object result = eval("{1}");
        assertNotNull(result);
        assertTrue("应该是数组", result.getClass().isArray());
        assertEquals(1, Array.getLength(result));
        assertEquals(1, Array.get(result, 0));
    }

    @Test
    public void array_braceLiteralAddition() throws Exception {
        Object result = eval("{1} + {2}");
        assertNotNull(result);
        // 这里的结果应该是字符串拼接，因为两个数组相加会被转换为字符串
        assertTrue("应该是字符串", result instanceof String);
    }

    @Test
    public void array_braceLiteralWithSize() throws Exception {
        Object result = eval("new int[3] {1, 2}");
        assertNotNull(result);
        assertTrue("应该是数组", result.getClass().isArray());
        assertEquals(3, Array.getLength(result));
        assertEquals(1, Array.get(result, 0));
        assertEquals(2, Array.get(result, 1));
        assertEquals(0, Array.get(result, 2)); // 应该补齐默认值
    }

    @Test
    public void array_literalWithIndex() throws Exception {
        Object result = eval("[1, 2, 3][1]");
        assertEquals(2, result);
    }

    @Test
    public void array_nestedLiteral() throws Exception {
        Object result = eval("[[1, 2], [3, 4]][1][0]");
        assertEquals(3, result);
    }

    @Test
    public void array_typedAssignment() throws Exception {
        Object result = eval("int[] arr = [10, 20, 30]; arr[1]");
        assertEquals(20, result);
    }

    @Test
    public void array_inferIntType() throws Exception {
        Object result = eval("[1, 2, 3]");
        assertNotNull(result);
        assertTrue("应该是数组", result.getClass().isArray());
        assertEquals(3, Array.getLength(result));
    }

    @Test
    public void array_inferDoubleType() throws Exception {
        Object result = eval("[1.5, 2.5, 3.5]");
        assertNotNull(result);
        assertTrue("应该是数组", result.getClass().isArray());
        Object elem = Array.get(result, 0);
        assertTrue("元素应该是数字", elem instanceof Number);
        assertEquals(1.5, ((Number) elem).doubleValue(), 0.001);
    }

    // ========== 字典测试 ==========

    @Test
    public void dict_literalInBrackets() throws Exception {
        Object result = eval("{\"a\": 1, \"b\": 2, \"c\": 3}");
        assertNotNull(result);
        assertTrue(result instanceof Map);
        Map<?, ?> map = (Map<?, ?>) result;
        assertEquals(3, map.size());
        assertEquals(1, map.get("a"));
        assertEquals(2, map.get("b"));
        assertEquals(3, map.get("c"));
    }

    // ========== 幂运算测试 ==========

    @Test
    public void op_power() throws Exception {
        Object result = eval("2 ** 3");
        assertNotNull(result);
        assertTrue(result instanceof Number);
        assertEquals(8, ((Number) result).intValue());
    }

    @Test
    public void op_powerInExpression() throws Exception {
        Object result = eval("1 + 2 ** 3");
        assertNotNull(result);
        assertTrue(result instanceof Number);
        assertEquals(9, ((Number) result).intValue());
    }

    @Test
    public void parse_fieldParsing() throws Exception {
        Object result = eval("java.lang.System.out");
        assertSame(System.out, result);
        Object result2 = eval("System.out");
        assertSame(System.out, result2);
    }

    @Test
    public void dcg_anonymousClassGeneration() throws Exception {
        Object result = eval("""
                obj := new Object() {
                    int x = 1;
                    public void setX(int x) { this.x = x; }
                    public int getX() { return this.x; }
                };
                obj.setX(2);
                obj.getX();
                """);
        assertEquals(2, result);
    }

    @Test
    public void method_staticMethodReference() throws Exception {
        Object result = eval("Comparator.comparing");
        assertNotNull("Comparator.comparing should return a MethodReference", result);
        assertTrue(" 应该是MethodReference类型", result instanceof MethodReference);
    }

    @Test
    public void method_staticMethodReferenceInvoke() throws Exception {
        Object result = eval("Integer.valueOf(42)");
        assertEquals(42, result);
    }

    @Test
    public void method_staticMethodReferenceOnClass() throws Exception {
        Object result = eval("Math.max(10, 20)");
        assertEquals(20, result);
    }

    @Test
    public void method_functionalInterfaceConversion() throws Exception {
        Object result = eval("""
                Arrays.stream(new int[]{1, 1, 4, 5, 1, 4})
                    .filter(i -> i > 3)
                    .sorted()
                    .toArray();
                """);
        int[] excepted = Arrays.stream(new int[]{1, 1, 4, 5, 1, 4})
                .filter(i -> i > 3)
                .sorted()
                .toArray();

        assertTrue(result.getClass().isArray());
        assertEquals(excepted.length, Array.getLength(result));
        for (int i = 0; i < excepted.length; i++) {
            assertEquals(excepted[i], Array.get(result, i));
        }
    }


}
