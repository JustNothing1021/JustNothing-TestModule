package com.justnothing.testmodule.command.functions.script_new.lexer;

import com.justnothing.testmodule.command.functions.script_new.ast.SourceLocation;

public class TokenTest {
    
    public static void main(String[] args) {
        System.out.println("=== Token and TokenType Test ===\n");
        
        int totalTests = 0;
        int passedTests = 0;
        
        totalTests++;
        if (testTokenType()) {
            passedTests++;
        }
        
        totalTests++;
        if (testTokenCreation()) {
            passedTests++;
        }
        
        totalTests++;
        if (testTokenMethods()) {
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
    
    private static boolean testTokenType() {
        System.out.println("Test 1: TokenType");
        try {
            assert TokenType.KEYWORD_INT.isKeyword() : "KEYWORD_INT should be a keyword";
            assert TokenType.OPERATOR_PLUS.isOperator() : "OPERATOR_PLUS should be an operator";
            assert TokenType.DELIMITER_SEMICOLON.isDelimiter() : "DELIMITER_SEMICOLON should be a delimiter";
            assert TokenType.LITERAL_INTEGER.isLiteral() : "LITERAL_INTEGER should be a literal";
            
            assert TokenType.isKeyword("int") : "'int' should be a keyword";
            assert TokenType.isKeyword("if") : "'if' should be a keyword";
            assert !TokenType.isKeyword("variable") : "'variable' should not be a keyword";
            
            assert TokenType.getKeywordType("int") == TokenType.KEYWORD_INT : "Keyword type should be KEYWORD_INT";
            assert TokenType.getKeywordType("if") == TokenType.KEYWORD_IF : "Keyword type should be KEYWORD_IF";
            assert TokenType.getKeywordType("xyz") == TokenType.IDENTIFIER : "Non-keyword should be IDENTIFIER";
            
            System.out.println("  ✓ PASSED");
            return true;
        } catch (Exception e) {
            System.out.println("  ✗ FAILED: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    private static boolean testTokenCreation() {
        System.out.println("Test 2: Token Creation");
        try {
            SourceLocation loc = new SourceLocation(1, 1);
            
            Token token1 = new Token(TokenType.LITERAL_INTEGER, "42", 42, loc);
            assert token1.getType() == TokenType.LITERAL_INTEGER : "Token type should be LITERAL_INTEGER";
            assert token1.getText().equals("42") : "Token text should be '42'";
            assert token1.getValue().equals(42) : "Token value should be 42";
            
            Token token2 = new Token(TokenType.IDENTIFIER, "x", loc);
            assert token2.getType() == TokenType.IDENTIFIER : "Token type should be IDENTIFIER";
            assert token2.getText().equals("x") : "Token text should be 'x'";
            
            Token token3 = new Token(TokenType.OPERATOR_PLUS, loc);
            assert token3.getType() == TokenType.OPERATOR_PLUS : "Token type should be OPERATOR_PLUS";
            assert token3.getText().equals("+") : "Token text should be '+'";
            
            System.out.println("  ✓ PASSED");
            return true;
        } catch (Exception e) {
            System.out.println("  ✗ FAILED: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    private static boolean testTokenMethods() {
        System.out.println("Test 3: Token Methods");
        try {
            SourceLocation loc = new SourceLocation(1, 1);
            
            Token keywordToken = new Token(TokenType.KEYWORD_INT, loc);
            assert keywordToken.isKeyword() : "Should be a keyword";
            assert !keywordToken.isOperator() : "Should not be an operator";
            
            Token operatorToken = new Token(TokenType.OPERATOR_PLUS, loc);
            assert operatorToken.isOperator() : "Should be an operator";
            assert !operatorToken.isKeyword() : "Should not be a keyword";
            
            Token literalToken = new Token(TokenType.LITERAL_INTEGER, loc);
            assert literalToken.isLiteral() : "Should be a literal";
            assert !literalToken.isKeyword() : "Should not be a keyword";
            
            Token eofToken = new Token(TokenType.EOF, loc);
            assert eofToken.isEOF() : "Should be EOF";
            
            assert keywordToken.is(TokenType.KEYWORD_INT) : "Should match KEYWORD_INT";
            assert !keywordToken.is(TokenType.OPERATOR_PLUS) : "Should not match OPERATOR_PLUS";
            
            String tokenStr = keywordToken.toString();
            assert tokenStr.contains("KEYWORD_INT") : "String representation should contain type";
            assert tokenStr.contains("line=1") : "String representation should contain line number";
            
            System.out.println("  ✓ PASSED");
            return true;
        } catch (Exception e) {
            System.out.println("  ✗ FAILED: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
}
