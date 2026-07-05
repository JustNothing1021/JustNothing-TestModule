package com.justnothing.engine.v2.parser;

import com.justnothing.engine.v2.ast.ASTNode;
import com.justnothing.engine.v2.ast.decl.VarDecl;
import com.justnothing.engine.v2.ast.expr.*;
import com.justnothing.engine.v2.ast.stmt.*;
import org.junit.Test;

import static org.junit.Assert.*;


/**
 * Parser 综合测试。
 *
 * @author JustNothing1021
 */
public class ParserTest {

    // ========== 辅助方法 ==========

    private ASTNode parseExpr(String source) {
        Parser parser = new Parser(source, "<test>");
        if (parser.hasErrors()) {
            fail("Lexer/Parser 错误: " + parser.getErrors());
        }
        ASTNode result = parser.parseExpression();
        assertNotNull("解析结果不应为 null: " + source, result);
        return result;
    }

    private ASTNode parseStmt(String source) {
        Parser parser = new Parser(source, "<test>");
        if (parser.hasErrors()) {
            fail("Lexer/Parser 错误: " + parser.getErrors());
        }
        ASTNode result = parser.parseStatement();
        assertNotNull("解析结果不应为 null: " + source, result);
        return result;
    }

    // ========== 字面量 ==========

    @Test
    public void literalInteger() {
        Parser parser = new Parser("42", "<test>");
        if (parser.hasErrors()) {
            fail("Lexer 错误: " + parser.getErrors());
        }
        ASTNode node = parser.parseExpression();
        assertNotNull("解析结果不应为 null", node);
        assertTrue("应为 LiteralExpr，实际为 " + node.nodeName(), node instanceof LiteralExpr);
        // Lexer 将 int 范围内的整数存为 Integer
        assertEquals(42, ((LiteralExpr) node).getValue());
    }

    @Test
    public void literalString() {
        ASTNode node = parseExpr("\"hello\"");
        assertTrue(node instanceof LiteralExpr);
        assertEquals("hello", ((LiteralExpr) node).getValue());
    }

    @Test
    public void literalDecimal() {
        ASTNode node = parseExpr("3.14");
        assertTrue(node instanceof LiteralExpr);
        assertEquals(3.14, ((LiteralExpr) node).getValue());
    }

    @Test
    public void literalBoolean() {
        ASTNode node = parseExpr("true");
        assertTrue(node instanceof LiteralExpr);
        assertEquals(true, ((LiteralExpr) node).getValue());
    }

    @Test
    public void literalNull() {
        ASTNode node = parseExpr("null");
        assertTrue(node instanceof LiteralExpr);
        assertNull(((LiteralExpr) node).getValue());
    }

    // ========== 变量引用 ==========

    @Test
    public void variableReference() {
        ASTNode node = parseExpr("x");
        assertTrue(node instanceof VariableExpr);
        assertEquals("x", ((VariableExpr) node).getName());
    }

    // ========== 二元运算 ==========

    @Test
    public void binaryAdd() throws Exception {
        // 1 + 2 会被常量折叠为 LiteralExpr(3)
        ASTNode node = parseExpr("1 + 2");
        assertTrue("常量折叠: 1+2 → LiteralExpr(3)", node instanceof LiteralExpr);
        assertEquals(3, ((LiteralExpr) node).getValue());

        // 非常量表达式不会被折叠
        ASTNode node2 = parseExpr("x + 2");
        assertTrue(node2 instanceof BinaryOpExpr);
        assertEquals("+", ((BinaryOpExpr) node2).getOperator());
    }

    @Test
    public void binaryPrecedence() {
        // x + 2 * 3 → x + (2 * 3)，2*3 会被折叠
        ASTNode node = parseExpr("x + 2 * 3");
        assertTrue(node instanceof BinaryOpExpr);
        BinaryOpExpr add = (BinaryOpExpr) node;
        assertEquals("+", add.getOperator());
        // 右边 2*3 被折叠为 LiteralExpr(6)
        assertTrue(add.getRight() instanceof LiteralExpr);
        assertEquals(6, ((LiteralExpr) add.getRight()).getValue());
    }

