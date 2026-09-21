package com.justnothing.testmodule.command.functions.classcmd.impl;


import com.justnothing.testmodule.command.framework.error.IllegalCommandLineArgumentException;
import com.justnothing.testmodule.command.framework.annotation.SubCommandInfo;
import com.justnothing.testmodule.command.framework.i18n.CliMessages;
import com.justnothing.testmodule.command.framework.i18n.Text;
import com.justnothing.testmodule.command.functions.classcmd.ClassTexts;
import com.justnothing.testmodule.command.functions.classcmd.model.ClassCommandContext;
import com.justnothing.testmodule.command.functions.classcmd.model.ClassInfo;
import com.justnothing.testmodule.command.functions.classcmd.model.FieldInfo;
import com.justnothing.testmodule.command.functions.classcmd.model.MethodInfo;
import com.justnothing.testmodule.command.functions.classcmd.request.ClassInfoRequest;
import com.justnothing.testmodule.command.functions.classcmd.response.ClassInfoResult;
import com.justnothing.testmodule.command.framework.output.Colors;
import com.justnothing.testmodule.utils.reflect.ClassResolver;
import com.justnothing.testmodule.utils.format.DescriptorColorizer;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;


@SubCommandInfo(
    description = ClassTexts.SUB_CLASS_INFO_DESC,
    usage = "class info [options] <class_name>",
    examples = {
        "class info java.lang.String",
        "class info -v java.lang.Object",
        "class info --interfaces java.util.ArrayList"
    },
    optionsDesc = ClassTexts.SUB_CLASS_INFO_OPTIONS
)
public class ClassInfoCommand extends AbstractClassCommand<ClassInfoRequest, ClassInfoResult> {

    public ClassInfoCommand() {
        super("class info", ClassInfoRequest.class, ClassInfoResult.class);
    }

    @Override
    protected ClassInfoResult executeClassCommand(ClassCommandContext<ClassInfoRequest> context) throws Exception {
        ClassInfoRequest request = context.execContext().getCommandRequest();
        String className = request.getClassName();

        if (className == null || className.isEmpty()) {
            throw new IllegalCommandLineArgumentException(Text.zhEn(
                    "参数不足, 需要至少1个参数: class info <class_name>",
                    "Not enough arguments; at least 1 is required: class info <class_name>").text());
        }

        boolean showInterfaces = request.isShowInterfaces();
        boolean showConstructors = request.isShowConstructors();
        boolean showSuper = request.isShowSuper();
        boolean showModifiers = request.isShowModifiers();
        boolean showFields = request.isShowFields();
        boolean showMethods = request.isShowMethods();
        boolean showAll = request.isShowAll();
        boolean verbose = request.isVerbose();

        ClassInfoResult result = new ClassInfoResult();
        ClassInfo classInfo = new ClassInfo();

        Class<?> targetClass = ClassResolver.findClassOrFail(className, context.classLoader());

        if ((showAll && !request.wasModifiersExplicitlySet()) || showModifiers) {
            context.execContext().print(CliMessages.LABEL_CLASS_NAME.text(), Colors.CYAN);
            context.execContext().println(targetClass.getName(), Colors.WHITE);
            context.execContext().print(CliMessages.LABEL_MODIFIERS.text(), Colors.CYAN);
            context.execContext().println(Modifier.toString(targetClass.getModifiers()), Colors.YELLOW);

            StringBuilder flags = new StringBuilder();
            if (targetClass.isInterface()) flags.append(Text.zhEn("接口类 ", "interface ").text());
            if (targetClass.isArray()) flags.append(Text.zhEn("数组 ", "array ").text());
            if (targetClass.isPrimitive()) flags.append(Text.zhEn("原始类型 ", "primitive ").text());
            if (targetClass.isAnnotation()) flags.append(Text.zhEn("注解 ", "annotation ").text());
            if (targetClass.isEnum()) flags.append(Text.zhEn("枚举 ", "enum ").text());
            if (targetClass.isAnonymousClass()) flags.append(Text.zhEn("匿名类 ", "anonymous class ").text());
            if (targetClass.isMemberClass()) flags.append(Text.zhEn("成员类 ", "member class ").text());
            if (targetClass.isLocalClass()) flags.append(Text.zhEn("本地类 ", "local class ").text());
            if (targetClass.isSynthetic()) flags.append(Text.zhEn("合成类 ", "synthetic class ").text());
            if (flags.length() > 0) {
                context.execContext().print(ClassTexts.LABEL_FLAGS.text(), Colors.CYAN);
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

        if ((showAll && !request.wasSuperExplicitlySet()) || showSuper) {
            Class<?> superClass = targetClass.getSuperclass();
            context.execContext().print(CliMessages.LABEL_SUPER_CLASS.text(), Colors.CYAN);
            if (superClass != null) {
                context.execContext().println(superClass.getName(), Colors.GREEN);
                classInfo.setSuperClass(superClass.getName());
            } else {
                context.execContext().println(CliMessages.VALUE_NONE.text(), Colors.GRAY);
            }
            context.execContext().println("");
        }

        if ((showAll && !request.wasInterfacesExplicitlySet()) || showInterfaces) {
            Class<?>[] interfaces = targetClass.getInterfaces();
            context.execContext().print(Text.zhEn("实现的接口", "Implemented interfaces").text(), Colors.CYAN);
            context.execContext().println(ClassTexts.COUNT_PAREN.format(interfaces.length), Colors.CYAN);

            List<String> interfaceList = new ArrayList<>();
            if (interfaces.length > 0) {
                for (Class<?> _interface : interfaces) {
                    context.execContext().print("  - ", Colors.GRAY);
                    context.execContext().println(_interface.getName(), Colors.GREEN);
                    interfaceList.add(_interface.getName());
                }
            } else {
                context.execContext().println(Text.zhEn("  无", "  none").text(), Colors.GRAY);
            }
            context.execContext().println("");

            classInfo.setInterfaces(interfaceList);
        }

        if ((showAll && !request.wasConstructorsExplicitlySet()) || showConstructors) {
            Constructor<?>[] constructors = targetClass.getDeclaredConstructors();
            context.execContext().print(Text.zhEn("构造函数", "Constructors").text(), Colors.CYAN);
            context.execContext().println(ClassTexts.COUNT_PAREN.format(constructors.length), Colors.CYAN);

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

        if (showAll || showFields) {
            Field[] fields = targetClass.getDeclaredFields();
            context.execContext().print(Text.zhEn("字段", "Fields").text(), Colors.CYAN);
            context.execContext().println(ClassTexts.COUNT_PAREN.format(fields.length), Colors.CYAN);

            List<FieldInfo> fieldList = new ArrayList<>();
            for (Field field : fields) {
                context.execContext().print("  ", Colors.GRAY);
                DescriptorColorizer.printColoredDescriptor(context.execContext(), field, !verbose);
                context.execContext().println("");
                fieldList.add(FieldInfo.fromField(field));
            }
            context.execContext().println("");

            classInfo.setFields(fieldList);
        }

        if (showAll || showMethods) {
            Method[] methods = targetClass.getDeclaredMethods();
            context.execContext().print(Text.zhEn("方法", "Methods").text(), Colors.CYAN);
            context.execContext().println(ClassTexts.COUNT_PAREN.format(methods.length), Colors.CYAN);

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
}
