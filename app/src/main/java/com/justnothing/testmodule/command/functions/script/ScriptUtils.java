package com.justnothing.testmodule.command.functions.script;

import com.justnothing.testmodule.hooks.XposedBasicHook;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

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

    static Class<?> findClassThroughApi(String className, ClassLoader classLoader) {
        try {
            return XposedBasicHook.ClassFinder.withClassLoader(classLoader).find(className);
        } catch (Exception e) {
            try {
                return Class.forName(className, false, classLoader);
            } catch (ClassNotFoundException ex) {
                return null;
            }
        }
    }

    static boolean isStandaloneMode() {
        try {
            Class<?> clazz = Class.forName("android.util.Log");
            Method method = clazz.getMethod("i", String.class, String.class);
            method.invoke(null, "TestInterpreter", "这个日志是用来测试现在是不是在安卓环境里的");
            return false;
        } catch (Exception e) {
            return true;
        }
    }


    static boolean isPrimitiveWrapperMatch(Class<?> target, Class<?> source) {
        if (target == int.class && source == Integer.class)
            return true;
        if (target == Integer.class && source == int.class)
            return true;
        if (target == long.class && source == Long.class)
            return true;
        if (target == Long.class && source == long.class)
            return true;
        if (target == float.class && source == Float.class)
            return true;
        if (target == Float.class && source == float.class)
            return true;
        if (target == double.class && source == Double.class)
            return true;
        if (target == Double.class && source == double.class)
            return true;
        if (target == boolean.class && source == Boolean.class)
            return true;
        if (target == Boolean.class && source == boolean.class)
            return true;
        if (target == char.class && source == Character.class)
            return true;
        if (target == Character.class && source == char.class)
            return true;
        if (target == byte.class && source == Byte.class)
            return true;
        if (target == Byte.class && source == byte.class)
            return true;
        if (target == short.class && source == Short.class)
            return true;
        return target == Short.class && source == short.class;
    }

    static boolean isTypeCompatible(Class<?> expected, Class<?> actual) {
        if (expected == Void.class && actual == Void.class)
            return true;

        if (actual == Void.class)
            return !expected.isPrimitive();

        if (expected.isPrimitive())
            return isPrimitiveWrapperMatch(expected, actual);

        return expected.isAssignableFrom(actual);
    }

    public static boolean isApplicableArgs(Class<?>[] methodArgsTypes, List<Class<?>> usingArgTypes,
                                           boolean isVarArgs) {
        // 如果是可变参数方法
        if (isVarArgs) {
            // 可变参数方法至少要有一个参数（可变参数数组本身）
            if (methodArgsTypes.length == 0)
                return false;

            // 可变参数数组类型是最后一个参数
            Class<?> varArgsType = methodArgsTypes[methodArgsTypes.length - 1];
            if (varArgsType.isArray()) {
                // 获取可变参数的元素类型
                Class<?> varArgsComponentType = varArgsType.getComponentType();

                // 固定参数的数量（不包括可变参数）
                int fixedParamCount = methodArgsTypes.length - 1;

                // 如果传入参数少于固定参数数量，不匹配
                if (usingArgTypes.size() < fixedParamCount) {
                    return false;
                }

                // 检查固定参数
                for (int i = 0; i < fixedParamCount; i++) {
                    Class<?> methodArgType = methodArgsTypes[i];
                    Class<?> usingArgType = usingArgTypes.get(i);
                    if (!isTypeCompatible(methodArgType, usingArgType)) {
                        return false;
                    }
                }

                // 检查可变参数
                for (int i = fixedParamCount; i < usingArgTypes.size(); i++) {
                    Class<?> usingArgType = usingArgTypes.get(i);

                    // 每个可变参数都必须可以赋值给可变参数的元素类型
                    if (!isTypeCompatible(varArgsComponentType, usingArgType)) {
                        return false;
                    }
                }
                return true;
            }
        }

        // 非可变参数方法或处理为普通方法
        if (methodArgsTypes.length != usingArgTypes.size()) {
            return false;
        }

        for (int i = 0; i < methodArgsTypes.length; i++) {
            if (!isTypeCompatible(methodArgsTypes[i], usingArgTypes.get(i))) {
                return false;
            }
        }

        return true;
    }

    // 实际上这仨重载就只用了两次
    public static boolean isApplicableArgs(Class<?>[] methodArgsTypes, Class<?>[] usingArgTypes, boolean isVarArgs) {
        return isApplicableArgs(methodArgsTypes, Arrays.asList(usingArgTypes), isVarArgs);
    }

    public static boolean isApplicableArgs(List<Class<?>> methodArgsTypes, List<Class<?>> usingArgTypes,
                                           boolean isVarArgs) {
        return isApplicableArgs(methodArgsTypes.toArray(new Class<?>[0]), usingArgTypes, isVarArgs);
    }

    public static boolean isApplicableArgs(List<Class<?>> methodArgsTypes, Class<?>[] usingArgTypes,
                                           boolean isVarArgs) {
        return isApplicableArgs(methodArgsTypes.toArray(new Class<?>[0]), usingArgTypes, isVarArgs);
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
        // 首先在当前类中查找方法
        ScriptModels.MethodDefinition methodDef = classDef.getMethod(methodName);
        if (methodDef != null) {
            return methodDef;
        }

        // 如果当前类没有找到，检查父类
        String superClassName = classDef.getSuperClassName();
        if (superClassName != null) {
            ScriptModels.ClassDefinition superClassDef = context.customClasses.get(superClassName);
            if (superClassDef != null) {
                return findMethodInHierarchy(superClassDef, methodName, context);
            }
        }

        // 如果没有找到，返回null
        return null;
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
