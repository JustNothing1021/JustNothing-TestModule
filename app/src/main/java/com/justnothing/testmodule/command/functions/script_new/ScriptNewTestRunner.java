package com.justnothing.testmodule.command.functions.script_new;

import com.justnothing.testmodule.command.functions.script_new.ast.SourceLocation;
import com.justnothing.testmodule.command.functions.script_new.ast.nodes.*;
import com.justnothing.testmodule.command.functions.script_new.evaluator.ScopeManager;
import com.justnothing.testmodule.command.functions.script_new.parser.Parser;
import com.justnothing.testmodule.command.functions.script_new.lexer.Lexer;

import java.util.Arrays;

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
                "x", literal, true, int.class, loc);
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
            assert assignment.getDeclaredType() == Object.class : "Auto type should be Object.class";
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
            assert assignment.getDeclaredType() == Object.class : "Auto type should be Object.class";
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
            assert assignment.getDeclaredType() == Object.class : "Auto type should be Object.class";
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
}
