package com.justnothing.testmodule.command.functions.classcmd;

import com.justnothing.testmodule.command.utils.CommandExceptionHandler;
import com.justnothing.testmodule.utils.reflect.ReflectionUtils;

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
            context.getLogger().warn("参数不足, 需要至少1个参数");
            return getHelpText();
        }

        String className = args[0];
        List<Object> params = new ArrayList<>();
        List<Class<?>> paramTypes = new ArrayList<>();

        for (int i = 1; i < args.length; i++) {
            String paramStr = args[i];
            int colonIndex = paramStr.indexOf(':');
            if (colonIndex <= 0) {
                context.getLogger().warn("参数形式不正确，获取到的: " + paramStr);
                return "参数形式不正确: " + paramStr +
                        "; 应为Type:value" +
                        "\n\n示例: " +
                        "\n    Integer:114514, Boolean:true";
            }

            String typeName = paramStr.substring(0, colonIndex);
            String valueExpr = paramStr.substring(colonIndex + 1);

            try {
                Object parsedValue = TypeParser.parse(typeName, valueExpr, context.getClassLoader());
                params.add(parsedValue);
                assert parsedValue != null;
                paramTypes.add(parsedValue.getClass());
                context.getLogger().info("参数" + (params.size() - 1) +
                        ": (" + paramTypes.get(paramTypes.size()-1).getName() +")" +
                        params.get(paramTypes.size()-1).toString());
            } catch (Exception e) {
                context.getLogger().warn("无法解析参数" + paramStr);
                Map<String, Object> errContext = new HashMap<>();
                errContext.put("参数索引", i-1);
                errContext.put("参数字符串", paramStr);
                return CommandExceptionHandler.handleException("class constructor", e, context.getLogger(), errContext, "解析参数失败");
            }
        }

        Class<?> targetClass = context.loadClass(className);

        Constructor<?> constructor = ReflectionUtils.findConstructor(targetClass, paramTypes.toArray(new Class<?>[0]));

        if (constructor == null) {
            context.getLogger().warn("没有找到类" + className + "的匹配构造函数");
            StringBuilder sb = new StringBuilder();
            sb.append("没有找到匹配的构造函数\n");
            sb.append("参数类型: ");
            for (int i = 0; i < paramTypes.size(); i++) {
                sb.append(paramTypes.get(i).getSimpleName());
                if (i < paramTypes.size() - 1) sb.append(", ");
            }
            sb.append("\n\n");
            sb.append("可用的构造函数:\n");
            for (Constructor<?> c : targetClass.getDeclaredConstructors()) {
                sb.append("  ").append(ReflectionUtils.getDescriptor(c)).append("\n");
            }
            return sb.toString();
        }

        constructor.setAccessible(true);
        Object instance = constructor.newInstance(params.toArray());

        context.getLogger().info("创建实例成功: " + instance);
        return "创建实例成功\n============================" + instance +
                "\n============================" +
                "\n类型: " + instance.getClass().getName() +
                "\nHash: " + System.identityHashCode(instance);
    }

    @Override
    public String getHelpText() {
        return """
            语法: class constructor <class_name> [params...]
            
            创建类的实例.
            提供参数的格式: Type:value (e.g. Integer:114514)
            
            示例:
                class constructor java.lang.Integer Integer:114514
                class constructor java.lang.String String:"1919810"
                class constructor java.util.ArrayList Integer:1 Integer:2 Integer:3
            """;
    }

}
