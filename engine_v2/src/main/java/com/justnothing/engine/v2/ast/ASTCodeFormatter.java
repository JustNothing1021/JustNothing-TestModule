package com.justnothing.engine.v2.ast;

import com.justnothing.engine.v2.ast.decl.*;
import com.justnothing.engine.v2.ast.expr.*;
import com.justnothing.engine.v2.ast.stmt.*;

import java.lang.reflect.Modifier;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * AST 代码重建格式化器。
 * <p>
 * 将 AST 树重建为可读的源代码文本，尽量接近原始 Java 语法。
 * </p>
 *
 * @author JustNothing1021
 */
public class ASTCodeFormatter implements ASTVisitor<String> {

    private ASTCodeFormatter() {}

    /**
     * 将 AST 节点格式化为可读的源代码文本。
     */
    public static String format(ASTNode node) {
        if (node == null) {
            return "";
        }
        return node.accept(new ASTCodeFormatter());
    }

    // ========== 表达式 ==========

    @Override
    public String visit(LiteralExpr node) {
        Object value = node.getValue();
        if (value == null) {
            return "null";
        }
        if (value instanceof String) {
            return "\"" + escapeString((String) value) + "\"";
        }
        if (value instanceof Character) {
            return "'" + escapeChar((Character) value) + "'";
        }
        if (value instanceof Long) {
            return value + "L";
        }
        if (value instanceof Float) {
            return value + "F";
        }
        if (value instanceof Double) {
            return value + "D";
        }
        return String.valueOf(value);
    }

    @Override
    public String visit(VariableExpr node) {
        return node.getName();
    }

    @Override
    public String visit(BinaryOpExpr node) {
        String left = format(node.getLeft());
        String right = format(node.getRight());
        return "(" + left + " " + node.getOperator() + " " + right + ")";
    }

    @Override
    public String visit(UnaryOpExpr node) {
        String operand = format(node.getOperand());
        String op = node.getOperator();
        if (node.isPrefix()) {
            return op + operand;
        }
        return operand + op;
    }

    @Override
    public String visit(TernaryExpr node) {
        String cond = format(node.getCondition());
        String trueExpr = format(node.getTrueExpr());
        String falseExpr = format(node.getFalseExpr());
        return cond + " ? " + trueExpr + " : " + falseExpr;
    }

    @Override
    public String visit(MethodCallExpr node) {
        StringBuilder sb = new StringBuilder();
        if (node.getTarget() != null) {
            sb.append(format(node.getTarget())).append(".");
        }
        sb.append(node.getMethodName());
        if (!node.getTypeArguments().isEmpty()) {
            sb.append("<").append(String.join(", ", node.getTypeArguments())).append(">");
        }
        sb.append("(").append(join(node.getArguments(), ", ")).append(")");
        return sb.toString();
    }

    @Override
    public String visit(FieldAccessExpr node) {
        return format(node.getTarget()) + "." + node.getFieldName();
    }

    @Override
    public String visit(AssignmentExpr node) {
        String target = format(node.getTarget());
        String value = format(node.getValue());
        return target + " " + node.getOperator() + " " + value;
    }

    @Override
    public String visit(CastExpr node) {
        return "(" + node.getTargetType() + ") " + format(node.getExpr());
    }

    @Override
    public String visit(LambdaExpr node) {
        StringBuilder sb = new StringBuilder();
        sb.append("(").append(String.join(", ", node.getParameters())).append(")");
        sb.append(" -> ");
        if (node.getBody() instanceof BlockStmt) {
            sb.append(formatBlock(node.getBody()));
        } else {
            sb.append(format(node.getBody()));
        }
        return sb.toString();
    }

    @Override
    public String visit(MethodReferenceExpr node) {
        StringBuilder sb = new StringBuilder();
        sb.append(format(node.getTarget())).append("::");
        List<String> typeArgs = node.getTypeArguments();
        if (!typeArgs.isEmpty()) {
            sb.append("<").append(String.join(", ", typeArgs)).append(">");
        }
        sb.append(node.getMethodName());
        return sb.toString();
    }

    @Override
    public String visit(NewExpr node) {
        return "new " + node.getClassName() + "(" + join(node.getArguments(), ", ") + ")";
    }

    @Override
    public String visit(ArrayAccessExpr node) {
        return format(node.getTarget()) + "[" + format(node.getIndex()) + "]";
    }

    @Override
    public String visit(ArrayLiteralExpr node) {
        return "[" + join(node.getElements(), ", ") + "]";
    }

    @Override
    public String visit(MapLiteralExpr node) {
        List<ASTNode> keys = node.getKeys();
        List<ASTNode> values = node.getValues();
        StringBuilder sb = new StringBuilder("{");
        for (int i = 0; i < keys.size(); i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(format(keys.get(i))).append(": ").append(format(values.get(i)));
        }
        sb.append("}");
        return sb.toString();
    }

