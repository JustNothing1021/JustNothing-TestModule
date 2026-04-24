package com.justnothing.javainterpreter.evaluator;

import com.justnothing.javainterpreter.api.ClassResolver;
import com.justnothing.javainterpreter.ast.ASTNode;
import com.justnothing.javainterpreter.ast.nodes.*;
import com.justnothing.javainterpreter.exception.EvaluationException;
import com.justnothing.javainterpreter.exception.ReturnException;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class MethodBodyExecutor {
    
    private static final Map<String, MethodInfo> methodRegistry = new ConcurrentHashMap<>();
    private static final ThreadLocal<String> currentClassContext = new ThreadLocal<>();
    private static final ThreadLocal<Object> currentInstanceContext = new ThreadLocal<>();
    
    public static String getCurrentClassContext() {
        return currentClassContext.get();
    }
    
    public static void setCurrentClassContext(String className) {
        currentClassContext.set(className);
    }
    
    public static void clearCurrentClassContext() {
        currentClassContext.remove();
    }
    
    public static Object getCurrentInstanceContext() {
        return currentInstanceContext.get();
    }
    
    public static void setCurrentInstanceContext(Object instance) {
        currentInstanceContext.set(instance);
    }
    
    public static void clearCurrentInstanceContext() {
        currentInstanceContext.remove();
    }
    
    public static void registerMethod(String classKey, String methodName, 
                                       MethodDeclarationNode methodDecl, 
                                       ExecutionContext creationContext) {
        String fullKey = classKey + "#" + methodName + getParameterSignature(methodDecl.getParameters());
        MethodInfo info = new MethodInfo(methodDecl, creationContext);
        methodRegistry.put(fullKey, info);
    }
    
    public static Object executeMethod(String classKey, String methodName, 
                                        Object instance, Object[] args) {
        MethodInfo info = findMethod(classKey, methodName, args);
        if (info == null) {
            throw new RuntimeException("Method not found: " + classKey + "#" + methodName);
        }
        
        String previousContext = currentClassContext.get();
        Object previousInstance = currentInstanceContext.get();
        currentClassContext.set(classKey);
        if (instance != null) {
            currentInstanceContext.set(instance);
        }
        try {
            return executeMethodBody(info, instance, args);
        } finally {
            if (previousContext != null) {
                currentClassContext.set(previousContext);
            } else {
                currentClassContext.remove();
            }
            if (previousInstance != null) {
                currentInstanceContext.set(previousInstance);
            } else {
                currentInstanceContext.remove();
            }
        }
    }
    
    private static MethodInfo findMethod(String classKey, String methodName, Object[] args) {
        String baseKey = classKey + "#" + methodName;
        
        for (Map.Entry<String, MethodInfo> entry : methodRegistry.entrySet()) {
            if (entry.getKey().startsWith(baseKey)) {
                MethodInfo info = entry.getValue();
                if (isCompatible(info.methodDecl.getParameters(), args)) {
                    return info;
                }
            }
        }
        
        return null;
    }
    
    private static boolean isCompatible(List<ParameterNode> params, Object[] args) {
        int paramCount = params != null ? params.size() : 0;
        int argCount = args != null ? args.length : 0;
        return paramCount == argCount;
    }
    
    private static Object executeMethodBody(MethodInfo info, Object instance, Object[] args) {
        MethodDeclarationNode methodDecl = info.methodDecl;
        ASTNode body = methodDecl.getBody();
        
        if (body == null) {
            return getDefaultValue(methodDecl.getReturnType().getTypeName());
        }
        
        ExecutionContext context = info.creationContext.createChildContext();
        
        context.getScopeManager().declareVariable("this", 
            instance != null ? instance.getClass() : Object.class, instance, false);
        
        Set<String> paramNames = new HashSet<>();
        List<ParameterNode> parameters = methodDecl.getParameters();
        if (parameters != null && args != null) {
            for (int i = 0; i < parameters.size() && i < args.length; i++) {
                ParameterNode param = parameters.get(i);
                Object argValue = args[i];
                paramNames.add(param.getParameterName());
                context.getScopeManager().declareVariable(
                    param.getParameterName(), 
                    Object.class,
                    argValue, 
                    false
                );
            }
        }
        
        Class<?> fieldSourceClass = null;
        if (instance != null) {
            fieldSourceClass = instance.getClass();
        } else {
            String className = getCurrentClassContext();
            if (className != null) {
                if (info.creationContext.hasCustomClass(className)) {
                    fieldSourceClass = info.creationContext.getCustomClass(className);
                } else {
                    fieldSourceClass = ClassResolver.findClassWithImports(
                        className, info.creationContext.getClassLoader(), info.creationContext.getImports());
                }
            }
        }
        
        if (fieldSourceClass != null) {
            try {
                Class<?> clazz = fieldSourceClass;
                while (clazz != null && clazz != Object.class) {
                    Field[] fields = clazz.getDeclaredFields();
                    for (Field field : fields) {
                        field.setAccessible(true);
                        String fieldName = field.getName();
                        if (paramNames.contains(fieldName)) continue;
                        Object fieldValue = null;
                        if (instance != null) {
                            fieldValue = field.get(instance);
                        } else if (Modifier.isStatic(field.getModifiers())) {
                            fieldValue = field.get(null);
                        }
                        context.getScopeManager().declareVariable(
                            fieldName, 
                            field.getType(),
                            fieldValue, 
                            false
                        );
                    }
                    clazz = clazz.getSuperclass();
                }
            } catch (Exception e) {
            }
        }
        
        try {
            Object result = evaluateWithFieldResolution(body, context, instance);
            return result;
        } catch (ReturnException e) {
            return e.getValue();
        } catch (EvaluationException e) {
            throw new RuntimeException("Method execution failed: " + e.getMessage(), e);
        }
    }
    
    private static Object evaluateWithFieldResolution(ASTNode node, ExecutionContext context, Object instance) 
            throws EvaluationException {
        if (node instanceof VariableNode varNode) {
            ScopeManager.Variable scopeVar = context.getScopeManager().getVariable(varNode.getName());
            if (scopeVar != null && !varNode.getName().equals("this")) {
                Object scopeValue = scopeVar.getValue();
                if (scopeValue != null) {
                    return scopeValue;
                }
            }
            Object fieldValue = null;
            if (instance != null) {
                fieldValue = resolveField(varNode.getName(), instance);
            }
            if (fieldValue == null) {
                ScopeManager.Variable thisVar = context.getScopeManager().getVariable("this");
                if (thisVar != null) {
                    Object thisInstance = thisVar.getValue();
                    if (thisInstance != null) {
                        fieldValue = resolveField(varNode.getName(), thisInstance);
                    }
                }
            }
            if (fieldValue == null && instance == null) {
                // 尝试访问静态字段
                String className = getCurrentClassContext();
                if (className != null) {
                    Class<?> targetClass = null;
                    if (context.hasCustomClass(className)) {
                        targetClass = context.getCustomClass(className);
                    } else {
                        targetClass = ClassResolver.findClassWithImports(
                            className, context.getClassLoader(), context.getImports());
                    }
                    if (targetClass != null) {
                        try {
                            Field field = findField(targetClass, varNode.getName());
                            if (field != null && Modifier.isStatic(field.getModifiers())) {
                                field.setAccessible(true);
                                fieldValue = field.get(null);
                            }
                        } catch (Exception e) {
                        }
                    }
                }
            }
            if (fieldValue != null) {
                return fieldValue;
            }
            return ASTEvaluator.evaluate(node, context);
        }
        
        if (node instanceof FieldAccessNode fieldNode) {
            ASTNode targetNode = fieldNode.getTarget();
            if (targetNode instanceof VariableNode varNode && varNode.getName().equals("this")) {
                // 处理 this.field 形式的字段访问
                Object fieldValue = null;
                // 首先尝试使用传入的 instance 参数
                if (instance != null) {
                    fieldValue = resolveField(fieldNode.getFieldName(), instance);
                }
                // 如果从 instance 参数中解析失败，尝试从 context 中获取 this 变量
                if (fieldValue == null) {
                    ScopeManager.Variable thisVar = context.getScopeManager().getVariable("this");
                    if (thisVar != null) {
                        Object thisInstance = thisVar.getValue();
                        if (thisInstance != null) {
                            fieldValue = resolveField(fieldNode.getFieldName(), thisInstance);
                        }
                    }
                }
                if (fieldValue != null) {
                    return fieldValue;
                }
            }
            // 如果字段解析失败，使用默认的字段访问逻辑
            return ASTEvaluator.evaluate(node, context);
        }
        
        if (node instanceof FieldAssignmentNode fieldNode) {
            ASTNode targetNode = fieldNode.getTarget();
            if (targetNode instanceof VariableNode varNode && varNode.getName().equals("this")) {
                Object value = evaluateWithFieldResolution(fieldNode.getValue(), context, instance);
                if (instance != null) {
                    try {
                        Field field = findField(instance.getClass(), fieldNode.getFieldName());
                        if (field != null) {
                            field.setAccessible(true);
                            field.set(instance, value);
                            return value;
                        }
                    } catch (Exception e) {
                    }
                }
                ScopeManager.Variable thisVar = context.getScopeManager().getVariable("this");
                if (thisVar != null) {
                    Object thisInstance = thisVar.getValue();
                    if (thisInstance != null) {
                        try {
                            Field field = findField(thisInstance.getClass(), fieldNode.getFieldName());
                            if (field != null) {
                                field.setAccessible(true);
                                field.set(thisInstance, value);
                                return value;
                            }
                        } catch (Exception e) {
                        }
                    }
                }
                if (instance == null) {
                    String className = getCurrentClassContext();
                    if (className != null) {
                        Class<?> targetClass = null;
                        if (context.hasCustomClass(className)) {
                            targetClass = context.getCustomClass(className);
                        } else {
                            targetClass = ClassResolver.findClassWithImports(
                                className, context.getClassLoader(), context.getImports());
                        }
                        if (targetClass != null) {
                            try {
                                Field field = findField(targetClass, fieldNode.getFieldName());
                                if (field != null && Modifier.isStatic(field.getModifiers())) {
                                    field.setAccessible(true);
                                    field.set(null, value);
                                    return value;
                                }
                            } catch (Exception e) {
                            }
                        }
                    }
                }
            }
            return ASTEvaluator.evaluate(node, context);
        }
        
        if (node instanceof UnaryOpNode unaryNode) {
            // 尝试处理递增和递减操作
            UnaryOpNode.Operator op = unaryNode.getOperator();
            if (op == UnaryOpNode.Operator.PRE_INCREMENT || op == UnaryOpNode.Operator.PRE_DECREMENT ||
                op == UnaryOpNode.Operator.POST_INCREMENT || op == UnaryOpNode.Operator.POST_DECREMENT) {
                ASTNode operand = unaryNode.getOperand();
                if (operand instanceof VariableNode varNode) {
                    // 处理 count++ 或 count-- 形式的字段访问
                    String fieldName = varNode.getName();
                    // 首先尝试使用传入的 instance 参数
                    if (instance != null) {
                        try {
                            Field field = findField(instance.getClass(), fieldName);
                            if (field != null) {
                                field.setAccessible(true);
                                Object currentValue = field.get(instance);
                                Object newValue;
                                if (op == UnaryOpNode.Operator.PRE_INCREMENT || op == UnaryOpNode.Operator.POST_INCREMENT) {
                                    if (currentValue instanceof Integer) {
                                        newValue = (Integer) currentValue + 1;
                                    } else if (currentValue instanceof Long) {
                                        newValue = (Long) currentValue + 1;
                                    } else if (currentValue instanceof Float) {
                                        newValue = (Float) currentValue + 1;
                                    } else if (currentValue instanceof Double) {
                                        newValue = (Double) currentValue + 1;
                                    } else if (currentValue instanceof Short) {
                                        newValue = (Short) currentValue + 1;
                                    } else if (currentValue instanceof Byte) {
                                        newValue = (Byte) currentValue + 1;
                                    } else {
                                        newValue = ((Number) currentValue).longValue() + 1;
                                    }
                                } else {
                                    if (currentValue instanceof Integer) {
                                        newValue = (Integer) currentValue - 1;
                                    } else if (currentValue instanceof Long) {
                                        newValue = (Long) currentValue - 1;
                                    } else if (currentValue instanceof Float) {
                                        newValue = (Float) currentValue - 1;
                                    } else if (currentValue instanceof Double) {
                                        newValue = (Double) currentValue - 1;
                                    } else if (currentValue instanceof Short) {
                                        newValue = (Short) currentValue - 1;
                                    } else if (currentValue instanceof Byte) {
                                        newValue = (Byte) currentValue - 1;
                                    } else {
                                        newValue = ((Number) currentValue).longValue() - 1;
                                    }
                                }
                                field.set(instance, newValue);
                                return op == UnaryOpNode.Operator.POST_INCREMENT || op == UnaryOpNode.Operator.POST_DECREMENT ? currentValue : newValue;
                            }
                        } catch (Exception e) {
                            // 字段访问失败，继续使用默认的字段访问逻辑
                        }
                    }
                    // 如果从 instance 参数中解析失败，尝试从 context 中获取 this 变量
                    if (instance == null) {
                        ScopeManager.Variable thisVar = context.getScopeManager().getVariable("this");
                        if (thisVar != null) {
                            Object thisInstance = thisVar.getValue();
                            if (thisInstance != null) {
                                try {
                                    Field field = findField(thisInstance.getClass(), fieldName);
                                    if (field != null) {
                                        field.setAccessible(true);
                                        Object currentValue = field.get(thisInstance);
                                        Object newValue;
                                        if (op == UnaryOpNode.Operator.PRE_INCREMENT || op == UnaryOpNode.Operator.POST_INCREMENT) {
                                            if (currentValue instanceof Integer) {
                                                newValue = (Integer) currentValue + 1;
                                            } else if (currentValue instanceof Long) {
                                                newValue = (Long) currentValue + 1;
                                            } else if (currentValue instanceof Float) {
                                                newValue = (Float) currentValue + 1;
                                            } else if (currentValue instanceof Double) {
                                                newValue = (Double) currentValue + 1;
                                            } else if (currentValue instanceof Short) {
                                                newValue = (Short) currentValue + 1;
                                            } else if (currentValue instanceof Byte) {
                                                newValue = (Byte) currentValue + 1;
                                            } else {
                                                newValue = ((Number) currentValue).longValue() + 1;
                                            }
                                        } else {
                                            if (currentValue instanceof Integer) {
                                                newValue = (Integer) currentValue - 1;
                                            } else if (currentValue instanceof Long) {
                                                newValue = (Long) currentValue - 1;
                                            } else if (currentValue instanceof Float) {
                                                newValue = (Float) currentValue - 1;
                                            } else if (currentValue instanceof Double) {
                                                newValue = (Double) currentValue - 1;
                                            } else if (currentValue instanceof Short) {
                                                newValue = (Short) currentValue - 1;
                                            } else if (currentValue instanceof Byte) {
                                                newValue = (Byte) currentValue - 1;
                                            } else {
                                                newValue = ((Number) currentValue).longValue() - 1;
                                            }
                                        }
                                        field.set(thisInstance, newValue);
                                        return op == UnaryOpNode.Operator.POST_INCREMENT || op == UnaryOpNode.Operator.POST_DECREMENT ? currentValue : newValue;
                                    }
                                } catch (Exception e) {
                                    // 字段访问失败，继续使用默认的字段访问逻辑
                                }
                            }
                        }
                    }
                }
            }
            // 递归处理操作数
            ASTNode operand = unaryNode.getOperand();
            Object operandValue = evaluateWithFieldResolution(operand, context, instance);
            // 如果字段解析失败，使用默认的字段访问逻辑
            return ASTEvaluator.evaluate(node, context);
        }
        
        if (node instanceof FunctionCallNode funcCall) {
            String funcName = funcCall.getFunctionName();
            if (instance != null) {
                Object[] argValues = new Object[funcCall.getArguments().size()];
                for (int i = 0; i < funcCall.getArguments().size(); i++) {
                    argValues[i] = evaluateWithFieldResolution(funcCall.getArguments().get(i), context, instance);
                }
                
                try {
                    String instanceClassName = instance.getClass().getSimpleName();
                    return executeMethod(instanceClassName, funcName, instance, argValues);
                } catch (RuntimeException e) {
                    if (e.getMessage() != null && e.getMessage().startsWith("Method not found")) {
                        try {
                            Method method = ASTEvaluator.findMethod(instance.getClass(), funcName, argValues);
                            if (method != null) {
                                method.setAccessible(true);
                                return method.invoke(instance, argValues);
                            }
                        } catch (Exception ex) {
                            // 反射调用也失败，继续使用默认的函数调用逻辑
                        }
                    } else {
                        throw e;
                    }
                }
            }
            return ASTEvaluator.evaluate(node, context);
        }
        
        if (node instanceof ReturnNode returnNode) {
            Object value = null;
            if (returnNode.getValue() != null) {
                value = evaluateWithFieldResolution(returnNode.getValue(), context, instance);
            }
            throw new ReturnException(value);
        }
        
        if (node instanceof AssignmentNode assignmentNode) {
            String varName = assignmentNode.getVariableName();
            Object value = evaluateWithFieldResolution(assignmentNode.getValue(), context, instance);
            
            // 检查是否是实例字段
            boolean isField = false;
            if (instance != null) {
                Field field = findField(instance.getClass(), varName);
                if (field != null) {
                    isField = true;
                    try {
                        field.setAccessible(true);
                        field.set(instance, value);
                    } catch (Exception e) {
                        // 字段设置失败，继续使用默认的赋值逻辑
                    }
                }
            }
            
            // 如果不是实例字段，或者字段设置失败，使用默认的赋值逻辑
            if (!isField) {
                if (assignmentNode.isDeclaration()) {
                    Class<?> declaredType = assignmentNode.getDeclaredClass() != null ? assignmentNode.getDeclaredClass() : Object.class;
                    context.getScopeManager().declareVariable(varName, declaredType, value, assignmentNode.isFinal());
                } else {
                    context.getScopeManager().setVariable(varName, value);
                }
            }
            
            return value;
        }
        
        if (node instanceof BlockNode blockNode) {
            context.getScopeManager().enterScope();
            try {
                Object result = null;
                for (ASTNode stmt : blockNode.getStatements()) {
                    result = evaluateWithFieldResolution(stmt, context, instance);
                }
                return result;
            } finally {
                context.getScopeManager().exitScope();
            }
        }
        
        return ASTEvaluator.evaluate(node, context);
    }
    
    public static Object resolveField(String fieldName, Object instance) {
        if (instance == null) {
            return null;
        }
        try {
            // 尝试使用反射直接获取字段，包括从父类继承的字段
            Field field = findField(instance.getClass(), fieldName);
            if (field != null) {
                field.setAccessible(true);
                return field.get(instance);
            }
        } catch (Exception e) {
            // 字段访问失败，返回 null 让调用者尝试其他方法
            return null;
        }
        // 字段未找到，返回 null 让调用者尝试其他方法
        return null;
    }
    
    public static Field findField(Class<?> clazz, String fieldName) {
        try {
            // 首先尝试在当前类中查找字段
            Field field = clazz.getDeclaredField(fieldName);
            return field;
        } catch (NoSuchFieldException e) {
            // 如果当前类没有该字段，尝试在父类中查找
            Class<?> superClass = clazz.getSuperclass();
            if (superClass != null && superClass != Object.class) {
                return findField(superClass, fieldName);
            }
            return null;
        }
    }
    
    private static Object getDefaultValue(String returnTypeName) {
        if (returnTypeName == null || returnTypeName.equals("void")) {
            return null;
        }
        return switch (returnTypeName) {
            case "int", "byte", "short", "char" -> 0;
            case "long" -> 0L;
            case "float" -> 0.0f;
            case "double" -> 0.0;
            case "boolean" -> false;
            default -> null;
        };
    }
    
    private static String getParameterSignature(List<ParameterNode> parameters) {
        if (parameters == null || parameters.isEmpty()) {
            return "()";
        }
        StringBuilder sb = new StringBuilder("(");
        for (ParameterNode param : parameters) {
            sb.append(param.getType().getTypeName() != null ? param.getType().getTypeName() : "Object");
            sb.append(",");
        }
        sb.append(")");
        return sb.toString();
    }
    
    private static class MethodInfo {
        final MethodDeclarationNode methodDecl;
        final ExecutionContext creationContext;
        
        MethodInfo(MethodDeclarationNode methodDecl, ExecutionContext context) {
            this.methodDecl = methodDecl;
            this.creationContext = context;
        }
    }
}
