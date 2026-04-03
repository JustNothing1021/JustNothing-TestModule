package com.justnothing.testmodule.command.functions.script.engine_new;

import com.justnothing.testmodule.command.functions.script.engine_new.ast.GenericType;
import com.justnothing.testmodule.command.functions.script.engine_new.ast.SourceLocation;
import com.justnothing.testmodule.command.functions.script.engine_new.ast.nodes.AssignmentNode;
import com.justnothing.testmodule.command.functions.script.engine_new.ast.nodes.BinaryOpNode;
import com.justnothing.testmodule.command.functions.script.engine_new.ast.nodes.BlockNode;
import com.justnothing.testmodule.command.functions.script.engine_new.ast.nodes.ClassReferenceNode;
import com.justnothing.testmodule.command.functions.script.engine_new.ast.nodes.ForNode;
import com.justnothing.testmodule.command.functions.script.engine_new.ast.nodes.IfNode;
import com.justnothing.testmodule.command.functions.script.engine_new.ast.nodes.LambdaNode;
import com.justnothing.testmodule.command.functions.script.engine_new.ast.nodes.LiteralNode;
import com.justnothing.testmodule.command.functions.script.engine_new.ast.nodes.MapLiteralNode;
import com.justnothing.testmodule.command.functions.script.engine_new.ast.nodes.MethodCallNode;
import com.justnothing.testmodule.command.functions.script.engine_new.ast.nodes.TryNode;
import com.justnothing.testmodule.command.functions.script.engine_new.ast.nodes.UnaryOpNode;
import com.justnothing.testmodule.command.functions.script.engine_new.ast.nodes.VariableNode;
import com.justnothing.testmodule.command.functions.script.engine_new.evaluator.ScopeManager;
import com.justnothing.testmodule.command.functions.script.engine_new.evaluator.ExecutionContext;
import com.justnothing.testmodule.command.functions.script.engine_new.evaluator.ASTEvaluator;
import com.justnothing.testmodule.command.functions.script.engine_new.evaluator.Lambda;
import com.justnothing.testmodule.command.functions.script.engine_new.parser.Parser;
import com.justnothing.testmodule.command.functions.script.engine_new.lexer.Lexer;

import java.util.Arrays;
import java.util.Map;

public class ScriptNewTestRunner {
    
    public static void main(String[] args) {
        System.out.println("=== Testing script_new architecture ===\n");
        
        testSourceLocation();
        testASTNodes();
        testScopeManager();
        testParser();
        
        System.out.println("\n=== All tests passed! ===");
    }
    
    private static void testSourceLocation() {
        System.out.println("Testing SourceLocation...");
        
        SourceLocation location = new SourceLocation(10, 20);
        assert location.getLine() == 10 : "Line should be 10";
        assert location.getColumn() == 20 : "Column should be 20";
        
        SourceLocation location2 = new SourceLocation(10, 20);
        assert location.equals(location2) : "Locations should be equal";
        
        System.out.println("  ✓ SourceLocation tests passed");
    }
    
