package com.justnothing.testmodule.command.functions.classcmd;

import com.justnothing.testmodule.command.output.Colors;
import com.justnothing.testmodule.command.utils.CommandExceptionHandler;
import com.justnothing.testmodule.utils.reflect.DescriptorColorizer;
import com.justnothing.testmodule.utils.reflect.ReflectionUtils;
import com.justnothing.testmodule.utils.reflect.ClassResolver;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ConstructorCommand extends AbstractClassCommand {

    public ConstructorCommand() {
        super("class constructor");
    }

    @Override
    protected String executeInternal(ClassCommandContext context) throws Exception {
        String[] args = context.getArgs();
        
        if (args.length < 1) {
            return CommandExceptionHandler.handleException(
                "class constructor",
                new IllegalArgumentException("参数不足, 需要至少1个参数: class constructor <class_name> [params...]"),
                context.getExecContext(),
                "参数错误"
            );
        }

        String className = args[0];
        List<Object> params = new ArrayList<>();
        List<Class<?>> paramTypes = new ArrayList<>();

        for (int i = 1; i < args.length; i++) {
            String paramStr = args[i];

            try {
                ExpressionParser.ParseResult result = ExpressionParser.parse(paramStr, context.getClassLoader());
                params.add(result.value);
                paramTypes.add(result.type);
                
                String typeHint = result.hasTypeHint ? " (有类型提示)" : "";
                String valueStr = result.value != null ? result.value.toString() : "null";
                context.getLogger().info("参数" + (params.size() - 1) +
                        ": (" + result.type.getName() + ")" + valueStr + typeHint);
            } catch (Exception e) {
                context.getLogger().warn("无法解析参数: " + paramStr);
                Map<String, Object> errContext = new HashMap<>();
                errContext.put("参数索引", i - 1);
                errContext.put("参数表达式", paramStr);
                return CommandExceptionHandler.handleException("class constructor", e, context.getExecContext(), errContext, "解析参数失败");
            }
        }

        Class<?> targetClass = ClassResolver.findClassOrFail(className, context.getClassLoader());

        if (!params.isEmpty()) {
            context.getExecContext().println("调用参数：", Colors.CYAN);
            for (int i = 0; i < params.size(); i++) {
                context.getExecContext().print("参数", Colors.YELLOW);
                context.getExecContext().print("[", Colors.WHITE);
                context.getExecContext().print(String.valueOf(i), Colors.LIGHT_GREEN);
                context.getExecContext().print("]", Colors.WHITE);
                context.getExecContext().print(" = ", Colors.WHITE);
                Object value = params.get(i);
                if (value == null) {
                    context.getExecContext().print("null", Colors.LIGHT_BLUE);
                } else {
                    context.getExecContext().print(String.valueOf(value), Colors.LIGHT_BLUE);
                }
                context.getExecContext().print(" (", Colors.WHITE);
                context.getExecContext().print(paramTypes.get(i).getName(), Colors.GREEN);
                context.getExecContext().println(")", Colors.WHITE);
            }
            context.getExecContext().println("");
        }

        Constructor<?> constructor = ReflectionUtils.findConstructor(targetClass, paramTypes.toArray(new Class<?>[0]));

        if (constructor == null) {
            context.getLogger().warn("没有找到类" + className + "的匹配构造函数");
            context.getExecContext().println("没有找到匹配的构造函数", Colors.RED);
            context.getExecContext().print("参数类型: ", Colors.CYAN);
            for (int i = 0; i < paramTypes.size(); i++) {
                context.getExecContext().print(paramTypes.get(i).getName(), Colors.GREEN);
                if (i < paramTypes.size() - 1) {
                    context.getExecContext().print(", ", Colors.WHITE);
                }
            }
            context.getExecContext().println("");
            context.getExecContext().println("");
            context.getExecContext().println("可用的构造函数:", Colors.CYAN);
            for (Constructor<?> c : targetClass.getDeclaredConstructors()) {
                context.getExecContext().print("  ", Colors.GRAY);
                DescriptorColorizer.printColoredDescriptor(context.getExecContext(), c, true);
                context.getExecContext().println("");
            }
            return "";
        }

        context.getExecContext().print("找到构造函数: ", Colors.CYAN);
        DescriptorColorizer.printColoredDescriptor(context.getExecContext(), constructor, true);
        context.getExecContext().println("");
        context.getExecContext().println("");

        constructor.setAccessible(true);
        Object instance = constructor.newInstance(params.toArray());

        context.getLogger().info("创建实例成功: " + instance);
        context.getExecContext().println("创建实例成功", Colors.GREEN);
        context.getExecContext().println("============================", Colors.CYAN);
        context.getExecContext().println(String.valueOf(instance), Colors.WHITE);
        context.getExecContext().println("============================", Colors.CYAN);
        context.getExecContext().print("类型: ", Colors.CYAN);
        context.getExecContext().println(instance.getClass().getName(), Colors.YELLOW);
        context.getExecContext().print("Hash: ", Colors.CYAN);
        context.getExecContext().println(String.valueOf(System.identityHashCode(instance)), Colors.LIGHT_GREEN);
        return "";
    }

    @Override
    public String getHelpText() {
        return """
            语法: class constructor <class_name> [params...]
            
            创建类的实例。
            参数支持表达式语法，可以直接写值或使用类型提示。
            
            参数格式:
                - 直接表达式: 123, "hello", true, null
                - 带类型提示: int:123, String:"hello", boolean:true
            
            表达式支持:
                - 字面量: 123, 3.14, "text", true, null
                - 算术运算: 1 + 2, 10 * 5
                - 字符串拼接: "Hello " + "World"
                - 方法调用: Math.abs(-5)
                - 字段访问: SomeClass.FIELD
                - 对象创建: new ArrayList()
            
            示例:
                class constructor java.lang.Integer 114514
                class constructor java.lang.Integer int:114514
                class constructor java.lang.String "1919810"
                class constructor java.lang.String String:"1919810"
                class constructor java.util.ArrayList
                class constructor java.io.File "/sdcard/test.txt"
            """;
    }

}
