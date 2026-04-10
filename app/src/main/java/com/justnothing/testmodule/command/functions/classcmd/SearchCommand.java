package com.justnothing.testmodule.command.functions.classcmd;


import com.justnothing.testmodule.command.output.Colors;
import com.justnothing.testmodule.command.utils.CommandExceptionHandler;
import com.justnothing.testmodule.utils.reflect.DescriptorColorizer;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;

public class SearchCommand extends AbstractClassCommand {

    public SearchCommand() {
        super("class search");
    }

    @Override
    protected void executeInternal(ClassCommandContext context) throws Exception {
        String[] args = context.getArgs();
        
        if (args.length < 2) {
            CommandExceptionHandler.handleException(
                "class search",
                new IllegalArgumentException("参数不足, 需要至少2个参数: class search <subcmd> <pattern>"),
                context.getExecContext(),
                "参数错误"
            );
            return;
        }

        String subCommand = args[0];
        String pattern = args[1];

        switch (subCommand) {
            case "class" -> searchClasses(pattern, context);
            case "method" -> searchMethods(pattern, context);
            case "field" -> searchFields(pattern, context);
            case "annotation" -> searchAnnotations(pattern, context);
            default -> {
                CommandExceptionHandler.handleException(
                    "class search",
                    new IllegalArgumentException("未知子命令: " + subCommand),
                    context.getExecContext(),
                    "参数错误"
                );
            }
        }
    }

    @Override
    public String getHelpText() {
        return """
            语法: class search <subcmd> <pattern>
            
            搜索类, 方法, 字段或注解.
            
            子命令:
                class <pattern>                - 搜索类名
                method <pattern>               - 搜索方法名
                field <pattern>                - 搜索字段名
                annotation <pattern>           - 搜索注解
            
            选项:
                pattern - 搜索模式, 支持通配符(*)
            
            示例:
                class search class *Activity
                class search method onCreate
                class search field m*
                class search annotation Override
            
            注意:
                - 搜索在已加载的类中进行
                - 支持通配符*匹配任意字符
                - 搜索结果包含完整类名和成员信息
            """;
    }

    private void searchClasses(String pattern, ClassCommandContext context) {
        context.getExecContext().println("===== 搜索类名 =====", Colors.CYAN);
        context.getExecContext().print("模式: ", Colors.CYAN);
        context.getExecContext().println(pattern, Colors.YELLOW);
        context.getExecContext().println("");
        
        List<Class<?>> matchedClasses = new ArrayList<>();
        
        try {
            Field classesField = ClassLoader.class.getDeclaredField("classes");
            classesField.setAccessible(true);
            
            @SuppressWarnings("unchecked")
            Vector<Class<?>> classes = (Vector<Class<?>>) classesField.get(context.getClassLoader());

            assert classes != null;
            for (Class<?> clazz : classes) {
                String className = clazz.getName();
                if (matchesPattern(className, pattern)) {
                    matchedClasses.add(clazz);
                }
            }
            
        } catch (Exception e) {
            context.getLogger().warn("搜索类失败", e);
        }
        
        if (matchedClasses.isEmpty()) {
            context.getExecContext().println("未找到匹配的类", Colors.GRAY);
        } else {
            context.getExecContext().print("找到 ", Colors.CYAN);
            context.getExecContext().print(String.valueOf(matchedClasses.size()), Colors.YELLOW);
            context.getExecContext().println(" 个匹配的类:", Colors.CYAN);
            context.getExecContext().println("");
            for (Class<?> clazz : matchedClasses) {
                context.getExecContext().print("  ", Colors.GRAY);
                context.getExecContext().println(clazz.getName(), Colors.GREEN);
            }
        }
    }

    private void searchMethods(String pattern, ClassCommandContext context) {
        context.getExecContext().println("===== 搜索方法名 =====", Colors.CYAN);
        context.getExecContext().print("模式: ", Colors.CYAN);
        context.getExecContext().println(pattern, Colors.YELLOW);
        context.getExecContext().println("");
        
        Map<String, List<Method>> matchedMethods = new LinkedHashMap<>();
        
        try {
            Field classesField = ClassLoader.class.getDeclaredField("classes");
            classesField.setAccessible(true);
            
            @SuppressWarnings("unchecked")
            Vector<Class<?>> classes = (Vector<Class<?>>) classesField.get(context.getClassLoader());

            assert classes != null;
            for (Class<?> clazz : classes) {
                for (Method method : clazz.getDeclaredMethods()) {
                    if (matchesPattern(method.getName(), pattern)) {
                        String className = clazz.getName();
                        matchedMethods.computeIfAbsent(className, k -> new ArrayList<>())
                                        .add(method);
                    }
                }
            }
            
        } catch (Exception e) {
            context.getLogger().warn("搜索方法失败", e);
        }
        
        if (matchedMethods.isEmpty()) {
            context.getExecContext().println("未找到匹配的方法", Colors.GRAY);
        } else {
            int totalCount = matchedMethods.values().stream().mapToInt(List::size).sum();
            context.getExecContext().print("找到 ", Colors.CYAN);
            context.getExecContext().print(String.valueOf(totalCount), Colors.YELLOW);
            context.getExecContext().println(" 个匹配的方法:", Colors.CYAN);
            context.getExecContext().println("");
            
            for (Map.Entry<String, List<Method>> entry : matchedMethods.entrySet()) {
                context.getExecContext().print("  ", Colors.GRAY);
                context.getExecContext().println(entry.getKey() + ":", Colors.GREEN);
                for (Method method : entry.getValue()) {
                    context.getExecContext().print("    ", Colors.GRAY);
                    DescriptorColorizer.printColoredDescriptor(context.getExecContext(), method, true);
                    context.getExecContext().println("");
                }
            }
        }
    }