    @Test
    public void binaryParenthesized() {
        // (x + 2) * 3
        ASTNode node = parseExpr("(x + 2) * 3");
        assertTrue(node instanceof BinaryOpExpr);
        BinaryOpExpr mul = (BinaryOpExpr) node;
        assertEquals("*", mul.getOperator());
        assertTrue(mul.getLeft() instanceof BinaryOpExpr);
        assertEquals("+", ((BinaryOpExpr) mul.getLeft()).getOperator());
    }

    // ========== 一元运算 ==========

    @Test
    public void unaryMinus() {
        ASTNode node = parseExpr("-x");
        assertTrue(node instanceof UnaryOpExpr);
        UnaryOpExpr unary = (UnaryOpExpr) node;
        assertEquals("-", unary.getOperator());
        assertTrue(unary.isPrefix());
        assertTrue(unary.getOperand() instanceof VariableExpr);
    }

    // ========== 三元运算 ==========

    @Test
    public void ternary() {
        ASTNode node = parseExpr("x ? 1 : 2");
        assertTrue(node instanceof TernaryExpr);
        TernaryExpr ternary = (TernaryExpr) node;
        assertTrue(ternary.getCondition() instanceof VariableExpr);
        assertTrue(ternary.getTrueExpr() instanceof LiteralExpr);
        assertTrue(ternary.getFalseExpr() instanceof LiteralExpr);
    }

    // ========== 方法调用 ==========

    @Test
    public void methodCall() {
        // obj.method(1, 2) — Lexer 把 obj.method 合并为 QUALIFIED_NAME
        // VariableExpr("obj.method") 后跟 (1, 2)，isCallTarget 生成 MethodCallExpr
        // 方法名是 "obj.method"（需要 Parser 层消歧来拆分 target/method）
        ASTNode node = parseExpr("obj.method(1, 2)");
        assertTrue(node instanceof MethodCallExpr);
        MethodCallExpr call = (MethodCallExpr) node;
        // 当前行为：方法名是 "obj.method"（限定名未消歧）
        assertEquals("obj.method", call.getMethodName());
        assertEquals(2, call.getArguments().size());
    }

    @Test
    public void staticMethodCall() {
        // Math.abs(-1) — Lexer 把 Math.abs 合并为 QUALIFIED_NAME
        ASTNode node = parseExpr("Math.abs(-1)");
        assertTrue(node instanceof MethodCallExpr);
        MethodCallExpr call = (MethodCallExpr) node;
        assertEquals("Math.abs", call.getMethodName());
    }

    // ========== 字段访问 ==========

    @Test
    public void fieldAccess() {
        // obj.field — Lexer 把 obj.field 合并为 QUALIFIED_NAME
        // 结果是 VariableExpr("obj.field")，不是 FieldAccessExpr
        // 这需要 Parser 层消歧，目前暂不处理
        ASTNode node = parseExpr("obj.field");
        assertTrue(node instanceof VariableExpr);
        assertEquals("obj.field", ((VariableExpr) node).getName());
    }

    // ========== 安全访问 ==========

    @Test
    public void safeFieldAccess() {
        ASTNode node = parseExpr("obj?.field");
        assertTrue("应解析为 SafeFieldAccessExpr", node instanceof SafeFieldAccessExpr);
        SafeFieldAccessExpr sfa = (SafeFieldAccessExpr) node;
        assertEquals("field", sfa.getFieldName());
        assertTrue(sfa.getTarget() instanceof VariableExpr);
    }

    @Test
    public void safeMethodCall() {
        ASTNode node = parseExpr("obj?.method(1)");
        assertTrue("应解析为 SafeMethodCallExpr", node instanceof SafeMethodCallExpr);
        SafeMethodCallExpr smc = (SafeMethodCallExpr) node;
        assertEquals("method", smc.getMethodName());
        assertEquals(1, smc.getArguments().size());
    }

    // ========== new 表达式 ==========

    @Test
    public void newObject() {
        ASTNode node = parseExpr("new String(\"hello\")");
        assertTrue(node instanceof NewExpr);
        NewExpr newExpr = (NewExpr) node;
        assertEquals("String", newExpr.getClassName());
        assertEquals(1, newExpr.getArguments().size());
    }

    @Test
    public void newArray() {
        ASTNode node = parseExpr("new int[5]");
        assertTrue("应解析为 NewArrayExpr", node instanceof NewArrayExpr);
        NewArrayExpr na = (NewArrayExpr) node;
        assertEquals("int", na.getElementTypeName());
        assertEquals(1, na.getDimensions().size());
        assertFalse(na.hasInitializer());
    }