    @Override
    public String visit(InterpolatedStringExpr node) {
        StringBuilder sb = new StringBuilder("\"");
        for (Object part : node.getParts()) {
            if (part instanceof String) {
                sb.append(escapeString((String) part));
            } else if (part instanceof ASTNode) {
                sb.append("${").append(format((ASTNode) part)).append("}");
            }
        }
        sb.append("\"");
        return sb.toString();
    }

    @Override
    public String visit(ClassReferenceExpr node) {
        return node.getClassName() + ".class";
    }

    @Override
    public String visit(InstanceofExpr node) {
        return format(node.getExpr()) + " instanceof " + node.getTypeName();
    }

    @Override
    public String visit(AsyncExpr node) {
        return "async " + format(node.getExpression());
    }

    @Override
    public String visit(AwaitExpr node) {
        return "await " + format(node.getExpression());
    }

    // ========== 语句 ==========

    @Override
    public String visit(BlockStmt node) {
        StringBuilder sb = new StringBuilder("{ ");
        for (ASTNode stmt : node.getStatements()) {
            sb.append(format(stmt)).append(" ");
        }
        sb.append("}");
        return sb.toString();
    }

    @Override
    public String visit(ExprStmt node) {
        return format(node.getExpression()) + ";";
    }

    @Override
    public String visit(IfStmt node) {
        StringBuilder sb = new StringBuilder();
        sb.append("if (").append(format(node.getCondition())).append(") ");
        sb.append(formatBlock(node.getThenBlock()));
        if (node.hasElse()) {
            sb.append(" else ");
            sb.append(formatBlock(node.getElseBlock()));
        }
        return sb.toString();
    }

    @Override
    public String visit(WhileStmt node) {
        return "while (" + format(node.getCondition()) + ") " + formatBlock(node.getBody());
    }

    @Override
    public String visit(DoWhileStmt node) {
        return "do " + formatBlock(node.getBody()) + " while (" + format(node.getCondition()) + ");";
    }

    @Override
    public String visit(ForStmt node) {
        String init = node.getInitializer() != null ? format(node.getInitializer()) : "";
        String cond = node.getCondition() != null ? format(node.getCondition()) : "";
        String update = node.getUpdate() != null ? format(node.getUpdate()) : "";
        return "for (" + init + "; " + cond + "; " + update + ") " + formatBlock(node.getBody());
    }

    @Override
    public String visit(ForEachStmt node) {
        StringBuilder sb = new StringBuilder("for (");
        if (node.isFinal()) sb.append("final ");
        if (node.getItemType() != null) {
            sb.append(node.getItemType().name()).append(" ");
        }
        sb.append(node.getVariableName()).append(" : ").append(format(node.getIterable())).append(") ");
        sb.append(formatBlock(node.getBody()));
        return sb.toString();
    }

    @Override
    public String visit(SwitchStmt node) {
        StringBuilder sb = new StringBuilder();
        sb.append("switch (").append(format(node.getSubject())).append(") { ");
        for (SwitchCase caseNode : node.getCases()) {
            sb.append(format(caseNode)).append(" ");
        }
        sb.append("}");
        return sb.toString();
    }

    @Override
    public String visit(SwitchCase node) {
        StringBuilder sb = new StringBuilder();
        if (node.isDefault()) {
            sb.append("default: ");
        } else {
            sb.append("case ");
            sb.append(node.getValues().stream().map(v -> format(v)).reduce((a, b) -> a + ", " + b).orElse(""));
            sb.append(": ");
        }
        sb.append(format(node.getBody()));
        return sb.toString();
    }

    @Override
    public String visit(TryStmt node) {
        StringBuilder sb = new StringBuilder();
        sb.append("try");
        if (node.getResources() != null && !node.getResources().isEmpty()) {
            sb.append(" (");
            for (var it = node.getResources().iterator(); it.hasNext();) {
                sb.append(format(it.next()));
                if (it.hasNext()) sb.append("; ");
            }
            sb.append(")");
        }
        sb.append(" ").append(formatBlock(node.getTryBlock()));
        for (TryStmt.CatchClause cc : node.getCatchClauses()) {
            sb.append(" catch (").append(String.join(" | ", cc.exceptionTypes())).append(" ").append(cc.variableName()).append(") ");
            sb.append(formatBlock(cc.block()));
        }
        if (node.getFinallyBlock() != null) {
            sb.append(" finally ").append(formatBlock(node.getFinallyBlock()));
        }
        return sb.toString();
    }

    @Override
    public String visit(ThrowStmt node) {
        return "throw " + format(node.getExpression()) + ";";
    }

