package com.justnothing.javainterpreter.evaluator;

import com.justnothing.javainterpreter.ast.ASTNode;
import com.justnothing.javainterpreter.ast.nodes.ArrayAccessNode;
import com.justnothing.javainterpreter.ast.nodes.ArrayAssignmentNode;
import com.justnothing.javainterpreter.ast.nodes.ArrayLiteralNode;
import com.justnothing.javainterpreter.ast.nodes.AssignmentNode;
import com.justnothing.javainterpreter.ast.nodes.AsyncNode;
import com.justnothing.javainterpreter.ast.nodes.AwaitNode;
import com.justnothing.javainterpreter.ast.nodes.BinaryOpNode;
import com.justnothing.javainterpreter.ast.nodes.BlockNode;
import com.justnothing.javainterpreter.ast.nodes.BreakNode;
import com.justnothing.javainterpreter.ast.nodes.CastNode;
import com.justnothing.javainterpreter.ast.nodes.ClassDeclarationNode;
import com.justnothing.javainterpreter.ast.nodes.ClassReferenceNode;
import com.justnothing.javainterpreter.ast.nodes.ConditionalAssignNode;
import com.justnothing.javainterpreter.ast.nodes.ConstructorCallNode;
import com.justnothing.javainterpreter.ast.nodes.ContinueNode;
import com.justnothing.javainterpreter.ast.nodes.DeleteNode;
import com.justnothing.javainterpreter.ast.nodes.DoWhileNode;
import com.justnothing.javainterpreter.ast.nodes.FieldAccessNode;
import com.justnothing.javainterpreter.ast.nodes.FieldAssignmentNode;
import com.justnothing.javainterpreter.ast.nodes.ForEachNode;
import com.justnothing.javainterpreter.ast.nodes.ForNode;
import com.justnothing.javainterpreter.ast.nodes.FunctionCallNode;
import com.justnothing.javainterpreter.ast.nodes.IfNode;
import com.justnothing.javainterpreter.ast.nodes.ImportNode;
import com.justnothing.javainterpreter.ast.nodes.InstanceofNode;
import com.justnothing.javainterpreter.ast.nodes.InterpolatedStringNode;
import com.justnothing.javainterpreter.ast.nodes.LambdaNode;
import com.justnothing.javainterpreter.ast.nodes.LiteralNode;
import com.justnothing.javainterpreter.ast.nodes.MapLiteralNode;
import com.justnothing.javainterpreter.ast.nodes.MethodCallNode;
import com.justnothing.javainterpreter.ast.nodes.MethodReferenceNode;
import com.justnothing.javainterpreter.ast.nodes.NewArrayNode;
import com.justnothing.javainterpreter.ast.nodes.NullCoalescingAssignNode;
import com.justnothing.javainterpreter.ast.nodes.PipelineNode;
import com.justnothing.javainterpreter.ast.nodes.ReturnNode;
import com.justnothing.javainterpreter.ast.nodes.SafeFieldAccessNode;
import com.justnothing.javainterpreter.ast.nodes.SafeMethodCallNode;
import com.justnothing.javainterpreter.ast.nodes.SuperMethodCallNode;
import com.justnothing.javainterpreter.ast.nodes.SwitchNode;
import com.justnothing.javainterpreter.ast.nodes.TernaryNode;
import com.justnothing.javainterpreter.ast.nodes.ThrowNode;
import com.justnothing.javainterpreter.ast.nodes.TryNode;
import com.justnothing.javainterpreter.ast.nodes.UnaryOpNode;
import com.justnothing.javainterpreter.ast.nodes.VariableNode;
import com.justnothing.javainterpreter.ast.nodes.WhileNode;
import com.justnothing.javainterpreter.exception.EvaluationException;
import com.justnothing.javainterpreter.exception.ErrorCode;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class EvaluatorRegistry {

    private final Map<Class<? extends ASTNode>, NodeEvaluator<?>> evaluators = new ConcurrentHashMap<>();
    private final Set<Class<? extends ASTNode>> disabledNodeTypes = ConcurrentHashMap.newKeySet();

    private static volatile EvaluatorRegistry defaultInstance = null;

    public EvaluatorRegistry() {
        registerDefaultEvaluators();
    }

    public static EvaluatorRegistry getDefault() {
        if (defaultInstance == null) {
            synchronized (EvaluatorRegistry.class) {
                if (defaultInstance == null) {
                    defaultInstance = new EvaluatorRegistry();
                }
            }
        }
        return defaultInstance;
    }

    @SuppressWarnings("unchecked")
    public <T extends ASTNode> void register(Class<T> nodeType, NodeEvaluator<T> evaluator) {
        evaluators.put(nodeType, evaluator);
    }

    @SuppressWarnings("unchecked")
    public <T extends ASTNode> NodeEvaluator<T> getEvaluator(Class<T> nodeType) {
        return (NodeEvaluator<T>) evaluators.get(nodeType);
    }

    @SuppressWarnings("unchecked")
    public NodeEvaluator<ASTNode> getEvaluator(ASTNode node) {
        return (NodeEvaluator<ASTNode>) evaluators.get(node.getClass());
    }

    public boolean hasEvaluator(Class<? extends ASTNode> nodeType) {
        return evaluators.containsKey(nodeType);
    }

    public void disableNodeType(Class<? extends ASTNode> nodeType) {
        disabledNodeTypes.add(nodeType);
    }

    public void enableNodeType(Class<? extends ASTNode> nodeType) {
        disabledNodeTypes.remove(nodeType);
    }

    public boolean isNodeTypeDisabled(Class<? extends ASTNode> nodeType) {
        return disabledNodeTypes.contains(nodeType);
    }

    public Set<Class<? extends ASTNode>> getDisabledNodeTypes() {
        return Collections.unmodifiableSet(disabledNodeTypes);
    }

    public void clearDisabledNodeTypes() {
        disabledNodeTypes.clear();
    }

    public Set<Class<? extends ASTNode>> getRegisteredNodeTypes() {
        return Collections.unmodifiableSet(evaluators.keySet());
    }

    public Object evaluate(ASTNode node, ExecutionContext context) throws EvaluationException {
        if (node == null) {
            return null;
        }

        Class<? extends ASTNode> nodeType = node.getClass();

        if (disabledNodeTypes.contains(nodeType)) {
            throw new EvaluationException(
                "Node type " + nodeType.getSimpleName() + " is disabled in restricted mode",
                ErrorCode.EVAL_INVALID_OPERATION,
                node
            );
        }

        NodeEvaluator<ASTNode> evaluator = getEvaluator(node);
        if (evaluator == null) {
            throw new EvaluationException(
                "Unsupported AST node type: " + nodeType.getSimpleName(),
                ErrorCode.EVAL_INVALID_OPERATION,
                node
            );
        }

        return evaluator.evaluate(node, context);
    }

    private void registerDefaultEvaluators() {
        register(BlockNode.class, ASTEvaluator::evaluateBlock);
        register(LiteralNode.class, (node, ctx) -> ASTEvaluator.evaluateLiteral(node));
        register(InterpolatedStringNode.class, ASTEvaluator::evaluateInterpolatedString);
        register(VariableNode.class, ASTEvaluator::evaluateVariable);
        register(BinaryOpNode.class, ASTEvaluator::evaluateBinaryOp);
        register(UnaryOpNode.class, ASTEvaluator::evaluateUnaryOp);
        register(AssignmentNode.class, ASTEvaluator::evaluateAssignment);
        register(ConditionalAssignNode.class, ASTEvaluator::evaluateConditionalAssign);
        register(NullCoalescingAssignNode.class, ASTEvaluator::evaluateNullCoalescingAssign);
        register(FieldAssignmentNode.class, ASTEvaluator::evaluateFieldAssignment);
        register(ArrayAssignmentNode.class, ASTEvaluator::evaluateArrayAssignment);
        register(MethodCallNode.class, ASTEvaluator::evaluateMethodCall);
        register(FunctionCallNode.class, ASTEvaluator::evaluateFunctionCall);
        register(FieldAccessNode.class, ASTEvaluator::evaluateFieldAccess);
        register(SafeFieldAccessNode.class, ASTEvaluator::evaluateSafeFieldAccess);
        register(SafeMethodCallNode.class, ASTEvaluator::evaluateSafeMethodCall);
        register(ArrayAccessNode.class, ASTEvaluator::evaluateArrayAccess);
        register(ClassReferenceNode.class, ASTEvaluator::evaluateClassReference);
        register(NewArrayNode.class, ASTEvaluator::evaluateNewArray);
        register(ArrayLiteralNode.class, ASTEvaluator::evaluateArrayLiteral);
        register(MapLiteralNode.class, ASTEvaluator::evaluateMapLiteral);
        register(IfNode.class, ASTEvaluator::evaluateIf);
        register(WhileNode.class, ASTEvaluator::evaluateWhile);
        register(DoWhileNode.class, ASTEvaluator::evaluateDoWhile);
        register(ForNode.class, ASTEvaluator::evaluateFor);
        register(ForEachNode.class, ASTEvaluator::evaluateForEach);
        register(TernaryNode.class, ASTEvaluator::evaluateTernary);
        register(PipelineNode.class, ASTEvaluator::evaluatePipeline);
        register(InstanceofNode.class, ASTEvaluator::evaluateInstanceof);
        register(CastNode.class, ASTEvaluator::evaluateCast);
        register(LambdaNode.class, ASTEvaluator::evaluateLambda);
        register(ReturnNode.class, ASTEvaluator::evaluateReturn);
        register(BreakNode.class, ASTEvaluator::evaluateBreak);
        register(ContinueNode.class, ASTEvaluator::evaluateContinue);
        register(MethodReferenceNode.class, ASTEvaluator::evaluateMethodReference);
        register(TryNode.class, ASTEvaluator::evaluateTry);
        register(ThrowNode.class, ASTEvaluator::evaluateThrow);
        register(SwitchNode.class, ASTEvaluator::evaluateSwitch);
        register(ImportNode.class, (node, ctx) -> {
            ctx.addImport(node.getPackageName());
            return null;
        });
        register(DeleteNode.class, ASTEvaluator::evaluateDelete);
        register(ClassDeclarationNode.class, ASTEvaluator::evaluateClassDeclaration);
        register(ConstructorCallNode.class, ASTEvaluator::evaluateConstructorCall);
        register(SuperMethodCallNode.class, ASTEvaluator::evaluateSuperMethodCall);
        register(AsyncNode.class, ASTEvaluator::evaluateAsync);
        register(AwaitNode.class, ASTEvaluator::evaluateAwait);
    }

    public static EvaluatorRegistry createRestrictedRegistry(Set<Class<? extends ASTNode>> allowedTypes) {
        EvaluatorRegistry registry = new EvaluatorRegistry();
        registry.disabledNodeTypes.clear();

        Set<Class<? extends ASTNode>> allTypes = new HashSet<>(registry.evaluators.keySet());
        for (Class<? extends ASTNode> type : allTypes) {
            if (!allowedTypes.contains(type)) {
                registry.disabledNodeTypes.add(type);
            }
        }

        return registry;
    }

    public static Set<Class<? extends ASTNode>> getSafeExpressionTypes() {
        Set<Class<? extends ASTNode>> safeTypes = new HashSet<>();
        safeTypes.add(LiteralNode.class);
        safeTypes.add(VariableNode.class);
        safeTypes.add(BinaryOpNode.class);
        safeTypes.add(UnaryOpNode.class);
        safeTypes.add(TernaryNode.class);
        safeTypes.add(InstanceofNode.class);
        safeTypes.add(CastNode.class);
        safeTypes.add(ArrayLiteralNode.class);
        safeTypes.add(MapLiteralNode.class);
        safeTypes.add(InterpolatedStringNode.class);
        safeTypes.add(ArrayAccessNode.class);
        safeTypes.add(NewArrayNode.class);
        return safeTypes;
    }

    public static Set<Class<? extends ASTNode>> getSafeStatementTypes() {
        Set<Class<? extends ASTNode>> safeTypes = getSafeExpressionTypes();
        safeTypes.add(BlockNode.class);
        safeTypes.add(IfNode.class);
        safeTypes.add(WhileNode.class);
        safeTypes.add(ForNode.class);
        safeTypes.add(ForEachNode.class);
        safeTypes.add(AssignmentNode.class);
        safeTypes.add(VariableNode.class);
        return safeTypes;
    }

    public static Set<Class<? extends ASTNode>> getDangerousTypes() {
        Set<Class<? extends ASTNode>> dangerousTypes = new HashSet<>();
        dangerousTypes.add(MethodCallNode.class);
        dangerousTypes.add(FunctionCallNode.class);
        dangerousTypes.add(ConstructorCallNode.class);
        dangerousTypes.add(ClassReferenceNode.class);
        dangerousTypes.add(FieldAccessNode.class);
        dangerousTypes.add(ImportNode.class);
        dangerousTypes.add(ClassDeclarationNode.class);
        dangerousTypes.add(AsyncNode.class);
        dangerousTypes.add(AwaitNode.class);
        dangerousTypes.add(TryNode.class);
        dangerousTypes.add(ThrowNode.class);
        dangerousTypes.add(ReturnNode.class);
        dangerousTypes.add(DeleteNode.class);
        return dangerousTypes;
    }
}
