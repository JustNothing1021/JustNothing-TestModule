package com.justnothing.testmodule.command.functions.script_new.evaluator;

import com.justnothing.testmodule.command.functions.script_new.exception.EvaluationException;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;

/**
 * 作用域管理器
 * <p>
 * 管理变量的作用域，支持层级作用域和变量查找。
 * 使用栈结构管理作用域，进入新作用域时压栈，退出时弹栈。
 * </p>
 * 
 * @author JustNothing1021
 * @since 1.0.0
 */
public class ScopeManager {
    
    /**
     * 变量信息
     */
    public static class Variable {
        private final String name;
        private final Class<?> type;
        private Object value;
        private final boolean isFinal;
        private final int scopeLevel;
        
        public Variable(String name, Class<?> type, Object value, boolean isFinal, int scopeLevel) {
            this.name = name;
            this.type = type;
            this.value = value;
            this.isFinal = isFinal;
            this.scopeLevel = scopeLevel;
        }
        
        public String getName() {
            return name;
        }
        
        public Class<?> getType() {
            return type;
        }
        
        public Object getValue() {
            return value;
        }
        
        public void setValue(Object value) {
            this.value = value;
        }
        
        public boolean isFinal() {
            return isFinal;
        }
        
        public int getScopeLevel() {
            return scopeLevel;
        }
    }
    
    /**
     * 作用域
     */
    private static class Scope {
        private final Map<String, Variable> variables;
        private final int level;
        
        public Scope(int level) {
            this.variables = new HashMap<>();
            this.level = level;
        }
        
        public Map<String, Variable> getVariables() {
            return variables;
        }
        
        public int getLevel() {
            return level;
        }
    }
    
    private final Deque<Scope> scopeStack;
    private int currentLevel;
    
    public ScopeManager() {
        this.scopeStack = new ArrayDeque<>();
        this.currentLevel = 0;
        enterScope();  // 创建全局作用域
    }
    
    /**
     * 进入新作用域
     */
    public void enterScope() {
        currentLevel++;
        scopeStack.push(new Scope(currentLevel));
    }
    
    /**
     * 退出当前作用域
     */
    public void exitScope() {
        if (scopeStack.size() > 1) {
            scopeStack.pop();
            currentLevel--;
        } else {
            throw new IllegalStateException("Cannot exit global scope");
        }
    }
    
    /**
     * 声明变量
     * 
     * @param name 变量名
     * @param type 变量类型
     * @param value 初始值
     * @param isFinal 是否是final
     */
    public void declareVariable(String name, Class<?> type, Object value, boolean isFinal) {
        Scope current = scopeStack.peek();
        if (current.getVariables().containsKey(name)) {
            throw new EvaluationException(
                "Variable already declared in current scope: " + name,
                0,
                0,
                com.justnothing.testmodule.command.functions.script_new.exception.ErrorCode.SCOPE_VARIABLE_ALREADY_DECLARED);
        }
        
        Variable variable = new Variable(name, type, value, isFinal, currentLevel);
        current.getVariables().put(name, variable);
    }
    
    /**
     * 获取变量
     * 
     * @param name 变量名
     * @return 变量
     */
    public Variable getVariable(String name) {
        for (Scope scope : scopeStack) {
            Variable variable = scope.getVariables().get(name);
            if (variable != null) {
                return variable;
            }
        }
        
        throw new EvaluationException(
            "Variable not found: " + name,
            0,
            0,
            com.justnothing.testmodule.command.functions.script_new.exception.ErrorCode.SCOPE_VARIABLE_NOT_FOUND);
    }
    
    /**
     * 设置变量值
     * 
     * @param name 变量名
     * @param value 新值
     */
    public void setVariable(String name, Object value) {
        Variable variable = getVariable(name);
        
        if (variable.isFinal()) {
            throw new EvaluationException(
                "Cannot assign to final variable: " + name,
                0,
                0,
                com.justnothing.testmodule.command.functions.script_new.exception.ErrorCode.SCOPE_CANNOT_ASSIGN_TO_FINAL);
        }
        
        variable.setValue(value);
    }
    
    /**
     * 检查变量是否存在
     * 
     * @param name 变量名
     * @return 是否存在
     */
    public boolean hasVariable(String name) {
        for (Scope scope : scopeStack) {
            if (scope.getVariables().containsKey(name)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * 获取当前作用域层级
     * 
     * @return 当前层级
     */
    public int getCurrentLevel() {
        return currentLevel;
    }
    
    /**
     * 获取当前作用域中的变量数量
     * 
     * @return 变量数量
     */
    public int getCurrentScopeVariableCount() {
        return scopeStack.peek().getVariables().size();
    }
}
