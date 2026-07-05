package com.justnothing.engine.v2.resolver;

import com.justnothing.engine.v2.ast.ASTNode;
import com.justnothing.engine.v2.ast.decl.ClassDecl;
import com.justnothing.engine.v2.ast.decl.FieldDecl;
import com.justnothing.engine.v2.ast.decl.MethodDecl;
import com.justnothing.engine.v2.ast.decl.ParamDecl;
import com.justnothing.engine.v2.ast.decl.VarDecl;
import com.justnothing.engine.v2.ast.expr.*;
import com.justnothing.engine.v2.ast.stmt.*;
import com.justnothing.engine.v2.parser.Parser;

import org.junit.Test;
import java.util.List;

import static org.junit.Assert.*;

public class ScopeResolverTest {

    private List<ASTNode> parse(String source) {
        Parser parser = new Parser(source, "test");
        List<ASTNode> decls = parser.parseProgram();
        assertFalse("parse error: " + parser.getErrors(), parser.hasErrors());
        return decls;
    }

    @Test
    public void variableBindsToLocalVarDecl() {
        var decls = parse("class A { void test() { int x = 1; System.out.println(x); } }");
        ScopeResolver resolver = new ScopeResolver();
        resolver.resolve(decls);

        ClassDecl cd = (ClassDecl) decls.get(0);
        MethodDecl md = (MethodDecl) cd.getMembers().get(0);
        BlockStmt body = (BlockStmt) md.getBody();
        ExprStmt exprStmt = (ExprStmt) body.getStatements().get(1);
        MethodCallExpr call = (MethodCallExpr) exprStmt.getExpression();
        VariableExpr arg = (VariableExpr) call.getArguments().get(0);

        assertNotNull("x should be bound", arg.getBinding());
        assertTrue("bound to VarDecl", arg.getBinding() instanceof VarDecl);
        assertEquals("x", ((VarDecl) arg.getBinding()).getName());
    }

    @Test
    public void variableBindsToParam() {
        var decls = parse("class A { void test(int x) { int y = x; } }");
        ScopeResolver resolver = new ScopeResolver();
        resolver.resolve(decls);

        ClassDecl cd = (ClassDecl) decls.get(0);
        MethodDecl md = (MethodDecl) cd.getMembers().get(0);
        BlockStmt body = (BlockStmt) md.getBody();
        VarDecl vd = (VarDecl) body.getStatements().get(0);
        VariableExpr init = (VariableExpr) vd.getInitializer();

        assertNotNull("x should bind to param", init.getBinding());
        assertTrue("bound to ParamDecl", init.getBinding() instanceof ParamDecl);
        assertEquals("x", ((ParamDecl) init.getBinding()).getName());
    }

    @Test
    public void fieldAccessBindsToFieldDecl() {
        var decls = parse("class A { int field; void test() { this.field = 1; } }");
        ScopeResolver resolver = new ScopeResolver();
        resolver.resolve(decls);

        ClassDecl cd = (ClassDecl) decls.get(0);
        MethodDecl md = (MethodDecl) cd.getMembers().get(1);
        BlockStmt body = (BlockStmt) md.getBody();
        ExprStmt exprStmt = (ExprStmt) body.getStatements().get(0);
        AssignmentExpr assign = (AssignmentExpr) exprStmt.getExpression();

        assertTrue("target is FieldAccess", assign.getTarget() instanceof FieldAccessExpr);
        FieldAccessExpr fa = (FieldAccessExpr) assign.getTarget();
        assertNotNull("field should bind", fa.getBinding());
        assertTrue("bound to FieldDecl", fa.getBinding() instanceof FieldDecl);
        assertEquals("field", ((FieldDecl) fa.getBinding()).getName());
    }

    @Test
    public void classRefBindsToClassDecl() {
        var decls = parse("class A { void m() { Object c = A.class; } }");
        ScopeResolver resolver = new ScopeResolver();
        resolver.resolve(decls);

        ClassDecl cd = (ClassDecl) decls.get(0);
        MethodDecl md = (MethodDecl) cd.getMembers().get(0);
        BlockStmt body = (BlockStmt) md.getBody();
        VarDecl vd = (VarDecl) body.getStatements().get(0);
        ClassReferenceExpr init = (ClassReferenceExpr) vd.getInitializer();

        assertNotNull("A.class should bind", init.getBinding());
        assertTrue("bound to ClassDecl", init.getBinding() instanceof ClassDecl);
        assertEquals("A", ((ClassDecl) init.getBinding()).getName());
    }
}
