package com.justnothing.engine.v2.ast;

import com.justnothing.engine.v2.ast.decl.*;
import com.justnothing.engine.v2.ast.expr.*;
import com.justnothing.engine.v2.ast.stmt.*;

/**
 * 默认 AST 访问者适配器。
 * <p>
 * 所有 visit 方法提供默认实现，委托给 {@link #defaultAction(ASTNode)}，
 * 子类只需覆盖感兴趣的节点类型。
 * </p>
 *
 * @param <T> 访问者返回类型
 * @author JustNothing1021
 */
public class DefaultASTVisitor<T> implements ASTVisitor<T> {



    /**
     * 默认动作，所有未覆盖的 visit 方法均委托给此方法。
     * 默认返回 null。
     */
    public T defaultAction(ASTNode node) {
        return null;
    }

    // ========== 表达式 ==========

    @Override public T visit(LiteralExpr node) { return defaultAction(node); }
    @Override public T visit(VariableExpr node) { return defaultAction(node); }
    @Override public T visit(BinaryOpExpr node) { return defaultAction(node); }
    @Override public T visit(UnaryOpExpr node) { return defaultAction(node); }
    @Override public T visit(TernaryExpr node) { return defaultAction(node); }
    @Override public T visit(MethodCallExpr node) { return defaultAction(node); }
    @Override public T visit(FieldAccessExpr node) { return defaultAction(node); }
    @Override public T visit(AssignmentExpr node) { return defaultAction(node); }
    @Override public T visit(CastExpr node) { return defaultAction(node); }
    @Override public T visit(LambdaExpr node) { return defaultAction(node); }
    @Override public T visit(MethodReferenceExpr node) { return defaultAction(node); }
    @Override public T visit(NewExpr node) { return defaultAction(node); }
    @Override public T visit(AsyncExpr node) { return defaultAction(node); }
    @Override public T visit(AwaitExpr node) { return defaultAction(node); }
    @Override public T visit(ArrayAccessExpr node) { return defaultAction(node); }
    @Override public T visit(ArrayLiteralExpr node) { return defaultAction(node); }
    @Override public T visit(MapLiteralExpr node) { return defaultAction(node); }
    @Override public T visit(InterpolatedStringExpr node) { return defaultAction(node); }
    @Override public T visit(ClassReferenceExpr node) { return defaultAction(node); }
    @Override public T visit(InstanceofExpr node) { return defaultAction(node); }
    @Override public T visit(SafeFieldAccessExpr node) { return defaultAction(node); }
    @Override public T visit(SafeMethodCallExpr node) { return defaultAction(node); }
    @Override public T visit(NewArrayExpr node) { return defaultAction(node); }
    @Override public T visit(SuperExpr node) { return defaultAction(node); }
    @Override public T visit(SuperConstructorCallExpr node) { return defaultAction(node); }

    // ========== 语句 ==========

    @Override public T visit(BlockStmt node) { return defaultAction(node); }
    @Override public T visit(ExprStmt node) { return defaultAction(node); }
    @Override public T visit(IfStmt node) { return defaultAction(node); }
    @Override public T visit(WhileStmt node) { return defaultAction(node); }
    @Override public T visit(DoWhileStmt node) { return defaultAction(node); }
    @Override public T visit(ForStmt node) { return defaultAction(node); }
    @Override public T visit(ForEachStmt node) { return defaultAction(node); }
    @Override public T visit(SwitchStmt node) { return defaultAction(node); }
    @Override public T visit(SwitchCase node) { return defaultAction(node); }
    @Override public T visit(TryStmt node) { return defaultAction(node); }
    @Override public T visit(ThrowStmt node) { return defaultAction(node); }
    @Override public T visit(AssertStmt node) { return defaultAction(node); }
    @Override public T visit(ReturnStmt node) { return defaultAction(node); }
    @Override public T visit(YieldStmt node) { return defaultAction(node); }
    @Override public T visit(BreakStmt node) { return defaultAction(node); }
    @Override public T visit(ContinueStmt node) { return defaultAction(node); }
    @Override public T visit(SynchronizedStmt node) { return defaultAction(node); }

    @Override public T visit(LabeledStmt node) { return defaultAction(node); }

    // ========== 声明 ==========

    @Override public T visit(VarDecl node) { return defaultAction(node); }
    @Override public T visit(ImportDecl node) { return defaultAction(node); }
    @Override public T visit(ClassDecl node) { return defaultAction(node); }
    @Override public T visit(MethodDecl node) { return defaultAction(node); }
    @Override public T visit(ConstructorDecl node) { return defaultAction(node); }
    @Override public T visit(FieldDecl node) { return defaultAction(node); }
    @Override public T visit(ParamDecl node) { return defaultAction(node); }
    @Override public T visit(AnnotationVal node) { return defaultAction(node); }
    @Override public T visit(StaticInitDecl node) { return defaultAction(node); }
    @Override public T visit(InstanceInitDecl node) { return defaultAction(node); }
}