    @Test
    public void newArrayWithInitializer() {
        ASTNode node = parseExpr("new int[]{1, 2, 3}");
        assertTrue(node instanceof NewArrayExpr);
        NewArrayExpr na = (NewArrayExpr) node;
        assertTrue(na.hasInitializer());
        assertEquals(3, na.getInitializer().size());
    }

    // ========== super ==========

    @Test
    public void superExpr() {
        ASTNode node = parseExpr("super");
        assertTrue("应解析为 SuperExpr", node instanceof SuperExpr);
    }

    // ========== 赋值 ==========

    @Test
    public void assignment() {
        ASTNode node = parseExpr("x = 10");
        assertTrue(node instanceof AssignmentExpr);
        AssignmentExpr assign = (AssignmentExpr) node;
        assertEquals("=", assign.getOperator());
        assertTrue(assign.getTarget() instanceof VariableExpr);
        assertTrue(assign.getValue() instanceof LiteralExpr);
    }

    // ========== instanceof ==========

    @Test
    public void instanceofExpr() {
        ASTNode node = parseExpr("x instanceof String");
        assertTrue(node instanceof InstanceofExpr);
        InstanceofExpr inst = (InstanceofExpr) node;
        assertEquals("String", inst.getTypeName());
    }

    // ========== Lambda ==========

    @Test
    public void lambdaExpression() {
        ASTNode node = parseExpr("(x, y) -> x + y");
        assertTrue(node instanceof LambdaExpr);
        LambdaExpr lambda = (LambdaExpr) node;
        assertEquals(2, lambda.getParameters().size());
    }

    // ========== 方法引用 ==========

    @Test
    public void methodReference() {
        ASTNode node = parseExpr("String::valueOf");
        assertTrue(node instanceof MethodReferenceExpr);
        MethodReferenceExpr ref = (MethodReferenceExpr) node;
        assertEquals("valueOf", ref.getMethodName());
    }

    // ========== switch 表达式 ==========

    @Test
    public void switchExprSimple() {
        ASTNode node = parseExpr("switch (x) { case 1 -> 10; case 2 -> 20; default -> 0; }");
        assertTrue(node instanceof SwitchStmt);
        assertEquals(3, ((SwitchStmt) node).getCases().size());
    }

    @Test
    public void switchExprBlockArm() {
        ASTNode node = parseExpr("switch (x) { case 1 -> { int y = 10; y; } default -> 0; }");
        assertTrue(node instanceof SwitchStmt);
        assertEquals(2, ((SwitchStmt) node).getCases().size());
    }

    @Test
    public void switchExprMultiValue() {
        ASTNode node = parseExpr("switch (s) { case \"a\", \"b\" -> 1; default -> 2; }");
        assertTrue(node instanceof SwitchStmt);
        assertEquals(2, ((SwitchStmt) node).getCases().size());
    }

    @Test
    public void switchExprAsVarInitializer() {
        // 模拟 String result = switch(...) { ... };
        ASTNode node = parseStmt("String result = switch (x) { case 1 -> 10; case 2 -> 20; default -> 0; };");
        assertTrue(node instanceof VarDecl);
        ASTNode init = ((VarDecl) node).getInitializer();
        assertTrue("init should be SwitchStmt, was: " + init.nodeName(), init instanceof SwitchStmt);
    }

    @Test
    public void switchExprAsVarWithBlockArms() {
        ASTNode node = parseStmt(
            "String result = switch (x) { case 1 -> { int y = 10; y; } default -> 0; };");
        assertTrue(node instanceof VarDecl);
        ASTNode init = ((VarDecl) node).getInitializer();
        assertTrue(init instanceof SwitchStmt);
    }

    @Test
    public void switchExprWithYieldAndMethodCall() {
        // 精准覆盖 AstRepl 场景：switch arm 内含 var decl + yield method call
        ASTNode node = parseStmt(
            "String result = switch (mode) { " +
            "case 1 -> { ASTNode n = parse(source); yield format(n, style); } " +
            "case 2 -> { ASTNode n = parse2(src); yield format2(n, s); } " +
            "};");
        assertTrue(node instanceof VarDecl);
        ASTNode init = ((VarDecl) node).getInitializer();
        assertTrue(init instanceof SwitchStmt);
        assertEquals(2, ((SwitchStmt) init).getCases().size());
    }

