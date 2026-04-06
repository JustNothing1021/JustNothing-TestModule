package com.justnothing.testmodule.command.functions.classcmd;


import com.justnothing.testmodule.command.output.Colors;
import com.justnothing.testmodule.command.utils.CommandExceptionHandler;
import com.justnothing.testmodule.utils.reflect.ClassResolver;
import com.justnothing.testmodule.utils.reflect.DescriptorColorizer;
import com.justnothing.testmodule.utils.reflect.ReflectionUtils;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;


public class InfoCommand extends AbstractClassCommand {

    public InfoCommand() {
        super("class info");
    }

    @Override
    protected String executeInternal(ClassCommandContext context) throws Exception {
        String[] args = context.getArgs();
        
        if (args.length < 1) {
            return CommandExceptionHandler.handleException(
                "class info", 
                new IllegalArgumentException("参数不足, 需要至少1个参数: class info <class_name>"),
                context.getExecContext(),
                "参数错误"
            );
        }


        boolean showInterfaces = false;
        boolean showConstructors = false;
        boolean showSuper = false;
        boolean showModifiers = false;
        boolean showAll = true;
        boolean verbose = false;

        for (int i = 0; i < args.length - 1; i++) {
            String arg = args[i];
            switch (arg) {
                case "-v", "--verbose" -> verbose = true;
                case "-i", "--interfaces" -> {
                    showInterfaces = true;
                    showAll = false;
                }
                case "-c", "--constructors" -> {
                    showConstructors = true;
                    showAll = false;
                }
                case "-s", "--super" -> {
                    showSuper = true;
                    showAll = false;
                }
                case "-m", "--modifiers" -> {
                    showModifiers = true;
                    showAll = false;
                }
                case "-a", "--all" -> showAll = true;
            }
        }

        String className = args[args.length - 1];

        
        Class<?> targetClass = ClassResolver.findClassOrFail(className, context.getClassLoader());

        if (showAll || showModifiers) {
            context.getExecContext().print("类名: ", Colors.CYAN);
            context.getExecContext().println(targetClass.getName(), Colors.WHITE);
            context.getExecContext().print("修饰符: ", Colors.CYAN);
            context.getExecContext().println(Modifier.toString(targetClass.getModifiers()), Colors.YELLOW);
            
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
                context.getExecContext().print("特性: ", Colors.CYAN);
                context.getExecContext().println(flags.toString().trim(), Colors.BLUE);
            }
            context.getExecContext().println("");
        }
        
        if (showAll || showSuper) {
            Class<?> superClass = targetClass.getSuperclass();
            context.getExecContext().print("父类: ", Colors.CYAN);
            if (superClass != null) {
                context.getExecContext().println(superClass.getName(), Colors.GREEN);
            } else {
                context.getExecContext().println("无", Colors.GRAY);
            }
            context.getExecContext().println("");
        }
        
        if (showAll || showInterfaces) {
            Class<?>[] interfaces = targetClass.getInterfaces();
            context.getExecContext().print("实现的接口", Colors.CYAN);
            context.getExecContext().println(" (" + interfaces.length + "个):", Colors.CYAN);
            if (interfaces.length > 0) {
                for (Class<?> _interface : interfaces) {
                    context.getExecContext().print("  - ", Colors.GRAY);
                    context.getExecContext().println(_interface.getName(), Colors.GREEN);
                }
            } else {
                context.getExecContext().println("  无", Colors.GRAY);
            }
            context.getExecContext().println("");
        }
        
        if (showAll || showConstructors) {
            Constructor<?>[] constructors = targetClass.getDeclaredConstructors();
            context.getExecContext().print("构造函数", Colors.CYAN);
            context.getExecContext().println(" (" + constructors.length + "个):", Colors.CYAN);
            for (Constructor<?> constructor : constructors) {
                context.getExecContext().print("  ", Colors.GRAY);
                DescriptorColorizer.printColoredDescriptor(context.getExecContext(), constructor, !verbose);
                context.getExecContext().println("");
            }
            context.getExecContext().println("");
        }
        
        if (showAll) {
            Field[] fields = targetClass.getDeclaredFields();
            context.getExecContext().print("字段", Colors.CYAN);
            context.getExecContext().println(" (" + fields.length + "个):", Colors.CYAN);
            for (Field field : fields) {
                context.getExecContext().print("  ", Colors.GRAY);
                DescriptorColorizer.printColoredDescriptor(context.getExecContext(), field, !verbose);
                context.getExecContext().println("");
            }
            context.getExecContext().println("");
            
            Method[] methods = targetClass.getDeclaredMethods();
            context.getExecContext().print("方法", Colors.CYAN);
            context.getExecContext().println(" (" + methods.length + "个):", Colors.CYAN);
            for (Method method : methods) {
                context.getExecContext().print("  ", Colors.GRAY);
                DescriptorColorizer.printColoredDescriptor(context.getExecContext(), method, !verbose);
                context.getExecContext().println("");
            }
        }
        
        context.getLogger().info("查看类信息: " + className);
        return "";
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
