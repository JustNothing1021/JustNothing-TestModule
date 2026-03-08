package com.justnothing.testmodule.utils.reflect;

import com.justnothing.testmodule.utils.functions.Logger;

import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ReflectionUtils {

    private static final Logger logger = Logger.getLoggerForName("ReflectionUtils");


    public static boolean isPrimitiveWrapperMatch(Class<?> target, Class<?> source) {
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

    public static boolean isApplicableArgs(Class<?>[] methodArgsTypes, List<Class<?>> usingArgTypes) {
        if (methodArgsTypes.length != usingArgTypes.size())
            return false;
        for (int i = 0; i < methodArgsTypes.length; i++)
            if (!methodArgsTypes[i].isAssignableFrom(usingArgTypes.get(i))
                    && !isPrimitiveWrapperMatch(methodArgsTypes[i], usingArgTypes.get(i))
                    && Void.class != usingArgTypes.get(i))
                return false;
        return true;
    }

    public static boolean isApplicableArgs(Class<?>[] methodArgsTypes, Class<?>[] usingArgTypes) {
        return isApplicableArgs(methodArgsTypes, Arrays.asList(usingArgTypes));
    }

    public static Method[] findMethod(Class<?> clazz, String methodName, String signature, ClassLoader classLoader)
            throws NoSuchMethodException, ClassNotFoundException {
        logger.debug("在类 " + clazz.getName() + " 中查找方法: " + methodName
                + (signature != null ? " (签名: " + signature + ")" : ""));

        Method[] methods = clazz.getDeclaredMethods();
        List<Method> candidates = new ArrayList<>();

        for (Method method : methods) {
            if (method.getName().equals(methodName)) {
                candidates.add(method);
                logger.debug("找到候选方法: " + method + " (参数: " + Arrays.toString(method.getParameterTypes()) + ")");
            }
        }

        if (candidates.isEmpty()) {
            Class<?> superClass = clazz.getSuperclass();
            if (superClass != null) {
                logger.debug("在父类 " + superClass.getName() + " 中继续查找");
                return findMethod(superClass, methodName, signature, classLoader);
            }

            StringBuilder availableMethods = new StringBuilder("可用方法:\n");
            for (Method m : clazz.getDeclaredMethods()) {
                availableMethods.append("  ").append(m).append("\n");
            }
            logger.error("未找到方法 " + methodName + "\n" + availableMethods);
            throw new NoSuchMethodException(
                    "未找到方法: " + methodName + " 在类 " + clazz.getName() + "\n" + availableMethods);
        }

        if (signature == null || signature.isEmpty()) {
            logger.debug("未指定签名，返回所有方法");
            return candidates.toArray(new Method[0]);
        }

        for (Method method : candidates) {
            if (isApplicableArgs(method.getParameterTypes(), SignatureUtils.parseSignature(signature, classLoader))) {
                return new Method[] { method };
            }
        }

        StringBuilder errorMsg = new StringBuilder("未找到匹配签名的方法: " + methodName + " (签名: " + signature + ")\n");
        errorMsg.append("候选方法:\n");
        for (Method m : candidates) {
            errorMsg.append("  ").append(m).append("\n");
        }
        logger.error(errorMsg.toString());
        throw new NoSuchMethodException(errorMsg.toString());
    }

    public static Constructor<?>[] findAllConstructors(Class<?> clazz, String signature, ClassLoader classLoader)
            throws NoSuchMethodException, ClassNotFoundException {
        logger.debug("在类 " + clazz.getName() + " 中查找构造函数"
                + (signature != null ? " (签名: " + signature + ")" : ""));

        Constructor<?>[] constructors = clazz.getDeclaredConstructors();
        List<Constructor<?>> candidates = new ArrayList<>();

        for (Constructor<?> constructor : constructors) {
            candidates.add(constructor);
            logger.debug("找到候选构造函数: " + Arrays.toString(constructors));
        }

        if (candidates.isEmpty()) {
            StringBuilder availableConstructors = new StringBuilder("可用构造函数:\n");
            for (Constructor<?> c : clazz.getDeclaredConstructors()) {
                availableConstructors.append("  ").append(c).append("\n");
            }
            logger.error("未找到构造函数\n" + availableConstructors);
            throw new NoSuchMethodException(
                    "在类" + clazz.getName() + "中未找到构造函数\n" + availableConstructors);
        }

        if (signature == null || signature.isEmpty()) {
            logger.debug("未指定签名，返回所有构造函数");
            return candidates.toArray(new Constructor<?>[0]);
        }

        for (Constructor<?> constructor : candidates) {
            if (isApplicableArgs(constructor.getParameterTypes(), SignatureUtils.parseSignature(signature, classLoader))) {
                return new Constructor<?>[] { constructor };
            }
        }

        StringBuilder errorMsg = new StringBuilder("未找到匹配签名的构造函数: (签名: " + signature + ")\n");
        errorMsg.append("候选构造函数:\n");
        for (Constructor<?> c : candidates) {
            errorMsg.append("  ").append(c).append("\n");
        }
        logger.error(errorMsg.toString());
        throw new NoSuchMethodException(errorMsg.toString());
    }

    public static Constructor<?> findConstructor(Class<?> clazz, Class<?>[] paramTypes) {
        for (Constructor<?> constructor : clazz.getDeclaredConstructors()) {
            if (constructor.getParameterCount() == paramTypes.length) {
                Class<?>[] constructorParams = constructor.getParameterTypes();
                boolean compatible = true;

                for (int i = 0; i < paramTypes.length; i++) {
                    if (!constructorParams[i].isAssignableFrom(paramTypes[i])) {
                        compatible = false;
                        break;
                    }
                }

                if (compatible) {
                    constructor.setAccessible(true);
                    return constructor;
                }
            }
        }
        return null;
    }

    private static Constructor<?> findConstructorOrFail(Class<?> clazz, Class<?>[] paramTypes) throws NoSuchMethodException {
        Constructor<?> constructor = findConstructor(clazz, paramTypes);
        if (constructor == null) throw new NoSuchMethodException(
                "没有找到" + clazz + "对应指定签名的构造函数: " + Arrays.toString(paramTypes));
        return constructor;
    }


    public static Object parseValue(String value, Class<?> type) {
        if (value == null) {
            return null;
        }

        try {
            if (type == String.class) {
                return value;
            } else if (type == int.class || type == Integer.class) {
                return Integer.parseInt(value);
            } else if (type == long.class || type == Long.class) {
                return Long.parseLong(value);
            } else if (type == float.class || type == Float.class) {
                return Float.parseFloat(value);
            } else if (type == double.class || type == Double.class) {
                return Double.parseDouble(value);
            } else if (type == boolean.class || type == Boolean.class) {
                return Boolean.parseBoolean(value);
            } else if (type == byte.class || type == Byte.class) {
                return Byte.parseByte(value);
            } else if (type == short.class || type == Short.class) {
                return Short.parseShort(value);
            } else if (type == char.class || type == Character.class) {
                if (value.length() == 1) {
                    return value.charAt(0);
                }
                throw new IllegalArgumentException("字符值必须是一个字符");
            }
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("无法将 '" + value + "' 转换为 " + type.getSimpleName());
        }

        return value;
    }

    public static String[] parseParams(String paramsStr) {
        if (paramsStr == null || paramsStr.trim().isEmpty()) {
            return new String[0];
        }

        List<String> params = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;

        for (char c : paramsStr.toCharArray()) {
            if (c == '"') {
                inQuotes = !inQuotes;
            } else if (c == ' ' && !inQuotes) {
                if (current.length() > 0) {
                    params.add(current.toString());
                    current = new StringBuilder();
                }
            } else {
                current.append(c);
            }
        }

        if (current.length() > 0) {
            params.add(current.toString());
        }

        return params.toArray(new String[0]);
    }

    public static Object[] convertParams(String[] params, Class<?>[] paramTypes) {
        if (params == null || params.length == 0) {
            return new Object[0];
        }

        Object[] result = new Object[params.length];
        for (int i = 0; i < params.length; i++) {
            if (i < paramTypes.length) {
                result[i] = parseValue(params[i], paramTypes[i]);
            } else {
                result[i] = parseValue(params[i], String.class);
            }
        }

        return result;
    }

    public static String getDescriptor(Member member) {
        return getDescriptor(member, false);
    }

    public static String getDescriptor(Member member, boolean simple) {
        StringBuilder sb = new StringBuilder();
        int modifiers = member.getModifiers();

        if (Modifier.isPublic(modifiers)) sb.append("public ");
        else if (Modifier.isPrivate(modifiers)) sb.append("private ");
        else if (Modifier.isProtected(modifiers)) sb.append("protected ");
        else sb.append("[package-private] ");

        if (Modifier.isStatic(modifiers)) sb.append("static ");
        if (Modifier.isFinal(modifiers)) sb.append("final ");
        if (Modifier.isSynchronized(modifiers)) sb.append("synchronized ");
        if (Modifier.isNative(modifiers)) sb.append("native ");

        if (Modifier.isInterface(modifiers)) sb.append("interface ");
        if (Modifier.isVolatile(modifiers)) sb.append("volatile ");
        if (Modifier.isStrict(modifiers)) sb.append("strictfp ");
        if (Modifier.isTransient(modifiers)) sb.append("transient ");

        if (member instanceof Method method) {
            sb.append(simple ? method.getReturnType().getSimpleName() :
                               method.getReturnType().getName()).append(" ");
            sb.append(method.getName()).append("(");

            Class<?>[] params = method.getParameterTypes();
            for (int i = 0; i < params.length; i++) {
                sb.append(simple ? params[i].getSimpleName() : params[i].getName());
                if (i < params.length - 1) sb.append(", ");
            }
            sb.append(")");
        } else if (member instanceof Field field) {
            sb.append(simple ? member.getDeclaringClass().getName() + "." + field.getName() : field.getName())
                    .append(" ");
            sb.append(field.getName());
        } else if (member instanceof Constructor<?> constructor) {
            sb.append(simple ? constructor.getDeclaringClass().getSimpleName() :
                               constructor.getDeclaringClass().getName());
            Class<?>[] params = constructor.getParameterTypes();
            for (int i = 0; i < params.length; i++) {
                sb.append(simple ? params[i].getSimpleName() : params[i].getName());
                if (i < params.length - 1) sb.append(", ");
            }
            sb.append(")");
        }

        return sb.toString().trim();
    }

    public static String getModifiersString(int modifiers) {
        return Modifier.toString(modifiers);
    }
}