package com.justnothing.engine.v2.resolver;

import com.justnothing.engine.v2.ast.ASTNode;
import com.justnothing.engine.v2.ast.DefaultASTVisitor;
import com.justnothing.engine.v2.ast.ResolverAccess;
import com.justnothing.engine.v2.ast.decl.ClassDecl;
import com.justnothing.engine.v2.ast.decl.ConstructorDecl;
import com.justnothing.engine.v2.ast.decl.FieldDecl;
import com.justnothing.engine.v2.ast.decl.MethodDecl;
import com.justnothing.engine.v2.ast.decl.ParamDecl;
import com.justnothing.engine.v2.ast.decl.VarDecl;
import com.justnothing.engine.v2.ast.expr.ClassReferenceExpr;
import com.justnothing.engine.v2.ast.expr.FieldAccessExpr;
import com.justnothing.engine.v2.ast.expr.MethodCallExpr;
import com.justnothing.engine.v2.ast.expr.VariableExpr;
import com.justnothing.engine.v2.ast.stmt.BlockStmt;
import com.justnothing.engine.v2.ast.stmt.ForEachStmt;
import com.justnothing.engine.v2.ast.stmt.ForStmt;
import com.justnothing.engine.v2.ast.stmt.TryStmt;
import com.justnothing.engine.v2.ast.stmt.TryStmt.CatchClause;
import com.justnothing.engine.v2.scope.ClassScope;
import com.justnothing.engine.v2.scope.GlobalScope;
import com.justnothing.engine.v2.scope.LocalScope;
import com.justnothing.engine.v2.scope.Scope;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

/**
 * 作用域构建 + 名称绑定。
 * <p>
 * 遍历 AST，构建 Scope 树，将 VariableExpr、MethodCallExpr 等引用绑到对应的 Decl。
 * </p>
 */
public class ScopeResolver extends DefaultASTVisitor<Void> {

    private final Deque<Scope> scopeStack = new ArrayDeque<>();
    private final GlobalScope globalScope;

    public ScopeResolver() {
        this.globalScope = new GlobalScope();
        scopeStack.push(globalScope);
    }

    public GlobalScope getGlobalScope() {
        return globalScope;
    }

    /** 对一组顶层声明执行解析 */
    public void resolve(List<ASTNode> declarations) {
        // 第一遍：登记所有顶层类
        for (ASTNode node : declarations) {
            if (node instanceof ClassDecl cd) {
                globalScope.define(cd.getName(), cd);
            }
        }
        // 第二遍：访问每个声明（构建内部作用域 + 绑定引用）
        for (ASTNode node : declarations) {
            node.accept(this);
        }
    }

    // ========== 作用域管理 ==========

    private Scope currentScope() {
        return scopeStack.peek();
    }

    private void pushScope(Scope scope) {
        scopeStack.push(scope);
    }

    private void popScope() {
        scopeStack.pop();
    }

    // ========== 声明：登记名称 ==========

    @Override
    public Void visit(ClassDecl node) {
        ClassScope cs = new ClassScope(currentScope(), node);
        pushScope(cs);

        for (ASTNode member : node.getMembers()) {
            if (member instanceof FieldDecl fd) {
                cs.define(fd.getName(), fd);
            } else if (member instanceof MethodDecl md) {
                cs.define(md.getName(), md);
            } else if (member instanceof ConstructorDecl cd) {
                cs.define(cd.getName(), cd);
            } else if (member instanceof ClassDecl inner) {
                cs.define(inner.getName(), inner);
            }
        }

        for (ASTNode member : node.getMembers()) {
            member.accept(this);
        }

        popScope();
        return null;
    }

    @Override
    public Void visit(MethodDecl node) {
        LocalScope ms = new LocalScope(currentScope());
        pushScope(ms);

        for (ParamDecl p : node.getParameters()) {
            ms.define(p.getName(), p);
        }

        if (node.getBody() != null) {
            node.getBody().accept(this);
        }
        if (node.getAnnotations() != null) {
            for (var ann : node.getAnnotations()) ann.accept(this);
        }

        popScope();
        return null;
    }

