package com.justnothing.testmodule.command.output;

import com.justnothing.javainterpreter.ScriptRunner;
import com.justnothing.javainterpreter.ast.ASTNode;
import com.justnothing.javainterpreter.ast.nodes.BlockNode;
import com.justnothing.javainterpreter.evaluator.ASTEvaluator;
import com.justnothing.javainterpreter.evaluator.ExecutionContext;
import com.justnothing.javainterpreter.exception.EvaluationException;
import com.justnothing.javainterpreter.security.BasicPermissionChecker;
import com.justnothing.javainterpreter.security.IPermissionChecker;
import com.justnothing.javainterpreter.security.SandboxGuard;
import com.justnothing.javainterpreter.security.SandboxConfig;
import com.justnothing.testmodule.utils.sandbox.BlockGuardSandbox;

import java.util.List;

public class CodeVerifier {

    private static final IPermissionChecker safeExpressionChecker = BasicPermissionChecker.createExpressionOnly();
    private static final IPermissionChecker sandboxChecker = BasicPermissionChecker.createSandbox();
    private static final IPermissionChecker restrictiveChecker = IPermissionChecker.getRestrictive();

    private CodeVerifier() {
    }

    public static Object evaluateSafely(String expression) throws Exception {
        return evaluateWithPermission(expression, safeExpressionChecker);
    }

    public static Object evaluateInSandbox(String expression) throws Exception {
        return evaluateWithPermission(expression, sandboxChecker);
    }

    public static Object evaluateInJail(String expression) throws Exception {
        return evaluateWithPermission(expression, restrictiveChecker);
    }

    public static Object evaluateWithBlockGuard(String expression) throws Exception {
        return BlockGuardSandbox.executeSandboxed(() -> evaluateWithPermission(expression, sandboxChecker));
    }

    public static Object evaluateWithBlockGuard(String expression, SandboxConfig config) throws Exception {
        return BlockGuardSandbox.execute(config, () -> evaluateWithPermission(expression, sandboxChecker));
    }

    public static Object evaluateExpressionWithBlockGuard(String expression) throws Exception {
        return BlockGuardSandbox.executeExpressionOnly(() -> evaluateWithPermission(expression, safeExpressionChecker));
    }

    public static Object evaluateWithExecutor(String expression) throws Exception {
        return SandboxGuard.executeRestricted(() -> evaluateWithPermission(expression, sandboxChecker));
    }

    public static Object evaluateWithExecutor(String expression, IPermissionChecker checker) throws Exception {
        return SandboxGuard.executeWithPermission(() -> evaluateWithPermission(expression, checker), checker);
    }

    public static Object evaluateWithPermission(String expression, IPermissionChecker permissionChecker) throws Exception {
        ScriptRunner runner = new ScriptRunner();
        List<ASTNode> nodes = runner.tryParse(expression);

        if (nodes == null || nodes.isEmpty()) {
            throw new EvaluationException("表达式为空或无法解析", null, null);
        }

        ExecutionContext context = runner.getExecutionContext();
        context.setPermissionChecker(permissionChecker);

        Object result = null;

        for (ASTNode node : nodes) {
            if (node instanceof BlockNode) {
                BlockNode block = (BlockNode) node;
                for (ASTNode stmt : block.getStatements()) {
                    result = ASTEvaluator.evaluate(stmt, context);
                }
            } else {
                result = ASTEvaluator.evaluate(node, context);
            }
        }

        return result;
    }

    public static boolean isExpressionSafe(String expression) {
        try {
            evaluateSafely(expression);
            return true;
        } catch (SecurityException e) {
            return false;
        } catch (Exception e) {
            return true;
        }
    }

    public static Object evaluateWithCustomContext(String expression, ExecutionContext context) throws Exception {
        ScriptRunner runner = new ScriptRunner();
        List<ASTNode> nodes = runner.tryParse(expression);

        if (nodes == null || nodes.isEmpty()) {
            throw new EvaluationException("表达式为空或无法解析", null, null);
        }

        if (context.getPermissionChecker() == null) {
            context.setPermissionChecker(safeExpressionChecker);
        }

        Object result = null;

        for (ASTNode node : nodes) {
            if (node instanceof BlockNode) {
                BlockNode block = (BlockNode) node;
                for (ASTNode stmt : block.getStatements()) {
                    result = ASTEvaluator.evaluate(stmt, context);
                }
            } else {
                result = ASTEvaluator.evaluate(node, context);
            }
        }

        return result;
    }

    public static IPermissionChecker getSafeExpressionChecker() {
        return safeExpressionChecker;
    }

    public static IPermissionChecker getSandboxChecker() {
        return sandboxChecker;
    }

    public static IPermissionChecker getRestrictiveChecker() {
        return restrictiveChecker;
    }
}
