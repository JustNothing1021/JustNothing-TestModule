package com.justnothing.testmodule.command.functions.script;


import com.justnothing.testmodule.constants.AppEnvironment;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import com.justnothing.testmodule.utils.reflect.ReflectionUtils;

public class ScriptUtils {


    public static boolean toBoolean(Object obj) {
        if (obj == null)
            return false;
        if (obj instanceof Boolean)
            return (Boolean) obj;
        if (obj instanceof Number)
            return ((Number) obj).doubleValue() != 0;
        if (obj instanceof String)
            return !((String) obj).isEmpty();
        if (obj instanceof Character)
            return ((Character) obj) != 0;
        return true;
    }

    static String repeat(String str, int times) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < times; i++) {
            sb.append(str);
        }
        return sb.toString();
    }

    public static String getStackTraceString(Throwable th) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        th.printStackTrace(pw);
        return sw.toString();
    }


    static boolean isStandaloneMode() {
        return !AppEnvironment.isAndroidEnv();
    }


    static boolean isExactTypeMatch(Class<?> expected, Class<?> actual) {
        return expected == actual;
    }



    static String getArrayTypeNameWithoutLength(String typeName) {
        boolean insideBracket = false;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < typeName.length(); i++) {
            if (typeName.charAt(i) == '[') {
                insideBracket = true;
                sb.append('[');
            } else if (typeName.charAt(i) == ']') {
                insideBracket = false;
                sb.append(']');
            } else if (!insideBracket)
                sb.append(typeName.charAt(i));
        }
        return sb.toString();
    }

    static final Set<String> KEYWORDS = Set.of(
            "public", "private", "protected", "static", "final", "abstract",
            "class", "interface", "enum", "void", "int", "long", "short",
            "byte", "char", "float", "double", "boolean", "true", "false",
            "if", "else", "for", "while", "do", "switch", "case", "default",
            "break", "continue", "return", "new", "this", "super",
            "null", "try", "catch", "finally", "throw", "throws",
            "auto" // auto类型java没有，但是也算
    );

    static final Set<String> ACCESS_MODIFIERS = Set.of(
            "public", "private", "protected");

    static final Set<String> TYPES = Set.of(
            "void", "int", "long", "short", "byte", "char", "float", "double", "boolean");

    static boolean isAccessModifier(String word) {
        return word != null && ACCESS_MODIFIERS.contains(word);
    }

    static boolean isKeyword(String word) {
        return word != null && KEYWORDS.contains(word);
    }

    static boolean isTypeOrVoid(String word) {
        if (word == null || word.isEmpty())
            return false;
        if (TYPES.contains(word))
            return true;
        return Character.isUpperCase(word.charAt(0));
    }


    public static ScriptModels.MethodDefinition findMethodInHierarchy(ScriptModels.ClassDefinition classDef, String methodName,
                                                                      ScriptModels.ExecutionContext context) {
        return findMethodInHierarchy(classDef, methodName, context, null);
    }

    public static ScriptModels.MethodDefinition findMethodInHierarchy(ScriptModels.ClassDefinition classDef, String methodName,
                                                                      ScriptModels.ExecutionContext context,
                                                                      List<Class<?>> argTypes) {
        // 首先在当前类中查找方法
        List<ScriptModels.MethodDefinition> methods = classDef.getMethods(methodName);
        if (methods != null && !methods.isEmpty()) {
            // 如果提供了参数类型，选择最匹配的方法
            if (argTypes != null && !argTypes.isEmpty()) {
                for (ScriptModels.MethodDefinition methodDef : methods) {
                    if (isMethodApplicable(methodDef, argTypes, context)) {
                        return methodDef;
                    }
                }
            } else {
                // 没有提供参数类型，返回第一个方法
                return methods.get(0);
            }
        }

        // 如果当前类没有找到，检查父类
        String superClassName = classDef.getSuperClassName();
        if (superClassName != null) {
            ScriptModels.ClassDefinition superClassDef = context.customClasses.get(superClassName);
            if (superClassDef != null) {
                return findMethodInHierarchy(superClassDef, methodName, context, argTypes);
            }
        }

        // 如果没有找到，返回null
        return null;
    }

    private static boolean isMethodApplicable(ScriptModels.MethodDefinition methodDef, List<Class<?>> argTypes,
                                              ScriptModels.ExecutionContext context) {
        List<ScriptModels.Parameter> parameters = methodDef.getParameters();
        
        // 如果参数数量不匹配，不适用
        if (parameters.size() != argTypes.size()) {
            return false;
        }

        // 检查每个参数的类型是否匹配
        for (int i = 0; i < parameters.size(); i++) {
            ScriptModels.Parameter param = parameters.get(i);
            Class<?> expectedType = getClassFromTypeName(param.getType(), context);
            Class<?> actualType = argTypes.get(i);

            if (expectedType != null && actualType != null) {
                if (!ReflectionUtils.isTypeCompatible(expectedType, actualType)) {
                    return false;
                }
            }
        }

        return true;
    }

    private static Class<?> getClassFromTypeName(String typeName, ScriptModels.ExecutionContext context) {
        try {
            switch (typeName) {
                case "int" -> {
                    return int.class;
                }
                case "long" -> {
                    return long.class;
                }
                case "float" -> {
                    return float.class;
                }
                case "double" -> {
                    return double.class;
                }
                case "boolean" -> {
                    return boolean.class;
                }
                case "char" -> {
                    return char.class;
                }
                case "byte" -> {
                    return byte.class;
                }
                case "short" -> {
                    return short.class;
                }
                case "void" -> {
                    return void.class;
                }
            }

            // 处理数组类型
            if (typeName.endsWith("[]")) {
                String componentTypeName = typeName.substring(0, typeName.length() - 2);
                Class<?> componentType = getClassFromTypeName(componentTypeName, context);
                if (componentType != null) {
                    return java.lang.reflect.Array.newInstance(componentType, 0).getClass();
                }
            }
            
            // 处理对象类型
            return (Class<?>) context.findClass(typeName);
        } catch (Exception e) {
            return null;
        }
    }

    public static ScriptModels.FieldDefinition findFieldInHierarchy(ScriptModels.ClassDefinition classDef, String fieldName,
                                                                    ScriptModels.ExecutionContext context) {

        // 同上
        ScriptModels.FieldDefinition fieldDef = classDef.getField(fieldName);
        if (fieldDef != null) {
            return fieldDef;
        }

        String superClassName = classDef.getSuperClassName();
        if (superClassName != null) {
            ScriptModels.ClassDefinition superClassDef = context.customClasses.get(superClassName);
            if (superClassDef != null) {
                return findFieldInHierarchy(superClassDef, fieldName, context);
            }
        }

        return null;
    }



}