    @Override
    public Void visit(ConstructorDecl node) {
        LocalScope cs = new LocalScope(currentScope());
        pushScope(cs);

        for (ParamDecl p : node.getParameters()) {
            cs.define(p.getName(), p);
        }

        if (node.getBody() != null) {
            node.getBody().accept(this);
        }
        if (node.getAnnotations() != null) {
            for (var ann : node.getAnnotations()) ann.accept(this);
        }

        popScope();
        return null;
    }

    @Override
    public Void visit(BlockStmt node) {
        LocalScope bs = new LocalScope(currentScope());
        pushScope(bs);
        for (ASTNode stmt : node.getStatements()) {
            stmt.accept(this);
        }
        popScope();
        return null;
    }

    @Override
    public Void visit(ForStmt node) {
        LocalScope fs = new LocalScope(currentScope());
        pushScope(fs);

        if (node.getInitializer() != null) node.getInitializer().accept(this);
        if (node.getCondition() != null) node.getCondition().accept(this);
        if (node.getUpdate() != null) node.getUpdate().accept(this);
        if (node.getBody() != null) node.getBody().accept(this);

        popScope();
        return null;
    }

    @Override
    public Void visit(ForEachStmt node) {
        LocalScope fs = new LocalScope(currentScope());
        pushScope(fs);

        // 注册 foreach 变量（名字仅字符串，用自身节点作占位声明）
        fs.define(node.getVariableName(), node);

        if (node.getIterable() != null) node.getIterable().accept(this);
        if (node.getBody() != null) node.getBody().accept(this);

        popScope();
        return null;
    }

    @Override
    public Void visit(TryStmt node) {
        LocalScope ts = new LocalScope(currentScope());
        pushScope(ts);

        if (node.getResources() != null) {
            for (ASTNode resource : node.getResources()) {
                if (resource instanceof VarDecl vd) {
                    ts.define(vd.getName(), vd);
                }
                resource.accept(this);
            }
        }
        if (node.getTryBlock() != null) node.getTryBlock().accept(this);
        if (node.getCatchClauses() != null) {
            for (CatchClause cc : node.getCatchClauses()) {
                if (cc.block() != null) cc.block().accept(this);
            }
        }
        if (node.getFinallyBlock() != null) node.getFinallyBlock().accept(this);

        popScope();
        return null;
    }

    // ========== 变量声明 ==========

    @Override
    public Void visit(VarDecl node) {
        currentScope().define(node.getName(), node);
        if (node.getInitializer() != null) {
            node.getInitializer().accept(this);
        }
        return null;
    }

    // ========== 引用绑定 ==========

    @Override
    public Void visit(VariableExpr node) {
        ASTNode decl = currentScope().resolve(node.getName());
        if (decl != null) {
            ResolverAccess.setBinding(node, decl);
        }
        return null;
    }

    @Override
    public Void visit(MethodCallExpr node) {
        if (node.getTarget() != null) {
            node.getTarget().accept(this);
        } else {
            ASTNode decl = currentScope().resolve(node.getMethodName());
            if (decl != null) {
                ResolverAccess.setBinding(node, decl);
            }
        }
        for (ASTNode arg : node.getArguments()) {
            arg.accept(this);
        }
        return null;
    }

    @Override
    public Void visit(FieldAccessExpr node) {
        if (node.getTarget() != null) {
            node.getTarget().accept(this);
        }
        ASTNode decl = currentScope().resolve(node.getFieldName());
        if (decl != null) {
            ResolverAccess.setBinding(node, decl);
        }
        return null;
    }

    @Override
    public Void visit(ClassReferenceExpr node) {
        ASTNode decl = globalScope.resolve(node.getClassName());
        if (decl != null) {
            ResolverAccess.setBinding(node, decl);
        }
        return null;
    }

    // ========== 默认委托：递归遍历子节点 ==========

    @Override
    public Void defaultAction(ASTNode node) {
        for (ASTNode child : node.getChildren()) {
            child.accept(this);
        }
        return null;
    }
}
