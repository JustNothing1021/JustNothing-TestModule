package com.justnothing.testmodule.command.functions.classcmd;


import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;

public class SearchCommand extends AbstractClassCommand {

    public SearchCommand() {
        super("class search");
    }

    @Override
    protected String executeInternal(ClassCommandContext context) throws Exception {
        String[] args = context.getArgs();
        
        if (args.length < 2) {
            context.getLogger().warn("参数不足, 需要至少2个参数");
            return getHelpText();
        }

        String subCommand = args[0];
        String pattern = args[1];

        return switch (subCommand) {
            case "class" -> searchClasses(pattern, context);
            case "method" -> searchMethods(pattern, context);
            case "field" -> searchFields(pattern, context);
            case "annotation" -> searchAnnotations(pattern, context);
            default -> "未知子命令: " + subCommand + "\n" + getHelpText();
        };
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

    private String searchClasses(String pattern, ClassCommandContext context) {
        StringBuilder sb = new StringBuilder();
        sb.append("===== 搜索类名 =====\n");
        sb.append("模式: ").append(pattern).append("\n\n");
        
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
            sb.append("未找到匹配的类\n");
        } else {
            sb.append("找到 ").append(matchedClasses.size()).append(" 个匹配的类:\n\n");
            for (Class<?> clazz : matchedClasses) {
                sb.append("  ").append(clazz.getName()).append("\n");
            }
        }
        
        return sb.toString();
    }

    private String searchMethods(String pattern, ClassCommandContext context) {
        StringBuilder sb = new StringBuilder();
        sb.append("===== 搜索方法名 =====\n");
        sb.append("模式: ").append(pattern).append("\n\n");
        
        Map<String, List<String>> matchedMethods = new LinkedHashMap<>();
        
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
                                        .add(method.toString());
                    }
                }
            }
            
        } catch (Exception e) {
            context.getLogger().warn("搜索方法失败", e);
        }
        
        if (matchedMethods.isEmpty()) {
            sb.append("未找到匹配的方法\n");
        } else {
            int totalCount = matchedMethods.values().stream().mapToInt(List::size).sum();
            sb.append("找到 ").append(totalCount).append(" 个匹配的方法:\n\n");
            
            for (Map.Entry<String, List<String>> entry : matchedMethods.entrySet()) {
                sb.append("  ").append(entry.getKey()).append(":\n");
                for (String method : entry.getValue()) {
                    sb.append("    ").append(method).append("\n");
                }
            }
        }
        
        return sb.toString();
    }

    private String searchFields(String pattern, ClassCommandContext context) {
        StringBuilder sb = new StringBuilder();
        sb.append("===== 搜索字段名 =====\n");
        sb.append("模式: ").append(pattern).append("\n\n");
        
        Map<String, List<String>> matchedFields = new LinkedHashMap<>();
        
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
                                      .add(field.getType().getName() + " " + field.getName());
                    }
                }
            }
            
        } catch (Exception e) {
            context.getLogger().warn("搜索字段失败", e);
        }
        
        if (matchedFields.isEmpty()) {
            sb.append("未找到匹配的字段\n");
        } else {
            int totalCount = matchedFields.values().stream().mapToInt(List::size).sum();
            sb.append("找到 ").append(totalCount).append(" 个匹配的字段:\n\n");
            
            for (Map.Entry<String, List<String>> entry : matchedFields.entrySet()) {
                sb.append("  ").append(entry.getKey()).append(":\n");
                for (String field : entry.getValue()) {
                    sb.append("    ").append(field).append("\n");
                }
            }
        }
        
        return sb.toString();
    }

    private String searchAnnotations(String pattern, ClassCommandContext context) {
        StringBuilder sb = new StringBuilder();
        sb.append("===== 搜索注解 =====\n");
        sb.append("模式: ").append(pattern).append("\n\n");
        
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
            sb.append("未找到匹配的注解\n");
        } else {
            int totalCount = matchedAnnotations.values().stream().mapToInt(List::size).sum();
            sb.append("找到 ").append(totalCount).append(" 个匹配的注解:\n\n");
            
            for (Map.Entry<String, List<String>> entry : matchedAnnotations.entrySet()) {
                sb.append("  ").append(entry.getKey()).append(":\n");
                for (String annotation : entry.getValue()) {
                    sb.append("    @").append(annotation).append("\n");
                }
            }
        }
        
        return sb.toString();
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
