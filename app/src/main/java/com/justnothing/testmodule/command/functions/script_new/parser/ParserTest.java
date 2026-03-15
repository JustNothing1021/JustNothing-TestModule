package com.justnothing.testmodule.command.functions.script_new.parser;

import com.justnothing.testmodule.command.functions.script_new.ast.nodes.*;
import com.justnothing.testmodule.command.functions.script_new.ast.ASTNode;
import com.justnothing.testmodule.command.functions.script_new.lexer.Lexer;
import com.justnothing.testmodule.command.functions.script_new.lexer.Token;
import com.justnothing.testmodule.command.functions.script_new.lexer.TokenType;

import java.util.List;

public class ParserTest {
    
    public static void main(String[] args) {
        System.out.println("=== Parser Test ===\n");
        
        int totalTests = 0;
        int passedTests = 0;
        
        totalTests++;
        if (testSimpleExpression()) {
            passedTests++;
        }
        
        totalTests++;
        if (testVariableDeclaration()) {
            passedTests++;
        }
        
        totalTests++;
        if (testIfStatement()) {
            passedTests++;
        }
        
        totalTests++;
        if (testForStatement()) {
            passedTests++;
        }
        
        totalTests++;
        if (testBinaryOperators()) {
            passedTests++;
        }
        
        totalTests++;
        if (testMethodCall()) {
            passedTests++;
        }
        
        totalTests++;
        if (testLambdaExpression()) {
            passedTests++;
        }
        
        System.out.println("\n=== Test Results ===");
        System.out.println("Total: " + totalTests + " tests");
        System.out.println("Passed: " + passedTests + " tests");
        System.out.println("Failed: " + (totalTests - passedTests) + " tests");
        
        if (passedTests == totalTests) {
            System.out.println("\n✅ All tests passed!");
            System.exit(0);
        } else {
            System.out.println("\n❌ Some tests failed!");
            System.exit(1);
        }
    }
    
    private static boolean testSimpleExpression() {
        System.out.println("Test 1: Simple Expression");
        try {
            String source = "x + y";
            Lexer lexer = new Lexer(source);
            List<Token> tokens = lexer.tokenize();
            Parser parser = new Parser(tokens);
            BlockNode block = parser.parse();
            
            assert block.getStatements().size() == 1 : "Should have 1 statement";
            ASTNode stmt = block.getStatements().get(0);
            assert stmt instanceof BinaryOpNode : "Should be BinaryOpNode";
            BinaryOpNode binaryOp = (BinaryOpNode) stmt;
            assert binaryOp.getOperator() == BinaryOpNode.Operator.ADD : "Should be ADD operator";
            
            System.out.println("  ✓ PASSED");
            return true;
        } catch (Exception e) {
            System.out.println("  ✗ FAILED: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    private static boolean testVariableDeclaration() {
        System.out.println("Test 2: Variable Declaration");
        try {
            String source = "int x = 42;";
            Lexer lexer = new Lexer(source);
            List<Token> tokens = lexer.tokenize();
            Parser parser = new Parser(tokens);
            BlockNode block = parser.parse();
            
            assert block.getStatements().size() == 1 : "Should have 1 statement";
            ASTNode stmt = block.getStatements().get(0);
            assert stmt instanceof AssignmentNode : "Should be AssignmentNode";
            AssignmentNode assignment = (AssignmentNode) stmt;
            assert assignment.getVariableName().equals("x") : "Variable name should be 'x'";
            assert assignment.isDeclaration() : "Should be a declaration";
            assert assignment.getDeclaredType() == int.class : "Type should be int.class";
            
            System.out.println("  ✓ PASSED");
            return true;
        } catch (Exception e) {
            System.out.println("  ✗ FAILED: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    private static boolean testIfStatement() {
        System.out.println("Test 3: If Statement");
        try {
            String source = "if (x > 0) { x = x + 1; }";
            Lexer lexer = new Lexer(source);
            List<Token> tokens = lexer.tokenize();
            Parser parser = new Parser(tokens);
            BlockNode block = parser.parse();
            
            assert block.getStatements().size() == 1 : "Should have 1 statement";
            ASTNode stmt = block.getStatements().get(0);
            assert stmt instanceof IfNode : "Should be IfNode";
            IfNode ifNode = (IfNode) stmt;
            assert ifNode.hasElse() : "Should have else block";
            
            System.out.println("  ✓ PASSED");
            return true;
        } catch (Exception e) {
            System.out.println("  ✗ FAILED: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    private static boolean testForStatement() {
        System.out.println("Test 4: For Statement");
        try {
            String source = "for (int i = 0; i < 10; i++) { println(i); }";
            Lexer lexer = new Lexer(source);
            List<Token> tokens = lexer.tokenize();
            Parser parser = new Parser(tokens);
            BlockNode block = parser.parse();
            
            assert block.getStatements().size() == 1 : "Should have 1 statement";
            ASTNode stmt = block.getStatements().get(0);
            assert stmt instanceof ForNode : "Should be ForNode";
            ForNode forNode = (ForNode) stmt;
            assert !forNode.isEnhanced() : "Should be traditional for loop";
            
            System.out.println("  ✓ PASSED");
            return true;
        } catch (Exception e) {
            System.out.println("  ✗ FAILED: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    private static boolean testBinaryOperators() {
        System.out.println("Test 5: Binary Operators");
        try {
            String source = "x + y * z - w / v";
            Lexer lexer = new Lexer(source);
            List<Token> tokens = lexer.tokenize();
            Parser parser = new Parser(tokens);
            BlockNode block = parser.parse();
            
            assert block.getStatements().size() == 1 : "Should have 1 statement";
            ASTNode stmt = block.getStatements().get(0);
            
            System.out.println("  ✓ PASSED");
            return true;
        } catch (Exception e) {
            System.out.println("  ✗ FAILED: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    private static boolean testMethodCall() {
        System.out.println("Test 6: Method Call");
        try {
            String source = "obj.method(1, 2, 3);";
            Lexer lexer = new Lexer(source);
            List<Token> tokens = lexer.tokenize();
            Parser parser = new Parser(tokens);
            BlockNode block = parser.parse();
            
            assert block.getStatements().size() == 1 : "Should have 1 statement";
            ASTNode stmt = block.getStatements().get(0);
            assert stmt instanceof MethodCallNode : "Should be MethodCallNode";
            MethodCallNode methodCall = (MethodCallNode) stmt;
            assert methodCall.getMethodName().equals("method") : "Method name should be 'method'";
            assert methodCall.getArguments().size() == 3 : "Should have 3 arguments";
            
            System.out.println("  ✓ PASSED");
            return true;
        } catch (Exception e) {
            System.out.println("  ✗ FAILED: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    private static boolean testLambdaExpression() {
        System.out.println("Test 7: Lambda Expression");
        try {
            String source = "x -> x + 1";
            Lexer lexer = new Lexer(source);
            List<Token> tokens = lexer.tokenize();
            Parser parser = new Parser(tokens);
            BlockNode block = parser.parse();
            
            assert block.getStatements().size() == 1 : "Should have 1 statement";
            ASTNode stmt = block.getStatements().get(0);
            assert stmt instanceof LambdaNode : "Should be LambdaNode";
            LambdaNode lambda = (LambdaNode) stmt;
            assert lambda.getParameters().size() == 1 : "Should have 1 parameter";
            assert lambda.getParameters().get(0).getName().equals("x") : "Parameter name should be 'x'";
            
            System.out.println("  ✓ PASSED");
            return true;
        } catch (Exception e) {
            System.out.println("  ✗ FAILED: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
}
