package com.justnothing.testmodule.command.functions.search;

import static com.justnothing.testmodule.constants.CommandServer.CMD_SEARCH_VER;

import android.util.Log;

import com.justnothing.testmodule.command.CommandExecutor;
import com.justnothing.testmodule.command.functions.CommandBase;

public class SearchMain extends CommandBase {

    public SearchMain() {
        super("SearchMain");
    }

    @Override
    public String getHelpText() {
        return String.format("""
                语法: search <subcmd> [args...]
                
                搜索类、方法、字段或注解.
                
                子命令:
                    class <pattern>                - 搜索类名
                    method <pattern>               - 搜索方法名
                    field <pattern>                - 搜索字段名
                    annotation <pattern>           - 搜索注解
                
                选项:
                    pattern - 搜索模式，支持通配符(*)
                
                示例:
                    search class *Activity
                    search method onCreate
                    search field m*
                    search annotation Override
                
                注意:
                    - 搜索在已加载的类中进行
                    - 支持通配符*匹配任意字符
                    - 搜索结果包含完整类名和成员信息

                
                (Submodule search %s)
                """, CMD_SEARCH_VER);
    }

    @Override
    public String runMain(CommandExecutor.CmdExecContext context) {
        String[] args = context.args();
        ClassLoader classLoader = context.classLoader();
        
        logger.debug("执行search命令，参数: " + java.util.Arrays.toString(args));
        
        if (args.length < 2) {
            logger.warn("参数不足");
            return getHelpText();
        }

        String subCommand = args[0];
        String pattern = args[1];

        try {
            switch (subCommand) {
                case "class":
                    return searchClasses(pattern, classLoader);
                case "method":
                    return searchMethods(pattern, classLoader);
                case "field":
                    return searchFields(pattern, classLoader);
                case "annotation":
                    return searchAnnotations(pattern, classLoader);
                default:
                    return "未知子命令: " + subCommand + "\n" + getHelpText();
            }
        } catch (Exception e) {
            logger.error("执行search命令失败", e);
            return "错误: " + e.getMessage() +
                    "\n堆栈追踪: \n" + Log.getStackTraceString(e);
        }
    }

