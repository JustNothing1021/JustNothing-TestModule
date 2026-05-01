package com.justnothing.testmodule.command.functions.classcmd.impl;


import com.justnothing.testmodule.command.functions.classcmd.AbstractClassCommand;
import com.justnothing.testmodule.command.functions.classcmd.ClassCommandContext;
import com.justnothing.testmodule.command.functions.classcmd.model.ClassInfo;
import com.justnothing.testmodule.command.functions.classcmd.model.FieldInfo;
import com.justnothing.testmodule.command.functions.classcmd.model.MethodInfo;
import com.justnothing.testmodule.command.functions.classcmd.request.ClassInfoRequest;
import com.justnothing.testmodule.command.functions.classcmd.response.ClassInfoResult;
import com.justnothing.testmodule.command.output.Colors;
import com.justnothing.testmodule.command.utils.CommandExceptionHandler;
import com.justnothing.testmodule.utils.reflect.ClassResolver;
import com.justnothing.testmodule.utils.reflect.DescriptorColorizer;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.function.Supplier;


public class InfoCommand extends AbstractClassCommand<ClassInfoRequest, ClassInfoResult> {

    public InfoCommand() {
        super("class info", ClassInfoRequest.class, ClassInfoResult.class);
    }

    @Override
    protected ClassInfoResult executeClassCommand(ClassCommandContext<ClassInfoRequest> context) throws Exception {
        String[] args = context.args();

        if (args.length < 1) {
            CommandExceptionHandler.handleException(
                "class info",
                new IllegalArgumentException("参数不足, 需要至少1个参数: class info <class_name>"),
                context.execContext(),
                "参数错误"
            );
            return null;
        }

        ClassInfoRequest request = context.execContext().getCommandRequest();
        boolean showInterfaces = request.isShowInterfaces();
        boolean showConstructors = request.isShowConstructors();
        boolean showSuper = request.isShowSuper();
        boolean showModifiers = request.isShowModifiers();
        boolean showAll = request.isShowAll();
        boolean verbose = request.isVerbose();
        String className = request.getClassName();

        ClassInfoResult result = new ClassInfoResult();
        ClassInfo classInfo = new ClassInfo();

        Class<?> targetClass = ClassResolver.findClassOrFail(className, context.classLoader());

        if (showAll || showModifiers) {
            context.execContext().print("类名: ", Colors.CYAN);
            context.execContext().println(targetClass.getName(), Colors.WHITE);
            context.execContext().print("修饰符: ", Colors.CYAN);
            context.execContext().println(Modifier.toString(targetClass.getModifiers()), Colors.YELLOW);

            StringBuilder flags = new StringBuilder();
            if (targetClass.isInterface()) flags.append("接口类 ");
            if (targetClass.isArray()) flags.append("数组 ");
            if (targetClass.isPrimitive()) flags.append("原始类型 ");
            if (targetClass.isAnnotation()) flags.append("注解 ");
            if (targetClass.isEnum()) flags.append("枚举 ");
            if (targetClass.isAnonymousClass()) flags.append("匿名类 ");
            if (targetClass.isMemberClass()) flags.append("成员类 ");
            if (targetClass.isLocalClass()) flags.append("本地类 ");
            if (targetClass.isSynthetic()) flags.append("合成类 ");
            if (flags.length() > 0) {
                context.execContext().print("特性: ", Colors.CYAN);
                context.execContext().println(flags.toString().trim(), Colors.BLUE);
            }
            context.execContext().println("");

            classInfo.setName(targetClass.getName());
            classInfo.setModifiers(targetClass.getModifiers());
            classInfo.setInterface(targetClass.isInterface());
            classInfo.setAnnotation(targetClass.isAnnotation());
            classInfo.setEnum(targetClass.isEnum());
            classInfo.setAbstract(Modifier.isAbstract(targetClass.getModifiers()));
            classInfo.setFinal(Modifier.isFinal(targetClass.getModifiers()));
        }

        if (showAll || showSuper) {
            Class<?> superClass = targetClass.getSuperclass();
            context.execContext().print("父类: ", Colors.CYAN);
            if (superClass != null) {
                context.execContext().println(superClass.getName(), Colors.GREEN);
                classInfo.setSuperClass(superClass.getName());
            } else {
                context.execContext().println("无", Colors.GRAY);
            }
            context.execContext().println("");
        }

        if (showAll || showInterfaces) {
            Class<?>[] interfaces = targetClass.getInterfaces();
            context.execContext().print("实现的接口", Colors.CYAN);
            context.execContext().println(" (" + interfaces.length + "个):", Colors.CYAN);

            List<String> interfaceList = new ArrayList<>();
            if (interfaces.length > 0) {
                for (Class<?> _interface : interfaces) {
                    context.execContext().print("  - ", Colors.GRAY);
                    context.execContext().println(_interface.getName(), Colors.GREEN);
                    interfaceList.add(_interface.getName());
                }
            } else {
                context.execContext().println("  无", Colors.GRAY);
            }
            context.execContext().println("");

            classInfo.setInterfaces(interfaceList);
        }

        if (showAll || showConstructors) {
            Constructor<?>[] constructors = targetClass.getDeclaredConstructors();
            context.execContext().print("构造函数", Colors.CYAN);
            context.execContext().println(" (" + constructors.length + "个):", Colors.CYAN);

            List<MethodInfo> constructorList = new ArrayList<>();
            for (Constructor<?> constructor : constructors) {
                context.execContext().print("  ", Colors.GRAY);
                DescriptorColorizer.printColoredDescriptor(context.execContext(), constructor, !verbose);
                context.execContext().println("");
                constructorList.add(MethodInfo.fromConstructor(constructor));
            }
            context.execContext().println("");

            classInfo.setConstructors(constructorList);
        }

        if (showAll) {
            Field[] fields = targetClass.getDeclaredFields();
            context.execContext().print("字段", Colors.CYAN);
            context.execContext().println(" (" + fields.length + "个):", Colors.CYAN);

            List<FieldInfo> fieldList = new ArrayList<>();
            for (Field field : fields) {
                context.execContext().print("  ", Colors.GRAY);
                DescriptorColorizer.printColoredDescriptor(context.execContext(), field, !verbose);
                context.execContext().println("");
                fieldList.add(FieldInfo.fromField(field));
            }
            context.execContext().println("");

            classInfo.setFields(fieldList);

            Method[] methods = targetClass.getDeclaredMethods();
            context.execContext().print("方法", Colors.CYAN);
            context.execContext().println(" (" + methods.length + "个):", Colors.CYAN);

            List<MethodInfo> methodList = new ArrayList<>();
            for (Method method : methods) {
                context.execContext().print("  ", Colors.GRAY);
                DescriptorColorizer.printColoredDescriptor(context.execContext(), method, !verbose);
                context.execContext().println("");
                methodList.add(MethodInfo.fromMethod(method));
            }

            classInfo.setMethods(methodList);
        }

        classInfo.setClassLoader(targetClass.getClassLoader() != null
            ? targetClass.getClassLoader().toString()
            : "Bootstrap ClassLoader");

        result.setClassInfo(classInfo);
        context.logger().info("查看类信息: " + className);
        return result;
    }

    @Override
    public String getHelpText() {
        return """
            语法: class info [options] <class_name>

            查看类的详细信息.

                -v, --verbose        显示详细信息
                -i, --interfaces     显示实现的接口
                -c, --constructors   显示构造函数
                -s, --super          显示父类信息
                -m, --modifiers      显示修饰符信息
                -a, --all            显示所有信息 (默认)

            示例:
                class info java.lang.String
                class info -i java.util.ArrayList
                class info -c android.view.View
            """;
    }
}
