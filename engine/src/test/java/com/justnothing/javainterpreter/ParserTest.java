package com.justnothing.javainterpreter;

import com.justnothing.javainterpreter.api.DefaultOutputHandler;
import com.justnothing.javainterpreter.builtins.MethodReference;
import com.justnothing.javainterpreter.evaluator.MethodBodyExecutor;
import com.justnothing.javainterpreter.exception.EvaluationException;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.Map;


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
        MethodBodyExecutor.clearAll();
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
        assertArrayEquals(new Integer[] {1, 2}, (Integer[]) result);
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

    // ========== 阶段1: 基础语法糖 ==========

    @Test
    public void fstring_basicInterpolation() throws Exception {
        Object result = eval("""
            auto name = "Alice";
            auto age = 18;
            f"My name is ${name}, age ${age}"
            """);
        assertEquals("My name is Alice, age 18", result);
    }

    @Test
    public void fstring_singleVarNoBraces() throws Exception {
        Object result = eval("""
            auto name = "Bob";
            f"Hello, $name!"
            """);
        assertEquals("Hello, Bob!", result);
    }

    @Test
    public void range_intRange() throws Exception {
        Object result = eval("1..5");
        assertTrue(result.getClass().isArray());
        Integer[] arr = (Integer[]) result;
        assertArrayEquals(new Integer[] {1, 2, 3, 4, 5}, arr);
    }

    @Test
    public void range_charRange() throws Exception {
        Object result = eval("'a'..'e'");
        assertNotNull(result);
    }

    @Test
    public void shortDecl_basicInt() throws Exception {
        Object result = eval("""
            x := 10;
            x;
            """);
        assertEquals(10, ((Number) result).intValue());
    }

    @Test
    public void shortDecl_stringType() throws Exception {
        Object result = eval("""
            name := "hello";
            name;
            """);
        assertEquals("hello", result);
    }

    @Test
    public void setOp_union() throws Exception {
        Object result = eval("""
            auto a = [1, 2, 3];
            auto b = [3, 4, 5];
            a | b;
            """);
        assertNotNull(result);
        assertTrue(result.getClass().isArray());
    }

    @Test
    public void setOp_difference() throws Exception {
        Object result = eval("""
            auto a = [1, 2, 3];
            auto b = [3, 4, 5];
            a - b;
            """);
        assertNotNull(result);
    }

    @Test
    public void setOp_intersection() throws Exception {
        Object result = eval("""
            auto a = [1, 2, 3];
            auto b = [2, 3, 4];
            a & b;
            """);
        assertNotNull(result);
    }

    @Test
    public void setOp_symmetricDifference() throws Exception {
        Object result = eval("""
            auto a = [1, 2, 3];
            auto b = [2, 3, 4];
            a ^ b;
            """);
        assertNotNull(result);
    }

    // ========== 阶段2: 空值安全 ==========

    @Test
    public void safeCall_nullObject() throws Exception {
        Object result = eval("""
            auto obj = null;
            obj?.toString();
            """);
        assertNull(result);
    }

    @Test
    public void safeCall_nonNullObject() throws Exception {
        Object result = eval("""
            auto obj = "hello";
            obj?.length();
            """);
        assertEquals(5, ((Number) result).intValue());
    }

    @Test
    public void safeCall_chained() throws Exception {
        Object result = eval("""
            auto obj = null;
            obj?.toString()?.length();
            """);
        assertNull(result);
    }

    @Test
    public void safeFieldAccess_nullObject() throws Exception {
        Object result = eval("""
            auto obj = null;
            obj?.someField;
            """);
        assertNull(result);
    }

    @Test
    public void elvis_nullValue() throws Exception {
        Object result = eval("""
            auto value = null;
            value ?: "default";
            """);
        assertEquals("default", result);
    }

    @Test
    public void elvis_nonNullValue() throws Exception {
        Object result = eval("""
            auto value = "actual";
            value ?: "default";
            """);
        assertEquals("actual", result);
    }

    @Test(expected = EvaluationException.class)
    public void nonNullAssert_nullThrows() throws Exception {
        eval("auto x = null; !!x;");
    }

    @Test
    public void nonNullAssert_nonNullPasses() throws Exception {
        Object result = eval("""
            auto x = "not null";
            !!x;
            """);
        assertEquals("not null", result);
    }

    @Test
    public void conditionalAssign_nullToValue() throws Exception {
        Object result = eval("""
            Integer a = null;
            a ?= 42;
            a;
            """);
        assertEquals(42, ((Number) result).intValue());
    }

    @Test
    public void conditionalAssign_alreadyHasValue() throws Exception {
        Object result = eval("""
            Integer a = 10;
            a ?= 42;
            a;
            """);
        assertEquals(10, ((Number) result).intValue());
    }

    // ========== 阶段3: 函数式特性 ==========

    @Test
    public void lambda_directCall_twoArgs() throws Exception {
        Object result = eval("""
            auto add = (x, y) -> x + y;
            add(3, 4);
            """);
        assertEquals(7, ((Number) result).intValue());
    }

    @Test
    public void lambda_blockBody() throws Exception {
        Object result = eval("""
            auto compute = (x) -> { return x * x + 1; };
            compute(5);
            """);
        assertEquals(26, ((Number) result).intValue());
    }

    @Test
    public void lambda_asRunnable() throws Exception {
        Object result = eval("""
            auto captured = 0;
            auto r = asRunnable(() -> { captured = captured + 1; });
            r.run();
            r.run();
            r.run();
            captured;
            """);
        assertEquals(3, ((Number) result).intValue());
    }

    @Test
    public void methodReference_staticMethod() throws Exception {
        Object result = eval("""
            auto parseInt = Integer::parseInt;
            parseInt("12345");
            """);
        assertEquals(12345, ((Number) result).intValue());
    }

    @Test
    public void methodReference_instanceMethod() throws Exception {
        Object result = eval("""
            auto strLen = "hello world"::length;
            strLen();
            """);
        assertEquals(11, ((Number) result).intValue());
    }

    @Test
    public void pipeline_basicTransform() throws Exception {
        Object result = eval("""
            "  hello world  "
                |> String::trim
                |> String::toUpperCase;
            """);
        assertEquals("HELLO WORLD", result);
    }

    @Test
    public void pipeline_withLambda() throws Exception {
        Object result = eval("""
            [1, 2, 3, 4, 5]
                |> filter((x) -> x > 2)
                |> map((x) -> x * 10);
            """);
        assertNotNull(result);
        assertTrue(result.getClass().isArray());
    }

    @Test
    public void pipeline_composedFunctions() throws Exception {
        Object result = eval("""
            auto twice = x -> x * 2;
            auto addOne = x -> x + 1;
            5 |> twice |> addOne;
            """);
        assertEquals(11, ((Number) result).intValue());
    }

    @Test
    public void lambda_inStream_filterMapReduce() throws Exception {
        Object result = eval("""
            Arrays.stream(new int[]{1, 2, 3, 4, 5})
                .filter(i -> i % 2 == 0)
                .map(i -> i * i)
                .sum();
            """);
        assertEquals(20.0, ((Number) result).doubleValue(), 0.001);
    }

    // ========== 阶段4: 异步与并发 ==========

    @Test
    public void async_basic() throws Exception {
        Object result = eval("""
            auto future = async {
                sleep(10);
                return "async_result";
            };
            await future;
            """);
        assertEquals("async_result", result);
    }

    @Test
    public void async_withValue() throws Exception {
        Object result = eval("""
            auto future = async { return 42 * 2; };
            await future;
            """);
        assertEquals(84, ((Number) result).intValue());
    }

    @Test
    public void runLater_basic() throws Exception {
        Object result = eval("""
            auto shared = 100;
            runLater(() -> { shared = shared + 1; });
            runLater(() -> { shared = shared + 10; });
            sleep(200);
            shared;
            """);
        assertEquals(111, ((Number) result).intValue());
    }

    @Test
    public void asFunction_basic() throws Exception {
        Object result = eval("""
            auto fn = asFunction((x) -> x.toUpperCase());
            fn("hello");
            """);
        assertEquals("HELLO", result);
    }

    // ========== 阶段5: 动态类系统 ==========

    @Test
    public void dynamicClass_basicFieldsAndConstructor() throws Exception {
        Object result = eval("""
            class Point {
                int x, y;
                Point(int x, int y) { this.x = x; this.y = y; }
                int sum() { return x + y; }
            }
            auto p = new Point(3, 4);
            p.sum();
            """);
        assertEquals(7, ((Number) result).intValue());
    }

    @Test
    public void dynamicClass_fieldAccess() throws Exception {
        Object result = eval("""
            class Box {
                String value;
                Box(String v) { value = v; }
            }
            Box b = new Box("hello");
            b.value;
            """);
        assertEquals("hello", result);
    }

    @Test
    public void dynamicClass_fieldMutation() throws Exception {
        Object result = eval("""
            class Counter {
                int count;
                Counter() { count = 0; }
                void increment() { count = count + 1; }
            }
            auto c = new Counter();
            c.increment();
            c.increment();
            c.increment();
            c.count;
            """);
        assertEquals(3, ((Number) result).intValue());
    }

    @Test
    public void dynamicClass_newSyntax() throws Exception {
        Object result = eval("""
            class Person {
                String name;
                int age;
                Person(String n, int a) { name = n; age = a; }
                String greet() { return "Hi, I'm " + name + ", " + age; }
            }
            auto p = new Person("Alice", 30);
            p.greet();
            """);
        assertEquals("Hi, I'm Alice, 30", result);
    }

    @Test
    public void anonymousClass_basic() throws Exception {
        Object result = eval("""
            obj := new Object() {
                int x = 114;
                public int getX() { return x; }
                public void setX(int val) { x = val; }
            };
            obj.setX(514);
            obj.getX();
            """);
        assertEquals(514, ((Number) result).intValue());
    }

    @Test
    public void anonymousClass_multipleMethods() throws Exception {
        Object result = eval("""
            calc := new Object() {
                public int add(int a, int b) { return a + b; }
                public int mul(int a, int b) { return a * b; }
            };
            calc.add(3, 4) + calc.mul(5, 6);
            """);
        assertEquals(37, ((Number) result).intValue());
    }

    // ========== 阶段7: 特殊运算符 ==========

    @Test
    public void powerOp_basic() throws Exception {
        Object result = eval("2 ** 10;");
        assertEquals(1024, ((Number) result).intValue());
    }

    @Test
    public void powerOp_fractional() throws Exception {
        Object result = eval("9 ** 0.5;");
        assertEquals(3.0, ((Number) result).doubleValue(), 0.001);
    }

    @Test
    public void spaceshipOp_lessThan() throws Exception {
        Object result = eval("1 <=> 5;");
        assertEquals(-1, ((Number) result).intValue());
    }

    @Test
    public void spaceshipOp_greaterThan() throws Exception {
        Object result = eval("10 <=> 3;");
        assertEquals(1, ((Number) result).intValue());
    }

    @Test
    public void spaceshipOp_equal() throws Exception {
        Object result = eval("7 <=> 7;");
        assertEquals(0, ((Number) result).intValue());
    }

    @Test(expected = EvaluationException.class)
    public void deleteVar_single() throws Exception {
        Object result = eval("""
            auto temp = 42;
            delete temp;
            temp;
            """);
        assertNull(result);
    }

    @Test
    public void newArray_basic() throws Exception {
        Object result = eval("new int[5];");
        assertNotNull(result);
        assertTrue(result.getClass().isArray());
        assertEquals(5, Array.getLength(result));
    }

    @Test
    public void newArray_withInit() throws Exception {
        Object result = eval("new int[]{10, 20, 30};");
        assertNotNull(result);
        assertTrue(result.getClass().isArray());
        assertEquals(3, Array.getLength(result));
        assertEquals(10, Array.get(result, 0));
        assertEquals(20, Array.get(result, 1));
        assertEquals(30, Array.get(result, 2));
    }

    @Test
    public void arrayAccess_readWrite() throws Exception {
        Object result = eval("""
            auto arr = [1, 2, 3];
            arr[1] = 99;
            arr[1];
            """);
        assertEquals(99, ((Number) result).intValue());
    }

    // ==================== 阶段6: 高级控制流 ====================

    @Test
    public void switchExpr_arrowSyntax() throws Exception {
        Object result = eval("""
            auto x = 2;
            auto result = switch (x) {
                case 1 -> "one";
                case 2 -> "two";
                default -> "other";
            };
            result;
            """);
        assertEquals("two", result);
    }

    @Test
    public void switchExpr_defaultBranch() throws Exception {
        Object result = eval("""
            auto x = 99;
            auto result = switch (x) {
                case 1 -> "one";
                case 2 -> "two";
                default -> "unknown";
            };
            result;
            """);
        assertEquals("unknown", result);
    }

    @Test
    public void switchStmt_colonSyntax() throws Exception {
        Object result = eval("""
            auto x = 1;
            String result = "";
            switch (x) {
                case 1:
                    result = "one";
                    break;
                case 2:
                    result = "two";
                    break;
            }
            result;
            """);
        assertEquals("one", result);
    }

    @Test
    public void switchStmt_colonSyntax_withDefault() throws Exception {
        Object result = eval("""
            auto x = 3;
            String result = "";
            switch (x) {
                case 1:
                    result = "one";
                    break;
                case 2:
                    result = "two";
                    break;
                default:
                    result = "other";
            }
            result;
            """);
        assertEquals("other", result);
    }

    @Test
    public void switchExpr_stringMatching() throws Exception {
        Object result = eval("""
            auto color = "red";
            auto result = switch (color) {
                case "red" -> "#FF0000";
                case "green" -> "#00FF00";
                case "blue" -> "#0000FF";
                default -> "#000000";
            };
            result;
            """);
        assertEquals("#FF0000", result);
    }

    @Test
    public void labeledBreak_basic() throws Exception {
        Object result = eval("""
            auto sum = 0;
            outer:
            for (int i = 0; i < 5; i++) {
                for (int j = 0; j < 5; j++) {
                    sum = sum + 1;
                    if (j == 2) break outer;
                }
            }
            sum;
            """);
        assertEquals(3, ((Number) result).intValue());
    }

    @Test
    public void forEach_list() throws Exception {
        Object result = eval("""
            auto items = [10, 20, 30];
            auto sum = 0;
            for (auto x : items) {
                sum = sum + x;
            }
            sum;
            """);
        assertEquals(60, ((Number) result).intValue());
    }

    @Test
    public void forEach_array() throws Exception {
        Object result = eval("""
            auto arr = new int[]{1, 2, 3, 4, 5};
            auto product = 1;
            for (auto x : arr) {
                product = product * x;
            }
            product;
            """);
        assertEquals(120, ((Number) result).intValue());
    }

    @Test
    public void forEach_withBreak() throws Exception {
        Object result = eval("""
            auto items = [10, 20, 30, 40, 50];
            auto sum = 0;
            for (auto x : items) {
                if (x > 25) break;
                sum = sum + x;
            }
            sum;
            """);
        assertEquals(30, ((Number) result).intValue());
    }

    @Test
    public void forEach_range() throws Exception {
        Object result = eval("""
            auto sum = 0;
            for (auto i : 1..5) {
                sum = sum + i;
            }
            sum;
            """);
        assertEquals(15, ((Number) result).intValue());
    }

    // ==================== 阶段8: 反射与互操作 ====================

    @Test
    public void reflect_getField() throws Exception {
        Object result = eval("""
            class Box {
                int value = 42;
            }
            auto b = new Box();
            getField(b, "value");
            """);
        assertEquals(42, ((Number) result).intValue());
    }

    @Test
    public void reflect_setField() throws Exception {
        Object result = eval("""
            class Box {
                int value = 42;
            }
            auto b = new Box();
            setField(b, "value", 99);
            getField(b, "value");
            """);
        assertEquals(99, ((Number) result).intValue());
    }

    @Test
    public void reflect_invokeMethod() throws Exception {
        Object result = eval("""
            class Calculator {
                int add(int a, int b) { return a + b; }
            }
            auto calc = new Calculator();
            invokeMethod(calc, "add", 3, 4);
            """);
        assertEquals(7, ((Number) result).intValue());
    }

    @Test
    public void reflect_typeOf() throws Exception {
        Object result = eval("""
            class MyClass {}
            auto obj = new MyClass();
            typeOf(obj);
            """);
        assertTrue(result.toString().contains("MyClass"));
    }

    @Test
    public void reflect_isInstanceOf() throws Exception {
        Object result = eval("""
            auto list = new ArrayList();
            isInstanceOf(list, "java.util.ArrayList");
            """);
        assertEquals(true, result);
    }

    @Test
    public void reflect_isInstanceOf_notMatch() throws Exception {
        Object result = eval("""
            auto str = "hello";
            isInstanceOf(str, "java.util.ArrayList");
            """);
        assertEquals(false, result);
    }

    @Test
    public void reflect_cast_success() throws Exception {
        Object result = eval("""
            Object obj = "hello world";
            String s = cast(obj, "java.lang.String");
            s.length();
            """);
        assertEquals(11, ((Number) result).intValue());
    }

    @Test
    public void reflect_dynamicClassWithReflection() throws Exception {
        Object result = eval("""
            class Person {
                String name;
                int age;
                Person(String name, int age) { this.name = name; this.age = age; }
                String greet() { return "Hi, I'm " + name + ", " + age; }
            }
            auto p = new Person("Alice", 30);
            auto greeting = invokeMethod(p, "greet");
            setField(p, "age", 31);
            auto newName = getField(p, "name");
            new Object[] {greeting, newName, getField(p, "age")};
            """);
        Object[] arr = (Object[]) result;
        assertEquals("Hi, I'm Alice, 30", arr[0]);
        assertEquals("Alice", arr[1]);
        assertEquals(31, ((Number) arr[2]).intValue());
    }

}
