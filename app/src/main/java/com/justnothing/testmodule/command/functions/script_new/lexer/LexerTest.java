package com.justnothing.testmodule.command.functions.script_new.lexer;

import java.util.List;

public class LexerTest {
    
    public static void main(String[] args) {
        System.out.println("=== Lexer Test ===\n");
        
        int totalTests = 0;
        int passedTests = 0;
        
        totalTests++;
        if (testSimpleExpression()) {
            passedTests++;
        }
        
        totalTests++;
        if (testKeywords()) {
            passedTests++;
        }
        
        totalTests++;
        if (testLiterals()) {
            passedTests++;
        }
        
        totalTests++;
        if (testOperators()) {
            passedTests++;
        }
        
        totalTests++;
        if (testDelimiters()) {
            passedTests++;
        }
        
        totalTests++;
        if (testComplexExpression()) {
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
            
            assert tokens.size() == 4 : "Should have 4 tokens (including EOF)";
            assert tokens.get(0).getType() == TokenType.IDENTIFIER : "First token should be IDENTIFIER";
            assert tokens.get(0).getText().equals("x") : "First token text should be 'x'";
            assert tokens.get(1).getType() == TokenType.OPERATOR_PLUS : "Second token should be OPERATOR_PLUS";
            assert tokens.get(2).getType() == TokenType.IDENTIFIER : "Third token should be IDENTIFIER";
            assert tokens.get(2).getText().equals("y") : "Third token text should be 'y'";
            assert tokens.get(3).getType() == TokenType.EOF : "Last token should be EOF";
            
            System.out.println("  ✓ PASSED");
            return true;
        } catch (Exception e) {
            System.out.println("  ✗ FAILED: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    private static boolean testKeywords() {
        System.out.println("Test 2: Keywords");
        try {
            String source = "if (true) { return; }";
            Lexer lexer = new Lexer(source);
            List<Token> tokens = lexer.tokenize();
            
            assert tokens.get(0).getType() == TokenType.KEYWORD_IF : "Should have KEYWORD_IF";
            assert tokens.get(1).getType() == TokenType.DELIMITER_LEFT_PAREN : "Should have LEFT_PAREN";
            assert tokens.get(2).getType() == TokenType.LITERAL_BOOLEAN : "Should have LITERAL_BOOLEAN";
            assert tokens.get(3).getType() == TokenType.DELIMITER_RIGHT_PAREN : "Should have RIGHT_PAREN";
            assert tokens.get(4).getType() == TokenType.DELIMITER_LEFT_BRACE : "Should have LEFT_BRACE";
            assert tokens.get(5).getType() == TokenType.KEYWORD_RETURN : "Should have KEYWORD_RETURN";
            assert tokens.get(6).getType() == TokenType.DELIMITER_SEMICOLON : "Should have SEMICOLON";
            assert tokens.get(7).getType() == TokenType.DELIMITER_RIGHT_BRACE : "Should have RIGHT_BRACE";
            
            System.out.println("  ✓ PASSED");
            return true;
        } catch (Exception e) {
            System.out.println("  ✗ FAILED: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    private static boolean testLiterals() {
        System.out.println("Test 3: Literals");
        try {
            String source = "42 3.14 \"hello\" 'world' true false null";
            Lexer lexer = new Lexer(source);
            List<Token> tokens = lexer.tokenize();
            
            assert tokens.get(0).getType() == TokenType.LITERAL_INTEGER : "Should have LITERAL_INTEGER";
            assert tokens.get(0).getValue().equals(42L) : "Integer value should be 42";
            
            assert tokens.get(1).getType() == TokenType.LITERAL_DECIMAL : "Should have LITERAL_DECIMAL";
            assert tokens.get(1).getValue().equals(3.14) : "Decimal value should be 3.14";
            
            assert tokens.get(2).getType() == TokenType.LITERAL_STRING : "Should have LITERAL_STRING";
            assert tokens.get(2).getValue().equals("hello") : "String value should be 'hello'";
            
            assert tokens.get(3).getType() == TokenType.LITERAL_STRING : "Should have LITERAL_STRING";
            assert tokens.get(3).getValue().equals("world") : "String value should be 'world'";
            
            assert tokens.get(4).getType() == TokenType.LITERAL_BOOLEAN : "Should have LITERAL_BOOLEAN";
            assert tokens.get(4).getValue().equals(true) : "Boolean value should be true";
            
            assert tokens.get(5).getType() == TokenType.LITERAL_BOOLEAN : "Should have LITERAL_BOOLEAN";
            assert tokens.get(5).getValue().equals(false) : "Boolean value should be false";
            
            assert tokens.get(6).getType() == TokenType.LITERAL_NULL : "Should have LITERAL_NULL";
            assert tokens.get(6).getValue() == null : "Null value should be null";
            
            System.out.println("  ✓ PASSED");
            return true;
        } catch (Exception e) {
            System.out.println("  ✗ FAILED: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    private static boolean testOperators() {
        System.out.println("Test 4: Operators");
        try {
            String source = "+ - * / % = == != < > <= >= && || ! & | ^ ~ << >> >>> ++ --";
            Lexer lexer = new Lexer(source);
            List<Token> tokens = lexer.tokenize();
            
            assert tokens.get(0).getType() == TokenType.OPERATOR_PLUS : "Should have OPERATOR_PLUS";
            assert tokens.get(1).getType() == TokenType.OPERATOR_MINUS : "Should have OPERATOR_MINUS";
            assert tokens.get(2).getType() == TokenType.OPERATOR_MULTIPLY : "Should have OPERATOR_MULTIPLY";
            assert tokens.get(3).getType() == TokenType.OPERATOR_DIVIDE : "Should have OPERATOR_DIVIDE";
            assert tokens.get(4).getType() == TokenType.OPERATOR_MODULO : "Should have OPERATOR_MODULO";
            assert tokens.get(5).getType() == TokenType.OPERATOR_ASSIGN : "Should have OPERATOR_ASSIGN";
            assert tokens.get(6).getType() == TokenType.OPERATOR_EQUAL : "Should have OPERATOR_EQUAL";
            assert tokens.get(7).getType() == TokenType.OPERATOR_NOT_EQUAL : "Should have OPERATOR_NOT_EQUAL";
            assert tokens.get(8).getType() == TokenType.OPERATOR_LESS_THAN : "Should have OPERATOR_LESS_THAN";
            assert tokens.get(9).getType() == TokenType.OPERATOR_GREATER_THAN : "Should have OPERATOR_GREATER_THAN";
            assert tokens.get(10).getType() == TokenType.OPERATOR_LESS_THAN_OR_EQUAL : "Should have OPERATOR_LESS_THAN_OR_EQUAL";
            assert tokens.get(11).getType() == TokenType.OPERATOR_GREATER_THAN_OR_EQUAL : "Should have OPERATOR_GREATER_THAN_OR_EQUAL";
            assert tokens.get(12).getType() == TokenType.OPERATOR_LOGICAL_AND : "Should have OPERATOR_LOGICAL_AND";
            assert tokens.get(13).getType() == TokenType.OPERATOR_LOGICAL_OR : "Should have OPERATOR_LOGICAL_OR";
            assert tokens.get(14).getType() == TokenType.OPERATOR_LOGICAL_NOT : "Should have OPERATOR_LOGICAL_NOT";
            assert tokens.get(15).getType() == TokenType.OPERATOR_BITWISE_AND : "Should have OPERATOR_BITWISE_AND";
            assert tokens.get(16).getType() == TokenType.OPERATOR_BITWISE_OR : "Should have OPERATOR_BITWISE_OR";
            assert tokens.get(17).getType() == TokenType.OPERATOR_BITWISE_XOR : "Should have OPERATOR_BITWISE_XOR";
            assert tokens.get(18).getType() == TokenType.OPERATOR_BITWISE_NOT : "Should have OPERATOR_BITWISE_NOT";
            assert tokens.get(19).getType() == TokenType.OPERATOR_LEFT_SHIFT : "Should have OPERATOR_LEFT_SHIFT";
            assert tokens.get(20).getType() == TokenType.OPERATOR_RIGHT_SHIFT : "Should have OPERATOR_RIGHT_SHIFT";
            assert tokens.get(21).getType() == TokenType.OPERATOR_UNSIGNED_RIGHT_SHIFT : "Should have OPERATOR_UNSIGNED_RIGHT_SHIFT";
            assert tokens.get(22).getType() == TokenType.OPERATOR_INCREMENT : "Should have OPERATOR_INCREMENT";
            assert tokens.get(23).getType() == TokenType.OPERATOR_DECREMENT : "Should have OPERATOR_DECREMENT";
            
            System.out.println("  ✓ PASSED");
            return true;
        } catch (Exception e) {
            System.out.println("  ✗ FAILED: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    private static boolean testDelimiters() {
        System.out.println("Test 5: Delimiters");
        try {
            String source = "; , ( ) { } [ ] ->";
            Lexer lexer = new Lexer(source);
            List<Token> tokens = lexer.tokenize();
            
            assert tokens.get(0).getType() == TokenType.DELIMITER_SEMICOLON : "Should have DELIMITER_SEMICOLON";
            assert tokens.get(1).getType() == TokenType.DELIMITER_COMMA : "Should have DELIMITER_COMMA";
            assert tokens.get(2).getType() == TokenType.DELIMITER_LEFT_PAREN : "Should have DELIMITER_LEFT_PAREN";
            assert tokens.get(3).getType() == TokenType.DELIMITER_RIGHT_PAREN : "Should have DELIMITER_RIGHT_PAREN";
            assert tokens.get(4).getType() == TokenType.DELIMITER_LEFT_BRACE : "Should have DELIMITER_LEFT_BRACE";
            assert tokens.get(5).getType() == TokenType.DELIMITER_RIGHT_BRACE : "Should have DELIMITER_RIGHT_BRACE";
            assert tokens.get(6).getType() == TokenType.DELIMITER_LEFT_BRACKET : "Should have DELIMITER_LEFT_BRACKET";
            assert tokens.get(7).getType() == TokenType.DELIMITER_RIGHT_BRACKET : "Should have DELIMITER_RIGHT_BRACKET";
            assert tokens.get(8).getType() == TokenType.DELIMITER_ARROW : "Should have DELIMITER_ARROW";
            
            System.out.println("  ✓ PASSED");
            return true;
        } catch (Exception e) {
            System.out.println("  ✗ FAILED: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    private static boolean testComplexExpression() {
        System.out.println("Test 6: Complex Expression");
        try {
            String source = "int x = 42; if (x > 0) { x = x + 1; }";
            Lexer lexer = new Lexer(source);
            List<Token> tokens = lexer.tokenize();
            
            assert tokens.get(0).getType() == TokenType.KEYWORD_INT : "Should have KEYWORD_INT";
            assert tokens.get(1).getType() == TokenType.IDENTIFIER : "Should have IDENTIFIER 'x'";
            assert tokens.get(2).getType() == TokenType.OPERATOR_ASSIGN : "Should have OPERATOR_ASSIGN";
            assert tokens.get(3).getType() == TokenType.LITERAL_INTEGER : "Should have LITERAL_INTEGER 42";
            assert tokens.get(4).getType() == TokenType.DELIMITER_SEMICOLON : "Should have SEMICOLON";
            assert tokens.get(5).getType() == TokenType.KEYWORD_IF : "Should have KEYWORD_IF";
            assert tokens.get(6).getType() == TokenType.DELIMITER_LEFT_PAREN : "Should have LEFT_PAREN";
            assert tokens.get(7).getType() == TokenType.IDENTIFIER : "Should have IDENTIFIER 'x'";
            assert tokens.get(8).getType() == TokenType.OPERATOR_GREATER_THAN : "Should have OPERATOR_GREATER_THAN";
            assert tokens.get(9).getType() == TokenType.LITERAL_INTEGER : "Should have LITERAL_INTEGER 0";
            assert tokens.get(10).getType() == TokenType.DELIMITER_RIGHT_PAREN : "Should have RIGHT_PAREN";
            assert tokens.get(11).getType() == TokenType.DELIMITER_LEFT_BRACE : "Should have LEFT_BRACE";
            assert tokens.get(12).getType() == TokenType.IDENTIFIER : "Should have IDENTIFIER 'x'";
            assert tokens.get(13).getType() == TokenType.OPERATOR_ASSIGN : "Should have OPERATOR_ASSIGN";
            assert tokens.get(14).getType() == TokenType.IDENTIFIER : "Should have IDENTIFIER 'x'";
            assert tokens.get(15).getType() == TokenType.OPERATOR_PLUS : "Should have OPERATOR_PLUS";
            assert tokens.get(16).getType() == TokenType.LITERAL_INTEGER : "Should have LITERAL_INTEGER 1";
            assert tokens.get(17).getType() == TokenType.DELIMITER_SEMICOLON : "Should have SEMICOLON";
            assert tokens.get(18).getType() == TokenType.DELIMITER_RIGHT_BRACE : "Should have RIGHT_BRACE";
            
            System.out.println("  ✓ PASSED");
            return true;
        } catch (Exception e) {
            System.out.println("  ✗ FAILED: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
}