    // ========== 数组访问 ==========

    @Test
    public void arrayAccess() {
        ASTNode node = parseExpr("arr[0]");
        assertTrue(node instanceof ArrayAccessExpr);
        ArrayAccessExpr aa = (ArrayAccessExpr) node;
        assertTrue(aa.getTarget() instanceof VariableExpr);
    }

    // ========== 语句 ==========

    @Test
    public void ifStatement() {
        ASTNode node = parseStmt("if (x > 0) { x = x - 1 }");
        assertTrue(node instanceof IfStmt);
        IfStmt ifStmt = (IfStmt) node;
        assertTrue(ifStmt.getCondition() instanceof BinaryOpExpr);
        assertTrue(ifStmt.getThenBlock() instanceof BlockStmt);
        assertFalse(ifStmt.hasElse());
    }

    @Test
    public void ifElseStatement() {
        ASTNode node = parseStmt("if (x > 0) { x } else { y }");
        assertTrue(node instanceof IfStmt);
        IfStmt ifStmt = (IfStmt) node;
        assertTrue(ifStmt.hasElse());
    }

    @Test
    public void whileStatement() {
        ASTNode node = parseStmt("while (x > 0) { x = x - 1 }");
        assertTrue(node instanceof WhileStmt);
    }

    @Test
    public void forStatement() {
        ASTNode node = parseStmt("for (i = 0; i < 10; i = i + 1) { x }");
        assertTrue(node instanceof ForStmt);
        ForStmt forStmt = (ForStmt) node;
        assertTrue(forStmt.getInitializer() instanceof AssignmentExpr);
        assertTrue(forStmt.getCondition() instanceof BinaryOpExpr);
    }

    @Test
    public void forEachStatement() {
        ASTNode node = parseStmt("for (int item : list) { item }");
        assertTrue("应解析为 ForEachStmt", node instanceof ForEachStmt);
        ForEachStmt forEach = (ForEachStmt) node;
        assertEquals("item", forEach.getVariableName());
    }

    @Test
    public void returnStatement() {
        ASTNode node = parseStmt("return 42;");
        assertTrue(node instanceof ReturnStmt);
        ReturnStmt ret = (ReturnStmt) node;
        assertTrue(ret.getValue() instanceof LiteralExpr);
    }

    @Test
    public void returnVoid() {
        ASTNode node = parseStmt("return;");
        assertTrue(node instanceof ReturnStmt);
        assertNull(((ReturnStmt) node).getValue());
    }

    @Test
    public void tryCatchFinally() {
        ASTNode node = parseStmt("try { x } catch (Exception e) { y } finally { z }");
        assertTrue(node instanceof TryStmt);
        TryStmt tryStmt = (TryStmt) node;
        assertEquals(1, tryStmt.getCatchClauses().size());
        assertNotNull(tryStmt.getFinallyBlock());
    }

    // ========== 链式调用 ==========

    @Test
    public void chainedMethodCalls() {
        ASTNode node = parseExpr("a.b().c().d()");
        assertTrue(node instanceof MethodCallExpr);
        MethodCallExpr outer = (MethodCallExpr) node;
        assertEquals("d", outer.getMethodName());
        assertTrue(outer.getTarget() instanceof MethodCallExpr);
        assertEquals("c", ((MethodCallExpr) outer.getTarget()).getMethodName());
    }

    @Test
    public void mixedFieldAndMethod() {
        // obj.list.size() — Lexer 把 obj.list.size 合并为 QUALIFIED_NAME
        // 后面跟 ()，isCallTarget 生成 MethodCallExpr
        ASTNode node = parseExpr("obj.list.size()");
        assertTrue(node instanceof MethodCallExpr);
        MethodCallExpr call = (MethodCallExpr) node;
        assertEquals("obj.list.size", call.getMethodName());
    }

    // ========== 复合赋值 ==========

    @Test
    public void compoundAssignment() {
        ASTNode node = parseExpr("x += 1");
        assertTrue(node instanceof AssignmentExpr);
        AssignmentExpr assign = (AssignmentExpr) node;
        assertEquals("+=", assign.getOperator());
    }
}
