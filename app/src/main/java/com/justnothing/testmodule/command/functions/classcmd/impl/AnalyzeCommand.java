package com.justnothing.testmodule.command.functions.classcmd.impl;

import com.justnothing.testmodule.command.functions.classcmd.AbstractClassCommand;
import com.justnothing.testmodule.command.functions.classcmd.ClassCommandContext;
import com.justnothing.testmodule.command.functions.classcmd.request.AnalyzeClassRequest;
import com.justnothing.testmodule.command.functions.classcmd.model.ClassInfo;
import com.justnothing.testmodule.command.functions.classcmd.model.FieldInfo;
import com.justnothing.testmodule.command.functions.classcmd.model.MethodInfo;
import com.justnothing.testmodule.command.functions.classcmd.response.AnalyzeReportResult;
import com.justnothing.testmodule.command.output.Colors;
import com.justnothing.testmodule.command.utils.CommandExceptionHandler;
import com.justnothing.testmodule.utils.reflect.ClassResolver;
import com.justnothing.testmodule.utils.reflect.DescriptorColorizer;

import java.lang.reflect.Constructor;
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

public class AnalyzeCommand extends AbstractClassCommand<AnalyzeClassRequest, AnalyzeReportResult> {

    public AnalyzeCommand() {
        super("class analyze", AnalyzeClassRequest.class, AnalyzeReportResult.class);
    }

    @Override
    public String getHelpText() {
        return """
            语法: class analyze [options] <class_name>

            分析类的字段和方法.

            选项:
                -v, --verbose        显示详细信息
                -f, --fields         只显示字段
                -m, --methods        只显示方法
                -c, --constructors   显示构造函数
                -i, --interfaces     显示实现的接口
                -s, --super          显示父类信息
                --modifiers          显示修饰符信息
                --hierarchy          显示继承层次
                --stats              显示统计信息
                --raw                原始输出 (JSON格式)
                -a, --all            显示所有信息 (默认)

            示例:
                class analyze java.lang.String
                class analyze -f com.android.server.am.ActivityManagerService
                class analyze -a java.util.ArrayList
                class analyze --raw java.lang.String
            """;
    }

