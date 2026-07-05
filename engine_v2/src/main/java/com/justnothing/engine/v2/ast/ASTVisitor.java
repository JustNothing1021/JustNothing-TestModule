package com.justnothing.engine.v2.ast;

import com.justnothing.engine.v2.ast.decl.*;
import com.justnothing.engine.v2.ast.expr.*;
import com.justnothing.engine.v2.ast.stmt.*;

/**
 * AST 访问者接口。
 * <p>
 * 通过双分派模式实现对不同 AST 节点类型的分派处理。
 * 所有 visit 方法均为抽象方法，实现类必须覆盖每一个。
 * </p>
 * <p>
 * 如只需覆盖部分节点，可继承 {@link DefaultASTVisitor}。
 * </p>
 *
 * @param <T> 访问者返回类型
 * @author JustNothing1021
 */
public interface ASTVisitor<T> {

    // ========== 表达式 ==========

    T visit(LiteralExpr node);

    T visit(VariableExpr node);

    T visit(AsyncExpr node);

    T visit(AwaitExpr node);

    T visit(BinaryOpExpr node);

    T visit(UnaryOpExpr node);

    T visit(TernaryExpr node);

    T visit(MethodCallExpr node);

    T visit(FieldAccessExpr node);

    T visit(AssignmentExpr node);

    T visit(CastExpr node);

    T visit(LambdaExpr node);

    T visit(MethodReferenceExpr node);

    T visit(NewExpr node);

    T visit(ArrayAccessExpr node);

    T visit(ArrayLiteralExpr node);

    T visit(MapLiteralExpr node);

    T visit(InterpolatedStringExpr node);

    T visit(ClassReferenceExpr node);

    T visit(InstanceofExpr node);

    T visit(SafeFieldAccessExpr node);

    T visit(SafeMethodCallExpr node);

    T visit(NewArrayExpr node);

    T visit(SuperExpr node);

    T visit(SuperConstructorCallExpr node);

    // ========== 语句 ==========

    T visit(BlockStmt node);

    T visit(ExprStmt node);

    T visit(IfStmt node);

    T visit(WhileStmt node);

    T visit(DoWhileStmt node);

    T visit(ForStmt node);

    T visit(ForEachStmt node);

    T visit(SwitchStmt node);

    T visit(SwitchCase node);

    T visit(TryStmt node);

    T visit(ThrowStmt node);

    T visit(AssertStmt node);

    T visit(ReturnStmt node);

    T visit(YieldStmt node);

    T visit(BreakStmt node);

    T visit(ContinueStmt node);

    T visit(SynchronizedStmt node);

    T visit(LabeledStmt node);

    // ========== 声明 ==========

    T visit(VarDecl node);

    T visit(ImportDecl node);

    T visit(ClassDecl node);

    T visit(MethodDecl node);

    T visit(ConstructorDecl node);

    T visit(FieldDecl node);

    T visit(ParamDecl node);

    T visit(AnnotationVal node);

    T visit(StaticInitDecl node);

    T visit(InstanceInitDecl node);
}
