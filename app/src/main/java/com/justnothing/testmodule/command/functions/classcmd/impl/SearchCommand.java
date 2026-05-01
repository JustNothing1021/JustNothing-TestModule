package com.justnothing.testmodule.command.functions.classcmd.impl;


import com.justnothing.testmodule.command.functions.classcmd.AbstractClassCommand;
import com.justnothing.testmodule.command.functions.classcmd.ClassCommandContext;
import com.justnothing.testmodule.command.functions.classcmd.request.SearchClassRequest;
import com.justnothing.testmodule.command.functions.classcmd.model.FieldInfo;
import com.justnothing.testmodule.command.functions.classcmd.response.SearchResult;
import com.justnothing.testmodule.command.functions.classcmd.model.MethodInfo;
import com.justnothing.testmodule.command.output.Colors;
import com.justnothing.testmodule.command.utils.CommandExceptionHandler;
import com.justnothing.testmodule.utils.reflect.DescriptorColorizer;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;

public class SearchCommand extends AbstractClassCommand<SearchClassRequest, SearchResult> {

    public SearchCommand() {
        super("class search", SearchClassRequest.class, SearchResult.class);
    }

    @Override
    protected SearchResult executeClassCommand(ClassCommandContext<SearchClassRequest> context) throws Exception {
        SearchClassRequest request = context.execContext().getCommandRequest();
        String searchType = request.getSearchType();
        String pattern = request.getPattern();

        if (searchType == null || pattern == null) {
            CommandExceptionHandler.handleException(
                "class search",
                new IllegalArgumentException("参数不足, 需要至少2个参数: class search <subcmd> <pattern>"),
                context.execContext(),
                "参数错误"
            );
            return null;
        }

        SearchResult result = new SearchResult();
        result.setPattern(pattern);
        result.setSuccess(true);

        switch (searchType) {
            case "class" -> {
                result.setSearchType("class");
                searchClasses(pattern, context, result);
            }
            case "method" -> {
                result.setSearchType("method");
                searchMethods(pattern, context, result);
            }
            case "field" -> {
                result.setSearchType("field");
                searchFields(pattern, context, result);
            }
            case "annotation" -> {
                result.setSearchType("annotation");
                searchAnnotations(pattern, context, result);
            }
            default -> {
                CommandExceptionHandler.handleException(
                    "class search",
                    new IllegalArgumentException("未知子命令: " + searchType),
                    context.execContext(),
                    "参数错误"
                );
                result.setSuccess(false);
            }
        }
        return result;
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

    private void searchClasses(String pattern, ClassCommandContext<SearchClassRequest> context, SearchResult result) {
        context.execContext().println("===== 搜索类名 =====", Colors.CYAN);
        context.execContext().print("模式: ", Colors.CYAN);
        context.execContext().println(pattern, Colors.YELLOW);
        context.execContext().println("");

        List<Class<?>> matchedClasses = new ArrayList<>();

        try {
            Field classesField = ClassLoader.class.getDeclaredField("classes");
            classesField.setAccessible(true);

            @SuppressWarnings("unchecked")
            Vector<Class<?>> classes = (Vector<Class<?>>) classesField.get(context.classLoader());

            assert classes != null;
            for (Class<?> clazz : classes) {
                String className = clazz.getName();
                if (matchesPattern(className, pattern)) {
                    matchedClasses.add(clazz);
                }
            }

        } catch (Exception e) {
            context.logger().warn("搜索类失败", e);
        }

        if (matchedClasses.isEmpty()) {
            context.execContext().println("未找到匹配的类", Colors.GRAY);
        } else {
            context.execContext().print("找到 ", Colors.CYAN);
            context.execContext().print(String.valueOf(matchedClasses.size()), Colors.YELLOW);
            context.execContext().println(" 个匹配的类:", Colors.CYAN);
            context.execContext().println("");
            for (Class<?> clazz : matchedClasses) {
                context.execContext().print("  ", Colors.GRAY);
                context.execContext().println(clazz.getName(), Colors.GREEN);
                result.getMatchedClasses().add(clazz.getName());
            }
        }

        result.setTotalCount(matchedClasses.size());
    }

    private void searchMethods(String pattern, ClassCommandContext<SearchClassRequest> context, SearchResult result) {
        context.execContext().println("===== 搜索方法名 =====", Colors.CYAN);
        context.execContext().print("模式: ", Colors.CYAN);
        context.execContext().println(pattern, Colors.YELLOW);
        context.execContext().println("");

        Map<String, List<Method>> matchedMethods = new LinkedHashMap<>();

        try {
            Field classesField = ClassLoader.class.getDeclaredField("classes");
            classesField.setAccessible(true);

            @SuppressWarnings("unchecked")
            Vector<Class<?>> classes = (Vector<Class<?>>) classesField.get(context.classLoader());

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
            context.logger().warn("搜索方法失败", e);
        }

        int totalCount = 0;

        if (matchedMethods.isEmpty()) {
            context.execContext().println("未找到匹配的方法", Colors.GRAY);
        } else {
            totalCount = matchedMethods.values().stream().mapToInt(List::size).sum();
            context.execContext().print("找到 ", Colors.CYAN);
            context.execContext().print(String.valueOf(totalCount), Colors.YELLOW);
            context.execContext().println(" 个匹配的方法:", Colors.CYAN);
            context.execContext().println("");

            for (Map.Entry<String, List<Method>> entry : matchedMethods.entrySet()) {
                context.execContext().print("  ", Colors.GRAY);
                context.execContext().println(entry.getKey() + ":", Colors.GREEN);
                for (Method method : entry.getValue()) {
                    context.execContext().print("    ", Colors.GRAY);
                    DescriptorColorizer.printColoredDescriptor(context.execContext(), method, true);
                    context.execContext().println("");
                    result.getMatchedMethods().add(new SearchResult.MatchedMethod(entry.getKey(), MethodInfo.fromMethod(method)));
                }
            }
        }

        result.setTotalCount(totalCount);
    }

    private void searchFields(String pattern, ClassCommandContext<SearchClassRequest> context, SearchResult result) {
        context.execContext().println("===== 搜索字段名 =====", Colors.CYAN);
        context.execContext().print("模式: ", Colors.CYAN);
        context.execContext().println(pattern, Colors.YELLOW);
        context.execContext().println("");

        Map<String, List<Field>> matchedFields = new LinkedHashMap<>();

        try {
            Field classesField = ClassLoader.class.getDeclaredField("classes");
            classesField.setAccessible(true);

            @SuppressWarnings("unchecked")
            Vector<Class<?>> classes = (Vector<Class<?>>) classesField.get(context.classLoader());

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
            context.logger().warn("搜索字段失败", e);
        }

        int totalCount = 0;

        if (matchedFields.isEmpty()) {
            context.execContext().println("未找到匹配的字段", Colors.GRAY);
        } else {
            totalCount = matchedFields.values().stream().mapToInt(List::size).sum();
            context.execContext().print("找到 ", Colors.CYAN);
            context.execContext().print(String.valueOf(totalCount), Colors.YELLOW);
            context.execContext().println(" 个匹配的字段:", Colors.CYAN);
            context.execContext().println("");

            for (Map.Entry<String, List<Field>> entry : matchedFields.entrySet()) {
                context.execContext().print("  ", Colors.GRAY);
                context.execContext().println(entry.getKey() + ":", Colors.GREEN);
                for (Field field : entry.getValue()) {
                    context.execContext().print("    ", Colors.GRAY);
                    DescriptorColorizer.printColoredDescriptor(context.execContext(), field, true);
                    context.execContext().println("");
                    result.getMatchedFields().add(FieldInfo.fromField(field));
                }
            }
        }

        result.setTotalCount(totalCount);
    }

    private void searchAnnotations(String pattern, ClassCommandContext<SearchClassRequest> context, SearchResult result) {
        context.execContext().println("===== 搜索注解 =====", Colors.CYAN);
        context.execContext().print("模式: ", Colors.CYAN);
        context.execContext().println(pattern, Colors.YELLOW);
        context.execContext().println("");

        Map<String, List<String>> matchedAnnotations = new LinkedHashMap<>();

        try {
            Field classesField = ClassLoader.class.getDeclaredField("classes");
            classesField.setAccessible(true);

            @SuppressWarnings("unchecked")
            Vector<Class<?>> classes = (Vector<Class<?>>) classesField.get(context.classLoader());

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
                            result.getMatchedAnnotations().add(new SearchResult.MatchedAnnotation(className, method.getName(), annotationName));
                        }
                    }
                }
            }

        } catch (Exception e) {
            context.logger().warn("搜索注解失败", e);
        }

        int totalCount = 0;

        if (matchedAnnotations.isEmpty()) {
            context.execContext().println("未找到匹配的注解", Colors.GRAY);
        } else {
            totalCount = matchedAnnotations.values().stream().mapToInt(List::size).sum();
            context.execContext().print("找到 ", Colors.CYAN);
            context.execContext().print(String.valueOf(totalCount), Colors.YELLOW);
            context.execContext().println(" 个匹配的注解:", Colors.CYAN);
            context.execContext().println("");

            for (Map.Entry<String, List<String>> entry : matchedAnnotations.entrySet()) {
                context.execContext().print("  ", Colors.GRAY);
                context.execContext().println(entry.getKey() + ":", Colors.GREEN);
                for (String annotation : entry.getValue()) {
                    context.execContext().print("    ", Colors.GRAY);
                    if (annotation.contains(" -> ")) {
                        String[] parts = annotation.split(" -> ");
                        context.execContext().print(parts[0], Colors.YELLOW);
                        context.execContext().print(" -> ", Colors.WHITE);
                        context.execContext().print("@", Colors.MAGENTA);
                        context.execContext().println(parts[1], Colors.GREEN);
                    } else {
                        context.execContext().print("@", Colors.MAGENTA);
                        context.execContext().println(annotation, Colors.GREEN);
                    }
                }
            }
        }

        result.setTotalCount(totalCount);
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