    @Override
    protected AnalyzeReportResult executeClassCommand(ClassCommandContext<AnalyzeClassRequest> context) throws Exception {
        AnalyzeClassRequest request = context.execContext().getCommandRequest();
        String className = request.getClassName();
        var cmd = context.execContext();

        if (className == null || className.isEmpty()) {
            CommandExceptionHandler.handleException(
                "class analyze",
                new IllegalArgumentException("参数不足: class analyze [options] <class_name>"),
                context.execContext(),
                "参数错误"
            );
            return null;
        }
        boolean showHierarchy = request.isShowHierarchy();
        boolean showFields = request.isShowFields();
        boolean showMethods = request.isShowMethods();
        boolean showConstructors = request.isShowConstructors();
        boolean showModifiers = request.isShowModifiers();
        boolean showStats = request.isShowStats();
        boolean rawOutput = request.isRawOutput();
        boolean showAll = request.isShowAll();
        boolean verbose = request.isVerbose();

        AnalyzeReportResult result = new AnalyzeReportResult();
        result.setClassName(className);
        result.setSuccess(true);

        context.logger().debug("目标类: " + className + ", 显示字段: " + showFields + ", 显示方法: " + showMethods + ", 显示全部: " + showAll);

        Class<?> targetClass = ClassResolver.findClassOrFail(className, context.classLoader());
        context.logger().info("成功加载类: " + targetClass.getName());

        if (showAll || showFields) {
            cmd.println("=== 字段 ===", Colors.CYAN);
            Map<String, FieldInfo> fieldMap = collectAllFields(targetClass, context);
            if (fieldMap.isEmpty()) {
                cmd.println("无字段", Colors.GRAY);
            } else {
                for (FieldInfo fieldInfo : fieldMap.values()) {
                    Field field = findDeclaredField(targetClass, fieldInfo.getName());
                    if (field == null) {
                        continue;
                    }
                    cmd.print("  ", Colors.GRAY);
                    DescriptorColorizer.printColoredDescriptor(cmd, field, !verbose);

                    if (Modifier.isStatic(field.getModifiers())) {
                        try {
                            cmd.print(" = ", Colors.WHITE);
                            Object value = field.get(null);
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
                            cmd.print(msg != null ? msg : "暂无错误信息", Colors.RED);
                            cmd.print("]", Colors.RED);
                        }
                    }

                    cmd.println("");

                    if (showHierarchy && fieldInfo.getDeclaringClass() != null && !fieldInfo.getDeclaringClass().equals(targetClass.getName())) {
                        cmd.print("    └─> 继承自 ", Colors.GRAY);
                        cmd.println(fieldInfo.getDeclaringClass(), Colors.GREEN);
                    }
                }
            }
            cmd.print("字段总数: ", Colors.CYAN);
            cmd.println(String.valueOf(fieldMap.size()), Colors.YELLOW);
            cmd.println("");

            result.setFields(new ArrayList<>(fieldMap.values()));
        }

        if (showAll || showMethods) {
            cmd.println("=== 方法 ===", Colors.CYAN);
            Map<String, MethodInfo> methodMap = collectAllMethods(targetClass, context);
            Map<String, List<String>> methodInterfaceMap = collectMethodInterfaces(targetClass, context);
            if (methodMap.isEmpty()) {
                cmd.println("无方法", Colors.GRAY);
            } else {
                for (MethodInfo methodInfo : methodMap.values()) {
                    Method method = findDeclaredMethod(targetClass, methodInfo.getName(), methodInfo.getParameterTypes());
                    cmd.print("  ", Colors.GRAY);
                    DescriptorColorizer.printColoredDescriptor(cmd, method, !verbose);
                    cmd.println("");

                    String signature = methodInfo.getSignature();
                    List<String> interfaceSources = methodInterfaceMap.get(signature);
                    if (showHierarchy && interfaceSources != null && !interfaceSources.isEmpty()) {
                        cmd.print("    └─> 实现接口 ", Colors.GRAY);
                        boolean first = true;
                        for (String iface : interfaceSources) {
                            if (!first) {
                                cmd.print(", ", Colors.WHITE);
                            }
                            cmd.print(iface, Colors.GREEN);
                            first = false;
                        }
                        cmd.println("");
                    }

                    if (showHierarchy && methodInfo.getDeclaringClass() != null && !methodInfo.getDeclaringClass().equals(targetClass.getName())) {
                        cmd.print("    └─> 继承自 ", Colors.GRAY);
                        cmd.println(methodInfo.getDeclaringClass(), Colors.GREEN);
                    }
                }
            }
            cmd.print("方法总数: ", Colors.CYAN);
            cmd.println(String.valueOf(methodMap.size()), Colors.YELLOW);
            cmd.println("");

            result.setMethods(new ArrayList<>(methodMap.values()));
        }

        if (showAll || showConstructors) {
            cmd.println("=== 构造函数 ===", Colors.CYAN);
            Constructor<?>[] constructors = targetClass.getDeclaredConstructors();
            List<MethodInfo> constructorList = new ArrayList<>();
            if (constructors.length == 0) {
                cmd.println("无构造函数", Colors.GRAY);
            } else {
                for (Constructor<?> constructor : constructors) {
                    cmd.print("  ", Colors.GRAY);
                    DescriptorColorizer.printColoredDescriptor(cmd, constructor, !verbose);
                    cmd.println("");
                    constructorList.add(MethodInfo.fromConstructor(constructor));
                }
            }
            cmd.print("构造函数总数: ", Colors.CYAN);
            cmd.println(String.valueOf(constructors.length), Colors.YELLOW);
            cmd.println("");
            result.setConstructors(constructorList);
        }

        if (showAll || showModifiers) {
            cmd.println("=== 类修饰符 ===", Colors.CYAN);
            int mods = targetClass.getModifiers();
            cmd.print("修饰符: ", Colors.CYAN);
            cmd.println(Modifier.toString(mods), Colors.YELLOW);

            String flags = ((targetClass.isInterface() ? "接口 " : "") +
                    (targetClass.isArray() ? "数组 " : "") +
                    (targetClass.isEnum() ? "枚举 " : "") +
                    (targetClass.isAnnotation() ? "注解 " : "") +
                    (Modifier.isAbstract(mods) ? "抽象 " : "") +
                    (Modifier.isFinal(mods) ? "final " : "") +
                    (targetClass.isAnonymousClass() ? "匿名类" : "")).trim();

            if (!flags.isEmpty()) {
                cmd.print("特性: ", Colors.CYAN);
                cmd.println(flags, Colors.BLUE);
            }
            cmd.println("");
        }

        if (showStats) {
            cmd.println("=== 统计信息 ===", Colors.CYAN);
            Map<String, FieldInfo> fieldMap = collectAllFields(targetClass, context);
            Map<String, MethodInfo> methodMap = collectAllMethods(targetClass, context);
            java.lang.reflect.Constructor<?>[] constructors = targetClass.getDeclaredConstructors();
            Class<?>[] interfaces = targetClass.getInterfaces();

            int staticFieldCount = 0, instanceFieldCount = 0;
            for (FieldInfo fi : fieldMap.values()) {
                java.lang.reflect.Field f = findDeclaredField(targetClass, fi.getName());
                if (f != null && Modifier.isStatic(f.getModifiers())) staticFieldCount++;
                else instanceFieldCount++;
            }

            int staticMethodCount = 0, instanceMethodCount = 0;
            for (MethodInfo mi : methodMap.values()) {
                Method m = findDeclaredMethod(targetClass, mi.getName(), mi.getParameterTypes());
                if (m != null && Modifier.isStatic(m.getModifiers())) staticMethodCount++;
                else instanceMethodCount++;
            }

            cmd.print("字段: ", Colors.CYAN);
            cmd.print(fieldMap.size() + " 个", Colors.YELLOW);
            cmd.print(" (静态: ", Colors.GRAY);
            cmd.print(staticFieldCount, Colors.GREEN);
            cmd.print(", 实例: ", Colors.GRAY);
            cmd.print(")", Colors.GRAY);
            cmd.println(instanceFieldCount, Colors.GREEN);

            cmd.print("方法: ", Colors.CYAN);
            cmd.print(methodMap.size() + " 个", Colors.YELLOW);
            cmd.print(" (静态: ", Colors.GRAY);
            cmd.print(staticMethodCount, Colors.GREEN);
            cmd.print(", 实例: ", Colors.GRAY);
            cmd.print(")", Colors.GRAY);
            cmd.println(instanceMethodCount, Colors.GREEN);

            cmd.print("构造函数: ", Colors.CYAN);
            cmd.println(constructors.length + " 个", Colors.YELLOW);

            cmd.print("接口: ", Colors.CYAN);
            cmd.println(interfaces.length + " 个", Colors.YELLOW);

            Class<?> current = targetClass;
            int depth = 0;
            while (current != null && current != Object.class) { depth++; current = current.getSuperclass(); }
            cmd.print("继承深度: ", Colors.CYAN);
            cmd.println(depth + " 层", Colors.YELLOW);
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

            List<String> interfaceList = new ArrayList<>();
            for (Class<?> _interface : interfaces) {
                interfaceList.add(_interface.getName());
            }
            result.setInterfaces(interfaceList);

            cmd.println("=== 包信息 ===", Colors.CYAN);
            cmd.print("包: ", Colors.CYAN);
            cmd.println(context.targetPackage() != null ? context.targetPackage() : "default", Colors.GREEN);
            cmd.print("类加载器: ", Colors.CYAN);
            cmd.println(context.classLoader() != null ? context.classLoader().toString() : "无", Colors.LIGHT_GREEN);
        }

        context.logger().info("执行成功");

        ClassInfo classInfo = new ClassInfo();
        classInfo.setName(targetClass.getName());
        classInfo.setModifiers(targetClass.getModifiers());
        classInfo.setInterface(targetClass.isInterface());
        result.setClassInfo(classInfo);

        if (targetClass.getSuperclass() != null) {
            result.setSuperClass(targetClass.getSuperclass().getName());
        }

        if (rawOutput) {
            try {
                cmd.println(result.toJson().toString(2), Colors.WHITE);
            } catch (Exception e) {
                context.logger().warn("JSON输出失败: " + e.getMessage());
            }
        }

        return result;
    }



