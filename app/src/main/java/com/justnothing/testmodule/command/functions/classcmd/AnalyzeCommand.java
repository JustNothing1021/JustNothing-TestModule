package com.justnothing.testmodule.command.functions.classcmd;

import com.justnothing.testmodule.utils.reflect.ReflectionUtils;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class AnalyzeCommand extends AbstractClassCommand {

    public AnalyzeCommand() {
        super("class analyze");
    }

    @Override
    protected String executeInternal(ClassCommandContext context) throws Exception {
        String[] args = context.getArgs();
        String error = requireArgs(context, args, 1);
        if (error != null) {
            return error;
        }

        boolean showFields = false;
        boolean showMethods = false;
        boolean showAll = true;
        String className = args[args.length - 1];

        for (int i = 0; i < args.length - 1; i++) {
            String arg = args[i];
            switch (arg) {
                case "-f", "--fields" -> {
                    showFields = true;
                    showMethods = false;
                    showAll = false;
                }
                case "-m", "--methods" -> {
                    showMethods = true;
                    showFields = false;
                    showAll = false;
                }
                case "-a", "--all" -> {
                    showAll = true;
                    showFields = false;
                    showMethods = false;
                }
            }
        }

        context.getLogger().debug("目标类: " + className + ", 显示字段: " + showFields + ", 显示方法: " + showMethods + ", 显示全部: " + showAll);

        Class<?> targetClass = context.loadClass(className);
        context.getLogger().info("成功加载类: " + targetClass.getName());

        StringBuilder sb = new StringBuilder();

        if (showAll || showFields) {
            sb.append("=== 字段 ===\n");
            Map<String, FieldInfo> fieldMap = collectAllFields(targetClass, context);
            if (fieldMap.isEmpty()) {
                sb.append("无字段\n");
            } else {
                for (Map.Entry<String, FieldInfo> entry : fieldMap.entrySet()) {
                    FieldInfo fieldInfo = entry.getValue();
                    sb.append("  ").append(ReflectionUtils.getDescriptor(fieldInfo.field));
                    if (Modifier.isStatic(fieldInfo.field.getModifiers())) {
                        try {
                            sb.append(" = ");
                            Object value = fieldInfo.field.get(null);
                            if (value == null) {
                                sb.append("null");
                            } else if (value instanceof String) {
                                sb.append("\"").append(value).append("\"");
                            } else {
                                sb.append(value);
                            }
                        } catch (IllegalAccessException | NullPointerException |  IllegalArgumentException e) {
                            sb.append("[无法访问: ").append(e.getMessage()).append("]");
                        }
                    }

                    sb.append("\n");

                    if (fieldInfo.fromClass != targetClass) {
                        sb.append("    └─> 继承自 ").append(fieldInfo.fromClass.getName()).append("\n");
                    }
                }
            }
            sb.append("字段总数: ").append(fieldMap.size()).append("\n\n");
        }

        if (showAll || showMethods) {
            sb.append("=== 方法 ===\n");
            Map<String, MethodInfo> methodMap = collectAllMethods(targetClass, context);
            if (methodMap.isEmpty()) {
                sb.append("无方法\n");
            } else {
                for (Map.Entry<String, MethodInfo> entry : methodMap.entrySet()) {
                    MethodInfo methodInfo = entry.getValue();
                    sb.append("  ").append(ReflectionUtils.getDescriptor(methodInfo.method)).append("\n");

                    if (!methodInfo.fromInterfaces.isEmpty()) {
                        sb.append("    └─> 实现接口 ");
                        boolean first = true;
                        for (Class<?> _interface : methodInfo.fromInterfaces) {
                            if (!first) sb.append(", ");
                            sb.append(_interface.getName());
                            first = false;
                        }
                        sb.append("\n");
                    }

                    if (methodInfo.fromClass != targetClass) {
                        sb.append("    └─> 继承自 ").append(methodInfo.fromClass.getName()).append("\n");
                    }
                }
            }
            sb.append("方法总数: ").append(methodMap.size()).append("\n\n");
        }

        if (showAll) {
            sb.append("=== 类信息 ===\n");
            sb.append("类名: ").append(targetClass.getName()).append("\n");
            sb.append("简单类名: ").append(targetClass.getSimpleName()).append("\n");
            sb.append("包名: ").append(targetClass.getPackage() != null ? targetClass.getPackage().getName() : "无").append("\n");

            String typeInfo = "普通类";
            if (targetClass.isArray()) {
                typeInfo = "数组类型";
            } else if (targetClass.isPrimitive()) {
                typeInfo = "原始类型";
            } else if (targetClass.isEnum()) {
                typeInfo = "枚举";
            } else if (targetClass.isAnnotation()) {
                typeInfo = "注解";
            } else if (targetClass.isInterface()) {
                typeInfo = "接口";
            }
            sb.append("类型: ").append(typeInfo).append("\n");

            List<String> modifiers = new ArrayList<>();
            if (Modifier.isAbstract(targetClass.getModifiers())) {
                modifiers.add("抽象类");
            }
            if (Modifier.isFinal(targetClass.getModifiers())) {
                modifiers.add("final类");
            }
            if (!modifiers.isEmpty()) {
                sb.append("修饰符: ");
                for (int i = 0; i < modifiers.size(); i++) {
                    if (i > 0) sb.append(", ");
                    sb.append(modifiers.get(i));
                }
                sb.append("\n");
            }
            sb.append("\n");

            sb.append("=== 父类 ===\n");
            Class<?> superClass = targetClass.getSuperclass();
            if (superClass != null) {
                sb.append(superClass.getName()).append("\n");
            } else {
                sb.append("无父类\n");
            }
            sb.append("\n");

            sb.append("=== 实现的接口 ===\n");
            Class<?>[] interfaces = targetClass.getInterfaces();
            if (interfaces.length == 0) {
                sb.append("无接口\n");
            } else {
                for (Class<?> _interface : interfaces) {
                    sb.append("  - ").append(_interface.getName()).append("\n");
                }
            }
            sb.append("接口总数: ").append(interfaces.length).append("\n\n");

            sb.append("=== 包信息 ===\n");
            sb.append("包: ").append(context.getTargetPackage() != null ? context.getTargetPackage() : "default").append("\n");
            sb.append("类加载器: ").append(context.getClassLoader() != null ? context.getClassLoader().toString() : "无").append("\n");
        }

        context.getLogger().info("执行成功");
        context.getLogger().debug("执行结果:\n" + sb);
        return sb.toString();
    }

    @Override
    public String getHelpText() {
        return """
            语法: class analyze [options] <class_name>
            
            分析类的字段和方法.
            
            选项:
                -f, --fields      只显示字段
                -m, --methods     只显示方法
                -a, --all         显示所有信息 (默认)
            
            示例:
                class analyze java.lang.String
                class analyze -f com.android.server.am.ActivityManagerService
                class analyze -a java.util.ArrayList
            """;
    }

    private static class FieldInfo {
        Field field;
        Class<?> fromClass;

        FieldInfo(Field field, Class<?> fromClass) {
            this.field = field;
            this.fromClass = fromClass;
        }
    }

    private static class MethodInfo {
        Method method;
        Class<?> fromClass;
        List<Class<?>> fromInterfaces;

        MethodInfo(Method method, Class<?> fromClass) {
            this.method = method;
            this.fromClass = fromClass;
            this.fromInterfaces = new ArrayList<>();
        }
    }

    private Map<String, FieldInfo> collectAllFields(Class<?> targetClass, ClassCommandContext context) {
        Map<String, FieldInfo> fieldMap = new LinkedHashMap<>();

        Class<?> current = targetClass;
        while (current != null && current != Object.class) {
            try {
                Field[] fields = current.getDeclaredFields();
                for (Field field : fields) {
                    String key = field.getName();
                    if (!fieldMap.containsKey(key)) {
                        field.setAccessible(true);
                        fieldMap.put(key, new FieldInfo(field, current));
                    }
                }
            } catch (Exception e) {
                context.getLogger().debug("获取字段失败: " + current.getName() + ", " + e.getMessage());
            }
            current = current.getSuperclass();
        }

        return fieldMap;
    }

    private Map<String, MethodInfo> collectAllMethods(Class<?> targetClass, ClassCommandContext context) {
        Map<String, MethodInfo> methodMap = new LinkedHashMap<>();

        Class<?> current = targetClass;
        while (current != null && current != Object.class) {
            try {
                Method[] methods = current.getDeclaredMethods();
                for (Method method : methods) {
                    String key = getMethodSignature(method);
                    if (!methodMap.containsKey(key)) {
                        method.setAccessible(true);
                        methodMap.put(key, new MethodInfo(method, current));
                    }
                }
            } catch (Exception e) {
                context.getLogger().debug("获取方法失败: " + current.getName() + ", " + e.getMessage());
            }
            current = current.getSuperclass();
        }

        Class<?>[] interfaces = getAllInterfaces(targetClass);
        for (Class<?> _interface : interfaces) {
            try {
                Method[] methods = _interface.getDeclaredMethods();
                for (Method method : methods) {
                    String key = getMethodSignature(method);
                    MethodInfo methodInfo = methodMap.get(key);
                    if (methodInfo != null) {
                        methodInfo.fromInterfaces.add(_interface);
                    } else {
                        method.setAccessible(true);
                        MethodInfo newInfo = new MethodInfo(method, _interface);
                        newInfo.fromInterfaces.add(_interface);
                        methodMap.put(key, newInfo);
                    }
                }
            } catch (Exception e) {
                context.getLogger().debug("获取接口方法失败: " + _interface.getName() + ", " + e.getMessage());
            }
        }

        return methodMap;
    }

    private Class<?>[] getAllInterfaces(Class<?> clazz) {
        List<Class<?>> interfaces = new ArrayList<>();
        Set<Class<?>> visited = new HashSet<>();

        Class<?> current = clazz;
        while (current != null) {
            Class<?>[] currentInterfaces = current.getInterfaces();
            for (Class<?> _interface : currentInterfaces) {
                if (!visited.contains(_interface)) {
                    visited.add(_interface);
                    interfaces.add(_interface);
                    interfaces.addAll(Arrays.asList(getAllInterfaces(_interface)));
                }
            }
            current = current.getSuperclass();
        }

        return interfaces.toArray(new Class<?>[0]);
    }

    private String getMethodSignature(Method method) {
        StringBuilder sb = new StringBuilder();
        sb.append(method.getName()).append("(");
        Class<?>[] params = method.getParameterTypes();
        for (int i = 0; i < params.length; i++) {
            if (i > 0) sb.append(",");
            sb.append(params[i].getName());
        }
        sb.append(")");
        return sb.toString();
    }

}
