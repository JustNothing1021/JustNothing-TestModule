package com.justnothing.testmodule.command.functions.script_new.ast.nodes;

import com.justnothing.testmodule.command.functions.script_new.ast.ASTNode;
import com.justnothing.testmodule.command.functions.script_new.ast.SourceLocation;
import com.justnothing.testmodule.command.functions.script_new.ast.visitor.ASTVisitor;

/**
 * 二元操作节点
 * <p>
 * 表示二元操作，如加减乘除、比较、逻辑运算等。
 * </p>
 * 
 * @author JustNothing1021
 * @since 1.0.0
 */
public class BinaryOpNode extends ASTNode {
    
    /**
     * 二元操作符
     */
    public enum Operator {
        ADD("+"),
        SUBTRACT("-"),
        MULTIPLY("*"),
        DIVIDE("/"),
        MODULO("%"),
        
        EQUAL("=="),
        NOT_EQUAL("!="),
        LESS_THAN("<"),
        LESS_THAN_OR_EQUAL("<="),
        GREATER_THAN(">"),
        GREATER_THAN_OR_EQUAL(">="),
        
        LOGICAL_AND("&&"),
        LOGICAL_OR("||"),
        
        BITWISE_AND("&"),
        BITWISE_OR("|"),
        BITWISE_XOR("^"),
        LEFT_SHIFT("<<"),
        RIGHT_SHIFT(">>"),
        UNSIGNED_RIGHT_SHIFT(">>>");
        
        private final String symbol;
        
        Operator(String symbol) {
            this.symbol = symbol;
        }
        
        public String getSymbol() {
            return symbol;
        }
    }
    
    private final Operator operator;
    private final ASTNode left;
    private final ASTNode right;
    
    public BinaryOpNode(Operator operator, ASTNode left, ASTNode right, SourceLocation location) {
        super(location);
        this.operator = operator;
        this.left = left;
        this.right = right;
    }
    
    public Operator getOperator() {
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
    public String formatString(int indent) {
        StringBuilder sb = new StringBuilder();
        sb.append(indent(indent)).append("BinaryOpNode\n");
        sb.append(indent(indent + 1)).append("operator: ").append(operator.getSymbol()).append("\n");
        sb.append(indent(indent + 1)).append("left:\n");
        sb.append(left.formatString(indent + 2)).append("\n");
        sb.append(indent(indent + 1)).append("right:\n");
        sb.append(right.formatString(indent + 2)).append("\n");
        return sb.toString().strip();
    }
    
    public static class Builder extends ASTNode.Builder<Builder> {
        private Operator operator;
        private ASTNode left;
        private ASTNode right;
        
        public Builder operator(Operator operator) {
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
        
        @Override
        public BinaryOpNode build() {
            return new BinaryOpNode(operator, left, right, location);
        }
    }
}