    private Map<String, FieldInfo> collectAllFields(Class<?> targetClass, ClassCommandContext<AnalyzeClassRequest> context) {
        Map<String, FieldInfo> fieldMap = new LinkedHashMap<>();

        Class<?> current = targetClass;
        while (current != null && current != Object.class) {
            try {
                Field[] fields = current.getDeclaredFields();
                for (Field field : fields) {
                    String key = field.getName();
                    if (!fieldMap.containsKey(key)) {
                        field.setAccessible(true);
                        fieldMap.put(key, FieldInfo.fromField(field));
                    }
                }
            } catch (Exception e) {
                context.logger().debug("获取字段失败: " + current.getName() + ", " + e.getMessage());
            }
            current = current.getSuperclass();
        }

        return fieldMap;
    }

    private Map<String, MethodInfo> collectAllMethods(Class<?> targetClass, ClassCommandContext<AnalyzeClassRequest> context) {
        Map<String, MethodInfo> methodMap = new LinkedHashMap<>();

        Class<?> current = targetClass;
        while (current != null && current != Object.class) {
            try {
                Method[] methods = current.getDeclaredMethods();
                for (Method method : methods) {
                    String key = getMethodSignature(method);
                    if (!methodMap.containsKey(key)) {
                        method.setAccessible(true);
                        methodMap.put(key, MethodInfo.fromMethod(method));
                    }
                }
            } catch (Exception e) {
                context.logger().debug("获取方法失败: " + current.getName() + ", " + e.getMessage());
            }
            current = current.getSuperclass();
        }

        return methodMap;
    }

