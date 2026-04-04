package com.justnothing.testmodule.command.functions.classcmd;

import com.justnothing.testmodule.command.utils.CommandExceptionHandler;
import com.justnothing.testmodule.command.output.Colors;

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
        
        generateClassInheritanceGraph(clazz, context);
        return null;
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

    private void generateClassInheritanceGraph(Class<?> clazz, ClassCommandContext context) {
        context.getExecContext().println("===== 类继承图 =====", Colors.CYAN);
        context.getExecContext().print("类名: ", Colors.CYAN);
        context.getExecContext().println(clazz.getName(), Colors.GREEN);
        context.getExecContext().println("");
        
        List<Class<?>> hierarchy = getClassHierarchy(clazz);
        
        context.getExecContext().println("继承层次（从顶层父类到当前类）:", Colors.CYAN);
        for (int i = 0; i < hierarchy.size(); i++) {
            Class<?> currentClass = hierarchy.get(i);
            
            for (int j = 0; j < i; j++) {
                context.getExecContext().print("  ", Colors.GRAY);
            }
            context.getExecContext().print("└─> ", Colors.GRAY);
            context.getExecContext().println(currentClass.getSimpleName(), Colors.GREEN);
            
            Class<?>[] interfaces = currentClass.getInterfaces();
            if (interfaces.length > 0) {
                for (int j = 0; j < i + 1; j++) {
                    context.getExecContext().print("  ", Colors.GRAY);
                }
                context.getExecContext().print(i != hierarchy.size() - 1 ? "├─" : "└", Colors.GRAY);
                context.getExecContext().print("实现接口: ", Colors.CYAN);
                for (int k = 0; k < interfaces.length; k++) {
                    if (k > 0) {
                        context.getExecContext().print(", ", Colors.WHITE);
                    }
                    context.getExecContext().print(interfaces[k].getSimpleName(), Colors.GREEN);
                }
                context.getExecContext().println("");
            }
        }
        
        context.getExecContext().println("");
        context.getExecContext().println("子类:", Colors.CYAN);
        List<Class<?>> subclasses = findSubclasses(clazz, context);
        if (subclasses.isEmpty()) {
            context.getExecContext().println("  (无)", Colors.GRAY);
        } else {
            for (Class<?> subclass : subclasses) {
                context.getExecContext().print("  ", Colors.GRAY);
                context.getExecContext().println(subclass.getName(), Colors.GREEN);
            }
        }
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
            CommandExceptionHandler.handleException("class graph", e, context.getExecContext(), "查找子类失败");
        }
        
        return subclasses;
    }
}
