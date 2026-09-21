package com.justnothing.testmodule.command.functions.classcmd.impl;

import com.justnothing.testmodule.command.framework.error.IllegalCommandLineArgumentException;
import com.justnothing.testmodule.command.framework.annotation.SubCommandInfo;
import com.justnothing.testmodule.command.framework.i18n.CliMessages;
import com.justnothing.testmodule.command.framework.i18n.Text;
import com.justnothing.testmodule.command.functions.classcmd.ClassTexts;
import com.justnothing.testmodule.command.functions.classcmd.model.ClassCommandContext;
import com.justnothing.testmodule.command.functions.classcmd.request.AnalyzeClassRequest;
import com.justnothing.testmodule.command.functions.classcmd.model.ClassInfo;
import com.justnothing.testmodule.command.functions.classcmd.model.FieldInfo;
import com.justnothing.testmodule.command.functions.classcmd.model.MethodInfo;
import com.justnothing.testmodule.command.functions.classcmd.response.AnalyzeReportResult;
import com.justnothing.testmodule.command.framework.output.Colors;
import com.justnothing.testmodule.hooks.api.HookAPI;
import com.justnothing.testmodule.command.framework.utils.GsonFactory;
import com.justnothing.testmodule.utils.reflect.ClassResolver;
import com.justnothing.testmodule.utils.format.DescriptorColorizer;

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

@SubCommandInfo(
    description = ClassTexts.SUB_CLASS_ANALYZE_DESC,
    usage = "class analyze [options] <class_name>",
    examples = {
        "class analyze java.lang.String",
        "class analyze --fields-only java.util.HashMap"
    },
    optionsDesc = ClassTexts.SUB_CLASS_ANALYZE_OPTIONS
)
public class ClassAnalyzeCommand extends AbstractClassCommand<AnalyzeClassRequest, AnalyzeReportResult> {

    public ClassAnalyzeCommand() {
        super("class analyze", AnalyzeClassRequest.class, AnalyzeReportResult.class);
    }

