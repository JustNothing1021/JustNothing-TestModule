package com.justnothing.testmodule.command.functions.classcmd;


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
            return "参数不足, 需要至少1个参数: class info <class_name>\n" + getHelpText();
        }


        boolean showInterfaces = false;
        boolean showConstructors = false;
        boolean showSuper = false;
        boolean showModifiers = false;
        boolean showAll = true;

        for (int i = 0; i < args.length - 1; i++) {
            String arg = args[i];
            switch (arg) {
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

        
        Class<?> targetClass = context.loadClass(className);
        if (targetClass == null) {
            context.getLogger().error("找不到类: " + className);
            return "找不到类: " + className;
        }

        StringBuilder result = new StringBuilder();
        
        if (showAll || showModifiers) {
            result.append("类名: ").append(targetClass.getName()).append("\n");
            result.append("修饰符: ").append(Modifier.toString(targetClass.getModifiers())).append("\n");
            if (targetClass.isInterface()) result.append("接口类 ");
            if (targetClass.isArray()) result.append("数组 ");
            if (targetClass.isPrimitive()) result.append("原始类型 ");
            if (targetClass.isAnnotation()) result.append("注解 ");
            if (targetClass.isEnum()) result.append("枚举 ");
            if (targetClass.isAnonymousClass()) result.append("匿名类 ");
            if (targetClass.isMemberClass()) result.append("成员类 ");
            if (targetClass.isLocalClass()) result.append("本地类 ");
            if (targetClass.isSynthetic()) result.append("合成类 ");
            result.append("\n");
        }
        
        if (showAll || showSuper) {
            Class<?> superClass = targetClass.getSuperclass();
            if (superClass != null) {
                result.append("父类: ").append(superClass.getName()).append("\n");
            } else {
                result.append("父类: 无\n");
            }
            result.append("\n");
        }
        
        if (showAll || showInterfaces) {
            Class<?>[] interfaces = targetClass.getInterfaces();
            if (interfaces.length > 0) {
                result.append("实现的接口:\n");
                for (Class<?> _interface : interfaces) {
                    result.append("  - ").append(_interface.getName()).append("\n");
                }
            } else {
                result.append("实现的接口: 无\n");
            }
            result.append("\n");
        }
        
        if (showAll || showConstructors) {
            Constructor<?>[] constructors = targetClass.getDeclaredConstructors();
            result.append("构造函数 (").append(constructors.length).append("个):\n");
            for (Constructor<?> constructor : constructors) {
                result.append("  ").append(constructor.toString()).append("\n");
            }
            result.append("\n");
        }
        
        if (showAll) {
            Field[] fields = targetClass.getDeclaredFields();
            result.append("字段 (").append(fields.length).append("个):\n");
            for (Field field : fields) {
                result.append("  ").append(field.toString()).append("\n");
            }
            result.append("\n");
            
            Method[] methods = targetClass.getDeclaredMethods();
            result.append("方法 (").append(methods.length).append("个):\n");
            for (Method method : methods) {
                result.append("  ").append(method.toString()).append("\n");
            }
        }
        
        context.getLogger().info("查看类信息: " + className);
        return result.toString();
    }

    @Override
    public String getHelpText() {
        return """
            语法: class info [options] <class_name>
            
            查看类的详细信息.
            
            选项:
                -i, --interfaces    显示实现的接口
                -c, --constructors  显示构造函数
                -s, --super         显示父类信息
                -m, --modifiers     显示修饰符信息
                -a, --all           显示所有信息 (默认)
            
            示例:
                class info java.lang.String
                class info -i java.util.ArrayList
                class info -c android.view.View
            """;
    }
}
