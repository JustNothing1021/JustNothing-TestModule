package com.justnothing.testmodule.command.functions.classcmd.impl;

import com.justnothing.testmodule.command.functions.classcmd.AbstractClassCommand;
import com.justnothing.testmodule.command.functions.classcmd.ClassCommandContext;
import com.justnothing.testmodule.command.functions.classcmd.request.ClassGraphRequest;
import com.justnothing.testmodule.command.functions.classcmd.response.ClassGraphResult;
import com.justnothing.testmodule.command.utils.CommandExceptionHandler;
import com.justnothing.testmodule.command.output.Colors;
import com.justnothing.testmodule.utils.reflect.ClassResolver;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

public class GraphCommand extends AbstractClassCommand<ClassGraphRequest, ClassGraphResult> {

    public GraphCommand() {
        super("class graph", ClassGraphRequest.class, ClassGraphResult.class);
    }

    @Override
    protected ClassGraphResult executeClassCommand(ClassCommandContext<ClassGraphRequest> context) throws Exception {
        ClassGraphRequest request = context.execContext().getCommandRequest();
        String className = request.getClassName();

        if (className == null || className.isEmpty()) {
            CommandExceptionHandler.handleException(
                    "class graph",
                    new IllegalArgumentException("参数不足, 需要至少1个参数: class graph <class_name>"),
                    context.execContext(),
                    "参数错误"
            );
            return null;
        }

        Class<?> clazz = ClassResolver.findClassOrFail(className, context.classLoader());

        ClassGraphResult result = new ClassGraphResult();
        result.setClassName(className);
        result.setSuccess(true);

        generateClassInheritanceGraph(clazz, context, result,
                request.isShowSubclasses(), request.isShowInterfaces(),
                request.getMaxDepth(), request.isCompactMode());
        return result;
    }

    @Override
    public String getHelpText() {
        return """
            语法: class graph [options] <class_name>

            生成类继承图.

            选项:
                --no-subclasses    不显示子类
                --no-interfaces    不显示接口
                --compact          紧凑模式输出
                --depth <N>       最大遍历深度 (默认10)

            示例:
                class graph java.util.ArrayList
                class graph --depth 5 android.view.View
                class graph --compact java.util.HashMap
            """;
    }

    private void generateClassInheritanceGraph(Class<?> clazz, ClassCommandContext<ClassGraphRequest> context, ClassGraphResult result,
            boolean showSubclasses, boolean showInterfaces, int maxDepth, boolean compactMode) {
        context.execContext().println("===== 类继承图 =====", Colors.CYAN);
        context.execContext().print("类名: ", Colors.CYAN);
        context.execContext().println(clazz.getName(), Colors.GREEN);
        context.execContext().println("");

        List<Class<?>> hierarchy = getClassHierarchy(clazz, maxDepth);

        List<ClassGraphResult.HierarchyLevel> hierarchyLevels = new ArrayList<>();
        List<String> allInterfaces = new ArrayList<>();

        context.execContext().println("继承层次（从顶层父类到当前类）:", Colors.CYAN);
        for (int i = 0; i < hierarchy.size(); i++) {
            Class<?> currentClass = hierarchy.get(i);

            if (!compactMode) {
                for (int j = 0; j < i; j++) {
                    context.execContext().print("  ", Colors.GRAY);
                }
            }
            context.execContext().print(compactMode ? "└" : "└─> ", Colors.GRAY);
            context.execContext().println(currentClass.getSimpleName(), Colors.GREEN);

            if (showInterfaces) {
                Class<?>[] interfaces = currentClass.getInterfaces();
                List<String> interfaceNames = new ArrayList<>();
                if (interfaces.length > 0) {
                    if (!compactMode) {
                        for (int j = 0; j < i + 1; j++) {
                            context.execContext().print("  ", Colors.GRAY);
                        }
                    }
                    context.execContext().print(i != hierarchy.size() - 1 && !compactMode ? "├─" : "└", Colors.GRAY);
                    context.execContext().print("实现接口: ", Colors.CYAN);
                    for (int k = 0; k < interfaces.length; k++) {
                        if (k > 0) {
                            context.execContext().print(", ", Colors.WHITE);
                        }
                        context.execContext().println(interfaces[k].getSimpleName(), Colors.GREEN);
                        interfaceNames.add(interfaces[k].getName());
                        allInterfaces.add(interfaces[k].getName());
                    }
                }

                hierarchyLevels.add(new ClassGraphResult.HierarchyLevel(currentClass.getName(), i, interfaceNames));
            } else {
                hierarchyLevels.add(new ClassGraphResult.HierarchyLevel(currentClass.getName(), i, new ArrayList<>()));
            }
        }

        result.setHierarchy(hierarchyLevels);
        result.setImplementedInterfaces(allInterfaces);

        if (showSubclasses) {
            context.execContext().println("");
            context.execContext().println("子类:", Colors.CYAN);
            List<Class<?>> subclasses = findSubclasses(clazz, context);
            if (subclasses.isEmpty()) {
                context.execContext().println("  (无)", Colors.GRAY);
            } else {
                for (Class<?> subclass : subclasses) {
                    context.execContext().print("  ", Colors.GRAY);
                    context.execContext().println(subclass.getName(), Colors.GREEN);
                    result.getSubclasses().add(subclass.getName());
                }
            }
        }
    }

    private List<Class<?>> getClassHierarchy(Class<?> clazz, int maxDepth) {
        List<Class<?>> hierarchy = new ArrayList<>();
        Class<?> current = clazz;
        int depth = 0;

        while (current != null && depth < maxDepth) {
            hierarchy.add(0, current);
            current = current.getSuperclass();
            depth++;
        }

        return hierarchy;
    }

    private List<Class<?>> findSubclasses(Class<?> superClass, ClassCommandContext<ClassGraphRequest> context) {
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
            CommandExceptionHandler.handleException("class graph", e, context.execContext(), "查找子类失败");
        }
        
        return subclasses;
    }
}