    private void searchFields(String pattern, ClassCommandContext context) {
        context.getExecContext().println("===== 搜索字段名 =====", Colors.CYAN);
        context.getExecContext().print("模式: ", Colors.CYAN);
        context.getExecContext().println(pattern, Colors.YELLOW);
        context.getExecContext().println("");
        
        Map<String, List<Field>> matchedFields = new LinkedHashMap<>();
        
        try {
            Field classesField = ClassLoader.class.getDeclaredField("classes");
            classesField.setAccessible(true);
            
            @SuppressWarnings("unchecked")
            Vector<Class<?>> classes = (Vector<Class<?>>) classesField.get(context.getClassLoader());

            assert classes != null;
            for (Class<?> clazz : classes) {
                for (Field field : clazz.getDeclaredFields()) {
                    if (matchesPattern(field.getName(), pattern)) {
                        String className = clazz.getName();
                        matchedFields.computeIfAbsent(className, k -> new ArrayList<>())
                                      .add(field);
                    }
                }
            }
            
        } catch (Exception e) {
            context.getLogger().warn("搜索字段失败", e);
        }
        
        if (matchedFields.isEmpty()) {
            context.getExecContext().println("未找到匹配的字段", Colors.GRAY);
        } else {
            int totalCount = matchedFields.values().stream().mapToInt(List::size).sum();
            context.getExecContext().print("找到 ", Colors.CYAN);
            context.getExecContext().print(String.valueOf(totalCount), Colors.YELLOW);
            context.getExecContext().println(" 个匹配的字段:", Colors.CYAN);
            context.getExecContext().println("");
            
            for (Map.Entry<String, List<Field>> entry : matchedFields.entrySet()) {
                context.getExecContext().print("  ", Colors.GRAY);
                context.getExecContext().println(entry.getKey() + ":", Colors.GREEN);
                for (Field field : entry.getValue()) {
                    context.getExecContext().print("    ", Colors.GRAY);
                    DescriptorColorizer.printColoredDescriptor(context.getExecContext(), field, true);
                    context.getExecContext().println("");
                }
            }
        }
    }

    private void searchAnnotations(String pattern, ClassCommandContext context) {
        context.getExecContext().println("===== 搜索注解 =====", Colors.CYAN);
        context.getExecContext().print("模式: ", Colors.CYAN);
        context.getExecContext().println(pattern, Colors.YELLOW);
        context.getExecContext().println("");
        
        Map<String, List<String>> matchedAnnotations = new LinkedHashMap<>();
        
        try {
            Field classesField = ClassLoader.class.getDeclaredField("classes");
            classesField.setAccessible(true);
            
            @SuppressWarnings("unchecked")
            Vector<Class<?>> classes = (Vector<Class<?>>) classesField.get(context.getClassLoader());

            assert classes != null;
            for (Class<?> clazz : classes) {
                for (Annotation annotation : clazz.getAnnotations()) {
                    String annotationName = annotation.annotationType().getSimpleName();
                    if (matchesPattern(annotationName, pattern)) {
                        String className = clazz.getName();
                        matchedAnnotations.computeIfAbsent(className, k -> new ArrayList<>())
                                      .add(annotationName);
                    }
                }
                
                for (Method method : clazz.getDeclaredMethods()) {
                    for (Annotation annotation : method.getAnnotations()) {
                        String annotationName = annotation.annotationType().getSimpleName();
                        if (matchesPattern(annotationName, pattern)) {
                            String className = clazz.getName();
                            matchedAnnotations.computeIfAbsent(className, k -> new ArrayList<>())
                                          .add(method.getName() + " -> " + annotationName);
                        }
                    }
                }
            }
            
        } catch (Exception e) {
            context.getLogger().warn("搜索注解失败", e);
        }
        
        if (matchedAnnotations.isEmpty()) {
            context.getExecContext().println("未找到匹配的注解", Colors.GRAY);
        } else {
            int totalCount = matchedAnnotations.values().stream().mapToInt(List::size).sum();
            context.getExecContext().print("找到 ", Colors.CYAN);
            context.getExecContext().print(String.valueOf(totalCount), Colors.YELLOW);
            context.getExecContext().println(" 个匹配的注解:", Colors.CYAN);
            context.getExecContext().println("");
            
            for (Map.Entry<String, List<String>> entry : matchedAnnotations.entrySet()) {
                context.getExecContext().print("  ", Colors.GRAY);
                context.getExecContext().println(entry.getKey() + ":", Colors.GREEN);
                for (String annotation : entry.getValue()) {
                    context.getExecContext().print("    ", Colors.GRAY);
                    if (annotation.contains(" -> ")) {
                        String[] parts = annotation.split(" -> ");
                        context.getExecContext().print(parts[0], Colors.YELLOW);
                        context.getExecContext().print(" -> ", Colors.WHITE);
                        context.getExecContext().print("@", Colors.MAGENTA);
                        context.getExecContext().println(parts[1], Colors.GREEN);
                    } else {
                        context.getExecContext().print("@", Colors.MAGENTA);
                        context.getExecContext().println(annotation, Colors.GREEN);
                    }
                }
            }
        }
    }

    private boolean matchesPattern(String text, String pattern) {
        if (pattern == null || pattern.isEmpty()) {
            return true;
        }
        
        if (pattern.equals("*")) {
            return true;
        }
        
        String regex = pattern.replace(".", "\\.")
                             .replace("*", ".*");
        
        return text.matches(regex);
    }
}