    private Map<String, List<String>> collectMethodInterfaces(Class<?> targetClass, ClassCommandContext<AnalyzeClassRequest> context) {
        Map<String, List<String>> interfaceMap = new LinkedHashMap<>();

        Class<?>[] interfaces = getAllInterfaces(targetClass);
        for (Class<?> _interface : interfaces) {
            try {
                Method[] methods = _interface.getDeclaredMethods();
                for (Method method : methods) {
                    String key = getMethodSignature(method);
                    interfaceMap.computeIfAbsent(key, k -> new ArrayList<>()).add(_interface.getName());
                }
            } catch (Exception e) {
                context.logger().debug("获取接口方法失败: " + _interface.getName() + ", " + e.getMessage());
            }
        }

        return interfaceMap;
    }

    private Field findDeclaredField(Class<?> targetClass, String fieldName) {
        Class<?> current = targetClass;
        while (current != null && current != Object.class) {
            try {
                return current.getDeclaredField(fieldName);
            } catch (NoSuchFieldException ignored) {}
            current = current.getSuperclass();
        }
        return null;
    }

    private Method findDeclaredMethod(Class<?> targetClass, String methodName, List<String> paramTypes) {
        Class<?> current = targetClass;
        while (current != null && current != Object.class) {
            try {
                for (Method m : current.getDeclaredMethods()) {
                    if (m.getName().equals(methodName)) {
                        List<String> types = new ArrayList<>();
                        for (Class<?> pt : m.getParameterTypes()) {
                            types.add(pt.getName());
                        }
                        if (types.equals(paramTypes)) {
                            return m;
                        }
                    }
                }
            } catch (Exception ignored) {}
            current = current.getSuperclass();
        }
        return null;
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
