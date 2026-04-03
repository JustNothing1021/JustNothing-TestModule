package com.justnothing.testmodule.command.functions.script.engine_new;

import com.justnothing.testmodule.command.functions.script.engine_new.ast.nodes.BlockNode;
import com.justnothing.testmodule.command.functions.script.engine_new.evaluator.ASTEvaluator;
import com.justnothing.testmodule.command.functions.script.engine_new.evaluator.ExecutionContext;
import com.justnothing.testmodule.command.functions.script.engine_new.lexer.Lexer;
import com.justnothing.testmodule.command.functions.script.engine_new.parser.Parser;

public class QuickTest {
    
    public static void main(String[] args) {
        System.out.println("=== Complex Script Test ===\n");
        testComplexProgram();
    }
    
    private static void testComplexProgram() {
        System.out.println("Testing Complex Program...");
        System.out.println("=".repeat(60));
        
        try {
            String source = """
                import java.util.ArrayList;
                import java.util.HashMap;
                import java.util.HashSet;
                import java.util.LinkedList;
                import java.util.TreeMap;
                
                println("=== 1. Basic .new() Syntax ===");
                
                auto list = ArrayList.new();
                list.add(1);
                list.add(2);
                list.add(3);
                println("ArrayList.new() = " + list);
                
                auto map = HashMap.new();
                map.put("name", "Alice");
                map.put("age", 25);
                println("HashMap.new() = " + map);
                
                auto set = HashSet.new();
                set.add("a");
                set.add("b");
                set.add("a");
                println("HashSet.new() = " + set);
                
                auto linkedList = LinkedList.new();
                linkedList.add("first");
                linkedList.add("second");
                linkedList.addFirst("zero");
                println("LinkedList.new() = " + linkedList);
                
                auto treeMap = TreeMap.new();
                treeMap.put(3, "three");
                treeMap.put(1, "one");
                treeMap.put(2, "two");
                println("TreeMap.new() (sorted) = " + treeMap);
                
                println("");
                println("=== 2. .new() in Lambda ===");
                
                auto createList = () -> ArrayList.new();
                auto newList = createList();
                newList.add(42);
                println("Lambda creating ArrayList.new() = " + newList);
                
                println("");
                println("=== 3. .new() with Pipeline ===");
                
                auto result = HashMap.new() |> (m) -> { m.put("x", 10); m.put("y", 20); m; };
                println("HashMap.new() |> pipeline = " + result);
                
                println("");
                println("=== 4. Complex Object Builder Pattern ===");
                
                auto createPerson = (name, age) -> {
                    auto person = HashMap.new();
                    person.put("name", name);
                    person.put("age", age);
                    person.put("greet", () -> "Hello, I'm " + name + "!");
                    person;
                };
                
                auto alice = createPerson("Alice", 25);
                println("Person: " + alice.get("name") + ", age " + alice.get("age"));
                println("Greeting: " + alice.get("greet").invoke());
                
                println("");
                println("=== 5. Factory Pattern with .new() ===");
                
                auto createFactory = () -> {
                    auto registry = HashMap.new();
                    auto factory = HashMap.new();
                    
                    factory.put("register", (name, creator) -> registry.put(name, creator));
                    factory.put("create", (name, args) -> {
                        auto creator = registry.get(name);
                        if (creator != null) creator.invoke(args);
                    });
                    factory.put("list", () -> registry.keySet());
                    factory;
                };
                
                auto factory = createFactory();
                factory.get("register").invoke("list", (items) -> {
                    auto l = ArrayList.new();
                    for (auto item : items) { l.add(item); }
                    l;
                });
                
                println("Factory registered types: " + factory.get("list").invoke());
                auto createdList = factory.get("create").invoke("list", [1, 2, 3, 4, 5]);
                println("Factory created list: " + createdList);
                
                println("");
                println("=== 6. Event System with .new() ===");
                
                auto createEventEmitter = () -> {
                    auto listeners = HashMap.new();
                    auto emitter = HashMap.new();
                    
                    emitter.put("on", (event, callback) -> {
                        auto list = listeners.get(event);
                        if (list == null) {
                            list = ArrayList.new();
                            listeners.put(event, list);
                        }
                        list.add(callback);
                    });
                    emitter.put("emit", (event, data) -> {
                        auto list = listeners.get(event);
                        if (list != null) {
                            auto i = 0;
                            while (i < list.size()) {
                                list.get(i).invoke(data);
                                i = i + 1;
                            }
                        }
                    });
                    emitter;
                };
                
                auto emitter = createEventEmitter();
                emitter.get("on").invoke("message", (data) -> println("Received: " + data));
                emitter.get("on").invoke("message", (data) -> println("Echo: " + data));
                emitter.get("emit").invoke("message", "Hello World!");
                
                println("");
                println("=== 7. Async Operations with .new() ===");
                
                auto asyncTask = async {
                    auto data = ArrayList.new();
                    data.add("async item 1");
                    Thread.sleep(50);
                    data.add("async item 2");
                    data;
                };
                auto asyncResult = await asyncTask;
                println("Async ArrayList: " + asyncResult);
                
                println("");
                println("=== 8. Recursive Data Structures ===");
                
                auto createTree = (value) -> {
                    auto node = HashMap.new();
                    node.put("value", value);
                    node.put("left", null);
                    node.put("right", null);
                    node.put("addLeft", (v) -> node.put("left", createTree(v)));
                    node.put("addRight", (v) -> node.put("right", createTree(v)));
                    node.put("traverse", () -> {
                        auto result = ArrayList.new();
                        auto left = node.get("left");
                        auto right = node.get("right");
                        if (left != null) result.addAll(left.get("traverse").invoke());
                        result.add(node.get("value"));
                        if (right != null) result.addAll(right.get("traverse").invoke());
                        result;
                    });
                    node;
                };
                
                auto root = createTree(5);
                root.get("addLeft").invoke(3);
                root.get("addRight").invoke(7);
                root.get("left").get("addLeft").invoke(1);
                root.get("left").get("addRight").invoke(4);
                root.get("right").get("addRight").invoke(9);
                
                println("Tree inorder traversal: " + root.get("traverse").invoke());
                
                println("");
                println("=== 9. Memoization with .new() ===");
                
                auto memoize = (fn) -> {
                    auto cache = HashMap.new();
                    (arg) -> {
                        auto cached = cache.get(arg);
                        if (cached != null) {
                            cached;
                        } else {
                            auto result = fn.invoke(arg);
                            cache.put(arg, result);
                            result;
                        }
                    };
                };
                
                auto fib = (n) -> {
                    if (n <= 1) {
                        n;
                    } else {
                        fib(n - 1) + fib(n - 2);
                    }
                };
                
                auto memoFib = memoize(fib);
                println("fib(15) = " + memoFib.invoke(15));
                
                println("");
                println("=== 10. Chain of Responsibility ===");
                
                auto createHandler = (name, action) -> {
                    auto handler = HashMap.new();
                    handler.put("name", name);
                    handler.put("next", null);
                    handler.put("setNext", (n) -> handler.put("next", n));
                    handler.put("handle", (request) -> {
                        println("  " + name + " handling: " + request);
                        auto result = action.invoke(request);
                        auto next = handler.get("next");
                        if (next != null && result != null) next.get("handle").invoke(result);
                    });
                    handler;
                };
                
                auto h1 = createHandler("Validator", (req) -> {
                    if (req != null && req != "") {
                        req;
                    } else {
                        null;
                    }
                });
                auto h2 = createHandler("Processor", (req) -> req + " [processed]");
                auto h3 = createHandler("Logger", (req) -> {
                    println("  Final result: " + req);
                    req;
                });
                
                h1.get("setNext").invoke(h2);
                h2.get("setNext").invoke(h3);
                
                println("Chain of responsibility:");
                h1.get("handle").invoke("test request");
                
                println("");
                println("=".repeat(60));
                println("All complex tests completed successfully!");
                """;
            
            Lexer lexer = new Lexer(source);
            Parser parser = new Parser(lexer.tokenize());
            BlockNode block = parser.parse();
            
            ExecutionContext context = new ExecutionContext(QuickTest.class.getClassLoader());
            ASTEvaluator.evaluate(block, context);
            
            System.out.println("\n✓ Complex Program test passed\n");
        } catch (Exception e) {
            System.err.println("✗ Complex Program test failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