    @Override
    protected AnalyzeReportResult executeClassCommand(ClassCommandContext<AnalyzeClassRequest> context) throws Exception {
        AnalyzeClassRequest request = context.execContext().getCommandRequest();
        String className = request.getClassName();
        var cmd = context.execContext();

        if (className == null || className.isEmpty()) {
            throw new IllegalCommandLineArgumentException(Text.zhEn(
                    "参数不足: class analyze [options] <class_name>",
                    "Not enough arguments: class analyze [options] <class_name>").text());
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
            cmd.println(Text.zhEn("=== 字段 ===", "=== Fields ===").text(), Colors.CYAN);
            Map<String, FieldInfo> fieldMap = collectAllFields(targetClass, context);
            if (fieldMap.isEmpty()) {
                cmd.println(ClassTexts.TEXT_NO_FIELDS.text(), Colors.GRAY);
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
                            Object value = readStaticFieldValue(field);
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
                            cmd.print(Text.zhEn(" [无法访问: ", " [inaccessible: ").text(), Colors.RED);
                            String msg = e.getMessage();
                            cmd.print(msg != null ? msg : Text.zhEn("暂无错误信息", "no error message").text(), Colors.RED);
                            cmd.print("]", Colors.RED);
                        }
                    }

                    cmd.println("");

                    if (showHierarchy && fieldInfo.getDeclaringClass() != null && !fieldInfo.getDeclaringClass().equals(targetClass.getName())) {
                        cmd.print(Text.zhEn("    └─> 继承自 ", "    └─> inherited from ").text(), Colors.GRAY);
                        cmd.println(fieldInfo.getDeclaringClass(), Colors.GREEN);
                    }
                }
            }
            cmd.print(ClassTexts.LABEL_FIELD_COUNT.text(), Colors.CYAN);
            cmd.println(String.valueOf(fieldMap.size()), Colors.YELLOW);
            cmd.println("");

            result.setFields(new ArrayList<>(fieldMap.values()));
        }

        if (showAll || showMethods) {
            cmd.println(Text.zhEn("=== 方法 ===", "=== Methods ===").text(), Colors.CYAN);
            Map<String, MethodInfo> methodMap = collectAllMethods(targetClass, context);
            Map<String, List<String>> methodInterfaceMap = collectMethodInterfaces(targetClass, context);
            if (methodMap.isEmpty()) {
                cmd.println(Text.zhEn("无方法", "No methods").text(), Colors.GRAY);
            } else {
                for (MethodInfo methodInfo : methodMap.values()) {
                    Method method = findDeclaredMethod(targetClass, methodInfo.getName(), methodInfo.getParameterTypes());
                    if (method == null) continue;

                    cmd.print("  ", Colors.GRAY);
                    DescriptorColorizer.printColoredDescriptor(cmd, method, !verbose);

                    String signature = methodInfo.getSignature();
                    List<String> interfaceSources = methodInterfaceMap.get(signature);

                    if (showHierarchy && interfaceSources != null && !interfaceSources.isEmpty()) {
                        cmd.println("");
                        cmd.print(Text.zhEn("      └─> 实现接口: ", "      └─> implements: ").text(), Colors.CYAN);
                        boolean first = true;
                        for (String iface : interfaceSources) {
                            if (!first) {
                                cmd.print(", ", Colors.WHITE);
                            }
                            cmd.print(iface, Colors.GREEN);
                            first = false;
                        }
                    }

                    if (showHierarchy && methodInfo.getDeclaringClass() != null && !methodInfo.getDeclaringClass().equals(targetClass.getName())) {
                        cmd.println("");
                        cmd.print(Text.zhEn("      └─> 继承自: ", "      └─> inherited from: ").text(), Colors.CYAN);
                        cmd.print(methodInfo.getDeclaringClass(), Colors.GREEN);
                    }

                    cmd.println("");

                }
            }
            cmd.print(Text.zhEn("方法总数: ", "Method count: ").text(), Colors.CYAN);
            cmd.println(String.valueOf(methodMap.size()), Colors.YELLOW);
            cmd.println("");

            result.setMethods(new ArrayList<>(methodMap.values()));
        }

        if (showAll || showConstructors) {
            cmd.println(Text.zhEn("=== 构造函数 ===", "=== Constructors ===").text(), Colors.CYAN);
            Constructor<?>[] constructors = targetClass.getDeclaredConstructors();
            List<MethodInfo> constructorList = new ArrayList<>();
            if (constructors.length == 0) {
                cmd.println(Text.zhEn("无构造函数", "No constructors").text(), Colors.GRAY);
            } else {
                for (Constructor<?> constructor : constructors) {
                    cmd.print("  ", Colors.GRAY);
                    DescriptorColorizer.printColoredDescriptor(cmd, constructor, !verbose);
                    cmd.println("");
                    constructorList.add(MethodInfo.fromConstructor(constructor));
                }
            }
            cmd.print(Text.zhEn("构造函数总数: ", "Constructor count: ").text(), Colors.CYAN);
            cmd.println(String.valueOf(constructors.length), Colors.YELLOW);
            cmd.println("");
            result.setConstructors(constructorList);
        }

        if (showAll || showModifiers) {
            cmd.println(Text.zhEn("=== 类修饰符 ===", "=== Class Modifiers ===").text(), Colors.CYAN);
            int mods = targetClass.getModifiers();
            cmd.print(CliMessages.LABEL_MODIFIERS.text(), Colors.CYAN);
            cmd.println(Modifier.toString(mods), Colors.YELLOW);

            String flags = ((targetClass.isInterface() ? Text.zhEn("接口 ", "interface ").text() : "") +
                    (targetClass.isArray() ? Text.zhEn("数组 ", "array ").text() : "") +
                    (targetClass.isEnum() ? Text.zhEn("枚举 ", "enum ").text() : "") +
                    (targetClass.isAnnotation() ? Text.zhEn("注解 ", "annotation ").text() : "") +
                    (Modifier.isAbstract(mods) ? Text.zhEn("抽象 ", "abstract ").text() : "") +
                    (Modifier.isFinal(mods) ? "final " : "") +
                    (targetClass.isAnonymousClass() ? Text.zhEn("匿名类", "anonymous class").text() : "")).trim();

            if (!flags.isEmpty()) {
                cmd.print(ClassTexts.LABEL_FLAGS.text(), Colors.CYAN);
                cmd.println(flags, Colors.BLUE);
            }
            cmd.println("");
        }

        if (showStats) {
            cmd.println(Text.zhEn("=== 统计信息 ===", "=== Statistics ===").text(), Colors.CYAN);
            Map<String, FieldInfo> fieldMap = collectAllFields(targetClass, context);
            Map<String, MethodInfo> methodMap = collectAllMethods(targetClass, context);
            Constructor<?>[] constructors = targetClass.getDeclaredConstructors();
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

            cmd.print(CliMessages.LABEL_FIELDS.text(), Colors.CYAN);
            cmd.print(ClassTexts.COUNT_UNIT.format(fieldMap.size()), Colors.YELLOW);
            cmd.print(ClassTexts.LABEL_STATIC_COUNT.text(), Colors.GRAY);
            cmd.print(staticFieldCount, Colors.GREEN);
            cmd.print(ClassTexts.LABEL_INSTANCE_COUNT.text(), Colors.GRAY);
            cmd.print(instanceFieldCount, Colors.GREEN);
            cmd.println(")", Colors.GRAY);

            cmd.print(CliMessages.LABEL_METHODS.text(), Colors.CYAN);
            cmd.print(ClassTexts.COUNT_UNIT.format(methodMap.size()), Colors.YELLOW);
            cmd.print(ClassTexts.LABEL_STATIC_COUNT.text(), Colors.GRAY);
            cmd.print(staticMethodCount, Colors.GREEN);
            cmd.print(ClassTexts.LABEL_INSTANCE_COUNT.text(), Colors.GRAY);
            cmd.print(instanceMethodCount, Colors.GREEN);
            cmd.println(")", Colors.GRAY);

            cmd.print(CliMessages.LABEL_CONSTRUCTORS.text(), Colors.CYAN);
            cmd.println(ClassTexts.COUNT_UNIT.format(constructors.length), Colors.YELLOW);

            cmd.print(CliMessages.LABEL_INTERFACES.text(), Colors.CYAN);
            cmd.println(ClassTexts.COUNT_UNIT.format(interfaces.length), Colors.YELLOW);

            Class<?> current = targetClass;
            int depth = 0;
            while (current != null && current != Object.class) { depth++; current = current.getSuperclass(); }
            cmd.print(Text.zhEn("继承深度: ", "Inheritance depth: ").text(), Colors.CYAN);
            cmd.println(Text.zhEn("%s 层", "%s level(s)").format(depth), Colors.YELLOW);
            cmd.println("");
        }

        if (showAll) {
            cmd.println(Text.zhEn("=== 类信息 ===", "=== Class Info ===").text(), Colors.CYAN);
            cmd.print(CliMessages.LABEL_CLASS_NAME.text(), Colors.CYAN);
            cmd.println(targetClass.getName(), Colors.GREEN);
            cmd.print(Text.zhEn("简单类名: ", "Simple name: ").text(), Colors.CYAN);
            cmd.println(targetClass.getSimpleName(), Colors.GREEN);
            cmd.print(CliMessages.LABEL_PACKAGE_NAME.text(), Colors.CYAN);
            cmd.println(targetClass.getPackage() != null
                    ? targetClass.getPackage().getName() : CliMessages.VALUE_NONE.text(), Colors.GREEN);

            String typeInfo = Text.zhEn("普通类", "plain class").text();
            if (targetClass.isArray()) {
                typeInfo = Text.zhEn("数组类型", "array type").text();
            } else if (targetClass.isPrimitive()) {
                typeInfo = Text.zhEn("原始类型", "primitive type").text();
            } else if (targetClass.isEnum()) {
                typeInfo = Text.zhEn("枚举", "enum").text();
            } else if (targetClass.isAnnotation()) {
                typeInfo = Text.zhEn("注解", "annotation").text();
            } else if (targetClass.isInterface()) {
                typeInfo = Text.zhEn("接口", "interface").text();
            }
            cmd.print(ClassTexts.LABEL_TYPE.text(), Colors.CYAN);
            cmd.println(typeInfo, Colors.MAGENTA);

            List<String> modifiers = new ArrayList<>();
            if (Modifier.isAbstract(targetClass.getModifiers())) {
                modifiers.add(Text.zhEn("抽象类", "abstract class").text());
            }
            if (Modifier.isFinal(targetClass.getModifiers())) {
                modifiers.add(Text.zhEn("final类", "final class").text());
            }
            if (!modifiers.isEmpty()) {
                cmd.print(CliMessages.LABEL_MODIFIERS.text(), Colors.CYAN);
                for (int i = 0; i < modifiers.size(); i++) {
                    if (i > 0) {
                        cmd.print(", ", Colors.WHITE);
                    }
                    cmd.print(modifiers.get(i), Colors.YELLOW);
                }
                cmd.println("");
            }
            cmd.println("");

            cmd.println(Text.zhEn("=== 父类 ===", "=== Super Class ===").text(), Colors.CYAN);
            Class<?> superClass = targetClass.getSuperclass();
            if (superClass != null) {
                cmd.println(superClass.getName(), Colors.GREEN);
            } else {
                cmd.println(Text.zhEn("无父类", "No super class").text(), Colors.GRAY);
            }
            cmd.println("");

            cmd.println(Text.zhEn("=== 实现的接口 ===", "=== Implemented Interfaces ===").text(), Colors.CYAN);
            Class<?>[] interfaces = targetClass.getInterfaces();
            if (interfaces.length == 0) {
                cmd.println(Text.zhEn("无接口", "No interfaces").text(), Colors.GRAY);
            } else {
                for (Class<?> _interface : interfaces) {
                    cmd.print("  - ", Colors.GRAY);
                    cmd.println(_interface.getName(), Colors.GREEN);
                }
            }
            cmd.print(Text.zhEn("接口总数: ", "Interface count: ").text(), Colors.CYAN);
            cmd.println(String.valueOf(interfaces.length), Colors.YELLOW);
            cmd.println("");

            List<String> interfaceList = new ArrayList<>();
            for (Class<?> _interface : interfaces) {
                interfaceList.add(_interface.getName());
            }
            result.setInterfaces(interfaceList);

            cmd.println(Text.zhEn("=== 包信息 ===", "=== Package Info ===").text(), Colors.CYAN);
            cmd.print(Text.zhEn("包: ", "Package: ").text(), Colors.CYAN);
            cmd.println(context.targetPackage() != null ? context.targetPackage() : "default", Colors.GREEN);
            cmd.print(CliMessages.LABEL_CLASS_LOADER.text(), Colors.CYAN);
            cmd.println(context.classLoader() != null
                    ? context.classLoader().toString() : CliMessages.VALUE_NONE.text(), Colors.LIGHT_GREEN);
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
                cmd.println(GsonFactory.getPrettyInstance().toJson(result), Colors.WHITE);
            } catch (Exception e) {
                context.logger().warn("JSON输出失败: " + e.getMessage());
            }
        }

        return result;
    }



    /**
     * 读取静态字段的值。
     *
     * <p>优先走 Hook API：它从 boot classpath 发起访问，因此能读到 boot 类
     * （如 {@code java.lang.String.serialPersistentFields}）的 private 成员；纯反射在这种
     * 情况下会被 ART 的访问检查拒绝（"Class X cannot access private ... of class java.lang.String"）。
     * 未注入 Xposed（Hook 不可用）或该字段读不到时退回反射，与该判断引入前的行为一致。</p>
     */
    private static Object readStaticFieldValue(Field field) throws IllegalAccessException {
        try {
            return HookAPI.getStaticObjectField(field.getDeclaringClass(), field.getName());
        } catch (Throwable hookUnavailable) {
            field.setAccessible(true);
            return field.get(null);
        }
    }

    private Map<String, FieldInfo> collectAllFields(Class<?> targetClass, ClassCommandContext<AnalyzeClassRequest> context) {
        Map<String, FieldInfo> fieldMap = new LinkedHashMap<>();

        Class<?> current = targetClass;
        while (current != null) {
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
        while (current != null) {
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