    @Override
    public String visit(AssertStmt node) {
        if (node.hasMessage()) {
            return "assert " + format(node.getCondition()) + " : " + format(node.getMessage()) + ";";
        }
        return "assert " + format(node.getCondition()) + ";";
    }

    @Override
    public String visit(ReturnStmt node) {
        if (node.hasValue()) {
            return "return " + format(node.getValue()) + ";";
        }
        return "return;";
    }

    @Override
    public String visit(YieldStmt node) {
        return "yield " + format(node.getValue()) + ";";
    }

    @Override
    public String visit(BreakStmt node) {
        if (node.getLabel() != null) {
            return "break " + node.getLabel() + ";";
        }
        return "break;";
    }

    @Override
    public String visit(ContinueStmt node) {
        if (node.getLabel() != null) {
            return "continue " + node.getLabel() + ";";
        }
        return "continue;";
    }

    @Override
    public String visit(SynchronizedStmt node) {
        return "";
    }

    @Override
    public String visit(LabeledStmt node) {
        return node.getLabel() + ": " + format(node.getBody());
    }

    // ========== 声明 ==========

    @Override
    public String visit(VarDecl node) {
        String typeName = node.getTypeName() != null ? node.getTypeName() : "auto";
        StringBuilder sb = new StringBuilder();
        if (node.isFinal()) sb.append("final ");
        sb.append(typeName).append(" ").append(node.getName());
        if (node.hasInitializer()) {
            sb.append(" = ").append(format(node.getInitializer()));
        }
        if (node.hasAdditionalVars()) {
            for (VarDecl extra : node.getAdditionalVars()) {
                sb.append(", ").append(extra.getName());
                if (extra.hasInitializer()) {
                    sb.append(" = ").append(format(extra.getInitializer()));
                }
            }
        }
        sb.append(";");
        return sb.toString();
    }

    @Override
    public String visit(ImportDecl node) {
        StringBuilder sb = new StringBuilder("import ");
        if (node.isStatic()) {
            sb.append("static ");
        }
        sb.append(node.getImportPath());
        if (node.isWildcard()) {
            sb.append(".*");
        }
        sb.append(";");
        return sb.toString();
    }

    @Override
    public String visit(ClassDecl node) {
        StringBuilder sb = new StringBuilder();
        sb.append(decodeModifiers(node.getModifiers()));
        sb.append(node.getKind().name().toLowerCase()).append(" ").append(node.getName());
        if (!node.getTypeParameters().isEmpty()) {
            sb.append("<").append(String.join(", ", node.getTypeParameters())).append(">");
        }
        if (node.getSuperClassName() != null) {
            sb.append(" extends ").append(node.getSuperClassName());
        }
        if (node.getInterfaces() != null && !node.getInterfaces().isEmpty()) {
            sb.append(" implements ").append(String.join(", ", node.getInterfaces()));
        }
        if (node.getPermitted() != null && !node.getPermitted().isEmpty()) {
            sb.append(" permits ").append(String.join(", ", node.getPermitted()));
        }
        sb.append(" { ");
        if (node.hasEnumConstants() && node.getEnumConstants() != null) {
            sb.append(String.join(", ", node.getEnumConstants()));
            sb.append("; ");
        }
        if (node.getMembers() != null) {
            for (ASTNode member : node.getMembers()) {
                sb.append(format(member)).append(" ");
            }
        }
        sb.append("}");
        return sb.toString();
    }

    @Override
    public String visit(MethodDecl node) {
        StringBuilder sb = new StringBuilder();
        sb.append(decodeModifiers(node.getModifiers()));
        if (!node.getTypeParameters().isEmpty()) {
            sb.append("<").append(String.join(", ", node.getTypeParameters())).append("> ");
        }
        sb.append(node.getReturnType()).append(" ").append(node.getName());
        sb.append("(").append(joinParams(node.getParameters())).append(")");
        if (node.getThrowsList() != null && !node.getThrowsList().isEmpty()) {
            sb.append(" throws ").append(String.join(", ", node.getThrowsList()));
        }
        sb.append(" ");
        sb.append(formatBlock(node.getBody()));
        return sb.toString();
    }

    @Override
    public String visit(ConstructorDecl node) {
        StringBuilder sb = new StringBuilder();
        for (var ann : node.getAnnotations()) {
            sb.append(format(ann)).append(" ");
        }
        sb.append(decodeModifiers(node.getModifiers()));
        if (!node.getTypeParameters().isEmpty()) {
            sb.append("<").append(String.join(", ", node.getTypeParameters())).append("> ");
        }
        sb.append(node.getName());
        sb.append("(").append(joinParams(node.getParameters())).append(") ");
        sb.append(formatBlock(node.getBody()));
        return sb.toString();
    }

