package com.justnothing.testmodule.command.functions.script_new;

import com.justnothing.testmodule.command.functions.script_new.ast.nodes.BlockNode;
import com.justnothing.testmodule.command.functions.script_new.evaluator.ASTEvaluator;
import com.justnothing.testmodule.command.functions.script_new.evaluator.ExecutionContext;
import com.justnothing.testmodule.command.functions.script_new.lexer.Lexer;
import com.justnothing.testmodule.command.functions.script_new.parser.Parser;

public class QuickTest {
    
    public static void main(String[] args) {
        System.out.println("=== Quick Tests ===\n");
        
        testAutoTypeInference();
        testLambdaAndMethodRef();
        testArrayConcat();
        testSimpleQuickSort();
        testFibonacci();
        testClosure();
        testHigherOrderFunction();
        testFilter();
        testReduce();
        testFactorial();
        testCompose();
        testPipeline();
        testForEach();
        testMagicOperators();
    }
    
    private static void testAutoTypeInference() {
        System.out.println("Testing Auto Type Inference...");
        
        try {
            String source = """
                auto a = 1;
                auto b = 2L;
                auto c = 3.14;
                auto d = true;
                auto e = "hello";
                
                println("a = " + a + " (type: int)");
                println("b = " + b + " (type: long)");
                println("c = " + c + " (type: double)");
                println("d = " + d + " (type: boolean)");
                println("e = " + e + " (type: String)");
                
                auto sum = a + 10;
                println("a + 10 = " + sum);
                """;
            
            Lexer lexer = new Lexer(source);
            Parser parser = new Parser(lexer.tokenize());
            BlockNode block = parser.parse();
            
            ExecutionContext context = new ExecutionContext(QuickTest.class.getClassLoader());
            ASTEvaluator.evaluate(block, context);
            
            System.out.println("✓ Auto Type Inference test passed\n");
        } catch (Exception e) {
            System.err.println("✗ Auto Type Inference test failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private static void testLambdaAndMethodRef() {
        System.out.println("Testing Lambda and Method Reference...");
        
        try {
            String source = """
                import java.util.function.Function;
                import java.util.function.Supplier;
                import java.lang.reflect.Method;
                
                auto f = () -> 42;
                println(f());
                
                auto add = (x, y) -> x + y;
                println(add(3, 4));
                
                Function g = (x) -> x * 2;
                println(g(5));
                
                Function<Integer, Integer> h = (x) -> x + 1;
                println(h(10));
                
                Supplier time = System::currentTimeMillis;
                println("Current time: " + time());
                
                auto stringClass = String.class;
                println("String.class = " + stringClass);
                
                Function<Method, String> getName = Method::getName;
                auto lengthMethod = stringClass.getMethod("length");
                println("Method::getName test: " + getName(lengthMethod));
                """;
            
            Lexer lexer = new Lexer(source);
            Parser parser = new Parser(lexer.tokenize());
            BlockNode block = parser.parse();
            
            System.out.println("AST: " + block.formatString(0));
            
            ExecutionContext context = new ExecutionContext(QuickTest.class.getClassLoader());
            ASTEvaluator.evaluate(block, context);
            
            System.out.println("✓ Lambda and Method Reference test passed\n");
        } catch (Exception e) {
            System.err.println("✗ Lambda and Method Reference test failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private static void testArrayConcat() {
        System.out.println("Testing Array Concat...");
        
        try {
            String source = """
                auto a = [1, 2];
                auto b = [3, 4];
                auto c = a + b;
                println("a + b = " + c);
                """;
            
            Lexer lexer = new Lexer(source);
            Parser parser = new Parser(lexer.tokenize());
            BlockNode block = parser.parse();
            
            ExecutionContext context = new ExecutionContext(QuickTest.class.getClassLoader());
            ASTEvaluator.evaluate(block, context);
            
            System.out.println("✓ Array concat test passed\n");
        } catch (Exception e) {
            System.err.println("✗ Array concat test failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private static void testSimpleQuickSort() {
        System.out.println("Testing Simple QuickSort...");
        
        try {
            String source = """
                auto quicksort = arr -> {
                    if (size(arr) <= 1) {
                        return arr;
                    }
                    auto pivot = arr[0];
                    auto left = [];
                    auto right = [];
                    for (int i = 1; i < size(arr); i = i + 1) {
                        if (arr[i] < pivot) {
                            left = left + [arr[i]];
                        } else {
                            right = right + [arr[i]];
                        }
                    }
                    return quicksort(left) + [pivot] + quicksort(right);
                };
                
                auto arr = [3, 1, 4, 1, 5, 9, 2, 6];
                auto sorted = quicksort(arr);
                println("Sorted: " + sorted);
                """;
            
            Lexer lexer = new Lexer(source);
            Parser parser = new Parser(lexer.tokenize());
            BlockNode block = parser.parse();
            
            ExecutionContext context = new ExecutionContext(QuickTest.class.getClassLoader());
            ASTEvaluator.evaluate(block, context);
            
            System.out.println("✓ QuickSort test passed\n");
        } catch (Exception e) {
            System.err.println("✗ QuickSort test failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private static void testFibonacci() {
        System.out.println("Testing Fibonacci...");
        
        try {
            String source = """
                auto fib = n -> {
                    if (n <= 1) {
                        return n;
                    }
                    return fib(n - 1) + fib(n - 2);
                };
                
                println("fib(10) = " + fib(10));
                """;
            
            Lexer lexer = new Lexer(source);
            Parser parser = new Parser(lexer.tokenize());
            BlockNode block = parser.parse();
            
            ExecutionContext context = new ExecutionContext(QuickTest.class.getClassLoader());
            ASTEvaluator.evaluate(block, context);
            
            System.out.println("✓ Fibonacci test passed\n");
        } catch (Exception e) {
            System.err.println("✗ Fibonacci test failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private static void testClosure() {
        System.out.println("Testing Closure...");
        
        try {
            String source = """
                auto makeCounter = start -> {
                    auto count = start;
                    return () -> {
                        count = count + 1;
                        return count;
                    };
                };
                
                auto counter = makeCounter(0);
                println("counter() = " + counter());
                println("counter() = " + counter());
                println("counter() = " + counter());
                """;
            
            Lexer lexer = new Lexer(source);
            Parser parser = new Parser(lexer.tokenize());
            BlockNode block = parser.parse();
            
            ExecutionContext context = new ExecutionContext(QuickTest.class.getClassLoader());
            ASTEvaluator.evaluate(block, context);
            
            System.out.println("✓ Closure test passed\n");
        } catch (Exception e) {
            System.err.println("✗ Closure test failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private static void testHigherOrderFunction() {
        System.out.println("Testing Higher-Order Function...");
        
        try {
            String source = """
                auto map = (arr, f) -> {
                    auto result = [];
                    for (int i = 0; i < size(arr); i = i + 1) {
                        result = result + [f(arr[i])];
                    }
                    return result;
                };
                
                auto doubleIt = x -> x * 2;
                auto nums = [1, 2, 3, 4, 5];
                auto doubled = map(nums, doubleIt);
                println("map([1,2,3,4,5], doubleIt) = " + doubled);
                """;
            
            Lexer lexer = new Lexer(source);
            Parser parser = new Parser(lexer.tokenize());
            BlockNode block = parser.parse();
            
            ExecutionContext context = new ExecutionContext(QuickTest.class.getClassLoader());
            ASTEvaluator.evaluate(block, context);
            
            System.out.println("✓ Higher-Order Function test passed\n");
        } catch (Exception e) {
            System.err.println("✗ Higher-Order Function test failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private static void testFilter() {
        System.out.println("Testing Filter...");
        
        try {
            String source = """
                auto filter = (arr, pred) -> {
                    auto result = [];
                    for (int i = 0; i < size(arr); i = i + 1) {
                        if (pred(arr[i])) {
                            result = result + [arr[i]];
                        }
                    }
                    return result;
                };
                
                auto isEven = x -> x % 2 == 0;
                auto nums = [1, 2, 3, 4, 5, 6, 7, 8, 9, 10];
                auto evens = filter(nums, isEven);
                println("filter([1..10], isEven) = " + evens);
                """;
            
            Lexer lexer = new Lexer(source);
            Parser parser = new Parser(lexer.tokenize());
            BlockNode block = parser.parse();
            
            ExecutionContext context = new ExecutionContext(QuickTest.class.getClassLoader());
            ASTEvaluator.evaluate(block, context);
            
            System.out.println("✓ Filter test passed\n");
        } catch (Exception e) {
            System.err.println("✗ Filter test failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private static void testReduce() {
        System.out.println("Testing Reduce...");
        
        try {
            String source = """
                auto reduce = (arr, f, init) -> {
                    auto acc = init;
                    for (int i = 0; i < size(arr); i = i + 1) {
                        acc = f(acc, arr[i]);
                    }
                    return acc;
                };
                
                auto sum = (a, b) -> a + b;
                auto nums = [1, 2, 3, 4, 5];
                auto total = reduce(nums, sum, 0);
                println("reduce([1,2,3,4,5], sum, 0) = " + total);
                """;
            
            Lexer lexer = new Lexer(source);
            Parser parser = new Parser(lexer.tokenize());
            BlockNode block = parser.parse();
            
            ExecutionContext context = new ExecutionContext(QuickTest.class.getClassLoader());
            ASTEvaluator.evaluate(block, context);
            
            System.out.println("✓ Reduce test passed\n");
        } catch (Exception e) {
            System.err.println("✗ Reduce test failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private static void testFactorial() {
        System.out.println("Testing Factorial...");
        
        try {
            String source = """
                auto factorial = n -> {
                    if (n <= 1) {
                        return 1;
                    }
                    return n * factorial(n - 1);
                };
                
                println("factorial(5) = " + factorial(5));
                println("factorial(10) = " + factorial(10));
                """;
            
            Lexer lexer = new Lexer(source);
            Parser parser = new Parser(lexer.tokenize());
            BlockNode block = parser.parse();
            
            ExecutionContext context = new ExecutionContext(QuickTest.class.getClassLoader());
            ASTEvaluator.evaluate(block, context);
            
            System.out.println("✓ Factorial test passed\n");
        } catch (Exception e) {
            System.err.println("✗ Factorial test failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private static void testCompose() {
        System.out.println("Testing Function Composition...");
        
        try {
            String source = """
                auto compose = (f, g) -> {
                    return x -> f(g(x));
                };
                
                auto addOne = x -> x + 1;
                auto multiplyTwo = x -> x * 2;
                auto addOneThenMultiply = compose(multiplyTwo, addOne);
                
                println("compose(*2, +1)(5) = " + addOneThenMultiply(5));
                println("compose(*2, +1)(10) = " + addOneThenMultiply(10));
                """;
            
            Lexer lexer = new Lexer(source);
            Parser parser = new Parser(lexer.tokenize());
            BlockNode block = parser.parse();
            
            ExecutionContext context = new ExecutionContext(QuickTest.class.getClassLoader());
            ASTEvaluator.evaluate(block, context);
            
            System.out.println("✓ Function Composition test passed\n");
        } catch (Exception e) {
            System.err.println("✗ Function Composition test failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private static void testPipeline() {
        System.out.println("Testing Pipeline...");
        
        try {
            String source = """
                auto map = (arr, f) -> {
                    auto result = [];
                    for (int i = 0; i < size(arr); i = i + 1) {
                        result = result + [f(arr[i])];
                    }
                    return result;
                };
                
                auto filter = (arr, pred) -> {
                    auto result = [];
                    for (int i = 0; i < size(arr); i = i + 1) {
                        if (pred(arr[i])) {
                            result = result + [arr[i]];
                        }
                    }
                    return result;
                };
                
                auto reduce = (arr, f, init) -> {
                    auto acc = init;
                    for (int i = 0; i < size(arr); i = i + 1) {
                        acc = f(acc, arr[i]);
                    }
                    return acc;
                };
                
                auto nums = [1, 2, 3, 4, 5, 6, 7, 8, 9, 10];
                auto isEven = x -> x % 2 == 0;
                auto doubleIt = x -> x * 2;
                auto sum = (a, b) -> a + b;
                
                auto evens = filter(nums, isEven);
                auto doubled = map(evens, doubleIt);
                auto total = reduce(doubled, sum, 0);
                
                println("Pipeline: sum(map(filter([1..10], isEven), *2)) = " + total);
                """;
            
            Lexer lexer = new Lexer(source);
            Parser parser = new Parser(lexer.tokenize());
            BlockNode block = parser.parse();
            
            ExecutionContext context = new ExecutionContext(QuickTest.class.getClassLoader());
            ASTEvaluator.evaluate(block, context);
            
            System.out.println("✓ Pipeline test passed\n");
        } catch (Exception e) {
            System.err.println("✗ Pipeline test failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private static void testForEach() {
        System.out.println("Testing For-Each...");
        
        try {
            String source = """
                auto arr = {1, 2, 3, 4, 5};
                auto sum = 0;
                for (auto item : arr) {
                    sum = sum + item;
                }
                println("sum of [1,2,3,4,5] = " + sum);
                
                auto names = {"Alice", "Bob", "Charlie"};
                for (auto name : names) {
                    println("Hello, " + name + "!");
                }
                """;
            
            Lexer lexer = new Lexer(source);
            Parser parser = new Parser(lexer.tokenize());
            BlockNode block = parser.parse();
            
            ExecutionContext context = new ExecutionContext(QuickTest.class.getClassLoader());
            ASTEvaluator.evaluate(block, context);
            
            System.out.println("✓ For-Each test passed\n");
        } catch (Exception e) {
            System.err.println("✗ For-Each test failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private static void testMagicOperators() {
        System.out.println("Testing Magic Operators...");
        
        try {
            String source = """
                auto p = 2 ** 10;
                auto d = 7 // 3;
                auto m = -7 %% 3;
                println(p);
                println(d);
                println(m);
                
                auto a = null;
                auto b = "hello";
                auto c = a ?? b;
                println(c);
                
                auto x = null;
                auto y = "default";
                auto z = x ?: y;
                println(z);
                
                auto arr1 = {1, 2, 3, 4};
                auto arr2 = {3, 4, 5, 6};
                auto diff = arr1 - arr2;
                auto inter = arr1 & arr2;
                auto union = arr1 | arr2;
                auto symm = arr1 ^ arr2;
                println(diff);
                println(inter);
                println(union);
                println(symm);
                
                auto arr = {1, 2, 3, 4, 5};
                auto rev = ~arr;
                println(rev);
                """;
            
            Lexer lexer = new Lexer(source);
            Parser parser = new Parser(lexer.tokenize());
            BlockNode block = parser.parse();
            
            ExecutionContext context = new ExecutionContext(QuickTest.class.getClassLoader());
            ASTEvaluator.evaluate(block, context);
            
            System.out.println("✓ Magic Operators test passed\n");
        } catch (Exception e) {
            System.err.println("✗ Magic Operators test failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
