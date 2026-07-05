package com.justnothing.engine.v2.ast;

import com.justnothing.engine.v2.ast.decl.*;
import com.justnothing.engine.v2.ast.expr.*;
import com.justnothing.engine.v2.ast.stmt.*;

import java.util.List;
import java.util.Map;

/**
 * AST 树形格式化器，将 AST 树格式化为缩进形式的文本，用于调试和可视化。
 *
 * @author JustNothing1021
 */
public class ASTTreeFormatter implements ASTVisitor<String> {

    private final int indent;

    private ASTTreeFormatter(int indent) {
        this.indent = indent;
    }

    /**
     * 将 AST 节点格式化为缩进形式的文本
     */
    public static String format(ASTNode node) {
        return new ASTTreeFormatter(0).formatChild(node);
    }

    // ========== 辅助方法 ==========

    private String indent() {
        return "  ".repeat(indent);
    }

    private String formatChild(ASTNode node) {
        if (node == null) return "";
        return new ASTTreeFormatter(indent + 1).doVisit(node);
    }

    private String formatChildren(List<? extends ASTNode> nodes) {
        if (nodes == null || nodes.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        for (ASTNode node : nodes) {
            sb.append(formatChild(node));
        }
        return sb.toString();
    }

    private String doVisit(ASTNode node) {
        return (String) node.accept(this);
    }

    private String header(ASTNode node, String... attrs) {
        StringBuilder sb = new StringBuilder(indent());
        sb.append(node.nodeName());
        SourceLocation loc = node.getLocation();
        if (loc != null) {
            sb.append(" [line=").append(loc.getLine()).append(", col=").append(loc.getColumn());
            for (String attr : attrs) {
                sb.append(", ").append(attr);
            }
            sb.append(']');
        } else if (attrs.length > 0) {
            sb.append(" [");
            for (int i = 0; i < attrs.length; i++) {
                if (i > 0) sb.append(", ");
                sb.append(attrs[i]);
            }
            sb.append(']');
        }
        sb.append('\n');
        return sb.toString();
    }

    private String headerNoLoc(ASTNode node, String... attrs) {
        StringBuilder sb = new StringBuilder(indent());
        sb.append(node.nodeName());
        if (attrs.length > 0) {
            sb.append(" [");
            for (int i = 0; i < attrs.length; i++) {
                if (i > 0) sb.append(", ");
                sb.append(attrs[i]);
            }
            sb.append(']');
        }
        sb.append('\n');
        return sb.toString();
    }

    private String field(String name, ASTNode child) {
        if (child == null) return "";
        return indent() + name + ":\n" + formatChild(child);
    }

    private String fieldList(String name, List<? extends ASTNode> children) {
        if (children == null || children.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        sb.append(indent()).append(name).append(":\n");
        for (ASTNode child : children) {
            sb.append(formatChild(child));
        }
        return sb.toString();
    }

    // ========== 表达式 ==========

    @Override
    public String visit(LiteralExpr node) {
        String valueStr = node.getValue() != null ? String.valueOf(node.getValue()) : "null";
        String typeStr = node.getType() != null ? node.getType().getSimpleName() : "null";
        return header(node, valueStr + " (" + typeStr + ")");
    }

    @Override
    public String visit(VariableExpr node) {
        return header(node, node.getName());
    }

    @Override
    public String visit(BinaryOpExpr node) {
        StringBuilder sb = new StringBuilder();
        sb.append(header(node, node.getOperator()));
        sb.append(field("left", node.getLeft()));
        sb.append(field("right", node.getRight()));
        return sb.toString();
    }

    @Override
    public String visit(UnaryOpExpr node) {
        StringBuilder sb = new StringBuilder();
        sb.append(header(node, node.getOperator() + ", " + (node.isPrefix() ? "prefix" : "postfix")));
        sb.append(field("operand", node.getOperand()));
        return sb.toString();
    }

    @Override
    public String visit(TernaryExpr node) {
        StringBuilder sb = new StringBuilder();
        sb.append(header(node));
        sb.append(field("condition", node.getCondition()));
        sb.append(field("trueExpr", node.getTrueExpr()));
        sb.append(field("falseExpr", node.getFalseExpr()));
        return sb.toString();
    }

    @Override
    public String visit(MethodCallExpr node) {
        StringBuilder sb = new StringBuilder();
        String info = node.getMethodName();
        if (!node.getTypeArguments().isEmpty()) {
            info += "<" + String.join(", ", node.getTypeArguments()) + ">";
        }
        sb.append(header(node, info));
        sb.append(field("target", node.getTarget()));
        sb.append(fieldList("arguments", node.getArguments()));
        return sb.toString();
    }

    @Override
    public String visit(FieldAccessExpr node) {
        StringBuilder sb = new StringBuilder();
        sb.append(header(node, node.getFieldName()));
        sb.append(field("target", node.getTarget()));
        return sb.toString();
    }

    @Override
    public String visit(AssignmentExpr node) {
        StringBuilder sb = new StringBuilder();
        sb.append(header(node, node.getOperator()));
        sb.append(field("target", node.getTarget()));
        sb.append(field("value", node.getValue()));
        return sb.toString();
    }

    @Override
    public String visit(CastExpr node) {
        StringBuilder sb = new StringBuilder();
        sb.append(header(node, node.getTargetType()));
        sb.append(field("expr", node.getExpr()));
        return sb.toString();
    }

    @Override
    public String visit(LambdaExpr node) {
        StringBuilder sb = new StringBuilder();
        sb.append(header(node, String.join(", ", node.getParameters())));
        sb.append(field("body", node.getBody()));
        return sb.toString();
    }

    @Override
    public String visit(MethodReferenceExpr node) {
        String typeArgs = node.getTypeArguments().isEmpty() ? "" : "<" + String.join(", ", node.getTypeArguments()) + ">";
        StringBuilder sb = new StringBuilder();
        sb.append(header(node, node.getMethodName() + typeArgs));
        sb.append(field("target", node.getTarget()));
        return sb.toString();
    }

    @Override
    public String visit(NewExpr node) {
        StringBuilder sb = new StringBuilder();
        sb.append(header(node, node.getClassName()));
        sb.append(fieldList("arguments", node.getArguments()));
        return sb.toString();
    }

    @Override
    public String visit(AsyncExpr node) {
        StringBuilder sb = new StringBuilder();
        sb.append(header(node));
        sb.append(field("expression", node.getExpression()));
        return sb.toString();
    }

    @Override
    public String visit(AwaitExpr node) {
        StringBuilder sb = new StringBuilder();
        sb.append(header(node));
        sb.append(field("expression", node.getExpression()));
        return sb.toString();
    }

    @Override
    public String visit(ArrayAccessExpr node) {
        StringBuilder sb = new StringBuilder();
        sb.append(header(node));
        sb.append(field("target", node.getTarget()));
        sb.append(field("index", node.getIndex()));
        return sb.toString();
    }

    @Override
    public String visit(ArrayLiteralExpr node) {
        StringBuilder sb = new StringBuilder();
        sb.append(header(node));
        sb.append(fieldList("elements", node.getElements()));
        return sb.toString();
    }

    @Override
    public String visit(MapLiteralExpr node) {
        StringBuilder sb = new StringBuilder();
        sb.append(header(node));
        sb.append(fieldList("keys", node.getKeys()));
        sb.append(fieldList("values", node.getValues()));
        return sb.toString();
    }

    @Override
    public String visit(InterpolatedStringExpr node) {
        StringBuilder sb = new StringBuilder();
        sb.append(header(node));
        for (Object part : node.getParts()) {
            if (part instanceof String) {
                sb.append(indent()).append("  \"").append(part).append("\"\n");
            } else if (part instanceof ASTNode) {
                sb.append(formatChild((ASTNode) part));
            }
        }
        return sb.toString();
    }

    @Override
    public String visit(ClassReferenceExpr node) {
        return header(node, node.getClassName());
    }

    @Override
    public String visit(InstanceofExpr node) {
        StringBuilder sb = new StringBuilder();
        sb.append(header(node, node.getTypeName()));
        sb.append(field("expr", node.getExpr()));
        return sb.toString();
    }

    @Override
    public String visit(SafeFieldAccessExpr node) {
        StringBuilder sb = new StringBuilder();
        sb.append(header(node, node.getFieldName()));
        sb.append(field("target", node.getTarget()));
        return sb.toString();
    }

    @Override
    public String visit(SafeMethodCallExpr node) {
        StringBuilder sb = new StringBuilder();
        String info = node.getMethodName();
        if (!node.getTypeArguments().isEmpty()) {
            info += "<" + String.join(", ", node.getTypeArguments()) + ">";
        }
        sb.append(header(node, info));
        sb.append(field("target", node.getTarget()));
        sb.append(fieldList("arguments", node.getArguments()));
        return sb.toString();
    }

    @Override
    public String visit(NewArrayExpr node) {
        StringBuilder sb = new StringBuilder();
        sb.append(header(node, node.getElementTypeName()));
        sb.append(fieldList("dimensions", node.getDimensions()));
        sb.append(fieldList("initializer", node.getInitializer()));
        return sb.toString();
    }

    @Override
    public String visit(SuperConstructorCallExpr node) {
        StringBuilder sb = new StringBuilder();
        sb.append(header(node));
        sb.append(fieldList("arguments", node.getArguments()));
        return sb.toString();
    }

    @Override
    public String visit(SuperExpr node) {
        return header(node);
    }

    // ========== 语句 ==========

    @Override
    public String visit(BlockStmt node) {
        StringBuilder sb = new StringBuilder();
        sb.append(header(node));
        sb.append(fieldList("statements", node.getStatements()));
        return sb.toString();
    }

    @Override
    public String visit(ExprStmt node) {
        StringBuilder sb = new StringBuilder();
        sb.append(header(node));
        sb.append(field("expression", node.getExpression()));
        return sb.toString();
    }

    @Override
    public String visit(IfStmt node) {
        StringBuilder sb = new StringBuilder();
        sb.append(header(node));
        sb.append(field("condition", node.getCondition()));
        sb.append(field("thenBlock", node.getThenBlock()));
        sb.append(field("elseBlock", node.getElseBlock()));
        return sb.toString();
    }

    @Override
    public String visit(WhileStmt node) {
        StringBuilder sb = new StringBuilder();
        sb.append(header(node));
        sb.append(field("condition", node.getCondition()));
        sb.append(field("body", node.getBody()));
        return sb.toString();
    }

    @Override
    public String visit(DoWhileStmt node) {
        StringBuilder sb = new StringBuilder();
        sb.append(header(node));
        sb.append(field("body", node.getBody()));
        sb.append(field("condition", node.getCondition()));
        return sb.toString();
    }

    @Override
    public String visit(ForStmt node) {
        StringBuilder sb = new StringBuilder();
        sb.append(header(node));
        sb.append(field("initializer", node.getInitializer()));
        sb.append(field("condition", node.getCondition()));
        sb.append(field("update", node.getUpdate()));
        sb.append(field("body", node.getBody()));
        return sb.toString();
    }

    @Override
    public String visit(ForEachStmt node) {
        StringBuilder sb = new StringBuilder();
        String info = node.getVariableName();
        if (node.isFinal()) info = "final " + info;
        sb.append(header(node, info));
        sb.append(field("iterable", node.getIterable()));
        sb.append(field("body", node.getBody()));
        return sb.toString();
    }

    @Override
    public String visit(SwitchStmt node) {
        StringBuilder sb = new StringBuilder();
        sb.append(header(node));
        sb.append(field("subject", node.getSubject()));
        sb.append(fieldList("cases", node.getCases()));
        return sb.toString();
    }

    @Override
    public String visit(SwitchCase node) {
        StringBuilder sb = new StringBuilder();
        String headerInfo;
        if (node.isDefault()) {
            headerInfo = "default";
        } else {
            headerInfo = node.getValues().stream().map(Object::toString).collect(java.util.stream.Collectors.joining(", "));
        }
        sb.append(header(node, headerInfo));
        sb.append(field("body", node.getBody()));
        return sb.toString();
    }

    @Override
    public String visit(TryStmt node) {
        StringBuilder sb = new StringBuilder();
        sb.append(header(node));
        if (node.getResources() != null && !node.getResources().isEmpty()) {
            sb.append(indent()).append("resources:\n");
            for (ASTNode res : node.getResources()) {
                sb.append(formatChild(res));
            }
        }
        sb.append(field("tryBlock", node.getTryBlock()));
        if (node.getCatchClauses() != null && !node.getCatchClauses().isEmpty()) {
            sb.append(indent()).append("catchClauses:\n");
            for (TryStmt.CatchClause cc : node.getCatchClauses()) {
                sb.append(indent()).append("  CatchClause [").append(String.join(" | ", cc.exceptionTypes()));
                if (cc.variableName() != null) {
                    sb.append(", ").append(cc.variableName());
                }
                sb.append("]\n");
                sb.append(formatChild(cc.block()));
            }
        }
        sb.append(field("finallyBlock", node.getFinallyBlock()));
        return sb.toString();
    }

    @Override
    public String visit(ThrowStmt node) {
        StringBuilder sb = new StringBuilder();
        sb.append(header(node));
        sb.append(field("expression", node.getExpression()));
        return sb.toString();
    }

    @Override
    public String visit(AssertStmt node) {
        StringBuilder sb = new StringBuilder();
        sb.append(header(node));
        sb.append(field("condition", node.getCondition()));
        if (node.hasMessage()) {
            sb.append(field("message", node.getMessage()));
        }
        return sb.toString();
    }

    @Override
    public String visit(ReturnStmt node) {
        StringBuilder sb = new StringBuilder();
        sb.append(header(node));
        sb.append(field("value", node.getValue()));
        return sb.toString();
    }

    @Override
    public String visit(YieldStmt node) {
        StringBuilder sb = new StringBuilder();
        sb.append(header(node));
        sb.append(field("value", node.getValue()));
        return sb.toString();
    }

    @Override
    public String visit(BreakStmt node) {
        if (node.getLabel() != null) {
            return header(node, "label=" + node.getLabel());
        }
        return header(node);
    }

    @Override
    public String visit(ContinueStmt node) {
        if (node.getLabel() != null) {
            return header(node, "label=" + node.getLabel());
        }
        return header(node);
    }

    @Override
    public String visit(SynchronizedStmt node) {
        StringBuilder sb = new StringBuilder();
        sb.append(header(node));
        sb.append(field("lock", node.getLock()));
        sb.append(field("body", node.getBody()));
        return sb.toString();
    }

    @Override
    public String visit(LabeledStmt node) {
        StringBuilder sb = new StringBuilder();
        sb.append(header(node, "label=" + node.getLabel()));
        sb.append(field("body", node.getBody()));
        return sb.toString();
    }

    // ========== 声明 ==========

    @Override
    public String visit(VarDecl node) {
        StringBuilder attrs = new StringBuilder();
        attrs.append(node.getName()).append(", ").append(node.getTypeName());
        if (node.isFinal()) attrs.append(", final");
        StringBuilder sb = new StringBuilder();
        sb.append(header(node, attrs.toString()));
        sb.append(field("initializer", node.getInitializer()));
        if (node.hasAdditionalVars()) {
            sb.append(indent()).append("additionalVars:\n");
            for (VarDecl extra : node.getAdditionalVars()) {
                sb.append(formatChild(extra));
            }
        }
        return sb.toString();
    }

    @Override
    public String visit(ImportDecl node) {
        return header(node, node.getImportPath()
                + (node.isStatic() ? ", static" : "")
                + (node.isWildcard() ? ", wildcard" : ""));
    }

    @Override
    public String visit(ClassDecl node) {
        StringBuilder attrs = new StringBuilder();
        attrs.append(node.getName()).append(", kind=").append(node.getKind());
        if (!node.getTypeParameters().isEmpty()) {
            attrs.append(", <").append(String.join(", ", node.getTypeParameters())).append(">");
        }
        if (node.getSuperClassName() != null) {
            attrs.append(", extends ").append(node.getSuperClassName());
        }
        if (node.getInterfaces() != null && !node.getInterfaces().isEmpty()) {
            attrs.append(", implements ").append(String.join(", ", node.getInterfaces()));
        }
        if (node.getPermitted() != null && !node.getPermitted().isEmpty()) {
            attrs.append(", permits ").append(String.join(", ", node.getPermitted()));
        }
        attrs.append(", modifiers=").append(node.getModifiers());
        StringBuilder sb = new StringBuilder();
        sb.append(header(node, attrs.toString()));
        if (node.isRecord() && node.getRecordComponents() != null && !node.getRecordComponents().isEmpty()) {
            sb.append(indent()).append("recordComponents:\n");
            for (ParamDecl p : node.getRecordComponents()) {
                sb.append(formatChild(p));
            }
        }
        if (node.hasEnumConstants() && node.getEnumConstants() != null) {
            sb.append(indent()).append("enumConstants: [").append(String.join(", ", node.getEnumConstants())).append("]\n");
        }
        sb.append(fieldList("members", node.getMembers()));
        return sb.toString();
    }

    @Override
    public String visit(MethodDecl node) {
        StringBuilder attrs = new StringBuilder();
        attrs.append(node.getName()).append(", ").append(node.getReturnType());
        if (!node.getTypeParameters().isEmpty()) {
            attrs.append(", <").append(String.join(", ", node.getTypeParameters())).append(">");
        }
        attrs.append(", modifiers=").append(node.getModifiers());
        StringBuilder sb = new StringBuilder();
        sb.append(header(node, attrs.toString()));
        sb.append(fieldList("parameters", node.getParameters()));
        if (node.getThrowsList() != null && !node.getThrowsList().isEmpty()) {
            sb.append("  ").append("throws: ").append(String.join(", ", node.getThrowsList())).append("\n");
        }
        sb.append(field("body", node.getBody()));
        return sb.toString();
    }

    @Override
    public String visit(ConstructorDecl node) {
        StringBuilder attrs = new StringBuilder();
        attrs.append(node.getName());
        if (!node.getTypeParameters().isEmpty()) {
            attrs.append(", <").append(String.join(", ", node.getTypeParameters())).append(">");
        }
        attrs.append(", modifiers=").append(node.getModifiers());
        StringBuilder sb = new StringBuilder();
        sb.append(header(node, attrs.toString()));
        sb.append(fieldList("parameters", node.getParameters()));
        sb.append(field("body", node.getBody()));
        return sb.toString();
    }

    @Override
    public String visit(FieldDecl node) {
        StringBuilder attrs = new StringBuilder();
        attrs.append(node.getName()).append(", ").append(node.getTypeName());
        attrs.append(", modifiers=").append(node.getModifiers());
        StringBuilder sb = new StringBuilder();
        sb.append(header(node, attrs.toString()));
        sb.append(field("initializer", node.getInitializer()));
        if (node.hasAdditionalVars()) {
            sb.append(indent()).append("additionalVars:\n");
            for (FieldDecl extra : node.getAdditionalVars()) {
                sb.append(formatChild(extra));
            }
        }
        return sb.toString();
    }

    @Override
    public String visit(ParamDecl node) {
        StringBuilder attrs = new StringBuilder();
        attrs.append(node.getName()).append(", ").append(node.getTypeName());
        if (node.isFinal()) attrs.append(", final");
        StringBuilder sb = new StringBuilder();
        sb.append(header(node, attrs.toString()));
        if (node.getAnnotations() != null && !node.getAnnotations().isEmpty()) {
            sb.append(indent()).append("annotations:\n");
            for (var ann : node.getAnnotations()) {
                sb.append(formatChild(ann));
            }
        }
        sb.append(field("defaultValue", node.getDefaultValue()));
        return sb.toString();
    }

    @Override
    public String visit(AnnotationVal node) {
        StringBuilder sb = new StringBuilder();
        sb.append(header(node, node.getName()));
        if (node.getAttributes() != null && !node.getAttributes().isEmpty()) {
            sb.append(indent()).append("attributes:\n");
            for (Map.Entry<String, ASTNode> entry : node.getAttributes().entrySet()) {
                sb.append(indent()).append("  ").append(entry.getKey()).append(":\n");
                sb.append(formatChild(entry.getValue()));
            }
        }
        return sb.toString();
    }

    @Override
    public String visit(InstanceInitDecl node) {
        StringBuilder sb = new StringBuilder();
        sb.append(header(node, "InitDecl"));
        sb.append(field("statements", node.getStatements()));
        return sb.toString();
    }

    @Override
    public String visit(StaticInitDecl node) {
        StringBuilder sb = new StringBuilder();
        sb.append(header(node, "StaticInitDecl"));
        sb.append(field("statements", node.getStatements()));
        return sb.toString();
    }

}