    @Override
    public String visit(FieldDecl node) {
        StringBuilder sb = new StringBuilder();
        sb.append(decodeModifiers(node.getModifiers()));
        sb.append(node.getTypeName()).append(" ").append(node.getName());
        if (node.getInitializer() != null) {
            sb.append(" = ").append(format(node.getInitializer()));
        }
        if (node.hasAdditionalVars()) {
            for (FieldDecl extra : node.getAdditionalVars()) {
                sb.append(", ").append(extra.getName());
                if (extra.getInitializer() != null) {
                    sb.append(" = ").append(format(extra.getInitializer()));
                }
            }
        }
        sb.append(";");
        return sb.toString();
    }

    @Override
    public String visit(ParamDecl node) {
        StringBuilder sb = new StringBuilder();
        for (var ann : node.getAnnotations()) {
            sb.append(format(ann)).append(" ");
        }
        if (node.isFinal()) sb.append("final ");
        sb.append(node.getTypeName()).append(" ").append(node.getName());
        if (node.getDefaultValue() != null) {
            sb.append(" = ").append(format(node.getDefaultValue()));
        }
        return sb.toString();
    }

    @Override
    public String visit(AnnotationVal node) {
        StringBuilder sb = new StringBuilder();
        sb.append("@").append(node.getName());
        Map<String, ASTNode> attrs = node.getAttributes();
        if (attrs != null && !attrs.isEmpty()) {
            sb.append("(");
            sb.append(attrs.entrySet().stream()
                    .map(e -> e.getKey() + " = " + format(e.getValue()))
                    .collect(Collectors.joining(", ")));
            sb.append(")");
        }
        return sb.toString();
    }
    
    @Override
    public String visit(StaticInitDecl node) {
        StringBuilder sb = new StringBuilder();
        sb.append("static ");
        sb.append(formatBlock(node.getStatements()));
        return sb.toString();
    }

    @Override
    public String visit(InstanceInitDecl node) {
        StringBuilder sb = new StringBuilder();
        sb.append(formatBlock(node.getStatements()));
        return sb.toString();
    }

    @Override
    public String visit(SafeFieldAccessExpr node) {
        return format(node.getTarget())  + "?." + node.getFieldName() + ";";
    }

    @Override
    public String visit(SafeMethodCallExpr node) {
        StringBuilder sb = new StringBuilder();
        sb.append(format(node.getTarget())).append("?.").append(node.getMethodName());
        if (!node.getTypeArguments().isEmpty()) {
            sb.append("<").append(String.join(", ", node.getTypeArguments())).append(">");
        }
        sb.append("(").append(join(node.getArguments(), ", ")).append(")");
        return sb.toString();
    }

    @Override
    public String visit(NewArrayExpr node) {
        StringBuilder sb = new StringBuilder();
        sb.append(node.getElementTypeName());
        for (ASTNode dim : node.getDimensions()) {
            sb.append("[").append(format(dim)).append("]");
        }
        if (node.hasInitializer())
            for (ASTNode init : node.getInitializer())
                sb.append(format(init));
        sb.append(";");

        return sb.toString();
    }

    @Override
    public String visit(SuperExpr node) {
        return "super";
    }

    @Override
    public String visit(SuperConstructorCallExpr node) {
        StringBuilder sb = new StringBuilder();
        sb.append("(").append(join(node.getArguments(), ", ")).append(")");
        return "super" + sb.toString();
    }



    // ========== 辅助方法 ==========

    private String join(List<ASTNode> nodes, String delimiter) {
        if (nodes == null || nodes.isEmpty()) {
            return "";
        }
        return nodes.stream()
                .map(ASTCodeFormatter::format)
                .collect(Collectors.joining(delimiter));
    }

    private String joinParams(List<ParamDecl> params) {
        if (params == null || params.isEmpty()) {
            return "";
        }
        return params.stream()
                .map(p -> format(p))
                .collect(Collectors.joining(", "));
    }

    private String formatBlock(ASTNode block) {
        if (block == null) {
            return "{}";
        }
        if (block instanceof BlockStmt) {
            return format(block);
        }
        return "{ " + format(block) + " }";
    }

    private String decodeModifiers(int modifiers) {
        if (modifiers == 0) {
            return "";
        }
        return Modifier.toString(modifiers) + " ";
    }

    private String escapeString(String s) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '\\' -> sb.append("\\\\");
                case '"' -> sb.append("\\\"");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                case '\b' -> sb.append("\\b");
                case '\f' -> sb.append("\\f");
                default -> sb.append(c);
            }
        }
        return sb.toString();
    }

    private String escapeChar(char c) {
        return switch (c) {
            case '\\' -> "\\\\";
            case '\'' -> "\\'";
            case '\n' -> "\\n";
            case '\r' -> "\\r";
            case '\t' -> "\\t";
            case '\b' -> "\\b";
            case '\f' -> "\\f";
            default -> String.valueOf(c);
        };
    }
}