    private static void testASTNodes() {
        System.out.println("Testing AST Nodes...");
        
        SourceLocation loc = new SourceLocation(1, 1);
        
        LiteralNode literal = new LiteralNode(42, int.class, loc);
        assert literal.getValue().equals(42) : "Literal value should be 42";
        assert literal.getType() == int.class : "Literal type should be int.class";
        
        VariableNode variable = new VariableNode("x", loc);
        assert variable.getName().equals("x") : "Variable name should be 'x'";
        
        LiteralNode left = new LiteralNode(1, int.class, loc);
        LiteralNode right = new LiteralNode(2, int.class, loc);
        BinaryOpNode binaryOp = new BinaryOpNode(
                BinaryOpNode.Operator.ADD, left, right, loc);
        assert binaryOp.getOperator() == BinaryOpNode.Operator.ADD : "Operator should be ADD";
        
        UnaryOpNode unaryOp = new UnaryOpNode(
                UnaryOpNode.Operator.NEGATIVE, left, loc);
        assert unaryOp.getOperator() == UnaryOpNode.Operator.NEGATIVE : "Operator should be NEGATIVE";
        
        AssignmentNode assignment = new AssignmentNode(
                "x", literal, true, GenericType.of(int.class), loc);
        assert assignment.getVariableName().equals("x") : "Variable name should be 'x'";
        assert assignment.isDeclaration() : "Should be a declaration";
        
        MethodCallNode methodCall = new MethodCallNode(
                variable, "test", Arrays.asList(literal, right), loc);
        assert methodCall.getMethodName().equals("test") : "Method name should be 'test'";
        assert methodCall.getArguments().size() == 2 : "Should have 2 arguments";
        
        LambdaNode.Parameter param = new LambdaNode.Parameter("x", int.class);
        LambdaNode lambda = new LambdaNode(
                Arrays.asList(param), literal, loc);
        assert lambda.getParameters().size() == 1 : "Should have 1 parameter";
        assert lambda.getParameters().get(0).getName().equals("x") : "Parameter name should be 'x'";
        
        BlockNode block = new BlockNode(Arrays.asList(literal, variable), loc);
        assert block.getStatements().size() == 2 : "Should have 2 statements";
        
        IfNode ifNode = new IfNode(
                new LiteralNode(true, boolean.class, loc),
                literal,
                right,
                loc);
        assert ifNode.hasElse() : "Should have else block";
        
        ForNode forNode = new ForNode(
                new LiteralNode(0, int.class, loc),
                new LiteralNode(true, boolean.class, loc),
                new LiteralNode(1, int.class, loc),
                literal,
                loc);
        assert !forNode.isEnhanced() : "Should be traditional for loop";
        
        ForNode enhancedFor = new ForNode(
                "item",
                variable,
                literal,
                loc);
        assert enhancedFor.isEnhanced() : "Should be enhanced for loop";
        
        System.out.println("  ✓ AST Node tests passed");
    }
    
    private static void testScopeManager() {
        System.out.println("Testing ScopeManager...");
        
        ScopeManager scopeManager = new ScopeManager();
        
        scopeManager.declareVariable("x", int.class, 42, false);
        assert scopeManager.hasVariable("x") : "Variable 'x' should exist";
        assert scopeManager.getVariable("x").getValue().equals(42) : "Variable value should be 42";
        
        scopeManager.enterScope();
        scopeManager.declareVariable("y", String.class, "hello", false);
        assert scopeManager.hasVariable("y") : "Variable 'y' should exist";
        assert scopeManager.getCurrentLevel() == 2 : "Current level should be 2";
        
        scopeManager.setVariable("y", "world");
        assert scopeManager.getVariable("y").getValue().equals("world") : "Variable value should be 'world'";
        
        scopeManager.exitScope();
        assert !scopeManager.hasVariable("y") : "Variable 'y' should not exist in outer scope";
        assert scopeManager.hasVariable("x") : "Variable 'x' should still exist";
        
        scopeManager.declareVariable("finalVar", int.class, 100, true);
        assert scopeManager.getVariable("finalVar").isFinal() : "Variable should be final";
        
        try {
            scopeManager.setVariable("finalVar", 200);
            assert false : "Should not be able to set final variable";
        } catch (RuntimeException e) {
            System.out.println("  ✓ Final variable protection works");
        }
        
        System.out.println("  ✓ ScopeManager tests passed");
    }
    
