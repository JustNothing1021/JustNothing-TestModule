package com.justnothing.testmodule.command.functions.classcmd;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

public class GraphCommand extends AbstractClassCommand {

    public GraphCommand() {
        super("class graph");
    }

    @Override
    protected String executeInternal(ClassCommandContext context) throws Exception {
        String[] args = context.getArgs();
        String error = requireArgs(context, args, 1);
        if (error != null) {
            return error;
        }

        String className = args[0];
        Class<?> clazz = context.loadClass(className);
        
        return generateClassInheritanceGraph(clazz, context);
    }

    @Override
    public String getHelpText() {
        return """
            语法: class graph <class_name>
            
            生成类继承图.
            
            示例:
                class graph java.util.ArrayList
            """;
    }

    private String generateClassInheritanceGraph(Class<?> clazz, ClassCommandContext context) {
        StringBuilder sb = new StringBuilder();
        sb.append("===== 类继承图 =====\n");
        sb.append("类名: ").append(clazz.getName()).append("\n\n");
        
        List<Class<?>> hierarchy = getClassHierarchy(clazz);
        
        sb.append("继承层次（从顶层父类到当前类）:\n");
        for (int i = 0; i < hierarchy.size(); i++) {
            Class<?> currentClass = hierarchy.get(i);
            
            for (int j = 0; j < i; j++) {
                sb.append("  ");
            }
            sb.append("└─> ").append(currentClass.getSimpleName()).append("\n");
            
            Class<?>[] interfaces = currentClass.getInterfaces();
            if (interfaces.length > 0) {
                for (int j = 0; j < i + 1; j++) {
                    sb.append("  ");
                }
                sb.append("├─ 实现接口: ");
                for (int k = 0; k < interfaces.length; k++) {
                    if (k > 0) {
                        sb.append(", ");
                    }
                    sb.append(interfaces[k].getSimpleName());
                }
                sb.append("\n");
            }
        }
        
        sb.append("\n子类:\n");
        List<Class<?>> subclasses = findSubclasses(clazz, context);
        if (subclasses.isEmpty()) {
            sb.append("  (无)\n");
        } else {
            for (Class<?> subclass : subclasses) {
                sb.append("  ").append(subclass.getName()).append("\n");
            }
        }
        
        return sb.toString();
    }

    private List<Class<?>> getClassHierarchy(Class<?> clazz) {
        List<Class<?>> hierarchy = new ArrayList<>();
        Class<?> current = clazz;
        
        while (current != null) {
            hierarchy.add(0, current);
            current = current.getSuperclass();
        }
        
        return hierarchy;
    }

    private List<Class<?>> findSubclasses(Class<?> superClass, ClassCommandContext context) {
        List<Class<?>> subclasses = new ArrayList<>();
        
        try {
            Field classesField = ClassLoader.class.getDeclaredField("classes");
            classesField.setAccessible(true);
            
            @SuppressWarnings("unchecked")
            Vector<Class<?>> classes = (Vector<Class<?>>) classesField.get(superClass.getClassLoader());

            assert classes != null;
            for (Class<?> clazz : classes) {
                if (superClass.isAssignableFrom(clazz) && !clazz.equals(superClass)) {
                    subclasses.add(clazz);
                }
            }
        } catch (Exception e) {
            context.getLogger().warn("查找子类失败", e);
        }
        
        return subclasses;
    }
}
