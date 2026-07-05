package com.justnothing.engine.v2.ast.expr;

import com.justnothing.engine.v2.ast.ASTNode;
import com.justnothing.engine.v2.ast.ASTVisitor;

import java.util.List;

import static com.justnothing.engine.v2.ast.OperatorSymbol.*;

public class BinaryOpExpr extends ASTNode {

    private final String operator;
    private final ASTNode left;
    private final ASTNode right;

    private BinaryOpExpr(Builder builder) {
        super(builder.getLocation());
        this.operator = builder.operator;
        this.left = builder.left;
        this.right = builder.right;
    }

    public String getOperator() {
        return operator;
    }

    public ASTNode getLeft() {
        return left;
    }

    public ASTNode getRight() {
        return right;
    }

    @Override
    public <T> T accept(ASTVisitor<T> visitor) {
        return visitor.visit(this);
    }

    @Override
    public String nodeName() {
        return "BinaryOp";
    }

    @Override
    public List<ASTNode> getChildren() {
        return ASTNode.children(left, right);
    }

    public static class Builder extends ASTNode.Builder<Builder> {
        private String operator;
        private ASTNode left;
        private ASTNode right;

        public Builder operator(String operator) {
            this.operator = operator;
            return this;
        }

        public Builder left(ASTNode left) {
            this.left = left;
            return this;
        }

        public Builder right(ASTNode right) {
            this.right = right;
            return this;
        }

        public ASTNode build() {
            if (left instanceof LiteralExpr && right instanceof LiteralExpr) {
                ASTNode folded = tryConstantFold();
                if (folded != null) {
                    return folded;
                }
            }
            return new BinaryOpExpr(this);
        }

        private ASTNode tryConstantFold() {
            LiteralExpr leftLit = (LiteralExpr) left;
            LiteralExpr rightLit = (LiteralExpr) right;
            Class<?> leftType = leftLit.getType();
            Class<?> rightType = rightLit.getType();

            if (leftType != rightType) {
                return null;
            }

            Class<?> type = leftType;

            if (type == Integer.class) {
                return foldInteger(leftLit, rightLit);
            } else if (type == Long.class) {
                return foldLong(leftLit, rightLit);
            } else if (type == Double.class) {
                return foldDouble(leftLit, rightLit);
            } else if (type == Float.class) {
                return foldFloat(leftLit, rightLit);
            } else if (type == Boolean.class) {
                return foldBoolean(leftLit, rightLit);
            }

            return null;
        }

        private ASTNode foldInteger(LiteralExpr leftLit, LiteralExpr rightLit) {
            int l = (Integer) leftLit.getValue();
            int r = (Integer) rightLit.getValue();
            Object result;

            switch (operator) {
                case ADD:                   result = l + r; break;
                case SUBTRACT:             result = l - r; break;
                case MULTIPLY:             result = l * r; break;
                case DIVIDE:               if (r == 0) return null; result = l / r; break;
                case MODULO:               if (r == 0) return null; result = l % r; break;
                case EQUAL:                result = l == r; break;
                case NOT_EQUAL:            result = l != r; break;
                case LESS_THAN:            result = l < r; break;
                case GREATER_THAN:         result = l > r; break;
                case LESS_THAN_OR_EQUAL:   result = l <= r; break;
                case GREATER_THAN_OR_EQUAL:result = l >= r; break;
                default:                   return null;
            }

            return new LiteralExpr.Builder()
                    .value(result)
                    .type(result.getClass())
                    .location(location != null ? location : left.getLocation())
                    .build();
        }

        private ASTNode foldLong(LiteralExpr leftLit, LiteralExpr rightLit) {
            long l = (Long) leftLit.getValue();
            long r = (Long) rightLit.getValue();
            Object result;

            switch (operator) {
                case ADD:                   result = l + r; break;
                case SUBTRACT:             result = l - r; break;
                case MULTIPLY:             result = l * r; break;
                case DIVIDE:               if (r == 0L) return null; result = l / r; break;
                case MODULO:               if (r == 0L) return null; result = l % r; break;
                case EQUAL:                result = l == r; break;
                case NOT_EQUAL:            result = l != r; break;
                case LESS_THAN:            result = l < r; break;
                case GREATER_THAN:         result = l > r; break;
                case LESS_THAN_OR_EQUAL:   result = l <= r; break;
                case GREATER_THAN_OR_EQUAL:result = l >= r; break;
                default:                   return null;
            }

            return new LiteralExpr.Builder()
                    .value(result)
                    .type(result.getClass())
                    .location(location != null ? location : left.getLocation())
                    .build();
        }

        private ASTNode foldDouble(LiteralExpr leftLit, LiteralExpr rightLit) {
            double l = (Double) leftLit.getValue();
            double r = (Double) rightLit.getValue();
            Object result;

            switch (operator) {
                case ADD:                   result = l + r; break;
                case SUBTRACT:             result = l - r; break;
                case MULTIPLY:             result = l * r; break;
                case DIVIDE:               result = l / r; break;
                case MODULO:               result = l % r; break;
                case EQUAL:                result = l == r; break;
                case NOT_EQUAL:            result = l != r; break;
                case LESS_THAN:            result = l < r; break;
                case GREATER_THAN:         result = l > r; break;
                case LESS_THAN_OR_EQUAL:   result = l <= r; break;
                case GREATER_THAN_OR_EQUAL:result = l >= r; break;
                default:                   return null;
            }

            return new LiteralExpr.Builder()
                    .value(result)
                    .type(result.getClass())
                    .location(location != null ? location : left.getLocation())
                    .build();
        }

        private ASTNode foldFloat(LiteralExpr leftLit, LiteralExpr rightLit) {
            float l = (Float) leftLit.getValue();
            float r = (Float) rightLit.getValue();
            Object result;

            switch (operator) {
                case ADD:                   result = l + r; break;
                case SUBTRACT:             result = l - r; break;
                case MULTIPLY:             result = l * r; break;
                case DIVIDE:               result = l / r; break;
                case MODULO:               result = l % r; break;
                case EQUAL:                result = l == r; break;
                case NOT_EQUAL:            result = l != r; break;
                case LESS_THAN:            result = l < r; break;
                case GREATER_THAN:         result = l > r; break;
                case LESS_THAN_OR_EQUAL:   result = l <= r; break;
                case GREATER_THAN_OR_EQUAL:result = l >= r; break;
                default:                   return null;
            }

            return new LiteralExpr.Builder()
                    .value(result)
                    .type(result.getClass())
                    .location(location != null ? location : left.getLocation())
                    .build();
        }

        private ASTNode foldBoolean(LiteralExpr leftLit, LiteralExpr rightLit) {
            boolean l = (Boolean) leftLit.getValue();
            boolean r = (Boolean) rightLit.getValue();
            Object result;

            switch (operator) {
                case LOGICAL_AND:  result = l && r; break;
                case LOGICAL_OR:   result = l || r; break;
                case EQUAL:        result = l == r; break;
                case NOT_EQUAL:    result = l != r; break;
                default:           return null;
            }

            return new LiteralExpr.Builder()
                    .value(result)
                    .type(Boolean.class)
                    .location(location != null ? location : left.getLocation())
                    .build();
        }
    }
}
