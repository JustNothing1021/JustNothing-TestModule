package com.justnothing.testmodule.command.functions.classcmd;

import com.justnothing.testmodule.command.CommandExecutor;
import com.justnothing.testmodule.command.output.Colors;
import com.justnothing.testmodule.command.utils.CommandExceptionHandler;
import com.justnothing.testmodule.utils.reflect.ClassResolver;
import com.justnothing.testmodule.utils.reflect.DescriptorColorizer;

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
    protected void executeInternal(ClassCommandContext context) throws Exception {
        String[] args = context.getArgs();
        CommandExecutor.CmdExecContext cmd = context.getExecContext();
        
        if (args.length < 1) {
            CommandExceptionHandler.handleException(
                "class analyze",
                new IllegalArgumentException("参数不足, 需要至少1个参数: class analyze <class_name>"),
                context.getExecContext(),
                "参数错误"
            );
            return;
        }

        boolean showFields = false;
        boolean showMethods = false;
        boolean showAll = true;
        boolean verbose = false;
        String className = args[args.length - 1];

        for (int i = 0; i < args.length - 1; i++) {
            String arg = args[i];
            switch (arg) {
                case "-v", "--verbose" -> verbose = true;
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

        Class<?> targetClass = ClassResolver.findClassOrFail(className, context.getClassLoader());
        context.getLogger().info("成功加载类: " + targetClass.getName());

        if (showAll || showFields) {
            cmd.println("=== 字段 ===", Colors.CYAN);
            Map<String, FieldInfo> fieldMap = collectAllFields(targetClass, context);
            if (fieldMap.isEmpty()) {
                cmd.println("无字段", Colors.GRAY);
            } else {
                for (Map.Entry<String, FieldInfo> entry : fieldMap.entrySet()) {
                    FieldInfo fieldInfo = entry.getValue();
                    cmd.print("  ", Colors.GRAY);
                    DescriptorColorizer.printColoredDescriptor(cmd, fieldInfo.field, !verbose);
                    
                    if (Modifier.isStatic(fieldInfo.field.getModifiers())) {
                        try {
                            cmd.print(" = ", Colors.WHITE);
                            Object value = fieldInfo.field.get(null);
                            if (value == null) {
                                cmd.print("null", Colors.LIGHT_GREEN);
                            } else if (value instanceof String) {
                                cmd.print("\"", Colors.WHITE);
                                cmd.print(value.toString(), Colors.LIGHT_GREEN);
                                cmd.print("\"", Colors.WHITE);
                            } else {
                                cmd.print(value.toString(), Colors.LIGHT_GREEN);
                            }
                        } catch (IllegalAccessException | NullPointerException | IllegalArgumentException e) {
                            cmd.print(" [无法访问: ", Colors.RED);
                            String msg = e.getMessage();
                            cmd.print(msg != null ? msg : "暂无错误信息", Colors.ORANGE);
                            cmd.print("]", Colors.RED);
                        }
                    }

                    cmd.println("");

                    if (fieldInfo.fromClass != targetClass) {
                        cmd.print("    └─> 继承自 ", Colors.GRAY);
                        cmd.println(fieldInfo.fromClass.getName(), Colors.GREEN);
                    }
                }
            }
            cmd.print("字段总数: ", Colors.CYAN);
            cmd.println(String.valueOf(fieldMap.size()), Colors.YELLOW);
            cmd.println("");
        }

        if (showAll || showMethods) {
            cmd.println("=== 方法 ===", Colors.CYAN);
            Map<String, MethodInfo> methodMap = collectAllMethods(targetClass, context);
            if (methodMap.isEmpty()) {
                cmd.println("无方法", Colors.GRAY);
            } else {
                for (Map.Entry<String, MethodInfo> entry : methodMap.entrySet()) {
                    MethodInfo methodInfo = entry.getValue();
                    cmd.print("  ", Colors.GRAY);
                    DescriptorColorizer.printColoredDescriptor(cmd, methodInfo.method, !verbose);
                    cmd.println("");

                    if (!methodInfo.fromInterfaces.isEmpty()) {
                        cmd.print("    └─> 实现接口 ", Colors.GRAY);
                        boolean first = true;
                        for (Class<?> _interface : methodInfo.fromInterfaces) {
                            if (!first) {
                                cmd.print(", ", Colors.WHITE);
                            }
                            cmd.print(_interface.getName(), Colors.GREEN);
                            first = false;
                        }
                        cmd.println("");
                    }

                    if (methodInfo.fromClass != targetClass) {
                        cmd.print("    └─> 继承自 ", Colors.GRAY);
                        cmd.println(methodInfo.fromClass.getName(), Colors.GREEN);
                    }
                }
            }
            cmd.print("方法总数: ", Colors.CYAN);
            cmd.println(String.valueOf(methodMap.size()), Colors.YELLOW);
            cmd.println("");
        }

        if (showAll) {
            cmd.println("=== 类信息 ===", Colors.CYAN);
            cmd.print("类名: ", Colors.CYAN);
            cmd.println(targetClass.getName(), Colors.GREEN);
            cmd.print("简单类名: ", Colors.CYAN);
            cmd.println(targetClass.getSimpleName(), Colors.GREEN);
            cmd.print("包名: ", Colors.CYAN);
            cmd.println(targetClass.getPackage() != null ? targetClass.getPackage().getName() : "无", Colors.GREEN);

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
            cmd.print("类型: ", Colors.CYAN);
            cmd.println(typeInfo, Colors.MAGENTA);

            List<String> modifiers = new ArrayList<>();
            if (Modifier.isAbstract(targetClass.getModifiers())) {
                modifiers.add("抽象类");
            }
            if (Modifier.isFinal(targetClass.getModifiers())) {
                modifiers.add("final类");
            }
            if (!modifiers.isEmpty()) {
                cmd.print("修饰符: ", Colors.CYAN);
                for (int i = 0; i < modifiers.size(); i++) {
                    if (i > 0) {
                        cmd.print(", ", Colors.WHITE);
                    }
                    cmd.print(modifiers.get(i), Colors.YELLOW);
                }
                cmd.println("");
            }
            cmd.println("");

            cmd.println("=== 父类 ===", Colors.CYAN);
            Class<?> superClass = targetClass.getSuperclass();
            if (superClass != null) {
                cmd.println(superClass.getName(), Colors.GREEN);
            } else {
                cmd.println("无父类", Colors.GRAY);
            }
            cmd.println("");

            cmd.println("=== 实现的接口 ===", Colors.CYAN);
            Class<?>[] interfaces = targetClass.getInterfaces();
            if (interfaces.length == 0) {
                cmd.println("无接口", Colors.GRAY);
            } else {
                for (Class<?> _interface : interfaces) {
                    cmd.print("  - ", Colors.GRAY);
                    cmd.println(_interface.getName(), Colors.GREEN);
                }
            }
            cmd.print("接口总数: ", Colors.CYAN);
            cmd.println(String.valueOf(interfaces.length), Colors.YELLOW);
            cmd.println("");

            cmd.println("=== 包信息 ===", Colors.CYAN);
            cmd.print("包: ", Colors.CYAN);
            cmd.println(context.getTargetPackage() != null ? context.getTargetPackage() : "default", Colors.GREEN);
            cmd.print("类加载器: ", Colors.CYAN);
            cmd.println(context.getClassLoader() != null ? context.getClassLoader().toString() : "无", Colors.LIGHT_GREEN);
        }

        context.getLogger().info("执行成功");
    }

    @Override
    public String getHelpText() {
        return """
            语法: class analyze [options] <class_name>
            
            分析类的字段和方法.
            
            选项:
                -v, --verbose     显示详细信息
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