    private static void testParser() {
        System.out.println("Testing Parser...");
        
        try {
            String source = "int x = 42;";
            Lexer lexer = new Lexer(source);
            Parser parser = new Parser(lexer.tokenize());
            BlockNode block = parser.parse();
            
            assert block.getStatements().size() == 1 : "Should have 1 statement";
            assert block.getStatements().get(0) instanceof AssignmentNode : "Should be AssignmentNode";
            
            System.out.println("  ✓ Parser basic test passed");
        } catch (Exception e) {
            System.err.println("  ✗ Parser test failed: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException(e);
        }
        
        testAutoType();
        testClassReference();
        testFullQualifiedName();
        testAutoWithClassReference();
        testAutoWithFieldAccess();
        testMethodChain();
        testMapLiteral();
        testMapLiteralEvaluation();
        testTryCatch();
        testTryCatchEvaluation();
        testLambda();
        testLambdaEvaluation();
        testLambdaDirectCall();
        testLambdaFunctionalInterface();
        testLambdaAsInterface();
        testLambdaAsRunnable();
        testLambdaAutoConvert();
        testBuiltins();
        testComplexPrograms();
    }
    
    private static void testLambdaAutoConvert() {
        System.out.println("Testing Lambda Auto Convert to Functional Interface...");
        
        try {
            String source = "java.util.function.Supplier f = () -> 42;";
            Lexer lexer = new Lexer(source);
            Parser parser = new Parser(lexer.tokenize());
            BlockNode block = parser.parse();
            
            ExecutionContext context = new ExecutionContext(ScriptNewTestRunner.class.getClassLoader());
            ASTEvaluator.evaluate(block, context);
            
            Object fValue = context.getScopeManager().getVariable("f").getValue();
            assert fValue instanceof java.util.function.Supplier : "f should be a Supplier";
            
            java.util.function.Supplier<?> supplier = (java.util.function.Supplier<?>) fValue;
            Object result = supplier.get();
            assert result.equals(42) : "supplier.get() should return 42, got: " + result;
            
            String source2 = "java.util.function.Function f2 = x -> x * 2;";
            lexer = new Lexer(source2);
            parser = new Parser(lexer.tokenize());
            block = parser.parse();
            ASTEvaluator.evaluate(block, context);
            
            Object f2Value = context.getScopeManager().getVariable("f2").getValue();
            assert f2Value instanceof java.util.function.Function : "f2 should be a Function";
            
            @SuppressWarnings("unchecked")
            java.util.function.Function<Object, Object> func = (java.util.function.Function<Object, Object>) f2Value;
            Object funcResult = func.apply(21);
            assert funcResult.equals(42.0) : "func.apply(21) should return 42.0, got: " + funcResult;
            
            System.out.println("  ✓ Lambda auto convert test passed");
        } catch (Exception e) {
            System.err.println("  ✗ Lambda auto convert test failed: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }
    
    private static void testLambdaDirectCall() {
        System.out.println("Testing Lambda Direct Call...");
        
        try {
            String source = "auto f = x -> x * 2; auto result = f(21);";
            Lexer lexer = new Lexer(source);
            Parser parser = new Parser(lexer.tokenize());
            BlockNode block = parser.parse();
            
            ExecutionContext context = new ExecutionContext(ScriptNewTestRunner.class.getClassLoader());
            ASTEvaluator.evaluate(block, context);
            
            Object result = context.getScopeManager().getVariable("result").getValue();
            assert result.equals(42) : "f(21) should return 42, got: " + result;
            
            System.out.println("  ✓ Lambda direct call test passed");
        } catch (Exception e) {
            System.err.println("  ✗ Lambda direct call test failed: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }
    
    private static void testLambdaFunctionalInterface() {
        System.out.println("Testing Lambda with Java Functional Interface...");
        
        try {
            String source = "auto f = x -> x + 1;";
            Lexer lexer = new Lexer(source);
            Parser parser = new Parser(lexer.tokenize());
            BlockNode block = parser.parse();
            
            ExecutionContext context = new ExecutionContext(ScriptNewTestRunner.class.getClassLoader());
            ASTEvaluator.evaluate(block, context);
            
            Object fValue = context.getScopeManager().getVariable("f").getValue();
            assert fValue instanceof Lambda : "f should be a Lambda";
            
            Lambda func = (Lambda) fValue;
            
            assert func instanceof java.util.function.Function : "Lambda should implement Function";
            
            java.util.function.Function<Object[], Object> javaFunc = (java.util.function.Function<Object[], Object>) func;
            Object result = javaFunc.apply(new Object[]{10});
            assert result.equals(11) : "apply(10) should return 11, got: " + result;
            
            Object callResult = func.call(20);
            assert callResult.equals(21) : "call(20) should return 21, got: " + callResult;
            
            System.out.println("  ✓ Lambda functional interface test passed");
        } catch (Exception e) {
            System.err.println("  ✗ Lambda functional interface test failed: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }
    
    private static void testLambdaAsInterface() {
        System.out.println("Testing Lambda asInterface...");
        
        try {
            String source = "auto f = x -> x * 2;";
            Lexer lexer = new Lexer(source);
            Parser parser = new Parser(lexer.tokenize());
            BlockNode block = parser.parse();
            
            ExecutionContext context = new ExecutionContext(ScriptNewTestRunner.class.getClassLoader());
            ASTEvaluator.evaluate(block, context);
            
            Lambda func = (Lambda) context.getScopeManager().getVariable("f").getValue();
            
            java.util.function.Function<Object, Object> objFunc = func.asInterface(java.util.function.Function.class);
            Object result = objFunc.apply(21);
            assert result.equals(42.0) : "Function.apply(21) should return 42.0, got: " + result;
            
            java.util.function.Consumer<Object> consumer = func.asInterface(java.util.function.Consumer.class);
            consumer.accept(10);
            
            System.out.println("  ✓ Lambda asInterface test passed");
        } catch (Exception e) {
            System.err.println("  ✗ Lambda asInterface test failed: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }
    
    private static void testLambdaAsRunnable() {
        System.out.println("Testing Lambda as Runnable/Supplier...");
        
        try {
            String source = "auto f = () -> 42;";
            Lexer lexer = new Lexer(source);
            Parser parser = new Parser(lexer.tokenize());
            BlockNode block = parser.parse();
            
            ExecutionContext context = new ExecutionContext(ScriptNewTestRunner.class.getClassLoader());
            ASTEvaluator.evaluate(block, context);
            
            Lambda func = (Lambda) context.getScopeManager().getVariable("f").getValue();
            
            java.util.function.Supplier<Object> supplier = func.asInterface(java.util.function.Supplier.class);
            Object supplierResult = supplier.get();
            assert supplierResult.equals(42) : "Supplier.get() should return 42, got: " + supplierResult;
            
            Runnable runnable = func.asInterface(Runnable.class);
            runnable.run();
            
            System.out.println("  ✓ Lambda as Runnable/Supplier test passed");
        } catch (Exception e) {
            System.err.println("  ✗ Lambda as Runnable/Supplier test failed: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }
    
    private static void testLambda() {
        System.out.println("Testing Lambda Expression...");
        
        try {
            String source = "x -> x + 1;";
            Lexer lexer = new Lexer(source);
            Parser parser = new Parser(lexer.tokenize());
            BlockNode block = parser.parse();
            
            assert block.getStatements().size() == 1 : "Should have 1 statement";
            assert block.getStatements().get(0) instanceof LambdaNode : "Should be LambdaNode";
            
            LambdaNode lambda = (LambdaNode) block.getStatements().get(0);
            assert lambda.getParameters().size() == 1 : "Should have 1 parameter";
            assert lambda.getParameters().get(0).getName().equals("x") : "Parameter name should be 'x'";
            
            System.out.println("  ✓ Lambda expression test passed");
        } catch (Exception e) {
            System.err.println("  ✗ Lambda expression test failed: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }
    
    private static void testLambdaEvaluation() {
        System.out.println("Testing Lambda Expression Evaluation...");
        
        try {
            String source = "auto f = x -> x + 1;";
            Lexer lexer = new Lexer(source);
            Parser parser = new Parser(lexer.tokenize());
            BlockNode block = parser.parse();
            
            ExecutionContext context = new ExecutionContext(ScriptNewTestRunner.class.getClassLoader());
            ASTEvaluator.evaluate(block, context);
            
            Object fValue = context.getScopeManager().getVariable("f").getValue();
            assert fValue instanceof Lambda : "f should be a Lambda";
            
            Lambda func = (Lambda) fValue;
            assert func.getParameterCount() == 1 : "Function should have 1 parameter";
            
            Object result = func.invoke(new Object[]{5});
            assert result.equals(6) : "f(5) should return 6, got: " + result;
            
            System.out.println("  ✓ Lambda expression evaluation test passed");
        } catch (Exception e) {
            System.err.println("  ✗ Lambda expression evaluation test failed: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }
    
    private static void testTryCatch() {
        System.out.println("Testing Try-Catch...");
        
        try {
            String source = "try { int x = 1; } catch (Exception e) { int y = 2; }";
            Lexer lexer = new Lexer(source);
            Parser parser = new Parser(lexer.tokenize());
            BlockNode block = parser.parse();
            
            assert block.getStatements().size() == 1 : "Should have 1 statement";
            assert block.getStatements().get(0) instanceof TryNode : "Should be TryNode";
            
            TryNode tryNode = (TryNode) block.getStatements().get(0);
            assert tryNode.getCatchClauses().size() == 1 : "Should have 1 catch clause";
            assert tryNode.getFinallyBlock() == null : "Should not have finally block";
            
            System.out.println("  ✓ Try-Catch test passed");
        } catch (Exception e) {
            System.err.println("  ✗ Try-Catch test failed: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }
    
    private static void testTryCatchEvaluation() {
        System.out.println("Testing Try-Catch Evaluation...");
        
        try {
            String source = "try { int x = 1; } catch (Exception e) { int y = 2; }";
            Lexer lexer = new Lexer(source);
            Parser parser = new Parser(lexer.tokenize());
            BlockNode block = parser.parse();
            
            ExecutionContext context = new ExecutionContext(ScriptNewTestRunner.class.getClassLoader());
            Object result = ASTEvaluator.evaluate(block, context);
            
            assert context.getScopeManager().hasVariable("x") : "Variable x should exist";
            assert context.getScopeManager().getVariable("x").getValue().equals(1) : "x should be 1";
            
            System.out.println("  ✓ Try-Catch evaluation test passed");
        } catch (Exception e) {
            System.err.println("  ✗ Try-Catch evaluation test failed: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }
    
    private static void testMapLiteral() {
        System.out.println("Testing Map Literal...");
        
        try {
            String source = "auto map = {\"key1\": \"value1\"};";
            Lexer lexer = new Lexer(source);
            Parser parser = new Parser(lexer.tokenize());
            BlockNode block = parser.parse();
            
            assert block.getStatements().size() == 1 : "Should have 1 statement";
            assert block.getStatements().get(0) instanceof AssignmentNode : "Should be AssignmentNode";
            
            AssignmentNode assignment = (AssignmentNode) block.getStatements().get(0);
            assert assignment.getVariableName().equals("map") : "Variable name should be 'map'";
            assert assignment.getValue() instanceof MapLiteralNode : "Value should be MapLiteralNode";
            
            System.out.println("  ✓ Map literal test passed");
        } catch (Exception e) {
            System.err.println("  ✗ Map literal test failed: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }
    
    private static void testMapLiteralEvaluation() {
        System.out.println("Testing Map Literal Evaluation...");
        
        try {
            String source = "auto map = {\"key1\": \"value1\", \"key2\": 42};";
            Lexer lexer = new Lexer(source);
            Parser parser = new Parser(lexer.tokenize());
            BlockNode block = parser.parse();
            
            ExecutionContext context = new ExecutionContext(ScriptNewTestRunner.class.getClassLoader());
            Object result = ASTEvaluator.evaluate(block, context);
            
            Object mapObj = context.getScopeManager().getVariable("map").getValue();
            assert mapObj instanceof Map : "Result should be a Map";
            
            @SuppressWarnings("unchecked")
            Map<Object, Object> map = (Map<Object, Object>) mapObj;
            assert map.size() == 2 : "Map should have 2 entries";
            assert map.get("key1").equals("value1") : "key1 should map to value1";
            assert map.get("key2").equals(42) : "key2 should map to 42";
            
            System.out.println("  ✓ Map literal evaluation test passed");
        } catch (Exception e) {
            System.err.println("  ✗ Map literal evaluation test failed: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }
    
    private static void testAutoWithClassReference() {
        System.out.println("Testing Auto with Class Reference...");
        
        try {
            String source = "auto cls = java.lang.System;";
            Lexer lexer = new Lexer(source);
            Parser parser = new Parser(lexer.tokenize());
            BlockNode block = parser.parse();
            
            assert block.getStatements().size() == 1 : "Should have 1 statement";
            assert block.getStatements().get(0) instanceof AssignmentNode : "Should be AssignmentNode";
            
            AssignmentNode assignment = (AssignmentNode) block.getStatements().get(0);
            assert assignment.getVariableName().equals("cls") : "Variable name should be 'cls'";
            assert assignment.getDeclaredType().getRawType() == Object.class : "Auto type should be Object.class";
            assert assignment.isDeclaration() : "Should be a declaration";
            
            System.out.println("  ✓ Auto with class reference test passed");
        } catch (Exception e) {
            System.err.println("  ✗ Auto with class reference test failed: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }
    
    private static void testAutoWithFieldAccess() {
        System.out.println("Testing Auto with Field Access...");
        
        try {
            String source = "auto b = java.lang.System.out;";
            Lexer lexer = new Lexer(source);
            Parser parser = new Parser(lexer.tokenize());
            BlockNode block = parser.parse();
            
            assert block.getStatements().size() == 1 : "Should have 1 statement";
            assert block.getStatements().get(0) instanceof AssignmentNode : "Should be AssignmentNode";
            
            AssignmentNode assignment = (AssignmentNode) block.getStatements().get(0);
            assert assignment.getVariableName().equals("b") : "Variable name should be 'b'";
            assert assignment.getDeclaredType().getRawType() == Object.class : "Auto type should be Object.class";
            assert assignment.isDeclaration() : "Should be a declaration";
            
            System.out.println("  ✓ Auto with field access test passed");
        } catch (Exception e) {
            System.err.println("  ✗ Auto with field access test failed: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }
    
    private static void testMethodChain() {
        System.out.println("Testing Method Chain...");
        
        try {
            String source = "cls.out.println(1);";
            Lexer lexer = new Lexer(source);
            Parser parser = new Parser(lexer.tokenize());
            BlockNode block = parser.parse();
            
            assert block.getStatements().size() == 1 : "Should have 1 statement";
            assert block.getStatements().get(0) instanceof MethodCallNode : "Should be MethodCallNode";
            
            MethodCallNode methodCall = (MethodCallNode) block.getStatements().get(0);
            assert methodCall.getMethodName().equals("println") : "Method name should be 'println'";
            assert methodCall.getArguments().size() == 1 : "Should have 1 argument";
            
            System.out.println("  ✓ Method chain test passed");
        } catch (Exception e) {
            System.err.println("  ✗ Method chain test failed: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }
    
    private static void testAutoType() {
        System.out.println("Testing Auto Type...");
        
        try {
            String source = "auto x = 42;";
            Lexer lexer = new Lexer(source);
            Parser parser = new Parser(lexer.tokenize());
            BlockNode block = parser.parse();
            
            assert block.getStatements().size() == 1 : "Should have 1 statement";
            assert block.getStatements().get(0) instanceof AssignmentNode : "Should be AssignmentNode";
            
            AssignmentNode assignment = (AssignmentNode) block.getStatements().get(0);
            assert assignment.getVariableName().equals("x") : "Variable name should be 'x'";
            assert assignment.getDeclaredType().getRawType() == Object.class : "Auto type should be Object.class";
            assert assignment.isDeclaration() : "Should be a declaration";
            
            System.out.println("  ✓ Auto type test passed");
        } catch (Exception e) {
            System.err.println("  ✗ Auto type test failed: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }
    
    private static void testClassReference() {
        System.out.println("Testing Class Reference...");
        
        try {
            String source = "String;";
            Lexer lexer = new Lexer(source);
            Parser parser = new Parser(lexer.tokenize());
            BlockNode block = parser.parse();
            
            assert block.getStatements().size() == 1 : "Should have 1 statement";
            assert block.getStatements().get(0) instanceof ClassReferenceNode : "Should be ClassReferenceNode";
            
            ClassReferenceNode classRef = (ClassReferenceNode) block.getStatements().get(0);
            assert classRef.getClassName().equals("String") : "Class name should be 'String'";
            
            System.out.println("  ✓ Class reference test passed");
        } catch (Exception e) {
            System.err.println("  ✗ Class reference test failed: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }
    
    private static void testFullQualifiedName() {
        System.out.println("Testing Fully Qualified Name...");
        
        try {
            String source = "java.lang.System;";
            Lexer lexer = new Lexer(source);
            Parser parser = new Parser(lexer.tokenize());
            BlockNode block = parser.parse();
            
            assert block.getStatements().size() == 1 : "Should have 1 statement";
            assert block.getStatements().get(0) instanceof ClassReferenceNode : "Should be ClassReferenceNode";
            
            ClassReferenceNode classRef = (ClassReferenceNode) block.getStatements().get(0);
            assert classRef.getClassName().equals("java.lang.System") : "Class name should be 'java.lang.System'";
            
            System.out.println("  ✓ Fully qualified name test passed");
        } catch (Exception e) {
            System.err.println("  ✗ Fully qualified name test failed: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }
    
    private static void testBuiltins() {
        System.out.println("Testing Builtins...");
        
        try {
            String source = "println(\"Hello, World!\");";
            Lexer lexer = new Lexer(source);
            Parser parser = new Parser(lexer.tokenize());
            BlockNode block = parser.parse();
            
            ExecutionContext context = new ExecutionContext(ScriptNewTestRunner.class.getClassLoader());
            ASTEvaluator.evaluate(block, context);
            
            System.out.println("  ✓ println test passed");
        } catch (Exception e) {
            System.err.println("  ✗ println test failed: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException(e);
        }
        
        try {
            String source = "auto nums = range(0, 10, 2); println(size(nums));";
            Lexer lexer = new Lexer(source);
            Parser parser = new Parser(lexer.tokenize());
            BlockNode block = parser.parse();
            
            ExecutionContext context = new ExecutionContext(ScriptNewTestRunner.class.getClassLoader());
            ASTEvaluator.evaluate(block, context);
            
            Object nums = context.getScopeManager().getVariable("nums").getValue();
            assert nums instanceof java.util.List : "range should return a List";
            assert ((java.util.List<?>) nums).size() == 5 : "range(0, 10, 2) should have 5 elements";
            
            System.out.println("  ✓ range and size test passed");
        } catch (Exception e) {
            System.err.println("  ✗ range/size test failed: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException(e);
        }
        
        try {
            String source = "auto r = random(); auto i = randint(1, 100); auto m = min(1, 2, 3);";
            Lexer lexer = new Lexer(source);
            Parser parser = new Parser(lexer.tokenize());
            BlockNode block = parser.parse();
            
            ExecutionContext context = new ExecutionContext(ScriptNewTestRunner.class.getClassLoader());
            ASTEvaluator.evaluate(block, context);
            
            Object r = context.getScopeManager().getVariable("r").getValue();
            assert r instanceof Double : "random should return Double";
            assert (Double) r >= 0.0 && (Double) r < 1.0 : "random should be in [0, 1)";
            
            Object i = context.getScopeManager().getVariable("i").getValue();
            assert i instanceof Integer : "randint should return Integer";
            assert (Integer) i >= 1 && (Integer) i <= 100 : "randint should be in [1, 100]";
            
            Object m = context.getScopeManager().getVariable("m").getValue();
            assert m.equals(1.0) : "min(1, 2, 3) should be 1.0";
            
            System.out.println("  ✓ math functions test passed");
        } catch (Exception e) {
            System.err.println("  ✗ math functions test failed: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException(e);
        }
        
        try {
            String source = "auto parts = split(\"a,b,c\", \",\"); auto joined = join(parts, \"-\");";
            Lexer lexer = new Lexer(source);
            Parser parser = new Parser(lexer.tokenize());
            BlockNode block = parser.parse();
            
            ExecutionContext context = new ExecutionContext(ScriptNewTestRunner.class.getClassLoader());
            ASTEvaluator.evaluate(block, context);
            
            Object parts = context.getScopeManager().getVariable("parts").getValue();
            assert parts instanceof java.util.List : "split should return a List";
            assert ((java.util.List<?>) parts).size() == 3 : "split should have 3 parts";
            
            Object joined = context.getScopeManager().getVariable("joined").getValue();
            assert joined.equals("a-b-c") : "join should produce 'a-b-c'";
            
            System.out.println("  ✓ string functions test passed");
        } catch (Exception e) {
            System.err.println("  ✗ string functions test failed: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException(e);
        }
        
        try {
            String source = "auto t = typename(42); auto arr = new int[5]; auto s = size(arr);";
            Lexer lexer = new Lexer(source);
            Parser parser = new Parser(lexer.tokenize());
            BlockNode block = parser.parse();
            
            ExecutionContext context = new ExecutionContext(ScriptNewTestRunner.class.getClassLoader());
            ASTEvaluator.evaluate(block, context);
            
            Object t = context.getScopeManager().getVariable("t").getValue();
            assert t.equals("Integer") : "typename(42) should be 'Integer'";
            
            Object s = context.getScopeManager().getVariable("s").getValue();
            assert s.equals(5) : "size of array should be 5";
            
            System.out.println("  ✓ utility functions test passed");
        } catch (Exception e) {
            System.err.println("  ✗ utility functions test failed: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }
    
    private static void testComplexPrograms() {
        System.out.println("Testing Complex Programs...");
        
        testQuickSort();
        testFibonacci();
        testClosure();
        testHigherOrderFunction();
    }
    
    private static void testQuickSort() {
        System.out.println("  Testing QuickSort...");
        
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
            
            ExecutionContext context = new ExecutionContext(ScriptNewTestRunner.class.getClassLoader());
            ASTEvaluator.evaluate(block, context);
            
            System.out.println("    ✓ QuickSort test passed");
        } catch (Exception e) {
            System.err.println("    ✗ QuickSort test failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private static void testFibonacci() {
        System.out.println("  Testing Fibonacci...");
        
        try {
            String source = """
                auto fib = n -> {
                    if (n <= 1) {
                        return n;
                    }
                    return fib(n - 1) + fib(n - 2);
                };
                
                println("fib(10) = " + fib(10));
                auto result = fib(10);
                """;
            
            Lexer lexer = new Lexer(source);
            Parser parser = new Parser(lexer.tokenize());
            BlockNode block = parser.parse();
            
            ExecutionContext context = new ExecutionContext(ScriptNewTestRunner.class.getClassLoader());
            ASTEvaluator.evaluate(block, context);
            
            Object result = context.getScopeManager().getVariable("result").getValue();
            assert result.equals(55) : "fib(10) should be 55";
            
            System.out.println("    ✓ Fibonacci test passed");
        } catch (Exception e) {
            System.err.println("    ✗ Fibonacci test failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private static void testClosure() {
        System.out.println("  Testing Closure...");
        
        try {
            String source = """
                auto makeCounter = () -> {
                    int count = 0;
                    return () -> {
                        count = count + 1;
                        return count;
                    };
                };
                
                auto counter = makeCounter();
                auto a = counter();
                auto b = counter();
                auto c = counter();
                println("Counter: " + a + ", " + b + ", " + c);
                """;
            
            Lexer lexer = new Lexer(source);
            Parser parser = new Parser(lexer.tokenize());
            BlockNode block = parser.parse();
            
            ExecutionContext context = new ExecutionContext(ScriptNewTestRunner.class.getClassLoader());
            ASTEvaluator.evaluate(block, context);
            
            Object a = context.getScopeManager().getVariable("a").getValue();
            Object b = context.getScopeManager().getVariable("b").getValue();
            Object c = context.getScopeManager().getVariable("c").getValue();
            
            assert a.equals(1) : "First call should return 1";
            assert b.equals(2) : "Second call should return 2";
            assert c.equals(3) : "Third call should return 3";
            
            System.out.println("    ✓ Closure test passed");
        } catch (Exception e) {
            System.err.println("    ✗ Closure test failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private static void testHigherOrderFunction() {
        System.out.println("  Testing Higher Order Function...");
        
        try {
            String source = """
                auto map = (arr, f) -> {
                    auto result = [];
                    for (auto x in arr) {
                        result = result + [f(x)];
                    }
                    return result;
                };
                
                auto filter = (arr, pred) -> {
                    auto result = [];
                    for (auto x in arr) {
                        if (pred(x)) {
                            result = result + [x];
                        }
                    }
                    return result;
                };
                
                auto reduce = (arr, f, init) -> {
                    auto acc = init;
                    for (auto x in arr) {
                        acc = f(acc, x);
                    }
                    return acc;
                };
                
                auto nums = [1, 2, 3, 4, 5];
                auto doubled = map(nums, x -> x * 2);
                auto evens = filter(nums, x -> x % 2 == 0);
                auto sum = reduce(nums, (acc, x) -> acc + x, 0);
                
                println("Original: " + nums);
                println("Doubled: " + doubled);
                println("Evens: " + evens);
                println("Sum: " + sum);
                """;
            
            Lexer lexer = new Lexer(source);
            Parser parser = new Parser(lexer.tokenize());
            BlockNode block = parser.parse();
            
            ExecutionContext context = new ExecutionContext(ScriptNewTestRunner.class.getClassLoader());
            ASTEvaluator.evaluate(block, context);
            
            System.out.println("    ✓ Higher Order Function test passed");
        } catch (Exception e) {
            System.err.println("    ✗ Higher Order Function test failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