    private String searchClasses(String pattern, ClassLoader classLoader) {
        StringBuilder sb = new StringBuilder();
        sb.append("===== 搜索类名 =====\n");
        sb.append("模式: ").append(pattern).append("\n\n");
        
        java.util.List<Class<?>> matchedClasses = new java.util.ArrayList<>();
        
        try {
            java.lang.reflect.Field classesField = ClassLoader.class.getDeclaredField("classes");
            classesField.setAccessible(true);
            
            @SuppressWarnings("unchecked")
            java.util.Vector<Class<?>> classes = (java.util.Vector<Class<?>>) classesField.get(classLoader);
            
            for (Class<?> clazz : classes) {
                String className = clazz.getName();
                if (matchesPattern(className, pattern)) {
                    matchedClasses.add(clazz);
                }
            }
            
        } catch (Exception e) {
            logger.warn("搜索类失败", e);
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

    private String searchMethods(String pattern, ClassLoader classLoader) {
        StringBuilder sb = new StringBuilder();
        sb.append("===== 搜索方法名 =====\n");
        sb.append("模式: ").append(pattern).append("\n\n");
        
        java.util.Map<String, java.util.List<String>> matchedMethods = new java.util.LinkedHashMap<>();
        
        try {
            java.lang.reflect.Field classesField = ClassLoader.class.getDeclaredField("classes");
            classesField.setAccessible(true);
            
            @SuppressWarnings("unchecked")
            java.util.Vector<Class<?>> classes = (java.util.Vector<Class<?>>) classesField.get(classLoader);
            
            for (Class<?> clazz : classes) {
                for (java.lang.reflect.Method method : clazz.getDeclaredMethods()) {
                    if (matchesPattern(method.getName(), pattern)) {
                        String className = clazz.getName();
                        matchedMethods.computeIfAbsent(className, k -> new java.util.ArrayList<>())
                                      .add(method.toString());
                    }
                }
            }
            
        } catch (Exception e) {
            logger.warn("搜索方法失败", e);
        }
        
        if (matchedMethods.isEmpty()) {
            sb.append("未找到匹配的方法\n");
        } else {
            int totalCount = matchedMethods.values().stream().mapToInt(java.util.List::size).sum();
            sb.append("找到 ").append(totalCount).append(" 个匹配的方法:\n\n");
            
            for (java.util.Map.Entry<String, java.util.List<String>> entry : matchedMethods.entrySet()) {
                sb.append("  ").append(entry.getKey()).append(":\n");
                for (String method : entry.getValue()) {
                    sb.append("    ").append(method).append("\n");
                }
            }
        }
        
        return sb.toString();
    }

    private String searchFields(String pattern, ClassLoader classLoader) {
        StringBuilder sb = new StringBuilder();
        sb.append("===== 搜索字段名 =====\n");
        sb.append("模式: ").append(pattern).append("\n\n");
        
        java.util.Map<String, java.util.List<String>> matchedFields = new java.util.LinkedHashMap<>();
        
        try {
            java.lang.reflect.Field classesField = ClassLoader.class.getDeclaredField("classes");
            classesField.setAccessible(true);
            
            @SuppressWarnings("unchecked")
            java.util.Vector<Class<?>> classes = (java.util.Vector<Class<?>>) classesField.get(classLoader);
            
            for (Class<?> clazz : classes) {
                for (java.lang.reflect.Field field : clazz.getDeclaredFields()) {
                    if (matchesPattern(field.getName(), pattern)) {
                        String className = clazz.getName();
                        matchedFields.computeIfAbsent(className, k -> new java.util.ArrayList<>())
                                      .add(field.getType().getName() + " " + field.getName());
                    }
                }
            }
            
        } catch (Exception e) {
            logger.warn("搜索字段失败", e);
        }
        
        if (matchedFields.isEmpty()) {
            sb.append("未找到匹配的字段\n");
        } else {
            int totalCount = matchedFields.values().stream().mapToInt(java.util.List::size).sum();
            sb.append("找到 ").append(totalCount).append(" 个匹配的字段:\n\n");
            
            for (java.util.Map.Entry<String, java.util.List<String>> entry : matchedFields.entrySet()) {
                sb.append("  ").append(entry.getKey()).append(":\n");
                for (String field : entry.getValue()) {
                    sb.append("    ").append(field).append("\n");
                }
            }
        }
        
        return sb.toString();
    }

    private String searchAnnotations(String pattern, ClassLoader classLoader) {
        StringBuilder sb = new StringBuilder();
        sb.append("===== 搜索注解 =====\n");
        sb.append("模式: ").append(pattern).append("\n\n");
        
        java.util.Map<String, java.util.List<String>> matchedAnnotations = new java.util.LinkedHashMap<>();
        
        try {
            java.lang.reflect.Field classesField = ClassLoader.class.getDeclaredField("classes");
            classesField.setAccessible(true);
            
            @SuppressWarnings("unchecked")
            java.util.Vector<Class<?>> classes = (java.util.Vector<Class<?>>) classesField.get(classLoader);
            
            for (Class<?> clazz : classes) {
                for (java.lang.annotation.Annotation annotation : clazz.getAnnotations()) {
                    String annotationName = annotation.annotationType().getSimpleName();
                    if (matchesPattern(annotationName, pattern)) {
                        String className = clazz.getName();
                        matchedAnnotations.computeIfAbsent(className, k -> new java.util.ArrayList<>())
                                      .add(annotationName);
                    }
                }
                
                for (java.lang.reflect.Method method : clazz.getDeclaredMethods()) {
                    for (java.lang.annotation.Annotation annotation : method.getAnnotations()) {
                        String annotationName = annotation.annotationType().getSimpleName();
                        if (matchesPattern(annotationName, pattern)) {
                            String className = clazz.getName();
                            matchedAnnotations.computeIfAbsent(className, k -> new java.util.ArrayList<>())
                                          .add(method.getName() + " -> " + annotationName);
                        }
                    }
                }
            }
            
        } catch (Exception e) {
            logger.warn("搜索注解失败", e);
        }
        
        if (matchedAnnotations.isEmpty()) {
            sb.append("未找到匹配的注解\n");
        } else {
            int totalCount = matchedAnnotations.values().stream().mapToInt(java.util.List::size).sum();
            sb.append("找到 ").append(totalCount).append(" 个匹配的注解:\n\n");
            
            for (java.util.Map.Entry<String, java.util.List<String>> entry : matchedAnnotations.entrySet()) {
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
